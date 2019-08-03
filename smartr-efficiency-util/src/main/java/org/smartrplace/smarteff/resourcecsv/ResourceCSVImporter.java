package org.smartrplace.smarteff.resourcecsv;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.smarteff.util.editgeneric.SubTypeHandler;

import de.iwes.util.resource.ValueResourceHelper;

/**
 * Allows the import of resources via CSV
 * @author jruckel
 *
 */
public class ResourceCSVImporter {
	//TODO: Make this an enum
	protected final boolean nonAllowedElementMode;
	private final ApplicationManagerSPExt appManExt;
	private final SubTypeHandler typeHandler;
	
	public ResourceCSVImporter(boolean nonAllowedElementMode, ApplicationManagerSPExt appManExt) {
		this.nonAllowedElementMode = nonAllowedElementMode;
		this.appManExt = appManExt;
		this.typeHandler = new SubTypeHandler(null, appManExt);
	}

	/**
	 * Extract data from a CSV file at filePaht and place it into targetParentResource
	 * @param filePath
	 * @param targetParentResource
	 * @return
	 */
	public Resource importFromFile(String filePath, Resource targetParentResource) {
		targetParentResource.activate(true);
		return targetParentResource;
	}
	
	protected boolean checkSubResourceCreation(Resource parent, String subResourceName) {
		if(!ResourceUtils.isValidResourceName(subResourceName)) {
			reportError("Non-valied resource name "+subResourceName+ " request for "+parent.getLocation());
			return false;
		}
		if(parent.getSubResource(subResourceName) != null) {
			//TODO: In some cases existing resources can be accepted, even merging would be nice
			//This is the simpliest security measure, though
			reportError("Subresource "+subResourceName+ " request for "+parent.getLocation()+ " already exists!");
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @param parent
	 * @param subResourceName
	 * @param value
	 * @param typeString may be null. In this case the type has to be identified via the name
	 */
	protected void createSingleValueResource(Resource parent, String subResourceName, String value,
			String typeString) {
		if(!checkSubResourceCreation(parent, subResourceName)) return;
		SingleValueResource newRes;
		if(typeString != null) {
			switch(typeString.toLowerCase()) {
			case "floatresource":
				newRes = parent.getSubResource(subResourceName, FloatResource.class);
				break;
			//TODO: Add more
			default:
				reportError("Unknown SingleValueResourceType:"+typeString);
				return;
			}
		} else {
			Class<? extends Resource> subType = typeHandler.getSubTypes(parent.getResourceType()).get(subResourceName);
			if(!(SingleValueResource.class.isAssignableFrom(subType))) {
				reportError(subResourceName+" at "+parent.getLocation()+" is not a SingleValueResource("+subType+")!");
				return;
			}
			newRes = (SingleValueResource) parent.getSubResource(subResourceName, subType);
		}
		newRes.create();
		//TODO: handler format exceptions
		ValueResourceUtils.setValue(newRes, value);
	}

	/** TODO: Create resource list
	 * 
	 * @param parent
	 * @param subResourceName
	 * @param typeString
	 * @param elementTypeString
	 * @return ResourceList created elementType must be set
	 */
	protected ResourceList<?> createResourceList(Resource parent, String subResourceName,
			String typeString, String elementTypeString) {
		ResourceList<?> result = null;
		return result ;
	}
	
	protected <T extends Resource> T createResourceListElement(ResourceList<T> parent, String elementName) {
		T result = parent.add();
		result.getSubResource("name", StringResource.class).<StringResource>create().setValue(elementName);
		return result;
	}
	
	private void reportError(String message) {
		if(nonAllowedElementMode) {
			System.out.println(message);
		} else
			throw new IllegalStateException(message);
	}
}
