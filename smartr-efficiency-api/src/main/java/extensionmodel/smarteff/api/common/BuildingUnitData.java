package extensionmodel.smarteff.api.common;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.model.locations.Room;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/** A bulding unit can be a room or any part of a building. Building units defined for a building can overlap*/
public interface BuildingUnitData extends SmartEffResource {
	ResourceList<BuildingUnitData> subUnits();
	
	Room roomData();
	
	FloatResource heatedLivingSpace();
}
