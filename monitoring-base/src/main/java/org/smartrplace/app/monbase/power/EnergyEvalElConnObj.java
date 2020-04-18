package org.smartrplace.app.monbase.power;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.model.connections.ElectricityConnection;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableLineI.EnergyEvalObjI;

public class EnergyEvalElConnObj implements EnergyEvalObjI {
	protected final ElectricityConnection conn;

	//Implementation for Electricity
	public EnergyEvalElConnObj(Resource conn) {
		this(conn, true);
	}
	public EnergyEvalElConnObj(Resource conn, boolean isElectricityStd) {
		if(conn == null)
			this.conn = null;
		else if(!isElectricityStd)
			this.conn = null;
		else {
			if(!(conn instanceof ElectricityConnection))
				throw new IllegalStateException("Wrong resource type"+conn.getResourceType());
			this.conn = (ElectricityConnection) conn;
		}
	}
	
	@Override
	public float getPowerValue() {
		return conn.powerSensor().reading().getValue();
	}

	@Override
	public float getEnergyValue() {
		return conn.energySensor().reading().getValue();
	}
	@Override
	public float getEnergyValue(long startTime, long endTime, String printVals) {
		RecordedData ts = conn.energySensor().reading().getHistoricalData();
		return getEnergyValue(ts, startTime, endTime, printVals);
	}
	protected static float getEnergyValue(ReadOnlyTimeSeries ts, long startTime, long endTime,
			String printVals) {
		SampledValue startV = ts.getNextValue(startTime);
		if(startV == null) return Float.NaN;
		SampledValue endV = ts.getNextValue(endTime);
		if(endV == null)
			endV = ts.getPreviousValue(endTime);
		if(endV == null) return Float.NaN;
		
		//if(printVals != null)
		//	System.out.println(printVals+": endV:"+endV.getValue().getFloatValue()+ "startV:"+startV.getValue().getFloatValue());
		return endV.getValue().getFloatValue() - startV.getValue().getFloatValue();			
	}
	
	@Override
	public boolean hasSubPhases() {
		return conn.subPhaseConnections().isActive();
	}
	
	@Override
	public boolean hasEnergySensor() {
		if(conn == null) return false;
		return conn.energySensor().reading().exists();
	}
	
	@Override
	public float getPowerValueSubPhase(int index) {
		ElectricityConnection subConn = conn.subPhaseConnections().getSubResource(
				"phase"+index, ElectricityConnection.class);
		return subConn.powerSensor().reading().getValue();
	}

	@Override
	public float getEnergyValueSubPhase(int index) {
		ElectricityConnection subConn = conn.subPhaseConnections().getSubResource(
				"phase"+index, ElectricityConnection.class);
		return subConn.energySensor().reading().getValue();
	}
	@Override
	public float getEnergyValueSubPhase(int index, long startTime, long endTime) {
		ElectricityConnection subConn = conn.subPhaseConnections().getSubResource(
				"phase"+index, ElectricityConnection.class);
		RecordedData ts = subConn.energySensor().reading().getHistoricalData();
		return getEnergyValue(ts, startTime, endTime, null);
	}
	
	@Override
	public Resource getMeterReadingResource() {
		return conn.energySensor().reading();			
	}

}
