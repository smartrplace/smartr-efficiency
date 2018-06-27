package org.smartrplace.smarteff.util.button;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.extensionservice.proposal.CalculatedData;
import org.smartrplace.extensionservice.proposal.ProjectProposal;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.defaultservice.BaseDataService;
import org.smartrplace.smarteff.util.SPPageUtil;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class ProposalResTableOpenButton extends TableOpenButton {
	private static final long serialVersionUID = 1L;
	protected final Class<? extends CalculatedData> resultType;

	public static final Map<OgemaLocale, String> BUTTON_TEXTS = new HashMap<>();
	static {
		BUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Results");
		BUTTON_TEXTS.put(OgemaLocale.GERMAN, "Ergebnisse");
		BUTTON_TEXTS.put(OgemaLocale.FRENCH, "Résultats");
	}

	public ProposalResTableOpenButton(WidgetPage<?> page, String id, String pid,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider,
			Class<? extends CalculatedData> resultType) {
		super(page, id, pid, "", exPage, controlProvider);
		setDefaultOpenInNewTab(false);
		this.resultType = resultType;
	}

	public ProposalResTableOpenButton(OgemaWidget parent, String id, String pid,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider, Class<? extends CalculatedData> resultType,
			OgemaHttpRequest req) {
		super(parent, id, pid, "", exPage, controlProvider, req);
		this.resultType = resultType;
	}

	@Override
	protected NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
			Class<? extends Resource> type, PageType typeRequested, OgemaHttpRequest req) {
		if(resultType == null || (ProjectProposal.class.isAssignableFrom(resultType)))
			return appData.systemAccessForPageOpening().getPageByProvider(SPPageUtil.getProviderURL(BaseDataService.RESULTTABLE_PROVIDER));//super.getPageData(appData, type, typeRequested);
		else
			throw new IllegalStateException("Only ProjectProposals supported so far!");
			//return appData.systemAccessForPageOpening().getPageByProvider(SPPageUtil.getProviderURL(BaseDataService.RESBYTYPE_PROVIDER));
	}
	
	@Override
	public void onGET(OgemaHttpRequest req) {
		super.onGET(req);
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		String text = BUTTON_TEXTS.get(req.getLocale());
		int size = getSize(getResource(appData, req), appData, resultType);
		if(text == null) text = BUTTON_TEXTS.get(OgemaLocale.ENGLISH);
		setText(text+"("+size+")", req);
	}

	public static int getSize(Resource myResource, ExtensionResourceAccessInitData appData,
			Class<? extends CalculatedData> resultType) {
		List<? extends CalculatedData> data;
		if(resultType == null)
			data = myResource.getSubResources(ProjectProposal.class, true);
		else
			data = myResource.getSubResources(resultType, true);
		return data.size();
	}
}
