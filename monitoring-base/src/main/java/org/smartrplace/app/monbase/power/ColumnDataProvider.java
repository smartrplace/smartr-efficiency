package org.smartrplace.app.monbase.power;

/** Each ColumnDataProvider object is generated for a specific cell in a KPI table.
 * For this reason no line and index information needs to be provided in the method
 * {@link #getValue(float, long, long)}*/
public interface ColumnDataProvider {
	/** Provide value to be displayed in KPI column
	 * 
	 * @param lineMainValue value of main datapoint of line
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	float getValue(float lineMainValue, long startTime, long endTime);
	
	/** String representation of the value to be used for actual display in the KPI table*/
	default String getString(float value) {
		return String.format("%.2f", value);
	};
}
