package org.smartrplace.app.monbase.power;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.devicefinder.api.ConsumptionInfo;
import org.ogema.devicefinder.api.ConsumptionInfo.AggregationMode;
import org.ogema.devicefinder.api.ConsumptionInfo.UtilityType;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.app.monbase.power.ConsumptionEvalAdmin.SumType;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableBase;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableLineBase;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableLineI;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class ConsumptionEvalTableGeneric extends ConsumptionEvalTableBase<ConsumptionEvalTableLineBase> {

	protected final UtilityType utility;
	
	protected boolean isInTable(UtilityType utilityType) {
		if(utility == utilityType)
			return true;
		if(utility == UtilityType.ENERGY_MIXED &&
				(utilityType == UtilityType.ELECTRICITY || utilityType == UtilityType.HEAT_ENERGY))
			return true;
		return false;
	}

	public ConsumptionEvalTableGeneric(WidgetPage<?> page, MonitoringController controller, UtilityType utility) {
		super(page, controller, new ConsumptionEvalTableLineBase(null, null, false, null, null, 0));
		this.utility = utility;
	}

	@Override
	public Collection<ConsumptionEvalTableLineBase> getAllObjectsInTable() {
		List<Datapoint> alldps = controller.dpService.getAllDatapoints();
		List<ConsumptionEvalTableLineBase> result = new ArrayList<>();
		int lineCounter = 0;

		List<ConsumptionEvalTableLineI> subLines = new ArrayList<>();
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
		}
		
		for(Datapoint dp: alldps) {
			ConsumptionInfo conInfo = dp.getConsumptionInfo();
			if(conInfo != null && dp.getResource() != null) {
				//TODO: Support also AggregationMode.Consumption2Meter
				if(conInfo.aggregationMode != AggregationMode.Meter2Meter)
					continue;
				if(!isInTable(conInfo.utilityType))
					continue;
				if(conInfo.utilityType == UtilityType.ELECTRICITY) {
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
				}
			}
		}
		if(utility == UtilityType.ENERGY_MIXED) {
			subLines.addAll(elLines);
			subLines.addAll(heatLines);
		}
		
		if(!isPowerTable()) {
			//TODO: This does not work like this
			addMainMeterLineBase("master", "editableData/buildingData/E_0/electricityMeterCountValue/recordedDataParent/program",
					result, lineCounter++, null);			
		}
		
		result.add(subSum);
		if(utility == UtilityType.ENERGY_MIXED) {
			result.add(elSum);					
			result.add(heatSum);					
		}
		return result;
	}

	@Override
	public UtilityType getUtilityType() {
		return utility;
	}

	@Override
	protected String getHeaderText(OgemaHttpRequest req) {
		return utility.toString()+" Consumption Evaluation Overview";
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
