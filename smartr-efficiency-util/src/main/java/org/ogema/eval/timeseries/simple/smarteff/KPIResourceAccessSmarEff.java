package org.ogema.eval.timeseries.simple.smarteff;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.tools.resource.util.ResourceUtils;

import de.iwes.util.resource.ResourceHelper;
import extensionmodel.smarteff.api.common.BuildingUnit;

public class KPIResourceAccessSmarEff {
	public static BuildingUnit getRoomConfigResource(String roomId, ApplicationManager appMan) {
		Resource baseRes;
		if(roomId == null)
			baseRes = ResourceHelper.getSubResource(appMan.getResourceAccess().getResource("master"),
					"editableData/buildingData/E_0/buildingUnit/"+DPRoom.BUILDING_OVERALL_ROOM_LABEL+"/");
		else {
			//TODO: In SmartrEfficiency the room resource name is E_0 etc. and the room has to be found via its name
			//For initial eval we generate the rooms like this
			baseRes = ResourceHelper.getSubResource(appMan.getResourceAccess().getResource("master"),
					"editableData/buildingData/E_0/buildingUnit/"+ResourceUtils.getValidResourceName(roomId)+"/");			
		}
		if(baseRes == null || (!(baseRes instanceof BuildingUnit)))
			return null;
		return (BuildingUnit) baseRes;
	}
	
}
