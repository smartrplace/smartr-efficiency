package org.ogema.timeseries.eval.simple.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DatapointImpl;
import org.smartrplace.app.monbase.gui.ProcessedReadOnlyTimeSeries;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.format.StringFormatHelper.StringProvider;

public class TimeProcUtil {
	public static int DEFAULT_MAX_ELS = 5;
	
	public static void printTimeSeriesSet(List<Datapoint> tsdlist, String setName, int maxTsToPrint, Long startTime, Long endTime) {
		int nonImplCount = 0;
		GoodData maxGood = null;
		//int maxSize = -1;
		//int maxNN = -1;
		//int maxNZ = -1;
		String maxName = "-";
		int minSize = Integer.MAX_VALUE;
		String minName = "-";
		int countTs = 0;
		int sizeSum = 0;
		int sizeNNSum = 0;
		int sizeNZSum = 0;
		List<Datapoint> listsToPrint = new ArrayList<>();
		for(Datapoint tsd: tsdlist) {
			if(!(tsd instanceof DatapointImpl)) {
				nonImplCount++;
				continue;
			}
			List<SampledValue> values = getValuesWithoutCalc(tsd.getTimeSeries(), null, startTime, endTime);
			GoodData good = getGoodNum(values);
			//int mysiz = (int) good.nonZeroNum;
			//int mysizNN = (int) good.nonNanNum;
			String myName = getName(tsd);
			if(maxGood == null || good.nonZeroNum > maxGood.nonZeroNum) {
				maxGood= good;
				maxName = myName;
			}
			if(good.totalNum < minSize) {
				minSize = (int) good.totalNum;
				minName = myName;
			}
			countTs++;
			sizeSum += good.totalNum;
			sizeNNSum += good.nonNanNum;
			sizeNZSum += good.nonZeroNum;
			if(listsToPrint.size() < maxTsToPrint)
				listsToPrint.add(tsd);
		}
		GoodData goodAv = new GoodData();
		goodAv.totalNum = sizeSum/countTs;
		goodAv.nonNanNum = sizeNNSum/countTs;
		goodAv.nonZeroNum = sizeNZSum/countTs;
		System.out.println("--TSSet:"+setName+"["+((nonImplCount>0)?tsdlist.size()+"!!NonImpl:"+nonImplCount:tsdlist.size())+"]::"
				+ "AvSize"+getGoodString(goodAv)+"  Max:"+getGoodStringInt(maxGood)+"/"+maxName+"  Min:"+minSize+"/"+minName);
		for(Datapoint tsd: listsToPrint) {
			printFirstElements(tsd.getTimeSeries(), startTime, endTime);
		}
	}
	
	public static class GoodData {
		public float totalNum;
		public float nonNanNum;
		public float nonZeroNum;
	}
	public static GoodData getGoodNum(List<SampledValue> values) {
		GoodData result = new GoodData();
		result.totalNum = values.size();
		for(SampledValue sv: values) {
			float val = sv.getValue().getFloatValue();
			if(!Float.isNaN(val)) {
				(result.nonNanNum)++;
				if(val != 0)
					(result.nonZeroNum)++;
			}
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
	protected static String getName(Datapoint tsd) {
		String name = tsd.label(null);
		if(name != null && !name.isEmpty()) {
			counter ++;
			return name;
		}
		return getName(tsd.getTimeSeries());
	}
	
	public static void printFirstElements(List<SampledValue> values, int maxEl) {
		String name = "TS2Plot_"+counter;
		printFirstElements(values, name, maxEl);
	}
	
	public static void printFirstElements(ReadOnlyTimeSeries ts, int maxEl, String name) {
		printFirstElements(ts, maxEl, name, null, null);
	}
	public static void printFirstElements(ReadOnlyTimeSeries ts, int maxEl, String name, Long startTime, Long endTime) {
		List<SampledValue> values = getValuesWithoutCalc(ts, null, startTime, endTime);
		printFirstElements(values, name, maxEl);
	}
	public static void printFirstElements(List<SampledValue> values, String name) {
		printFirstElements(values, name, DEFAULT_MAX_ELS);
	}
	public static void printFirstElements(List<SampledValue> values, String name, int maxEl) {
		StringProvider<SampledValue> fhelp = new StringProvider<SampledValue>() {

			@Override
			public String label(SampledValue object) {
				return StringFormatHelper.getFullTimeDateInLocalTimeZone(object.getTimestamp())+":"+
						String.format("%.2f", object.getValue().getFloatValue());
			}
		};
		GoodData good = getGoodNum(values);
		System.out.println("  TS:"+name+ getGoodStringInt(good)+ " : "+StringFormatHelper.getListToPrint(values, fhelp , maxEl));
	}
	public static String getGoodString(GoodData good) {
		return "["+good.totalNum+"/"+good.nonNanNum+"/"+good.nonZeroNum+"]";
	}
	public static String getGoodStringInt(GoodData good) {
		return "["+(int)(good.totalNum)+"/"+(int)(good.nonNanNum)+"/"+(int)(good.nonZeroNum)+"]";
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