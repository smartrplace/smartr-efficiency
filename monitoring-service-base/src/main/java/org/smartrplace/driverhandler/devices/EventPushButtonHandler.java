package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.actors.EventPushButton;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

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
		BooleanResource online = device.online();
		if(online.isActive())
			return online;
		TimeResource cnt = device.getSubResource("event_cnt", TimeResource.class);
		if(cnt.isActive())
			return cnt;
		return device.event().reading();
	}

	@Override
	protected void addMoreValueWidgets(InstallAppDevice object, EventPushButton device,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan, Alert alert) {
		if(req == null) {
			vh.registerHeaderEntry("Cnt");
			return;
		}
		TimeResource cnt = device.getSubResource("event_cnt", TimeResource.class);
		if(cnt.isActive())
			vh.timeLabel("Cnt", id, cnt, row, 7);
		else
			vh.floatLabel("Cnt", id, device.event().reading(), row, "%.0f");	
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
	
	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		EventPushButton device = (EventPushButton) appDevice.device();
		AlarmingUtiH.setTemplateValues(appDevice, device.online(),
				0.5f, 1.5f, 0.1f, 360);
	}
	
	@Override
	public String getInitVersion() {
		return "A";
	}
}
