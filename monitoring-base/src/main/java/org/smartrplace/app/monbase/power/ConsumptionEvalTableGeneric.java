package org.smartrplace.app.monbase.power;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointInfo.UtilityType;
import org.ogema.devicefinder.util.DPUtil;
import org.ogema.devicefinder.util.DatapointInfoImpl;
import org.ogema.model.connections.ElectricityConnection;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.app.monbase.gui.TimeSeriesNameProviderImpl;
import org.smartrplace.app.monbase.power.ConsumptionEvalAdmin.SumType;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableLineI.CostProvider;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableLineI.EnergyEvalObjI;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.format.StringFormatHelper.StringProvider;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class ConsumptionEvalTableGeneric extends ConsumptionEvalTableBase<ConsumptionEvalTableLineBase> {

	//protected final UtilityType utility;
	protected final List<GaRoDataType> utilities;
	protected final TimeSeriesNameProviderImpl nameProvider;
	protected final List<UtilityType> utilsSorted;
	
	public static class LineInfo {
		public boolean isInTable = true;
		public String label;
		public UtilityType util = null;
		public CostProvider costProvider = null;
		public ConsumptionEvalTableLineBase tableLine = null;
		public Float factor = null;
	}
	public static class SumLineInfo extends LineInfo {
		public List<ConsumptionEvalTableLineI> subLines = new ArrayList<>();
		public ConsumptionEvalTableLineBase subSum;
		public String id;
	}
	
	public static class LineInfoDp extends LineInfo {
		public List<String> sumIds;
		public Datapoint dp;
		public EnergyEvalObjI conn = null;
		//TODO: Implement this later
		//boolean includePhases;
		//boolean includeCostInfo;
	}
	
	/** Overwrite default if necessary*/
	protected List<SumLineInfo> getSumLines() {
		Collection<UtilityType> utypes = getUtilityType();
		List<SumLineInfo> result = new ArrayList<>();
		for(UtilityType util: utypes) {
			SumLineInfo info = new SumLineInfo();
			info.id = util.name()+"_Sum";
			info.label = getSumLineLabel(util);
			info.util = util;
			info.costProvider = getCostProvider(util);
			result.add(info);
		}
		return result;
	}
	protected String getSumLineLabel(UtilityType util) {
		return "Sum "+util.toString();
	}
	
	/** Override if required
	 * Note that result.conn needs to be replaced if the datapoint resource is not a meter counter resource
	 * @param dpsPerUtilType */
	protected LineInfoDp getDpLineInfo(GaRoDataType gaRoDataTypeI, Datapoint dp,
			Map<UtilityType, List<LineInfo>> dpsPerUtilType) {
		LineInfoDp result = new LineInfoDp();
		result.isInTable = utilities.contains(gaRoDataTypeI);
		if(!result.isInTable)
			return result;
		result.dp = dp;
		UtilityType util = dp.info().getUtilityType(); //DatapointInfoImpl.getUtilityType(gaRoDataTypeI);
		if(util != null)
			result.sumIds = Arrays.asList(new String[] {util.name()+"_Sum"});
		else
			result.sumIds = null;
		result.util = util;
		if(dp.getResource() != null && dp.getResource() instanceof Schedule) {
			AggregationMode mode = dp.info().getAggregationMode();
			if(mode == null)
				mode = AggregationMode.Meter2Meter;
			result.conn = new EnergyEvalObjSchedule(dp, null,
					mode, controller);
		} else if(dp.getResource() == null) {
			ReadOnlyTimeSeries ts = dp.getTimeSeries();
			if(ts != null) {
				AggregationMode mode = dp.info().getAggregationMode();
				if(mode == null)
					mode = AggregationMode.Meter2Meter;
				//TimeSeriesDataImpl ts = UserServlet.knownTS.get(dp.getTimeSeriesID());
				//if(ts != null)
				result.conn = new EnergyEvalObjSchedule(dp, null, mode, controller);
			} else {
				result.isInTable = false;
				return result;
			}
		}
		result.costProvider = getCostProvider(util);
		return result;
	}

	public CostProvider getCostProvider(UtilityType util) {
		switch(util) {
		case ELECTRICITY:
			return new CostProvider() {
				@Override
				public String getCost(float value) {
					return (elPrice != null)?String.format("%.2f", value*elPrice.getValue()):"--";
				}
			};
		case HEAT_ENERGY:
			return new CostProvider() {
				@Override
				public String getCost(float value) {
					return ((gasPrice != null) && (gasEff != null))?
						String.format("%.2f", value*gasPrice.getValue()*gasEff.getValue()/100):"--";
				}
			};
		case WATER:
			return new CostProvider() {
				@Override
				public String getCost(float value) {
					return (waterprice != null)?String.format("%.2f", value*waterprice.getValue()):"--";
				}
			};
		case FOOD:	
			return new CostProvider() {
				@Override
				public String getCost(float value) {
					return (foodprice != null)?String.format("%.2f", value*foodprice.getValue()):"--";
				}
			};
		default:
			return null;
		}		
	}
	
	/** Sum lines are the first entries in each List in the map*/
	protected List<LineInfo> getSortedList(Map<UtilityType, List<LineInfo>> dpsPerUtilType) {
		List<LineInfo> result = new ArrayList<>();
		for(UtilityType util: UtilityType.values()) {
			List<LineInfo> lineList = dpsPerUtilType.get(util);
			if(lineList == null)
				continue;
			sortList(lineList);
			result.addAll(lineList);
		}
		return result;
	}

	protected void sortList(List<LineInfo> lineList) {
		lineList.sort(new Comparator<LineInfo>() {

			@Override
			public int compare(LineInfo o1, LineInfo o2) {
				if(o1 instanceof SumLineInfo && (!(o2 instanceof SumLineInfo)))
					return -1;
				if(o2 instanceof SumLineInfo && (!(o1 instanceof SumLineInfo)))
					return 1;
				return o1.label.compareTo(o2.label);
			}
		});		
	}
	
	public ConsumptionEvalTableGeneric(WidgetPage<?> page, MonitoringController controller, UtilityType utility) {
		super(page, controller, new ConsumptionEvalTableLineBase(null, null, false, null, null, 0));
		this.utilities = DatapointInfoImpl.getGaRotypes(utility);
		utilsSorted = new ArrayList<>();
		utilsSorted.add(utility);
		if(this.utilities == null)
			throw new IllegalStateException("Unknown utility:"+utility);
		nameProvider = new TimeSeriesNameProviderImpl(controller);
	}
	public ConsumptionEvalTableGeneric(WidgetPage<?> page, MonitoringController controller, UtilityType[] utilities) {
		super(page, controller, new ConsumptionEvalTableLineBase(null, null, false, null, null, 0));
		this.utilities = new ArrayList<>();
		utilsSorted = Arrays.asList(utilities);	
		nameProvider = new TimeSeriesNameProviderImpl(controller);
		for(UtilityType utility: utilities) {
			List<GaRoDataType> loc = DatapointInfoImpl.getGaRotypes(utility);
			if(loc == null)
				throw new IllegalStateException("Unknown utility:"+utility);
			this.utilities.addAll(loc);
		}
		if(this.utilities.isEmpty())
			throw new IllegalStateException("No GaRo type on page!!");
	}
	public ConsumptionEvalTableGeneric(WidgetPage<?> page, MonitoringController controller,
			List<GaRoDataType> utilities) {
		super(page, controller, new ConsumptionEvalTableLineBase(null, null, false, null, null, 0));
		this.utilities = utilities;
		utilsSorted = new ArrayList<>(getUtilityType());	
		nameProvider = new TimeSeriesNameProviderImpl(controller);
	}

	@Override
	public Collection<ConsumptionEvalTableLineBase> getAllObjectsInTable() {
DPUtil.printDatapointsOfType(GaRoDataType.HeatEnergyIntegral, controller.dpService);
		List<Datapoint> alldps = controller.dpService.getAllDatapoints();
		List<ConsumptionEvalTableLineBase> result = new ArrayList<>();
		int lineCounter = 0;

		Map<UtilityType, List<LineInfo>> dpsPerUtilType = new HashMap<>();

		Map<String, SumLineInfo> sumLines = new HashMap<>();
		for(SumLineInfo info: getSumLines()) {
			sumLines.put(info.id, info);
			List<LineInfo> lineList = getLineInfoList(info.util, dpsPerUtilType);
			lineList.add(info);
		}
		
		for(Datapoint dp: alldps) {
//System.out.println("DP Label: "+dp.id()+ "  /  "+dp.getGaroDataType().label(null));			
			//ConsumptionInfo conInfo = dp.getConsumptionInfo();
			if(dp.getResource() == null) {
				if(dp.getGaroDataType() == null)
					continue;
				AggregationMode aggMode = dp.info().getAggregationMode(); //ConsumptionInfo.getConsumptionMode(dp.getGaroDataType());
				if(aggMode == null || aggMode == AggregationMode.Power2Meter)
					continue;
			}
			//TODO: Support also AggregationMode.Consumption2Meter
			//if(conInfo.aggregationMode != AggregationMode.Meter2Meter)
			//	continue;
			LineInfoDp lineInfo = getDpLineInfo(dp.getGaroDataType(), dp, dpsPerUtilType);
			if(!lineInfo.isInTable)
				continue;
			//if(!(dp.getResource() instanceof FloatResource))
			//	continue;
			List<LineInfo> lineList = getLineInfoList(lineInfo.util, dpsPerUtilType);
			lineList.add(lineInfo);
		}
		List<LineInfo> lineList = getSortedList(dpsPerUtilType);
		for(LineInfo lineInfo: lineList) {
			if(lineInfo instanceof SumLineInfo) {
				SumLineInfo info = (SumLineInfo) lineInfo;
				info.subSum = new ConsumptionEvalTableLineBase(null, info.label,
						isPowerTable(), SumType.SUM_LINE, info.subLines, (lineCounter++)) {
					@Override
					public CostProvider getCostProvider() {
						return info.costProvider;
					}
				};
				info.subSum.factorEnergy = lineInfo.factor;
				result.add(info.subSum);
				continue;
			}
			LineInfoDp lineInfoDp = (LineInfoDp) lineInfo;
			Datapoint dp = lineInfoDp.dp;
			String label;
			if(lineInfo.label != null)
				label = lineInfo.label;
			else
				label = getLabel(dp);
			final ConsumptionEvalTableLineBase retVal;
			if(lineInfoDp.tableLine != null) {
				lineInfoDp.tableLine.setIndex(lineCounter++);
				result.add(lineInfoDp.tableLine);
				retVal = lineInfoDp.tableLine;
			} else if(lineInfoDp.conn != null)
				retVal = addLineBase(lineInfoDp.conn, null, isPowerTable(), result, (lineCounter++),
						label, dp, lineInfo.costProvider);
			else
				retVal = addLineBase((FloatResource)dp.getResource(), null, isPowerTable(), result, (lineCounter++),
					label, dp, lineInfo.costProvider);
			retVal.factorEnergy = lineInfoDp.factor;
			if(lineInfoDp.sumIds != null) for(String sumId: lineInfoDp.sumIds) {
				SumLineInfo sumLineInfo = sumLines.get(sumId);
				if(sumLineInfo != null)
					sumLineInfo.subLines.add(retVal);
			}
		}
		
		//for(SumLineInfo info: sumLines.values()) {
		//	result.add(info.subSum);
		//}
		return result;
	}

	protected List<LineInfo> getLineInfoList(UtilityType util, Map<UtilityType, List<LineInfo>> dpsPerUtilType) {
		if(util == null) 
			util = UtilityType.UNKNOWN;
		List<LineInfo> dpsList = dpsPerUtilType.get(util);
		if(dpsList == null) {
			dpsList = new ArrayList<>();
			dpsPerUtilType.put(util, dpsList);
		}
		return dpsList;
	}
	
	public String getLabel(Datapoint dp) {
		String label= dp.label();
		if(label == null) {
			label = nameProvider.getShortNameForTypeI(dp.getGaroDataType(), dp.getResource().getLocation());
			dp.setLabel(label);
		}
		return label;
	}
	
	@Override
	public Collection<UtilityType> getUtilityType() {
		Set<UtilityType> result = new LinkedHashSet<>();
		for(GaRoDataType type: getDataTypes()) {
			UtilityType util = DatapointInfoImpl.getUtilityType(type);
			if(util != null)
				result.add(util);
		}
		return result;
	}

	@Override
	public List<GaRoDataType> getDataTypes() {
		return utilities;
	}
	
	@Override
	protected String getHeaderText(OgemaHttpRequest req) {
		return "Consumption Evaluation Overview for "+StringFormatHelper.getListToPrint(utilities, new StringProvider<GaRoDataType>() {
			@Override
			public String label(GaRoDataType arg0) {
				return arg0.label(null);
			}
		});
	}

	@Override
	public ElectricityConnection getResource(ConsumptionEvalTableLineBase object, OgemaHttpRequest req) {
		return null;
	}

	@Override
	public Collection<ConsumptionEvalTableLineBase> getObjectsInTable(OgemaHttpRequest req) {
		return getAllObjectsInTable();
	}

}
