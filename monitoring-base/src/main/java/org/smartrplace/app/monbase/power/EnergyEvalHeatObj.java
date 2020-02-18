package org.smartrplace.app.monbase.power;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.sensors.EnergyAccumulatedSensor;
import org.ogema.model.sensors.PowerSensor;
import org.smartrplace.app.monbase.power.EnergyEvaluationTableLine.EnergyEvalObj;

public class EnergyEvalHeatObj extends EnergyEvalObj {
	protected final SensorDevice heatDevice;
	protected final FloatResource powerReading;
	protected final FloatResource energyReading;
	
	public EnergyEvalHeatObj(SensorDevice conn) {
		super(conn, false);
		//if(!(conn instanceof SensorDevice))
		//	throw new IllegalStateException("Wrong resource type for heat meter:"+conn.getResourceType());
		heatDevice = conn;
		FloatResource res = heatDevice.getSubResource("POWER_0_0", PowerSensor.class).reading();
		if(res.isActive()) {
			powerReading = res;
		} else
			powerReading = null;
		res = heatDevice.getSubResource("ENERGY_0_0", EnergyAccumulatedSensor.class).reading();
		if(res.isActive()) {
			energyReading = res;
		} else
			energyReading = null;
	}

	@Override
	float getPowerValue() {
		if(powerReading == null) return Float.NaN;
		return powerReading.getValue();
	}

	@Override
	float getEnergyValue() {
		if(energyReading == null) return Float.NaN;
		return energyReading.getValue();
	}
	@Override
	float getEnergyValue(long startTime, long endTime, String label) {
		if(energyReading == null) return Float.NaN;
		RecordedData recTs = energyReading.getHistoricalData();
		return getEnergyValue(recTs, startTime, endTime, label);
	}
	
	@Override
	boolean hasSubPhases() {
		return false;
	}
	
	@Override
	boolean hasEnergySensor() {
		return (energyReading != null);
	}
	
	@Override
	float getPowerValueSubPhase(int index) {
		return Float.NaN;
	}

	@Override
	float getEnergyValueSubPhase(int index) {
		return Float.NaN;
	}
	@Override
	float getEnergyValueSubPhase(int index, long startTime, long endTime) {
		return Float.NaN;
	}
	
	@Override
	Resource getMeterReadingResource() {
		return energyReading;
	}
}
