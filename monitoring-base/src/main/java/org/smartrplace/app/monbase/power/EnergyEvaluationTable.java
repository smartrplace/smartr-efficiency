package org.smartrplace.app.monbase.power;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.gateway.EvalCollection;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.app.monbase.config.EnergyEvalInterval;
import org.smartrplace.app.monbase.power.ConsumptionEvalAdmin.SumType;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableLineI.EnergyEvalObjI;

import de.iwes.util.logconfig.EvalHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class EnergyEvaluationTable extends ConsumptionEvalTableBase<EnergyEvaluationTableLine> {

	@Override
	protected void configurePricingInformation() {
		FloatResource elPriceLoc;
		elPriceLoc = ResourceHelper.getSubResource(controller.appMan.getResourceAccess().getResource("master"),
				"editableData/buildingData/E_0/electricityPrice", FloatResource.class);
		if(elPriceLoc == null) {
			EvalCollection evalCollection = EvalHelper.getEvalCollection(appMan);
			elPriceLoc = evalCollection.getSubResource("elPrice", FloatResource.class);
		}
		elPrice = elPriceLoc;
		elPrice.create();
		if(!elPrice.isActive())
			elPrice.activate(false);
		FloatResource gasPriceLoc;
		gasPriceLoc = ResourceHelper.getSubResource(controller.appMan.getResourceAccess().getResource("master"),
				"editableData/buildingData/E_0/gasPrice", FloatResource.class);
		if(gasPriceLoc == null) {
			EvalCollection evalCollection = EvalHelper.getEvalCollection(appMan);
			gasPriceLoc = evalCollection.getSubResource("gasPrice", FloatResource.class);
		}
		gasPrice = gasPriceLoc;
		gasPrice.create();
		FloatResource gasEffLoc;
		gasEffLoc = ResourceHelper.getSubResource(controller.appMan.getResourceAccess().getResource("master"),
				"editableData/buildingData/E_0/heatingEfficiency", FloatResource.class);
		if(gasEffLoc == null) {
			EvalCollection evalCollection = EvalHelper.getEvalCollection(appMan);
			gasEffLoc = evalCollection.getSubResource("gasEff", FloatResource.class);
		}
		gasEff = gasEffLoc;
		gasEff.create();		
	}
	
	public EnergyEvaluationTable(WidgetPage<?> page, MonitoringController controller) {
		super(page, controller, new EnergyEvaluationTableLine((ElectricityConnection)null, "init", true, null, null, 0));
	}

	@Override
	public ElectricityConnection getResource(EnergyEvaluationTableLine object, OgemaHttpRequest req) {
		Resource res = object.conn.getMeterReadingResource();
		if(res instanceof ElectricityConnection)
			return (ElectricityConnection) res;
		return null;
		//return object.conn.getMeterReadingResource().conn;
	}
	
	@Override
	public Collection<EnergyEvaluationTableLine> getObjectsInTable(OgemaHttpRequest req) {
		EnergyEvalInterval intv = initResType.getSelectedItem(req);
		boolean lineShowsPower = !(intv.start().isActive());
		return getObjectsInTable(intv, lineShowsPower, req);
	}
	public Collection<EnergyEvaluationTableLine> getObjectsInTable(EnergyEvalInterval intv, boolean lineShowsPower, OgemaHttpRequest req) {
		List<EnergyEvaluationTableLine> result = new ArrayList<>();
		int lineCounter = 0;
		
		List<ConsumptionEvalTableLineI> rexoLines = new ArrayList<>();
		List<ConsumptionEvalTableLineI> pumpLines = new ArrayList<>();
		List<ConsumptionEvalTableLineI> heatLines = new ArrayList<>();
		//EnergyEvaluationTableLine rexoSum = new EnergyEvaluationTableLine(null, "Verteilung gesamt",
		//		lineShowsPower, null, SumType.SUM_LINE, null, intv, (lineCounter++));					
		//EnergyEvaluationTableLine pumpSum = new EnergyEvaluationTableLine(null, "Pumpen gesamt",
		//				lineShowsPower, null, SumType.SUM_LINE, null, intv, (lineCounter++));					
		EnergyEvaluationTableLine rexoSum = new EnergyEvaluationTableLine((ElectricityConnection)null, "Strom Summe Abgänge",
				lineShowsPower, SumType.SUM_LINE, rexoLines , (lineCounter++));					
		EnergyEvaluationTableLine pumpSum = new EnergyEvaluationTableLine((ElectricityConnection)null, "Pumpen gesamt",
						lineShowsPower, SumType.SUM_LINE, pumpLines, (lineCounter++));					
		EnergyEvaluationTableLine heatSum = new EnergyEvaluationTableLine((ElectricityConnection)null, "Wärme gesamt",
				lineShowsPower, SumType.SUM_LINE, heatLines, (lineCounter++));					

		//List<EnergyEvaluationTableLine> clearList = Arrays.asList(new EnergyEvaluationTableLine[] {rexoSum, pumpSum});
		//In first line we have to add clearList
		addRexoLine("electricityConnectionBox_A/connection",
				lineShowsPower, result, rexoLines, intv, lineCounter++, req);
		addRexoLine("electricityConnectionBox_C/connection",
				lineShowsPower, result, rexoLines, intv, lineCounter++, req);
		addRexoLine("electricityConnectionBox_B/connection",
				lineShowsPower, result, rexoLines, intv, lineCounter++, req);
		addRexoLine("electricityConnectionBox_D/connection",
				lineShowsPower, result, null, intv, lineCounter++, req);
		if(!lineShowsPower) {
			addMainMeterLine("master", "editableData/buildingData/E_0/electricityMeterCountValue/recordedDataParent/program",
					result, intv, lineCounter++);			
		}
		
		//add heat meters
		addHeatLine("_10120253",
				lineShowsPower, result, heatLines, intv, lineCounter++, req);
		addHeatLine("_39009357",
				lineShowsPower, result, heatLines, intv, lineCounter++, req);
		addHeatLine("_39009358",
				lineShowsPower, result, heatLines, intv, lineCounter++, req);
		
		Resource hm =  controller.appMan.getResourceAccess().getResource("HomeMaticIP");
		List<ElectricityConnection> hmconns = hm.getSubResources(ElectricityConnection.class, true);
		Map<String, ElectricityConnection> conns = new HashMap<String, ElectricityConnection>();
		for(ElectricityConnection conn: hmconns) {
			if(!useConnection(conn)) continue;
			String label = controller.getRoomLabel(conn.getLocation(), req.getLocale())+"-Pumpe";
			conns.put(label, conn);
		}
		List<String> sorted = new ArrayList<>(conns.keySet());
		sorted.sort(null);
		for(String label: sorted) {
			ElectricityConnection conn = conns.get(label);
			EnergyEvaluationTableLine newLine = new EnergyEvaluationTableLine(
					conn, label, lineShowsPower, SumType.STD, null, (lineCounter++));
			//result.add(new EnergyEvaluationTableLine(conn, label, lineShowsPower, pumpSum, SumType.STD, null, intv, (lineCounter++)));
			result.add(newLine);
			pumpLines.add(newLine);
		}
		result.add(rexoSum);					
		result.add(pumpSum);					
		result.add(heatSum);					
		/*addEvalLine("HomeMaticIP", "HomeMaticIP/devices/HM_HMIP_PSM_0001D3C99C4830/HM_SingleSwitchBox_0001D3C99C4830/electricityConnection",
				lineShowsPower, result);
		addEvalLine("HomeMaticIP", "HomeMaticIP/devices/HM_HMIP_PSM_0001D3C99C4847/HM_SingleSwitchBox_0001D3C99C4847/electricityConnection",
				lineShowsPower, result);*/
		return result;
	}

	protected static boolean useConnection(ElectricityConnection conn) {
		if(conn.isReference(false)) return false;
		if(conn.powerSensor().exists() || conn.energySensor().exists()) return true;
		return false;
	}
	
	protected EnergyEvaluationTableLine addRexoLine(String subPath, boolean lineShowsPower,
			List<EnergyEvaluationTableLine> result, List<ConsumptionEvalTableLineI> rexoLines,
			EnergyEvalInterval intv, int lineIdx, OgemaHttpRequest req) {
		//Resource rexo = controller.appMan.getResourceAccess().getResource(MAIN_ELECTRICITY_METER_RES);
		ElectricityConnection conn =controller.getElectrictiyMeterDevice(subPath); // ResourceHelper.getSubResource(rexo,
		//		subPath, ElectricityConnection.class);

		String label = controller.getRoomLabel(conn.getLocation(), req.getLocale());
		//EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(conn, label, lineShowsPower, rexoSum, SumType.STD, clearList, intv, lineIdx);
		EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(conn, label, lineShowsPower, SumType.STD, null, lineIdx);
		if(rexoLines != null)
			rexoLines.add(retVal);
		result.add(retVal);
		return retVal;
	}

	protected EnergyEvaluationTableLine addHeatLine(String subPath, boolean lineShowsPower,
			List<EnergyEvaluationTableLine> result, List<ConsumptionEvalTableLineI> heatLines,
			EnergyEvalInterval intv, int lineIdx, OgemaHttpRequest req) {
		//Resource rexo = controller.appMan.getResourceAccess().getResource(MAIN_HEAT_METER_RES);
		SensorDevice conn = controller.getHeatMeterDevice(subPath); //ResourceHelper.getSubResource(rexo,
		//		subPath, SensorDevice.class);

		String label = "Wärme "+controller.getRoomLabel(conn.getLocation(), req.getLocale());
		//EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(conn, label, lineShowsPower, rexoSum, SumType.STD, clearList, intv, lineIdx);
		EnergyEvalObjI connObj = new EnergyEvalHeatObj(conn);
		EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(connObj, label, lineShowsPower, SumType.STD, null, lineIdx);
		if(heatLines != null)
			heatLines.add(retVal);
		result.add(retVal);
		return retVal;
	}
	
	protected EnergyEvaluationTableLine addMainMeterLine(String topResName, String subPath,
			List<EnergyEvaluationTableLine> result,
			EnergyEvalInterval intv, int lineIdx) {
		Resource rexo = controller.appMan.getResourceAccess().getResource(topResName);
		Schedule sched = ResourceHelper.getSubResource(rexo,
				subPath, Schedule.class);
		
		String label = "Ablesung Hauptzähler Strom";
		EnergyEvalElConnObj connObj = new EnergyEvalElConnObj(null) {
			@Override
			public
			float getPowerValue() {
				return Float.NaN;
			}

			@Override
			public
			float getEnergyValue() {
				SampledValue val = sched.getPreviousValue(Long.MAX_VALUE);
				if(val == null) return Float.NaN;
				return val.getValue().getFloatValue();
			}
			
			@Override
			public
			float getEnergyValue(long startTime, long endTime, String label) {
				return getEnergyValue(sched, startTime, endTime, label);
			}
			
			@Override
			public
			boolean hasSubPhases() {
				return false;
			}
			
			@Override
			public
			boolean hasEnergySensor() {
				return true;
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
				return Float.NaN;
			}
			
			@Override
			public
			Resource getMeterReadingResource() {
				return sched;			
			}
			
		};
		//EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(conn, label, lineShowsPower, rexoSum, SumType.STD, clearList, intv, lineIdx);
		EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(connObj, label, false, SumType.STD, null, lineIdx);
		result.add(retVal);
		return retVal;
	}
}
