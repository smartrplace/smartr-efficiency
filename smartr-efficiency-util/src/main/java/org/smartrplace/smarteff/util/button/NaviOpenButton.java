package org.smartrplace.smarteff.util.button;

import java.util.Arrays;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForCreate;
import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForPageOpening;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.EditPageBase;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.RedirectButton;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class NaviOpenButton extends RedirectButton {
	protected static final long serialVersionUID = -4145439981103486352L;
	//protected final Class<? extends Resource> defaultType;
	protected final ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
	protected final PageType pageType;
	//doCreate only relevant for pageType EDIT
	protected final boolean doCreate;
	protected Object getContext(ExtensionResourceAccessInitData appData, Resource object) {return null;}
	
	protected final ButtonControlProvider controlProvider;
	
	//Override especially for pages that declare opening resource types == 0
	/** Adapt this if {@link EditPageBase#getReqData(OgemaHttpRequest) is changed}*/
	protected Resource getResource(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
		if(appData.entryResources() == null) return null;
		return appData.entryResources().get(0);
	}
	//protected Class<? extends Resource> type(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
	//	return defaultType;
	//}
	protected NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
			Class<? extends Resource> type, PageType typeRequested, OgemaHttpRequest req) {
		return SPPageUtil.getPageData(appData, type, pageType);
	}
	public NaviOpenButton(WidgetPage<?> page, String id, String pid, String text,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			PageType pageType, boolean doCreate, ButtonControlProvider controlProvider) {
		super(page, id, text);
		this.exPage = exPage;
		this.pageType = pageType;
		this.doCreate = doCreate;
		this.controlProvider = controlProvider;
	}
	public NaviOpenButton(OgemaWidget parent, String id, String pid, String text,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			PageType pageType, boolean doCreate, ButtonControlProvider controlProvider, OgemaHttpRequest req) {
		super(parent, id, text, "", req);
		this.exPage = exPage;
		this.pageType = pageType;
		this.doCreate = doCreate;
		this.controlProvider = controlProvider;
	}
	@Override
	public void onGET(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		final Class<? extends Resource> type = getEntryType(appData, req);
		/*if(this instanceof CreateButtonI) {
			type = ((CreateButtonI)this).typeToCreate();
		} else {
			final Resource object = getResource(appData, req);
			type = object.getResourceType();
		}*/
		if(getPageData(appData, type, pageType, req) == null)
			disable(req);
		else enable(req);
	}

	@Override
	public void onPrePOST(String data, OgemaHttpRequest req) {
		if(controlProvider != null) {
			setOpenInNewTab(controlProvider.openInNewTab(req), req);
		}

		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		final Resource object = getResource(appData, req);
		final Class<? extends Resource> type = getEntryType(appData, req);
		NavigationPublicPageData pageData = getPageData(appData, type, pageType, req);
		final String configId = getConfigId(pageType, object, appData.systemAccessForPageOpening(),
				pageData, doCreate, type, getContext(appData, object)); //type(appData, req)
		if(configId.startsWith(CapabilityHelper.ERROR_START)) setUrl("error/"+configId, req);
		else setUrl(pageData.getUrl()+"?configId="+configId, req);
	}

	private Class<? extends Resource> getEntryType(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
		if(this instanceof CreateButtonI) {
			return ((CreateButtonI)this).typeToCreate(appData, req);
		} else {
			final Resource object = getResource(appData, req);
			if(object == null) return null;
			return object.getResourceType();
		}
	}
	
	protected String getConfigId(PageType pageType, Resource object,
			ExtensionPageSystemAccessForPageOpening systemAccess,
			NavigationPublicPageData pageData, boolean doCreate,
			Class<? extends Resource> type, Object context) {
		if((this instanceof CreateButtonI) && doCreate) { //if((pageType == PageType.EDIT_PAGE) && doCreate) {
			return ((ExtensionPageSystemAccessForCreate)systemAccess).accessCreatePage(pageData, SPPageUtil.getEntryIdx(pageData, type),
				object);
		} else {
			if(object == null)  {
				return systemAccess.accessPage(pageData, -1, null, context);			
			}
			else {
				return systemAccess.accessPage(pageData, SPPageUtil.getEntryIdx(pageData, type),
						Arrays.asList(new Resource[]{object}), context);			
			}
		}
		
	}
	
	//This does not make sense for a RedirectButton
	@Override
	public void registerDependentWidget(OgemaWidget other) {}
	@Override
	public void registerDependentWidget(OgemaWidget other, OgemaHttpRequest req) {}
}
