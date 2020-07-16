package org.smartrplace.apps.alarmingconfig.mgmt;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.sensors.PowerSensor;
import org.ogema.model.sensors.Sensor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric.DefaultSetModes;

import de.iwes.util.resource.ResourceHelper;
import extensionmodel.smarteff.api.common.BuildingUnit;

public class AlarmingUtiH {
	public static final String ACTIVESTATUS_RES_NAME = "activeStatus";
	
	/*public static AlarmConfiguration getAlarmConfig(ResourceList<AlarmConfiguration> configs, SmartEffTimeSeries dev) {
		for(AlarmConfiguration ac: configs.getAllElements()) {
			if(ac.supervisedTS().equalsLocation(dev)) {
				return ac;
			}
		}
		return null;
	}*/
	public static AlarmConfiguration getAlarmConfig(ResourceList<AlarmConfiguration> configs, Sensor dev) {
		for(AlarmConfiguration ac: configs.getAllElements()) {
			if(ac.supervisedSensor().equalsLocation(dev)) {
				return ac;
			}
		}
		return null;
	}
	
	public static void cleanUpAlarmConfigs(ResourceList<AlarmConfiguration> configs) {
		for(AlarmConfiguration ac: configs.getAllElements()) {
			if((!ac.supervisedSensor().exists())) //&&(!ac.supervisedTS().exists()))
				ac.delete();
		}		
	}
	
	/** Simplified version of GenericDriverProvider#getTimeSeries(SmartEffTimeSeries dpTS)
	 */
	public static Resource getResourceForTimeSeries(SmartEffTimeSeries dpTS) {
		if(dpTS.schedule().isActive()) return dpTS.schedule();
		if(dpTS.recordedDataParent().isActive()) return dpTS.recordedDataParent();
		return null;
	}
	
	public static Resource getSupervised(AlarmConfiguration ac) {
		//if(ac.supervisedTS().exists()) return ac.supervisedTS();
		return ac.supervisedSensor();
	}
	
	public static OnOffSwitch getSwitchFromSensor(Sensor sens) {
		if(!(sens instanceof PowerSensor)) {
			return null;
		}
		Resource hm = ResourceHelper.getFirstParentOfType(sens, "org.ogema.drivers.homematic.xmlrpc.hl.types.HmDevice");
		if(hm == null) return null;
		List<OnOffSwitch> onOffs = hm.getSubResources(OnOffSwitch.class, true);
		if(onOffs.isEmpty()) return null;
		if(onOffs.size() == 2) {
			if(onOffs.get(0).isReference(false))
				return onOffs.get(1);
			if(onOffs.get(1).isReference(false))
				return onOffs.get(0);
		}
		if(onOffs.size() > 1) {
			System.out.println("More than one OnOfSwitch in:"+hm.getLocation());
			return null;
		}
		return onOffs.get(0);
	}
	
	public static String getRoomNameFromSub(Resource res) {
		BuildingUnit bu = ResourceHelper.getFirstParentOfType(res, BuildingUnit.class);
		String room;
		if(bu != null) {
			room = ResourceUtils.getHumanReadableShortName(bu);
		} else
			room = "Anlage";
		return room;
	}
	
	public static void setDefaultValuesStatic(AlarmConfiguration data, DefaultSetModes mode) {
		EditPageGeneric.setDefault(data.alarmLevel(), 1, mode);
		EditPageGeneric.setDefault(data.alarmRepetitionTime(), 60, mode);
		EditPageGeneric.setDefault(data.maxViolationTimeWithoutAlarm(), 10, mode);
		EditPageGeneric.setDefault(data.lowerLimit(), 0, mode);
		EditPageGeneric.setDefault(data.upperLimit(), 100, mode);
		EditPageGeneric.setDefault(data.maxIntervalBetweenNewValues(), -1, mode);
		EditPageGeneric.setDefault(data.sendAlarm(), false, mode);
		if(data.supervisedSensor().exists() &&
				AlarmingUtiH.getSwitchFromSensor(data.supervisedSensor()) != null) {
			EditPageGeneric.setDefault(data.performAdditinalOperations(), true, mode);
		} else
			EditPageGeneric.setDefault(data.performAdditinalOperations(), false, mode);
	}
}
