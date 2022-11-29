package org.ogema.accessadmin.api.util;

import org.ogema.model.devices.buildingtechnology.AirConditioner;

public class DeviceUtil {
	public static int getEffectiveAirconAvMode(AirConditioner aircon) {
		int val = aircon.operationModesSupported().getValue();
		return getEffectiveAirconAvMode(val);
	}
	public static int getEffectiveAirconAvMode(int val) {
		if(val != 0)
			return val;
		int result = Integer.getInteger("org.ogema.model.devices.buildingtechnology.airconmodessupporteddefault", 1);
		if(result > 0 && result <= 3)
			return result;
		return 1;
	}
	
	public static String getAirconAvModeName(int avMode) {
		int defaultVal;
		switch(avMode) {
		case 0:
			defaultVal = getEffectiveAirconAvMode(avMode);
			if(defaultVal != 0)
				return "System default ("+getAirconAvModeName(defaultVal)+")";
			else
				throw new IllegalStateException("defaultVal:0");
		case 1:
			return "Cooling only";
		case 2:
			return "Heating only";
		case 3:
			return "Cooling+Heating";
		default:
			return "Unknown("+avMode+")";
		}
	}
}
