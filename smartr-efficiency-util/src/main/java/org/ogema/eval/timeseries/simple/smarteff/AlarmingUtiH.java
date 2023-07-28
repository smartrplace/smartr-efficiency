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
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
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

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import extensionmodel.smarteff.api.common.BuildingUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AlarmingUtiH {
	public static final String ACTIVESTATUS_RES_NAME = "activeStatus";
	public static final int DEFAULT_ALARM_REPETITION_MINUTES = 48*60;
	public static final int DEFAULT_ALARM_LOWPRIO_REPETITION_MINUTES = 7*24*60;
	public static final int DEFAULT_NOVALUE_MINUTES = 300; //120
	public static final int DEFAULT_NOVALUE_120MINUTES = 120;
	public static final int DEFAULT_NOVALUE_IP_MINUTES = DEFAULT_NOVALUE_MINUTES;
	public static final int DEFAULT_NOVALUE_FORHOURLY_MINUTES = Math.max(DEFAULT_NOVALUE_MINUTES, 180);
	public static final int DEFAULT_NOVALUE_NIGHTLY_MINUTES = Math.max(DEFAULT_NOVALUE_MINUTES, 840); //600
	public static final int DEFAULT_NOVALUE_FOROCCASIONAL_MINUTES = Math.max(DEFAULT_NOVALUE_MINUTES, 1440); //600

	public static final String SP_SUPPORT_FIRST = "Smartrplace Support First";
	public static final String CUSTOMER_FIRST = "Customer First";
	public static final String CUSTOMER_SP_SAME = "Both together";

	public static List<String> roomAlarms = StringFormatHelper.getListFromString(
			"setpointLtOutsideAndOutsideWarm,"
			+ "setpointHighForLong, setpointHighOften");
	public static List<String> deviceAlarms = StringFormatHelper.getListFromString("roomTempHighValvesClosedOutsideCold,"
			+ "roomAboveSetpointValvesClosed, roomAboveSetpointValvesOpen");
	public static List<String> effectiveAlarms = StringFormatHelper.getListFromString(
			"setpointHighForLong, setpointHighOften, roomAboveSetpointValvesOpen, roomTempHighValvesClosedOutsideCold, "
			+ "setpointLtOutsideAndOutsideWarm");

	public static enum DestType {
		SP_SUPPORT_FIRST,
		CUSTOMER_FIRST,
		CUSTOMER_SP_SAME
	}
	public static String getDestString(DestType type) {
		switch(type) {
		case SP_SUPPORT_FIRST: return SP_SUPPORT_FIRST;
		case CUSTOMER_FIRST: return CUSTOMER_FIRST;
		case CUSTOMER_SP_SAME: return CUSTOMER_SP_SAME;
		default: throw new IllegalArgumentException("Unknown DestTye:"+type);
		}
	}
	
	public static interface AlarmingUpdater {
		void updateAlarming();
		
		/** Update alarming with some delay leaving time for further changes to take place*/
		void updateAlarmingWithRetard();
		
		/** Trigger an update of alarming, but allow some retard for more configuration changes to be applied
		 * 
		 * @param maximumRetard
		 * @param restartWithNewCall if true and not another call with this flag set false is pending then the
		 * 		maximumRetard is reset
		 */
		//void updateAlarming(long maximumRetard, boolean restartWithNewCall);
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
		EditPageGeneric.setDefault(data.alarmRepetitionTime(), DEFAULT_ALARM_REPETITION_MINUTES, mode);
		EditPageGeneric.setDefault(data.maxViolationTimeWithoutAlarm(), DEFAULT_NOVALUE_MINUTES, mode);
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
	
	final static Map<ResourceList<AlarmConfiguration>, Map<SingleValueResource, AlarmConfiguration>> alarmConfigs = new ConcurrentHashMap<>();
	final static AtomicInteger callcounter = new AtomicInteger();
	
	static Map<SingleValueResource, AlarmConfiguration> createAlarmConfigMap(ResourceList<AlarmConfiguration> l) {
		Map<SingleValueResource, AlarmConfiguration> m = new ConcurrentHashMap<>();
		for(AlarmConfiguration ac : l.getAllElements()) {
			m.put(ac.sensorVal(), ac);
			m.put(ac.sensorVal().getLocationResource(), ac);
		}
		return m;
	}
	
	static AlarmConfiguration findAlarmConfigInList(ResourceList<AlarmConfiguration> l, SingleValueResource res) {
		return l.getAllElements().stream()
				.filter(ac -> res.equalsLocation(ac.sensorVal()))
				.findFirst().orElse(null);
	}
	
	static AlarmConfiguration findAlarmInfo(ResourceList<AlarmConfiguration> l, SingleValueResource res) {
		Map<SingleValueResource, AlarmConfiguration> res2ac = alarmConfigs.computeIfAbsent(l, AlarmingUtiH::createAlarmConfigMap);
		return res2ac.computeIfAbsent(res, r -> findAlarmConfigInList(l, r));
	}
	
	public static AlarmConfiguration getAlarmConfig(SingleValueResource sensVal, ResourceList<AlarmConfiguration> configs) {
		return findAlarmInfo(configs, sensVal);
		/*
		for(AlarmConfiguration ac: configs.getAllElements()) {
			if(ac.sensorVal().equalsLocation(sensVal)) {
				return ac;
			}
		}
		return null;
		*/
	}

	public static AlarmConfiguration getOrCreateReferencingSensorVal(SingleValueResource sensVal, ResourceList<AlarmConfiguration> list) {
		//System.err.printf("getOrCreateReferencingSensorVal(%s, %s) [%d]%n", sensVal.getPath(), list.getPath(), callcounter.incrementAndGet());
		Map<SingleValueResource, AlarmConfiguration> m = alarmConfigs.computeIfAbsent(list, AlarmingUtiH::createAlarmConfigMap);
		AlarmConfiguration ac = m.computeIfAbsent(sensVal, sv -> {
			AlarmConfiguration result = AlarmingUtiH.findAlarmConfigInList(list, sv);
			if (result != null) {
				return result;
			}
			//System.err.printf("creating new AlarmingConfig for %s in %s%n", sensVal.getPath(), list.getPath());
			result = list.add();
			result.sensorVal().setAsReference(sensVal);
			AlarmingUtiH.setDefaultValuesStatic(result, DefaultSetModes.OVERWRITE);
			result.activate(true);
			return result;
		});
		return ac;
		/*
		for(AlarmConfiguration ac: list.getAllElements()) {
			if(ac.sensorVal().equalsLocation(sensVal))
				return ac;
		}
		AlarmConfiguration result = list.add();
		result.sensorVal().setAsReference(sensVal);
		AlarmingUtiH.setDefaultValuesStatic(result, DefaultSetModes.OVERWRITE);
		result.activate(true);
		return result;
		*/
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
	public static AlarmConfiguration setTemplateValues(InstallAppDevice appDevice, SingleValueResource res,
			 float min, float max,
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues) {
		return setTemplateValues(appDevice, res, min, max, maxViolationTimeWithoutAlarm, maxIntervalBetweenNewValues, true, true,
				DEFAULT_ALARM_REPETITION_MINUTES);
	}
	public static AlarmConfiguration setTemplateValuesIfNew(InstallAppDevice appDevice, SingleValueResource res,
			 float min, float max,
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues) {
		return setTemplateValues(appDevice, res, min, max, maxViolationTimeWithoutAlarm, maxIntervalBetweenNewValues, true, false,
				DEFAULT_ALARM_REPETITION_MINUTES);
	}
	public static AlarmConfiguration setTemplateValues(InstallAppDevice appDevice, SingleValueResource res,
			 float min, float max,
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues,
			float alarmRepetitionTime) {
		return setTemplateValues(appDevice, res, min, max, maxViolationTimeWithoutAlarm, maxIntervalBetweenNewValues, true, true,
				alarmRepetitionTime);
	}
	public static AlarmConfiguration setTemplateValues(InstallAppDevice appDevice, SingleValueResource res,
			 float min, float max,
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues,
			float alarmRepetitionTime,
			boolean overwriteExisting) {
		return setTemplateValues(appDevice, res, min, max, maxViolationTimeWithoutAlarm, maxIntervalBetweenNewValues, true, overwriteExisting,
				alarmRepetitionTime);		
	}
	
	/**
	 * 
	 * @param appDevice
	 * @param res
	 * @param min
	 * @param max
	 * @param maxViolationTimeWithoutAlarm
	 * @param maxIntervalBetweenNewValues
	 * @param alarmRepetitionTime
	 * @param destType
	 * @param alarmLevel 1:LOW, 2:MEDIUM/Normal, 3:HIGH
	 * @return
	 */
	public static AlarmConfiguration setTemplateValues(InstallAppDevice appDevice, SingleValueResource res,
			 float min, float max,
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues,
			float alarmRepetitionTime,
			DestType destType, Integer alarmLevel) {
		return setTemplateValues(appDevice, res, min, max, maxViolationTimeWithoutAlarm, maxIntervalBetweenNewValues, true, true,
				DEFAULT_ALARM_REPETITION_MINUTES, destType, alarmLevel);
	}
	public static AlarmConfiguration setTemplateValuesIfNew(InstallAppDevice appDevice, SingleValueResource res,
			 float min, float max,
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues,
			float alarmRepetitionTime,
			DestType destType, Integer alarmLevel) {
		return setTemplateValues(appDevice, res, min, max, maxViolationTimeWithoutAlarm, maxIntervalBetweenNewValues, true, false,
				DEFAULT_ALARM_REPETITION_MINUTES, destType, alarmLevel);
	}
	
	
	public static AlarmConfiguration setTemplateValues(InstallAppDevice appDevice, SingleValueResource res,
			 float min, float max,
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues,
			boolean sendAlarminitially) {
		return setTemplateValues(appDevice, res, min, max, maxViolationTimeWithoutAlarm, maxIntervalBetweenNewValues, sendAlarminitially, true,
				DEFAULT_ALARM_REPETITION_MINUTES);
	}
	public static AlarmConfiguration setTemplateValues(InstallAppDevice appDevice, SingleValueResource res,
			 float min, float max,
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues,
			boolean sendAlarminitially,
			boolean overWriteExisting,
			float alarmRepetitionTime) {
		return setTemplateValues(appDevice, res, min, max, maxViolationTimeWithoutAlarm, maxIntervalBetweenNewValues, sendAlarminitially, true,
				alarmRepetitionTime, null, null);
		
	}
	public static AlarmConfiguration setTemplateValues(InstallAppDevice appDevice, SingleValueResource res,
			 float min, float max,
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues,
			boolean sendAlarminitially,
			boolean overWriteExisting,
			float alarmRepetitionTime,
			DestType destType, Integer alarmLevel) {
		/*if(!res.exists())
			return null;
		AlarmConfiguration alarm = AlarmingUtiH.getOrCreateReferencingSensorVal(
				res, appDevice.alarms());*/
		AlarmConfiguration alarm = getAlarmingConfiguration(appDevice, res);
		if(alarm == null)
			return null;
		setTemplateValues(alarm, min, max, maxViolationTimeWithoutAlarm,
				maxIntervalBetweenNewValues, sendAlarminitially, overWriteExisting, alarmRepetitionTime, destType, alarmLevel);
		return alarm;
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
		setTemplateValues(data, min, max, maxViolationTimeWithoutAlarm, maxIntervalBetweenNewValues, sendAlarminitially,
				overWriteExisting, DEFAULT_ALARM_REPETITION_MINUTES);
	}
	public static void setTemplateValues(AlarmConfiguration data, float min, float max, 
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues,
			boolean sendAlarminitially,
			boolean overWriteExisting, float alarmRepetitionTime) {		
		setTemplateValues(data, min, max, maxViolationTimeWithoutAlarm, maxIntervalBetweenNewValues, sendAlarminitially,
				overWriteExisting, alarmRepetitionTime, null, null);		
	}
	public static void setTemplateValues(AlarmConfiguration data, float min, float max, 
			float maxViolationTimeWithoutAlarm, float maxIntervalBetweenNewValues,
			boolean sendAlarminitially,
			boolean overWriteExisting, float alarmRepetitionTime,
			DestType destType, Integer alarmLevel) {		
		Long shortIntervalForTesting = Long.getLong("org.smartrplace.apps.hw.install.init.alarmtesting.shortintervals");
		if(shortIntervalForTesting != null) {
			double floatVal = ((double)shortIntervalForTesting)/TimeProcUtil.MINUTE_MILLIS;
			if(maxIntervalBetweenNewValues >  floatVal)
				maxIntervalBetweenNewValues = (float) floatVal;
		}

		//Special handlings
		//if(data.sensorVal().getLocation().toLowerCase().contains("homematicip")) {
		//	if((maxIntervalBetweenNewValues >= 0) && (maxIntervalBetweenNewValues < 4*60))
		//		maxIntervalBetweenNewValues = 4*60;
		//}

		//String defaultAlarmSetMode = System.getProperty("org.smartrplace.apps.hw.install.init.alarmtesting.defaultAlarmSetMode");
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
			EditPageGeneric.setDefault(data.alarmRepetitionTime(), alarmRepetitionTime, mode);
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
		
		if(destType != null)
			EditPageGeneric.setDefault(data.alarmingAppId(), getDestString(destType), mode);

		if(alarmLevel != null)
			EditPageGeneric.setDefault(data.alarmLevel(), alarmLevel, mode);
	}
	
	public static void addAlarmingHomematic(PhysicalElement dev, InstallAppDevice appDevice) {
		addAlarmingHomematic(dev, appDevice, 2);
	}
	public static void addAlarmingHomematic(PhysicalElement dev, InstallAppDevice appDevice,
			int batteryNum) {
		addAlarmingHomematic(dev, appDevice, batteryNum, true);
	}
	public static void addAlarmingHomematic(PhysicalElement dev, InstallAppDevice appDevice,
			int batteryNum,
			boolean overwriteExisting) {
		dev = dev.getLocationResource();
		DefaultSetModes mode = overwriteExisting?DefaultSetModes.OVERWRITE:DefaultSetModes.SET_IF_NEW;
		VoltageResource batteryVoltage = DeviceHandlerBase.getBatteryVoltage(dev); //dev.getSubResource("battery", ElectricityStorage.class).internalVoltage().reading();
		//if(!batteryVoltage.isActive())
		//	batteryVoltage = ResourceHelper.getSubResourceOfSibbling(dev,
		//			"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "battery/internalVoltage/reading", VoltageResource.class);
		if(batteryVoltage != null && batteryVoltage.isActive() && batteryNum > 0) {
			if(batteryNum == 2)
				AlarmingUtiH.setTemplateValues(appDevice, batteryVoltage,
					2.3f, 3.5f, 10, DEFAULT_NOVALUE_FOROCCASIONAL_MINUTES, DEFAULT_ALARM_LOWPRIO_REPETITION_MINUTES, overwriteExisting);
			else
				AlarmingUtiH.setTemplateValues(appDevice, batteryVoltage,
						batteryNum*1.1f, batteryNum*1.8f, 10, DEFAULT_NOVALUE_FOROCCASIONAL_MINUTES, DEFAULT_ALARM_LOWPRIO_REPETITION_MINUTES, overwriteExisting);
		}
		BooleanResource batteryStatus = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "batteryLow", BooleanResource.class);
		if(batteryStatus != null && batteryStatus.exists())
			AlarmingUtiH.setTemplateValues(appDevice, batteryStatus,
					0.0f, 0.0f, 10, -1, DEFAULT_ALARM_LOWPRIO_REPETITION_MINUTES, overwriteExisting);
		//BooleanResource comDisturbed = ResourceHelper.getSubResourceOfSibbling(dev,
		//		"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "communicationStatus/communicationDisturbed", BooleanResource.class);
		//if(comDisturbed != null && comDisturbed.exists())
		//	AlarmingUtiH.setTemplateValues(appDevice, comDisturbed,
		//			0.0f, 1.0f, 60, -1);
		IntegerResource rssiDevice = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiDevice", IntegerResource.class);
		if(rssiDevice != null && rssiDevice.exists()) {
			int ccuType = DeviceTableBase.getHomematicType(dev.getLocation());
			float low;
			if(ccuType == 1 || ccuType == 2)
				low = -65535f;
			else
				low = -120f;
			AlarmingUtiH.setTemplateValues(appDevice, rssiDevice,
					low, 128f, 10, 720);
		}
		IntegerResource rssiPeer = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiPeer", IntegerResource.class);
		if(rssiPeer != null && rssiPeer.exists() && overwriteExisting)
			AlarmingUtiH.setAlarmingActiveStatus(appDevice, rssiPeer, false);
			//AlarmingUtiH.setTemplateValues(appDevice, rssiPeer,
			//		-120f, -10f, 10, -1);
	}

	public static void addAlarmingMQTT(PhysicalElement dev, InstallAppDevice appDevice) {
		dev = dev.getLocationResource();
		CommunicationStatus comStat = dev.getSubResource("communicationStatus", CommunicationStatus.class);
		if(comStat.isActive()) {
			AlarmingUtiH.setTemplateValues(appDevice, comStat.quality(),
					0.05f, 1.0f, 30, DEFAULT_NOVALUE_MINUTES);
		}
	}
	
	public static AlarmConfiguration getAlarmingConfiguration(InstallAppDevice appDevice, SingleValueResource res) {
		if(!res.exists())
			return null;
		AlarmConfiguration data = AlarmingUtiH.getOrCreateReferencingSensorVal(
				res, appDevice.alarms());
		return data;
	}
	public static AlarmConfiguration setAlarmingActiveStatus(InstallAppDevice appDevice, SingleValueResource res, boolean sendAlarminitially) {
		DefaultSetModes mode = DefaultSetModes.OVERWRITE;
		AlarmConfiguration data = getAlarmingConfiguration(appDevice, res);
		if(data == null)
			return null;
		if(sendAlarminitially)
			EditPageGeneric.setDefault(data.sendAlarm(), true, mode);
		else
			EditPageGeneric.setDefault(data.sendAlarm(), false, mode);
		return data;
	}
	
	public static String getAutoThermostatModeShort(int value) {
		switch(value) {
		case 0:
			return "CfP-dependent";
		case 1:
			return "Allow";
		case 2:
			return "Off";
		case 3:
			return "OFF Forced";
		default:
			return "UNKNOWN STATE:"+value;
		}
	}
	
	public static String getWeeklyPostponeModeShort(int value) {
		switch(value) {
		case 0:
			return "Property:"+Boolean.getBoolean("org.smartrplace.homematic.devicetable.autostart.shiftdecalc");
		case 1:
			return "No Postpone/Daily";
		case 2:
			return "Postpone";
		case 3:
			return "Decalc Daily";
		default:
			return "UNKNOWN STATE:"+value;
		}
	}
	
	public static String getSendIntervalModeShort(int value) {
		switch(value) {
		case 0:
			return "Default Cyclic";
		case 1:
			return "Default Cyclic Forced";
		case 2:
			return "No Cyclic";
		case 3:
			return "No Cyclic Forced";
		case 4:
			return "Saving Cyclic";
		case 5:
			return "Saving Cyclic Forced";
		default:
			return "UNKNOWN STATE:"+value;
		}
	}
}
