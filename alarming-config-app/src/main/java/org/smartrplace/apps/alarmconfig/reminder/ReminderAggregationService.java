package org.smartrplace.apps.alarmconfig.reminder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.Resource;
import org.ogema.messaging.api.MailSessionServiceI;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.smartrplace.apps.alarmconfig.util.AlarmResourceUtil;
import org.smartrplace.tissue.util.resource.GatewayUtil;

/**
 * Aggregates device issue reminder emails per recipient and sends them once per day at 7am. 
 * It looks for subresources of name {@link AlarmResourceUtil#PENDING_REMINDER_EMAIL_SUBRESOURCE} and type {@link PendingEmail}
 * of the device alarm resources of type {@link AlarmGroupData}. These are created by {@link DeviceAlarmReminderService}. 
 */
public class ReminderAggregationService implements TimerListener, AutoCloseable {

	private static final AtomicLong THREAD_COUNT = new AtomicLong();
	private final ApplicationManager appMan;
	private final Timer timer;
	
	public ReminderAggregationService(ApplicationManager appMan) {
		this.appMan = appMan;
		this.timer = appMan.createTimer(untilNextExec(), this);
	}
	
	@Override
	public void close() {
		try {
			this.timer.destroy();
		} catch (Exception ignore) {}
	}
	
	private void send() throws InterruptedException {
		final List<AlarmGroupData> alarmsWithPendingReminders = appMan.getResourceAccess().getResources(AlarmGroupData.class).stream()
			.filter(ReminderAggregationService::hasPendingAlarm)
			.collect(Collectors.toList());
		appMan.getLogger().info("Pending alarm reminders for aggregation: {}", alarmsWithPendingReminders.size());
		if (alarmsWithPendingReminders.isEmpty())
			return;
		final BundleContext ctx = appMan.getAppID().getBundle().getBundleContext();
		final ServiceReference<MailSessionServiceI> serviceRef = ctx.getServiceReference(MailSessionServiceI.class);
		final MailSessionServiceI service = serviceRef != null ? ctx.getService(serviceRef) : null;
		if (service == null) {  
			appMan.getLogger().error("No mail service configured, cannot send alarm reminders for issues {}", alarmsWithPendingReminders);
			return;
		}
		final ExecutorService exec = Executors.newSingleThreadExecutor(r -> new Thread(r, "device-alarm-reminder-aggregation-" + THREAD_COUNT.getAndIncrement()));
		final Future<?> future = exec.submit(() -> sendAll(alarmsWithPendingReminders.stream(), ctx, serviceRef, service));
		try {
			future.get(15, TimeUnit.MINUTES);
		} catch (ExecutionException|TimeoutException e) {
			appMan.getLogger().warn("Sending {} device alarm reminders in aggregation did not succeed", alarmsWithPendingReminders.size(), e);
		} finally {
			exec.shutdownNow();
		}
	}
	
	// aggregates alarms by the responsibility() subresource
	private void sendAll(Stream<AlarmGroupData> alarms, final BundleContext ctx, final ServiceReference<MailSessionServiceI> serviceRef, final MailSessionServiceI service) {
		try {
			final AtomicInteger success = new AtomicInteger(0);
			final AtomicInteger failure = new AtomicInteger(0);
			final Map<String, List<AlarmGroupData>> alarmsByRecipient = alarms.collect(Collectors.groupingBy(alarm -> alarm.responsibility().getValue()));
			final int subjLimit = 3;
			final String gwId = GatewayUtil.getGatewayId(appMan.getResourceAccess());
			for (Map.Entry<String, List<AlarmGroupData>> alarmGroup: alarmsByRecipient.entrySet()) {
				try {
					final List<AlarmMessage> activeAlarms = alarmGroup.getValue().stream()
							.map(alarm -> alarm.<PendingEmail> getSubResource(AlarmResourceUtil.PENDING_REMINDER_EMAIL_SUBRESOURCE))
							.filter(res -> res != null && res.isActive())
							.map(msg -> new AlarmMessage(msg))
							.filter(AlarmMessage::isValid)
							.collect(Collectors.toList());
					if (activeAlarms.isEmpty())
						continue;
					final String recipient = alarmGroup.getKey();
					if ("".equals(recipient))  // since the alarm creation, the recipient has been deleted(?)
						continue;
					final int sz = activeAlarms.size();
					String devices = activeAlarms.stream().map(msg -> msg.subject).limit(subjLimit).collect(Collectors.joining(", "));
					if (sz > subjLimit)
						devices += ", ...";
					final String fullMessage = activeAlarms.stream().map(msg -> msg.message).collect(Collectors.joining("<br><br>"));
					service.newMessage()
						.withSender(activeAlarms.get(0).senderEmail, activeAlarms.get(0).senderName)
						.withSubject("Device issue reminder " + gwId + ": " + devices)
						.addHtml(fullMessage)
						.addTo(alarmGroup.getKey())  // the recipient
						.send();
					activeAlarms.stream().map(alarm -> alarm.resource).forEach(Resource::delete);  // remove PendingEmail resource
					success.incrementAndGet();
				} catch (Exception e) {
					appMan.getLogger().warn("Failed to send aggregated issue reminders to {}", alarmGroup.getKey(), e);
					failure.incrementAndGet();
				}
				
			}
			appMan.getLogger().info("Sent {} aggregated device alarm reminders successfully, {} failures. All recipients: {}", success.get(), failure.get(),
					alarmsByRecipient.keySet());
		} finally {
			ctx.ungetService(serviceRef);
		}
	}
	
	private static boolean hasPendingAlarm(AlarmGroupData issue) {
		final Resource pending = issue.getSubResource(AlarmResourceUtil.PENDING_REMINDER_EMAIL_SUBRESOURCE);
		return pending instanceof PendingEmail && pending.isActive();
	}

	private long untilNextExec() {
		final long now = appMan.getFrameworkTime();
		final ZonedDateTime nowZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault());
		ZonedDateTime nextExec;
		if (Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.devicealarmreminder.debug")) {  // truncate to full 5 minutes, for debugging only!
			final int debugMinutesInterval = Integer.getInteger("org.smartrplace.apps.alarmingconfig.devicealarmreminder.debug.aggregation.minutes", 5);
			nextExec = nowZdt.truncatedTo(ChronoUnit.MINUTES);
			final int minute = nextExec.getMinute();
	        final int remainder = minute % debugMinutesInterval;
	        if (remainder != 0)
	             nextExec = nextExec.withMinute(minute - remainder);
	        while (nextExec.compareTo(nowZdt) <= 0)
	        	nextExec = nextExec.plusMinutes(debugMinutesInterval);
		} else { 
			nextExec = nowZdt.with(LocalTime.of(7, 0));  // executes at 7am every day, hardcoded => ok?
			while (nextExec.compareTo(nowZdt) <= 0)
				nextExec = nextExec.plusDays(1);
		}
		final long millis = Duration.between(nowZdt, nextExec).toMillis();
		return millis;
	}

	@Override
	public void timerElapsed(final Timer timer) {
		timer.stop();
		try {
			send();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			if (Thread.interrupted()) {
				Thread.currentThread().interrupt();	
			} else {
				timer.setTimingInterval(untilNextExec());
				timer.resume();
			}
		}
	}
	
	private static class AlarmMessage {

		final PendingEmail resource;
		final String senderEmail;
		final String senderName;
		final String subject;
		final String message;
		
		public AlarmMessage(final PendingEmail resource, String senderEmail, String senderName, String subject, String message) {
			this.resource = resource;
			this.senderEmail = senderEmail;
			this.senderName = senderName;
			this.subject = subject;
			this.message = message;
		}
		
		public AlarmMessage(final PendingEmail resource) {
			this(resource, resource.senderEmail().getValue(), resource.senderName().getValue(), resource.subject().getValue(), resource.message().getValue());
		}
		
		public boolean isValid() {
			return message != null && !message.isEmpty() && subject != null && !subject.isEmpty() && senderEmail != null && senderEmail.indexOf('@') > 0;
		}
		
	}
	
	
	
}
