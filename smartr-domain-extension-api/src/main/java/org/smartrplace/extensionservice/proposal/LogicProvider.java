package org.smartrplace.extensionservice.proposal;

import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;
import org.smartrplace.extensionservice.driver.DriverProvider;
import org.smartrplace.extensionservice.resourcecreate.ExtensionCapabilityForCreate;

/** A LogicProvider calculates some results based on an entry point-based input resource and further user and
 * general data it finds itself based on the entry point-based resource(s). So a LogicProvider acts very
 * similar to a GUI Page provider that generates resources, but the LogicProvider generates the content
 * automatically whereas the GUI provider lets the user enter the content. So a LogicProvider is
 * suitable to be operated in batch processes etc.<br>
 * LogicProviders are not designed to operate as a background service, listen to new resources etc. This
 * has to be done by a separate module if required that would start relevant LogicProviders when
 * new data is available etc.<br>
 * TODO: An importer could use this interface, but then the result type should be able to specify
 * and GenericDataTypeDeclaration. In this case input resources would act as the configuration
 * resources in {@link DriverProvider}. Also here no regular import would occur, but it has to be
 * triggered from somewhere else.
 */
public interface LogicProvider extends ExtensionCapabilityForCreate, LogicProviderPublicData {
	/** Called from framework to init provider
	 * 
	 * @param appManExt
	 * @param userData user data of use declared via {@link #userName()}
	 */
	void init(ApplicationManagerSPExt appManExt, ExtensionUserDataNonEdit userData);
}
