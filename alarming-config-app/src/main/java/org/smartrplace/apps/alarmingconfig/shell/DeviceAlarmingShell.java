package org.smartrplace.apps.alarmingconfig.shell;

import java.io.PrintStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.smartrplace.apps.alarmconfig.reminder.AlarmReminderPattern;
import org.smartrplace.apps.alarmconfig.reminder.DeviceAlarmReminderService;
import org.smartrplace.apps.alarmconfig.reminder.DeviceAlarmReminderService.AlarmConfig;
import org.smartrplace.apps.alarmconfig.util.AlarmResourceUtil;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmGroupDataMajor;
import org.ogema.tools.resource.util.ResourceUtils;

@Component(
		service=DeviceAlarmingShell.class,
		property= {
				"osgi.command.scope=devicealarms",
				"osgi.command.function=devices",
				"osgi.command.function=deviceIssues",
				"osgi.command.function=reminders"
		}
)
public class DeviceAlarmingShell {
	
	private final CountDownLatch startLatch = new CountDownLatch(1);
	private volatile ServiceRegistration<Application> appService;
	private ApplicationManager appMan;
	

	@Activate
	protected void activate(BundleContext ctx) {
		final Application app = new Application() {

			@Override
			public void start(ApplicationManager appManager) {
				appMan = appManager;
				startLatch.countDown();
			}

			@Override
			public void stop(AppStopReason reason) {
				appMan = null;
			}
			
		};
		this.appService = ctx.registerService(Application.class, app, null);
	}
	
	@Deactivate 
	protected void deactivate() {
		final ServiceRegistration<Application> appService = this.appService;
		if (appService != null) {
			try {
				appService.unregister();
			} catch (Exception ignore) {}
			this.appService = null;
			this.appMan = null;
		} 
				
	}
	
	private Stream<InstallAppDevice> getKnownDevices() {
		return appMan.getResourceAccess().getResources(HardwareInstallConfig.class).stream()
				.flatMap(cfg -> cfg.knownDevices().getAllElements().stream());
	}
	
	@Descriptor("Show devices")
	public void devices(CommandSession shell,
			@Descriptor("Include trash")
			@Parameter(names= {"-t", "--trash"}, absentValue="false", presentValue="true")
			boolean trashIncluded,
			@Descriptor("Include device resource path")
			@Parameter(names= {"-d", "--device"}, absentValue="false", presentValue="true")
			boolean showDevicePath,
			@Descriptor("Include known device resource path")
			@Parameter(names= {"-kd", "--known-device"}, absentValue="false", presentValue="true")
			boolean showKnownDevicePath
			) throws InterruptedException {
		startLatch.await(30, TimeUnit.SECONDS);
		final PrintStream console = shell.getConsole();
		Stream<InstallAppDevice> stream = appMan.getResourceAccess().getResources(InstallAppDevice.class).stream();
		if (!trashIncluded)
			stream = stream.filter(d -> !d.isTrash().isActive() || !d.isTrash().getValue());
		stream.forEach(d -> {
			final StringBuilder sb = new StringBuilder();
			if (d.deviceId().isActive())
				sb.append("Device id: ").append(d.deviceId().getValue()).append(", ");
			if (d.device().isActive()) {
				final String name = ResourceUtils.getHumanReadableName(d.device());
				final String loc = d.device().getLocation();
				if (!loc.equals(name))
					sb.append("device: ").append(name).append(", ");
				if (showDevicePath)
					sb.append("device resource: ").append(loc).append(", ");
				if (d.device().location().room().isActive())
					sb.append("room: ").append(ResourceUtils.getHumanReadableName(d.device().location().room())).append(", ");
			}
			if (showKnownDevicePath)
				sb.append("resource: ").append(d.getLocation()).append(", ");
			sb.append("device in fault state: ").append(d.knownFault().isActive());
			console.println(sb.toString());
		});		
	}
	
	// TODO filter by building, room, device type, ...
	@Descriptor("Show device issues")
	public void deviceIssues(CommandSession shell,
			@Descriptor("Long format; show everything")
			@Parameter(names= {"-l", "--long"}, absentValue="false", presentValue="true")
			boolean longFormat,
			@Descriptor("Include device resource path")
			@Parameter(names= {"-d", "--device"}, absentValue="false", presentValue="true")
			boolean showDevicePath,
			@Descriptor("Include alarm resource path")
			@Parameter(names= {"-a", "--alarm"}, absentValue="false", presentValue="true")
			boolean showAlarmPath,
			@Descriptor("Include assignments")
			@Parameter(names= {"-ass", "--assigned"}, absentValue="false", presentValue="true")
			boolean showAssignment,
			@Descriptor("Include room")
			@Parameter(names= {"-rm", "--room"}, absentValue="false", presentValue="true")
			boolean showRoom,
			@Descriptor("Show only major issues")
			@Parameter(names= {"-maj", "--major"}, absentValue="false", presentValue="true")
			boolean majorOnly,
			@Descriptor("Show only minor issues")
			@Parameter(names= {"-min", "--minor"}, absentValue="false", presentValue="true")
			boolean minorOnly,
			@Descriptor("Show only released issues")
			@Parameter(names= {"-r", "--release"}, absentValue="false", presentValue="true")
			boolean releaseOnly,
			@Descriptor("Exclude released issues")
			@Parameter(names= {"-nr", "--no-release"}, absentValue="false", presentValue="true")
			boolean noRelease
			) throws InterruptedException  {
		if (minorOnly && majorOnly)
			throw new IllegalArgumentException("--minor and --major parameters are mutually exclusive");
		if (releaseOnly && noRelease)
			throw new IllegalArgumentException("--release and --no-release parameters are mutually exclusive");
		if (longFormat) {
			showDevicePath = true;
			showAssignment = true;
			showRoom = true;
		}
		startLatch.await(30, TimeUnit.SECONDS);
		final PrintStream console = shell.getConsole();
		final Class<? extends AlarmGroupData> type = majorOnly ? AlarmGroupDataMajor.class : AlarmGroupData.class;
		@SuppressWarnings("unchecked")
		Stream<AlarmGroupData> stream = (Stream<AlarmGroupData>) appMan.getResourceAccess().getResources(type).stream();
		if (minorOnly)
			stream = stream.filter(a -> !(a instanceof AlarmGroupDataMajor));
		if (releaseOnly)
			stream = stream.filter(AlarmResourceUtil::isReleased);
		if (noRelease)
			stream = stream.filter(a -> !AlarmResourceUtil.isReleased(a));
		Stream<AlarmData> alarmStream = stream
			.map(a -> new AlarmData(a));
		final boolean showDevicePath1 = showDevicePath;
		final boolean showRoom1 = showRoom;
		final boolean showAssignment1 = showAssignment;
		alarmStream.forEach(a -> console.println(a.toString(false, showAlarmPath, showDevicePath1, showRoom1, showAssignment1)));
	}
	
	// TODO filter for released alarms? And trashed/inactive devices?
	@Descriptor("Show pending alarm reminders")
	public void reminders(
			CommandSession shell,
			@Descriptor("Time horizon for alarms to include, such as '1h', '1d', '1w', '1mo', '1y'. Default: 1 month.")
			@Parameter(names= {"-h", "--horizon"}, absentValue="1mo")
			String horizon,
			@Descriptor("Display provisional reminder email content")
			@Parameter(names= {"-e", "--email"}, absentValue="false", presentValue="true")
			boolean email
			) throws InterruptedException {
		startLatch.await(30, TimeUnit.SECONDS);
		final long now = appMan.getFrameworkTime();
		Stream<AlarmReminderPattern> stream = appMan.getResourcePatternAccess()
				.getPatterns(AlarmReminderPattern.class, AccessPriority.PRIO_LOWEST).stream()
				.filter(AlarmReminderPattern::isActive)
				.filter(alarm -> alarm.dueDate.getValue() >= now && alarm.dueDate.getValue() < now + parseHorizon(horizon));
		final PrintStream out = shell.getConsole();
		if (email) {
			stream
				.map(AlarmConfig::new)
				.sorted()
				.map(alarm -> DeviceAlarmReminderService.extractEmailData(alarm, appMan, null, false))
				.forEach(emailData -> {
					out.println("Device " + emailData.deviceId + " (" + formatTime(emailData.due) + ")");
					out.println("  Recipient: " + emailData.recipient);
					out.println("  Subject  : " + emailData.subject);
					out.println("  Message  : " + emailData.msg);
				});
		} else {
			stream.forEach(alarm -> {
				final InstallAppDevice device = AlarmResourceUtil.getDeviceForKnownFault(alarm.model);
				final String dueDate = formatTime(alarm.dueDate.getValue());
				final String deviceId = device == null ? "" : device.deviceId().isActive() ? device.deviceId().getValue() : 
					device.device().isActive() ? device.device().getLocation() : device.getLocation();
				final StringBuilder sb= new StringBuilder();
				sb.append("Reminder[device ").append(deviceId).append(", due ").append(dueDate);
				if (alarm.model.reminderType().isActive()) {
					final int reminder = alarm.model.reminderType().getValue();
					sb.append(" (").append(reminder == 1 ? "daily" : reminder == 2 ? "weekly" : reminder == 3 ? "monthly" : "default frequency").append(")");
				}
				if (alarm.responsible.isActive())
					sb.append(", responsible: ").append(alarm.responsible.getValue());
				sb.append(", alarm resource: ").append(alarm.model.getLocation());
				sb.append("]");
				out.println(sb.toString());
			});
		}
	}
	
	private static String formatTime(long t) {
		return DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(t), ZoneId.of("Z")));
	}
	
	private static long parseHorizon(String s) {
		s = s.trim();
		if (s.isEmpty())
			throw new IllegalArgumentException("Invalid horizon");
		final StringBuilder numeric = new StringBuilder();
		for (int idx=0; idx<s.length(); idx++) {
			final char c = s.charAt(idx);
			if (!Character.isDigit(c))
				break;
			numeric.append(c);
		}
		final int number = numeric.length() > 0 ? Integer.parseInt(numeric.toString()) : 1;
		final String unit = numeric.length() == s.length() ? "d" : s.substring(numeric.length());
		return number * parseHorizonUnit(unit);
	}

	private static long parseHorizonUnit(String s) {  // fallthroughs deliberate
		long h = 1;  // 1 ms
		boolean dateSet = false;
		switch (s.toLowerCase()) {
		case "y":
		case "a": 
			h = h * 365;
			dateSet = true;
		case "mo":
			if (!dateSet) { 
				h = h*30;
				dateSet = true;
			}
		case "w":
			if (!dateSet) { 
				h = h*7;
				dateSet = true;
			}
		case "d":
			h = h * 24;
		case "h":
			h = h * 60;
		case "m":
			h = h * 60;
		case "s":
			h = h * 1000;
			break;
		default:
			throw new IllegalArgumentException("Unknown time horizon " + s);
		}
		return h;
	}
	

}
