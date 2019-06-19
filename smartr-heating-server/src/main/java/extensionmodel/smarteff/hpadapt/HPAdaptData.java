package extensionmodel.smarteff.hpadapt;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.LengthResource;
import org.ogema.core.model.units.PercentageResource;
import org.ogema.core.model.units.PowerResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.model.units.VolumeResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.SmartEffTimeSeries;

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
	/** Given as price type when the value of {{@link #dimensioningForPriceType()} is to be used. */
	final static int USE_USER_DEFINED_PRICE_TYPE = -1;
	/** Conventional pricing */
	final static int PRICE_TYPE_CONVENTIONAL = 0;
	/** CO2-neutral pricing */
	final static int PRICE_TYPE_CO2_NEUTRAL = 1;
	/** 100EE pricing */
	final static int PRICE_TYPE_100EE = 2;
	/** Number of price type ids. If all types are to be calculated for, iterate from 0 to this. */
	final static int PRICE_TYPES_COUNT = 2;
	/** English names for price types */
	final static String[] PRICE_TYPE_NAMES_EN = {"Conventional", "CO2-neutral", "100EE"};
	/** Dimensioning for price type */
	IntegerResource dimensioningForPriceType();
	
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
	
	/** Historical Temperature Data to be imported via CSV */
	SmartEffTimeSeries temperatureHistory();
	
}
