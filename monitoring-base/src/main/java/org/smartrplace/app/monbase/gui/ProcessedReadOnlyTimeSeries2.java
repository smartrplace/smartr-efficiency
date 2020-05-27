package org.smartrplace.app.monbase.gui;

import java.util.Collections;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.util.DatapointImpl;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;

import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;

public abstract class ProcessedReadOnlyTimeSeries2 extends ProcessedReadOnlyTimeSeries {
	
	protected abstract List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start, long end,
			AggregationMode mode);
	protected String getLabelPostfix() {return "";}

	
	//final protected MonitoringController controller;
	final protected TimeSeriesDataImpl tsdi;
	final protected Datapoint dp;
	
	/** only relevant if dp == null*/
	final protected TimeSeriesNameProvider nameProvider;
	
	//final protected ProcessedReadOnlyTimeSeries newTs;
	final protected AggregationMode mode;
	
	private Long firstTimestampInSource = null;
	private Long lastTimestampInSource = null;

	/*public ProcessedReadOnlyTimeSeries2(TimeSeriesDataImpl tsdi, TimeSeriesNameProvider nameProvider,
			MonitoringController controller) {
		this(tsdi, nameProvider, getMode(controller, tsdi.label(null)));
	}*/
	public ProcessedReadOnlyTimeSeries2(TimeSeriesDataImpl tsdi, TimeSeriesNameProvider nameProvider,
			AggregationMode mode) {
		this(tsdi, nameProvider, mode, null);
	}
	public ProcessedReadOnlyTimeSeries2(TimeSeriesDataImpl tsdi, TimeSeriesNameProvider nameProvider,
			AggregationMode mode, Datapoint dp) {
		super(InterpolationMode.NONE);
		this.nameProvider = nameProvider;
		this.tsdi = tsdi;
		this.dp = dp;
		this.mode = mode;
	}

	public ProcessedReadOnlyTimeSeries2(Datapoint dp) {
		this(dp.getTimeSeriesDataImpl(), null, dp.info().getAggregationMode(), dp);
	}
	
	/*static AggregationMode getMode(MonitoringController controller, String label) {
		final String cparam = controller.getConfigParam(label);
		if(cparam != null && cparam.contains(AggregationMode.Consumption2Meter.name()))
			return AggregationMode.Consumption2Meter;
		else
			return AggregationMode.Meter2Meter;		
	}*/

	@Override
	protected List<SampledValue> updateValues(long start, long end) {
		ReadOnlyTimeSeries ts = tsdi.getTimeSeries();
		if(firstTimestampInSource == null) {
			SampledValue sv = ts.getNextValue(0);
			if(sv != null)
				firstTimestampInSource = sv.getTimestamp();
			else
				return Collections.emptyList();
		}
		if(lastTimestampInSource == null) {
			SampledValue sv = ts.getPreviousValue(Long.MAX_VALUE);
			if(sv != null)
				lastTimestampInSource = sv.getTimestamp();
			else
				return Collections.emptyList();
		}
		if(end < firstTimestampInSource)
			return Collections.emptyList();
		if(start > lastTimestampInSource)
			return Collections.emptyList();
		if(end > lastTimestampInSource)
			end = lastTimestampInSource;
		if(start < firstTimestampInSource)
			start = firstTimestampInSource;
		return getResultValues(ts, start, end, mode);
	}

	public String getShortId() {
		return getShortId(tsdi, nameProvider, dp);
	}
	
	public static String getShortId(TimeSeriesDataImpl tsdi, TimeSeriesNameProvider nameProvider,
			Datapoint dp) {
		String shortId = tsdi.label(null);
		if(tsdi instanceof TimeSeriesDataExtendedImpl) {
			TimeSeriesDataExtendedImpl tse = (TimeSeriesDataExtendedImpl) tsdi;
			if(tse.type != null && tse.type instanceof GaRoDataTypeI) {
				GaRoDataTypeI dataType = (GaRoDataTypeI) tse.type;
				if(nameProvider != null)
					shortId = nameProvider.getShortNameForTypeI(dataType, tse);
				else
					shortId = dp.label();
			}
		}
		return shortId;		
	}
	
	public TimeSeriesDataExtendedImpl getResultSeries() {
		return new TimeSeriesDataExtendedImpl(this,
				getShortId()+getLabelPostfix(), tsdi.description(null)+getLabelPostfix(), InterpolationMode.NONE);
	}
	
	public DatapointImpl getResultSeriesDP() {
		String label;
		String tsLocationOrBaseId;
		if(dp != null) {
			label = dp.label()+getLabelPostfix();
			tsLocationOrBaseId = dp.getLocation()+getLabelPostfix();
		} else {
			label = getShortId()+getLabelPostfix();
			tsLocationOrBaseId = tsdi.id()+getLabelPostfix();
		}
		DatapointImpl result = new DatapointImpl(this, tsLocationOrBaseId, label, false);
		return result ;
	}
}
