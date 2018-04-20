package org.smartrplace.commontypes;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.smarteff.util.NaviPageBase;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.smarteff.util.button.AddEntryButton;
import org.smartrplace.smarteff.util.button.TabButton;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;
import org.smartrplace.util.directresourcegui.GUIHelperExtension;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;
import org.smartrplace.util.directresourcegui.ResourceGUITablePage;

import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.RedirectButton;
import extensionmodel.smarteff.api.base.SmartEffUserData;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;
import extensionmodel.smarteff.api.common.BuildingData;

public class BuildingTablePage extends NaviPageBase<BuildingData> {
	protected TablePage tablePage;
	
	public BuildingTablePage() {
		super();
	}

	public class TablePage extends ResourceGUITablePage<BuildingData> {
		//private final ApplicationManagerMinimal appManMin;
		private ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
		protected TabButton tabButton;
		
		public TablePage(ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage, ApplicationManagerMinimal appManMin) {
			super(exPage.getPage(), null, appManMin, BuildingData.class, false);
			this.exPage = exPage;
			//this.appManMin = appManMin;
			triggerPageBuild();
		}

		@Override
		public void addWidgets(BuildingData object, ResourceGUIHelper<BuildingData> vh, String id,
				OgemaHttpRequest req, Row row, ApplicationManager appMan) {
			ExtensionResourceAccessInitData appData = null;
			if(req != null) appData = exPage.getAccessData(req);

			vh.stringLabel("Name", id, ResourceUtils.getHumanReadableName(object), row);
			vh.floatLabel("Heated Area", id, object.heatedLivingSpace(), row, "%.0f m2");
			SPPageUtil.addResEditOpenButton("Edit", object, vh, id, row, appData, (tabButton!=null)?tabButton.control:null, req);
			SPPageUtil.addResTableOpenButton("Open", object, vh, id, row, appData, (tabButton!=null)?tabButton.control:null, req);
			if(object.isActive()) {
				SPPageUtil.addProviderTableOpenButton("Evaluations", object, vh, id, row, appData, (tabButton!=null)?tabButton.control:null, req);
				//TableOpenButton proposalTableOpenButton = new StandardProposalTableOpenButton(vh.getParent(), "proposalTableOpenButton", pid(), "Proposal providers", object.getResourceType(), exPage, req);
				//row.addCell("Evaluations", proposalTableOpenButton);
			} else
				vh.stringLabel("Evaluations", id, "Inactive", row);
			GUIHelperExtension.addDeleteButton(null, object, mainTable, id, alert, row, vh, req);
			//vh.linkingButton("Delete", id, object, row, "Delete", "delete.html");
		}

		@Override
		public void addWidgetsAboveTable() {
			tabButton = new TabButton(page, "tabButton", pid());
			RedirectButton addEntry = new AddEntryButton(page, "addEntry", pid(), "Add Building", BuildingData.class, exPage, tabButton.control) {
				private static final long serialVersionUID = 1L;
				@Override
				protected Resource getResource(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
					return appData.userData();
				}
			};
			StaticTable topTable = new StaticTable(1, 2, new int[]{10,2});
			topTable.setContent(0, 0, addEntry).setContent(0, 1, tabButton);
			page.append(topTable);
			exPage.registerDependentWidgetOnInit(mainTable);
		}
		
		@Override
		public List<BuildingData> getResourcesInTable(OgemaHttpRequest req) {
			ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
			return ((SmartEffUserData)appData.userData()).buildingData().getAllElements();
		}
	}

	@Override
	protected Class<BuildingData> primaryEntryTypeClass() {
		return BuildingData.class;
	}
	
	@Override //optional
	public String pid() {
		return BuildingTablePage.class.getSimpleName();
	}

	@Override
	protected String label(OgemaLocale locale) {
		return "Standard Building Overview Table";
	}

	@Override
	protected void addWidgets() {
		tablePage = new TablePage(exPage, appManExt);		
	}

	@Override
	protected List<EntryType> getEntryTypes() {
		return null;
	}
	
	@Override
	protected String getHeader(OgemaHttpRequest req) {
		return provider.label(null);
	}

	@Override
	protected PageType getPageType() {
		return PageType.TABLE_PAGE;
	}
}
