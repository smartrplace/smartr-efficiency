package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.devices.buildingtechnology.ElectricLight;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class LightWLANDevHandler extends DeviceHandlerSimple<ElectricLight> {

	public LightWLANDevHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<ElectricLight> getResourceType() {
		return ElectricLight.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(ElectricLight device, InstallAppDevice deviceConfiguration) {
		return device.onOffSwitch().stateFeedback();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(ElectricLight device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		addDatapoint(device.onOffSwitch().stateControl(), result, dpService);
		addDatapoint(device.getSubResource("b", IntegerResource.class), result, dpService);
		addDatapoint(device.getSubResource("g", IntegerResource.class), result, dpService);
		addDatapoint(device.getSubResource("r", IntegerResource.class), result, dpService);
		addDatapoint(device.getSubResource("c", IntegerResource.class), result, dpService); //cold white
		addDatapoint(device.getSubResource("w", IntegerResource.class), result, dpService); //warm white
		addDatapoint(device.dimmer().onOffSwitch().stateControl(), result, dpService);
		addDatapoint(device.dimmer().setting().stateControl(), result, dpService);
		addDatapoint(device.dimmer().setting().stateFeedback(), result, dpService);

		return result;
	}

	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		ElectricLight device = (ElectricLight) appDevice.device();
		
		AlarmingUtiH.setTemplateValues(appDevice, device.onOffSwitch().stateFeedback(),
				0f, 1f, 1, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES, 2880);
	}
	
	@Override
	public String getInitVersion() {
		return "A";
	}
	
	@Override
	public String getTableTitle() {
		return "Electric Lights";
	}

	@Override
	protected Class<? extends ResourcePattern<ElectricLight>> getPatternClass() {
		return LightWLANPattern.class;
	}
}
