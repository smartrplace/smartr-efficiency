package org.smartrplace.smarteff.util.editgeneric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.resourcecreate.ProviderPublicDataForCreate.PagePriority;
import org.smartrplace.smarteff.defaultservice.ResourceTablePage;
import org.smartrplace.smarteff.util.button.AddEditButton;
import org.smartrplace.smarteff.util.button.AddEntryButton;
import org.smartrplace.smarteff.util.button.BackButton;
import org.smartrplace.smarteff.util.button.TabButton;
import org.smartrplace.smarteff.util.button.TableOpenButton;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.RedirectButton;

public class GenericResourceByTypeTablePageBase extends ResourceTablePage {
	public static final Map<OgemaLocale, String> SUPEREDITBUTTON_TEXTS = new HashMap<>();
	static {
		SUPEREDITBUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Step Up");
		SUPEREDITBUTTON_TEXTS.put(OgemaLocale.GERMAN, "Ebene hoch");
	}
	
	public boolean triggerForTablePageDone;

	public GenericResourceByTypeTablePageBase() {
		super(false);
	}
	
	public static class ResourceOfTypeContext {
		public String dataTypeName;
		
		/** If present this URL shall be used for edit page*/
		public String editPageURL = null;
	}
	
	protected Class<? extends Resource> typeSelected(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		if(appData.getConfigInfo().context == null) throw new IllegalStateException("Context required for resource type!");
		if(!(appData.getConfigInfo().context instanceof ResourceOfTypeContext)) throw new IllegalStateException("Type must be transmitted as ResourceOfTypeContext!");
		ResourceOfTypeContext param = (ResourceOfTypeContext)appData.getConfigInfo().context;
		for(ExtensionResourceTypeDeclaration<?> decl: appManExt.getAllTypeDeclarations()) {
			if(decl.dataType().getName().equals(param.dataTypeName)) return decl.dataType();
		}
		return null;
	}
	/*protected Class<? extends Resource> typeSelected(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		if(appData.getConfigInfo().context == null) throw new IllegalStateException("Context required for resource type!");
		if(!(appData.getConfigInfo().context instanceof String)) throw new IllegalStateException("Type must be transmitted as String!");
		String param = (String)appData.getConfigInfo().context;
		for(ExtensionResourceTypeDeclaration<?> decl: appManExt.getAllTypeDeclarations()) {
			if(decl.dataType().getName().equals(param)) return decl.dataType();
		}
		return null;
	}*/
	
	protected Map<OgemaLocale, String> getSuperEditButtonTexts() {
		return SUPEREDITBUTTON_TEXTS;
	}
	
	public List<GenericDataTypeDeclaration> getElementTypes() {
		return null;
	}
	
	@Override
	protected boolean isInherited() {
		return true;
	}

	@Override
	protected List<Resource> provideResourcesInTable(OgemaHttpRequest req) {
		List<? extends Resource> resultAll = getReqData(req).getSubResources(typeSelected(req), true);
		List<Resource> toRemove = new ArrayList<>();
		for(Resource o: resultAll) {
			if(o.isReference(false)) toRemove.add(o);
		}
		resultAll.removeAll(toRemove);
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
		Class<? extends Resource> type = typeSelected(req);
		if(type == null) {
			ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
			ResourceOfTypeContext param = (ResourceOfTypeContext)appData.getConfigInfo().context;
			return "Resources of type "+param+" in "+ResourceUtils.getHumanReadableName(getReqData(req));
		}
		return "Resources of type "+typeSelected(req).getSimpleName()+" in "+ResourceUtils.getHumanReadableName(getReqData(req));
	}
	
	@Override
	protected PagePriority getPriorityImpl() {
		return PagePriority.SECONDARY;
	}
	
	//@Override
	//protected void addWidgetsAboveTable(Class<? extends Resource> resourceType) {
	//}

	//Can be called to trigger page build if automated trigger is overwritten in finishInit
	public void triggerPageBuild() {
		tablePage.triggerPageBuild();
	}
	
	@Override
	protected void addTopButtonsExceptFirstTwo(AddEditButton editResource, OgemaWidget allResourceButton2,
			TabButton tabButton) {
		if(getSuperEditButtonTexts() != null) editResource.setButtonTexts(getSuperEditButtonTexts());
		RedirectButton addEntry = new AddEntryButton(page, "addEntry", pid(), ">  +  <", null, exPage, tabButton.control) {
			private static final long serialVersionUID = 1L;
			@Override
			public Class<? extends Resource> typeToCreate(ExtensionResourceAccessInitData appData,
					OgemaHttpRequest req) {
				return typeSelected(req);
			}
		};
		RedirectButton allBuildingsButton = new RedirectButton(page, "allBuildingsButton", "Home", "org_smartrplace_commontypes_BuildingTablePage.html");
		allBuildingsButton.setDefaultOpenInNewTab(false);
		TableOpenButton backButton = new BackButton(page, "back", pid(), exPage, tabButton.control);
		
		StaticTable topTable = new StaticTable(1, 6);
		topTable.setContent(0, 0, addEntry).setContent(0, 1, editResource).
				setContent(0, 2, "").setContent(0, 3, allBuildingsButton).
				setContent(0,  4, backButton).setContent(0, 5, tabButton);
		//In standard variant we do not provide link to Data Explorer here (allResourceButton2)
		//topTable.setContent(0, 0, editResource).setContent(0, 1, allResourceButton2).
		//	setContent(0, 2, addEntry).setContent(0, 3, allBuildingsButton).
		//	setContent(0,  4, backButton).setContent(0, 5, tabButton);
		page.append(topTable);		
	}
}
