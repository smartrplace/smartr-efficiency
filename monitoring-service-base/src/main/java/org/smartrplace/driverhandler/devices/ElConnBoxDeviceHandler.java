package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.EnergyResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH.DestType;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.sensors.ElectricEnergySensor;
import org.ogema.recordeddata.RecordedDataStorage;
import org.ogema.timeseries.eval.simple.mon3.MeteringEvalUtil;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class ElConnBoxDeviceHandler extends DeviceHandlerSimple<ElectricityConnectionBox> {
	protected final ApplicationManagerPlus appMan;
	
	//TODO: We have to close the listeners when the calling bundle closes
	public static class EnergyAccumulationDpDataBase {
		public ResourceValueListener<EnergyResource> aggListener;
		public Datapoint evalDp;		
		public Datapoint resourceDp;
	}
	public static class EnergyAccumulationDpData extends EnergyAccumulationDpDataBase {
		public RecordedDataStorage recStor;
		
	}
	
	public ElConnBoxDeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
		this.appMan = appMan;
	}
	
	@Override
	public Class<ElectricityConnectionBox> getResourceType() {
		return ElectricityConnectionBox.class;
	}

	@Override
	protected Class<? extends ResourcePattern<ElectricityConnectionBox>> getPatternClass() {
		return ElConnBoxPattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(ElectricityConnectionBox dev,
			InstallAppDevice deviceConfiguration) {
		return getDatapointsStatic(dev.connection(), dpService, appMan);
	}
	/*@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService) {
		ElectricityConnectionBox dev = (ElectricityConnectionBox) installDeviceRes.device();
		return getDatapointsStatic(dev.connection(), dpService);
	}*/
	
	/** Search for datapoints in an {@link ElectricityConnection}
	 * 
	 * @param connection
	 * @param dpService
	 * @param util set this to null if a special resource energyDaily shall NOT be summed up into a real
	 * 		metering resource (see {@link #provideAccumulatedDatapoint(String, EnergyResource, Datapoint, List, ElectricityConnection, DatapointService, EnergyAccumulationData)}).
	 * 		Not relevant if such a special decorator sub resource does not exist.
	 * @return
	 */
	public static List<Datapoint> getDatapointsStatic(ElectricityConnection connection, DatapointService dpService,
			ApplicationManagerPlus appMan) {
		List<Datapoint> result = new ArrayList<>();
		addConnDatapoints(result, connection, dpService, appMan);
		for(ElectricityConnection subConn: connection.subPhaseConnections().getAllElements()) {
			addConnDatapoints(result, subConn, subConn.getName(), dpService);			
		}
		//TODO: Workaround
		for(int i=1; i<=3; i++) {
			ElectricityConnection subConn = connection.getSubResource("L"+i, ElectricityConnection.class);
			addConnDatapoints(result, subConn, subConn.getName(), dpService);			
		}
		
		return result;
	}
	
	protected static void addConnDatapoints(List<Datapoint> result, ElectricityConnection conn,
			DatapointService dpService, ApplicationManagerPlus appMan) {
		//addDatapoint(conn.voltageSensor().reading(), result, dpService);
		Datapoint powerDp = addDatapoint(conn.powerSensor().reading(), result, dpService);
		Datapoint energyDp = addDatapoint(conn.energySensor().reading(), result, dpService);
		EnergyResource energyDaily = conn.getSubResource("energyDaily", ElectricEnergySensor.class).reading();
		addDatapoint(energyDaily, result, dpService);
		addDatapoint(conn.getSubResource("energyAccumulatedDaily", ElectricEnergySensor.class).reading(), result, dpService);
		addDatapoint(conn.getSubResource("energyReactiveAccumulatedDaily", ElectricEnergySensor.class).reading(), result, dpService);
		addDatapoint(conn.getSubResource("billedEnergy", ElectricEnergySensor.class).reading(), result, dpService);
		addDatapoint(conn.getSubResource("billedEnergyReactive", ElectricEnergySensor.class).reading(), result, dpService);
		addDatapoint(conn.currentSensor().reading(), result, dpService);
		addDatapoint(conn.voltageSensor().reading(), result, dpService);
		addDatapoint(conn.frequencySensor().reading(), result, dpService);		
		addDatapoint(conn.reactivePowerSensor().reading(), result, dpService);
		addDatapoint(conn.reactiveAngleSensor().reading(), result, dpService);
		
		MeteringEvalUtil.addDailyMeteringEval(energyDp, powerDp, conn, result, appMan);
	}
	
	protected static void addConnDatapoints(List<Datapoint> result, ElectricityConnection conn,
			String ph, DatapointService dpService) {
		addDatapoint(conn.powerSensor().reading(), result, ph, dpService);
		addDatapoint(conn.energySensor().reading(), result, ph, dpService);
		addDatapoint(conn.currentSensor().reading(), result, ph, dpService);
		addDatapoint(conn.voltageSensor().reading(), result, ph, dpService);
		addDatapoint(conn.frequencySensor().reading(), result, ph, dpService);		
		addDatapoint(conn.reactivePowerSensor().reading(), result, ph, dpService);
		addDatapoint(conn.reactiveAngleSensor().reading(), result, ph, dpService);			
	}
	
	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		ElectricityConnectionBox device = (ElectricityConnectionBox) appDevice.device();
		AlarmingUtiH.setTemplateValues(appDevice, device.connection().powerSensor().reading(),
				0.0f, 9999999.0f, 30, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES, 2880, DestType.CUSTOMER_SP_SAME, 2);
		//AlarmingUtiH.addAlarmingMQTT(device, appDevice);
	}

	@Override
	public String getInitVersion() {
		return "C";
	}
	
	@Override
	public String getTableTitle() {
		return "Standard Electricity Meters";
	}
	
	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "ECB";
	}
	
	@Override
	public ComType getComType() {
		return ComType.IP;
	}

	@Override
	public SingleValueResource getMainSensorValue(ElectricityConnectionBox device,
			InstallAppDevice deviceConfiguration) {
		return device.connection().powerSensor().reading();
	}

	@Override
	protected void addMoreValueWidgets(InstallAppDevice object, ElectricityConnectionBox device,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan, Alert alert) {

		DeviceTableRaw.addTenantWidgetStatic(vh, id, req, row, appMan, device);
		
		vh.stringLabel("InternalName", id, device.getName(), row);

		if(req == null) {
			vh.registerHeaderEntry("Reading (kWh)");
			return;
		}
		EnergyResource counterRes = device.connection().energySensor().reading();
		if(counterRes.exists())
			vh.floatLabel("Reading (kWh)", id, counterRes, row, "%.1f");
		
		if(!Boolean.getBoolean("org.smartrplace.driverhandler.devices.residentialmetering1"))
			return;
		IntegerResource type = device.getSubResource("meterHierarchyType", IntegerResource.class);
		Map<String, String> valuesToSet = new HashMap<>();
		valuesToSet.put("0", "Undefined");
		valuesToSet.put("1", "Main Mater");
		valuesToSet.put("2", "Submeter");
		vh.dropdown("Type", id, type, row, valuesToSet);
	}
	
	@Override
	protected boolean setColumnTitlesToUse(ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh) {
		if(!Boolean.getBoolean("org.smartrplace.driverhandler.devices.residentialmetering1"))
			return super.setColumnTitlesToUse(vh);
		vh.registerHeaderEntry("InternalName");
		vh.registerHeaderEntry("ID");
		vh.registerHeaderEntry("Reading (kWh)");
		vh.registerHeaderEntry(getValueTitle());
		vh.registerHeaderEntry("Tenant");
		vh.registerHeaderEntry("Type");
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
