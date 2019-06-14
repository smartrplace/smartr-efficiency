package extensionmodel.smarteff.hpadapt;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.LengthResource;
import org.ogema.core.model.units.PercentageResource;
import org.ogema.core.model.units.PowerResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.model.units.VolumeResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/**
 * Data for HPAdapt.
 * Yellow cells in "LastBuilding" spread sheet.
 * @author jruckel
 * */
public interface HPAdaptData extends SmartEffResource {

	/* GENERAL DATA */

	/** Estimated savings after basic renovation */
	PercentageResource savingsAfterBasicRenovation();
	/** Known or estimated warm drinking water consumption */
	VolumeResource wwConsumption();
	/** Estimated warm water energy loss from storage, circulation at current temperature in heated areas */
	PercentageResource wwLossHeatedAreas();
	/** Warm water energy loss in unheated areas */
	PercentageResource wwLossUnheatedAreas();
	/** Warm water temperature */
	TemperatureResource wwTemp();
	/** Warm water temperature can be lowered to */
	TemperatureResource wwTempMin();
	/** Heating limit temperature */
	TemperatureResource heatingLimitTemp();
	/** Outside design temperature */
	TemperatureResource outsideDesignTemp();
	/** Estimated savings from condensing boiler */
	FloatResource savingsFromCDBoiler();
	/** Dimensioning for price type */
	IntegerResource designedForPriceType();
	
	/* U-VALUE DATA */
	
	/** U-Value basement → facade */
	FloatResource uValueBasementFacade();
	/** U-Value roof → facade */
	FloatResource uValueRoofFacade();
	/** Thickness of inner walls */
	LengthResource innerWallThickness();
	/** Basement temperature during heating season */
	TemperatureResource basementTempHeatingSeason();
	
	/** Boiler power (boiler only) */
	PowerResource boilerPowerBoilerOnly();
	/** Boiler power (bivalent heat pump) */
	PowerResource boilerPowerBivalentHP();
	/** Heat pump power (bivalent heat pump) */
	PowerResource hpPowerBivalentHP();
	
}
