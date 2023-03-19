package org.smartrplace.driverhandler.devices;

import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.PowerResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.devices.storage.ElectricityStorage;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.ElectricCurrentSensor;
import org.ogema.model.sensors.EnergyAccumulatedSensor;
import org.ogema.model.sensors.PowerSensor;
import org.ogema.model.sensors.Sensor;
import org.ogema.simulation.shared.api.RoomInsideSimulationBase;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
import org.ogema.timeseries.eval.simple.mon3.MeteringEvalUtil;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
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
	public SingleValueResource getMainSensorValue(ElectricityStorage device, InstallAppDevice deviceConfiguration) {
		return device.electricityConnection().powerSensor().reading();
	}
	
	@Override
	protected void addMoreValueWidgets(InstallAppDevice object, ElectricityStorage device,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan, Alert alert) {
		if(!Boolean.getBoolean("org.smartrplace.driverhandler.devices.stationarybattery.suppressQ")) {
			PowerResource reactSens = device.electricityConnection().reactivePowerSensor().reading();
			Label valueLabel = vh.floatLabel("Q", id, reactSens, row, "%.1f");
			Label lastContact = addLastContact("Last Q", vh, id, req, row, reactSens);
			if(req != null) {
				valueLabel.setPollingInterval(DEFAULT_POLL_RATE, req);
				lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
			}
		}
		
		DeviceTableRaw.addTenantWidgetStatic(vh, id, req, row, appMan, device);
		
		vh.stringLabel("InternalName", id, device.getName(), row);

		if(req == null) {
			vh.registerHeaderEntry("Reading (kWh)");
			return;
		}
	}
	
	@Override
	protected Collection<Datapoint> getDatapoints(ElectricityStorage device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = ESE_ElConnBoxDeviceHandler.getDatapointsStatic(device.electricityConnection(), dpService);
		Datapoint powerDp = addDatapoint(device.electricityConnection().powerSensor().settings().setpoint(), result);
		addDatapoint(device.electricityConnection().reactivePowerSensor().settings().setpoint(), result);
		addDatapoint(device.electricityConnection().currentSensor().reading(), result);
		addDatapoint(device.electricityConnection().voltageSensor().reading(), result);
		addDatapoint(device.temperatureSensor().reading(), result);
		addDatapoint(device.chargeSensor().reading(), result);
		addDatapoint(device.chargeSensor().battery().internalVoltage().reading(), result);
		Datapoint dp = addDatapoint(device.getSubResource("sma_type", IntegerResource.class), result);
		if(dp != null)
			dp.addToSubRoomLocationAtomic(null, null, "smaType", false);
		dp = addDatapoint(device.getSubResource("initControl", IntegerResource.class), result);
		if(dp != null)
			dp.addToSubRoomLocationAtomic(null, null, "initControl", false);
		
		//Workaround for BDI devices
		Resource parent = device.getLocationResource().getParent();
		if((parent != null) && (parent instanceof PhysicalElement) && (parent.getName().equals("BDI"))) {
			addDatapoint(parent.getSubResource("bdiDcCurrent", ElectricCurrentSensor.class).reading(), result, "bdiDcCurrent", dpService);
			addDatapoint(parent.getSubResource("bdiDcPower", PowerSensor.class).reading(), result, "bdiDcPower", dpService);
			addDatapoint(parent.getSubResource("bdiExtDcCurrent", ElectricCurrentSensor.class).reading(), result, "bdiExtDcCurrent", dpService);
		}
		
		MeteringEvalUtil.addDailyMeteringEval(null, powerDp, device.electricityConnection(), result, appMan);

		return result;
	}

	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		ElectricityStorage device = (ElectricityStorage) appDevice.device();
		
		AlarmingUtiH.setTemplateValues(appDevice, device.electricityConnection().powerSensor().reading(),
				0f, 20000f, 1, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES);
	}

	@Override
	public String getInitVersion() {
		return "A";
	}
	
	@Override
	public List<RoomInsideSimulationBase> startSupportingLogicForDevice(InstallAppDevice device,
			ElectricityStorage deviceResource, SingleRoomSimulationBase roomSimulation, DatapointService dpService) {
		//Workaround for BDI devices
		Resource parent = deviceResource.getParent();
		if((parent != null) && (parent instanceof PhysicalElement) && (parent.getName().equals("BDI"))) {
			ElectricityConnection bdiOut = parent.getSubResource("bdiOutput", ElectricityConnection.class);
			createVirtualElConnBox(parent, bdiOut, "bdiOutputMeter");
			
			Resource peaTop = parent.getParent();
			if(peaTop != null) {
			
				PhysicalElement scc = peaTop.getSubResource("SCC1", PhysicalElement.class);
				createVirtualSensorDevice(peaTop, scc, "sensDevSCC1", false, true);
				scc = peaTop.getSubResource("SCC2", PhysicalElement.class);
				createVirtualSensorDevice(peaTop, scc, "sensDevSCC2", false, true);
				scc = peaTop.getSubResource("SCC3", PhysicalElement.class);
				createVirtualSensorDevice(peaTop, scc, "sensDevSCC3", false, true);
				scc = peaTop.getSubResource("SCC4", PhysicalElement.class);
				createVirtualSensorDevice(peaTop, scc, "sensDevSCC4", false, true);
				Room weatherRoom = peaTop.getSubResource("WeatherData", Room.class);
				createVirtualSensorDevice(peaTop, weatherRoom, "sensDevWeatherData", true, false);
	
				Resource totals = peaTop.getSubResource("totals", Resource.class);
				createVirtualSensorDevice(peaTop, totals, "sensDevTotals", false, false);
				
			}
		}
		return super.startSupportingLogicForDevice(device, deviceResource, roomSimulation, dpService);
	}
	
	@Override
	public String getTableTitle() {
		return "Stationary Batteries";
	}

	@Override
	protected Class<? extends ResourcePattern<ElectricityStorage>> getPatternClass() {
		return BatteryPattern.class;
	}

	@Override
	public ComType getComType() {
		return ComType.IP;
	}
	
	protected ElectricityConnectionBox createVirtualElConnBox(Resource bdiMain, ElectricityConnection conn, String name) {
		if(conn.isActive()) {
			ElectricityConnectionBox meter = bdiMain.getSubResource(name, ElectricityConnectionBox.class);
			meter.create();
			meter.connection().setAsReference(conn);
			meter.activate(true);
			return meter;
		}
		return null;
	}
	
	protected SensorDevice createVirtualSensorDevice(Resource bdiMain, Resource orgList, String name,
			boolean recursive, boolean chargerPv) {
		if(orgList.isActive()) {
			SensorDevice sensDev = bdiMain.getSubResource(name, SensorDevice.class);
			sensDev.create();
			sensDev.sensors().create();
			List<Sensor> senss = orgList.getSubResources(Sensor.class, recursive);
			for(Sensor el: senss) {
				sensDev.sensors().addDecorator(el.getName(), el);
			}
			if(chargerPv) {
				removeIfExitsting(sensDev.sensors(), "currentSensor");
				removeIfExitsting(sensDev.sensors(), "dailyEnergy");
				removeIfExitsting(sensDev.sensors(), "dailyEnergyAh");
				removeIfExitsting(sensDev.sensors(), "energySensor");
				removeIfExitsting(sensDev.sensors(), "energySensorAh");
				removeIfExitsting(sensDev.sensors(), "powerSensor");
				removeIfExitsting(sensDev.sensors(), "voltageSensor");
				senss = orgList.getSubResource("chargerConnection", ElectricityConnection.class).getSubResources(Sensor.class, false);
				for(Sensor el: senss) {
					sensDev.sensors().addDecorator("charger_"+el.getName(), el);
				}
				senss = orgList.getSubResource("pvConnection", ElectricityConnection.class).getSubResources(Sensor.class, false);
				for(Sensor el: senss) {
					sensDev.sensors().addDecorator("pv_"+el.getName(), el);
				}
			}
			sensDev.activate(true);
			return sensDev;
		}
		return null;		
	}
	
	private void removeIfExitsting(Resource orgList, String name) {
		Resource res = orgList.getSubResource(name);
		if((res != null) && res.isReference(false))
			res.delete();
	}
	
	@Override
	protected boolean setColumnTitlesToUse(ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh) {
		if(!Boolean.getBoolean("org.smartrplace.driverhandler.devices.residentialmetering1"))
			return super.setColumnTitlesToUse(vh);
		vh.registerHeaderEntry("InternalName");
		vh.registerHeaderEntry("ID");
		vh.registerHeaderEntry(getValueTitle());
		vh.registerHeaderEntry("Tenant");
		vh.registerHeaderEntry("Last Contact");
		vh.registerHeaderEntry("Location");
		vh.registerHeaderEntry("Status");
		vh.registerHeaderEntry("Comment");
		vh.registerHeaderEntry("Plot");
		return true;
	}
	
	@Override
	protected String getValueTitle() {
		if(!Boolean.getBoolean("org.smartrplace.driverhandler.devices.residentialmetering1"))
			return super.getValueTitle();
		return "Power (W)";
	}
}
