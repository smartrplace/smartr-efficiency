package org.ogema.timeseries.eval.simple.mon;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries2;

public abstract class TimeseriesSetProcSingleToSingle implements TimeseriesSetProcessor {
	/** Perform calculation on a certain input series.
	 * 
	 * @param timeSeries input time series
	 * @param start
	 * @param end
	 * @param mode
	 * @param newTs2 This series will contain the result time series, but also has the reference to
	 * 		the input datapoint that can be accessed with {@link ProcessedReadOnlyTimeSeries2#getDp()}
	 * @return
	 */
	protected abstract List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start,
			long end, AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2);
	//protected TimeSeriesNameProvider nameProvider() {return null;}
	//protected abstract AggregationMode getMode(String tsLabel);
	protected final String labelPostfix;
	
	/** Return true if informatoin relevant for the labelling has been added*/
	protected boolean addDatapointInfo(Datapoint tsdi) {
		return false;
	}
	
	public TimeseriesSetProcSingleToSingle(String labelPostfix) {
		this.labelPostfix = labelPostfix;
	}
	@Override
	public List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService) {
		List<Datapoint> result = new ArrayList<>();
		for(Datapoint tsdi: input) {
			//if(nameProvider() != null)
			//	tsdi.setLabel(nameProvider().getShortNameForTypeI(tsdi.getGaroDataType(), tsdi.getTimeSeriesDataImpl()));
			String location = ProcessedReadOnlyTimeSeries2.getDpLocation(tsdi, labelPostfix);
			ProcTsProvider provider = new ProcTsProvider() {
				
				@Override
				public ProcessedReadOnlyTimeSeries2 getTimeseries(Datapoint newtsdi) {
					return new ProcessedReadOnlyTimeSeries2(tsdi) {
						@Override
						protected List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start,
								long end, AggregationMode mode) {
							return calculateValues(timeSeries, start, end, mode, this);						
						}
						@Override
						protected String getLabelPostfix() {
							return labelPostfix;
						}
						
						@Override
						protected long getCurrentTime() {
							return dpService.getFrameworkTime();
						}
					};
				}
			};
			Datapoint newtsdi = getOrUpdateTsDp(location, provider , dpService);
			
			/*ProcessedReadOnlyTimeSeries2 newTs2 = null;
			Datapoint newtsdi = null;
			if(dpService != null) {
				String location = ProcessedReadOnlyTimeSeries2.getDpLocation(tsdi, labelPostfix);
				newtsdi = dpService.getDataPointStandard(location);
				ReadOnlyTimeSeries dpts = newtsdi.getTimeSeries();
				if((dpts != null) && (dpts instanceof ProcessedReadOnlyTimeSeries2))
					newTs2 = (ProcessedReadOnlyTimeSeries2) dpts; 
			}
			if(newTs2 == null) {
				newTs2 = new ProcessedReadOnlyTimeSeries2(tsdi) {
					@Override
					protected List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start,
							long end, AggregationMode mode) {
						return calculateValues(timeSeries, start, end, mode, this);						
					}
					@Override
					protected String getLabelPostfix() {
						return labelPostfix;
					}
					
					@Override
					protected long getCurrentTime() {
						return dpService.getFrameworkTime();
					}
				}; 
				newtsdi = newTs2.getResultSeriesDP(dpService);
			}*/
			result.add(newtsdi);
		}
		return result;
	}

	public interface ProcTsProvider {
		ProcessedReadOnlyTimeSeries2 getTimeseries(Datapoint newtsdi);
	}
	
	/**
	 * 
	 * @param resultLocation result location, usually generated based on tsdi location and postfix
	 * @param tsdi data point used as input
	 * @param provider
	 * @param dpService
	 * @return
	 */
	public static Datapoint getOrUpdateTsDp(String resultLocation, ProcTsProvider provider, DatapointService dpService) {
		ProcessedReadOnlyTimeSeries2 newTs2 = null;
		Datapoint newtsdi = null;
		if(dpService != null) {
			//String location = ProcessedReadOnlyTimeSeries2.getDpLocation(tsdi, labelPostfix);
			newtsdi = dpService.getDataPointStandard(resultLocation);
			ReadOnlyTimeSeries dpts = newtsdi.getTimeSeries();
			if((dpts != null) && (dpts instanceof ProcessedReadOnlyTimeSeries2))
				newTs2 = (ProcessedReadOnlyTimeSeries2) dpts; 
		}
		if(newTs2 == null) {
			newTs2 = provider.getTimeseries(newtsdi);
			newtsdi = newTs2.getResultSeriesDP(dpService);
		}
		return newtsdi;
	}
}
