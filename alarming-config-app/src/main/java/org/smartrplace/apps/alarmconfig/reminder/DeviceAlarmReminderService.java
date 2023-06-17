package org.smartrplace.apps.alarmconfig.reminder;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.smartrplace.apps.alarmconfig.util.AlarmResourceUtil;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.tissue.util.resource.GatewayUtil;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

public class DeviceAlarmReminderService implements PatternListener<AlarmReminderPattern>, ResourceValueListener<TimeResource>, AutoCloseable {
	
	private static final long PAST_REMINDER_DURATION = 48*3_600_000; // 2 days
	private final ApplicationManager appMan;
	private final ApplicationManagerPlus appManPlus;
	private Timer timer;
	
	private final List<AlarmReminderPattern> alarms = new ArrayList<>();
	
	public DeviceAlarmReminderService(ApplicationManagerPlus appManPlus) {
		this.appMan = appManPlus.appMan();
		this.appManPlus = appManPlus;
		long startupDelay = 200_000;
		try {
			startupDelay = Long.getLong("org.smartrplace.apps.alarmingconfig.devicealarmreminder.delay");
		} catch (Exception ignore) {}
		this.timer = appMan.createTimer(startupDelay, this::init);
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
		this.appMan.getResourcePatternAccess().removePatternDemand(AlarmReminderPattern.class, this);
	}
	
	private void retrigger() {
		this.closeTimer();
		final List<AlarmConfig> configs = alarms.stream()
			//.filter(alarm -> !alarm.s.isActive() || alarm.releaseStatus.getValue() == 2) // not reminding of alarms proposed for release(?)
			.map(AlarmConfig::new)
			.sorted()
			.collect(Collectors.toList());
		if (configs.isEmpty())
			return;
		final long now = appMan.getFrameworkTime();
		final List<AlarmConfig> alarmsToBeSent = configs.stream()
			.filter(cfg -> cfg.t <= now && now - cfg.t < PAST_REMINDER_DURATION)  // at most 2 days old reminders are triggered
			.collect(Collectors.toList());
		if (!alarmsToBeSent.isEmpty()) {
			final BundleContext ctx = appMan.getAppID().getBundle().getBundleContext();
			final ServiceReference<MailSessionServiceI> serviceRef = ctx.getServiceReference(MailSessionServiceI.class);
			final MailSessionServiceI service = serviceRef != null ? ctx.getService(serviceRef) : null;
			if (service == null) {
				appMan.getLogger().error("No mail service configured, cannot send alarm reminders {}", alarmsToBeSent);
			} else {
				new Thread(() -> {
					try {
						alarmsToBeSent.forEach(a -> this.trigger(a, service));
					} finally {
						ctx.ungetService(serviceRef);
					}
				}, "device-alarm-reminder-service").start();
			}
		}
		final Optional<AlarmConfig> next = configs.stream().filter(a -> a.t > now).findFirst();
		if (next.isPresent())
			this.timer = appMan.createTimer(next.get().t - now, t -> this.retrigger());
	}

	private void trigger(AlarmConfig cfg, MailSessionServiceI emailService) {
		boolean reRemind = (cfg.config.reminderType == null) || (cfg.config.reminderType.getValue() >= 0);
		if(!reRemind)
			cfg.config.dueDate.deactivate(false);
		else {
			long startOfDay = AbsoluteTimeHelper.getIntervalStart(appMan.getFrameworkTime(), AbsoluteTiming.DAY);
			int type = 0;
			if(cfg.config.reminderType != null) {
				type = cfg.config.reminderType.getValue();
			}
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
			final AlarmGroupData alarm = cfg.config.model;
			final String gwId = GatewayUtil.getGatewayId(appMan.getResourceAccess());
			final StringBuilder sb = new StringBuilder()
				.append("This is a reminder for the device alarm ");
			String deviceName; 
			try {
				InstallAppDevice iad = AlarmResourceUtil.getDeviceForKnownFault(alarm);
				deviceName = iad.deviceId().getValue()+"("+ResourceUtils.getHumanReadableName(iad.device())+")";
				String nameInHwInstall = DeviceTableRaw.getName(iad, appManPlus);
				deviceName = nameInHwInstall + " : "+deviceName;
			} catch (Exception e) {
				deviceName = alarm.getPath();
			}
			sb.append(deviceName).append(" on gateway ").append(gwId);
			sb.append('.');
			if (alarm.comment().isActive()) {
				sb.append("\r\nComment: ").append(alarm.comment().getValue());
			}
			if (alarm.ongoingAlarmStartTime().isActive()) {
				sb.append("\r\nIn alarm state since: ")
					.append(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(alarm.ongoingAlarmStartTime().getValue()), ZoneId.systemDefault())))
					.append('.');
			}
			final LocalGatewayInformation gwRes = ResourceHelper.getLocalGwInfo(appMan);
			final String baseUrl = gwRes.gatewayBaseUrl().getValue();
			final String subject = "Device issue reminder " + gwId + ": " + deviceName;
			String msg = sb.toString();
			if (baseUrl != null && !baseUrl.isEmpty())
				msg += "\r\nLink: " + baseUrl + "/org/smartrplace/alarmingexpert/deviceknownfaults.html";
			appMan.getLogger().info("Sending device alarm reminder to {}: {}", recipient, msg);
			emailService.newMessage()
				.withSender("no-reply@smartrplace.com", "Smartrplace Messaging")
				.withSubject(subject)
				.addText(msg)
				.addTo(recipient)
				.send();
		} catch (IOException e) {
			appMan.getLogger().error("Failed to send alarm reminder for {} to {}", cfg.config.model, recipient, e);
		}
		if(reRemind)
			retrigger();
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
