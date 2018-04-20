package org.smartrplace.smarteff.admin.util;

import java.util.HashMap;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.smarteff.util.SPPageUtil;

public class TypeAdministration {
	public Map<Class<? extends SmartEffResource>, SmartrEffExtResourceTypeData> resourceTypes = new HashMap<>();
	private final SpEffAdminController app;
	
	public TypeAdministration(SpEffAdminController app) {
		this.app = app;
	}

	public void registerService(SmartEffExtensionService service) {
		int i = 0;
    	if(service.resourcesDefined() != null) for(ExtensionResourceTypeDeclaration<? extends SmartEffResource> rtd: service.resourcesDefined()) {
    		try {
	    		Class<? extends SmartEffResource> rt = rtd.dataType();
	    		SmartrEffExtResourceTypeData data = resourceTypes.get(rt);
	    		if(data == null) {
	    			data = new SmartrEffExtResourceTypeData(rtd, service, app);
	    			resourceTypes.put(rt, data );    			
	    		} else data.addParent(service);
	    		i++;
    		} catch(Exception e) {
    			System.out.println("Error in service "+service.getClass().getName()+" Resource Type "+i);
    			throw e;
    		}
    	}
		
	}
	
	public void unregisterService(SmartEffExtensionService service) {
		if(service.resourcesDefined() != null) for(ExtensionResourceTypeDeclaration<? extends SmartEffResource> rtd: service.resourcesDefined()) {
    		Class<? extends SmartEffResource> rt = rtd.dataType();
    		SmartrEffExtResourceTypeData data = resourceTypes.get(rt);
    		if(data == null) {
    			//should not occur
    			app.log.error("Resource type "+rt.getName()+" not found when service "+SPPageUtil.buildId(service)+ "unregistered!");
    		} else if(data.removeParent(service)) resourceTypes.remove(rt);
    	}
	}

	public void registerElement(Resource res) {
		SmartrEffExtResourceTypeData data = resourceTypes.get(res.getResourceType());
		if(data == null) return;
		data.registerElement(res);
	}
	
	public void unregisterElement(SmartEffResource res) {
		SmartrEffExtResourceTypeData data = resourceTypes.get(res.getResourceType());
		if(data == null) return;
		data.unregisterElement(res);
	}
}
