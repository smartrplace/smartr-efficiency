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
import org.ogema.devicefinder.api.DpConnection;
import org.ogema.devicefinder.util.DatapointInfoImpl;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSimpleProcUtil;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.app.monbase.gui.TimeSeriesNameProviderImpl;
import org.smartrplace.app.monbase.power.ConsumptionEvalAdmin.SumType;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableLineI.EnergyEvalObjI;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.format.StringFormatHelper.StringProvider;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

/** Standard KPI page template
 * The lines provided are determined either via suitable datapoints or via {@link DpConnection}s, depending
 * on {@link #useConnections()}. The lines use {@link EnergyEvalObjI} instances for evaluation. For
 * schedules and time series that are not RecordedData usually an {@link EnergyEvalObjSchedule} is used
 * that uses TimeseriesSimpleProcUtil.METER_EVAL. This is also used for other {@link AggregationMode}s
 * then {@link AggregationMode#Meter2Meter}.<br>
 * Otherwise an {@link EnergyEvalObjBase} is used that just reads the meter values at start and end and calculates the
 * difference.<br>
 * Note that METER_EVAL uses one of the resources provided in
 * {@link TimeseriesSimpleProcUtil#getDefaultMeteringReferenceResource(org.ogema.core.resourcemanager.ResourceAccess)}.
 * to determine the reference time. If the resource does not exist it is created and set to the start time of the initial
 * evaluation.
 *
 * 
 * @author dnestle
 *
 */
public class ConsumptionEvalTableGeneric extends ConsumptionEvalTableBase<ConsumptionEvalTableLineBase> {

	//protected final UtilityType utility;
	protected final List<GaRoDataType> utilities;
	protected final TimeSeriesNameProviderImpl nameProvider;
	protected final List<UtilityType> utilsSorted;

	/** Exactly one of the two options should be non-null. Either a datapoint with time series is provided or
	 * a cost provider used to calculate the value based on the other actual values in the row (for now it is just
	 * the main value that is provided, should be changed in the future)
	 */
	/*public static class ColumnValue {
		public ColumnValue(Datapoint dp) {
			this.dp = dp;
			this.valueProvider = null;
		}
		public ColumnValue(CostProvider valueProvider) {
			this.dp = null;
			this.valueProvider = valueProvider;
		}
		Datapoint dp;
		CostProvider valueProvider;
	}*/
	
	/** Collection of configuration information for each line of the KPI table*/
	public static class LineInfo {
		public boolean isInTable = true;
		public String label;
		public UtilityType util = null;
		public ColumnDataProvider costProvider = null;
		public ConsumptionEvalTableLineBase tableLine = null;
		public Float factor = null;
		/** Only relevant if the respective phase num is configured. Additional datapoints will be
		 * inserted into additional phase columns. If phaseNum > 0 then the respective number of
		 * datapoints has to be delivered. Note that additional datapoints mean that an
		 * {@link EnergyEvalObjSchedule} is used even if an {@link EnergyEvalObjBase} would be sufficient otherwise.
		 * TODO: Providing CostProviders in the result list is not implemented yet in {@link ConsumptionEvalTableBase}*/
		public List<ColumnDataProvider> additionalDatapoints = new ArrayList<>();
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
	protected boolean useConnections() {
		return false;
	}
	
	/** Defines one sum line per {@link UtilityType}. Overwrite this in order to provide additional sum lines*/
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

	/** Overwrite this to change the default sum line labels. Custom sum lines usually should
	 * get their labels directly.
	 * @param util
	 * @return
	 */
	protected String getSumLineLabel(UtilityType util) {
		switch(util) {
		case ELECTRICITY:
			return "Sum Main Meters (kWh)";
		case HEAT_ENERGY:
			return "Sum Heat Energy (kWh)";
		case ENERGY_MIXED:
			return "Sum Electricity and Heat (kWh)";
		case WATER:
			return "Sum Fresh Water (m3)";
		case FOOD:
			return "Sum Food (kg)";
		default:
			throw new IllegalStateException("unknown UtilityType:"+util.toString());
		}
	}

	
	protected final TimeseriesSimpleProcUtil tsUtil;
	public ConsumptionEvalAdmin evalAdm = null;
	
	public ConsumptionEvalTableGeneric(WidgetPage<?> page, MonitoringController controller, UtilityType utility) {
		super(page, controller, new ConsumptionEvalTableLineBase(null, null, false, null, null, 0, (UtilityType)null));
		tsUtil = new TimeseriesSimpleProcUtil(controller.appMan, controller.dpService);
		this.utilities = DatapointInfoImpl.getGaRotypes(utility);
		utilsSorted = new ArrayList<>();
		utilsSorted.add(utility);
		if(this.utilities == null)
			throw new IllegalStateException("Unknown utility:"+utility);
		nameProvider = new TimeSeriesNameProviderImpl(controller);
	}
	public ConsumptionEvalTableGeneric(WidgetPage<?> page, MonitoringController controller, UtilityType[] utilities) {
		super(page, controller, new ConsumptionEvalTableLineBase(null, null, false, null, null, 0, (UtilityType)null));
		tsUtil = new TimeseriesSimpleProcUtil(controller.appMan, controller.dpService);
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
		super(page, controller, new ConsumptionEvalTableLineBase(null, null, false, null, null, 0, (UtilityType)null));
		tsUtil = new TimeseriesSimpleProcUtil(controller.appMan, controller.dpService);
		this.utilities = utilities;
		utilsSorted = new ArrayList<>(getUtilityType());	
		nameProvider = new TimeSeriesNameProviderImpl(controller);
	}
	
	/** Override if required, e.g. in order to determine shich lines shall be used, to change factors,
	 *  assignment of lines to sim lines etc.
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
		/*if(hasSubPhaseNum() > 0) {
			List<ColumnValue> phaseValData = getAdditionalDatapoints(dp);
			boolean hasAdditionalDp = false;
			for(ColumnValue cv: phaseValData) {
				if(cv.dp != null) {
					hasAdditionalDp = true;
					break;
				}
			}
			if(hasAdditionalDp) {
				ReadOnlyTimeSeries ts = dp.getTimeSeries();
				if(ts != null) {
					AggregationMode mode = dp.info().getAggregationMode();
					if(mode == null)
						mode = AggregationMode.Meter2Meter;
					result.conn = new EnergyEvalObjSchedule(dp, null, mode, tsUtil,
							phaseValData) {
						@Override
						public int hasSubPhaseNum() {
							return ConsumptionEvalTableGeneric.this.hasSubPhaseNum();
						}
					};
				} else {
					result.isInTable = false;
					return result;
				}
			}
		}*/
		AggregationMode mode = dp.info().getAggregationMode();
		if(mode == null)
			mode = AggregationMode.Meter2Meter;
		if(dp.getResource() != null && ((dp.getResource() instanceof Schedule) || (mode != AggregationMode.Meter2Meter))) {
			result.conn = new EnergyEvalObjSchedule(dp, null,
					mode, tsUtil);
		} else if(dp.getResource() == null) {
			ReadOnlyTimeSeries ts = dp.getTimeSeries();
			if(ts != null) {
				//TimeSeriesDataImpl ts = UserServlet.knownTS.get(dp.getTimeSeriesID());
				//if(ts != null)
				result.conn = new EnergyEvalObjSchedule(dp, null, mode, tsUtil);
			} else {
				result.isInTable = false;
				return result;
			}
		}
		result.costProvider = getCostProvider(util);
		return result;
	}

	public ColumnDataProvider getCostProvider(UtilityType util) {
		switch(util) {
		case ELECTRICITY:
			return new ColumnDataProvider() {
				@Override
				public float getValue(float lineMainValue, long startTime, long endTime) {
					return (elPrice != null)?lineMainValue*elPrice.getValue():Float.NaN;
				}
				@Override
				public String getString(float value) {
					return (elPrice != null)?String.format("%.2f", value):"--";
				}
			};
		case HEAT_ENERGY:
			return new ColumnDataProvider() {
				@Override
				public float getValue(float lineMainValue, long startTime, long endTime) {
					return ((gasPrice != null) && (gasEff != null))?
							(lineMainValue*gasPrice.getValue()*gasEff.getValue()/100):Float.NaN;
				}
				@Override
				public String getString(float value) {
					return ((gasPrice != null) && (gasEff != null))?
						String.format("%.2f", value):"--";
				}
			};
		case WATER:
			return new ColumnDataProvider() {
				@Override
				public float getValue(float lineMainValue, long startTime, long endTime) {
					return (waterprice != null)?(lineMainValue*waterprice.getValue()):Float.NaN;
				}
				@Override
				public String getString(float value) {
					return (waterprice != null)?String.format("%.2f", value):"--";
				}
			};
		case FOOD:	
			return new ColumnDataProvider() {
				@Override
				public float getValue(float lineMainValue, long startTime, long endTime) {
					return (foodprice != null)?(lineMainValue*foodprice.getValue()):Float.NaN;
				}
				@Override
				public String getString(float value) {
					return (foodprice != null)?String.format("%.2f", value):"--";
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

	protected static final Comparator<LineInfo> lineComparator = new Comparator<LineInfo>() {

		@Override
		public int compare(LineInfo o1, LineInfo o2) {
			if(o1 instanceof SumLineInfo && (!(o2 instanceof SumLineInfo)))
				return -1;
			if(o2 instanceof SumLineInfo && (!(o1 instanceof SumLineInfo)))
				return 1;
			return o1.label.compareTo(o2.label);
		}
	};
	protected void sortList(List<LineInfo> lineList) {
		lineList.sort(lineComparator);		
	}
	

	@Override
	public Collection<ConsumptionEvalTableLineBase> getAllObjectsInTable() {
//DPUtil.printDatapointsOfType(GaRoDataType.HeatEnergyIntegral, controller.dpService);
		List<ConsumptionEvalTableLineBase> result = new ArrayList<>();
		int lineCounter = 0;

		Map<UtilityType, List<LineInfo>> dpsPerUtilType = new HashMap<>();

		Map<String, SumLineInfo> sumLines = new HashMap<>();
		for(SumLineInfo info: getSumLines()) {
			sumLines.put(info.id, info);
			List<LineInfo> lineList = getLineInfoList(info.util, dpsPerUtilType);
			lineList.add(info);
		}
		
		List<Datapoint> alldps;
		if(useConnections()) {
			alldps = new ArrayList<>();
			for(DpConnection conn: controller.dpService.getConnections(null)) {
				if(isPowerTable() && conn.getPowerSensorDp() != null)
					alldps.add(conn.getPowerSensorDp());
				else if((!isPowerTable()) && conn.getEnergySensorDp() != null)
					alldps.add(conn.getEnergySensorDp());
				else if(conn.getPowerSensorDp() != null)
					alldps.add(conn.getPowerSensorDp());
				//for now we do not support energy sensors for power pages
			}
		} else
			alldps = controller.dpService.getAllDatapoints();
		for(Datapoint dp: alldps) {
//System.out.println("DP Label: "+dp.id()+ "  /  "+dp.getGaroDataType().label(null));			
			//ConsumptionInfo conInfo = dp.getConsumptionInfo();
			if(dp.getResource() == null) {
				if(dp.getGaroDataType() == null)
					continue;
				AggregationMode aggMode = dp.info().getAggregationMode(); //ConsumptionInfo.getConsumptionMode(dp.getGaroDataType());
				if(aggMode == null) // || aggMode == AggregationMode.Power2Meter)
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
						isPowerTable(), SumType.SUM_LINE, info.subLines, (lineCounter++), info.util) {
					@Override
					public ColumnDataProvider getCostProvider() {
						return info.costProvider;
					}
					@Override
					public int hasSubPhaseNum() {
						return ConsumptionEvalTableGeneric.this.hasSubPhaseNum();
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
					label, dp, lineInfo.costProvider, tsUtil);
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
		if(evalAdm != null)
			evalAdm.update(this, false);
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
		String label= dp.labelDefault();
		if(label == null) {
			label = nameProvider.getShortNameForTypeI(dp.getGaroDataType(), dp.getResource().getLocation());
			dp.setLabel(label, null);
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
	protected String getShortLabel() {
		return "KPI";
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
