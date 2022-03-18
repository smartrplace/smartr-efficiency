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
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.actors.Actor;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.resource.ValueResourceHelper;

public class FaultMessageDeviceHandler extends DeviceHandlerSimple<SensorDevice> {

	private static final long MIN_UPDATE_TIME = 60000;

	public FaultMessageDeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, false);
	}

	@Override
	public Class<SensorDevice> getResourceType() {
		return SensorDevice.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(SensorDevice device, InstallAppDevice deviceConfiguration) {
		return updateFaultCounter(device, appMan.getFrameworkTime());
	}

	@Override
	public String getTableTitle() {
		return "Fault Message Devices";
	}

	@Override
	protected Collection<Datapoint> getDatapoints(SensorDevice device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		SingleValueResource main = getMainSensorValue(device, deviceConfiguration);
		addDatapoint(main, result);
		//List<OnOffSwitch> onOffs = device.getSubResources(OnOffSwitch.class, false);
		//for(OnOffSwitch onoff: onOffs) {
		//	addDatapoint(onoff.stateFeedback(), result);			
		//}
		List<Actor> actors = device.getLocationResource().getSubResources(Actor.class, false);
		for(Actor act: actors) {
			if(act instanceof OnOffSwitch) {
				addDatapoint(((OnOffSwitch)act).stateFeedback(), result);							
			} else if(act.stateFeedback() instanceof IntegerResource)
				addDatapoint((IntegerResource)act.stateFeedback(), result);			
			else if(act.stateFeedback() instanceof BooleanResource)
				addDatapoint((BooleanResource)act.stateFeedback(), result);			
		}
		return result;
	}

	@Override
	protected Class<? extends ResourcePattern<SensorDevice>> getPatternClass() {
		return FaultMessageDevicePattern.class;
	}

	public static IntegerResource updateFaultCounter(SensorDevice device, long now) {
		IntegerResource faultCounter = device.getSubResource("faultCounter", IntegerResource.class);
		if(now - faultCounter.getLastUpdateTime() < MIN_UPDATE_TIME) {
			return faultCounter;
		}
		int count = 0;
		List<Actor> actors = device.getSubResources(Actor.class, false);
		for(Actor act: actors) {
			if(act instanceof OnOffSwitch) {
				if(((OnOffSwitch)act).stateFeedback().getValue())
					count++;
			} else if(act.stateFeedback() instanceof IntegerResource)
				if(((IntegerResource)act.stateFeedback()).getValue() > 0)
					count++;
			else if(act.stateFeedback() instanceof BooleanResource)
				if(((BooleanResource)act.stateFeedback()).getValue())
					count++;
		}
		ValueResourceHelper.setCreate(faultCounter, count);
		return faultCounter;
	}
}
