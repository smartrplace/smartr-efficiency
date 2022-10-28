package org.ogema.timeseries.eval.simple.mon3;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.EnergyResource;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.model.sensors.ElectricEnergySensor;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.slf4j.Logger;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIDataBase;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIMgmt;

import de.iwes.util.timer.AbsoluteTiming;

public class VirtualSensorKPIMgmtMeter2Interval extends VirtualSensorKPIMgmt {
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

	@Override
	public SingleValueResource getAndConfigureValueResourceSingle(Datapoint dpSource, VirtualSensorKPIDataBase mapData,
			String newSubResName, Resource device) {
		Resource conn = device;
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
