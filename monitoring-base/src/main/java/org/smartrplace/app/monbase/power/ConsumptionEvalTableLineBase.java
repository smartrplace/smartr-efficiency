package org.smartrplace.app.monbase.power;

import java.util.List;

import org.smartrplace.app.monbase.power.ConsumptionEvalAdmin.SumType;

public class ConsumptionEvalTableLineBase implements ConsumptionEvalTableLineI {
	private static final long MAX_CACHE_TIME = 10000;
	
	public final EnergyEvalObjI conn;
	//protected final EnergyEvalInterval eeInterval;
	protected final boolean lineShowsPower;
	protected final String label;
	//protected final float[] sumUps;
	// line which counts the sumup for the current line
	//protected final EnergyEvaluationTableLine sumUpIndex2;
	protected final List<ConsumptionEvalTableLineI> sourcesToSum;
	//protected final List<EnergyEvaluationTableLine> sumLinesToClear;
	//protected final ApplicationManager appMan;
	
	protected final float[] lastValues;
	protected long lastUpdateTime = -1;
	
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
	public ConsumptionEvalTableLineBase(String label, boolean lineShowsPower,
			SumType type,
			List<ConsumptionEvalTableLineI> sourcesToSum,
			int index,
			EnergyEvalObjI energyEvalObjI) {
		this(energyEvalObjI, label, lineShowsPower, type, sourcesToSum, index);
	}
	public ConsumptionEvalTableLineBase(EnergyEvalObjI conn, String label, boolean lineShowsPower,
			SumType type,
			List<ConsumptionEvalTableLineI> sourcesToSum,
			int index) {
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
		//this.eeInterval = eeInterval;
	}

	/** 0: overall, 1: L1, 2: L2, 3: L3*/
	@Override
	public float getPhaseValue(int index, long startTime, long endTime, long now, List<ConsumptionEvalTableLineI> allLines) {
		if(type == SumType.SUM_LINE) {
			float val = 0;
			for(ConsumptionEvalTableLineI source: sourcesToSum) {
				float sval = source.getLastValue(index);
				if(!Float.isNaN(sval))
					val += source.getLastValue(index);
			}
			return val;
		} else {
			synchronized(this) {
				if(now - lastUpdateTime > MAX_CACHE_TIME) {
					for(ConsumptionEvalTableLineI line: allLines) {
						if(line.getLineType() == SumType.SUM_LINE)
							break;
						for(int indexAll=0; indexAll<=3; indexAll++) {
							//float value = line.getPhaseValueInternal(indexAll, startTime, endTime);
							//line.lastValues[indexAll] = value;
							//line.setLastValue(indexAll, value);
							line.updatePhaseValueInternal(index, startTime, endTime, now);
						}
					}
				} 
				return lastValues[index];
			}
		}
	}
	@Override
	public void updatePhaseValueInternal(int index, long startTime, long endTime, long now) {
		if(now - lastUpdateTime <= MAX_CACHE_TIME) {
			return;
		}
		float val = getPhaseValueRealInternal(index, startTime, endTime);
		lastValues[index] = val;
		lastUpdateTime = now;
	}
	
	public float getPhaseValueRealInternal(int index, long startTime, long endTime) {
		if(lineShowsPower) {
			if(index == 0) {
				return conn.getPowerValue();
			} else if(conn.hasSubPhases()) {
				return conn.getPowerValueSubPhase(index);
			}
		} else {
			//if(startTime != eeInterval.start().getValue()) {
			if(index == 0) {
				float val = conn.getEnergyValue(startTime, endTime, label);
				return val;
			} else if(conn.hasSubPhases()) {
				return conn.getEnergyValueSubPhase(index, startTime, endTime);
			}
			return Float.NaN;
		}
		return Float.NaN;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
	
	@Override
	public org.smartrplace.app.monbase.power.ConsumptionEvalAdmin.SumType getLineType() {
		return type;
	}
	
	@Override
	public void setLastValue(int index, float value) {
		lastValues[index] = value;	
	}
	
	@Override
	public float getLastValue(int index) {
		return lastValues[index];
	}
	
	@Override
	public boolean lineShowsPower() {
		return lineShowsPower;
	}
	
	@Override
	public String getLinePosition() {
		return index;
	}
}
