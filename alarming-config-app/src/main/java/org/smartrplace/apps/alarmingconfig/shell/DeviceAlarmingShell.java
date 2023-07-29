package org.smartrplace.apps.alarmingconfig.shell;

import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.smartrplace.apps.alarmconfig.util.AlarmResourceUtil;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmGroupDataMajor;
import org.ogema.tools.resource.util.ResourceUtils;

@Component(
		service=DeviceAlarmingShell.class,
		property= {
				"osgi.command.scope=devicealarms",
				"osgi.command.function=devices",
				"osgi.command.function=deviceIssues"
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
			@Parameter(names= {"--alarm"}, absentValue="false", presentValue="true")
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
	

}
