package org.ogema.timeseries.eval.simple.api;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.ConsumptionInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.smartrplace.app.monbase.gui.ProcessedReadOnlyTimeSeries2;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;

public abstract class TimeseriesSetProcSingleToSingle implements TimeseriesSetProcessor {
	protected abstract List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start,
			long end, AggregationMode mode);
	protected abstract TimeSeriesNameProvider nameProvider();
	protected abstract AggregationMode getMode(String tsLabel);
	protected final String labelPostfix;
	
	/** Return true if informatoin relevant for the labelling has been added*/
	protected boolean addDatapointInfo(TimeSeriesDataImpl tsdi) {
		return false;
	}
	
	public TimeseriesSetProcSingleToSingle(String labelPostfix) {
		this.labelPostfix = labelPostfix;
	}
	@Override
	public List<TimeSeriesData> getResultSeries(List<TimeSeriesData> input, DatapointService dpService) {
		List<TimeSeriesData> result = new ArrayList<>();
		for(TimeSeriesData tsd: input) {
			TimeSeriesDataImpl tsdi = (TimeSeriesDataImpl) tsd;
			ProcessedReadOnlyTimeSeries2 newTs2 = new ProcessedReadOnlyTimeSeries2(tsdi, nameProvider(),
					getMode(tsdi.label(null))) {
				@Override
				protected List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start,
						long end, AggregationMode mode) {
					return calculateValues(timeSeries, start, end, mode);						
				}
				@Override
				protected String getLabelPostfix() {
					return labelPostfix;
				}
			}; 
			TimeSeriesDataExtendedImpl newtsdi = newTs2.getResultSeries();
			result.add(newtsdi);
		}
		return result;
	}

}
