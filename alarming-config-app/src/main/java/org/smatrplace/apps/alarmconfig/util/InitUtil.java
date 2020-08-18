package org.smatrplace.apps.alarmconfig.util;

public class InitUtil {
	/*public static void initAlarmingForManual(ApplicationManagerPlus appManPlus) {
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
	}*/

	/*public static void initAlarmForSensor(Sensor dev, Room room, SmartEffUserDataNonEdit user,
			MonitoringController controller) {
		BuildingUnit bu = controller.getBuildingUnitByRoom(dev, room, user.editableData());
		if(bu == null) {
			controller.appMan.getLogger().warn("No room found for device "+dev);
			return;
		}
		initAlarmForSensor(dev, bu, user, controller);
	}*/
	/*public static void initAlarmForSensor2(SingleValueResource dev, BuildingUnit bu,
			RoomLabelProvider roomLabelProv) {
		@SuppressWarnings("unchecked")
		ResourceList<AlarmConfiguration> alarms = bu.getSubResource("alarmConfigs", ResourceList.class);
		alarms.create();
		if(alarms.getElementType() == null)
			alarms.setElementType(AlarmConfiguration.class);

		AlarmConfiguration ac = AlarmingUtiH.getAlarmConfig2(alarms, dev);
		if(ac == null) {
			ac = alarms.add();
			AlarmingUtiH.setDefaultValuesStatic(ac , DefaultSetModes.OVERWRITE);
			ac.sensorVal().setAsReference(dev);
			ac.activate(true);
			dev.addDecorator(AlarmingService.ALARMSTATUS_RES_NAME, IntegerResource.class).activate(false);
		}
		//ValueResourceHelper.setCreate(ac.name(), controller.getLabel(ac, bu.name().getValue().equals("gesamt")));
		if(ValueResourceHelper.setIfNew(ac.name(), roomLabelProv.getLabel(ac, bu.name().getValue().equals("gesamt"))))
			ac.activate(true);
	}*/
}
