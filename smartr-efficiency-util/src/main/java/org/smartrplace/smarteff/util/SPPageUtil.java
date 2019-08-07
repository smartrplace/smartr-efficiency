package org.smartrplace.smarteff.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionGeneralData;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration.Cardinality;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.extensionservice.proposal.LogicProviderPublicData;
import org.smartrplace.extensionservice.proposal.ProjectProposalEfficiency;
import org.smartrplace.extensionservice.proposal.ProjectProposal100EE;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForCreate;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForPageOpening;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.defaultservice.BaseDataService;
import org.smartrplace.smarteff.defaultservice.ResultTablePageEff;
import org.smartrplace.smarteff.defaultservice.ResultTablePage100EE;
import org.smartrplace.smarteff.util.button.AddEditButton;
import org.smartrplace.smarteff.util.button.AddEntryButton;
import org.smartrplace.smarteff.util.button.ButtonControlProvider;
import org.smartrplace.smarteff.util.button.LogicProvTableOpenButton;
import org.smartrplace.smarteff.util.button.ProposalResTableOpenButton;
import org.smartrplace.smarteff.util.button.ResourceTableOpenButton;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.ValueFormat;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.util.format.WidgetPageFormatter;

import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.TemplateRedirectButton;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class SPPageUtil {
	public static final String OPEN_NEW_TAB_STRING = "New Tab";
	public static final String OPEN_SAME_TAB_STRING = "Same Tab";

	public static WidgetPageFormatter STANDARD_PAGE_FORMATTER = new WidgetPageFormatter();
	
	public static final Map<OgemaLocale, String> BUTTON_PARAMS_TEXTS = new HashMap<>();
	static {
		BUTTON_PARAMS_TEXTS.put(OgemaLocale.ENGLISH, "Parameters");
		BUTTON_PARAMS_TEXTS.put(OgemaLocale.GERMAN, "Parameter");
	}

	public static OgemaWidget addOpenButton(String columnName, Resource object,
			ObjectResourceGUIHelper<?,?> vh, String id, Row row,
			NavigationPublicPageData pageData,
			ExtensionPageSystemAccessForPageOpening systemAccess,
			String text, String alternativeText, boolean openWhenLocked,
			PageType pageType,
			ButtonControlProvider controlProvider, Object context, OgemaHttpRequest req) {
		if(pageData != null) {
			if(openWhenLocked ||(!((ExtensionPageSystemAccessForCreate)systemAccess).isLocked(object))) {
				//String configId = NaviOpenButton.getConfigId(pageType, object, type, systemAccess, pageData);
				//Here we never create a new resource
				final String configId;
				if(object == null) {
					configId = systemAccess.accessPage(pageData, -1, null, context);					
				} else {
					Class<? extends Resource> type = object.getResourceType();
					configId = systemAccess.accessPage(pageData, getEntryIdx(pageData, type),
							Arrays.asList(new Resource[]{object}), context);
				}/*String columnId = ResourceUtils.getValidResourceName(columnName);
				ResourceRedirectButton<Resource> button = new ResourceRedirectButton<Resource>(vh.getParent(), columnId+id, text, pageData.getUrl(), req) { //pageData.getUrl()+"?configId="+configId
					private static final long serialVersionUID = 1L;
					@Override
					protected String getConfigId(Resource object) {
						return configId;
					}
					@Override
					public void onGET(OgemaHttpRequest req) {
						super.onGET(req);
						ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
						List<CalculatedData> data = object.getSubResources(CalculatedData.class, true);
						String text = BUTTON_TEXTS.get(req.getLocale());
						if(text == null) text = BUTTON_TEXTS.get(OgemaLocale.ENGLISH);
						setText(text+"("+data.size()+")", req);
					}
					
				};
				row.addCell(columnId, button);*/
				TemplateRedirectButton<?> button = vh.linkingButton(columnName, id, null, row, text, pageData.getUrl()+"?configId="+configId);
				if(controlProvider != null) {
					//This does not have much effect as the tabButton is used when the page is opened
					button.setOpenInNewTab(controlProvider.openInNewTab(req), req);
					//This should work
					controlProvider.registerRedirectButtonForStateSetting(button);
				} else {
					button.setDefaultOpenInNewTab(false);					
				}
				return button;
			} else {
				return vh.stringLabel(columnName, id, alternativeText, row);						
			}
		} else {
			return vh.stringLabel(columnName, id, alternativeText+" (No Data)", row);
		}
	}
	public static OgemaWidget addResEditOpenButton(String columnName, Resource object,
			ObjectResourceGUIHelper<?,?> vh, String id, Row row,
			ExtensionResourceAccessInitData appData,
			ButtonControlProvider controlProvider, OgemaHttpRequest req) {
		return addResEditOpenButton(columnName, object, vh, id, row, appData, controlProvider, req, null);
	}
	public static OgemaWidget addResEditOpenButton(String columnName, Resource object,
			ObjectResourceGUIHelper<?,?> vh, String id, Row row,
			ExtensionResourceAccessInitData appData,
			ButtonControlProvider controlProvider, OgemaHttpRequest req,
			String editPageURL) {
		if(appData != null) {
			final NavigationPublicPageData pageData;
			if(editPageURL == null)
				pageData = getPageData(appData, object.getResourceType(), PageType.EDIT_PAGE);
			else
				pageData = appData.systemAccessForPageOpening().getPageByProvider(editPageURL);
			int size = AddEditButton.getSize(object, appData);
			String text = ValueFormat.getLocaleString(req, AddEditButton.BUTTON_TEXTS);
			return addOpenButton(columnName, object, vh, id, row, pageData, appData.systemAccess(),
					text+"("+size+")", "Locked", false, PageType.EDIT_PAGE, controlProvider, null, req);
		} else {
			vh.registerHeaderEntry(columnName);
			return null;
		}
	}
	public static OgemaWidget addResEditOrCreateOpenButton(String columnName, Resource object,
			ObjectResourceGUIHelper<?,?> vh, String id, Row row,
			ExtensionResourceAccessInitData appData,
			ButtonControlProvider controlProvider, OgemaHttpRequest req,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage) {
		if(appData != null) {
			if(object.exists()) {
				NavigationPublicPageData pageData = getPageData(appData, object.getResourceType(), PageType.EDIT_PAGE);
				int size = AddEditButton.getSize(object, appData);
				String text = ValueFormat.getLocaleString(req, AddEditButton.BUTTON_TEXTS);
				return addOpenButton(columnName, object, vh, id, row, pageData, appData.systemAccess(),
						text+"("+size+")", "Locked", false, PageType.EDIT_PAGE, controlProvider, null, req);
			}
			String widgetId = WidgetHelper.getValidWidgetId(columnName);
			AddEntryButton addButton = new AddEntryButton(vh.getParent(), widgetId, id, "Add Sub Resource", object.getResourceType(), exPage, controlProvider, req);
			row.addCell(widgetId, addButton);
			return addButton;
		} else {
			vh.registerHeaderEntry(columnName);
			return null;
		}
	}
	public static OgemaWidget addResTableOpenButton(String columnName, Resource object,
			ObjectResourceGUIHelper<?,?> vh, String id, Row row,
			ExtensionResourceAccessInitData appData, ButtonControlProvider controlProvider, OgemaHttpRequest req) {
		if(appData != null) {
			NavigationPublicPageData pageData = getPageData(appData, object.getResourceType(), PageType.TABLE_PAGE);
			String text = ValueFormat.getLocaleString(req, ResourceTableOpenButton.BUTTON_TEXTS);
			int size = ResourceTableOpenButton.getSize(object, appData);
			return addOpenButton(columnName, object, vh, id, row, pageData, appData.systemAccess(),
					text+"("+size+")", "No Page", true, PageType.TABLE_PAGE, controlProvider, null, req);
		} else {
			vh.registerHeaderEntry(columnName);
			return null;
		}
	}
	public static OgemaWidget addProviderTableOpenButton(String columnName, Resource object,
			ObjectResourceGUIHelper<?,?> vh, String id, Row row,
			ExtensionResourceAccessInitData appData, ButtonControlProvider controlProvider, OgemaHttpRequest req) {
		if(appData != null) {
			List<LogicProviderPublicData> provs = appData.systemAccessForPageOpening().getLogicProviders(object.getResourceType());
			if(provs.isEmpty()) {
				return vh.stringLabel(columnName, id, "No Providers", row);
			} else {
				NavigationPublicPageData pageData = appData.systemAccessForPageOpening().getPageByProvider(SPPageUtil.getProviderURL(BaseDataService.PROPOSALTABLE_PROVIDER));
				String text = ValueFormat.getLocaleString(req, LogicProvTableOpenButton.BUTTON_TEXTS);
				int size = LogicProvTableOpenButton.getSize(object, appData);
				return addOpenButton(columnName, object, vh, id, row, pageData, appData.systemAccess(),
						text+"("+size+")", "No BaseEval", true, PageType.TABLE_PAGE, controlProvider, null, req);
			}
		} else {
			vh.registerHeaderEntry(columnName);
			return null;
		}
	}
	public static OgemaWidget addProjectResultTableOpenButton(String columnName, Resource object,
			ObjectResourceGUIHelper<?,?> vh, String id, Row row,
			ExtensionResourceAccessInitData appData, ButtonControlProvider controlProvider, OgemaHttpRequest req) {

		if(appData == null) {
			vh.registerHeaderEntry(columnName);
			return null;
		}

		List<ProjectProposalEfficiency> resultsAvail = object.getSubResources(ResultTablePageEff.TYPE_SHOWN, true);
		if(resultsAvail.isEmpty()) {
			return vh.stringLabel(columnName, id, "No Results", row);
		} else {
			NavigationPublicPageData pageData = appData.systemAccessForPageOpening()
					.getPageByProvider(SPPageUtil.getProviderURL(BaseDataService.RESULTTABLE_PROVIDER));
			String text = ValueFormat.getLocaleString(req, ProposalResTableOpenButton.BUTTON_TEXTS);
			int size = ProposalResTableOpenButton.getSize(object, appData, ProjectProposalEfficiency.class);
			return addOpenButton(columnName, object, vh, id, row, pageData, appData.systemAccess(),
					text+"("+size+")", "No BaseResult", true, PageType.TABLE_PAGE, controlProvider, null, req);
		}

	}


	public static OgemaWidget addProjectResultTableOpenButton100EE(String columnName, Resource object,
			ObjectResourceGUIHelper<?,?> vh, String id, Row row,
			ExtensionResourceAccessInitData appData, ButtonControlProvider controlProvider, OgemaHttpRequest req) {

		if(appData == null) {
			vh.registerHeaderEntry(columnName);
			return null;
		}

		List<ProjectProposal100EE> resultsAvail = object.getSubResources(ResultTablePage100EE.TYPE_SHOWN, true);
		if(resultsAvail.isEmpty()) {
			return vh.stringLabel(columnName, id, "No Results", row);
		} else {
			NavigationPublicPageData pageData = appData.systemAccessForPageOpening()
					.getPageByProvider(SPPageUtil.getProviderURL(BaseDataService.RESULTTABLE100EE_PROVIDER));
			String text = ValueFormat.getLocaleString(req, ProposalResTableOpenButton.BUTTON_TEXTS_100EE);
			int size = ProposalResTableOpenButton.getSize(object, appData, ProjectProposal100EE.class);
			return addOpenButton(columnName, object, vh, id, row, pageData, appData.systemAccess(),
					text+"("+size+")", "No BaseResult", true, PageType.TABLE_PAGE, controlProvider, null, req);
		}

	}

	public static OgemaWidget addParameterEditOpenButton(String columnName,
			LogicProviderPublicData object, ExtensionGeneralData globalData,
			ObjectResourceGUIHelper<?,?> vh, String id, Row row,
			ExtensionResourceAccessInitData appData, ButtonControlProvider controlProvider, OgemaHttpRequest req) {
		if(appData != null) {
			Resource param = null;
			Class<? extends Resource> paramType = null;
			paramType = object.getParamType();
			List<? extends Resource> all = appData.userData().getSubResources(paramType, true);
			if(all.isEmpty()) {
				List<? extends Resource> all2 = globalData.getSubResources(paramType, true);
				if(!all2.isEmpty()) param = all2.get(0);
			}
			else param = all.get(0);
			if(param == null) {
				return vh.stringLabel(columnName, id, "No Parameter", row);
			} else {
				NavigationPublicPageData pageData = appData.systemAccessForPageOpening().
						getMaximumPriorityPage(paramType, PageType.EDIT_PAGE); //.getPageByProvider(SPPageUtil.getProviderURL(BaseDataService.RESULTTABLE_PROVIDER));
				String text = ValueFormat.getLocaleString(req, BUTTON_PARAMS_TEXTS);
				int size = AddEditButton.getSize(param, appData);
				return addOpenButton(columnName, param, vh, id, row, pageData, appData.systemAccess(),
						text+"("+size+")", "No ParamPage", true, PageType.EDIT_PAGE, controlProvider, null, req);
			}
		} else {
			vh.registerHeaderEntry(columnName);
			return null;
		}
	}

	/** For a timeseries based evaluation a KPI result as well as a normal resource result
	 * is generated
	 */
	public static OgemaWidget addKPITableOpenButton(String columnName, Resource object,
			ObjectResourceGUIHelper<?,?> vh, String id, Row row,
			ExtensionResourceAccessInitData appData, ButtonControlProvider controlProvider,
			LogicProviderPublicData proposalPublic, OgemaHttpRequest req) {
		if(appData != null) {
			List<KPIStatisticsManagementI> kpis = appData.getEvaluationManagement().getKPIManagement(
					object, proposalPublic.getProviderId());
			//List<MultiEvalStartConfiguration> resultsAvail = object.getSubResources(MultiEvalStartConfiguration.class, true);
			if(kpis == null || kpis.isEmpty()) {
				return vh.stringLabel(columnName, id, "No Results", row);
			} else {
				NavigationPublicPageData pageData = appData.systemAccessForPageOpening().getPageByProvider("org_sp_eval_kpi_basic_gui_KPITablePageSP.html");
				String text = ValueFormat.getLocaleString(req, ProposalResTableOpenButton.BUTTON_TEXTS)+"(KPI)";
				int size = kpis.size(); //ProposalResTableOpenButton.getSize(object, appData, CalculatedData.class);
				return addOpenButton(columnName, object, vh, id, row, pageData, appData.systemAccess(),
						text+"("+size+")", "No BaseResult", true, PageType.TABLE_PAGE, controlProvider, null, req);
			}
		} else {
			vh.registerHeaderEntry(columnName);
			return null;
		}
	}
	
	public static NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
			Class<? extends Resource> type, PageType typeRequested) {
		return appData.systemAccessForPageOpening().getMaximumPriorityPage(type, typeRequested);
	}
	
	public static int getEntryIdx(NavigationPublicPageData navi, Class<? extends Resource> type) {
		int idx = 0;
		for(EntryType et: navi.getEntryTypes()) {
			if(et.getType().representingResourceType().isAssignableFrom(type)) {
				return idx;
			}
			idx++;
		}
		throw new IllegalStateException(type.getSimpleName()+" not found in "+navi.getUrl());
	}
	
	public static String buildId(SmartEffExtensionService service) {
		String id = service.id();
		if(id == null) return service.getClass().getName();
		else return id;
	}
	public static String buildId(ExtensionCapability service) {
		String id = service.id();
		if(id == null) return service.getClass().getName();
		else return id;
	}
	public static String buildValidWidgetId(ExtensionCapability service) {
		String id = WidgetHelper.getValidWidgetId(buildId(service));
		return id;
	}

	public static String getProviderURL(ExtensionCapability navi) {
		return WidgetHelper.getValidWidgetId(buildId(navi))+".html";	
	}

	public static boolean isMulti(Cardinality card) {
		if(card == Cardinality.MULTIPLE_OPTIONAL || card == Cardinality.MULTIPLE_REQUIRED) return true;
		return false;
	}
}
