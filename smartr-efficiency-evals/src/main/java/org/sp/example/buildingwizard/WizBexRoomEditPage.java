package org.sp.example.buildingwizard;

import java.util.ArrayList;
import java.util.List;

import org.smartrplace.commontypes.RoomRegistration;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericTableWidgetProvider;
import org.sp.example.smarteff.roomext.RoomLightingRegistration;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.basic.evals.RoomLightingData;

public class WizBexRoomEditPage extends EditPageGeneric<BuildingUnit> {
	protected static final List<EditPageGenericTableWidgetProvider<BuildingUnit>> provList =
			new ArrayList<>();
	public static final String ROOM_NEXT_LABEL_ID = "#roomNext";
	public static final String ROOM_BACK_LABEL_ID = "#roomBack";
	static {
		provList.add(new WizBexWidgetProvider<>());
	}

	public WizBexRoomEditPage() {
		super(provList);
	}
	
	@Override
	public void setData(BuildingUnit sr) {
		setLabel(sr.name(), EN, "Room Name", DE, "Raumname");
		setEditable(sr.name(), false);
		setLabel(sr.manualTemperatureReading(), EN, "Manual temperature reading (°C)");
		setLabel(sr.manualHumidityReading(), EN, "Manual humidity reading (%)");
		setLabel(sr.roomTemperatureQualityRating(), EN, "Room temperature comfort level user feedback",
				DE, "Nutzer-Feedback zur Raumtemperatur");
		setDisplayOptions(sr.roomTemperatureQualityRating(), EN, RoomRegistration.TEMPFB_MAP_EN);
		setDisplayOptions(sr.roomTemperatureQualityRating(), DE, RoomRegistration.TEMPFB_MAP_DE);
		RoomLightingData subS = sr.getSubResource(CapabilityHelper.getSingleResourceName(
				RoomLightingData.class), RoomLightingData.class);
		setLabel(subS.lightingSituation(), EN, "Current lighting/usage situation",
				DE, "Aufnahme der aktuellen Beleuchtungs-/Nutzungssituation");
		setDisplayOptions(subS.lightingSituation(), EN, RoomLightingRegistration.LITSIT_MAP_EN);
		setDisplayOptions(subS.lightingSituation(), DE, RoomLightingRegistration.LITSIT_MAP_DE);
		setLabel(ROOM_NEXT_LABEL_ID, EN, "Next room", DE, "Nächster Raum");
		setLabel(ROOM_BACK_LABEL_ID, EN, "Previous room", DE, "Vorheriger Raum");
	}

	@Override
	protected Class<BuildingUnit> primaryEntryTypeClass() {
		return BuildingUnit.class;
	}
	
	@Override //optional
	public String label(OgemaLocale locale) {
		return "Building Wizard Room Page";
	}
}
