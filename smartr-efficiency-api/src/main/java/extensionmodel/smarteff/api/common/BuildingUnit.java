package extensionmodel.smarteff.api.common;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.units.AreaResource;
import org.ogema.core.model.units.VolumeResource;
import org.ogema.model.locations.Room;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/** A bulding unit can be a room or any part of a building. Building units defined for a building can overlap*/
public interface BuildingUnit extends SmartEffResource {
	ResourceList<BuildingUnit> subUnits();
	
	Room roomData();
	
	//FloatResource heatedLivingSpace();
	AreaResource groundArea();
	VolumeResource volume();
	AreaResource outsideWindowArea();
	/**Including window area*/
	AreaResource totalOutsideWallArea();
}
