package org.smartrplace.smarteff.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.tools.timeseriesimport.api.TimeseriesImport;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ApplicationManagerSpExtMinimal;
import org.smartrplace.extensionservice.ExtensionGeneralData;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.extensionservice.proposal.CalculatedData;
import org.smartrplace.extensionservice.proposal.CalculatedEvalResult;
import org.smartrplace.extensionservice.proposal.ProjectProposal;
import org.smartrplace.extensionservice.proposal.ProjectProposal100EE;
import org.smartrplace.extensionservice.proposal.ProjectProposalEfficiency;
import org.smartrplace.smarteff.access.api.GenericPageConfigurationProvider;
import org.smartrplace.smarteff.admin.config.SmartEffAdminData;
import org.smartrplace.smarteff.admin.gui.NaviOverviewPage;
import org.smartrplace.smarteff.admin.gui.ResTypePage;
import org.smartrplace.smarteff.admin.object.NavigationPageData;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.smarteff.admin.protect.NavigationPageSystemAccessForPageOpening;
import org.smartrplace.smarteff.admin.timeseries.GenericDriverProvider;
import org.smartrplace.smarteff.admin.util.ConfigIdAdministration;
import org.smartrplace.smarteff.admin.util.GUIPageAdministation;
import org.smartrplace.smarteff.admin.util.ResourceLockAdministration;
import org.smartrplace.smarteff.admin.util.TypeAdministration;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.format.ValueFormat;

import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;
import de.iwes.widgets.html.filedownload.FileDownload;

public class SpEffAdminController {
	public final static String APPCONFIGDATA_LOCATION = ValueFormat.firstLowerCase(SmartEffAdminData.class.getSimpleName());
	public static final Integer[] INTERVALS_OFFERED =
			new Integer[]{AbsoluteTiming.DAY, AbsoluteTiming.WEEK, AbsoluteTiming.MONTH};
	
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
	public final GenericDriverProvider tsDriver;
	public final ApplicationManagerSpExtMinimal appManExtMin = new ApplicationManagerSpExtMinimal() {
		
		@Override
		public long getFrameworkTime() {
			if(appMan != null) return appMan.getFrameworkTime();
			return -1;
		}
		
		@Override
		public NavigationPublicPageData getMaximumPriorityPageStatic(Class<? extends Resource> type, PageType pageType) {
			return NavigationPageSystemAccessForPageOpening.getMaximumPriorityPageStatic(type, pageType,
					guiPageAdmin.navigationPublicData);
		}
	};
	
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
		public List<ExtensionResourceTypeDeclaration<?>> getAllTypeDeclarations() {
			List<ExtensionResourceTypeDeclaration<?>> result = new ArrayList<>();
			for(SmartrEffExtResourceTypeData data: typeAdmin.resourceTypes.values()) {
				result.add(data.typeDeclaration);
			}
			return result;
		}
		
		/** Get declared types that are parents in the Java inheritance chain. Resource and prototypes are omitted*/
		@Override
		public List<ExtensionResourceTypeDeclaration<?>> getInheritedParentTypes(Class<? extends Resource> type) {
			List<ExtensionResourceTypeDeclaration<?>> result = new ArrayList<>();
			Set<Class<? extends SmartEffResource>> parents = typeAdmin.inheritedTypes.get(type);
			if(parents == null) return Collections.emptyList();
			for(Class<? extends SmartEffResource> parent: parents) {
				result.add(typeAdmin.resourceTypes.get(parent).typeDeclaration);
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

		@Override
		public List<Class<? extends Resource>> getSystemTypes() {
			List<Class<? extends Resource>> result = new ArrayList<>();
			result.add(Resource.class);
			result.add(CalculatedData.class);
			result.add(ProjectProposal.class);
			result.add(ProjectProposalEfficiency.class);
			result.add(ProjectProposal100EE.class);
			result.add(CalculatedEvalResult.class);
			return result ;
		}

		@Override
		public NavigationPublicPageData getMaximumPriorityPageStatic(Class<? extends Resource> type,
				PageType pageType) {
			return NavigationPageSystemAccessForPageOpening.getMaximumPriorityPageStatic(type, pageType,
					guiPageAdmin.navigationPublicData);
		}

		@Override
		public FileDownload getFileDownload(WidgetPage<?> page, String widgetId)  {
			return new FileDownload(page, widgetId, appMan.getWebAccessManager(), true);		
		}
		
		@Override
		public String registerStaticConfigId(int entryIdx, List<Resource> entryResources,
				Object context) {
			return configIdAdmin.getConfigId(entryIdx, entryResources, null,
					null, context, null, null);
		}
	};
	
    public SpEffAdminController(ApplicationManager appMan, ServiceAccess evaluationOCApp, final WidgetApp widgetApp,
    		TimeseriesImport csvImport) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.widgetApp = widgetApp;
		this.serviceAccess = evaluationOCApp;
		this.tsDriver = new GenericDriverProvider(csvImport, appMan);
		init();
	}
    
    protected void init() {
		userAdmin = new UserAdmin(this);
		this.typeAdmin = new TypeAdministration(this);
		this.guiPageAdmin = new GUIPageAdministation(this);    	
		pageAdmin = new StandardPageAdmin(widgetApp, this);
    }
    
    public void processOpenServices() {
		for(SmartEffExtensionService service: serviceAccess.providersToProcess()) {
			processNewService(service);
		}    	
		for(GenericPageConfigurationProvider service: serviceAccess.pageConfigProvidersToProcess()) {
			processNewPageConfigService(service);
		}    	
    }

    public void processNewService(SmartEffExtensionService service) {
    	service.start(appManExt);
     	
    	typeAdmin.registerService(service);
    	guiPageAdmin.registerService(service);
    	
    	
    	servicesKnown.add(service);
    }
    public void processNewPageConfigService(GenericPageConfigurationProvider service) {
    	//nothing to do for now
    }
       
    public void unregisterService(SmartEffExtensionService service) {
    	servicesKnown.remove(service);
    	typeAdmin.unregisterService(service);
    	guiPageAdmin.unregisterService(service);
    }
    public void unregisterPageProviderService(GenericPageConfigurationProvider service) {
    	//nothing to do for now
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

	public ObjectGUITablePage<NavigationPageData, Resource> getNaviPage(WidgetPage<?> pageNavis, NavigationPageData navi) {
		return new NaviOverviewPage(pageNavis, this, navi, true);
	}
	public ResTypePage getResTypePage(WidgetPage<?> pageNavis, SmartrEffExtResourceTypeData initData) {
		return new ResTypePage(pageNavis, this, initData);
	}

	public boolean registerExtendedNaviMenuPages() {
		if(Boolean.getBoolean("org.smartrplace.smarteff.admin.reducedStandardPages"))
			return false;
		return true;
	}

}
