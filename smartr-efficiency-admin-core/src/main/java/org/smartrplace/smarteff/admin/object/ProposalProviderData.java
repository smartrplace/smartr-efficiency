package org.smartrplace.smarteff.admin.object;

import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extenservice.proposal.ProposalProvider;

/** Standard ProposalProviders take parameter data from entry resources as well as from 
 * parameter resources and provide a result in a result resource. We assume that all parameters
 * are available as the proposal providers sets default values itself and the entry resources
 * have all their standard data set. <br>
 * As an extension proposal providers shall be available to define Pre-evaluation requirements.
 * This means that certain other evaluation result resources must be available. The system
 * shall start the respective evaluations automatically if the resources are not available or
 * outdated. As a further extension it should be possible not to store all pre-evaluation results
 * persistently but in MemoryResources (but this is really not important in the first testing period).
 * Generally we assume that old evaluation results are overwritten for the same primary
 * entry resource.<br>
 * Another extension is the Multi-evaluation functionality. The fixed resource structure
 * should be an effective base for starting multi-evaluations on all sub-resources of
 * a type.<br>
 * Another extension ist the possibility to process time series data. The new EvaluationProvider
 * GenericGaRoSingleEvalProviderResResult allows to calculate a Resource result from
 * time series. In contrast to input resource values we cannot assume that all possible
 * or expected time series are available for every input set. So here the GaRo mechanism
 * for defining requirements regarding individual input series and starting evaluations
 * based on them can also make sense here. An evaluation should only be offered to be
 * started in the GUI when required input time series are available. Output time series
 * shall be stored in schedules here. Input times series can be acquired from various
 * data providers that provide data based on OGEMA Resources.<br>
 * We assume that time series are also data that is directly attached to locations,
 * buildings and rooms like with the systems evaluated with GaRo now. So we do not need
 * to define additional access systems for time series that refer to sub levels of the SmartEff resource
 * structure.
 */
public class ProposalProviderData {
	public final ProposalProvider provider;
	public final SmartEffExtensionService parent;
	
	public ProposalProviderData(ProposalProvider provider, SmartEffExtensionService parent) {
		this.provider = provider;
		this.parent = parent;
	}
	
}
