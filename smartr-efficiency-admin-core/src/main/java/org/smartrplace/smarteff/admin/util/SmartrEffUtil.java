package org.smartrplace.smarteff.admin.util;

import org.ogema.core.model.Resource;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.extensionservice.resourcecreate.ProviderPublicDataForCreate.PagePriority;
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
	
	public static int comparePagePriority(NavigationPublicPageData page1, NavigationPublicPageData page2,
			Class<? extends Resource> type) {
		int isDirect1 = getEntryTypePosition(page1, type);
		int isDirect2 = getEntryTypePosition(page2, type);
		if((isDirect1>=0) && (isDirect2<0)) return 3;
		if((isDirect2>=0) && (isDirect1<0)) return -3;
		if((isDirect1>=0) && (isDirect2>=0)) return comparePagePriorities(page1.getPriority(),
				page2.getPriority(), isDirect1, isDirect2);
		
		Class<? extends Resource> inherited1 = getClosestInheritedEntry(page1, type);
		Class<? extends Resource> inherited2 = getClosestInheritedEntry(page2, type);
		if((inherited1 != null)&&(inherited2 == null)) return 1;
		if((inherited2 != null)&&(inherited1 == null)) return -1;
		if((inherited1 == null)&&(inherited2 == null)) return 0;
		int prioComp = comparePagePriorities(page1.getPriority(), page2.getPriority());
		if(prioComp != 0) return prioComp;
		if(getCloserInherited(inherited1, inherited2, type).equals(inherited1)) return 4;
		return -4;
	}

	public static int getEntryTypePosition(NavigationPublicPageData page,
			Class<? extends Resource> type) {
		int idx = 0;
		for(EntryType et: page.getEntryTypes()) {
			if(et.getType().representingResourceType().equals(type))
				return idx;
			idx++;
		}
		return -1;
	}
	
	public static int comparePagePriorities(PagePriority prioA, PagePriority prioB,
			int posA, int posB) {
		int pagePrioRes = SmartrEffUtil.comparePagePriorities(prioA, prioB);
		if(pagePrioRes != 0) return pagePrioRes;
		return Integer.compare(posA, posB);
	}
	
	public static Class<? extends Resource> getClosestInheritedEntry(NavigationPublicPageData page,
			Class<? extends Resource> type) {
		// we do not want inheritance to prototypes here
		if((!SmartEffResource.class.isAssignableFrom(type))) return null;
		
		Class<? extends Resource> result = null;
		for(EntryType et: page.getEntryTypes()) {
			if(result == null) {
				if(et.getType().representingResourceType().isAssignableFrom(type)) {
					result = et.getType().representingResourceType();
				}
			} else {
				result = getCloserInherited(result, et.getType().representingResourceType(), type);
			}
		}
		return result;
	}
	
	public static <T> Class<? extends T> getCloserInherited(Class<? extends T> classA, Class<? extends T> classB, Class<?> child) {
		boolean isA = classA.isAssignableFrom(child);
		boolean isB = classB.isAssignableFrom(child);
		if(isA && (!isB)) return classA;
		if(isB && (!isA)) return classB;
		if((!isA) && (!isB)) return null;
		if(classA.isAssignableFrom(classB)) return classB;
		return classA;
	}
}
