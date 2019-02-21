package org.sp.example.buildingwizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.commontypes.RoomRegistration;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.editgeneric.DefaultWidgetProvider.SmartEffTimeSeriesWidgetContext;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericTableWidgetProvider;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericWithTable;
import org.smartrplace.smarteff.util.editgeneric.GenericResourceByTypeTablePageBase;
import org.smartrplace.smarteff.util.wizard.WizBexWidgetProvider;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;
import org.sp.example.smarteff.roomext.RoomLightingRegistration;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.basic.evals.RoomLightingData;

public class WizBexRoomEditPage extends EditPageGenericWithTable<BuildingUnit> {
	protected static final List<EditPageGenericTableWidgetProvider<BuildingUnit>> provList =
			new ArrayList<>();
	private static ApplicationManagerMinimal appManMinStatic = null;
	static {
		provList.add(new WizBexWidgetProviderSpec<BuildingUnit>() {
			/*@Override
			protected void checkResource(BuildingUnit res) {
				IntegerResource subPos = res.getSubResource("wizardPosition", IntegerResource.class);
				if(!subPos.isActive()) {
					subPos.create();
					subPos.activate(true);
				}
			}*/

			@Override
			protected ApplicationManagerMinimal appManMin() {
				return appManMinStatic;
			}

			@Override
			protected List<SmartEffTimeSeriesWidgetContext> tsCountersImpl() {
				return counterCts;
			}
			
			@Override
			protected Resource getTableResource(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
				Resource room = super.getTableResource(appData, req);
				return room.getParent().getParent();
			}
		});
	}
	public static final SmartEffTimeSeriesWidgetContext tempQualityCt = new SmartEffTimeSeriesWidgetContext();
	public static final SmartEffTimeSeriesWidgetContext lightingCt = new SmartEffTimeSeriesWidgetContext();
	public static final SmartEffTimeSeriesWidgetContext radiatprCt = new SmartEffTimeSeriesWidgetContext();
	public static final List<SmartEffTimeSeriesWidgetContext> counterCts = Arrays.asList(new SmartEffTimeSeriesWidgetContext[]{
			tempQualityCt, lightingCt, radiatprCt});
	
	public WizBexRoomEditPage(ApplicationManagerSPExt appManExt) {
		super(provList, true);
		appManMinStatic = appManExt;
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
		setWidgetContext(sr.roomTemperatureQualityRating(), tempQualityCt);

		RoomLightingData subS = sr.getSubResource(CapabilityHelper.getSingleResourceName(
				RoomLightingData.class), RoomLightingData.class);
		setLabel(subS.lightingSituation(), EN, "Current lighting/usage situation",
				DE, "Aufnahme der aktuellen Beleuchtungs-/Nutzungssituation");
		setDisplayOptions(subS.lightingSituation(), EN, RoomLightingRegistration.LITSIT_MAP_EN);
		setDisplayOptions(subS.lightingSituation(), DE, RoomLightingRegistration.LITSIT_MAP_DE);
		setWidgetContext(subS, lightingCt);
		
		String subPRad = getSubPath(sr.heatRadiator())+"#$0/heatCostAllocatorReadings"; 
		setLabel(subPRad, EN, "Read Heat cost allocator 1", DE, "Heizkostenverteiler ablesen 1 (aktueller Wert)");
		setWidgetContext(subPRad, radiatprCt);
		setLabel(getSubPath(sr.heatRadiator())+"#$1/heatCostAllocatorReadings", EN, "Read Heat cost allocator 2", DE, "Heizkostenverteiler ablesen 2 (aktueller Wert)");
		setLabel(getSubPath(sr.heatRadiator())+"#$2/heatCostAllocatorReadings", EN, "Read Heat cost allocator 3", DE, "Heizkostenverteiler ablesen 3 (aktueller Wert)");
		
		setLabel(WizBexWidgetProvider.ROOM_NEXT_LABEL_ID, EN, "Next room", DE, "Nächster Raum");
		setLabel(WizBexWidgetProvider.ROOM_BACK_LABEL_ID, EN, "Previous room", DE, "Vorheriger Raum");

		RoomLightingData sub = sr.getSubResource(CapabilityHelper.getSingleResourceName(
				RoomLightingData.class), RoomLightingData.class);
		setLabel(sub, EN, "Lighting Data", DE, "Beleuchtung");

		setLabel(WizBexWidgetProvider.ROOM_TABLE_LABEL_ID, EN, "Wizard Room Overview", DE, "Wizard-Überblick Räume");
		
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
	
	@Override
	protected GenericResourceByTypeTablePageBase createTablePage() {
		return new WizBexRoomTablePage(this, this.getClass().getName()+"_TablePage");
	}
}
