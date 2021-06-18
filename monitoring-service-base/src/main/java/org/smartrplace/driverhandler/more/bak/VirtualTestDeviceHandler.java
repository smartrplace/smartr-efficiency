package org.smartrplace.driverhandler.more.bak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gateway.device.VirtualTestDevice;

import de.iwes.util.resource.ValueResourceHelper;

public class VirtualTestDeviceHandler extends DeviceHandlerSimple<VirtualTestDevice> {

	public VirtualTestDeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<VirtualTestDevice> getResourceType() {
		return VirtualTestDevice.class;
	}

	@Override
	protected SingleValueResource getMainSensorValue(VirtualTestDevice device, InstallAppDevice deviceConfiguration) {
		return device.sensor_SF().reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(VirtualTestDevice device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), "SF", result);
		addDatapoint(device.sensor_CF().reading(), "CF", result);
		addDatapoint(device.sensor_BT().reading(), "BT", result);
		return result;
	}

	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		VirtualTestDevice device = (VirtualTestDevice) appDevice.device();
		AlarmConfiguration ac = AlarmingUtiH.setTemplateValues(appDevice, device.sensor_SF().reading(), 0, 100, 0.1f, -1);
		ValueResourceHelper.setCreate(ac.alarmRepetitionTime(), 0.5f);
		ValueResourceHelper.setCreate(ac.alarmingAppId(), AlarmingUtiH.SP_SUPPORT_FIRST);
		ac = AlarmingUtiH.setTemplateValues(appDevice, device.sensor_CF().reading(), 0, 100, 0.1f, -1);
		ValueResourceHelper.setCreate(ac.alarmRepetitionTime(), 0.5f);
		ValueResourceHelper.setCreate(ac.alarmingAppId(), AlarmingUtiH.CUSTOMER_FIRST);
		ac = AlarmingUtiH.setTemplateValues(appDevice, device.sensor_BT().reading(), 0, 100, 0.1f, -1);
		ValueResourceHelper.setCreate(ac.alarmRepetitionTime(), 0.5f);
		ValueResourceHelper.setCreate(ac.alarmingAppId(), AlarmingUtiH.CUSTOMER_SP_SAME);
	}
	
	@Override
	public String getTableTitle() {
		return "Virtual Test Devices for Alarming";
	}

	@Override
	protected Class<? extends ResourcePattern<VirtualTestDevice>> getPatternClass() {
		return VirtualTestDevicePattern.class;
	}

	
}
