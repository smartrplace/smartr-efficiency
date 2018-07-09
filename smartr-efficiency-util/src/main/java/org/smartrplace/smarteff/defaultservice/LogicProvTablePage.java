package org.smartrplace.smarteff.defaultservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.proposal.ProjectProposal;
import org.smartrplace.extensionservice.proposal.LogicProviderPublicData;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.resourcecreate.ProviderPublicDataForCreate.PagePriority;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.NaviPageBase;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.smarteff.util.button.ResourceOfTypeTableOpenButton;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class LogicProvTablePage extends NaviPageBase<Resource> {
	protected TablePage tablePage;
	
	/** Providers that take a resource type as input that is a direct sub type of the resource
	 * opened and has cardinality single can also be displayed and the respective sub resources
	 * can be created/edited via this page as these can be considered "specific parameters" for
	 * the LogicProvider.
	 */
	protected boolean includeSubProviders = true;
	public static final Map<OgemaLocale, String> RESBUTTON_TEXTS = new HashMap<>();
	static {
		RESBUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Logic Result");
		RESBUTTON_TEXTS.put(OgemaLocale.GERMAN, "Logik-Ergebnis");
	}
	
	public LogicProvTablePage() {
		super();
	}

	public class TablePage extends ObjectGUITablePage<LogicProviderPublicData, Resource> {
		//private final ApplicationManagerMinimal appManMin;
		private ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
		
		public TablePage(ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage, ApplicationManagerMinimal appManMin) {
			super(exPage.getPage(), null, null);
			this.exPage = exPage;
		}

		@Override
		public void addWidgets(LogicProviderPublicData object, ObjectResourceGUIHelper<LogicProviderPublicData, Resource> vh, String id,
				OgemaHttpRequest req, Row row, ApplicationManager appMan) {
			if(req != null) {
				vh.stringLabel("Name", id, object.label(req.getLocale()), row);

				ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
				SPPageUtil.addParameterEditOpenButton("Parameters", object, appManExt.globalData(),
						vh, id, row, appData, null, req);
				
				Resource entryResource = getReqData(req);
				List<Class<? extends Resource>> types = new ArrayList<>();
				for(EntryType et: object.getEntryTypes()) types.add(et.getType().representingResourceType());
				Class<? extends Resource> primarySubType = object.getEntryTypes().get(0).getType().representingResourceType();
				Resource subRes = null;
				if(!types.contains(entryResource.getResourceType())) {
					subRes = CapabilityHelper.getSubResourceOfTypeSingle(entryResource,
							primarySubType);
					SPPageUtil.addResEditOrCreateOpenButton("Input Data", subRes, vh, id, row, appData, null, req,
							exPage);
				} else {
					if(!primarySubType.equals(entryResource.getResourceType())) {
						vh.stringLabel("Input Data", id, "2nd plus", row);						
					} else {
						vh.stringLabel("Input Data", id, "--", row);
					}
				}
				
				boolean offerCalc = true;
				if(subRes != null && (!subRes.isActive())) {
					vh.stringLabel("Calculate", id, "Input Data missing", row);
					offerCalc = false;
				}
				if(offerCalc) {
					Button calculateButton = new Button(vh.getParent(), "calculateButton"+pid()+id, "Calculate", req) {
						private static final long serialVersionUID = 1L;
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
							object.calculate(appData);
						}
					};
					row.addCell("Calculate", calculateButton);					
				}
				for(EntryType o: object.getEntryTypes()) if(o.getType() instanceof GaRoDataTypeI) {
					SPPageUtil.addKPITableOpenButton("KPIs", getReqData(req), vh, id, row, appData, null,
							object, req);
					break;
				}
				//if(object.getEntryTypes() != null && object.getEntryTypes().size() > 1) {
				//}
				if(!ProjectProposal.class.isAssignableFrom(object.resultTypes().get(0).resourceType())) {
					ResourceOfTypeTableOpenButton resultButton = new ResourceOfTypeTableOpenButton(vh.getParent(),
							"resultButton"+id, pid(), exPage, null, req) {
						private static final long serialVersionUID = 1L;

						@Override
						protected Class<? extends Resource> typeToOpen(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
							return object.resultTypes().get(0).resourceType();
						}
						protected Map<OgemaLocale, String> getTextMap(OgemaHttpRequest req) {
							return LogicProvTablePage.RESBUTTON_TEXTS;
						}
					};
					resultButton.openResSub(true);
					row.addCell("Results", resultButton);
					
				} else
					SPPageUtil.addProjectResultTableOpenButton("Results", getReqData(req), vh, id, row, appData, null, req);
				
			} else {
				vh.registerHeaderEntry("Name");
				vh.registerHeaderEntry("Parameters");
				vh.registerHeaderEntry("Input Data");
				vh.registerHeaderEntry("Calculate");
				vh.registerHeaderEntry("Results");
				vh.registerHeaderEntry("KPIs");
			}
		}

		@Override
		public void addWidgetsAboveTable() {
		}
		
		@Override
		public List<LogicProviderPublicData> getObjectsInTable(OgemaHttpRequest req) {
			ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
			Class<? extends Resource> type =  getReqData(req).getResourceType();
			if(!includeSubProviders)
				return appData.systemAccessForPageOpening().getLogicProviders(type);
			List<LogicProviderPublicData> result = appData.systemAccessForPageOpening().getLogicProviders(type);
			for(Class<? extends Resource> subtype: appManExt.getSubTypes(type)) {
				List<LogicProviderPublicData> subres = appData.systemAccessForPageOpening().getLogicProviders(subtype);
				result.addAll(subres);
			}
			return result;
		}

		@Override
		public Resource getResource(LogicProviderPublicData object, OgemaHttpRequest req) {
			throw new IllegalStateException("Resource not provided for proposal table");
		}
	}
	
	@Override
	protected Class<Resource> primaryEntryTypeClass() {
		return Resource.class;
	}
	
	@Override //optional
	public String pid() {
		return LogicProvTablePage.class.getSimpleName();
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
		return "Logic providers for "+ResourceUtils.getHumanReadableName(getReqData(req)); //super.getHeader(req);
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
