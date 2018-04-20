package org.smartrplace.smarteff.defaultservice;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extenservice.proposal.ProjectProposal;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extenservice.resourcecreate.ProviderPublicDataForCreate.PagePriority;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.NaviPageBase;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.smarteff.util.button.ProposalProvTableOpenButton;
import org.smartrplace.smarteff.util.button.ResourceTableOpenButton;
import org.smartrplace.smarteff.util.button.TableOpenButton;
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
			vh.floatLabel("Savings/a (EUR)", id, object.yearlySavings(), row, "%.2f");
			vh.floatLabel("CO2-Saved/a (kg)", id, object.yearlyCO2savings(), row, "%.2f");
			if(req != null) {
			vh.floatLabel("Interest rate", id, CapabilityHelper.getForUser(
					((SmartEffGeneralData)appManExt.globalData()).smartEffPriceData().yearlyInterestRate()
					, appData.userData()), row, "%.2f%%");
			} else vh.registerHeaderEntry("Interest rate");
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
			ProposalProvTableOpenButton proposalTableOpenButton = new ProposalProvTableOpenButton(page, "proposalTableOpenButton", pid(), exPage, null);
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
	protected PagePriority getPriority() {
		return PagePriority.HIDDEN;
	}
}
