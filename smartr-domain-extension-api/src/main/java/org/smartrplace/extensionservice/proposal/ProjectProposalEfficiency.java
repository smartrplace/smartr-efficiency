package org.smartrplace.extensionservice.proposal;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;

/** A ProjectProposal is a special CalculatedData that provides some information on a
 * project planned. 
 */
public interface ProjectProposalEfficiency extends ProjectProposal, MeasureEfficiency {
	
	/** A description of the project benefit. If inherited models include modeling of benefits,
	 * e.g. monetary and/or environmental benefits an additional description may not be necessary here
	 */
	@Override
	StringResource benefitDescription();
	
	/** Amortization for chosen price scenario (years) */
	FloatResource amortization();	
}
