package org.smartrplace.smarteff.util.editgeneric;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.ValueResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric.TypeResult;

/** Utility class that allows to determine type of sub resources*/
public class SubTypeHandler {
	public SubTypeHandler(Class<? extends Resource> primaryEntryTypeClass, ApplicationManagerSPExt appManExt) {
		this.primaryEntryTypeClass = primaryEntryTypeClass;
		this.appManExt = appManExt;
	}
	private final ApplicationManagerSPExt appManExt;
	private final Class<? extends Resource> primaryEntryTypeClass;
	
	@SuppressWarnings("unchecked")
	private void fillMap(Map<String, Class<? extends Resource>> typeMap, Class<? extends Resource> resType) {
		//typeMap.put("name", StringResource.class);
		for(Method m: resType.getMethods()) {
			Class<?> rawClass = m.getReturnType();
			if(Resource.class.isAssignableFrom(rawClass) && (m.getParameterCount()==0)) {
				typeMap.put(m.getName(), (Class<? extends Resource>) rawClass);				
			}
		}
		for(Class<? extends Resource> rawClass: appManExt.getSubTypes(resType)) {
			ExtensionResourceTypeDeclaration<? extends Resource> decl = appManExt.getTypeDeclaration(rawClass);
			String name = CapabilityHelper.getSingleResourceName(rawClass);
			if(SPPageUtil.isMulti(decl.cardinality()))
				typeMap.put(name, ResourceList.class);
			else
				typeMap.put(name, rawClass);
		}
	}
	Map<String, Class<? extends Resource>> typesGlob = null;
	Map<String, Class<? extends Resource>> getTypes() {
		if(typesGlob == null) {
			typesGlob = new HashMap<>();
			fillMap(typesGlob, primaryEntryTypeClass);
		}
		return typesGlob;
	}
	//ResourceType -> (Element Name -> Type)
	Map<String, Map<String, Class<? extends Resource>>> subTypesGlob = new HashMap<>();
	public Map<String, Class<? extends Resource>> getSubTypes(Class<? extends Resource> parentType) {
		String typeStr = parentType.getName();
		Map<String, Class<? extends Resource>> subTypeMap = subTypesGlob.get(typeStr);
		if(subTypeMap == null) {
			subTypeMap = new HashMap<>();
			subTypesGlob.put(typeStr, subTypeMap);
			fillMap(subTypeMap, parentType);
		}
		return subTypeMap;
	}
	
	public TypeResult getType(String subPath) {
		if(subPath.startsWith("#$ResourceType:")) {
			String requested = subPath.substring("#$ResourceType:".length());
			Class<? extends Resource> type = null;
			for(ExtensionResourceTypeDeclaration<?> known: appManExt.getAllTypeDeclarations()) {
				if(known.dataType().getName().equals(requested)) {
					type = known.dataType();
					TypeResult result = new TypeResult(ResourceList.class);
					result.elementType = type;
					return result;
				}
			}
			return null;
		}
		if(subPath.startsWith("#")) {
			return new TypeResult(subPath);
		}
		String[] els = subPath.split("/");
		Class<? extends Resource> cr = primaryEntryTypeClass;
		for(int i=0; i<=els.length; i++) {
			if(cr == null)
				return null;
			if(i == els.length) {
				if(ResourceList.class.isAssignableFrom(cr)) {
					TypeResult result = new TypeResult(cr);
					ExtensionResourceTypeDeclaration<?> superType = CapabilityHelper.getTypeFromName(els[i-1], appManExt);
					result.elementType = superType.dataType();
					return result;
				}
				return new TypeResult(cr);
			}
			//we cannot go over ValueResources and ResourceLists here
			if(els[i].contains("#$")) {
				int inStringIdx = els[i].indexOf("#$");
				//String intString = els[i].substring(inStringIdx+2);
				String realSub = els[i].substring(0, inStringIdx);
				//int index = Integer.parseInt(intString);
				cr = getElementClassOfResourceList(cr, realSub);
				if(cr == null)
					throw new IllegalStateException("Path "+subPath+" does not reference ResourceList or could not be processed at "+els[i]+"!");
				continue;
			}
			if(ValueResource.class.isAssignableFrom(cr)) throw new IllegalStateException("Path "+subPath+" includes a ValueResouce in middle!");
			if(ResourceList.class.isAssignableFrom(cr)) {
				throw new IllegalStateException("Path "+subPath+" includes a ResourceList in middle!");
			}
			cr = getSubTypes(cr).get(els[i]);
		}
		//if(els.length == 1) return new TypeResult(cr);
		throw new IllegalStateException("we should never get here");		
	}
	
	@SuppressWarnings("unchecked")
	public static Class<? extends Resource> getElementClassOfResourceList(
			Class<? extends Resource> parentClass, String resListName) {
		for(Method m: parentClass.getMethods()) {
			Class<?> rawClass = m.getReturnType();
			if(ResourceList.class.isAssignableFrom(rawClass) && (m.getParameterCount()==0)) {
				//Analyse this;
				if(m.getName().equals(resListName)) {
					//getSubType
					Type genClass = m.getGenericReturnType();
					if(genClass instanceof ParameterizedType) {
						Type[] types = ((ParameterizedType)genClass).getActualTypeArguments();
						if(types.length == 1) {
							if(types[0] instanceof Class) {
								Class<?> cl = (Class<?>)(types[0]);
								if(Resource.class.isAssignableFrom(cl))
									return (Class<? extends Resource>) cl;
							}
						}
					}
					return null;
				}
			}
		}
		return null;
		
	}

}
