package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.PowerResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.devices.storage.ElectricityStorage;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class BatteryDevHandler extends DeviceHandlerSimple<ElectricityStorage> {
	
	public BatteryDevHandler(ApplicationManagerPlus appMan) {
		super(appMan, false);
	}
	
	@Override
	public Class<ElectricityStorage> getResourceType() {
		return ElectricityStorage.class;
	}
	
	@Override
	protected SingleValueResource getMainSensorValue(ElectricityStorage device, InstallAppDevice deviceConfiguration) {
		return device.electricityConnection().powerSensor().reading();
	}
	
	@Override
	protected void addMoreValueWidgets(InstallAppDevice object, ElectricityStorage device,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		PowerResource reactSens = device.electricityConnection().reactivePowerSensor().reading();
		Label valueLabel = vh.floatLabel("Q", id, reactSens, row, "%.1f");
		Label lastContact = addLastContact("Last Q", vh, id, req, row, reactSens);
		if(req != null) {
			valueLabel.setPollingInterval(DEFAULT_POLL_RATE, req);
			lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
		}
	}
	
	@Override
	protected Collection<Datapoint> getDatapoints(ElectricityStorage device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = ESE_ElConnBoxDeviceHandler.getDatapointsStatic(device.electricityConnection(), dpService);
		addDatapoint(device.electricityConnection().powerSensor().settings().setpoint(), result);
		addDatapoint(device.electricityConnection().reactivePowerSensor().settings().setpoint(), result);
		addDatapoint(device.chargeSensor().reading(), result);
		Datapoint dp = addDatapoint(device.getSubResource("sma_type", IntegerResource.class), result);
		if(dp != null)
			dp.addToSubRoomLocationAtomic(null, null, "smaType", false);
		dp = addDatapoint(device.getSubResource("initControl", IntegerResource.class), result);
		if(dp != null)
			dp.addToSubRoomLocationAtomic(null, null, "initControl", false);
		return result;
	}

	@Override
	protected String getTableTitle() {
		return "Stationary Batteries";
	}

	@Override
	protected Class<? extends ResourcePattern<ElectricityStorage>> getPatternClass() {
		return BatteryPattern.class;
	}

}
