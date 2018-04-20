package extensionmodel.smarteff.api.common;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.StringResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface MasterUserData extends SmartEffResource {
	LocationExtended address();
	/** c/o field or contact within company*/
	StringResource addressExtension();
	StringResource phoneNumber();
	StringResource faxNumer();
	StringResource mobileNumber();
	StringResource homePageUrl();
	StringResource emailAddress();
	
	/**If false the user is treated as organization*/
	BooleanResource isNaturalPerson();
	
	BooleanResource makeNamePublic();
	BooleanResource makeEmailPublic();
	BooleanResource makeAddressPublic();
	BooleanResource makePhonePublic();
}
