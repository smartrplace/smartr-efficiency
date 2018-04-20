package org.smartrplace.smarteff.admin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionGeneralData;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;
import org.smartrplace.smarteff.admin.config.SmartEffAdminData;
import org.smartrplace.smarteff.admin.gui.NaviOverviewPage;
import org.smartrplace.smarteff.admin.gui.ResTypePage;
import org.smartrplace.smarteff.admin.object.NavigationPageData;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.smarteff.admin.util.ConfigIdAdministration;
import org.smartrplace.smarteff.admin.util.GUIPageAdministation;
import org.smartrplace.smarteff.admin.util.ResourceLockAdministration;
import org.smartrplace.smarteff.admin.util.TypeAdministration;
import org.smartrplace.util.format.ValueFormat;

import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class SpEffAdminController {
	public final static String APPCONFIGDATA_LOCATION = ValueFormat.firstLowerCase(SmartEffAdminData.class.getSimpleName());
	
	public final ServiceAccess serviceAccess;
	public final OgemaLogger log;
    public final ApplicationManager appMan;
    public final WidgetApp widgetApp;

	public Set<SmartEffExtensionService> servicesKnown = new HashSet<>();
	public GUIPageAdministation guiPageAdmin;

	public ResourceLockAdministration lockAdmin = new ResourceLockAdministration();
	public ConfigIdAdministration configIdAdmin = new ConfigIdAdministration();
	public TypeAdministration typeAdmin;
	private UserAdmin userAdmin;
	public StandardPageAdmin pageAdmin;
	
	public final ApplicationManagerSPExt appManExt = new ApplicationManagerSPExt() {
		
		@SuppressWarnings("unchecked")
		@Override
		public <T extends Resource> ExtensionResourceTypeDeclaration<T> getTypeDeclaration(
				Class<? extends T> resourceType) {
			SmartrEffExtResourceTypeData typeData = typeAdmin.resourceTypes.get(resourceType);
			if(typeData == null) return null;
			return (ExtensionResourceTypeDeclaration<T>) typeData.typeDeclaration;
		}
		
		public List<Class<? extends Resource>> getSubTypes(Class<? extends Resource> parentType) {
			List<Class<? extends Resource>> result = new ArrayList<>();
			for(Entry<Class<? extends SmartEffResource>, SmartrEffExtResourceTypeData> e: typeAdmin.resourceTypes.entrySet()) {
				if((e.getValue().typeDeclaration.parentType() != null) && e.getValue().typeDeclaration.parentType().isAssignableFrom(parentType)) {
					result.add(e.getKey());
				}
			}
			return result;
		}

		@Override
		public List<ExtensionResourceTypeDeclaration<?>> getAllTypeDeclararions() {
			List<ExtensionResourceTypeDeclaration<?>> result = new ArrayList<>();
			for(SmartrEffExtResourceTypeData data: typeAdmin.resourceTypes.values()) {
				result.add(data.typeDeclaration);
			}
			return result;
		}

		@Override
		public ExtensionGeneralData globalData() {
			SmartEffAdminData appCD = getUserAdmin().getAppConfigData();
			return appCD.globalData();
		}

		@Override
		public long getFrameworkTime() {
			if(appMan != null) return appMan.getFrameworkTime();
			return -1;
		}

		@Override
		public OgemaLogger log() {
			if(appMan != null) return appMan.getLogger();
			return null;
		}
	};
	
    public SpEffAdminController(ApplicationManager appMan, ServiceAccess evaluationOCApp, final WidgetApp widgetApp) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.widgetApp = widgetApp;
		this.serviceAccess = evaluationOCApp;
		init();
	}
    
    protected void init() {
		userAdmin = new UserAdmin(this);
		this.typeAdmin = new TypeAdministration(this);
		this.guiPageAdmin = new GUIPageAdministation(this);    	
		pageAdmin = new StandardPageAdmin(widgetApp, this);
    }
    
    public void processOpenServices() {
		for(SmartEffExtensionService service: serviceAccess.getEvaluations().values()) {
			processNewService(service);
		}    	
    }

    public void processNewService(SmartEffExtensionService service) {
    	service.start(appManExt);
     	
    	typeAdmin.registerService(service);
    	guiPageAdmin.registerService(service);

    	servicesKnown.add(service);
    }
    
    public void unregisterService(SmartEffExtensionService service) {
    	servicesKnown.remove(service);
    	typeAdmin.unregisterService(service);
    	guiPageAdmin.unregisterService(service);
    }
    
    
	public void close() {
    }

	/** Here the action is performed without checking user permissions*/
	/*public <T extends SmartEffResource> T addResource(SmartEffResource parent,
			String name, Class<T> type, SmartEffUserDataNonEdit userData, NavigationGUIProvider entryProvider) {
		T result = parent.getSubResource(name, type);
		result.create();
		//entryProvider.initResource(result);
		SmartrEffExtResourceTypeData rtd = typeAdmin.resourceTypes.get(type);
		rtd.registerElement(result);
		return result;
	}*/
	public void removeResource(SmartEffResource object) {
		// TODO Auto-generated method stub
		
	}

	public NavigationMenu getNavigationMenu() {
		return pageAdmin.menu;
	}
	
	public UserAdmin getUserAdmin() {
		return userAdmin;
	}

	public NaviOverviewPage getNaviPage(WidgetPage<?> pageNavis, NavigationPageData navi) {
		return new NaviOverviewPage(pageNavis, this, navi);
	}
	public ResTypePage getResTypePage(WidgetPage<?> pageNavis, SmartrEffExtResourceTypeData initData) {
		return new ResTypePage(pageNavis, this, initData);
	}

}
