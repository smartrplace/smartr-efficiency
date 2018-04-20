package org.smartrplace.efficiency.api.capabilities;

import java.util.List;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;

import de.iwes.widgets.template.LabelledItem;
import extensionmodel.smarteff.api.base.SmartEffGeneralData;
import extensionmodel.smarteff.api.base.SmartEffUserData;

/** To be provided as OSGi service
  * */
public interface SmartEffRecommendationProvider extends ExtensionCapability {
	/** TODO: Should we rather provide a Recommendation resource here? This makes the interface much more flexible,
	if an older version of a data model is used just some fields may be missing, would be replaced by default value.
	With this solution the main domain app will have to catch NoSuchMethods-Exceptions and fill in defaults.
	*/
	public interface Recommendation extends LabelledItem {
		float getNetInvestment();
		float getAnnualNetSavings();
		float getAnnualCO2Savings();
		//...
	}
	
	/** If any resource of a type listed here changes the updateRecommendations method shall be
	 * called. Note that not listeners on all elements may be registered, but the application may
	 * only trigger this if it changed a value itself or let an extension make changes
	 */
	public List<Class<? extends SmartEffResource>> inputResourceTypes();
	
	/** Trigger initial calculation of recommendations or update for a certain user
	 * 
	 * @param userData reference to data of the user
	 * @param generalData
	 * @param resourcesChanged known resources to be changed. If null the recommendations shall be re-calculated entirely
	 * @param recommendations may be an emtpy list on initial calculation
	 */
	public void updateRecommendations(SmartEffUserData userData, SmartEffGeneralData generalData,
			List<SmartEffResource> resourcesChanged,
			List<Recommendation> recommendations);
	
	/** The recommendation provider may provide a detailed view with information that is specific for the
	 * respective recommendation/evaluation.
	 * 
	 * @return may be null of not result page definition is provided
	 */
	public NavigationGUIProvider resultPageDefinition();

}
