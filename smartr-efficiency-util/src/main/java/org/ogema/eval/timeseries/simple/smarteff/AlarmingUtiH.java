package org.ogema.eval.timeseries.simple.smarteff;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.PowerResource;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.communication.CommunicationStatus;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.PowerSensor;
import org.ogema.model.sensors.Sensor;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
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
	
	
	public static List<AlarmConfiguration> getDoubletsForReferencingSensorVal(ResourceList<AlarmConfiguration> list) {
		Set<String> knownSensValLocs = new HashSet<>();
		List<AlarmConfiguration> toRemove = new ArrayList<>();
		for(AlarmConfiguration el: list.getAllElements()) {
			String loc = el.sensorVal().getLocation();
			if(knownSensValLocs.contains(loc)) {
				toRemove.add(el);
			} else
				knownSensValLocs.add(loc);
		}
		return toRemove;
	}

	/** Configure alarming parameters for a value. The alarm is set to "sendAlarm" status by default, but this can be
	 * avoided with an additional last parameter. No alarms will be sent if the general configuration
	 * {@link HardwareInstallConfig#isAlarmingActive()} is not set to true.
	 * @param appDevice
	 * @param res
	 * @param min
	 * @param max
	 * @param maxViolationTimeWithoutAlarm
	 * @param maxIntervalBetweenNewValues
	 */
	public static void setTemplateValues(InstallAppDevice appDevice, SingleValueResource res,
			 float min, float max,
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues) {
		setTemplateValues(appDevice, res, min, max, maxViolationTimeWithoutAlarm, maxIntervalBetweenNewValues, true, true);
	}
	public static void setTemplateValues(InstallAppDevice appDevice, SingleValueResource res,
			 float min, float max,
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues,
			boolean sendAlarminitially) {
		setTemplateValues(appDevice, res, min, max, maxViolationTimeWithoutAlarm, maxIntervalBetweenNewValues, sendAlarminitially, true);
	}
	public static void setTemplateValues(InstallAppDevice appDevice, SingleValueResource res,
			 float min, float max,
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues,
			boolean sendAlarminitially,
			boolean overWriteExisting) {
		if(!res.exists())
			return;
		AlarmConfiguration alarm = AlarmingUtiH.getOrCreateReferencingSensorVal(
				res, appDevice.alarms());
		setTemplateValues(alarm, min, max, maxViolationTimeWithoutAlarm,
				maxIntervalBetweenNewValues, sendAlarminitially, overWriteExisting);		
	}
	
	public static void setTemplateValues(AlarmConfiguration data, float min, float max, 
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues,
			boolean sendAlarminitially) {		
		setTemplateValues(data, min, max, maxViolationTimeWithoutAlarm, maxIntervalBetweenNewValues, sendAlarminitially, true);
	}
	public static void setTemplateValues(AlarmConfiguration data, float min, float max, 
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues,
			boolean sendAlarminitially,
			boolean overWriteExisting) {		
		Long shortIntervalForTesting = Long.getLong("org.smartrplace.apps.hw.install.init.alarmtesting.shortintervals");
		if(shortIntervalForTesting != null) {
			double floatVal = ((double)shortIntervalForTesting)/TimeProcUtil.MINUTE_MILLIS;
			if(maxIntervalBetweenNewValues >  floatVal)
				maxIntervalBetweenNewValues = (float) floatVal;
		}

		//Special handlings
		if(data.sensorVal().getLocation().toLowerCase().contains("homematicip")) {
			if((maxIntervalBetweenNewValues >= 0) && (maxIntervalBetweenNewValues < 4*60))
				maxIntervalBetweenNewValues = 4*60;
		}

		String defaultAlarmSetMode = System.getProperty("org.smartrplace.apps.hw.install.init.alarmtesting.defaultAlarmSetMode");
		DefaultSetModes mode = overWriteExisting?DefaultSetModes.OVERWRITE:DefaultSetModes.SET_IF_NEW;
		/*DefaultSetModes mode = null;
		if(defaultAlarmSetMode != null) try {
			mode = DefaultSetModes.valueOf(defaultAlarmSetMode);
		} catch(IllegalArgumentException e) {}
		if(mode == null)
			mode = DefaultSetModes.OVERWRITE;*/
		EditPageGeneric.setDefault(data.alarmLevel(), 1, mode);
		Long shortResend = Long.getLong("org.smartrplace.apps.hw.install.init.alarmtesting.shortresend");
		if(shortResend != null)
			EditPageGeneric.setDefault(data.alarmRepetitionTime(), ((double)shortResend)/TimeProcUtil.MINUTE_MILLIS, mode);
		else
			EditPageGeneric.setDefault(data.alarmRepetitionTime(), 48*60, mode);
		EditPageGeneric.setDefault(data.maxViolationTimeWithoutAlarm(), maxViolationTimeWithoutAlarm, mode);
		EditPageGeneric.setDefault(data.lowerLimit(), min, mode);
		EditPageGeneric.setDefault(data.upperLimit(), max, mode);
		
		EditPageGeneric.setDefault(data.maxIntervalBetweenNewValues(), maxIntervalBetweenNewValues, mode);
		//if(Boolean.getBoolean("org.smartrplace.apps.hw.install.init.sendAlarmsinitially"))
		if(sendAlarminitially)
			EditPageGeneric.setDefault(data.sendAlarm(), true, mode);
		else
			EditPageGeneric.setDefault(data.sendAlarm(), false, mode);
		
		//TODO: By default we perform switch supervision, may not really work
		if(data.sensorVal().exists() &&
				AlarmingUtiH.getSwitchFromSensor(data.sensorVal()) != null) {
			EditPageGeneric.setDefault(data.performAdditinalOperations(), true, mode);
		} else
			EditPageGeneric.setDefault(data.performAdditinalOperations(), false, mode);
	}
	
	public static void addAlarmingHomematic(PhysicalElement dev, InstallAppDevice appDevice) {
		addAlarmingHomematic(dev, appDevice, 2);
	}
	public static void addAlarmingHomematic(PhysicalElement dev, InstallAppDevice appDevice,
			int batteryNum) {
		dev = dev.getLocationResource();
		VoltageResource batteryVoltage = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "battery/internalVoltage/reading", VoltageResource.class);
		if(batteryVoltage != null && batteryNum > 0) {
			if(batteryNum == 2)
				AlarmingUtiH.setTemplateValues(appDevice, batteryVoltage,
					1.5f, 3.5f, 10, 70);
			else
				AlarmingUtiH.setTemplateValues(appDevice, batteryVoltage,
						batteryNum*0.7f, batteryNum*1.8f, 10, 70);
		}
		BooleanResource batteryStatus = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "batteryLow", BooleanResource.class);
		if(batteryStatus != null && batteryStatus.exists())
			AlarmingUtiH.setTemplateValues(appDevice, batteryStatus,
					0.0f, 0.0f, 10, -1);
		//BooleanResource comDisturbed = ResourceHelper.getSubResourceOfSibbling(dev,
		//		"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "communicationStatus/communicationDisturbed", BooleanResource.class);
		//if(comDisturbed != null && comDisturbed.exists())
		//	AlarmingUtiH.setTemplateValues(appDevice, comDisturbed,
		//			0.0f, 1.0f, 60, -1);
		IntegerResource rssiDevice = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiDevice", IntegerResource.class);
		if(rssiDevice != null && rssiDevice.exists())
			AlarmingUtiH.setTemplateValues(appDevice, rssiDevice,
					-94f, -10f, 10, 300);
		IntegerResource rssiPeer = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiPeer", IntegerResource.class);
		if(rssiPeer != null && rssiPeer.exists())
			AlarmingUtiH.setTemplateValues(appDevice, rssiPeer,
					-94f, -10f, 10, 300);
	}

	public static void addAlarmingMQTT(PhysicalElement dev, InstallAppDevice appDevice) {
		dev = dev.getLocationResource();
		CommunicationStatus comStat = dev.getSubResource("communicationStatus", CommunicationStatus.class);
		if(comStat.isActive()) {
			AlarmingUtiH.setTemplateValues(appDevice, comStat.quality(),
					0.1f, 1.0f, 30, 30);
		}
	}
}
