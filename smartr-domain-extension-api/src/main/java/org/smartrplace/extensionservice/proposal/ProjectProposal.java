package org.smartrplace.extensionservice.proposal;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;

/** A ProjectProposal is a special CalculatedData that provides some information on a
 * project planned. 
 */
public interface ProjectProposal extends CalculatedData {
	/** Total cost to be paid if project is done as planned. This includes cost for additional
	 * material, transportation etc., but not cost for own labor of the client if the 
	 * ownHours field is set.
	 */
	FloatResource costOfProject();
	/** Like costOfProject, but includes cost for ownHours, Loss of income etc. according to
	 * customer specification. These additional costs are usually not planned by the
	 * LogicProvider, but are just added based on a cost-per-hour-rate and additional costs
	 * specified by the planner. 
	 */
	FloatResource costOfProjectIncludingInternal();
	/** Absolute operating costs for building energy supply after the measure took place. For
	 * small measures it is possible to give just a value for {@link ProjectProposalEfficiency#yearlySavings()}.
	 * The cost shall include heat and electricity.
	 */
	FloatResource yearlyOperatingCosts();

	/** Number of working hours estimated for the client if the project is
	 * done as planned. For private customers this is the number of hours done as
	 * Do-it-Yourself (DIY), for commercial customers this is the number of hours
	 * estimated for own staff (e.g. technical building managers).
	 * This is in addition to the costOfProject*/
	FloatResource ownHours();
	
	/** Name of the planner. The value must exist as user name on the system.*/
	StringResource plannerUserName();
	
	/** A description of the project benefit. If inherited models include modeling of benefits,
	 * e.g. monetary and/or environmental benefits an additinal description may not be necessary here
	 */
	StringResource benefitDescription();
}
