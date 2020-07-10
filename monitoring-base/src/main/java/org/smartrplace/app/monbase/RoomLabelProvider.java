package org.smartrplace.app.monbase;

import org.ogema.core.model.Resource;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.sp.smarteff.monitoring.alarming.AlarmingUtil;

import de.iwes.util.logconfig.LogHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.monitoring.AlarmConfigBase;

public interface RoomLabelProvider {
	String getRoomLabel(String resLocation, OgemaLocale locale);
	String getLabel(AlarmConfigBase ac, boolean isOverall);
	HardwareInstallConfig getHardwareConfig();
	String getTsName(AlarmConfigBase ac);
	
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

	public static String getTsNameDefault(AlarmConfigBase ac) {
		String baseName = ac.name().getValue();
		if(baseName.contains("-")) return baseName;
		String room = AlarmingUtil.getRoomNameFromSub(ac);
		return room+"-"+baseName;
	}
}
