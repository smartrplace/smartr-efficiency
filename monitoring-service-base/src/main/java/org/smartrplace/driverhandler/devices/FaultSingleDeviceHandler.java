package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.actors.Actor;
import org.ogema.model.actors.OnOffSwitch;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class FaultSingleDeviceHandler extends DeviceHandlerSimple<Actor> {

	public FaultSingleDeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, false);
	}

	@Override
	public Class<Actor> getResourceType() {
		return Actor.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(Actor act, InstallAppDevice deviceConfiguration) {
		if(act instanceof OnOffSwitch) {
			return ((OnOffSwitch)act).stateFeedback();
		} else if(act.stateFeedback() instanceof IntegerResource)
			return (IntegerResource)act.stateFeedback();
		else if(act.stateFeedback() instanceof BooleanResource)
			return (BooleanResource)act.stateFeedback();
		else if(act.stateFeedback() instanceof SingleValueResource)
			return (SingleValueResource)act.stateFeedback();
		else
			return null;
	}

	@Override
	public String getTableTitle() {
		return "Fault Single Devices";
	}

	@Override
	protected Collection<Datapoint> getDatapoints(Actor act, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		SingleValueResource main = getMainSensorValue(act, deviceConfiguration);
		addDatapoint(main, result);

		//List<Actor> actors = device.sensors().getSubResources(Actor.class, false);
		return result;
	}

	@Override
	public Datapoint addDatapoint(SingleValueResource res, List<Datapoint> result) {
		return addDatapointWithResOrSensorName(res, result);
	}
	
	@Override
	protected Class<? extends ResourcePattern<Actor>> getPatternClass() {
		return FaultSingleDevicePattern.class;
	}

	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		Actor act = (Actor) appDevice.device();
		if(act instanceof OnOffSwitch) {
			AlarmingUtiH.setTemplateValues(appDevice, ((OnOffSwitch)act).stateFeedback(),
					0f, 0.5f, 0.1f, -1);
		} else if(act.stateFeedback() instanceof IntegerResource)
			AlarmingUtiH.setTemplateValues(appDevice, (IntegerResource)act.stateFeedback(),
					0f, FaultMessageDeviceHandler.MAX_NONALARM_VALUE, 0.1f, -1);
		else if(act.stateFeedback() instanceof BooleanResource)
			AlarmingUtiH.setTemplateValues(appDevice, (BooleanResource)act.stateFeedback(),
					0f, 0.5f, 0.1f, -1);
	}
	
	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "FAUT";
	}
}
