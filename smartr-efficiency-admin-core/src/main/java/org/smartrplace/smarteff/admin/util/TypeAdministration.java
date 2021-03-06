package org.smartrplace.smarteff.admin.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.core.model.Resource;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.smarteff.util.SPPageUtil;

public class TypeAdministration {
	public Map<Class<? extends SmartEffResource>, SmartrEffExtResourceTypeData> resourceTypes = new HashMap<>();
	public Map<Class<? extends SmartEffResource>, Set<Class<? extends SmartEffResource>>> inheritedTypes = new HashMap<>();
	private final SpEffAdminController app;
	
	public TypeAdministration(SpEffAdminController app) {
		this.app = app;
	}

	/** Note that we only need the parent resource type, not the parent ExtensionResourceTypeDeclaration here.
	 * So there should be no problem regarding dependencies.
	 */
	public void registerService(SmartEffExtensionService service) {
		int i = 0;
    	Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> resDefList = service.resourcesDefined();
		if(resDefList  != null) for(ExtensionResourceTypeDeclaration<? extends SmartEffResource> rtd: resDefList) {
    		try {
	    		Class<? extends SmartEffResource> rt = rtd.dataType();
	    		registerInherited(rt);
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
    		//TODO: Unregister inheritance
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
	
	public void registerInherited(Class<? extends SmartEffResource> type) {
		List<Class<? extends SmartEffResource>> parents = new ArrayList<>();
		for(Class<? extends SmartEffResource> known: resourceTypes.keySet()) {
			if(known.isAssignableFrom(type)) {
				parents.add(known);
			} else if(type.isAssignableFrom(known)) {
				Set<Class<? extends SmartEffResource>> ls = inheritedTypes.get(known);
				if(ls == null) {
					ls = new HashSet<Class<? extends SmartEffResource>>();
					inheritedTypes.put(known, ls);
				}
				ls.add(type);
				app.guiPageAdmin.registerInheritanceForNewType(known, type);
			}
		}
		if(!parents.isEmpty()) {
			Set<Class<? extends SmartEffResource>> ls = inheritedTypes.get(type);
			if(ls == null) {
				ls = new HashSet<Class<? extends SmartEffResource>>();
				inheritedTypes.put(type, ls);
			}
			ls.addAll(parents);
			for(Class<? extends SmartEffResource> p: parents) {
				app.guiPageAdmin.registerInheritanceForNewType(p, type);				
			}
		}
	}
}
