package org.smartrplace.app.monbase.power;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.timeseries.eval.simple.api.TimeseriesSetProcessor;
import org.ogema.timeseries.eval.simple.api.TimeseriesSimpleProcUtil;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableGeneric.ColumnValue;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableLineI.EnergyEvalObjI;

/** TODO: This is currently only implemented for {@link AggregationMode#Consumption2Meter} and
 * for {@link AggregationMode#Meter2Meter}
 */
public class EnergyEvalObjSchedule implements EnergyEvalObjI {
	protected final FloatResource powerReading;
	//protected final ReadOnlyTimeSeries energyReading;
	protected final Datapoint energyReading;
	protected final AggregationMode mode;
	protected MonitoringController controller;
	
	protected final List<ColumnValue> phaseValData;
	/**
	 * 
	 * @param energyReading
	 * @param powerReading may be null
	 */
	public EnergyEvalObjSchedule(Datapoint energyReading, MonitoringController controller) {
		this(energyReading, null, AggregationMode.Consumption2Meter, controller);
	}
	public EnergyEvalObjSchedule(Datapoint energyReading, FloatResource powerReading,
			AggregationMode mode, MonitoringController controller) {
		this(energyReading, powerReading, AggregationMode.Consumption2Meter, controller, Collections.emptyList());
	}
	public EnergyEvalObjSchedule(Datapoint energyReading, FloatResource powerReading,
			AggregationMode mode, MonitoringController controller,
			List<ColumnValue> phaseValData) {
		this.energyReading = energyReading;
		this.powerReading = powerReading;
		this.mode = mode;
		this.controller = controller;
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
		/*if(energyReading == null) return Float.NaN;
		if(mode == AggregationMode.Meter2Meter)
			return EnergyEvalElConnObj.getEnergyValue(energyReading.getTimeSeries(), startTime, endTime, label);
		TimeseriesSimpleProcUtil util = new TimeseriesSimpleProcUtil(controller.appMan, controller.dpService);
		TimeseriesSetProcessor proc = util.getProcessor(TimeseriesSimpleProcUtil.METER_EVAL);
		List<Datapoint> resultTs = proc.getResultSeries(Arrays.asList(new Datapoint[] {
				energyReading}), controller.dpService);
		//List<TimeSeriesData> resultTs = proc.getResultSeriesTSD(Arrays.asList(new TimeSeriesData[] {
		//		new TimeSeriesDataImpl(energyReading, "", "", InterpolationMode.NONE)}),
		//		controller.dpService, null, controller);
		if(resultTs == null || resultTs.isEmpty())
			return Float.NaN;
		ReadOnlyTimeSeries ts = resultTs.get(0).getTimeSeries();
		return EnergyEvalElConnObj.getEnergyValue(ts, startTime, endTime, label);*/
	}
	
	@Override
	public
	int hasSubPhaseNum() {
		return 0;
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
	float getEnergyValueSubPhase(int index, long startTime, long endTime) {
		ColumnValue cv = phaseValData.get(index);
		if(cv.dp != null)
			return getEnergyValue(cv.dp, cv.dp.info().getAggregationMode(), startTime, endTime, null);
		return Float.NaN;
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
		TimeseriesSimpleProcUtil util = new TimeseriesSimpleProcUtil(controller.appMan, controller.dpService);
		TimeseriesSetProcessor proc = util.getProcessor(TimeseriesSimpleProcUtil.METER_EVAL);
		List<Datapoint> resultTs = proc.getResultSeries(Arrays.asList(new Datapoint[] {
				energyReadingLoc}), controller.dpService);
		//List<TimeSeriesData> resultTs = proc.getResultSeriesTSD(Arrays.asList(new TimeSeriesData[] {
		//		new TimeSeriesDataImpl(energyReading, "", "", InterpolationMode.NONE)}),
		//		controller.dpService, null, controller);
		if(resultTs == null || resultTs.isEmpty())
			return Float.NaN;
		ReadOnlyTimeSeries ts = resultTs.get(0).getTimeSeries();
		return EnergyEvalElConnObj.getEnergyValue(ts, startTime, endTime, label);
	}

}
