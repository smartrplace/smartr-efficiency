package org.smartrplace.apps.alarmingconfig.shell;

import java.util.concurrent.Callable;
import java.util.function.Function;

import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmGroupDataMajor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmconfig.util.AlarmResourceUtil;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class AlarmData {

	private final AlarmGroupData alarmData;
	private final InstallAppDevice device;
	private final String deviceId;
	private final String deviceName;
	private final String deviceResource;
	
	public AlarmData(AlarmGroupData alarmData) {
		this(alarmData, AlarmResourceUtil.getDeviceForKnownFault(alarmData));
	}
	
	public AlarmData(AlarmGroupData alarmData, InstallAppDevice device) {
		this.alarmData = alarmData;
		this.device = device;
		if (device != null && device.deviceId().isActive()) // FIXME should we involve the device handler here?
			this.deviceId = device.deviceId().getValue();
		else
			this.deviceId = null;
		if (device != null) {
			this.deviceName = ResourceUtils.getHumanReadableName(device);
			this.deviceResource = device.getLocation();
		} else {
			this.deviceName = null;
			this.deviceResource = null;
		}
	}
	
	public String toString(
			boolean details, 
			boolean alarmResourcePath, 
			boolean deviceResourcePath,
			boolean showRoom,
			boolean showAssignment
			) {
		final StringBuilder sb = new StringBuilder();
		final Callable<Void> separator = () -> {
			if (details)
				 sb.append("\n");
			 else
				 sb.append(", ");
			return null;
		};
		final int maxLength = 15;
		final Function<String, StringBuilder> header = (String h) -> {
			sb.append(h).append(": ");
			if (details) {
				 final int diff = maxLength - h.length();
				 if (diff > 0)
					 sb.append(diff);
			}
			return sb;
		};
		try {
			if (device != null) {
				 header.apply("Device").append(deviceId);
				 if (alarmResourcePath) {
					 separator.call();
					 header.apply("Alarm resource").append(alarmData.getLocation());
				 }
				 if (deviceResourcePath) {
					 separator.call();
					 header.apply("Device resource").append(deviceResource);
				 }
				 if (showRoom && device.device().location().room().isActive()) {
					 separator.call();
					 header.apply("Room").append(ResourceUtils.getHumanReadableName(device.device().location().room()));
				 }
			} else {
				header.apply("Alarm").append(alarmData.getLocation());
			}
			if (alarmData instanceof AlarmGroupDataMajor) {
				separator.call();
				final boolean released = AlarmResourceUtil.isReleased(alarmData);
				sb.append("Major issue, status: ").append(released ? "released" : "open");
			}
			if (showAssignment && alarmData.assigned().isActive()) {
				separator.call();
				header.apply("Assignment").append(AlarmingConfigUtil.ASSIGNEMENT_ROLES.get(alarmData.assigned().getValue() + ""));
			}
		} catch (Exception e) {
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException(e);
		}
		return sb.toString();
	}
	
	
	
	
}
