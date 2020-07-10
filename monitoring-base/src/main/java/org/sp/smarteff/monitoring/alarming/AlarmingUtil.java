package org.sp.smarteff.monitoring.alarming;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.sensors.PowerSensor;
import org.ogema.model.sensors.Sensor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extensionservice.SmartEffTimeSeries;

import de.iwes.util.resource.ResourceHelper;
import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.monitoring.AlarmConfigBase;

public class AlarmingUtil {
	public static final String ACTIVESTATUS_RES_NAME = "activeStatus";
	
	public static AlarmConfigBase getAlarmConfig(ResourceList<AlarmConfigBase> configs, SmartEffTimeSeries dev) {
		for(AlarmConfigBase ac: configs.getAllElements()) {
			if(ac.supervisedTS().equalsLocation(dev)) {
				return ac;
			}
		}
		return null;
	}
	public static AlarmConfigBase getAlarmConfig(ResourceList<AlarmConfigBase> configs, Sensor dev) {
		for(AlarmConfigBase ac: configs.getAllElements()) {
			if(ac.supervisedSensor().equalsLocation(dev)) {
				return ac;
			}
		}
		return null;
	}
	
	public static void cleanUpAlarmConfigs(ResourceList<AlarmConfigBase> configs) {
		for(AlarmConfigBase ac: configs.getAllElements()) {
			if((!ac.supervisedSensor().exists())&&(!ac.supervisedTS().exists()))
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
	
	public static Resource getSupervised(AlarmConfigBase ac) {
		if(ac.supervisedTS().exists()) return ac.supervisedTS();
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
}
