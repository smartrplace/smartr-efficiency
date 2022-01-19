package org.smartrplace.mqtt.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.EnergyResource;
import org.ogema.core.model.units.PowerResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
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
import org.ogema.timeseries.eval.simple.mon3.TimeseriesSimpleProcUtil3;
import org.slf4j.Logger;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.driverhandler.devices.ESE_ElConnBoxDeviceHandler;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIDataBase;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIMgmt;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class DeviceHandlerMQTT_ElecConnBox extends DeviceHandlerBase<ElectricityConnectionBox> {
	private final ApplicationManagerPlus appMan;
	private final VirtualSensorKPIMgmtMeter2Interval utilAggDaily;
	private final VirtualSensorKPIMgmtMeter2Interval utilAggDailyFromPower;
	private final VirtualSensorKPIMgmtMeter2Interval utilAggMonthly;
	private final VirtualSensorKPIMgmtMeter2Interval utilAggYearly;
	private final VirtualSensorKPIMgmtMeter2Interval utilAggPower2Meter;
	
	protected class VirtualSensorKPIMgmtMeter2Interval extends VirtualSensorKPIMgmt {
		protected final int interval;
		protected final AggregationMode sourceAggMode;
		
		public VirtualSensorKPIMgmtMeter2Interval(int interval, TimeseriesSimpleProcUtil3 util, Logger logger,
				DatapointService dpService) {
			this(interval, util, logger, dpService, AggregationMode.Meter2Meter);
		}
		public VirtualSensorKPIMgmtMeter2Interval(int interval, TimeseriesSimpleProcUtil3 util, Logger logger,
				DatapointService dpService, AggregationMode sourceAggMode) {
			super(util, logger, dpService);
			this.interval = interval;
			this.sourceAggMode = sourceAggMode;
		}

		/*@Override
		public VirtualSensorKPIDataBase getDatapointDataAccumulationSingle(Datapoint dpSource,
				String newSubResName, Resource device,
				Long intervalToStayBehindNow,
				boolean registerGovernedSchedule,
				boolean registerRemoteScheduleViaHeartbeat,
				List<Datapoint> result) {
			VirtualSensorKPIDataBase mapData = getDatapointData(dpSource, newSubResName, device,
					intervalToStayBehindNow>=0?intervalToStayBehindNow:15*TimeProcUtil.MINUTE_MILLIS,
					registerGovernedSchedule, registerRemoteScheduleViaHeartbeat, result);
			return mapData;
		}*/
		
		@Override
		public SingleValueResource getAndConfigureValueResourceSingle(Datapoint dpSource, VirtualSensorKPIDataBase mapData,
				String newSubResName, Resource device) {
			ElectricityConnection conn = (ElectricityConnection) device;
			EnergyResource energyDailyRealAgg = conn.getSubResource(newSubResName, ElectricEnergySensor.class).reading();
			energyDailyRealAgg.getSubResource("unit", StringResource.class).<StringResource>create().setValue("kWh");
			energyDailyRealAgg.getParent().activate(true);
			
			//dpSource.info().setAggregationMode(AggregationMode.Meter2Meter);
			dpSource.info().setAggregationMode(sourceAggMode);
			mapData.evalDp = createEvalDp(dpSource);
			//If the datapoint requires absoluteTiming, set it here
			mapData.absoluteTiming = interval;
			
			return energyDailyRealAgg;
		}
		
		public Datapoint createEvalDp(Datapoint dpSource) {
			final String evalStr;
			switch(interval) {
			case AbsoluteTiming.YEAR:
				evalStr = TimeProcUtil.PER_YEAR_EVAL;
				break;
			case AbsoluteTiming.MONTH:
				evalStr = TimeProcUtil.PER_MONTH_EVAL;
				break;
			case AbsoluteTiming.HOUR:
				//Note: This is a special case. We do not aggregate meter values here, but we calculate getMeterFromConsumption
				evalStr = TimeProcUtil.METER_EVAL;
				break;
			default:
				evalStr = TimeProcUtil.PER_DAY_EVAL;
			}
			return tsProcUtil.processSingle(evalStr, dpSource);
		}
	}
	
	public DeviceHandlerMQTT_ElecConnBox(ApplicationManagerPlus appMan) {
		this.appMan = appMan;
		
		utilAggDaily = new VirtualSensorKPIMgmtMeter2Interval(AbsoluteTiming.DAY,
				new TimeseriesSimpleProcUtil3(appMan.appMan(), appMan.dpService(), 4, Long.getLong("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.mininterval", 10000)),
				appMan.getLogger(), appMan.dpService());
		utilAggDailyFromPower = new VirtualSensorKPIMgmtMeter2Interval(AbsoluteTiming.DAY,
				new TimeseriesSimpleProcUtil3(appMan.appMan(), appMan.dpService(), 4, Long.getLong("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.mininterval", 10000)),
				appMan.getLogger(), appMan.dpService(), AggregationMode.Power2Meter);
		utilAggMonthly = new VirtualSensorKPIMgmtMeter2Interval(AbsoluteTiming.MONTH,
				new TimeseriesSimpleProcUtil3(appMan.appMan(), appMan.dpService(), 4, Long.getLong("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.mininterval", 10000)),
				appMan.getLogger(), appMan.dpService());
		utilAggYearly = new VirtualSensorKPIMgmtMeter2Interval(AbsoluteTiming.YEAR,
				new TimeseriesSimpleProcUtil3(appMan.appMan(), appMan.dpService(), 4, Long.getLong("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.mininterval", 10000)),
				appMan.getLogger(), appMan.dpService());		
		utilAggPower2Meter = new VirtualSensorKPIMgmtMeter2Interval(AbsoluteTiming.HOUR,
				new TimeseriesSimpleProcUtil3(appMan.appMan(), appMan.dpService(), 4, Long.getLong("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.mininterval", 10000)),
				appMan.getLogger(), appMan.dpService());		
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
		//if(!Boolean.getBoolean("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.suppressdaily") &&
		//		(dev.getLocation().startsWith("elMetersPM2x") || dev.getLocation().startsWith("MQTTMeter"))) {
if(installDeviceRes.getLocation().contains("knownDevices_117"))
System.out.println("powerPath(1):"+(powerDp!=null?powerDp.getLocation():"null"));
		if(!Boolean.getBoolean("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.suppressdaily")) {
if(installDeviceRes.getLocation().contains("knownDevices_117"))
System.out.println("powerPath(2):"+(powerDp!=null?powerDp.getLocation():"null"));
			Datapoint daily = null;
			boolean createResource = !Boolean.getBoolean("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.suppress_resourcecreation_plus");
			if(energyDp != null) {
				daily = provideIntervalFromMeterDatapoint("energyDaily", energyDp, result, dev.connection(), dpService,
						utilAggDaily, createResource);			
			} else if(powerDp != null) {
				daily = provideIntervalFromMeterDatapoint("energyDaily", powerDp, result, dev.connection(), dpService,
						utilAggDailyFromPower, createResource);
if(installDeviceRes.getLocation().contains("knownDevices_117"))
System.out.println("result#:"+result.size()+" daily:"+(daily!=null?daily.getLocation():"null"));
				if(energyDp == null && Boolean.getBoolean("org.smartrplace.mqtt.devicetable.PM2xenergy.power2meter")) {
					provideIntervalFromMeterDatapoint("energyY", powerDp, result, dev.connection(), dpService, utilAggPower2Meter, createResource);
				}
			}
			if(daily != null) {
				Datapoint monthly = provideIntervalFromMeterDatapoint("energyMonthly", daily, result, dev.connection(), dpService, utilAggMonthly, createResource);
				if(Boolean.getBoolean("org.smartrplace.mqtt.devicetable.PM2xenergyYearly"))
					provideIntervalFromMeterDatapoint("energyYearly", monthly, result, dev.connection(), dpService, utilAggYearly, createResource);				
			}
		}
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
			List<Datapoint> result, ElectricityConnection conn, DatapointService dpService,
			VirtualSensorKPIMgmtMeter2Interval util, boolean createResource) {
		if(dpSource != null && util != null) {
			final VirtualSensorKPIDataBase mapData1;
			if(createResource)
				mapData1 = util.addVirtualDatapointSingle(dpSource, newSubResName, conn,
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
		return "Electricity Meters";
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
}
