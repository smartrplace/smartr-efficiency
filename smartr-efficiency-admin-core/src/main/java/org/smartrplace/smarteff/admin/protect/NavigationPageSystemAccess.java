package org.smartrplace.smarteff.admin.protect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.resourcemanager.ResourceException;
import org.smartrplace.extenservice.proposal.ProposalPublicData;
import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForCreate;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.smarteff.admin.util.ConfigIdAdministration;
import org.smartrplace.smarteff.admin.util.ResourceLockAdministration;
import org.smartrplace.smarteff.admin.util.TypeAdministration;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.util.format.ValueFormat;

public class NavigationPageSystemAccess extends NavigationPageSystemAccessForPageOpening
		implements ExtensionPageSystemAccessForCreate {
	private final String userName;
	private final String applicationName;
	private final ResourceLockAdministration lockAdmin;
	private final ApplicationManagerSPExt appExt;
	private final TypeAdministration typeAdmin;
	
	public NavigationPageSystemAccess(String userName, String applicationName,
			Map<Class<? extends Resource>, List<NavigationPublicPageData>> pageInfo,
			List<NavigationPublicPageData> startPagesData,
			ResourceLockAdministration lockAdmin, ConfigIdAdministration configIdAdmin,
			TypeAdministration typeAdmin,
			ApplicationManagerSPExt appExt,
			Map<Class<? extends Resource>, List<ProposalPublicData>> proposalInfo,
			Resource myPrimaryResource, String myUrl) {
		super(pageInfo, startPagesData, configIdAdmin, proposalInfo, myPrimaryResource, myUrl);
		this.userName = userName;
		this.applicationName = applicationName;
		this.lockAdmin = lockAdmin;
		this.typeAdmin = typeAdmin;
		this.appExt = appExt;
	}

	@Override
	public String accessCreatePage(NavigationPublicPageData pageData, int entryIdx,
			Resource parent) {
		Class<? extends Resource> type = pageData.getEntryTypes().get(entryIdx).getType();
		ExtensionResourceTypeDeclaration<? extends Resource> typeDecl = appExt.getTypeDeclaration(type);
		String name = CapabilityHelper.getNewMultiResourceName(type, parent);
		NewResourceResult<? extends Resource> newResource = getNewResource(parent, name, typeDecl);
		if(newResource.result != ResourceAccessResult.OK) {
			System.out.println("Error while trying to create "+parent.getLocation()+"/"+name+": "+newResource.result);
			return CapabilityHelper.ERROR_START+newResource.result;			
		}
		List<Resource> entryResources = Arrays.asList(new Resource[] {newResource.newResource});
		return accessPage(pageData, entryIdx, entryResources );
	}
	
	@Override
	public LockResult lockResource(Resource resource) {
		if(!checkAllowed(resource)) return null;
		return lockAdmin.lockResource(resource, userName, applicationName);
	}

	@Override
	public void unlockResource(Resource resource, boolean activate) {
		if(!checkAllowed(resource)) return;
		if(activate) {
			resource.activate(true);
		}
		lockAdmin.unlockResource(resource);
	}

	@Override
	public boolean isLocked(Resource resource) {
		return lockAdmin.isLocked(resource);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Resource> NewResourceResult<T> getNewResource(Resource parentIn,
			String name, ExtensionResourceTypeDeclaration<T> type) {
		if(SPPageUtil.isMulti(type.cardinality())) {
			ResourceList<T> parent = null;
			if(parentIn instanceof ResourceList) {
				parent = (ResourceList<T>) parentIn;
				if(!parent.isActive()) parent.create().activate(false);
			} else {
				name = ValueFormat.firstLowerCase(type.dataType().getSimpleName());
				for(ResourceList<?> rl: parentIn.getSubResources(ResourceList.class, false)) {
					if(rl.getElementType() == null) continue;
					if(rl.getElementType().isAssignableFrom(type.dataType())) {
						parent = (ResourceList<T>) rl;
						if(!parent.getName().equals(name))
							appExt.log().error("Name of ResourceList for "+type.dataType().getName()+" is not "+name);
						break;
					}
				}
				if(parent == null) {
					try {
						parent = parentIn.getSubResource(name, ResourceList.class);
						parent.create();
						parent.setElementType(type.dataType());
						parent.activate(false);
					} catch(ResourceException e) {
						NewResourceResult<T> result = new NewResourceResult<>();
						result.result = ResourceAccessResult.RESOURCE_ALREADY_EXISTS_DIFFENT_TYPE;
						return result;			
					}
					
				}
			}
			String elName = CapabilityHelper.getnewDecoratorName("E", parent);
			//We add as decorator here as type may be inherited class
			T res = parent.getSubResource(elName, type.dataType());
			return getNewResource(res);
		}
		Resource parent = parentIn;
		List<? extends T> existing = parent.getSubResources(type.dataType(), false);
		if(!existing.isEmpty()) {
			NewResourceResult<T> result = new NewResourceResult<>();
			result.result = ResourceAccessResult.SINGLE_RESOURCETYPE_ALREADY_EXISTS;
			return result;						
		}
		try {
			T res = (T) parentIn.getSubResource(name, type.dataType());
			return getNewResource(res);
		} catch(ResourceException e) {
			NewResourceResult<T> result = new NewResourceResult<>();
			result.result = ResourceAccessResult.RESOURCE_ALREADY_EXISTS_DIFFENT_TYPE;
			return result;			
		}
	}
	
	@Override
	public <T extends Resource> NewResourceResult<T> getNewResource(T virtualResource) {
		NewResourceResult<T> result = new NewResourceResult<>();
		if(!checkAllowed(virtualResource)) {
			result.result = ResourceAccessResult.NOT_ALLOWED;
			return result;
		}
		if(virtualResource.exists()) {
			result.result = ResourceAccessResult.RESOURCE_ALREADY_EXISTS;
			return result;
		}
		virtualResource.create();
		result.result = ResourceAccessResult.OK;
		result.newResource = virtualResource;
		typeAdmin.registerElement(virtualResource);
		return result;
	}

	@Override
	public void activateResource(Resource resource) {
		unlockResource(resource, true);
	}
	
	private boolean checkAllowed(Resource resource) {
		String[] els = resource.getLocation().split("/", 2);
		if(els.length == 0) throw new IllegalStateException("Resource location should not be empty!");
		if(els[0].equals(userName)) return true;
		return false;
	}
}
