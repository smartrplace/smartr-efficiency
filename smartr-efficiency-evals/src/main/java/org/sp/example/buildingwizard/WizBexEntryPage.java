package org.sp.example.buildingwizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.smarteff.util.button.AddEditButton;
import org.smartrplace.smarteff.util.button.BackButton;
import org.smartrplace.smarteff.util.button.TabButton;
import org.smartrplace.smarteff.util.button.TableOpenButton;
import org.smartrplace.smarteff.util.editgeneric.DefaultWidgetProvider;
import org.smartrplace.smarteff.util.editgeneric.EditLineProvider;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;
import org.smartrplace.smarteff.util.wizard.AddEditButtonWizardList;
import org.smartrplace.smarteff.util.editgeneric.DefaultWidgetProvider.SmartEffTimeSeriesWidgetContext;
import org.sp.example.smarteff.eval.capability.SPEvalDataService;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.RedirectButton;
import extensionmodel.smarteff.api.common.BuildingData;

/** This page is no real edit page, but just uses the grid functionality of the edit page. The type of the page is arbitrary*/
public class WizBexEntryPage extends EditPageGeneric<SmartEffResource> {
	public static final Map<OgemaLocale, String> STARTBUTTON_TEXTS = new HashMap<>();
	public static final Map<OgemaLocale, String> HEADER_TEXTS = new HashMap<>();
	public static final List<SmartEffTimeSeriesWidgetContext> counterCts = new ArrayList<>();
	static {
		STARTBUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Start");
		STARTBUTTON_TEXTS.put(OgemaLocale.GERMAN, "Beginnen");
		HEADER_TEXTS.put(OgemaLocale.ENGLISH, "Building Measurement Data Entry Wizard Start Page");
		HEADER_TEXTS.put(OgemaLocale.GERMAN, "Eingabedialog Gebäude-Messdaten");
		counterCts.addAll(WizBexBuildingEditPage.counterCts);
		counterCts.addAll(WizBexRoomEditPage.counterCts);
	}
	

	
	@Override
	public void setData(SmartEffResource srAny) {
		TabButton tabButton = new TabButton(page, "tabButton", pid());
		EditLineProvider tabProv = new EditLineProvider() {
			@Override
			public OgemaWidget valueColumn() {
				return tabButton;
			}
		};
		setLineProvider("#tabControl", tabProv);
		setLabel("#tabControl", EN, "Tab Control");

		RedirectButton startButton = new AddEditButton(page, "openBuildingPage", pid(),
				exPage, tabButton.control) {
			private static final long serialVersionUID = 1L;

			@Override
			protected Map<OgemaLocale, String> getButtonTexts(OgemaHttpRequest req) {
				return STARTBUTTON_TEXTS;
			}
			@Override
			protected String getDestinationURL(ExtensionResourceAccessInitData appData, Resource object,
					OgemaHttpRequest req) {
				return SPPageUtil.getProviderURL(SPEvalDataService.WIZBEX_BUILDING.provider);
			}
			@Override
			public void onGET(OgemaHttpRequest req) {
				super.onGET(req);
				enable(req);
			}
			@Override
			protected Resource getResource(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
				List<BuildingData> all = appData.userData().getSubResources(BuildingData.class, true);
				return all.get(0);
			}
			@Override
			protected Integer getSizeInternal(Resource myResource, ExtensionResourceAccessInitData appData) {
				return AddEditButtonWizardList.getTimeSeriesNumWithoutValueForToday(null, counterCts, appManExt);
			}
		};
		EditLineProvider startProv = new EditLineProvider() {
			@Override
			public OgemaWidget valueColumn() {
				return startButton;
			}
		};
		setLineProvider("#startWiz", startProv);
		setLabel("#startWiz", EN, "Start Wizard", DE, "Eingabe beginnen");		
	}

	@Override
	protected Class<SmartEffResource> primaryEntryTypeClass() {
		return SmartEffResource.class;
	}
	@Override
	protected String pid() {
		return WizBexEntryPage.class.getSimpleName();
	}
	
	@Override //optional
	public String label(OgemaLocale locale) {
		return "Building Wizard Entry Page";
	}
	
	@Override
	protected List<EntryType> getEntryTypes() {
		return null;
	}
	
	@Override
	protected void buildMainTable() {
		BuildMainTableCoreResult tableData = buildMainTableCore(null);
		TableOpenButton tableButton = new BackButton(page, "back", pid(), exPage, null);
		tableData.table.setContent(tableData.c, 1, tableButton);

		page.append(tableData.table);
		//TODO: Not clear if we need this here
		exPage.registerAppTableWidgetsDependentOnInit(tableData.table);		
	}
	
	@Override
	protected String getHeader(OgemaHttpRequest req) {
		return DefaultWidgetProvider.getLocalString(req.getLocale(), HEADER_TEXTS);
	}
}
