package org.smartrplace.extenservice.proposal;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;

public interface ProjectProposal extends CalculatedData {
	/** Total cost to be paid if project is done as planned. This includes cost for additional
	 * material, transportation etc., but not cost for own labor of the client if the 
	 * ownHours field is set.
	 */
	FloatResource costOfProject();
	/** Like costOfProject, but includes cost for ownHours, Loss of income etc. according to
	 * customer specification. These additional costs are usually not planned by the
	 * ProposalProvider, but are just added based on a cost-per-hour-rate and additional costs
	 * specified by the planner. 
	 */
	FloatResource costOfProjectIncludingInternal();
	FloatResource yearlySavings();
	/** Number of working hours estimated for the client if the project is
	 * done as planned. For private customers this is the number of hours done as
	 * Do-it-Yourself (DIY), for commercial customers this is the number of hours
	 * estimated for own staff (e.g. technical building managers).
	 * This is in addition to the costOfProject*/
	FloatResource ownHours();
	/** CO2 emission savings in kg/year*/
	FloatResource yearlyCO2savings();
	
	/** Name of the planner. The value must exist as user name on the system.*/
	StringResource plannerUserName();
}
