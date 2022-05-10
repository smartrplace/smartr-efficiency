package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.actors.EventPushButton;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class EventPushButtonHandler extends DeviceHandlerSimple<EventPushButton> {

	public EventPushButtonHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<EventPushButton> getResourceType() {
		return EventPushButton.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(EventPushButton device, InstallAppDevice deviceConfiguration) {
		TimeResource cnt = device.getSubResource("event_cnt", TimeResource.class);
		if(cnt.isActive())
			return cnt;
		return device.event().reading();
	}

	@Override
	public String getTableTitle() {
		return "Event Buttons";
	}

	@Override
	protected Collection<Datapoint> getDatapoints(EventPushButton device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(device.event().reading(), "event", result);
		addDatapoint(device.getSubResource("event_cnt", TimeResource.class), "event_cnt", result);
		addDatapoint(device.battery().chargeSensor().reading(), result);
		addDatapoint(device.online(), "online", result);
		addDatapoint(device.getSubResource("disableLedStatus", BooleanResource.class), "disableLedStatus", result);
		return result;
	}

	@Override
	protected Class<? extends ResourcePattern<EventPushButton>> getPatternClass() {
		return EventPushButtonPattern.class;
	}
}
