package org.smartrplace.app.monbase.power;

import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.model.connections.ElectricityConnection;
import org.smartrplace.app.monbase.config.EnergyEvalInterval;
import org.smartrplace.app.monbase.config.EnergyEvalIntervalMeterData;

public class EnergyEvaluationTableLine {
	public static interface EnergyEvalSessionDataProvider {
		//TODO: Not used currently
	}

	private static final long MAX_CACHE_TIME = 2000;
	
	protected final EnergyEvalSessionDataProvider sessionDataProvider;
	
	public static class EnergyEvalObj {
		protected final ElectricityConnection conn;

		//Implementation for Electricity
		public EnergyEvalObj(Resource conn) {
			this(conn, true);
		}
		public EnergyEvalObj(Resource conn, boolean isElectricityStd) {
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
		
		float getPowerValue() {
			return conn.powerSensor().reading().getValue();
		}

		float getEnergyValue() {
			return conn.energySensor().reading().getValue();
		}
		float getEnergyValue(long startTime, long endTime, String printVals) {
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
		
		boolean hasSubPhases() {
			return conn.subPhaseConnections().isActive();
		}
		
		boolean hasEnergySensor() {
			if(conn == null) return false;
			return conn.energySensor().reading().exists();
		}
		
		float getPowerValueSubPhase(int index) {
			ElectricityConnection subConn = conn.subPhaseConnections().getSubResource(
					"phase"+index, ElectricityConnection.class);
			return subConn.powerSensor().reading().getValue();
		}

		float getEnergyValueSubPhase(int index) {
			ElectricityConnection subConn = conn.subPhaseConnections().getSubResource(
					"phase"+index, ElectricityConnection.class);
			return subConn.energySensor().reading().getValue();
		}
		float getEnergyValueSubPhase(int index, long startTime, long endTime) {
			ElectricityConnection subConn = conn.subPhaseConnections().getSubResource(
					"phase"+index, ElectricityConnection.class);
			RecordedData ts = subConn.energySensor().reading().getHistoricalData();
			return getEnergyValue(ts, startTime, endTime, null);
		}
		
		Resource getMeterReadingResource() {
			return conn.energySensor().reading();			
		}
	}
	
	protected final EnergyEvalObj conn;
	protected final EnergyEvalInterval eeInterval;
	protected final boolean lineShowsPower;
	protected final String label;
	//protected final float[] sumUps;
	// line which counts the sumup for the current line
	//protected final EnergyEvaluationTableLine sumUpIndex2;
	protected final List<EnergyEvaluationTableLine> sourcesToSum;
	//protected final List<EnergyEvaluationTableLine> sumLinesToClear;
	//protected final ApplicationManager appMan;
	
	protected final float[] lastValues;
	protected long lastUpdateTime = -1;
	
	public static enum SumType {
		STD,
		INIT,
		SUM_LINE
	}
	protected final SumType type;
	protected final String index;
	
	/** 
	 * 
	 * @param conn
	 * @param label
	 * @param lineShowsPower
	 * @param sumUpIndex a sumup is a value that is calculated as the sum  of several other lines. The
	 * sumUpIndex indicates to which sum the line shall contribute. Sumup indices must be used consequently
	 * starting from zero.
	 * @param sumUpResult if not null the line is a sumup result line. The line must be ordered after all
	 * contributors are processed. If negative the line is the init line in which all sumUps are reset.
	 */
	public EnergyEvaluationTableLine(ElectricityConnection conn, String label, boolean lineShowsPower,
			//EnergyEvaluationTableLine sumUpIndex,
			SumType type,
			List<EnergyEvaluationTableLine> sourcesToSum,
			//List<EnergyEvaluationTableLine> sumLinesToClear,
			EnergyEvalInterval eeInterval, int index,
			EnergyEvalSessionDataProvider sessionDataProvider) {
		this(new EnergyEvalObj(conn), label, lineShowsPower, type, sourcesToSum, eeInterval, index,
				sessionDataProvider);
	}
	public EnergyEvaluationTableLine(EnergyEvalObj conn, String label, boolean lineShowsPower,
			//EnergyEvaluationTableLine sumUpIndex,
			SumType type,
			List<EnergyEvaluationTableLine> sourcesToSum,
			//List<EnergyEvaluationTableLine> sumLinesToClear,
			EnergyEvalInterval eeInterval, int index,
			EnergyEvalSessionDataProvider sessionDataProvider) {
		this.conn = conn;
		this.lineShowsPower = lineShowsPower;
		this.label = label;
		//this.sumUpIndex2 = sumUpIndex;
		this.type = type;
		this.sourcesToSum = sourcesToSum;
		//this.sumLinesToClear = sumLinesToClear;
		this.index = String.format("%06d", index);
		if(type == SumType.SUM_LINE)
			lastValues = null;
		//	sumUps = new float[4];
		else
			lastValues = new float[4];
		//	sumUps = null;
		this.eeInterval = eeInterval;
		this.sessionDataProvider = sessionDataProvider;
	}

	/** 0: overall, 1: L1, 2: L2, 3: L3*/
	public float getPhaseValue(int index, long startTime, long endTime, long now, List<EnergyEvaluationTableLine> allLines) {
		if(type == SumType.SUM_LINE) {
			float val = 0;
			for(EnergyEvaluationTableLine source: sourcesToSum) {
				float sval = source.lastValues[index];
				if(!Float.isNaN(sval))
					val += source.lastValues[index];
			}
			return val;
		} else {
			synchronized(this) {
				if(now - lastUpdateTime > MAX_CACHE_TIME) {
					for(EnergyEvaluationTableLine line: allLines) {
						if(line.type == SumType.SUM_LINE)
							break;
						for(int indexAll=0; indexAll<=3; indexAll++) {
							float value = line.getPhaseValueInternal(indexAll, startTime, endTime);
							line.lastValues[indexAll] = value;
						}
					}
					lastUpdateTime = now;
				} 
				return lastValues[index];
				//float value = getPhaseValueInternal(index, startTime, endTime);
				//lastValues[index] = value;
				//return value;
			}
		}
	}
	protected float getPhaseValueInternal(int index, long startTime, long endTime) {
		if(lineShowsPower) {
			if(index == 0) {
				return conn.getPowerValue();
			} else if(conn.hasSubPhases()) {
				return conn.getPowerValueSubPhase(index);
			}
		} else {
			//if(startTime != eeInterval.start().getValue()) {
			if(index == 0) {
				return conn.getEnergyValue(startTime, endTime, label);
			} else if(conn.hasSubPhases()) {
				return conn.getEnergyValueSubPhase(index, startTime, endTime);
			}
			return Float.NaN;
			/*}
			float startVal = 999999999;
			if(eeInterval != null) {
				EnergyEvaluationIntervalMeterData meter = getMeterData(conn, eeInterval, index);
				if(meter != null) {
					if(meter.energyConsumedInInterval().isActive())
						return meter.energyConsumedInInterval().getValue();
					else startVal = meter.startCounterValue().getValue();
				} else return Float.NaN;
			}
			Float rawCounter = null;
			if(index == 0) {
				rawCounter = conn.getEnergyValue();
			} else if(conn.hasSubPhases()) {
				rawCounter = conn.getEnergyValueSubPhase(index);
			}
			if(rawCounter == null) return Float.NaN;
			return rawCounter - startVal;*/
		}
		return Float.NaN;
	};
	
	public String getLabel() {
		return label;
	}
	
	public static EnergyEvalIntervalMeterData getMeterData(EnergyEvalObj conn,
			EnergyEvalInterval eeInt, int index) {
		//We do not support sub phase meter counter saving yet
		if(index != 0) return null;
		
		for(EnergyEvalIntervalMeterData meter: eeInt.meterData().getAllElements()) {
			if(meter.conn().equalsLocation(conn.getMeterReadingResource())) {
				return meter;
			}
		}
		return null;
	}
}
