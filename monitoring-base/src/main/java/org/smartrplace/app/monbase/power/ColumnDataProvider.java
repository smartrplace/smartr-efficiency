package org.smartrplace.app.monbase.power;

/** Each ColumnDataProvider object is generated for a specific cell in a KPI table.
 * For this reason no line and index information needs to be provided in the method
 * {@link #getValue(float, long, long)}<br>
 * Ths is e.g. used to provide elements of the cost column and to provide a
 * reference comparison to a yearly reference value.*/
public abstract class ColumnDataProvider {
	/** Provide value to be displayed in KPI column
	 * 
	 * @param lineMainValue value of main datapoint of line
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	abstract public float getValue(float lineMainValue, long startTime, long endTime);
	public String getString(float lineMainValue, long startTime, long endTime) {
		float val = getValue(lineMainValue, startTime, endTime);
		return getString(val);
	}
	
	/** String representation of the value to be used for actual display in the KPI table*/
	protected String getString(float value) {
		return String.format("%.2f", value);
	};
}
