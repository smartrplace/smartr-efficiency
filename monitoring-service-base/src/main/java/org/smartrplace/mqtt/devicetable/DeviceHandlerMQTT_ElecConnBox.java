package org.smartrplace.mqtt.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.EnergyResource;
import org.ogema.core.model.units.PowerResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH.DestType;
import org.ogema.model.communication.CommunicationStatus;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.ElectricCurrentSensor;
import org.ogema.model.sensors.ElectricEnergySensor;
import org.ogema.model.sensors.ElectricPowerSensor;
import org.ogema.model.sensors.ElectricVoltageSensor;
import org.ogema.model.sensors.PowerSensor;
import org.ogema.model.sensors.ReactivePowerAngleSensor;
import org.ogema.model.sensors.Sensor;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon3.MeteringEvalUtil;
import org.ogema.timeseries.eval.simple.mon3.VirtualSensorKPIMgmtMeter2Interval;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.autoconfig.api.DeviceTypeProvider;
import org.smartrplace.driverhandler.devices.ESE_ElConnBoxDeviceHandler;
import org.smartrplace.timeseries.manual.model.ManualEntryData;
import org.smartrplace.timeseries.manual.servlet.ManualTimeseriesServlet;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIDataBase;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class DeviceHandlerMQTT_ElecConnBox extends DeviceHandlerBase<ElectricityConnectionBox> {
	private final ApplicationManagerPlus appMan;
	private final ResourceList<ElectricityConnectionBox> manualMeters;
	
	public DeviceHandlerMQTT_ElecConnBox(ApplicationManagerPlus appMan) {
		this.appMan = appMan;
		manualMeters = appMan.getResourceAccess().getResource("manualMeters");
		if(manualMeters != null) {
			if(manualMeters.getElementType() == null)
				manualMeters.setElementType(ElectricityConnectionBox.class);
			for(ElectricityConnectionBox meter: manualMeters.getAllElements()) {
				ManualEntryData manData = meter.addDecorator("manualEntryData", ManualEntryData.class);
				if(!manData.isActive())
					continue;
				EnergyResource energy = manData.manualEntryDataHolder().getSubResource("energySensor", EnergyResource.class);
				if(!energy.exists())
					continue;
				energy.program().create().activate(true);
				Datapoint dpEnergy = appMan.dpService().getDataPointStandard(energy);
				if(dpEnergy != null)
					dpEnergy.setTimeSeries(energy.program());
				ManualTimeseriesServlet.registerManualEntryData(manData);
			}
		}
	}
	
	@Override
	public Class<ElectricityConnectionBox> getResourceType() {
		return ElectricityConnectionBox.class;
	}

	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert,
			InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector, this) {
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

				final ElectricityConnectionBox box =
						(ElectricityConnectionBox) addNameWidget(object, vh, id, req, row, appMan);

				Room deviceRoom = box.location().room();
				//Label lastContact = null;

				ElectricityConnection cc = box.connection();
				if(cc.powerSensor().isActive() || req == null)
					addPowerEnergySensor(cc, vh, id, req, row);
				else for (ElectricityConnection c : box.getSubResources(ElectricityConnection.class, true)) {
					if(c.powerSensor().isActive()) {
						addPowerEnergySensor(c, vh, id, req, row);
						break;
					}
				}

				addRoomWidget(vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object, vh, id, req, row);
				addInstallationStatus(object, vh, id, req, row);
				addComment(object, vh, id, req, row);

				appSelector.addWidgetsExpert(null, object, vh, id, req, row, appMan);
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return DeviceHandlerMQTT_ElecConnBox.this.getResourceType();
			}
			
			@Override
			protected String id() {
				return DeviceHandlerMQTT_ElecConnBox.this.id();
			}

			protected void addPowerEnergySensor(ElectricityConnection c, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row) {   
				Label voltage = vh.floatLabel("Voltage", // (" + ResourceUtils.getHumanReadableShortName(c) + ")",
						id, c.voltageSensor().reading(), row, "%.1f");
				Label power = vh.floatLabel("Power", // (" + ResourceUtils.getHumanReadableShortName(c) + ")",
						id, c.powerSensor().reading(), row, "%.1f");
				Label lastContact = addLastContact(vh, id, req, row,
							c.powerSensor().reading());
				if (req != null) {
					voltage.setPollingInterval(DEFAULT_POLL_RATE, req);
					power.setPollingInterval(DEFAULT_POLL_RATE, req);
					lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
				}

			}
		};
	}
	

	@Override
	protected Class<? extends ResourcePattern<ElectricityConnectionBox>> getPatternClass() {
		return ElectricityConnectionBoxPattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}

	Set<String> devDone = new HashSet<>();
	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService) {
		ElectricityConnectionBox dev = (ElectricityConnectionBox) installDeviceRes.device();
		if(Boolean.getBoolean("org.smartrplace.mqtt.devicetable.activate")) {
			if(!devDone.contains(dev.getLocation())) {
				dev.activate(true);
				devDone.add(dev.getLocation());
			}
		}
		List<Datapoint> result = new ArrayList<>();
		Datapoint[] connDps = addConnDatapoints(result, dev.connection(), null, dpService);
		Datapoint energyDp = connDps[0];
		Datapoint powerDp = connDps[1];
		for(ElectricityConnection subConn: dev.connection().subPhaseConnections().getAllElements()) {
			addConnDatapoints(result, subConn, subConn.getName(), dpService);			
		}
		
		MeteringEvalUtil.addDailyMeteringEval(energyDp, powerDp, dev.connection(), result, appMan);
		return result;
	}
	
	/** Return Datapoint for the energy counter*/
	protected Datapoint[] addConnDatapoints(List<Datapoint> result, ElectricityConnection conn, String phase, DatapointService dpService) {
		Datapoint[] returnVal = new Datapoint[2];
		addDatapoint(conn.voltageSensor().reading(), result, phase, dpService);
		Datapoint powerDp = addDatapoint(conn.powerSensor().reading(), result, phase, dpService);
		addDatapoint(conn.powerSensor().settings().setpoint(), result, phase, dpService);
		addDatapoint(conn.reactivePowerSensor().reading(), result, phase, dpService);
		addDatapoint(conn.reactivePowerSensor().settings().setpoint(), result, phase, dpService);
		addDatapoint(conn.reactiveAngleSensor().reading(), result, phase, dpService);
		Datapoint energyDp = addDatapoint(conn.energySensor().reading(), result, phase, dpService);
		addDatapoint(conn.currentSensor().reading(), result, phase, dpService);
		addDatapoint(conn.frequencySensor().reading(), result, phase, dpService);
		addDecoratorDatapoints(conn.getSubResources(ElectricVoltageSensor.class, false), result, phase, dpService);
		addDecoratorDatapoints(conn.getSubResources(ElectricCurrentSensor.class, false), result, phase, dpService);
		addDecoratorDatapoints(conn.getSubResources(ElectricEnergySensor.class, false), result, phase, dpService);
		addDecoratorDatapoints(conn.getSubResources(PowerSensor.class, false), result, phase, dpService);
		addDecoratorDatapoints(conn.getSubResources(ReactivePowerAngleSensor.class, false), result, phase, dpService);
		
		addDatapoint(conn.getSubResource("apparentPowerSensor", ElectricPowerSensor.class).reading(), result, phase, dpService);
		//addDatapoint(conn.getSubResource("voltageSensorAB", ElectricVoltageSensor.class).reading(), result, phase, dpService);
		//addDatapoint(conn.getSubResource("voltageSensorBC", ElectricVoltageSensor.class).reading(), result, phase, dpService);
		//addDatapoint(conn.getSubResource("voltageSensorCA", ElectricVoltageSensor.class).reading(), result, phase, dpService);
		
		addDatapoint(conn.getSubResource("QPCC_min", PowerResource.class), result, phase, dpService);
		addDatapoint(conn.getSubResource("QPCC_max", PowerResource.class), result, phase, dpService);
		
		returnVal[0] = energyDp;
		returnVal[1] = powerDp;
		return returnVal;
	}
	
	protected <T extends Sensor> void addDecoratorDatapoints(List<T> valRess, List<Datapoint> result, String phase, DatapointService dpService) {
		for(T sens: valRess) {
			if(!sens.isDecorator())
				continue;
			ValueResource valRes = sens.reading();
			if(valRes instanceof SingleValueResource) {
				final String phaseToUse;
				if(sens instanceof ElectricVoltageSensor) {
					String name = sens.getName();
					if(name.startsWith("voltageSensor") && name.length() > "voltageSensor".length()) {
						String subName = name.substring("voltageSensor".length());
						if(phase == null)
							phaseToUse = subName;
						else phaseToUse = phase+"_"+subName;
					} else
						phaseToUse = phase;
				} else
					phaseToUse = phase;
				addDatapoint((SingleValueResource) sens.reading(), result, phaseToUse, dpService);
			}
		}
	}
	
	/**Derived from {@link ESE_ElConnBoxDeviceHandler#provideAccumulatedDatapoint2()}
	 * */
	protected static Datapoint provideIntervalFromMeterDatapoint(String newSubResName,
			Datapoint dpSource,
			List<Datapoint> result, Resource destinationResParent, DatapointService dpService,
			VirtualSensorKPIMgmtMeter2Interval util, boolean createResource) {
		if(dpSource != null && util != null) {
			final VirtualSensorKPIDataBase mapData1;
			if(createResource)
				mapData1 = util.addVirtualDatapointSingle(dpSource, newSubResName, destinationResParent,
						15*TimeProcUtil.MINUTE_MILLIS, false, true, result);
			else {
				Datapoint dp = util.createEvalDp(dpSource);
				if(result != null)
					result.add(dp);
				return dp;
			}
			/*final VirtualSensorKPIDataBase mapData1 = util.getDatapointDataAccumulationSingle(dpSource, newSubResName, conn,
					15*TimeProcUtil.MINUTE_MILLIS, false, true, result);*/
if(mapData1 == null) {
	System.out.println("   !!!!  WARNING: Unexpected null value in provideIntervalFromMeterDatapoint for "+dpSource.getLocation()+" nSubRN:"+newSubResName);
	return null;
}
			return mapData1.evalDp;
		}
		return null;
	}

	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		ElectricityConnectionBox device = (ElectricityConnectionBox) appDevice.device();
		if(Boolean.getBoolean("org.smartrplace.project.smFac1")) {
			AlarmingUtiH.setTemplateValues(appDevice, device.connection().powerSensor().reading(),
					0.0f, 9999999.0f, 30, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES, 2880, DestType.CUSTOMER_SP_SAME, 2);
			AlarmingUtiH.setAlarmingActiveStatus(appDevice, device.connection().voltageSensor().reading(), false);
			AlarmingUtiH.setAlarmingActiveStatus(appDevice, device.connection().frequencySensor().reading(), false);
			CommunicationStatus comStat = device.getSubResource("communicationStatus", CommunicationStatus.class);
			if(comStat.isActive())
				AlarmingUtiH.setAlarmingActiveStatus(appDevice, comStat.quality(), false);
			return;
		}
		AlarmingUtiH.setTemplateValues(appDevice, device.connection().powerSensor().reading(),
				0.0f, Float.MAX_VALUE, 10, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES);
		AlarmingUtiH.setTemplateValues(appDevice, device.connection().voltageSensor().reading(),
				200f, 245f, 10, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES);
		AlarmingUtiH.setTemplateValues(appDevice, device.connection().frequencySensor().reading(),
				49.8f, 50.2f, 1, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES);
		AlarmingUtiH.addAlarmingMQTT(device, appDevice);
	}

	@Override
	public String getInitVersion() {
		if(Boolean.getBoolean("org.smartrplace.project.smFac1"))
			return "C";
		return "A";
	}

	public String getTableTitle() {
		return "MQTT Electricity Meters";
	}
	
	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "MQEM";
	}
	
	@Override
	public ComType getComType() {
		return ComType.IP;
	}

	@Override
	public SingleValueResource getMainSensorValue(ElectricityConnectionBox box,
			InstallAppDevice deviceConfiguration) {
		ElectricityConnection cc = box.connection();
		if(cc.powerSensor().isActive())
			return cc.powerSensor().reading();
		else for (ElectricityConnection c : box.getSubResources(ElectricityConnection.class, true)) {
			if(c.powerSensor().isActive()) {
				return c.powerSensor().reading();
			}
		}
		return null;
	}
	
	protected class ManualMeterDeviceType implements DeviceTypeProvider<ElectricityConnectionBox> {
		public ManualMeterDeviceType(ApplicationManagerPlus appManPlus) {
			this.appMan = appManPlus.appMan();
			this.appManPlus = appManPlus;
		}

		private final ApplicationManager appMan;
		private final ApplicationManagerPlus appManPlus;
		
		@Override
		public String id() {
			return "manualMeter";
		}

		@Override
		public String label(OgemaLocale arg0) {
			return "Energy Meter for manual input";
		}

		@Override
		public Class<ElectricityConnectionBox> getDeviceType() {
			return ElectricityConnectionBox.class;
		}

		@Override
		public String description(OgemaLocale locale) {
			return "address shall provide meter ID, no password and configuration required";
		}

		@Override
		public CreateAndConfigureResult<ElectricityConnectionBox> addAndConfigureDevice(
				DeviceTypeConfigData<ElectricityConnectionBox> configData) {
			
			CreateAndConfigureResult<ElectricityConnectionBox> result = new CreateAndConfigureResult<ElectricityConnectionBox>();

			if(configData.address.isEmpty()) {
				result.resultMessage = "Empty address cannot be processed!";
				return result;
			}
			if(configData.governingResource == null) {
				if(manualMeters == null) {
					ResourceList<?> manualMeters = appMan.getResourceManagement().createResource("manualMeters", ResourceList.class);
					manualMeters.setElementType(ElectricityConnectionBox.class);
				}
				configData.governingResource = manualMeters.addDecorator(ResourceUtils.getValidResourceName(
						"manualMeter_"+configData.address),
						ElectricityConnectionBox.class);
			}
			
			ManualEntryData manData = configData.governingResource.addDecorator("manualEntryData", ManualEntryData.class);
			ValueResourceHelper.setCreate(manData.lowerLimit(), 0);
			ValueResourceHelper.setCreate(manData.upperLimit(), Float.MAX_VALUE);
			manData.manualEntryDataHolder().create();
			//TODO: Really init with zero?
			ValueResourceHelper.setCreate(configData.governingResource.connection().energySensor().reading(), 0);
			EnergyResource energy = manData.manualEntryDataHolder().addDecorator("energySensor", configData.governingResource.connection().energySensor().reading());
			energy.program().create().activate(true);
			Datapoint dpEnergy = appManPlus.dpService().getDataPointStandard(energy);
			if(dpEnergy != null)
				dpEnergy.setTimeSeries(energy.program());
			configData.governingResource.activate(true);
			ManualTimeseriesServlet.registerManualEntryData(manData);
			
			result.resultConfig = configData;
			result.resultMessage = "Created manual meter device on "+configData.governingResource.getLocation();
			return result ;
		}

		@Override
		public Collection<DeviceTypeConfigData<ElectricityConnectionBox>> getKnownConfigs() {
			List<DeviceTypeConfigData<ElectricityConnectionBox>> result = new ArrayList<>();
			List<ElectricityConnectionBox> allRes = appMan.getResourceAccess().getResources(ElectricityConnectionBox.class);
			for(ElectricityConnectionBox con: allRes) {
				String address;
				String resName = con.getName();
				if(resName.length() <= ("manualMeter_".length()+1))
					address = resName;
				else
					address = resName.substring("manualMeter_".length());
				DeviceTypeConfigData<ElectricityConnectionBox> config = new DeviceTypeConfigData<ElectricityConnectionBox>(address,
						null, "");
				config.governingResource = con;
				config.dtbProvider = this;
				result.add(config);
			}
			return result;
		}

		@Override
		public boolean deleteConfig(DeviceTypeConfigData<ElectricityConnectionBox> configData) {
			configData.governingResource.delete();
			return true;
		}

		@Override
		public DeviceTypeConfigDataBase getPlaceHolderData() {
			return new DeviceTypeConfigDataBase("0000:0000", null, "");
		}		
	}
	
	@Override
	public Collection<DeviceTypeProvider<?>> getDeviceTypeProviders() {
		List<DeviceTypeProvider<?>> result = new ArrayList<>();
		DeviceTypeProvider<ElectricityConnectionBox> prov = new ManualMeterDeviceType(appMan);
		result.add(prov);
		return result;
	}
}
