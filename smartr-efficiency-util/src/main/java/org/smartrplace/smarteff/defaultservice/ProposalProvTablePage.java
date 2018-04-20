package org.smartrplace.smarteff.defaultservice;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extenservice.proposal.ProposalPublicData;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extenservice.resourcecreate.ProviderPublicDataForCreate.PagePriority;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.NaviPageBase;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class ProposalProvTablePage extends NaviPageBase<Resource> {
	protected TablePage tablePage;
	
	public ProposalProvTablePage() {
		super();
	}

	public class TablePage extends ObjectGUITablePage<ProposalPublicData, Resource> {
		//private final ApplicationManagerMinimal appManMin;
		private ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
		
		public TablePage(ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage, ApplicationManagerMinimal appManMin) {
			super(exPage.getPage(), null, null);
			this.exPage = exPage;
		}

		@Override
		public void addWidgets(ProposalPublicData object, ObjectResourceGUIHelper<ProposalPublicData, Resource> vh, String id,
				OgemaHttpRequest req, Row row, ApplicationManager appMan) {
			if(req != null) {
				vh.stringLabel("Name", id, object.label(req.getLocale()), row);
				Button calculateButton = new Button(vh.getParent(), "calculateButton"+pid(), "Calculate", req) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
						object.calculate(appData);
					}
				};
				row.addCell("Calculate", calculateButton);
				ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
				SPPageUtil.addResultTableOpenButton("Results", getReqData(req), vh, id, row, appData, null, req);
			} else {
				vh.registerHeaderEntry("Name");
				vh.registerHeaderEntry("Calculate");
				vh.registerHeaderEntry("Results");
			}
		}

		@Override
		public void addWidgetsAboveTable() {
		}
		
		@Override
		public List<ProposalPublicData> getObjectsInTable(OgemaHttpRequest req) {
			ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
			Class<? extends Resource> type =  getReqData(req).getResourceType();
			return appData.systemAccessForPageOpening().getProposalProviders(type);
		}

		@Override
		public Resource getResource(ProposalPublicData object, OgemaHttpRequest req) {
			throw new IllegalStateException("Resource not provided for proposal table");
		}
	}
	
	@Override
	protected Class<Resource> primaryEntryTypeClass() {
		return Resource.class;
	}
	
	@Override //optional
	public String pid() {
		return ProposalProvTablePage.class.getSimpleName();
	}

	@Override
	protected String label(OgemaLocale locale) {
		return "Proposal providers for Resource";
	}

	@Override
	protected void addWidgets() {
		tablePage = new TablePage(exPage, appManExt);		
	}

	@Override
	protected String getHeader(OgemaHttpRequest req) {
		return "Proposal providers for "+ResourceUtils.getHumanReadableName(getReqData(req)); //super.getHeader(req);
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
