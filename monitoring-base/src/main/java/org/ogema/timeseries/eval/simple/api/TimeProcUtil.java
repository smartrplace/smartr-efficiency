package org.ogema.timeseries.eval.simple.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.smartrplace.app.monbase.gui.ProcessedReadOnlyTimeSeries;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.format.StringFormatHelper.StringProvider;

public class TimeProcUtil {
	public static int DEFAULT_MAX_ELS = 5;
	
	public static void printTimeSeriesSet(List<TimeSeriesData> tsdlist, String setName, int maxTsToPrint, Long startTime, Long endTime) {
		int nonImplCount = 0;
		int maxSize = -1;
		String maxName = "-";
		int minSize = Integer.MAX_VALUE;
		String minName = "-";
		int countTs = 0;
		int sizeSum = 0;
		int sizeNNSum = 0;
		List<TimeSeriesDataImpl> listsToPrint = new ArrayList<>();
		for(TimeSeriesData tsdIn: tsdlist) {
			if(!(tsdIn instanceof TimeSeriesDataImpl)) {
				nonImplCount++;
				continue;
			}
			TimeSeriesDataImpl tsd = (TimeSeriesDataImpl) tsdIn;
			List<SampledValue> values = getValuesWithoutCalc(tsd.getTimeSeries(), null, startTime, endTime);
			int mysiz = values.size();
			int mysizNN = getNonNaNNum(values);
			String myName = getName(tsd);
			if(mysiz > maxSize) {
				maxSize = mysiz;
				maxName = myName;
			}
			if(mysiz < minSize) {
				minSize = mysiz;
				minName = myName;
			}
			countTs++;
			sizeSum += mysiz;
			sizeNNSum += mysizNN;
			if(listsToPrint.size() < maxTsToPrint)
				listsToPrint.add(tsd);
		}
		System.out.println("--TSSet:"+setName+"["+((nonImplCount>0)?tsdlist.size()+"!!NonImpl:"+nonImplCount:tsdlist.size())+"]::"
				+ "Av:"+(sizeSum/countTs)+", AvNonNaN:"+(sizeNNSum/countTs)+"  Max:"+maxSize+"/"+maxName+"  Min:"+minSize+"/"+minName);
		for(TimeSeriesDataImpl tsd: listsToPrint) {
			printFirstElements(tsd.getTimeSeries(), startTime, endTime);
		}
	}
	
	private static int getNonNaNNum(List<SampledValue> values) {
		int result = 0;
		for(SampledValue sv: values) {
			if(!Float.isNaN(sv.getValue().getFloatValue()))
				result++;
		}
		return result;
	}

	public static void printFirstElements(ReadOnlyTimeSeries ts) {
		if(ts instanceof ProcessedReadOnlyTimeSeries)
		printFirstElements(ts, DEFAULT_MAX_ELS, null, null);
	}
	public static void printFirstElements(ReadOnlyTimeSeries ts, Long startTime, Long endTime) {
		printFirstElements(ts, DEFAULT_MAX_ELS, startTime, endTime);
	}
	
	public static void printFirstElements(ReadOnlyTimeSeries ts, int maxEl) {
		printFirstElements(ts, maxEl, null, null);
	}
	public static void printFirstElements(ReadOnlyTimeSeries ts, int maxEl, Long startTime, Long endTime) {
		String name = getName(ts);
		printFirstElements(ts, maxEl, name, startTime, endTime);
	}
	
	static int counter = 0;
	protected static String getName(ReadOnlyTimeSeries ts) {
		String name;
		if(ts instanceof Schedule)
			name = ((Schedule)ts).getLocation();
		else if(ts instanceof RecordedData)
			name = ((RecordedData)ts).getPath();
		else 
			name = "TS2Plot_"+counter;
		counter ++;
		return name;
	}
	protected static String getName(TimeSeriesDataImpl tsd) {
		String name = tsd.label(null);
		if(name != null && !name.isEmpty()) {
			counter ++;
			return name;
		}
		return getName(tsd.getTimeSeries());
	}
	
	public static void printFirstElements(List<SampledValue> values, int maxEl) {
		String name = "TS2Plot_"+counter;
		printFirstElements(values, maxEl, name);
	}
	
	public static void printFirstElements(ReadOnlyTimeSeries ts, int maxEl, String name) {
		printFirstElements(ts, maxEl, name, null, null);
	}
	public static void printFirstElements(ReadOnlyTimeSeries ts, int maxEl, String name, Long startTime, Long endTime) {
		List<SampledValue> values = getValuesWithoutCalc(ts, null, startTime, endTime);
		printFirstElements(values, maxEl, name);
	}
	public static void printFirstElements(List<SampledValue> values, int maxEl, String name) {
		StringProvider<SampledValue> fhelp = new StringProvider<SampledValue>() {

			@Override
			public String label(SampledValue object) {
				return StringFormatHelper.getFullTimeDateInLocalTimeZone(object.getTimestamp())+":"+
						String.format("%.2f", object.getValue().getFloatValue());
			}
		};
		System.out.println("  TSPrint:"+name+"["+values.size()+"] : "+StringFormatHelper.getListToPrint(values, fhelp , maxEl));
	}
	
	public static List<SampledValue> getValuesWithoutCalc(ReadOnlyTimeSeries ts, Integer limitResSize, Long startTime, Long endTime) {
		List<SampledValue> result;
		if(ts instanceof ProcessedReadOnlyTimeSeries) {
			result = ((ProcessedReadOnlyTimeSeries)ts).getCurrentValues();
			if(result == null)
				return Collections.emptyList();
			int firstIdx = 0;
			int lastIdx = result.size()-1;
			boolean changed = false;
			if(startTime != null) {
				boolean found = false;
				for(int i=0; i<result.size(); i++) {
					if(result.get(i).getTimestamp() >= startTime) {
						found = true;
						changed = true;
						firstIdx = i;
						break;
					}
				}
			}
			if(endTime != null) {
				boolean found = false;
				for(int i=result.size()-1; i>=firstIdx; i--) {
					if(result.get(i).getTimestamp() <= endTime) {
						found = true;
						changed = true;
						lastIdx = i;
						break;
					}
				}
			}
			if(limitResSize != null && (lastIdx - firstIdx >= limitResSize)) {
				lastIdx = firstIdx + limitResSize - 1;
				changed = true;
			}
			if(changed) {
				result = result.subList(firstIdx, lastIdx+1);
			}
			return result; //ProcessedReadOnlySeries
		}
		SampledValue sv;
		if(startTime == null) {
			sv = ts.getNextValue(0);
			if(sv == null)
				return Collections.emptyList();
			startTime = sv.getTimestamp();
		}
		if(endTime == null) {
			sv = ts.getPreviousValue(Long.MAX_VALUE);
			if(sv == null)
				return Collections.emptyList();
			endTime = sv.getTimestamp();
		}
		result = ts.getValues(startTime, endTime);
		if(limitResSize != null && result.size() > limitResSize) {
			result = result.subList(0, limitResSize);
		}
		return result;
	}
}