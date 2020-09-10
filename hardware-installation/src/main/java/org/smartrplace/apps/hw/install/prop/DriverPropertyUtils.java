package org.smartrplace.apps.hw.install.prop;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.array.StringArrayResource;

import de.iwes.util.resource.ResourceHelper;

public class DriverPropertyUtils {
	public static final String RES_NAMES = "propertyNames";	
	public static final String RES_VALUES = "propertyValues";	
	
	public static StringArrayResource[] getPropertyResources(Resource parent, boolean createIfNotExisting) {
		if(createIfNotExisting) {
			StringArrayResource names = parent.getSubResource(RES_NAMES, StringArrayResource.class);
			if(!names.isActive()) {
				names.create().activate(false);
			}
			StringArrayResource values = parent.getSubResource(RES_VALUES, StringArrayResource.class);
			if(!values.isActive()) {
				values.create().activate(false);
			}
			return new StringArrayResource[] {names, values};
		}
		StringArrayResource names = ResourceHelper.getSubResourceIfExisting(parent, RES_NAMES,
				StringArrayResource.class);
		StringArrayResource values = ResourceHelper.getSubResourceIfExisting(parent, RES_VALUES,
				StringArrayResource.class);
		if(names != null && values != null)
			return new StringArrayResource[] {names, values};
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Resource> List<T> getResourcesWithProperties(ApplicationManager appMan, Class<T> resourceType) {
		List<T> result = new ArrayList<>();
		List<StringArrayResource> arrays = appMan.getResourceAccess().getResources(StringArrayResource.class);
		for(StringArrayResource arr: arrays) {
			if(!arr.getName().equals(RES_NAMES))
				continue;
			Resource parent = arr.getParent();
			if(parent == null || (!resourceType.isAssignableFrom(parent.getResourceType())))
				continue;
			StringArrayResource values = parent.getSubResource(RES_VALUES, StringArrayResource.class);
			if(!values.isActive()) {
				continue;
			}
			result.add((T) parent);
		}
		return result ;
	}
}