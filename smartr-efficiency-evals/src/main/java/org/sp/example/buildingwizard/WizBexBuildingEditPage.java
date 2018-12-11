package org.sp.example.buildingwizard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericTableWidgetProvider;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.basic.evals.BuildingControlQualityFeedback;

public class WizBexBuildingEditPage extends EditPageGeneric<BuildingData> {
	public static final Map<String, String> OVERALLFB_MAP_EN = new LinkedHashMap<>();
	public static final Map<String, String> OVERALLFB_MAP_DE = new LinkedHashMap<>();
	static {
		OVERALLFB_MAP_EN.put("1", "OK, no complaints");
		OVERALLFB_MAP_EN.put("2", "Feels improved compared to earlier situations");
		OVERALLFB_MAP_EN.put("10", "Minor issues (please specify as comment"); 
		OVERALLFB_MAP_EN.put("100", "Immediate support action requested (please speficy as comment)");
		
		OVERALLFB_MAP_DE.put("1", "OK, keine Beschwerden");
		OVERALLFB_MAP_DE.put("2", "Gegenüber vorherigem Zustand verbessert");
		OVERALLFB_MAP_DE.put("10", "Kleine Probleme (bitte im Kommentar erläutern)");
		OVERALLFB_MAP_DE.put("100", "Dringend Abhilfe durch Support benötigt (bitte i.K. erläutern)");

	}
	protected static final List<EditPageGenericTableWidgetProvider<BuildingData>> provList =
			new ArrayList<>();
	public static final String ROOM_NEXT_LABEL_ID = "#roomPageSeries";
	static {
		provList.add(new WizBexWidgetProvider<>());
	}

	public WizBexBuildingEditPage() {
		super(provList);
	}
	
	@Override
	public void setData(BuildingData sr) {
		setLabel(sr.name(), EN, "Buidling Name", DE, "Gebäudename");
		setEditable(sr.name(), false);
		
		setLabel(sr.electricityMeterCountValue(), EN, "Meter count with time of measuerment", DE, "Aktueller Zählerstand");
		SmartEffTimeSeries sub = sr.getSubResource(CapabilityHelper.getSingleResourceName(
				BuildingControlQualityFeedback.class), BuildingControlQualityFeedback.class).overallFeedback();
		setLabel(sub, EN, "Overall system quality feedback", DE, "Rückmeldung Qualität Gesamtsystem");
		setDisplayOptions(sub, EN, OVERALLFB_MAP_EN);
		setDisplayOptions(sub, DE, OVERALLFB_MAP_DE);
		setLabel(sr.buildingUnit(), EN, "Rooms / Sub Units", DE, "Räume / Gebäudeteile");
		setLabel(ROOM_NEXT_LABEL_ID, EN, "Enter room data", DE, "Raumdaten eingeben");
	}

	@Override
	protected Class<BuildingData> primaryEntryTypeClass() {
		return BuildingData.class;
	}
	
	@Override //optional
	public String label(OgemaLocale locale) {
		return "Building Wizard Building Page";
	}
}
