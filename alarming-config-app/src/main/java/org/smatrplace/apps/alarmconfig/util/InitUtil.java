package org.smatrplace.apps.alarmconfig.util;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.sensors.Sensor;
import org.smartrplace.apps.alarmingconfig.mgmt.AlarmingManager;
import org.smartrplace.apps.alarmingconfig.mgmt.AlarmingUtiH;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric.DefaultSetModes;

import de.iwes.util.resource.ValueResourceHelper;
import extensionmodel.smarteff.api.base.SmartEffUserData;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnit;

public class InitUtil {
	public static void initAlarmingForManual(ApplicationManagerPlus appManPlus) {
		SmartEffUserDataNonEdit user = appManPlus.appMan().getResourceAccess().getResource("master");
		if(user == null) return;
		if(user.editableData().buildingData().size() != 1) return;
		ResourceList<BuildingUnit> buildings = user.editableData().buildingData().getAllElements().get(0).buildingUnit();
		for(BuildingUnit bu: buildings.getAllElements()) {
			@SuppressWarnings("unchecked")
			ResourceList<AlarmConfiguration> alarms = bu.getSubResource("alarmConfigs", ResourceList.class);
			if(!alarms.exists()) {
				alarms.create();
				alarms.setElementType(AlarmConfiguration.class);
			} 
			//FIXME: This is a workaround because import from JSON does not work properly
			else if(alarms.getElementType() == null) {
				alarms.setElementType(AlarmConfiguration.class);				
			}
			
			// We do not manual resources for now
			/*StringResource manualAlarmNames = buildings.getSubResource("manualAlarmNames", StringResource.class);
			if(!manualAlarmNames.isActive()) {
				manualAlarmNames.create();
				manualAlarmNames.activate(true);
			}
			List<String> alarmNames = StringFormatHelper.getListFromString(manualAlarmNames.getValue());
			List<SmartEffTimeSeries> tss = controller.getManualTimeSeries(bu);
			if(tss != null) {
				for(SmartEffTimeSeries ts: tss) {
					AlarmConfiguration ac = AlarmingUtiH.getAlarmConfig(alarms, ts);
					//Usually no alarming needs to be configured for manual data entry
					if(alarmNames.contains(ts.getName())) {
						if(ac == null) {
							ac = alarms.add();
							AlarmingEditPage.setDefaultValuesStatic(ac, DefaultSetModes.OVERWRITE);
							ac.supervisedTS().setAsReference(ts);
							ts.addDecorator(AlarmingManager.ALARMSTATUS_RES_NAME, IntegerResource.class);
						}
						ValueResourceHelper.setCreate(ac.name(), controller.getLabel(ac, false));
					} else {
						if((ac != null) && (!ac.sendAlarm().getValue())) {
							ac.delete();
						}
					}
				}
			}*/
			if(!alarms.isActive()) alarms.activate(true);
			AlarmingUtiH.cleanUpAlarmConfigs(alarms);
		}
	}
	
	public static BuildingUnit getBuildingUnitByRoom(final String roomName, SmartEffUserData user) {
		for(BuildingData build: user.buildingData().getAllElements()) {
			for(BuildingUnit buildUnit: build.buildingUnit().getAllElements()) {
				if(buildUnit.name().getValue().equals(roomName))
					return buildUnit;				
			}
		}
		return null;
	}

	/*public static void initAlarmForSensor(Sensor dev, Room room, SmartEffUserDataNonEdit user,
			MonitoringController controller) {
		BuildingUnit bu = controller.getBuildingUnitByRoom(dev, room, user.editableData());
		if(bu == null) {
			controller.appMan.getLogger().warn("No room found for device "+dev);
			return;
		}
		initAlarmForSensor(dev, bu, user, controller);
	}*/
	public static void initAlarmForSensor(Sensor dev, BuildingUnit bu,
			RoomLabelProvider roomLabelProv) {
		@SuppressWarnings("unchecked")
		ResourceList<AlarmConfiguration> alarms = bu.getSubResource("alarmConfigs", ResourceList.class);
		alarms.create();
		if(alarms.getElementType() == null)
			alarms.setElementType(AlarmConfiguration.class);

		AlarmConfiguration ac = AlarmingUtiH.getAlarmConfig(alarms, dev);
		if(ac == null) {
			ac = alarms.add();
			AlarmingUtiH.setDefaultValuesStatic(ac , DefaultSetModes.OVERWRITE);
			ac.supervisedSensor().setAsReference(dev);
			ac.activate(true);
			dev.addDecorator(AlarmingManager.ALARMSTATUS_RES_NAME, IntegerResource.class).activate(false);
		}
		//ValueResourceHelper.setCreate(ac.name(), controller.getLabel(ac, bu.name().getValue().equals("gesamt")));
		if(ValueResourceHelper.setIfNew(ac.name(), roomLabelProv.getLabel(ac, bu.name().getValue().equals("gesamt"))))
			ac.activate(true);
	}
}
