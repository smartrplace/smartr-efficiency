package org.smartrplace.extensionservice.proposal;

import org.ogema.core.model.simple.FloatResource;

/** A ProjectProposal is a special CalculatedData that provides some information on a
 * project planned. 
 */
public interface ProjectProposal100EE extends ProjectProposal {
	/** Like {@link #yearlyOperatingCosts()}, but for CO2-neutral energy supply
	 */
	FloatResource yearlyOperatingCostsCO2Neutral();

	/** Like {@link #yearlyOperatingCosts(), but for 100EE-energy supply according to
	 * {@link https://gitlab.com/100ee-space/buildings-100ee/wikis/Geb%C3%A4udeenergiewende_100EE/Preisstruktur_100EE}
	 */
	FloatResource yearlyOperatingCosts100EE();
}
