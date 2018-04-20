package org.smartrplace.smarteff.admin.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.core.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extenservice.proposal.ProposalProvider;
import org.smartrplace.extenservice.proposal.ProposalPublicData;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.admin.object.NavigationPageData;
import org.smartrplace.smarteff.admin.object.ProposalProviderData;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData.ServiceCapabilities;
import org.smartrplace.smarteff.admin.protect.NavigationPublicPageDataImpl;
import org.smartrplace.smarteff.admin.protect.ProposalPublicDataImpl;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class GUIPageAdministation {
	public List<NavigationPageData> startPages = new ArrayList<>();
	public List<NavigationPageData> navigationPages = new ArrayList<>();
	public List<ProposalProviderData> proposalProviders = new ArrayList<>();
	public Map<Class<? extends Resource>, List<NavigationPublicPageData>> navigationPublicData = new HashMap<>();
	public List<NavigationPublicPageData> startPagesData = new ArrayList<>();
	public Map<Class<? extends Resource>, List<ProposalPublicData>> proposalInfo = new HashMap<>();
	private final SpEffAdminController app;
	
	public GUIPageAdministation(SpEffAdminController app) {
		this.app = app;
	}

	public NavigationPageData selectedStartPage;
	
	protected final static Logger logger = LoggerFactory.getLogger(SpEffAdminController.class);
	
	public void registerService(SmartEffExtensionService service) {
    	ServiceCapabilities caps = SmartrEffExtResourceTypeData.getServiceCaps(service);
    	int i=0;
    	for(NavigationGUIProvider navi: caps.naviProviders) try {
    		String id = WidgetHelper.getValidWidgetId(SPPageUtil.buildId(navi));
    		String url = SPPageUtil.getProviderURL(navi);
    		WidgetPage<?> page = app.widgetApp.createWidgetPage(url);
  		
  			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> dataExPage = app.getUserAdmin().
  					getNaviPage(page, url, "dataExplorer.html", id, navi);
    		navi.initPage(dataExPage, app.appManExt);
    		
    		NavigationPageData data = new NavigationPageData(navi, service, url, dataExPage);
    		app.pageAdmin.registerPage(data, page);
			if(navi.getEntryTypes() == null) {
				startPages.add(data);
				startPagesData.add(new NavigationPublicPageDataImpl(data));
			}
			else {
				navigationPages.add(data);
				for(EntryType t: navi.getEntryTypes()) {
					List<NavigationPublicPageData> listPub = navigationPublicData.get(t.getType());
					if(listPub == null) {
						listPub = new ArrayList<>();
						navigationPublicData.put(t.getType(), listPub);
					}
					NavigationPublicPageData dataPub = new NavigationPublicPageDataImpl(data);
					listPub.add(dataPub);
					System.out.println("Navi-URL: "+url+" EntryType:"+t.getType().getSimpleName()+" List# now:"+listPub.size());				 
				}
			}
			i++;
    	} catch(Exception e) {
			System.out.println("Navi-Provider["+i+"] failed: Label:"+navi.label(null)+" service:"+service.getClass().getSimpleName());
			e.printStackTrace();
		}
    	
    	i = 0;
    	for(ProposalProvider navi: caps.proposalProviders) try {
    		navi.init(app.appManExt);
    		
    		ProposalProviderData data = new ProposalProviderData(navi, service);
			proposalProviders.add(data);
			if(navi.getEntryTypes() == null) continue;
			for(EntryType t: navi.getEntryTypes()) {
				List<ProposalPublicData> listPub = proposalInfo.get(t.getType());
				if(listPub == null) {
					listPub = new ArrayList<>();
					proposalInfo.put(t.getType(), listPub);
				}
				ProposalPublicDataImpl dataPub = new ProposalPublicDataImpl(data);
				listPub.add(dataPub);
			}
			i++;
    	} catch(Exception e) {
			System.out.println("Proposal-Provider["+i+"] failed: Label:"+navi.label(null)+" service:"+service.getClass().getSimpleName());				
			e.printStackTrace();
		}
	}
	
	public void unregisterService(SmartEffExtensionService service) {
    	ServiceCapabilities caps = SmartrEffExtResourceTypeData.getServiceCaps(service);
    	List<NavigationPageData> toRemove = new ArrayList<>();
    	String serviceId = SPPageUtil.buildId(service);
    	for(NavigationPageData navi: startPages) {
    		if(SPPageUtil.buildId(navi.parent).equals(serviceId)) toRemove.add(navi);
    	}
    	startPages.removeAll(toRemove);
    	navigationPages.removeAll(toRemove);
    	for(NavigationGUIProvider navi: caps.naviProviders) {
    		String naviId = SPPageUtil.buildId(navi);
 			if(navi.getEntryTypes() == null) continue;
			else for(EntryType t: navi.getEntryTypes()) {
				List<NavigationPublicPageData> listPub = navigationPublicData.get(t.getType());
				if(listPub == null) {
					logger.error("Navigation Public pages have no entry for "+t.getType().getName()+" when deregistering "+serviceId);
					continue;
				}
				for(NavigationPublicPageData l:listPub) {
					if(l.id().equals(naviId)) {
						listPub.remove(l);
						break;
					}
				}
				if(listPub.isEmpty()) navigationPublicData.remove(t.getType());
			}
    	}
    	
     	for(ProposalProvider navi: caps.proposalProviders) {
    		String naviId = SPPageUtil.buildId(navi);
 			if(navi.getEntryTypes() == null) continue;
			else for(EntryType t: navi.getEntryTypes()) {
				List<ProposalPublicData> listPub = proposalInfo.get(t.getType());
				if(listPub == null) {
					logger.error("Proposal providers have no entry for "+t.getType().getName()+" when deregistering "+serviceId);
					continue;
				}
				for(ProposalPublicData l:listPub) {
					if(l.id().equals(naviId)) {
						listPub.remove(l);
						break;
					}
				}
				if(listPub.isEmpty()) proposalInfo.remove(t.getType());
			}
    		
    	}

	}
	
	public Collection<NavigationPageData> getAllProviders() {
		 Set<NavigationPageData> result = new HashSet<>();
		 result.addAll(startPages);
		 result.addAll(navigationPages);
		 return result;
	}
}
