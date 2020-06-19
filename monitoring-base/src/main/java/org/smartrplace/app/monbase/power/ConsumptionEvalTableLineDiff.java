package org.smartrplace.app.monbase.power;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.devicefinder.api.DatapointInfo.UtilityType;
import org.smartrplace.app.monbase.power.ConsumptionEvalAdmin.SumType;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableGeneric.LineInfo;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableGeneric.LineInfoDp;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableGeneric.SumLineInfo;

public abstract class ConsumptionEvalTableLineDiff extends ConsumptionEvalTableLineBase {
	protected final Map<UtilityType, List<LineInfo>> dpsPerUtilType;
	protected final ColumnDataProvider cprov;
	protected final LineInfoDp lineInfoDp;
	
	protected List<LineInfo> linesToInclude;
	
	/** Overwrite this for another logic*/
	protected final float getAggregatedValue(float rawValue, List<ConsumptionEvalTableLineI> diffLines) {
		float diffVal = 0;
		for(ConsumptionEvalTableLineI line: diffLines) {
			diffVal += line.getLastValue(0);
		}
		return rawValue - diffVal;
	}
	
	protected boolean useDiffLine(ConsumptionEvalTableLineI line) {
		for(LineInfo li: linesToInclude) {
			if(line.getLabel().equals(li.label)) {
				return true;
			}
		}
		return false;
	}

	protected List<LineInfo> updateLinesToInclude() {
		List<LineInfo> all = dpsPerUtilType.get(UtilityType.ELECTRICITY);
		List<LineInfo> result = new ArrayList<>();
		if(all == null)
			return result;
		for(LineInfo li: all) {
			if((!(li instanceof SumLineInfo)) && (!(li == lineInfoDp))) {
				result.add(li);
			}
		}
		return result ;
	};

	
	public ConsumptionEvalTableLineDiff(LineInfoDp lineInfoDp, boolean lineShowsPower, int index,
			ColumnDataProvider cprov,
			Map<UtilityType, List<LineInfo>> dpsPerUtilType) {
		super(getConn(lineInfoDp), lineInfoDp.label, lineShowsPower, SumType.STD, null, index, lineInfoDp.util);
		this.dpsPerUtilType = dpsPerUtilType;
		this.cprov = cprov;
		this.lineInfoDp = lineInfoDp;
	}

	protected static EnergyEvalObjI getConn(LineInfoDp lineInfoDp) {
		if(lineInfoDp.conn != null)
			return lineInfoDp.conn;
		EnergyEvalObjBase connLoc = new EnergyEvalObjBase((FloatResource)lineInfoDp.dp.getResource(), null);
		return connLoc; 
	}
	
	@Override
	public ColumnDataProvider getCostProvider() {
		return cprov;
	}

	//@Override
	//public void updatePhaseValueInternal(int index, long startTime, long endTime, long now) {
	//	super.getPhaseValueRealInternal(index, startTime, endTime);
	//}
	
	List<ConsumptionEvalTableLineI> diffLines = null;
	
	@Override
	public float getPhaseValue(int index, long startTime, long endTime, long now,
			Collection<ConsumptionEvalTableLineI> allLines) {
		if(diffLines == null) {
			diffLines = new ArrayList<>();
			linesToInclude = updateLinesToInclude();
			for(ConsumptionEvalTableLineI line: allLines) {
				if(useDiffLine(line))
					diffLines.add(line);
			}
		}
		float result = super.getPhaseValue(index, startTime, endTime, now, allLines);
		updatePhaseValueInternal(index, startTime, endTime, now);
		return result;
	}

	@Override
	public float getPhaseValueRealInternal(int index, float lineMainValue, long startTime, long endTime) {
		if(diffLines == null)
			return 0;
		
		float myRawVal = super.getPhaseValueRealInternal(index, lineMainValue, startTime, endTime);
		
		float result = getAggregatedValue(myRawVal, diffLines);
		
		return result;
	}
}
