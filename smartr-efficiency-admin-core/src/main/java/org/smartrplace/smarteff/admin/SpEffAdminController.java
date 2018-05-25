package org.smartrplace.smarteff.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.tools.timeseriesimport.api.TimeseriesImport;
import org.ogema.util.evalcontrol.EvalScheduler;
import org.ogema.util.evalcontrol.EvalScheduler.OverwriteMode;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionGeneralData;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;
import org.smartrplace.extensionservice.driver.DriverProvider;
import org.smartrplace.smarteff.admin.config.SmartEffAdminData;
import org.smartrplace.smarteff.admin.gui.NaviOverviewPage;
import org.smartrplace.smarteff.admin.gui.ResTypePage;
import org.smartrplace.smarteff.admin.object.NavigationPageData;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.smarteff.admin.timeseries.GenericDriverProvider;
import org.smartrplace.smarteff.admin.util.ConfigIdAdministration;
import org.smartrplace.smarteff.admin.util.GUIPageAdministation;
import org.smartrplace.smarteff.admin.util.ResourceLockAdministration;
import org.smartrplace.smarteff.admin.util.TypeAdministration;
import org.smartrplace.util.format.ValueFormat;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.timeseries.eval.api.configuration.Configuration;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;

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
		
		@Override
		@Deprecated
		public long[] calculateKPIs(GaRoSingleEvalProvider eval, Resource entryResource,
				List<Configuration<?>> configurations,
				Resource userData, ExtensionUserDataNonEdit userDataNonEdit,
				List<DriverProvider> drivers, boolean saveJsonResult,
				int defaultIntervalsToCalculate) {
			EvalScheduler scheduler = serviceAccess.evalResultMan().getEvalScheduler();
			if(scheduler == null) throw new IllegalStateException("We need an implementation with scheduler here!");
			
			int[] intarray = new int[INTERVALS_OFFERED.length];
			for(int i=0; i<INTERVALS_OFFERED.length; i++) {
				intarray[i] = INTERVALS_OFFERED[i];
			}
			
			List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse = new ArrayList<>();
			List<DriverProvider> driversToCheck;
			if(drivers == null) driversToCheck = guiPageAdmin.drivers;
			else driversToCheck = drivers;
			for(DriverProvider driver: driversToCheck) {
				int idx = 0;
				for(EntryType et: driver.getEntryTypes()) {
					if(et.getType().representingResourceType().equals(entryResource.getResourceType())) {
						DataProvider<?> dp = driver.getDataProvider(idx, Arrays.asList(new Resource[]{entryResource}),
								userDataNonEdit.editableData(), userDataNonEdit);
						if(dp != null) dataProvidersToUse.add((GaRoMultiEvalDataProvider<?>) dp);
						break;
					}
					idx++;
				}
			}
			if(dataProvidersToUse.isEmpty()) return null;
			
			String subConfigId = entryResource.getLocation();
			MultiKPIEvalConfiguration startConfig = scheduler.getOrCreateConfig(eval.id(),
					subConfigId);
			long[] result = scheduler.getStandardStartEndTime(startConfig, defaultIntervalsToCalculate);
			result = scheduler.queueEvalConfig(startConfig, saveJsonResult, null,
					result[0], result[1], dataProvidersToUse, true, OverwriteMode.NO_OVERWRITE);
			
			
			//TODO: We should hand in data providers with eval call, this is not thread-safe!
			scheduler.setStandardDataProvidersToUse(dataProvidersToUse);
			//List<MultiKPIEvalConfiguration> result = scheduler.registerProviderForKPI(eval, true, intarray , true, gwIds);
			//scheduler.deactivateAutoEvaluation(eval.id());
			
			return result;
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
