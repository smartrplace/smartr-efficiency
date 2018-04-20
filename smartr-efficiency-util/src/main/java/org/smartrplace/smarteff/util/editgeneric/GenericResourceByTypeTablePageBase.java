package org.smartrplace.smarteff.util.editgeneric;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extenservice.resourcecreate.ProviderPublicDataForCreate.PagePriority;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.defaultservice.ResourceTablePage;
import org.smartrplace.smarteff.util.button.AddEntryButton;
import org.smartrplace.smarteff.util.button.BackButton;
import org.smartrplace.smarteff.util.button.TabButton;
import org.smartrplace.smarteff.util.button.TableOpenButton;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.RedirectButton;
import extensionmodel.smarteff.api.common.HeatCostBillingInfo;

public class GenericResourceByTypeTablePageBase<T extends Resource> extends ResourceTablePage {
	public boolean triggerForTablePageDone;

	public GenericResourceByTypeTablePageBase() {
		super();
	}
	
	protected Class<? extends Resource> typeSelected(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		if(appData.getConfigInfo().context == null) throw new IllegalStateException("Context required for resource type!");
		if(!(appData.getConfigInfo().context instanceof String)) throw new IllegalStateException("Type must be transmitted as String!");
		String param = (String)appData.getConfigInfo().context;
		for(ExtensionResourceTypeDeclaration<?> decl: appManExt.getAllTypeDeclararions()) {
			if(decl.dataType().getName().equals(param)) return decl.dataType();
		}
		return null;
	}
	
	@Override
	protected boolean isInherited() {
		return true;
	}

	@Override
	protected List<Resource> provideResourcesInTable(OgemaHttpRequest req) {
		List<? extends Resource> resultAll = getReqData(req).getSubResources(typeSelected(req), true);
		List<Resource> result = new ArrayList<>();
		for(Resource r: resultAll) {
			result.add(r);
		}
		return result;
	}
	
	@Override //optional
	public String pid() {
		return GenericResourceByTypeTablePageBase.class.getSimpleName();
	}

	@Override
	protected String label(OgemaLocale locale) {
		return "Generic All-Resource Overview Table";
	}

	@Override
	protected String getHeader(OgemaHttpRequest req) {
		return "Resources of type "+typeSelected(req).getSimpleName()+" in "+ResourceUtils.getHumanReadableName(getReqData(req));
	}
	
	@Override
	protected PagePriority getPriority() {
		return PagePriority.HIDDEN;
	}
	
	//@Override
	//protected void addWidgetsAboveTable(Class<? extends Resource> resourceType) {
	//}

	//Can be called to trigger page build if automated trigger is overwritten in finishInit
	public void triggerPageBuild() {
		tablePage.triggerPageBuild();
	}
	
	@Override
	protected void addTopButtonsExceptFirstTwo(OgemaWidget editResource, OgemaWidget allResourceButton2,
			TabButton tabButton) {
		RedirectButton addEntry = new AddEntryButton(page, "addEntry", pid(), ">  +  <", HeatCostBillingInfo.class, exPage, tabButton.control);
		RedirectButton allBuildingsButton = new RedirectButton(page, "allBuildingsButton", "Home", "org_smartrplace_commontypes_BuildingTablePage.html");
		allBuildingsButton.setDefaultOpenInNewTab(false);
		TableOpenButton backButton = new BackButton(page, "back", pid(), exPage, tabButton.control);
		
		StaticTable topTable = new StaticTable(1, 6);
		topTable.setContent(0, 0, editResource).setContent(0, 1, allResourceButton2).
				setContent(0, 2, addEntry).setContent(0, 3, allBuildingsButton).
				setContent(0,  4, backButton).setContent(0, 5, tabButton);
		page.append(topTable);		
	}
}
