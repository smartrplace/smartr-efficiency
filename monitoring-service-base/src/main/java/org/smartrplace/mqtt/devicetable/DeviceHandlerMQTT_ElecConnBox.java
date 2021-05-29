package org.smartrplace.mqtt.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.ElectricCurrentSensor;
import org.ogema.model.sensors.ElectricEnergySensor;
import org.ogema.model.sensors.ElectricVoltageSensor;
import org.ogema.model.sensors.PowerSensor;
import org.ogema.model.sensors.ReactivePowerAngleSensor;
import org.ogema.model.sensors.Sensor;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSimpleProcUtil;
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
	private final VirtualSensorKPIMgmt utilAggDaily;
	private final VirtualSensorKPIMgmt utilAggMonthly;
	
	protected class VirtualSensorKPIMgmtMeter2Interval extends VirtualSensorKPIMgmt {
		protected final int interval;

		public VirtualSensorKPIMgmtMeter2Interval(int interval, TimeseriesSimpleProcUtil util, Logger logger,
				DatapointService dpService) {
			super(util, logger, dpService);
			this.interval = interval;
		}

		@Override
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
		}
		
		@Override
		public SingleValueResource getAndConfigureValueResourceSingle(Datapoint dpSource, VirtualSensorKPIDataBase mapData,
				String newSubResName, Resource device) {
			ElectricityConnection conn = (ElectricityConnection) device;
			EnergyResource energyDailyRealAgg = conn.getSubResource(newSubResName, ElectricEnergySensor.class).reading();
			energyDailyRealAgg.getSubResource("unit", StringResource.class).<StringResource>create().setValue("kWh");
			energyDailyRealAgg.getParent().activate(true);
			
			dpSource.info().setAggregationMode(AggregationMode.Meter2Meter);
			mapData.evalDp = tsProcUtil.processSingle(
					interval==AbsoluteTiming.MONTH?TimeProcUtil.SUM_PER_MONTH_EVAL:TimeProcUtil.SUM_PER_DAY_EVAL,
					dpSource);
			//If the datapoint requires absoluteTiming, set it here
			mapData.absoluteTiming = interval;
			
			return energyDailyRealAgg;
		}
	}
	
	public DeviceHandlerMQTT_ElecConnBox(ApplicationManagerPlus appMan) {
		this.appMan = appMan;
		
		utilAggDaily = new VirtualSensorKPIMgmtMeter2Interval(AbsoluteTiming.DAY,
				new TimeseriesSimpleProcUtil(appMan.appMan(), appMan.dpService(), 4, Long.getLong("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.mininterval")),
				appMan.getLogger(), appMan.dpService());
		utilAggMonthly = new VirtualSensorKPIMgmtMeter2Interval(AbsoluteTiming.MONTH,
				new TimeseriesSimpleProcUtil(appMan.appMan(), appMan.dpService(), 4, Long.getLong("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.mininterval")),
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

			@Override
			public String getTableTitle() {
				return "Electricity Meters";
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

	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService) {
		ElectricityConnectionBox dev = (ElectricityConnectionBox) installDeviceRes.device();
		List<Datapoint> result = new ArrayList<>();
		Datapoint energyDp = addConnDatapoints(result, dev.connection(), null, dpService);
		for(ElectricityConnection subConn: dev.connection().subPhaseConnections().getAllElements()) {
			addConnDatapoints(result, subConn, subConn.getName(), dpService);			
		}
		if(!Boolean.getBoolean("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.suppressdaily") && dev.getLocation().startsWith("elMetersPM2x") && energyDp != null) {
			Datapoint daily = provideIntervalFromMeterDatapoint("energyDaily", energyDp, result, dev.connection(), dpService, utilAggDaily);			
			provideIntervalFromMeterDatapoint("energyMonthly", daily, result, dev.connection(), dpService, utilAggMonthly);		
		}
		
		return result;
	}
	
	/** Return Datapoint for the energy counter*/
	protected Datapoint addConnDatapoints(List<Datapoint> result, ElectricityConnection conn, String phase, DatapointService dpService) {
		addDatapoint(conn.voltageSensor().reading(), result, phase, dpService);
		addDatapoint(conn.powerSensor().reading(), result, phase, dpService);
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
		
		addDatapoint(conn.getSubResource("QPCC_max", PowerResource.class), result, phase, dpService);
		addDatapoint(conn.getSubResource("QPCC_min", PowerResource.class), result, phase, dpService);
		
		return energyDp;
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
			VirtualSensorKPIMgmt util) {
		if(dpSource != null && util != null) {
			final VirtualSensorKPIDataBase mapData1 = util.getDatapointDataAccumulationSingle(dpSource, newSubResName, conn,
					15*TimeProcUtil.MINUTE_MILLIS, false, true, result);
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
		return "A";
	}
}
