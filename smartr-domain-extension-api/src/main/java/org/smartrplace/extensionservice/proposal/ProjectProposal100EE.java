package org.smartrplace.extensionservice.proposal;

import org.ogema.core.model.simple.FloatResource;

/** A ProjectProposal is a special CalculatedData that provides some information on a
 * project planned. ProjectProposal100EE is intended for project calculations based on 100EE.
 * Check the <a href="https://gitlab.com/100ee-space/buildings-100ee/wikis/home">Buildings-100EE Wiki</a>
 * for more information.
 */
public interface ProjectProposal100EE extends ProjectProposal {
	/** Like {@link #yearlyOperatingCosts()}, but for CO2-neutral energy supply
	 */
	FloatResource yearlyOperatingCostsCO2Neutral();

	/** Like {@link #yearlyOperatingCosts()}, but for 100EE-energy supply
	 * according to the 100EE price structure.
	 * <a href="https://gitlab.com/100ee-space/buildings-100ee/wikis/Geb%C3%A4udeenergiewende_100EE/Preisstruktur_100EE">Wiki Link</a>.
	 */
	FloatResource yearlyOperatingCosts100EE();
	
	/** Amortization for chosen price scenario (years) */
	FloatResource amortization();
}
