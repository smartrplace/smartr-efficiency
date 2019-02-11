package extensionmodel.smarteff.api.common;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.units.AreaResource;
import org.ogema.core.model.units.PercentageResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.model.units.VolumeResource;
import org.ogema.model.locations.Room;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.extensionservice.SpartEffModelModifiers.DataType;

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
	
	ResourceList<HeatRadiator> heatRadiator();
	
	/** This is a rating that can be given by users for the current situation or
	 * e.g. for an entire day. At this stage we assume that it is not realistic to
	 * ask the user for how long this rating is valid, but different values indicate
	 * something. Values:
	 * 1: OK, no complaints
	 * 2: Feels improved compared to earlier situations
	 * 10: Too cold
	 * 11: Too warm
	 * 12: Too cold / warm mixed / I do not understand how I can control the temperature
	 * 100: Heating does not seem to work at all
	 * 101: Heating working full power, too warm, cannot be reduced
	 */
	SmartEffTimeSeries roomTemperatureQualityRating();
	
	@DataType(resourcetype=TemperatureResource.class)
	SmartEffTimeSeries manualTemperatureReading();
	
	@DataType(resourcetype=PercentageResource.class)
	SmartEffTimeSeries manualHumidityReading();
}
