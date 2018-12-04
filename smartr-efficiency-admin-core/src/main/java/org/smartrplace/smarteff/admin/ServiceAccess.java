package org.smartrplace.smarteff.admin;

import java.util.List;
import java.util.Map;

import org.ogema.core.application.Application;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.smarteff.access.api.GenericPageConfigurationProvider;


public interface ServiceAccess extends Application {
	public Map<String, SmartEffExtensionService> getEvaluations();
	
	public EvalResultManagement evalResultMan();
	//public NavigationMenu getMenu();
	
	public Map<String, GenericPageConfigurationProvider> getPageConfigProviders();
	
	List<SmartEffExtensionService> providersToProcess();
    List<GenericPageConfigurationProvider> pageConfigProvidersToProcess();
}
