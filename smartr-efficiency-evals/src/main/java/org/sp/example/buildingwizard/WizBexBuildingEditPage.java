package org.sp.example.buildingwizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.editgeneric.DefaultWidgetProvider;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericTableWidgetProvider;
import org.smartrplace.smarteff.util.editgeneric.DefaultWidgetProvider.SmartEffTimeSeriesWidgetContext;
import org.smartrplace.smarteff.util.wizard.WizBexWidgetProvider;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.basic.evals.BuildingControlQualityFeedback;

public class WizBexBuildingEditPage extends EditPageGeneric<BuildingData> {
	public static final Map<String, String> OVERALLFB_MAP_EN = new LinkedHashMap<>();
	public static final Map<String, String> OVERALLFB_MAP_DE = new LinkedHashMap<>();
	public static final Map<OgemaLocale, String> HEADER_MAP = new LinkedHashMap<>();
	static {
		OVERALLFB_MAP_EN.put("1", "OK, no complaints");
		OVERALLFB_MAP_EN.put("2", "Feels improved compared to earlier situations");
		OVERALLFB_MAP_EN.put("10", "Minor issues (please specify as comment"); 
		OVERALLFB_MAP_EN.put("100", "Immediate support action requested (please speficy as comment)");
		
		OVERALLFB_MAP_DE.put("1", "OK, keine Beschwerden");
		OVERALLFB_MAP_DE.put("2", "Gegenüber vorherigem Zustand verbessert");
		OVERALLFB_MAP_DE.put("10", "Kleine Probleme (bitte im Kommentar erläutern)");
		OVERALLFB_MAP_DE.put("100", "Dringend Abhilfe durch Support benötigt (bitte i.K. erläutern)");

		HEADER_MAP.put(EN, "Building Wizard Page: ");
		HEADER_MAP.put(EN, "Gesamtgebäude Startseite: ");
	}
	
	public static final SmartEffTimeSeriesWidgetContext elMeterCt = new SmartEffTimeSeriesWidgetContext();
	public static final SmartEffTimeSeriesWidgetContext overallQualityCt = new SmartEffTimeSeriesWidgetContext();
	public static final List<SmartEffTimeSeriesWidgetContext> counterCts = Arrays.asList(new SmartEffTimeSeriesWidgetContext[]{
			elMeterCt, overallQualityCt});
	
	protected static final List<EditPageGenericTableWidgetProvider<BuildingData>> provList =
			new ArrayList<>();
	
	private static ApplicationManagerMinimal appManMinStatic = null;
	static {
		provList.add(new WizBexWidgetProviderSpec<BuildingData>() {

			@Override
			protected ApplicationManagerMinimal appManMin() {
				return appManMinStatic;
			}
			
			@Override
			protected List<SmartEffTimeSeriesWidgetContext> tsCountersImpl() {
				return WizBexRoomEditPage.counterCts;
			}
		});
	}

	public WizBexBuildingEditPage(ApplicationManagerSPExt appManExt) {
		super(provList);
		appManMinStatic = appManExt;
	}
	
	@Override
	public void setData(BuildingData sr) {
		setLabel(sr.name(), EN, "Buidling Name", DE, "Gebäudename");
		setEditable(sr.name(), false);
		setLabel("#location", EN, "Resource Location", DE, "Interner Speicherort");
		
		setLabel(sr.electricityMeterCountValue(), EN, "Meter count with time of measuerment", DE, "Aktueller Zählerstand");
		setWidgetContext(sr.electricityMeterCountValue(), elMeterCt);
		setLabel(WizBexWidgetProvider.ROOM_FIRST_LABEL_ID, EN, "Enter room data", DE, "Raumdaten eingeben");
		
		SmartEffTimeSeries sub = sr.getSubResource(CapabilityHelper.getSingleResourceName(
				BuildingControlQualityFeedback.class), BuildingControlQualityFeedback.class).overallFeedback();
		setLabel(sub, EN, "Overall system quality feedback", DE, "Rückmeldung Qualität Gesamtsystem");
		setDisplayOptions(sub, EN, OVERALLFB_MAP_EN);
		setDisplayOptions(sub, DE, OVERALLFB_MAP_DE);
		setWidgetContext(sub, overallQualityCt);

		setLabel(WizBexWidgetProvider.ROOM_TABLE_LABEL_ID, EN, "Wizard Room Overview", DE, "Wizard-Überblick Räume");
		setLabel(sr.buildingUnit(), EN, "Rooms / Sub Units Administration", DE, "Räume / Gebäudeteile verwalten");
	}

	@Override
	public boolean checkResource(BuildingData res) {
		List<BuildingUnit> missingPosId = new ArrayList<>();
		for(BuildingUnit bu: res.buildingUnit().getAllElements()) {
			if(!bu.getSubResource("wizardPosition", IntegerResource.class).isActive()) {
				missingPosId.add(bu);
			}
		}
		if(!missingPosId.isEmpty()) {
			List<BuildingUnit> defaults = res.buildingUnit().getAllElements();
			for(BuildingUnit bum: missingPosId) {
				IntegerResource subPos = bum.getSubResource("wizardPosition", IntegerResource.class);
				subPos.create();
				subPos.activate(true);
				int idx = defaults.indexOf(bum);
				if(idx < 0) idx = defaults.size();
				subPos.setValue(idx);
			}
		}
		return super.checkResource(res);
	}
	
	@Override
	public Class<BuildingData> primaryEntryTypeClass() {
		return BuildingData.class;
	}
	
	@Override //optional
	public String label(OgemaLocale locale) {
		return "Building Wizard Building Page";
	}
	
	@Override
	protected String getHeader(OgemaHttpRequest req) {
		return DefaultWidgetProvider.getLocalString(req.getLocale(), HEADER_MAP) +
				getReqData(req).name().getValue();
	}
}
