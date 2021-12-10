package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.metering.ElectricityMeter;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon3.std.StandardEvalAccess;
import org.ogema.timeseries.eval.simple.mon3.std.StandardEvalAccess.StandardDeviceEval;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIMgmt;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public class HMIEC_ElConnDeviceHandler extends DeviceHandlerSimple<ElectricityConnectionBox> {

	public static final long VIRTUAL_SENSOR_UPDATE_RATE = 3*TimeProcUtil.MINUTE_MILLIS;

	//private final VirtualSensorKPIMgmt utilAggSubPhases;

	protected final ApplicationManagerPlus appManPlus;
	
	public HMIEC_ElConnDeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, false);
		this.appManPlus = appMan;
		/*TimeseriesSimpleProcUtil3 util = new TimeseriesSimpleProcUtil3(appMan.appMan(), appMan.dpService(), 2, VIRTUAL_SENSOR_UPDATE_RATE) {
		};
		utilAggSubPhases = new VirtualSensorKPIMgmt(
				util,
				appMan.getLogger(), appMan.dpService()) {
			
			@Override
			public SingleValueResource getAndConfigureValueResourceSingle(Datapoint dpSource, VirtualSensorKPIDataBase mapData,
					String newSubResName, Resource device) {
				throw new IllegalStateException("Multi-input required!");
				
			}
			@Override
			public SingleValueResource getAndConfigureValueResource(List<Datapoint> dpSource, VirtualSensorKPIDataBase mapData,
					String newSubResName, Resource device) {
				ElectricityConnectionBox iota = (ElectricityConnectionBox) device;
				ElectricityConnection conn = iota.elConn();
	
				EnergyResource energyDailyRealAgg = conn.getSubResource(newSubResName, ElectricEnergySensor.class).reading();
				energyDailyRealAgg.getSubResource("unit", StringResource.class).<StringResource>create().setValue("kWh");
				energyDailyRealAgg.getParent().activate(true);

				List<Datapoint> sums;
				Integer absoluteTiming;
				if(newSubResName.toLowerCase().contains("15min")) {
					sums = VirtualSensorKPIMgmt.registerEnergySumDatapointOverSubPhases(conn, AggregationMode.Meter2Meter, tsProcUtil, dpService,
							"15min");
					absoluteTiming = AbsoluteTiming.FIFTEEN_MINUTE;
				} else if(newSubResName.toLowerCase().contains("hour")) {
					sums = VirtualSensorKPIMgmt.registerEnergySumDatapointOverSubPhases(conn, AggregationMode.Meter2Meter, tsProcUtil, dpService,
							"hour");
					absoluteTiming = AbsoluteTiming.HOUR;
				} else {
					sums = VirtualSensorKPIMgmt.registerEnergySumDatapointOverSubPhasesFromDay(conn, AggregationMode.Meter2Meter, tsProcUtil, dpService,
							dpSource.get(0));					
					absoluteTiming = AbsoluteTiming.DAY;
				}
				
				if(!sums.isEmpty()) {
					mapData.evalDp = sums.get(0);
					mapData.absoluteTiming = absoluteTiming;
				}
				return energyDailyRealAgg;
			}
		};*/
	}

	@Override
	public Class<ElectricityConnectionBox> getResourceType() {
		return ElectricityConnectionBox.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(ElectricityConnectionBox device,
			InstallAppDevice deviceConfiguration) {
		List<ElectricityMeter> meters = device.meters().getAllElements();
		if(meters.isEmpty())
			return null;
		ElectricityMeter consumptionMeter = null;
		int maxIdx = Integer.MAX_VALUE;
		for(ElectricityMeter meter: meters) {
			if(meter.type().getValue() > 0 && meter.type().getValue() < maxIdx) {
				consumptionMeter = meter;
				maxIdx = meter.type().getValue();
			}
		}
		if(consumptionMeter == null)
			consumptionMeter = meters.get(0);
		return consumptionMeter.energyReading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(ElectricityConnectionBox device,
			InstallAppDevice deviceConfiguration) {
		// We have to sychronize with reading remote slotsdb and setting up the time series for mirror devices here
		VirtualSensorKPIMgmt.waitForCollectingGatewayServerInit(appMan.getResourceAccess());
		/*if(mirrorList != null) {
			IntegerResource initStatus = mirrorList.getSubResource("initStatus", IntegerResource.class);
			while(initStatus.isActive() && (initStatus.getValue() < 2) && Boolean.getBoolean("org.smartrplace.app.srcmon.iscollectinggateway")) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}*/
		
		List<Datapoint> result = new ArrayList<>();
		//List<Datapoint> energy = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		for(ElectricityMeter meter: device.meters().getAllElements()) {
			String ph = meter.getName();
			Datapoint dp = addDatapoint(meter.energyReading(), result, ph, dpService);
			addDatapoint(meter.powerReading(), result, ph, dpService);

			if(Boolean.getBoolean("virtualSensors.disable")) {
				//System.out.println("   *** Disabling HM_IEC virtualSensors based on energySensor: "+device.getLocation());
				continue;
			}
			if(dp == null) {
				//System.out.println("   !!! WARNING: HM-IEC without energySensor to use: "+device.getLocation());
				continue;
			}
			
			dp.info().setAggregationMode(AggregationMode.Meter2Meter);
			Datapoint evalDp = StandardEvalAccess.getDatapointBaseEvalMetering(dp,
					StandardDeviceEval.COUNTER_TO_15MIN, dpService);
			result.add(evalDp);
			Datapoint evalDpDaily = StandardEvalAccess.getDatapointBaseEvalMetering(dp,
					StandardDeviceEval.COUNTER_TO_DAILY_B15, dpService);

			FloatResource dailyTraffic = meter.getSubResource("energySumDaily", FloatResource.class);
			dailyTraffic.create().activate(true);
			Datapoint dpDaily = StandardEvalAccess.addVirtualDatapoint(dailyTraffic,
					evalDpDaily, dpService, result);
			dpDaily.addToSubRoomLocationAtomic(null, null, meter.getName()+"-daily", false);
		}
		addtStatusDatapointsHomematic(device, dpService, result);
		
		return result;
	}

	@Override
	public void addMoreWidgetsExpert(InstallAppDevice object,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		if(req == null) {
			vh.registerHeaderEntry("RefTimeCounter");
			vh.registerHeaderEntry("EnergyAsKwh");
			return;
		}
		ElectricityConnectionBox device = (ElectricityConnectionBox)object.device();

		vh.floatEdit("RefTimeCounter", id, device.getSubResource("refTimeCounter", FloatResource.class), row, null,
				0, Float.MAX_VALUE, "Reference counter must be grater zero!", 3);
		
	}
	
	@Override
	public String getTableTitle() {
		return "IEC62056 Metering";
	}

	@Override
	protected Class<? extends ResourcePattern<ElectricityConnectionBox>> getPatternClass() {
		return HMIEC_ElConnBoxPattern.class;
	}

	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		ElectricityConnectionBox device = (ElectricityConnectionBox) appDevice.device();
		
		for(ElectricityMeter meter: device.meters().getAllElements()) {
			AlarmingUtiH.setTemplateValues(appDevice, meter.energyReading(),
					0f, Float.MAX_VALUE, 10, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES);
		}
	}
	
	@Override
	public String getInitVersion() {
		return "A";
	}
	
	@Override
	public ComType getComType() {
		return ComType.IP;
	}
}
