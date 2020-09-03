package org.smartrplace.apps.hw.install.gui.prop;

public class PropertyData {
	final String propertyName;
	
	final String propertyValue;
	final String dataPointResourceLocation;
	
	public PropertyData(String propertyName, String propertyValue, String dataPointResourceLocation) {
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
		this.dataPointResourceLocation = dataPointResourceLocation;
	}
}
