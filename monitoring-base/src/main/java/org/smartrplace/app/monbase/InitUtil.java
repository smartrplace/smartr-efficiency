package org.smartrplace.app.monbase;

import java.util.List;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.Sensor;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.monbase.alarming.AlarmingManagement;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric.DefaultSetModes;
import org.sp.smarteff.monitoring.alarming.AlarmingEditPage;
import org.sp.smarteff.monitoring.alarming.AlarmingUtil;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ValueResourceHelper;
import extensionmodel.smarteff.api.base.SmartEffUserData;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.monitoring.AlarmConfigBase;

public class InitUtil {
	public static void initAlarmingForManual(MonitoringController controller) {
		SmartEffUserDataNonEdit user = controller.appMan.getResourceAccess().getResource("master");
		if(user == null) return;
		if(user.editableData().buildingData().size() != 1) return;
		ResourceList<BuildingUnit> buildings = user.editableData().buildingData().getAllElements().get(0).buildingUnit();
		for(BuildingUnit bu: buildings.getAllElements()) {
			@SuppressWarnings("unchecked")
			ResourceList<AlarmConfigBase> alarms = bu.getSubResource("alarmConfig", ResourceList.class);
			if(!alarms.exists()) {
				alarms.create();
				alarms.setElementType(AlarmConfigBase.class);
			} 
			//FIXME: This is a workaround because import from JSON does not work properly
			else if(alarms.getElementType() == null) {
				alarms.setElementType(AlarmConfigBase.class);				
			}
			StringResource manualAlarmNames = buildings.getSubResource("manualAlarmNames", StringResource.class);
			if(!manualAlarmNames.isActive()) {
				manualAlarmNames.create();
				manualAlarmNames.activate(true);
			}
			List<String> alarmNames = StringFormatHelper.getListFromString(manualAlarmNames.getValue());
			List<SmartEffTimeSeries> tss = controller.getManualTimeSeries(bu);
			if(tss != null) {
				for(SmartEffTimeSeries ts: tss) {
					AlarmConfigBase ac = AlarmingUtil.getAlarmConfig(alarms, ts);
					//Usually no alarming needs to be configured for manual data entry
					if(alarmNames.contains(ts.getName())) {
						if(ac == null) {
							ac = alarms.add();
							AlarmingEditPage.setDefaultValuesStatic(ac, DefaultSetModes.OVERWRITE);
							ac.supervisedTS().setAsReference(ts);
							ts.addDecorator(AlarmingManagement.ALARMSTATUS_RES_NAME, IntegerResource.class);
						}
						ValueResourceHelper.setCreate(ac.name(), controller.getLabel(ac, false));
					} else {
						if((ac != null) && (!ac.sendAlarm().getValue())) {
							ac.delete();
						}
					}
				}
			}
			if(!alarms.isActive()) alarms.activate(true);
			AlarmingUtil.cleanUpAlarmConfigs(alarms);
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

	public static void initAlarmForSensor(Sensor dev, Room room, SmartEffUserDataNonEdit user,
			MonitoringController controller) {
		BuildingUnit bu = controller.getBuildingUnitByRoom(dev, room, user.editableData());
		if(bu == null) {
			controller.appMan.getLogger().warn("No room found for device "+dev);
			return;
		}
		initAlarmForSensor(dev, bu, user, controller);
	}
	public static void initAlarmForSensor(Sensor dev, BuildingUnit bu, SmartEffUserDataNonEdit user,
			MonitoringController controller) {
		@SuppressWarnings("unchecked")
		ResourceList<AlarmConfigBase> alarms = bu.getSubResource("alarmConfig", ResourceList.class);
		alarms.create();
		if(alarms.getElementType() == null)
			alarms.setElementType(AlarmConfigBase.class);

		AlarmConfigBase ac = AlarmingUtil.getAlarmConfig(alarms, dev);
		if(ac == null) {
			ac = alarms.add();
			AlarmingEditPage.setDefaultValuesStatic(ac , DefaultSetModes.OVERWRITE);
			ac.supervisedSensor().setAsReference(dev);
			ac.activate(true);
			dev.addDecorator(AlarmingManagement.ALARMSTATUS_RES_NAME, IntegerResource.class);
		}
		ValueResourceHelper.setCreate(ac.name(), controller.getLabel(ac, bu.name().getValue().equals("gesamt")));
		
	}
}
