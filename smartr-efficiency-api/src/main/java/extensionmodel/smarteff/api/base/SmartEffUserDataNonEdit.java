package extensionmodel.smarteff.api.base;

import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;

public interface SmartEffUserDataNonEdit extends ExtensionUserDataNonEdit {
	@Override
	SmartEffUserData editableData();
	SmartEffConfigurationSpace configurationSpace();
	
	/**  0: anonymous user
	 *  10: logic provider admin user
	 * 100: master admin user  
	 */
	IntegerResource userType();
}
