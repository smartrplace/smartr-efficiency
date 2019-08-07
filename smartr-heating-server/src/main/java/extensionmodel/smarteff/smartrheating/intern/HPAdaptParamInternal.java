package extensionmodel.smarteff.smartrheating.intern;

import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/**
 * Parameters for HPAdapt.
 * Yellow cells in "Parameter" spread sheet.
 * @author jruckel
 *
 */
public interface HPAdaptParamInternal extends SmartEffResource {

	/* MISC COSTS */

	/** Condensing Boiler → Condensing Boiler (CD→CD), Base (EUR) */
	FloatResource boilerChangeCDtoCD();
	/** Low-Temperature Boiler → Condensing Boiler (LT→CD), Base (EUR) */
	FloatResource boilerChangeLTtoCD();
	/** Additional CD→CD (per kW) (EUR) */
	FloatResource boilerChangeCDtoCDAdditionalPerkW();
	/** Additional LT→CD (per kW) (EUR)*/
	FloatResource boilerChangeLTtoCDAdditionalPerkW();
	/** Additional Bivalent Heat Pump Base (EUR)*/
	FloatResource additionalBivalentHPBase();
	/** Additional Bivalent Heat Pump per kW (EUR)*/
	FloatResource additionalBivalentHPPerkW();
}
