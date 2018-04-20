package org.smartrplace.extensionservice.driver;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;

import de.iwes.timeseries.eval.api.DataProvider;

/** A driver provider reads data from external sources and provides it to other extension modules via the
 * interface DataProvider. Usually this data shall not be stored persistently on the OGEMA system, but this
 * can be done by a separate component. The driver itself shall not store the data persistently except for
 * each configuration data. Drivers are primarily intended for reading data that changes over time and this
 * would have to be stored and processed as time series. For synchronization of configuration-like data for which
 * time-series processing is not relevant other mechanisms should be developed.<br>
 * Usually a separate DataProvider shall be generated for each user to that the user can only access its own
 * data. To get the right data for a user it is usually required to first set the respective configuration
 * data in the user data. This is usually done via a separate GUI or can be done for all users that are allowed
 * to access such data via another OGEMA app. In the latter case the respective data has to be stored in
 * the NonEdit part of the user data so that users cannot change their configuration to access data they
 * are not allowed to get.<br>
 * A driver may also provide public data. If this public data contains a lot of time series of which only one or
 * a few are relevant for each user / building the selection shall also be done via configuration information in the
 * user data. In this case also user-specific DataProviders shall be generated although all data can be accessed by
 * all users. If data is provided that does not need user-specific individualization still the DataProvider is
 * requested per user. The driver can then just always return the same DataProvider.<br>
 * Configuration data that is not relevant to any user can be written into the generalData. TODO: Mechanism to
 * write to generalData still has to be defined.
 *
 */
public interface DriverProvider extends ExtensionCapability {
	Class<? extends DataProvider<Resource>> getDataProviderType();
	
	void init(ApplicationManagerSPExt appManExt);

	/** Get data provider for the user. See {@link NavigationGUIProvider#initPage(ExtensionNavigationPageI, Resource)}
	 * 
	 * @return data provider for the user. If the provider does not find the necessary configuration data for
	 * the user it shall return null.
	 */
	DataProvider<Resource> getDataProvider(int entryTypeIdx, List<Resource> entryResources, Resource userData,
			ExtensionUserDataNonEdit userDataNonEdit);
}
