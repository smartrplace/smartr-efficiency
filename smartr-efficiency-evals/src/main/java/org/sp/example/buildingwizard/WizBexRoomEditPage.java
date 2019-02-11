package org.sp.example.buildingwizard;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.commontypes.RoomRegistration;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericTableWidgetProvider;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericWithTable;
import org.smartrplace.smarteff.util.wizard.WizBexWidgetProvider;
import org.sp.example.smarteff.roomext.RoomLightingRegistration;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.basic.evals.RoomLightingData;

public class WizBexRoomEditPage extends EditPageGenericWithTable<BuildingUnit> {
	protected static final List<EditPageGenericTableWidgetProvider<BuildingUnit>> provList =
			new ArrayList<>();
	static {
		provList.add(new WizBexWidgetProviderSpec());
	}

	public WizBexRoomEditPage() {
		super(provList, true);
	}
	
	@Override
	public void setData(BuildingUnit sr) {
		setLabel(sr.name(), EN, "Room Name", DE, "Raumname");
		setEditable(sr.name(), false);
		setLabel("#location", EN, "Resource Location", DE, "Interner Speicherort");
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
		
		setLabel(getSubPath(sr.heatRadiator())+"#$0/heatCostAllocatorReadings", EN, "Read Heat cost allocator 1", DE, "Heizkostenverteiler ablesen 1 (aktueller Wert)");
		setLabel(getSubPath(sr.heatRadiator())+"#$1/heatCostAllocatorReadings", EN, "Read Heat cost allocator 2", DE, "Heizkostenverteiler ablesen 2 (aktueller Wert)");
		setLabel(getSubPath(sr.heatRadiator())+"#$2/heatCostAllocatorReadings", EN, "Read Heat cost allocator 3", DE, "Heizkostenverteiler ablesen 3 (aktueller Wert)");
		
		setLabel(WizBexWidgetProvider.ROOM_NEXT_LABEL_ID, EN, "Next room", DE, "Nächster Raum");
		setLabel(WizBexWidgetProvider.ROOM_BACK_LABEL_ID, EN, "Previous room", DE, "Vorheriger Raum");

		RoomLightingData sub = sr.getSubResource(CapabilityHelper.getSingleResourceName(
				RoomLightingData.class), RoomLightingData.class);
		setLabel(sub, EN, "Lighting Data", DE, "Beleuchtung");
		
		setTableHeader(sr.name(), EN, "Name");
		setTableHeader(sr.groundArea(), EN, "Ground Area (m2)", DE, "Nutzfläche (m2)");
		IntegerResource subPos = sr.getSubResource("wizardPosition", IntegerResource.class);
		setTableHeader(subPos, EN, "Wizard Position", DE, "Position in Wizard-Abfolge");
		setTableHeader(subPos, CN, "edit");
		
		setTableHeader(WizBexWidgetProvider.ROOM_ENTRY_LABEL_ID, EN, "Enter Wizard", DE, "Wizard hier starten");
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
