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
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.resourcemanager.pattern.PatternListener;
import org.ogema.messaging.api.MailSessionServiceI;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.tools.resource.util.ResourceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.smartrplace.apps.alarmconfig.util.AlarmResourceUtil;
import org.smartrplace.tissue.util.resource.GatewayUtil;

import de.iwes.util.resource.ResourceHelper;

public class DeviceAlarmReminderService implements PatternListener<AlarmReminderPattern>, ResourceValueListener<TimeResource>, AutoCloseable {
	
	private static final long PAST_REMINDER_DURATION = 48*3_600_000; // 2 days
	private final ApplicationManager appMan;
	private Timer timer;
	
	private final List<AlarmReminderPattern> alarms = new ArrayList<>();
	
	public DeviceAlarmReminderService(ApplicationManager appMan) {
		this.appMan = appMan;
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
			.filter(alarm -> !alarm.releaseStatus.isActive() || alarm.releaseStatus.getValue() == 2) // not reminding of alarms proposed for release(?)
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
		cfg.config.dueDate.deactivate(false);
		final String recipient = cfg.config.responsible.isActive() && cfg.config.responsible.getValue().contains("@") ?
				cfg.config.responsible.getValue() : "alarming@smartrplace.com";
		try {
			final AlarmGroupData alarm = cfg.config.model;
			final String gwId = GatewayUtil.getGatewayId(appMan.getResourceAccess());
			final StringBuilder sb = new StringBuilder()
				.append("This is a reminder for the device alarm ");
			String deviceName = alarm.getPath();
			try {
				deviceName = ResourceUtils.getHumanReadableName(AlarmResourceUtil.getDeviceForKnownFault(alarm).device());
			} catch (Exception e) {}
			sb.append(deviceName).append(" on gateway ").append(gwId);
			sb.append('.');
			if (alarm.comment().isActive()) {
				sb.append(" Comment: ").append(alarm.comment().getValue());
			}
			if (alarm.ongoingAlarmStartTime().isActive()) {
				sb.append(" In alarm state since: ")
					.append(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(alarm.ongoingAlarmStartTime().getValue()), ZoneId.systemDefault())))
					.append('.');
			}
			final LocalGatewayInformation gwRes = ResourceHelper.getLocalGwInfo(appMan);
			final String baseUrl = gwRes.gatewayBaseUrl().getValue();
			final String subject = "Device issue reminder " + gwId + ": " + deviceName;
			if (baseUrl != null && !baseUrl.isEmpty())
				sb.append(" Link: ").append(baseUrl).append("/org/smartrplace/alarmingexpert/deviceknownfaults.html");
			final String msg = sb.toString();
			appMan.getLogger().info("Sending device alarm reminder to {}: {}", recipient, msg);
			emailService.newMessage()
				.withSender("alarming@smartrplace.com") // ?
				.withSubject(subject)
				.addText(msg)
				.addTo(recipient)
				.send();
		} catch (IOException e) {
			appMan.getLogger().error("Failed to send alarm reminder for {} to {}", cfg.config.model, recipient, e);
		}
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
