package org.smartrplace.smarteff.util;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.proposal.ProposalPublicData;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.smarteff.defaultservice.BaseDataService;
import org.smartrplace.smarteff.util.button.TableOpenButton;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class StandardProposalTableOpenButton extends TableOpenButton {
	private static final long serialVersionUID = 1L;
	
	public  StandardProposalTableOpenButton(WidgetPage<?> page, String id, String pid, String text,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage) {
		super(page, id, pid, text, exPage, null);
	}

	public  StandardProposalTableOpenButton(OgemaWidget parent, String id, String pid, String text,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			OgemaHttpRequest req) {
		super(parent, id, pid, text, exPage, null, req);
	}
	
	@Override
	protected NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
			Class<? extends Resource> type, PageType typeRequested, OgemaHttpRequest req) {
		return appData.systemAccessForPageOpening().getPageByProvider(SPPageUtil.getProviderURL(BaseDataService.PROPOSALTABLE_PROVIDER));//super.getPageData(appData, type, typeRequested);
	}
	
	@Override
	public void onGET(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		Class<? extends Resource> type = getResource(appData, req).getResourceType();
		List<ProposalPublicData> provs = appData.systemAccessForPageOpening().getProposalProviders(type);
		if(provs.isEmpty()) disable(req);
		else enable(req);
	}
}
