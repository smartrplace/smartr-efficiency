package org.smartrplace.apps.hw.install.devicetypes;

import org.ogema.core.model.simple.StringResource;

import de.iwes.util.resource.ValueResourceHelper;

public class InitialConfig {

	public static boolean isInitDone(String initID, StringResource res) {
		String status = res.getValue();
		if(status != null && status.contains(initID+","))
			return true;
		return false;
	}
	
	public static void addString(String initID, StringResource res) {
		if(!res.exists()) {
			ValueResourceHelper.setCreate(res, initID);
		} else {
			String exist = res.getValue();
			if(exist.contains(initID+","))
				return;
			res.setValue(exist+initID+",");
		}
	}

}
