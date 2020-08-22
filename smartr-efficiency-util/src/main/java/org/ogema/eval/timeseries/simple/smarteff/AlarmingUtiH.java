package org.ogema.eval.timeseries.simple.smarteff;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.PowerResource;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.sensors.PowerSensor;
import org.ogema.model.sensors.Sensor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
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
	public static AlarmConfiguration getAlarmConfig2(ResourceList<AlarmConfiguration> configs, SingleValueResource dev) {
		for(AlarmConfiguration ac: configs.getAllElements()) {
			if(ac.sensorVal().equalsLocation(dev)) {
				return ac;
			}
		}
		return null;
	}
	
	public static void cleanUpAlarmConfigs(ResourceList<AlarmConfiguration> configs) {
		for(AlarmConfiguration ac: configs.getAllElements()) {
			if((!ac.sensorVal().exists())) //&&(!ac.supervisedTS().exists()))
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
	
	/*public static Resource getSupervised(AlarmConfiguration ac) {
		//if(ac.supervisedTS().exists()) return ac.supervisedTS();
		return ac.supervisedSensor();
	}*/
	
	public static OnOffSwitch getSwitchFromSensor(SingleValueResource sensVal) {
		if(!(sensVal instanceof PowerResource)) {
			return null;
		}
		Sensor sensValP = ResourceHelper.getFirstParentOfType(sensVal, Sensor.class);
		if(sensValP == null || (!(sensValP instanceof PowerSensor)))
			return null;
		PowerSensor sens = (PowerSensor)sensValP;
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
		if(data.sensorVal().exists() &&
				AlarmingUtiH.getSwitchFromSensor(data.sensorVal()) != null) {
			EditPageGeneric.setDefault(data.performAdditinalOperations(), true, mode);
		} else
			EditPageGeneric.setDefault(data.performAdditinalOperations(), false, mode);
	}
	
	public static AlarmConfiguration getOrCreateReferencingSensorVal(SingleValueResource sensVal, ResourceList<AlarmConfiguration> list) {
		for(AlarmConfiguration el: list.getAllElements()) {
			if(el.sensorVal().equalsLocation(sensVal))
				return el;
		}
		AlarmConfiguration result = list.add();
		result.sensorVal().setAsReference(sensVal);
		AlarmingUtiH.setDefaultValuesStatic(result, DefaultSetModes.OVERWRITE);
		result.activate(true);
		return result;
	}

	public static void setTemplateValues(InstallAppDevice appDevice, SingleValueResource res,
			 float min, float max,
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues) {
		if(!res.exists())
			return;
		AlarmConfiguration alarm = AlarmingUtiH.getOrCreateReferencingSensorVal(
				res, appDevice.alarms());
		setTemplateValues(alarm, 5.0f, 35.0f, 15, 20);		
	}
	public static void setTemplateValues(AlarmConfiguration data, float min, float max, 
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues) {
		DefaultSetModes mode = DefaultSetModes.OVERWRITE;
		EditPageGeneric.setDefault(data.alarmLevel(), 1, mode);
		EditPageGeneric.setDefault(data.alarmRepetitionTime(), 60, mode);
		EditPageGeneric.setDefault(data.maxViolationTimeWithoutAlarm(), maxViolationTimeWithoutAlarm, mode);
		EditPageGeneric.setDefault(data.lowerLimit(), min, mode);
		EditPageGeneric.setDefault(data.upperLimit(), max, mode);
		EditPageGeneric.setDefault(data.maxIntervalBetweenNewValues(), maxIntervalBetweenNewValues, mode);
		EditPageGeneric.setDefault(data.sendAlarm(), true, mode);
		
		//TODO: By default we perform switch supervision, may not really work
		if(data.sensorVal().exists() &&
				AlarmingUtiH.getSwitchFromSensor(data.sensorVal()) != null) {
			EditPageGeneric.setDefault(data.performAdditinalOperations(), true, mode);
		} else
			EditPageGeneric.setDefault(data.performAdditinalOperations(), false, mode);
	}
}
