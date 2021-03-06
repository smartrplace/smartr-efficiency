package org.smartrplace.smarteff.util.button;

import java.util.Arrays;

import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForCreate;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForPageOpening;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.EditPageBase;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.util.format.WidgetHelper;

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
	protected final boolean isBackButton;
	private final Resource fixedParentResource;
	
	/** Overwrite this to provide a context object to the page opened*/
	protected Object getContext(ExtensionResourceAccessInitData appData, Resource object, OgemaHttpRequest req) {
		if(appData.getConfigInfo() == null) return null;
		return appData.getConfigInfo().lastContext;
	}
	protected String getDestinationURL(ExtensionResourceAccessInitData appData, Resource object, OgemaHttpRequest req) {
		return null;
	}
	
	protected final ButtonControlProvider controlProvider;
	
	//Override especially for pages that declare opening resource types == 0
	/** Adapt this if {@link EditPageBase#getReqData(OgemaHttpRequest) is changed}*/
	protected Resource getResource(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
		if(appData.entryResources() == null) return null;
		return appData.entryResources().get(0);
	}

	protected NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
			Class<? extends Resource> type, PageType typeRequested, OgemaHttpRequest req) {
		return SPPageUtil.getPageData(appData, type, pageType);
	}
	public NaviOpenButton(WidgetPage<?> page, String id, String pid, String text,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			PageType pageType, boolean doCreate, ButtonControlProvider controlProvider) {
		this(page, id, pid, text, exPage, pageType, doCreate, false, controlProvider);
	}
	public NaviOpenButton(WidgetPage<?> page, String id, String pid, String text,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			PageType pageType, boolean doCreate, boolean isBackButton,
			ButtonControlProvider controlProvider) {
		super(page, WidgetHelper.getValidWidgetId(id+pid), text);
		this.exPage = exPage;
		this.pageType = pageType;
		this.doCreate = doCreate;
		this.isBackButton = isBackButton;
		this.controlProvider = controlProvider;
		this.fixedParentResource = null;
	}
	public NaviOpenButton(OgemaWidget parent, String id, String pid, String text,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			PageType pageType, boolean doCreate, ButtonControlProvider controlProvider, 
			Resource parentResource, OgemaHttpRequest req) {
		super(parent, id+pid, text, "", req);
		this.exPage = exPage;
		this.pageType = pageType;
		this.doCreate = doCreate;
		this.controlProvider = controlProvider;
		this.isBackButton = false;
		this.fixedParentResource = parentResource;
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
		String url = getDestinationURL(appData, object, req);
		NavigationPublicPageData pageData;
		if(url != null) {
			pageData = appData.systemAccessForPageOpening().getPageByProvider(url);			
			if(pageData == null) {
				throw new IllegalStateException("Special Page to open for "+url+", "+pageType+", typesToShow:"+"not found in "+this.getClass().getSimpleName()+"! Maybe not registered?");
			}
		} else {
			pageData = getPageData(appData, type, pageType, req);
			if(pageData == null) {
				throw new IllegalStateException("Page to open for "+type.getSimpleName()+", "+pageType+", typesToShow:"+"not found in "+this.getClass().getSimpleName()+"! Maybe not registered?");
			}
			url = pageData.getUrl();
		}
		final String configId = getConfigId(pageType, object, appData.systemAccessForPageOpening(),
				pageData, doCreate, type, getContext(appData, object, req)); //type(appData, req)
		if(configId == null) {
			setUrl(url, req);
			return;
		}
		if(configId.startsWith(CapabilityHelper.ERROR_START)) {
			setUrl("error/"+configId, req);
			return;
		}
		setUrl(url+"?configId="+configId, req);
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
			Resource parent;
			if(fixedParentResource != null)
				parent = fixedParentResource;
			else parent = object;
			return ((ExtensionPageSystemAccessForCreate)systemAccess).accessCreatePage(pageData, SPPageUtil.getEntryIdx(pageData, type),
				parent);
		} else {
			if(object == null)  {
				return systemAccess.accessPage(pageData, -1, null, context, isBackButton);			
			}
			else {
				if(pageData.getEntryTypes() == null)
					return systemAccess.accessPage(pageData, -1,
							Arrays.asList(new Resource[]{object}), context, isBackButton);			
				else
					return systemAccess.accessPage(pageData, SPPageUtil.getEntryIdx(pageData, type),
							Arrays.asList(new Resource[]{object}), context, isBackButton);			
			}
		}
		
	}
	
	//This does not make sense for a RedirectButton. But we may have to register dependency (e.g. for SmartEffTimeseries data entry)
	@Override
	public void registerDependentWidget(OgemaWidget other) {}
	@Override
	public void registerDependentWidget(OgemaWidget other, OgemaHttpRequest req) {}
}
