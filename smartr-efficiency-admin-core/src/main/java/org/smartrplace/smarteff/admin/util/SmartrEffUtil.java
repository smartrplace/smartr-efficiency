package org.smartrplace.smarteff.admin.util;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ProviderPublicDataForCreate.PagePriority;
import org.smartrplace.smarteff.admin.SpEffAdminController;

public class SmartrEffUtil {
	public static enum AccessType {
		PUBLIC,
		READONLY,
		READWRITE
	}
	public static AccessType getAccessType(Resource res) {
		if(res.getLocation().startsWith(SpEffAdminController.APPCONFIGDATA_LOCATION+"/userDataNonEdit"))
			return AccessType.READONLY;
		else if(res.getLocation().startsWith(SpEffAdminController.APPCONFIGDATA_LOCATION+"/generalData"))
			return AccessType.PUBLIC;
		else return AccessType.READWRITE; 
	}
	
	public static int comparePagePriorities(PagePriority prioA, PagePriority prioB ) {
		if(prioA == prioB) return 0;
		if(prioA == PagePriority.STANDARD) return 2;
		if(prioB == PagePriority.STANDARD) return -2;
		if(prioA == PagePriority.SECONDARY) return 1;
		return -1;
	}
}
