package extensionmodel.smarteff.hpadapt;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.AreaResource;
import org.ogema.core.model.units.EnergyResource;
import org.ogema.core.model.units.PowerResource;
import org.ogema.core.model.units.TemperatureResource;
import org.smartrplace.extensionservice.proposal.ProjectProposal100EE;
/**
 * Results of HPAdapt.
 * Yellow cells in "Letzte Rechnung" spread sheet.
 * @author jruckel
 * */
public interface HPAdaptResult extends ProjectProposal100EE {
	
	/* DESIGN 1 */
	
	/** Warm water energy (losses during heating season in heated areas are included) */
	EnergyResource wwEnergyPreRenovation();
	/** Heating energy (pre-renovation) */
	EnergyResource heatingEnergPreRenovation();
	/** Warm water energy (post-renovation) */
	EnergyResource wwEnergyPostRenovation();
	/** Heating energy (post-renovation) */
	EnergyResource heatingEnergyPostRenovation();
	/** Total energy (post-renovation) */
	EnergyResource totalEnergyPostRenovation();
	
	
	/* HEATING CHARACTERISTICS */
	
	/** Heating degree days */
	FloatResource heatingDegreeDays();
	/** Number of heating days */
	FloatResource numberOfHeatingDays();
	/** Heating degree days (hourly basis) */
	FloatResource heatingDegreeDaysHourly();
	/** Number of heating days (hourly basis) */
	FloatResource numberOfHeatingDaysHourly();
	/** Full load hours excl. warm water (h/a) */
	FloatResource fullLoadHoursExclWW();
	/** Full load hours incl. warm water (h/a) */
	FloatResource fullLoadHoursInclWW();
	/** Median heating outside temperature */
	TemperatureResource medianHeatingOutsideTemp();
	
	
	/* BIVALENT SYSTEM DESIGN */
	
	/** Maximum power of heat pump from BadRoom */
	PowerResource maxPowerHPfromBadRoom();
	
	
	/* U-VALUE */
	
	/** Window area */
	AreaResource windowArea();
	/** Window power loss (W/K) */
	FloatResource pLossWindow();
	/** Number of rooms facing outside */
	IntegerResource numberOfRoomsFacingOutside();
	/** Facade wall area */
	AreaResource facadeWallArea();
	/** Basement area */
	AreaResource basementArea();
	/** Roof area */
	AreaResource roofArea();
	/** Weighted exterior surface area excl. windows */
	AreaResource weightedExtSurfaceAreaExclWindows();
	/** Active power while heating */
	PowerResource activePowerWhileHeating();
	/** Total power loss (W/K) */
	FloatResource totalPowerLoss();
	/** U-Value of facade */
	FloatResource uValueFacade();
	/** Basement heating power loss */
	PowerResource powerLossBasementHeating();
	/** Other power loss (W/K) */
	FloatResource otherPowerLoss();
	/** Power loss at 0°C */
	PowerResource powerLossAtFreezing();
	/** Power loss at outside design temperature */
	PowerResource powerLossAtOutsideDesignTemp();
}
