package org.smartrplace.smarteff.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.generictype.GenericAttribute;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionGeneralData;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration.Cardinality;
import org.smartrplace.extensionservice.proposal.LogicProviderPublicData;
import org.smartrplace.extensionservice.ExtensionUserData;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForCreate;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForCreate.NewResourceResult;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.util.format.ValueFormat;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class CapabilityHelper {
	public static final String ERROR_START = "ERROR: ";
	public static final String STD_LOCATION = "smartEffAdminData/generalData/";
	public static final int STD_LOCATION_LENGTH = STD_LOCATION.length();
	
	public static String getNewMultiElementResourceName(Class<? extends Resource> type, Resource parent) {
		return getnewDecoratorName(getSingleResourceName(type), parent, "_");
	}
	public static String getnewDecoratorName(String baseName, Resource parent) {
		return getnewDecoratorName(baseName, parent, "_");
	}
	public static String getnewDecoratorName(String baseName, Resource parent, String separator) {
		int i=0;
		String name = baseName+separator+i;
		while(parent.getSubResource(name) != null) {
			i++;
			name = baseName+separator+i;
		}
		return name;
	}

	/** Standard resource name is returned. For Multi-resources the default resource type is returned*/
	public static String getSingleResourceName(ExtensionResourceTypeDeclaration<? extends Resource> typeDecl) {
		if(SPPageUtil.isMulti(typeDecl.cardinality())) {
			return "default"+typeDecl.dataType().getSimpleName();
		} else return getSingleResourceName(typeDecl.dataType());
	}
	public static String getSingleResourceName(Class<? extends Resource> type) {
		return ValueFormat.firstLowerCase(type.getSimpleName());		
	}
	
	public static ExtensionResourceTypeDeclaration<?> getTypeFromName(String elementName, ApplicationManagerSPExt appManExt) {
		final String nameToUse;
		if(elementName.startsWith("default")) nameToUse = ValueFormat.firstUpperCase(elementName.substring(7));
		else  nameToUse = ValueFormat.firstUpperCase(elementName);
		for(ExtensionResourceTypeDeclaration<?> decl: appManExt.getAllTypeDeclarations()) {
			if(decl.dataType().getSimpleName().equals(nameToUse)) return decl;
		}
		return null;
	}
	
	@SafeVarargs
	public static List<EntryType> getStandardEntryTypeList(Class<? extends Resource>... types) {
		List<EntryType> result = new ArrayList<>();
		for(Class<? extends Resource> t: types) {
			EntryType r = getEntryType(t);
			result.add(r);
		}
		return result ;
	}
	
	/*@SafeVarargs
	public static List<EntryType> getStandardEntryTypeList(ApplicationManagerSPExt appManExt, Class<? extends Resource>... types) {
		List<EntryType> result = new ArrayList<>();
		for(Class<? extends Resource> t: types) {
			EntryType r = getEntryType(appManExt.getTypeDeclaration(t));
			result.add(r);
		}
		return result ;
	}*/
	public static <T extends Resource> T getSubResourceSingle(Resource parent, Class<T> type) {
		return getSubResourceSingle(parent, type, null);
	}
	/** Use this method to acquire elements of a type that is not specified as element in its
	 * parent resource type yet
	 * 
	 * @param parent
	 * @param type
	 * @param appManExt
	 * @return standard resource of the relevant type. For Multi-Resources the respective
	 * 		default-Resource is returned.
	 */
	public static <T extends Resource> T getSubResourceSingle(Resource parent, Class<T> type, ApplicationManagerSPExt appManExt) {
		List<T> list = parent.getSubResources(type, false);
		if(list.isEmpty()) {
			if(appManExt == null) return null;
			ExtensionResourceTypeDeclaration<T> decl = appManExt.getTypeDeclaration(type);
			if(decl == null) throw new IllegalStateException("Using resource type "+type.getName()+" without type declaration!");
			if(decl.parentType() == null) {
				if(!((parent instanceof ExtensionUserData)||(parent instanceof ExtensionUserDataNonEdit)))
					throw new IllegalStateException("Requested sub type from resource "+parent.getLocation()+" for toplevel-type!");
			}
			else if(!decl.parentType().isAssignableFrom(parent.getResourceType()))
				throw new IllegalStateException("Requested sub type from resource "+parent.getLocation()+" for type with parent "+decl.parentType().getName());
			return parent.getSubResource(getSingleResourceName(decl), type);
		}
		if(list.size() > 1) throw new IllegalStateException("Multiple resources of single type "+type.getName()+" found in "+parent.getLocation());
		return list.get(0);
	}
	
	/** Make sure a resource list with fitting name exists and element is added
	 * 
	 * @param parent parent of resource list
	 * @param name name of new element in resource list to be added if null the name will
	 * 		be chosen automatically
	 * @param resourceToAddAsReference existing resource to add as reference
	 * @return the element that was added or found if checkIfExists is true and it already
	 * 		existed
	 */
	public static <T extends Resource> T addMultiTypeToList(Resource parent, String name,
			T resourceToAddAsReference) {
		@SuppressWarnings("unchecked")
		Class<T> elementType = (Class<T>) resourceToAddAsReference.getResourceType();
		String listName = getSingleResourceName(elementType);
		@SuppressWarnings("unchecked")
		ResourceList<T> resList =
				parent.getSubResource(listName, ResourceList.class);
		if(!resList.exists()) {
			resList.create();
			resList.setElementType(elementType);
			resList.activate(false);
		}
		if(name == null) name = ResourceListHelper.createNewDecoratorName(name, resList);
		return resList.addDecorator(ResourceUtils.getValidResourceName(name), resourceToAddAsReference);
	}
	/** Like {@link #addMultiTypeToList(Resource, String, Resource)}, but create new element
	 * directly in list
	 * @param elementType type of element in ResourceList that shall be created
	 * @return
	 */
	public static <T extends Resource> T addMultiTypeToList(Resource parent, String name,
			Class<T> elementType) {
		String listName = getSingleResourceName(elementType);
		@SuppressWarnings("unchecked")
		ResourceList<T> resList =
				parent.getSubResource(listName, ResourceList.class);
		if(!resList.exists()) {
			resList.create();
			resList.setElementType(elementType);
			resList.activate(false);
		}
		if(name == null) return resList.add();
		return resList.addDecorator(ResourceUtils.getValidResourceName(name), elementType);
	}
	public static <T extends Resource> ResourceList<T> getMultiTypeList(Resource parent, Class<T> elementType) {
		String listName = getSingleResourceName(elementType);
		@SuppressWarnings("unchecked")
		ResourceList<T> resList =
				parent.getSubResource(listName, ResourceList.class);
		if(!resList.exists()) {
			resList.create();
			resList.setElementType(elementType);
		} else if(!resList.getElementType().isAssignableFrom(elementType))
			throw new IllegalStateException("Type of "+resList.getLocation()+":"+resList.getElementType().getSimpleName()+", requested:"+elementType.getSimpleName());
		return resList;
	}
	
	/** Works like {@link ResourceHelper#getSubResource(Resource, String)} but can create intermediate
	 * decorators
	 * @param parent
	 * @param subPath
	 * @param extPageC
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Resource> T getOrcreateResource(Resource parent, String subPath, ExtensionPageSystemAccessForCreate extPageC,
			ApplicationManagerSPExt appManExt, Class<T> resultType) {
		if(!subPath.contains("#$")) {
			T res = ResourceHelper.getSubResource(parent, subPath, resultType);
			if(res != null) return res;
		}
		String[] els = subPath.split("/", 2);
		final String realSub;
		int index = -1;
		final Resource newRes;
		if(els[0].contains("#$")) {
			int inStringIdx = els[0].indexOf("#$");
			String intString = els[0].substring(inStringIdx+2);
			realSub = els[0].substring(0, inStringIdx);
			index = Integer.parseInt(intString);
			//cr = getElementClassOfResourceList(cr, realSub);
			ResourceList<?> resList = parent.getSubResource(realSub, ResourceList.class);
			//ExtensionResourceTypeDeclaration<?> type = CapabilityHelper.getTypeFromName(realSub, appManExt);
			//NewResourceResult<?> newParent = extPageC.getNewResource(parent, realSub, type);
			//newRes = newParent.newResource;
			//if(!(newRes instanceof ResourceList)) return null;
			//ResourceList<?> resList = (ResourceList<?>)newRes;
			if(resList.size() <= index) return null;
			newRes = resList.getAllElements().get(index);
			if(!newRes.exists()) return null;
			if(els.length == 1) return (T) newRes;
		} else {
			if(els.length <= 1) return null;
			realSub = els[0];
			ExtensionResourceTypeDeclaration<?> type = CapabilityHelper.getTypeFromName(realSub, appManExt);
			NewResourceResult<?> newParent = extPageC.getNewResource(parent, realSub, type);
			newRes = newParent.newResource;
		}
		String newSubPath = els[1];
		return getOrcreateResource(newRes, newSubPath, extPageC, appManExt, resultType);
	}
	
	/** Get resource to be used for a user. If a user-specific resource is available for the resource
	 * requested it will be returned, otherwise the global default resource. <br>
	 * Note that this method can only be used to access resources that have a single cardinality
	 * in all levels of its resource tree below globalData. This can also be used to access a
	 * user-specific default resource. It does not support to find a respective default value for
	 * a certain building-type or similar, though.<br>
	 * Note that this concept could be extended in the future to manage even more variants of a
	 * resource.
	 * 
	 * @param resourceInGlobal resource requested relative to globalData
	 * @param userData
	 * @return resource below userData if the relative path exists and is active
	 */
	public static <T extends Resource> T getForUser(T resourceInGlobal, ExtensionUserData userData) {
		String subPath = resourceInGlobal.getLocation().substring(STD_LOCATION_LENGTH);
		@SuppressWarnings("unchecked")
		T userResource = (T) ResourceHelper.getSubResource(userData, subPath , resourceInGlobal.getResourceType());
		if((userResource == null) || (!userResource.isActive())) return resourceInGlobal;
		return userResource;
	}
	/** Like {@link #getForUser(Resource, ExtensionUserData)}, but also return user resource if it does not
	 * exists (as virtual resource then) or if it is not active
	 * @param resourceInGlobal
	 * @param userData
	 */
	public static <T extends Resource> T getForUserVirtual(T resourceInGlobal, ExtensionUserData userData) {
		String subPath = resourceInGlobal.getLocation().substring(STD_LOCATION_LENGTH);
		@SuppressWarnings("unchecked")
		T userResource = (T) ResourceHelper.getSubResource(userData, subPath , resourceInGlobal.getResourceType());
		return userResource;
	}
	public static <T extends Resource> T getGlobalVirtual(T resourceInUser, ExtensionGeneralData globalData) {
		String subPath = getSubPathBelowUser(resourceInUser, null);
		if(subPath == null) throw new IllegalStateException("Not a subpath of userDataSpace");
		@SuppressWarnings("unchecked")
		T globalResource = (T) ResourceHelper.getSubResource(globalData, subPath , resourceInUser.getResourceType());
		return globalResource;
	}
	
	/**Returns subpath below user Read-Write-Space. Returns null if not in user space
	 * @param userName if null any user name is accepted*/
	public static String getSubPathBelowUser(Resource resourceInUser, String userName) {
		String[] els = resourceInUser.getLocation().split("/", 3);
		if(els.length < 3) return null;
		String subPath = els[2];
		if(userName == null) return subPath;
		String resourceUser = getUserName(resourceInUser);
		if(!resourceUser.equals(userName)) return null;
		//if(!els[0].equals(userName)) return null;
		return subPath;
	}
	
	public static String getUserName(Resource resourceInUser) {
		SmartEffUserDataNonEdit top = getNonEditUserData(resourceInUser);
		if(top == null) return null;
		return top.ogemaUserName().getValue();
		/*String[] els = resourceInUser.getLocation().split("/", 3);
		if(els.length < 3) return null;
		return els[0];*/
	}
	
	public static SmartEffUserDataNonEdit getNonEditUserData(Resource resourceInUser) {
		Resource topr = ResourceHelper.getToplevelResource(resourceInUser);
		if(!(topr instanceof SmartEffUserDataNonEdit)) return null;
		SmartEffUserDataNonEdit top = (SmartEffUserDataNonEdit)topr;
		return top;
	}

	/**
	 * @deprecated use {@link MyParam} instead	 * 
	 */
	public static float floatParam(FloatResource resourceInGlobal, ExtensionResourceAccessInitData data) {
		return getForUser(resourceInGlobal, data.userData()).getValue();
	}
	/** Get parameter object access for a certain type merged from global and user data*/
	public static <T extends Resource> MyParam<T> getMyParams(Class<T> type,
			ExtensionUserData userData, ApplicationManagerSPExt appManExt) {
		T glob = getSubResourceSingle(appManExt.globalData(), type, appManExt);
		T myRes = getForUser(glob, userData);
		MyParam<T> result = new MyParam<T>(glob, myRes, userData);
		return result;
	}
	
	public static class ParamVariants<T extends Resource> {
		public T globalVariant;
		public T userVariant;
	}
	public static <T extends Resource> ParamVariants<T> getParamVariants(T globalOrUser,
			ExtensionUserData userData, ApplicationManagerSPExt appManExt) {
		ParamVariants<T> result = new ParamVariants<T>();
		if(globalOrUser.getLocation().startsWith(STD_LOCATION)) {
			result.globalVariant = globalOrUser;
			result.userVariant = getForUserVirtual(globalOrUser, userData);
		} else {
			result.userVariant = globalOrUser;
			result.globalVariant = getGlobalVirtual(globalOrUser, appManExt.globalData());			
		}
		return result;
	}
	
	public static GenericDataTypeDeclaration getGenericDataTypeDeclaration(Class<? extends Resource> type) {
		return new GenericDataTypeDeclaration() {
			
			@Override
			public String label(OgemaLocale arg0) {
				return type.getSimpleName();
			}
			
			@Override
			public String id() {
				return type.getName();
			}
			
			@Override
			public TypeCardinality typeCardinality() {
				return TypeCardinality.OBJECT;
			}
			
			@Override
			public Class<? extends Resource> representingResourceType() {
				return type;
			}
			
			@Override
			public List<GenericAttribute> attributes() {
				return Collections.emptyList();
			}
		};
		
	}
	
	private static EntryType getEntryType(Class<? extends Resource> type) {
		return new EntryType() {

			/*@Override
			public Class<? extends Resource> getType() {
				return type;
			}*/

			@Override
			public Cardinality getCardinality() {
				return Cardinality.SINGLE_VALUE_REQUIRED;
			}

			@Override
			public GenericDataTypeDeclaration getType() {
				return getGenericDataTypeDeclaration(type);
			}
			
		};		
	}
	
	public static EntryType getEntryType(GaRoDataTypeI type) {
		return new EntryType() {
			@Override
			public Cardinality getCardinality() {
				return Cardinality.MULTIPLE_REQUIRED;
			}

			@Override
			public GenericDataTypeDeclaration getType() {
				return type;
			}
			
		};		
	}
	
	/** Get existing or virtual resource. Note that name of virtual resource will be replaced when actual
	 * create process takes place
	 */
	public static <T extends Resource> T getSubResourceOfTypeSingle(Resource parent, Class<T> t) {
		List<T> resOfType = parent.getSubResources(t, false);
		if(resOfType.size() > 1) throw new IllegalStateException("Found "+resOfType.size()+" resources of expected single type "+t.getName());
		if(resOfType.isEmpty()) {
			return parent.getSubResource("Virtual"+t.getSimpleName(), t);
		} else return resOfType.get(0);		
	}
	
	/** Like {@link #getSubResourceOfTypeSingle(Resource, Class)}, but returns null if the resource does not exist
	 */
	public static <T extends Resource> T getSubResourceOfTypeSingleIfExisting(Resource parent, Class<T> t) {
		List<T> resOfType = parent.getSubResources(t, false);
		if(resOfType.size() > 1) throw new IllegalStateException("Found "+resOfType.size()+" resources of expected single type "+t.getName());
		if(resOfType.isEmpty()) {
			return null;
		} else return resOfType.get(0);		
	}
	
	public static LogicProviderPublicData getLogicProviderByName(String moduleClassName, ExtensionResourceAccessInitData appData) {
		for(LogicProviderPublicData provider : appData.systemAccessForPageOpening().getLogicProviders(null)) {
			if(provider.getClass().getName().equals(moduleClassName)) {
				return provider;
			}
		}
		return null;		
	}
}
