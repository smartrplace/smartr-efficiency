package org.smartrplace.apps.alarmconfig.reminder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.resourcemanager.pattern.PatternListener;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.messaging.api.MailSessionServiceI;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.model.user.NaturalPerson;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.smartrplace.apps.alarmconfig.util.AlarmResourceUtil;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.tissue.util.resource.GatewayUtil;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

/**
 * For device issues represented by AlarmGroupData resources this service creates reminder emails. Depending on the settings of the
 * recipient (see {@link AlarmGroupData#responsibility()}), these are either sent immediately when the {@link AlarmGroupData#dueDateForResponsibility()} has passed, or are
 * aggregated per recipient and sent once per day (see {@link ReminderAggregationService}).
 */
public class DeviceAlarmReminderService implements PatternListener<AlarmReminderPattern>, ResourceValueListener<TimeResource>, AutoCloseable {
	
	private static final AtomicLong THREAD_COUNT = new AtomicLong();
	private static final long PAST_REMINDER_DURATION = 48*3_600_000; // 2 days
	private final ApplicationManager appMan;
	private final ApplicationManagerPlus appManPlus;
	private final String senderEmail;
	private final String senderName;
	private final ReminderAggregationService aggregationService;
	private Timer timer;
	
	private final List<AlarmReminderPattern> alarms = new ArrayList<>();
	
	public DeviceAlarmReminderService(ApplicationManagerPlus appManPlus) {
		this.appMan = appManPlus.appMan();
		this.appManPlus = appManPlus;
		long startupDelay = 200_000;
		try {
			startupDelay = Long.getLong("org.smartrplace.apps.alarmingconfig.devicealarmreminder.delay");
		} catch (Exception ignore) {}
		this.senderEmail = System.getProperty("org.smartrplace.apps.alarmingconfig.devicealarmreminder.sender.email", "no-reply@smartrplace.com");
		this.senderName = System.getProperty("org.smartrplace.apps.alarmingconfig.devicealarmreminder.sender.name", "Smartrplace Messaging");
		this.timer = appMan.createTimer(startupDelay, this::init);
		this.aggregationService = new ReminderAggregationService(appMan);
	}
	
	private void init(Timer timer) {
		appMan.getLogger().info("{} starting", getClass().getName());
		// initialize contacts in GatewaySuperiorData#responsibilityContacts... provisional
		new ResponsibilityContactsInitializer(appMan).run();
		this.appMan.getResourcePatternAccess().addPatternDemand(AlarmReminderPattern.class, this, AccessPriority.PRIO_LOWEST);
		this.closeTimer();
	}
	
	private void closeTimer() {
		if (timer != null) {
			try {
				this.timer.destroy();
			} catch (Exception e) {}
		}
		this.timer = null;
	}
	
	@Override
	public void close() {
		this.closeTimer();
		this.aggregationService.close();
		this.appMan.getResourcePatternAccess().removePatternDemand(AlarmReminderPattern.class, this);
	}
	
	/**
	 * Must only be called from app thread => use appMan#submitEvent if in doubt
	 */
	private void retrigger() {
		this.closeTimer();
		final List<AlarmConfig> configs = alarms.stream()
			.filter(AlarmReminderPattern::isActive)
			.map(AlarmConfig::new)
			.sorted()
			.collect(Collectors.toList());
		if (configs.isEmpty())
			return;
		final long now = appMan.getFrameworkTime();
		final List<AlarmConfig> alarmsToBeSent = configs.stream()
			.filter(cfg -> cfg.t <= now && now - cfg.t < PAST_REMINDER_DURATION)  // at most 2 days old reminders are triggered
			.collect(Collectors.toList());
		boolean retrigger = false;
		if (!alarmsToBeSent.isEmpty()) {
			final BundleContext ctx = appMan.getAppID().getBundle().getBundleContext();
			final ServiceReference<MailSessionServiceI> serviceRef = ctx.getServiceReference(MailSessionServiceI.class);
			final MailSessionServiceI service = serviceRef != null ? ctx.getService(serviceRef) : null;
			if (service == null) {
				appMan.getLogger().error("No mail service configured, cannot send alarm reminders {}", alarmsToBeSent);
			} else {
				final ExecutorService exec = Executors.newSingleThreadExecutor(r -> new Thread(r, "device-alarm-reminder-" + THREAD_COUNT.getAndIncrement()));
				final Future<Integer> future = exec.submit(() -> {
					try {
						final int retriggerCount = alarmsToBeSent.stream()
							.map(a -> this.trigger(a, service))
							.map(bool -> bool ? 1 : 0)
							.reduce(0, (a,b) -> a+b);
						return retriggerCount;
					} finally {
						ctx.ungetService(serviceRef);
					}
				});
				try {
					final int count = future.get(15, TimeUnit.MINUTES); // FIXME this blocks the app thread, may not be ideal... but should not take so long normally
					if (count > 0)
						retrigger = true;
				} catch (ExecutionException|TimeoutException e) {
					appMan.getLogger().warn("Sending {} device alarm reminders did not succeed", alarmsToBeSent.size(), e);
					retrigger = true; // ?
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					exec.shutdownNow();
				}
			}
		}
		if (Thread.interrupted()) {
			Thread.currentThread().interrupt();
			return;
		}
		final Optional<AlarmConfig> next = configs.stream().filter(a -> a.t > now).findFirst();
		final long nextExec = retrigger ? 60_000 : next.isPresent() ? Math.max(next.get().t - now, 60_000) : -1;
		if (nextExec > 0)
			this.timer = appMan.createTimer(nextExec, t -> this.retrigger());
	}

	private boolean trigger(AlarmConfig cfg, MailSessionServiceI emailService) {
		if (!cfg.config.isActive())
			return false;
		boolean reRemind = cfg.config.reminderType==null || !cfg.config.reminderType.isActive() || cfg.config.reminderType.getValue() >= 0;
		if(!reRemind)
			cfg.config.dueDate.deactivate(false);
		else {
			long startOfDay = AbsoluteTimeHelper.getIntervalStart(appMan.getFrameworkTime(), AbsoluteTiming.DAY);
			final int type = cfg.config.reminderType.isActive() ? cfg.config.reminderType.getValue() : 0;
			long nextReminder;
			if(type == 1)
				nextReminder = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startOfDay, 1, AbsoluteTiming.DAY) + 4*TimeProcUtil.HOUR_MILLIS;
			else if(type == 2)
				nextReminder = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startOfDay, 7, AbsoluteTiming.DAY) + 4*TimeProcUtil.HOUR_MILLIS;
			else if(type == 3) {
				long startOfMonth = AbsoluteTimeHelper.getIntervalStart(appMan.getFrameworkTime(), AbsoluteTiming.MONTH);
				nextReminder = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startOfMonth, 1, AbsoluteTiming.MONTH) + 4*TimeProcUtil.HOUR_MILLIS;
			} else
				nextReminder = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startOfDay, 3, AbsoluteTiming.DAY) + 4*TimeProcUtil.HOUR_MILLIS;
			cfg.config.dueDate.setValue(nextReminder);
		}
		final String recipient = cfg.config.responsible.isActive() && cfg.config.responsible.getValue().contains("@") ?
				cfg.config.responsible.getValue() : "alarming@smartrplace.com";
		try {
			boolean deactivated = false;
			final AlarmGroupData alarm = cfg.config.model;
			final String gwId = GatewayUtil.getGatewayId(appMan.getResourceAccess());
			final StringBuilder sb = new StringBuilder()
				.append("This is a reminder for the device alarm ");
			String deviceName;
			String deviceId;
			try {
				InstallAppDevice iad = AlarmResourceUtil.getDeviceForKnownFault(alarm);
				if((!iad.device().isActive()) || iad.isTrash().getValue())
					deactivated = true;
				deviceId = iad.deviceId().getValue();
				deviceName = deviceId +" ("+ResourceUtils.getHumanReadableName(iad.device())+")";
				String nameInHwInstall = DeviceTableRaw.getName(iad, appManPlus);
				deviceName = nameInHwInstall + " : "+deviceName;
				deviceId = nameInHwInstall + " : "+deviceId;
			} catch (Exception e) {
				deviceId = alarm.getPath();
				deviceName = alarm.getPath();
			}
			if(deactivated) {
				cfg.config.dueDate.deactivate(false);
				return false;
			}
			sb.append(deviceName).append(" on gateway ").append(gwId);
			sb.append('.');
			if (alarm.comment().isActive()) {
				sb.append("<br>Comment: ").append(alarm.comment().getValue());
			}
			if (alarm.ongoingAlarmStartTime().isActive()) {
				sb.append("<br>In alarm state since: ")
					.append(StringFormatHelper.getTimeDateInLocalTimeZone(alarm.ongoingAlarmStartTime().getValue())) //DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(alarm.ongoingAlarmStartTime().getValue()), ZoneId.systemDefault())))
					.append('.');
			}
			sb.append("<br>Next reminder: ")
				.append(StringFormatHelper.getTimeDateInLocalTimeZone(alarm.dueDateForResponsibility().getValue()));
			final LocalGatewayInformation gwRes = ResourceHelper.getLocalGwInfo(appMan);
			final String baseUrl = gwRes.gatewayBaseUrl().getValue();
			final String subject = "Device issue reminder " + gwId + ": " + deviceName;
			String msg = sb.toString();
			if(alarm.linkToTaskTracking().isActive()) {
				msg += "<br>Issue Link: " +generateHtmlLink(alarm.linkToTaskTracking().getValue());
			}
			if (baseUrl != null && !baseUrl.isEmpty())
				msg += "<br>Issue Data: <a href=\"" + baseUrl + "/org/smartrplace/alarmingexpert/deviceknownfaults.html\">" + baseUrl + "/org/smartrplace/alarmingexpert/deviceknownfaults.html</a>";
			final NaturalPerson responsible = findResponsible(recipient);
			if (requiresAggregation(responsible)) {
				final PendingEmail pending = alarm.addDecorator(AlarmResourceUtil.PENDING_REMINDER_EMAIL_SUBRESOURCE, PendingEmail.class);
				pending.senderEmail().<StringResource> create().setValue(senderEmail);
				pending.senderName().<StringResource> create().setValue(senderName);
				pending.subject().<StringResource> create().setValue(deviceId);
				pending.message().<StringResource> create().setValue(msg);
				pending.activate(true);
			} else {
				appMan.getLogger().info("Sending device alarm reminder to {}: {}", recipient, msg);
				emailService.newMessage()
					.withSender(senderEmail, senderName)
					.withSubject(subject)
					.addHtml(msg)
					//.addText(msg)
					.addTo(recipient)
					.send();
			}
		} catch (IOException e) {
			appMan.getLogger().error("Failed to send alarm reminder for {} to {}", cfg.config.model, recipient, e);
		}
		return reRemind;
	}
	
	public static String generateHtmlLink(String link) {
		return "<a href=\"" + link + "\">" + link + "</a>";
	}
	
	private static boolean requiresAggregation(NaturalPerson contact) {
		if (contact == null)
			return false;
		final StringResource aggregationMode = contact.getSubResource(AlarmResourceUtil.EMAIL_AGGREGATION_SUBRESOURCE);
		return aggregationMode != null && aggregationMode.isActive() && !"none".equals(aggregationMode.getValue());
	}

	private NaturalPerson findResponsible(String email) {
		final ResourceList<NaturalPerson> contacts = appMan.getResourceAccess().getResource("gatewaySuperiorDataRes/responsibilityContacts");
		if (contacts == null || !contacts.isActive())
			return null;
		return contacts.getAllElements().stream().filter(ctc -> email.equalsIgnoreCase(ctc.getSubResource("emailAddress", StringResource.class).getValue()))
			.findAny().orElse(null);
	}
	
	@Override
	public void patternAvailable(AlarmReminderPattern pattern) {
		alarms.add(pattern);
		pattern.dueDate.addValueListener(this);
		final long t = pattern.dueDate.getValue();
		final long now = appMan.getFrameworkTime();
		if (t - now > - PAST_REMINDER_DURATION) // consider all at most 2 days old
			retrigger();
	}

	@Override
	public void patternUnavailable(AlarmReminderPattern pattern) {
		alarms.remove(pattern);
		pattern.dueDate.removeValueListener(this);
	}
	
	@Override
	public void resourceChanged(TimeResource dueDate) {
		this.retrigger();
	}
	
	private static class AlarmConfig implements Comparable<AlarmConfig> {
		
		private final long t;
		private final AlarmReminderPattern config;
		
		public AlarmConfig(AlarmReminderPattern config) {
			this.t = config.dueDate.getValue();
			this.config = config;
		}
		
		@Override
		public int compareTo(AlarmConfig other) {
			return Long.compare(this.t, other.t);
		}
		
		@Override
		public String toString() {
			return "AlarmConfig[" + config.model + "]";
		}
		
	}

	
}
