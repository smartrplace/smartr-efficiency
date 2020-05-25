package org.smartrplace.app.monbase.power;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.ConsumptionInfo.AggregationMode;
import org.ogema.devicefinder.api.Datapoint;
import org.smartrplace.app.monbase.power.ConsumptionEvalAdmin.SumType;

public interface ConsumptionEvalTableLineI {
	public static interface EnergyEvalObjI {
		
		float getPowerValue();
		/** Get current energy value*/
		float getEnergyValue();
		
		/** Get energy value for a certain time period
		 * 
		 * @param startTime
		 * @param endTime
		 * @param printVals if not null then the String shall be used as prefix for debug pritings.
		 * 		If null no debug printings shall be made
		 * @return
		 */
		float getEnergyValue(long startTime, long endTime, String printVals);
		
		/** Even if the {@link EnergyEvaluationTable}(s) using this line have sub phases not all lines
		 * included need to support this*/
		int hasSubPhaseNum();
		
		/** If false this indicates that energy has to be calculated from power
		 * TODO: This needs to be replaced by returing the {@link AggregationMode}
		 * @return
		 */
		boolean hasEnergySensor();
		
		float getPowerValueSubPhase(int index);
		/** Get current meter reading*/
		float getEnergyValueSubPhase(int index);
		/** Get energy value for a certain time period
		 * 
		 * @param startTime
		 * @param endTime
		 * @param printVals if not null then the String shall be used as prefix for debug pritings.
		 * 		If null no debug printings shall be made
		 * @return
		 */
		float getEnergyValueSubPhase(int index, long startTime, long endTime);
		
		/** May return null if no such resource is available
		 * TODO: Check if necessary/meaningful
		 * @return
		 */
		Resource getMeterReadingResource();
	}
	
	/** Update sum lines and cached values
	 * TODO: Adapt naming
	 * @param index 0: overall, 1: L1, 2: L2, 3: L3
	 * */
	public float getPhaseValue(int index, long startTime, long endTime, long now, List<ConsumptionEvalTableLineI> allLines);
	
	/** Get value for a phase and an interval. Implementation needs not to support sum lines*/
	void updatePhaseValueInternal(int index, long startTime, long endTime, long currentTime);
	
	public String getLabel();
	
	SumType getLineType();
	/** If true the line shows power, otherwise line shows aggregated consumption like energy*/
	boolean lineShowsPower();
	
	/** Set last value cached for a phase
	 * 
	 * @param index phase for which value is supplied
	 */
	void setLastValue(int index, float value);
	float getLastValue(int index);
	
	/** A line with a lower position index will be displayed above a line with a higher position index
	 * @return */
	String getLinePosition();
	
	/** May return null if no such information is available*/
	Datapoint getDatapoint();

	/** Return number of sub phase values*/
	public int hasSubPhaseNum();
	
	public static interface CostProvider {
		String getCost(float value);
	}
	default CostProvider getCostProvider( ) {return null;}
}
