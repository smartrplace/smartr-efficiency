package org.smartrplace.smarteff.util;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionGeneralData;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration.Cardinality;
import org.smartrplace.extensionservice.ExtensionUserData;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;
import org.smartrplace.util.format.ValueFormat;

import de.iwes.util.resource.ResourceHelper;

public class CapabilityHelper {
	public static final String ERROR_START = "ERROR: ";
	public static final String STD_LOCATION = "smartEffAdminData/generalData/";
	public static final int STD_LOCATION_LENGTH = STD_LOCATION.length();
	
	public static String getNewMultiResourceName(Class<? extends Resource> type, Resource parent) {
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
		for(ExtensionResourceTypeDeclaration<?> decl: appManExt.getAllTypeDeclararions()) {
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
			if(decl == null) throw new IllegalStateException("Using resource type without type declaration!");
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
		
		String[] els = resourceInUser.getLocation().split("/", 3);
		if(els.length < 3) throw new IllegalStateException("Not a subpath of userDataSpace");
		String subPath = els[2];
		@SuppressWarnings("unchecked")
		T globalResource = (T) ResourceHelper.getSubResource(globalData, subPath , resourceInUser.getResourceType());
		return globalResource;
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
	
	private static EntryType getEntryType(Class<? extends Resource> type) {
		return new EntryType() {

			@Override
			public Class<? extends Resource> getType() {
				return type;
			}

			@Override
			public Cardinality getCardinality() {
				return Cardinality.SINGLE_VALUE_REQUIRED;
			}
			
		};		
	}}
