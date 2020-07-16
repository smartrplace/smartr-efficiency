package org.smatrplace.apps.alarmconfig.util;

import org.ogema.core.model.Resource;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmingconfig.mgmt.AlarmingUtiH;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.logconfig.LogHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public interface RoomLabelProvider {
	String getRoomLabel(String resLocation, OgemaLocale locale);
	String getLabel(AlarmConfiguration ac, boolean isOverall);
	HardwareInstallConfig getHardwareConfig();
	String getTsName(AlarmConfiguration ac);
	
	public static String getDatapointShortLabelDefault(Resource res, boolean bareName, RoomLabelProvider roomLabelProv) {
		PhysicalElement device = LogHelper.getDeviceResource(res, true);
		if(device == null)
			return null;
		String deviceRoomName = ResourceUtils.getHumanReadableName(device.location().room());
		if(!bareName) {
			InstallAppDevice iad = getDeviceHwInfo(device, roomLabelProv);
			if(iad != null && iad.installationLocation().exists())
				deviceRoomName += "-"+iad.installationLocation().getValue();
		}
		return deviceRoomName;
	}

	public static InstallAppDevice getDeviceHwInfo(Resource resInDevice, RoomLabelProvider roomLabelProv) {
		PhysicalElement device = LogHelper.getDeviceResource(resInDevice, true);
		if(device == null)
			return null;
		for(InstallAppDevice knownDev: roomLabelProv.getHardwareConfig().knownDevices().getAllElements()) {
			if(knownDev.device().equalsLocation(device))
				return knownDev;
		}
		return null;
	}

	public static String getTsNameDefault(AlarmConfiguration ac) {
		String baseName = ac.name().getValue();
		if(baseName.contains("-")) return baseName;
		String room = AlarmingUtiH.getRoomNameFromSub(ac);
		return room+"-"+baseName;
	}
}
