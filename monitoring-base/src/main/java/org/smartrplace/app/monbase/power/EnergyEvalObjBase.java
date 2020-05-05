package org.smartrplace.app.monbase.power;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.recordeddata.RecordedData;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableLineI.EnergyEvalObjI;

public class EnergyEvalObjBase implements EnergyEvalObjI {
	protected final FloatResource powerReading;
	protected final FloatResource energyReading;
	
	/**
	 * 
	 * @param energyReading
	 * @param powerReading may be null
	 */
	public EnergyEvalObjBase(FloatResource energyReading, FloatResource powerReading) {
		this.energyReading = energyReading;
		this.powerReading = powerReading;
	}

	@Override
	public
	float getPowerValue() {
		if(powerReading == null) return Float.NaN;
		return powerReading.getValue();
	}

	@Override
	public
	float getEnergyValue() {
		if(energyReading == null) return Float.NaN;
		return energyReading.getValue();
	}
	@Override
	public
	float getEnergyValue(long startTime, long endTime, String label) {
		if(energyReading == null) return Float.NaN;
		RecordedData recTs = energyReading.getHistoricalData();
		return EnergyEvalElConnObj.getEnergyValue(recTs, startTime, endTime, label);
	}
	
	@Override
	public
	int hasSubPhaseNum() {
		return 0;
	}
	
	@Override
	public
	boolean hasEnergySensor() {
		return (energyReading != null);
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
		return energyReading;
	}
}
