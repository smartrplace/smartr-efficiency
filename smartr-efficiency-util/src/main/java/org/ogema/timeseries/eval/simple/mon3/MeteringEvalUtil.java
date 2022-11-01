package org.ogema.timeseries.eval.simple.mon3;

import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIDataBase;

import de.iwes.util.timer.AbsoluteTiming;

public class MeteringEvalUtil {
	private static VirtualSensorKPIMgmtMeter2Interval utilAggDaily2 = null;
	private static VirtualSensorKPIMgmtMeter2Interval utilAggDailyFromPower2 = null;
	private static VirtualSensorKPIMgmtMeter2Interval utilAggMonthly2 = null;
	private static VirtualSensorKPIMgmtMeter2Interval utilAggYearly2 = null;
	private static VirtualSensorKPIMgmtMeter2Interval utilAggPower2Meter2 = null;

	public static VirtualSensorKPIMgmtMeter2Interval utilAggDaily(ApplicationManagerPlus appMan) {
		if(utilAggDaily2 == null) {
			utilAggDaily2 = new VirtualSensorKPIMgmtMeter2Interval(AbsoluteTiming.DAY,
					new TimeseriesSimpleProcUtil3(appMan.appMan(), appMan.dpService(), 4, Long.getLong("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.mininterval", 10000)),
					appMan.getLogger(), appMan.dpService());
		}
		return utilAggDaily2;
	};
	public static VirtualSensorKPIMgmtMeter2Interval utilAggDailyFromPower(ApplicationManagerPlus appMan) {
		if(utilAggDailyFromPower2 == null) {
			utilAggDailyFromPower2 = new VirtualSensorKPIMgmtMeter2Interval(AbsoluteTiming.DAY,
					new TimeseriesSimpleProcUtil3(appMan.appMan(), appMan.dpService(), 4, Long.getLong("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.mininterval", 10000)),
					appMan.getLogger(), appMan.dpService(), AggregationMode.Power2Meter);			
		}
		return utilAggDailyFromPower2;
	}
	public static VirtualSensorKPIMgmtMeter2Interval utilAggMonthly(ApplicationManagerPlus appMan) {
		if(utilAggMonthly2 == null) {
			utilAggMonthly2 = new VirtualSensorKPIMgmtMeter2Interval(AbsoluteTiming.MONTH,
					new TimeseriesSimpleProcUtil3(appMan.appMan(), appMan.dpService(), 4, Long.getLong("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.mininterval", 10000)),
					appMan.getLogger(), appMan.dpService());			
		}
		return utilAggMonthly2;
	}
	public static VirtualSensorKPIMgmtMeter2Interval utilAggYearly(ApplicationManagerPlus appMan) {
		if(utilAggYearly2 == null) {
			utilAggYearly2 = new VirtualSensorKPIMgmtMeter2Interval(AbsoluteTiming.YEAR,
					new TimeseriesSimpleProcUtil3(appMan.appMan(), appMan.dpService(), 4, Long.getLong("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.mininterval", 10000)),
					appMan.getLogger(), appMan.dpService());					
		}
		return utilAggYearly2;
	}
	public static VirtualSensorKPIMgmtMeter2Interval utilAggPower2Meter(ApplicationManagerPlus appMan) {
		if(utilAggPower2Meter2 == null) {
			utilAggPower2Meter2 = new VirtualSensorKPIMgmtMeter2Interval(AbsoluteTiming.HOUR,
					new TimeseriesSimpleProcUtil3(appMan.appMan(), appMan.dpService(), 4, Long.getLong("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.mininterval", 10000)),
					appMan.getLogger(), appMan.dpService());			
		}
		return utilAggPower2Meter2;
	}
	
	/**
	 * 
	 * @param energyDp at least energyDp or powerDp should be non-zero
	 * @param powerDp
	 */
	public static Datapoint addDailyMeteringEval(Datapoint energyDp, Datapoint powerDp, Resource destinationResParent,
			List<Datapoint> result , ApplicationManagerPlus appMan) {
		if(!Boolean.getBoolean("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.suppressdaily")) {
			DatapointService dpService = appMan.dpService();
			Datapoint daily = null;
			boolean createResource = !Boolean.getBoolean("org.smartrplace.mqtt.devicetable.PM2xenergyDaily.suppress_resourcecreation_plus");
			if(energyDp != null) {
				daily = provideIntervalFromMeterDatapoint("energyDaily", energyDp, result, destinationResParent, dpService,
						utilAggDaily(appMan), createResource);			
			} else if(powerDp != null) {
				daily = provideIntervalFromMeterDatapoint("energyDaily", powerDp, result, destinationResParent, dpService,
						utilAggDailyFromPower(appMan), createResource);
				if(energyDp == null && Boolean.getBoolean("org.smartrplace.mqtt.devicetable.PM2xenergy.power2meter")) {
					provideIntervalFromMeterDatapoint("energy", powerDp, result, destinationResParent, dpService, utilAggPower2Meter(appMan), createResource);
				}
			}
			if(daily != null) {
				Datapoint monthly = provideIntervalFromMeterDatapoint("energyMonthly", daily, result, destinationResParent, dpService, utilAggMonthly(appMan), createResource);
				if(Boolean.getBoolean("org.smartrplace.mqtt.devicetable.PM2xenergyYearly"))
					provideIntervalFromMeterDatapoint("energyYearly", monthly, result, destinationResParent, dpService, utilAggYearly(appMan), createResource);				
			}
			return daily;
		}
		return null;
	}
	
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
}
