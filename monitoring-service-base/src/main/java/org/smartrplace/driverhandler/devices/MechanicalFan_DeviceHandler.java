package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.devices.buildingtechnology.MechanicalFan;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class MechanicalFan_DeviceHandler extends DeviceHandlerSimple<MechanicalFan> {

	public MechanicalFan_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<MechanicalFan> getResourceType() {
		return MechanicalFan.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(MechanicalFan device, InstallAppDevice deviceConfiguration) {
		return device.onOffSwitch().stateFeedback();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(MechanicalFan device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		addDatapoint(device.onOffSwitch().stateControl(), result);
		addDatapoint(device.setting().stateFeedback(), result);
		addDatapoint(device.setting().stateControl(), result);
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Mechanical Fans BACnet";
	}

	@Override
	protected Class<? extends ResourcePattern<MechanicalFan>> getPatternClass() {
		return MechanicalFan_Pattern.class;
	}

	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "HTMB";
	}
}
