package org.smartrplace.app.monbase.power;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.devicefinder.api.ConsumptionInfo;
import org.ogema.devicefinder.api.ConsumptionInfo.UtilityType;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.model.connections.ElectricityConnection;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.app.monbase.power.ConsumptionEvalAdmin.SumType;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.format.StringFormatHelper.StringProvider;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class ConsumptionEvalTableGeneric extends ConsumptionEvalTableBase<ConsumptionEvalTableLineBase> {

	//protected final UtilityType utility;
	protected final List<GaRoDataType> utilities;
	
	public static class SumLineInfo {
		List<ConsumptionEvalTableLineI> subLines = new ArrayList<>();
		ConsumptionEvalTableLineBase subSum;
		String id;
		String label;
	}
	
	public static class DatapointStatus {
		public boolean isInTable;
		public List<String> sumIds;
		
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
			info.id = util.name();
			info.label = "Sum "+util.toString();
			result.add(info);
		}
		return result;
	}
	
	/** Override if required*/
	protected DatapointStatus isInTable(GaRoDataTypeI gaRoDataTypeI, Datapoint dp) {
		DatapointStatus result = new DatapointStatus();
		result.isInTable = utilities.contains(gaRoDataTypeI);
		result.sumIds = null;
		return result;
	}

	public ConsumptionEvalTableGeneric(WidgetPage<?> page, MonitoringController controller, UtilityType utility) {
		super(page, controller, new ConsumptionEvalTableLineBase(null, null, false, null, null, 0));
		this.utilities = ConsumptionInfo.typeByUtility.get(utility);
		if(this.utilities == null)
			throw new IllegalStateException("Unknown utility:"+utility);
	}
	public ConsumptionEvalTableGeneric(WidgetPage<?> page, MonitoringController controller, UtilityType[] utilities) {
		super(page, controller, new ConsumptionEvalTableLineBase(null, null, false, null, null, 0));
		this.utilities = new ArrayList<>();
		for(UtilityType utility: utilities) {
			List<GaRoDataType> loc = ConsumptionInfo.typeByUtility.get(utility);
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
	}

	@Override
	public Collection<ConsumptionEvalTableLineBase> getAllObjectsInTable() {
		List<Datapoint> alldps = controller.dpService.getAllDatapoints();
		List<ConsumptionEvalTableLineBase> result = new ArrayList<>();
		int lineCounter = 0;

		Map<String, SumLineInfo> sumLines = new HashMap<>();
		for(SumLineInfo info: getSumLines()) {
			info.subSum = new ConsumptionEvalTableLineBase(null, info.label,
					isPowerTable(), SumType.SUM_LINE, info.subLines, (lineCounter++));
			sumLines.put(info.id, info);
		}
		
		/*List<ConsumptionEvalTableLineI> subLines = new ArrayList<>();
		List<ConsumptionEvalTableLineI> elLines = new ArrayList<>();
		List<ConsumptionEvalTableLineI> heatLines = new ArrayList<>();
		ConsumptionEvalTableLineBase elSum = null;
		ConsumptionEvalTableLineBase heatSum = null;
		ConsumptionEvalTableLineBase subSum = new ConsumptionEvalTableLineBase(null, "Sum Submeter",
				isPowerTable(), SumType.SUM_LINE, subLines , (lineCounter++));
		if(utility == UtilityType.ENERGY_MIXED) {
			elSum = new ConsumptionEvalTableLineBase(null, "Sum Electricity",
					isPowerTable(), SumType.SUM_LINE, elLines , (lineCounter++));
			heatSum = new ConsumptionEvalTableLineBase(null, "Sum Heat",
					isPowerTable(), SumType.SUM_LINE, heatLines , (lineCounter++));			
		}*/
		
		for(Datapoint dp: alldps) {
			ConsumptionInfo conInfo = dp.getConsumptionInfo();
			if(conInfo != null && dp.getResource() != null) {
				//TODO: Support also AggregationMode.Consumption2Meter
				//if(conInfo.aggregationMode != AggregationMode.Meter2Meter)
				//	continue;
				DatapointStatus lineInfo = isInTable(dp.getGaroDataType(), dp);
				if(!lineInfo.isInTable)
					continue;
				if(!(dp.getResource() instanceof FloatResource))
					continue;
				ConsumptionEvalTableLineBase retVal = addLineBase((FloatResource)dp.getResource(), null, isPowerTable(), result, (lineCounter++),
						dp.label(null), dp);
				for(String sumId: lineInfo.sumIds) {
					SumLineInfo sumLineInfo = sumLines.get(sumId);
					if(sumLineInfo != null)
						sumLineInfo.subLines.add(retVal);
				}
				/*if(conInfo.utilityType == UtilityType.ELECTRICITY) {
					ElectricityConnection conn = ResourceUtils.getFirstParentOfType(dp.getResource(), ElectricityConnection.class);
					if(conn == null)
						continue;
					addRexoLineBase(conn , isPowerTable(), result, (elLines!=null)?elLines:subLines, (lineCounter++), null, dp);
				} else if(conInfo.utilityType == UtilityType.HEAT_ENERGY) {
					SensorDevice conn = ResourceUtils.getFirstParentOfType(dp.getResource(), SensorDevice.class);
					if(conn == null)
						continue;
					addHeatLineBase(conn , isPowerTable(), result, (heatLines!=null)?heatLines:subLines, (lineCounter++), null, dp);
				} else if(conInfo.utilityType == UtilityType.WATER) {
					if(!(dp.getResource() instanceof FloatResource))
						continue;
					addLineBase((FloatResource)dp.getResource(), null, isPowerTable(), result, subLines, (lineCounter++),
							dp.label(null)+"(FW)", dp);
				} else if(conInfo.utilityType == UtilityType.FOOD) {
					if(!(dp.getResource() instanceof FloatResource))
						continue;
					addLineBase((FloatResource)dp.getResource(), null, isPowerTable(), result, subLines, (lineCounter++),
							dp.label(null)+"(Food)", dp);
				}*/
			}
		}
		/*if(utility == UtilityType.ENERGY_MIXED) {
			subLines.addAll(elLines);
			subLines.addAll(heatLines);
		}*/
		
		if(!isPowerTable()) {
			//TODO: This does not work like this
			addMainMeterLineBase("master", "editableData/buildingData/E_0/electricityMeterCountValue/recordedDataParent/program",
					result, lineCounter++, null);			
		}
		
		for(SumLineInfo info: sumLines.values()) {
			result.add(info.subSum);
		}
		/*result.add(subSum);
		if(utility == UtilityType.ENERGY_MIXED) {
			result.add(elSum);					
			result.add(heatSum);					
		}*/
		return result;
	}

	@Override
	public Collection<UtilityType> getUtilityType() {
		Set<UtilityType> result = new HashSet<>();
		for(GaRoDataType type: getDataTypes()) {
			for(Entry<UtilityType, List<GaRoDataType>> tlist: ConsumptionInfo.typeByUtility.entrySet()) {
				if(tlist.getValue().contains(type))
					result.add(tlist.getKey());
			}
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
