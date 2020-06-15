package org.smartrplace.app.monbase.power;

import java.util.Collections;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSimpleProcUtil;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableLineI.EnergyEvalObjI;

/** TODO: This is currently only implemented for {@link AggregationMode#Consumption2Meter} and
 * for {@link AggregationMode#Meter2Meter}
 */
public class EnergyEvalObjSchedule implements EnergyEvalObjI {
	protected final FloatResource powerReading;
	//protected final ReadOnlyTimeSeries energyReading;
	protected final Datapoint energyReading;
	protected final AggregationMode mode;
	//protected MonitoringController controller;
	protected final TimeseriesSimpleProcUtil util;

	
	protected List<ColumnDataProvider> phaseValData;
	/**
	 * 
	 * @param energyReading
	 * @param powerReading may be null
	 */
	public EnergyEvalObjSchedule(Datapoint energyReading, TimeseriesSimpleProcUtil util) {
		this(energyReading, null, AggregationMode.Consumption2Meter, util);
	}
	public EnergyEvalObjSchedule(Datapoint energyReading, FloatResource powerReading,
			AggregationMode mode, TimeseriesSimpleProcUtil util) {
		this(energyReading, powerReading, AggregationMode.Consumption2Meter, util, Collections.emptyList());
	}
	public EnergyEvalObjSchedule(Datapoint energyReading, FloatResource powerReading,
			AggregationMode mode, TimeseriesSimpleProcUtil util,
			List<ColumnDataProvider> phaseValData) {
		this.energyReading = energyReading;
		this.powerReading = powerReading;
		this.mode = mode;
		//this.controller = controller;
		this.util = util;
		this.phaseValData = phaseValData;
	}

	@Override
	public
	float getPowerValue() {
		if(powerReading == null) return Float.NaN;
		return powerReading.getValue();
	}

	@Override
	public
	float getEnergyValue() {
		//!! not supported !!
		return Float.NaN;
	}
	@Override
	public
	float getEnergyValue(long startTime, long endTime, String label) {
		return getEnergyValue(energyReading, mode, startTime, endTime, label);
	}
	
	@Override
	public
	int hasSubPhaseNum() {
		return phaseValData.size();
	}
	
	@Override
	public
	boolean hasEnergySensor() {
		return (energyReading != null);
	}
	
	@Override
	public
	float getPowerValueSubPhase(int index) {
		return Float.NaN;
	}

	@Override
	public
	float getEnergyValueSubPhase(int index) {
		return Float.NaN;
	}
	@Override
	public
	float getEnergyValueSubPhase(int index, float lineMainValue, long startTime, long endTime) {
		ColumnDataProvider cv = phaseValData.get(index-1);
		return cv.getValue(lineMainValue, startTime, endTime);
		//if(cv.dp != null)
		//	return getEnergyValue(cv.dp, cv.dp.info().getAggregationMode(), startTime, endTime, null);
		//return Float.NaN;
	}
	
	@Override
	public
	Resource getMeterReadingResource() {
		if(energyReading instanceof Resource)
			return (Resource) energyReading;
		return null;
	}
	
	float getEnergyValue(Datapoint energyReadingLoc, AggregationMode modeLoc,
			long startTime, long endTime, String label) {
		if(energyReadingLoc == null) return Float.NaN;
		if(modeLoc == AggregationMode.Meter2Meter)
			return EnergyEvalElConnObj.getEnergyValue(energyReadingLoc.getTimeSeries(), startTime, endTime, label);
		Datapoint resultDp = util.processSingle(TimeProcUtil.METER_EVAL, energyReadingLoc);
		if(resultDp == null)
			return Float.NaN;
		/*
		//TimeseriesSimpleProcUtil util = new TimeseriesSimpleProcUtil(controller.appMan, controller.dpService);
		TimeseriesSetProcessor proc = util.getProcessor(TimeProcUtil.METER_EVAL);
		List<Datapoint> resultTs = proc.getResultSeries(Arrays.asList(new Datapoint[] {
				energyReadingLoc}), util.dpService);
		//List<TimeSeriesData> resultTs = proc.getResultSeriesTSD(Arrays.asList(new TimeSeriesData[] {
		//		new TimeSeriesDataImpl(energyReading, "", "", InterpolationMode.NONE)}),
		//		controller.dpService, null, controller);
		if(resultTs == null || resultTs.isEmpty())
			return Float.NaN;
		
		ReadOnlyTimeSeries ts = resultTs.get(0).getTimeSeries();*/
		ReadOnlyTimeSeries ts = resultDp.getTimeSeries();
		return EnergyEvalElConnObj.getEnergyValue(ts, startTime, endTime, label);
	}

	@Override
	public Datapoint getDailyConsumptionValues() {
		return util.processSingle(TimeProcUtil.PER_DAY_EVAL, energyReading);
	}
	@Override
	public Datapoint getHourlyConsumptionValues() {
		return util.processSingle(TimeProcUtil.PER_HOUR_EVAL, energyReading);
	}
	
	@Override
	public Datapoint getMeterComparisonValues() {
		return util.processSingle(TimeProcUtil.METER_EVAL, energyReading);
	}
}
