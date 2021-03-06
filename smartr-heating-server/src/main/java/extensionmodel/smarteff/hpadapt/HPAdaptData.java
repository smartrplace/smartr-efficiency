package extensionmodel.smarteff.hpadapt;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.AreaResource;
import org.ogema.core.model.units.LengthResource;
import org.ogema.core.model.units.PercentageResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.model.units.VolumeResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.sp.calculator.hpadapt.HPAdaptEditPage;

/**
 * Data for HPAdapt.
 * Yellow cells in "LastBuilding" spread sheet.
 * For links to detailed documentation see {@link HPAdaptEditPage}
 * @author jruckel
 * */
public interface HPAdaptData extends SmartEffResource {

	/* GENERAL DATA */

	/** Height of rooms (default value if no specific value for a room is given)*/
	LengthResource roomHeight();
	
	/** Estimated savings after basic renovation */
	PercentageResource savingsAfterBasicRenovation();
	/** Known or estimated warm drinking water consumption */
	@Deprecated //moved to SmartrHeatingData
	VolumeResource wwConsumption();
	/** Estimated warm water energy loss from storage, circulation at current temperature in heated areas */
	@Deprecated //moved to SmartrHeatingData
	PercentageResource wwLossHeatedAreas();
	/** Warm water energy loss in unheated areas */
	@Deprecated //moved to SmartrHeatingData
	PercentageResource wwLossUnheatedAreas();
	/** Warm water temperature */
	@Deprecated //moved to SmartrHeatingData
	TemperatureResource wwTemp();
	/** Warm water temperature can be lowered to */
	TemperatureResource wwTempMin();
	/** Heating limit temperature */
	TemperatureResource heatingLimitTemp();
	/** Outside design temperature */
	TemperatureResource outsideDesignTemp();
	/** Estimated savings from condensing boiler */
	PercentageResource savingsFromCDBoiler();
	/** Given as price type when the value of {{@link #dimensioningForPriceType()} is to be used. */
	final static int USE_USER_DEFINED_PRICE_TYPE = -1;
	/** Conventional pricing */
	final static int PRICE_TYPE_CONVENTIONAL = 0;
	/** CO2-neutral pricing */
	final static int PRICE_TYPE_CO2_NEUTRAL = 1;
	/** 100EE pricing */
	final static int PRICE_TYPE_100EE = 2;
	/** Number of price type ids. If all types are to be calculated for, iterate from 0 to this. */
	@Deprecated
	final static int PRICE_TYPES_COUNT = 2;
	/** English names for price types */
	final static String[] PRICE_TYPE_NAMES_EN = {"Conventional", "CO2-neutral", "100EE"};
	/** German names for price types */
	final static String[] PRICE_TYPE_NAMES_DE = {"Konventionell", "CO2-neutral", "100EE"};
	/** Dimensioning for price type */
	IntegerResource dimensioningForPriceType();
	/** Offset for adapting to historical outside temperature data */
	FloatResource outsideTempOffset();
	/** Comfort temperature (default value if no specific value for a room is given) */
	TemperatureResource comfortTemp();
	/* U-VALUE DATA */
	
	/** U-Value basement → facade */
	FloatResource uValueBasementFacade();
	/** U-Value roof → facade */
	FloatResource uValueRoofFacade();
	/** Thickness of inner walls */
	LengthResource innerWallThickness();
	/** Basement temperature during heating season */
	TemperatureResource basementTempHeatingSeason();
	
	/** Roof area (not north) for PV */
	AreaResource roofAreaForPV();
	
}
