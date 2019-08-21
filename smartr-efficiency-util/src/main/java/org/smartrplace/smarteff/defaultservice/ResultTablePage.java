package org.smartrplace.smarteff.defaultservice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.proposal.ProjectProposal;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.resourcecreate.ProviderPublicDataForCreate.PagePriority;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.EditPageBase;
import org.smartrplace.smarteff.util.NaviPageBase;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.smarteff.util.button.LogicProvTableOpenButton;
import org.smartrplace.smarteff.util.button.ResourceTableOpenButton;
import org.smartrplace.smarteff.util.button.TableOpenButton;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;
import org.smartrplace.util.directresourcegui.ResourceGUITablePage;

import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import extensionmodel.smarteff.api.base.SmartEffGeneralData;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class ResultTablePage extends NaviPageBase<Resource> {
	public static final Map<OgemaLocale, Map<String, String>> STATUS_VALUES = new LinkedHashMap<>();
	public static final Map<String, String> STATUS_VALUES_DE = new LinkedHashMap<>();
	public static final Map<String, String> STATUS_VALUES_EN = new LinkedHashMap<>();
	static {
		STATUS_VALUES_EN.put("0", "not rated");
		STATUS_VALUES_EN.put("1", "Interested");
		STATUS_VALUES_EN.put("2", "Too expensive");
		STATUS_VALUES_EN.put("3", "Price/Value offer not accepted");
		STATUS_VALUES_EN.put("4", "Not feasible");
		STATUS_VALUES_EN.put("5", "Not a real building (test building)");
		STATUS_VALUES_EN.put("6", "Rejected for other reasons");
		STATUS_VALUES_EN.put("7", "Pre-registration for ordering");
		STATUS_VALUES_EN.put("8", "Please send me an offer");
		STATUS_VALUES_EN.put("9", "Ordered");
		/*STATUS_VALUES_EN.put("10", "Order accepted");
		STATUS_VALUES_EN.put("11", "Project started");
		STATUS_VALUES_EN.put("12", "Project finished");
		*/
		STATUS_VALUES_DE.put("0", "nicht bewertet");
		STATUS_VALUES_DE.put("1", "Interessiert");
		STATUS_VALUES_DE.put("2", "Zu teuer");
		STATUS_VALUES_DE.put("3", "Preis/Leistungsverhältnis nicht akzeptiert");
		STATUS_VALUES_DE.put("4", "Nicht umsetzbar");
		STATUS_VALUES_DE.put("5", "Kein reales Gebäude (Eingabe zu Testzwecken)");
		STATUS_VALUES_DE.put("6", "Abgelehnt aus sonstigen Gründen");
		STATUS_VALUES_DE.put("7", "Registrierung für eine Bestellung, wenn möglich");
		STATUS_VALUES_DE.put("8", "Bitte senden Sie mir ein Angebot");
		STATUS_VALUES_DE.put("9", "Bestellt");
		/*STATUS_VALUES_DE.put("10", "Bestellung angenommen");
		STATUS_VALUES_DE.put("11", "Projekt gestartet");
		STATUS_VALUES_DE.put("12", "Projekt beendet");
		*/
		STATUS_VALUES.put(EditPageBase.EN, STATUS_VALUES_EN);
		STATUS_VALUES.put(EditPageBase.DE, STATUS_VALUES_DE);
	}

	public final static Class<ProjectProposal> TYPE_SHOWN = ProjectProposal.class;
	
	protected TablePage tablePage;
	
	public ResultTablePage() {
		super();
	}

	public class TablePage extends ResourceGUITablePage<ProjectProposal> {
		//private final ApplicationManagerMinimal appManMin;
		private ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
		
		public TablePage(ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage, ApplicationManagerMinimal appManMin) {
			super(exPage.getPage(), null, appManMin, ProjectProposal.class, false);
			this.exPage = exPage;
			//this.appManMin = appManMin;
			triggerPageBuild();
		}

		@Override
		public void addWidgets(ProjectProposal object, ResourceGUIHelper<ProjectProposal> vh, String id,
				OgemaHttpRequest req, Row row, ApplicationManager appMan) {
			ExtensionResourceAccessInitData appData = null;
			if(req != null) appData = exPage.getAccessData(req);
			vh.stringLabel("Type", id, object.getResourceType().getSimpleName(), row);
			vh.stringLabel("Name", id, ResourceTablePage.getSimpleName(object), row);
			SPPageUtil.addResEditOpenButton("Open", object, vh, id, row, appData, null, req);
			vh.floatLabel("Total Cost (EUR)", id, object.costOfProject(), row, "%.0f");
			vh.floatLabel("+ Work (h)", id, object.ownHours(), row, "%.1f");
			/*if(req != null) {
			vh.floatLabel("Interest rate", id, CapabilityHelper.getForUser(
					((SmartEffGeneralData)appManExt.globalData()).smartEffPriceData().yearlyInterestRate()
					, appData.userData()), row, "%.2f%%");
			} else vh.registerHeaderEntry("Interest rate");*/
		}

		@Override
		public void addWidgetsAboveTable() {
			StaticTable topTable = new StaticTable(1, 3);
			TableOpenButton resourceButton2 = new ResourceTableOpenButton(page, "resourceButton", pid(), exPage, null);
			/*new TableOpenButton(page, "resourceButton", pid(), "Resources", resourceType, exPage) {
				private static final long serialVersionUID = 1L;
				@Override
				protected NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
						Class<? extends Resource> type, PageType typeRequested, OgemaHttpRequest req) {
					return appData.systemAccessForPageOpening().getPageByProvider(SPPageUtil.getProviderURL(BaseDataService.RESOURCE_NAVI_PROVIDER));
				}
			};*/
			LogicProvTableOpenButton proposalTableOpenButton = new LogicProvTableOpenButton(page, "proposalTableOpenButton", pid(), exPage, null);
			topTable.setContent(0, 0, "--").setContent(0, 1, resourceButton2).setContent(0, 2, proposalTableOpenButton);
			page.append(topTable);
		}
		
		@Override
		public List<ProjectProposal> getResourcesInTable(OgemaHttpRequest req) {
			return getReqData(req).getSubResources(TYPE_SHOWN, true);
		}
	}

	
	@Override
	protected Class<Resource> primaryEntryTypeClass() {
		return Resource.class;
	}
	
	@Override
	protected String label(OgemaLocale locale) {
		return "Results for Resource Recursive";
	}

	@Override
	protected void addWidgets() {
		tablePage = new TablePage(exPage, appManExt);		
	}

	@Override
	protected String getHeader(OgemaHttpRequest req) {
		return "Results for "+ResourceUtils.getHumanReadableName(getReqData(req))+" (+Subtree)";
	}
	
	@Override
	protected List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(primaryEntryTypeClass());
	}

	@Override
	protected PageType getPageType() {
		return PageType.TABLE_PAGE;
	}
	
	@Override
	protected PagePriority getPriorityImpl() {
		return PagePriority.HIDDEN;
	}
}
