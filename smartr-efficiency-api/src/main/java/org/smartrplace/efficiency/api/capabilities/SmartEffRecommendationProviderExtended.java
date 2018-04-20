package org.smartrplace.efficiency.api.capabilities;

import org.smartrplace.efficiency.api.base.SmartEffResource;

/** To be provided as OSGi service
  * */
@Deprecated
public interface SmartEffRecommendationProviderExtended extends SmartEffRecommendationProvider {
	/* Missing options:
	 * - Possibility to add to recommendation input definition DataProviders / JAXB data 
	 * - There should be a way to store recommendations persistently. In resources or json files
	 * like Multi-Evaluations? Maybe both should be options: Recommendations can calculate key figures
	 * that should be part of the resource database. Recommendations can also calculate large and
	 * highly structured results. This may require domain-specific and generic class structures like
	 * MultiResult, but MultiResult itself may not fit the generic case here. This should be done in
	 * parallel with relevant implementations. The structures should be harmonized with the
	 * MultiResult structures so that results of Recommendations can be used as input to
	 * EvaluationProviders. EvaluationProviders can also be wrapped into the recommendation interface
	 * in case they would be needed in such an environment like SmartEff.
	 * - Possibility to start certain recommendations directly. OK, if the list of resources changed just
	 * cotains the data to be evaluated for recommendations it is pretty close. So this is not really the issue.
	 * - Definition of Scenarios / Input Data sets. This requires storage of data variants, possibly not
	 * all in the resource data base directly
	 * 
	 * Scopes: EvaluationProviders evaluate time series (but can use other information as input also),
	 * "normal" recommendations search a set of resource objects for results (usually do not search through
	 * time series, although this is not forbidden). Recommendations have no direct "online" evaluation
	 * feature, but the option to "update" previous evaluations.
	 */
	
	/**TODO: needs more specification*/
	public Object getRecommendationData();
	
	/** Such a result makes sense if the result is stored in a resource structure
	 * 
	 * @param scenario
	 * @return
	 */
	public SmartEffResource getRecommendationData2();
	/** This is not really a MultiResult, but this could work with json persistence*/
	//public MultiResult<Resource> getRecommendationData3();
}
