package org.smartrplace.groupalarm.std;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.api.AlarmOngoingGroup;
import org.ogema.devicefinder.api.AlarmingExtension;
import org.ogema.devicefinder.api.AlarmingExtensionListener;
import org.ogema.devicefinder.api.AlarmingGroupType;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmingData;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Class providing standard grouping of alarms
 * TODO: Initially we create groups in addition to the base alarms, but no message sending is suppressed and no
 * base alarms are consumed here. This shall allow for testing and development of this functionality before
 * base alarm consumption is activated. This also allows to generate groups quickly and not suppress device groups
 * when an entire communication system fails.
 * @author dnestle
 *
 */
public class StandardGroupAlarms implements AlarmingExtension {
	protected final ApplicationManagerPlus appManPlus;
	protected final DatapointService dpService;
	
	@Override
	public String id() {
		return this.getClass().getName();
	}

	@Override
	public String label(OgemaLocale locale) {
		return "Standard Group Alarm Issues";
	}

	/** Note that the constructor is only called on system startup. Operations that shall be performed on every restart
	 * of alarming need to be implemented in #onStartAlarming()
	 * @param appMan
	 */
	public StandardGroupAlarms(ApplicationManagerPlus appManPlus) {
		this.appManPlus = appManPlus;
		this.dpService = appManPlus.dpService();
		
		//clean up
		AlarmingData ad = ResourceHelper.getOrCreateTopLevelResource(AlarmingData.class, appManPlus.appMan());
		List<AlarmGroupData> all = new ArrayList<>(ad.ongoingGroups().getAllElements());
		for(AlarmGroupData agd: all) {
			if(!agd.ongoingAlarmStartTime().exists())
				agd.delete();
		}
	}

	@Override
	public void onStartAlarming(BaseAlarmI baseAlarm) {
		
	}
	
	@Override
	public boolean offerInGeneralAlarmingConfiguration(AlarmConfiguration ac) {
		return false;
	}

	@Override
	public AlarmingExtensionListener getListener(SingleValueResource res, AlarmConfiguration ac) {
		throw new IllegalStateException("Grouping-only AlarmingExtension!");
	}

	@Override
	public boolean getAlarmNotifications() {
		return true;
	}
	
	protected final Map<String, AlarmOngoingGroupMulti> multiGroups = new HashMap<>();
	
	@Override
	public AlarmNotificationResult newAlarmNotification(BaseAlarm baseAlarm) {
		AlarmNotificationResult result = new AlarmNotificationResult();
		result.sendAlarmDirectly = true;
		
		//For now we just process no-value alarms
		//if(!baseAlarm.isNoValueAlarm)
		//	return result;
		
		InstallAppDevice device1 = null;
		Datapoint dp = dpService.getDataPointAsIs(baseAlarm.ac.sensorVal());
		long now = appManPlus.getFrameworkTime();
		if(dp != null) {
			device1 = ResourceHelper.getFirstParentOfType(baseAlarm.ac, InstallAppDevice.class);
		}
		final InstallAppDevice device = device1;
		if(device == null) {
			return result;			
		}
		// We only add a single group per datapoint and do not change the name/id for now, so we always use DevNoValue
		getOrAddAlarmToGroup("DevNoValue", device, now, null, baseAlarm, baseAlarm.ac.getLocation(), null, 2);
		return result;
	}
	
	/*private AlarmingGroupType getType(String commType) {
		switch(commType) {
		case "Homematic":
			return AlarmingGroupType.HomematicFailed;
		case "wMBus":
			return AlarmingGroupType.wMBusFailed;
		case "MQTT":
			return AlarmingGroupType.MQTTFailed;
		case"LAN-IP":
			return AlarmingGroupType.LANFailed;
		default:
			throw new IllegalStateException("Unknown commType "+commType);
		}
	}

	protected AlarmOngoingGroup getOrAddSingleGroup(BaseAlarm ba, String subType, long now) {
		String id = ba.ac.getLocation()+"_"+subType;
		AlarmOngoingGroupSingle result = (AlarmOngoingGroupSingle) dpService.alarming().getOngoingGroup(id);
		if(result == null && !ba.isRelease) {
			result = new AlarmOngoingGroupSingle(ba.ac, subType, now, AlarmingGroupType.SingleSensorFailed, appManPlus);
			dpService.alarming().registerOngoingAlarmGroup(result);
		} else if(ba.isRelease) {
			result.setFinished();
		}
		return result;
	}*/
	
	/** The type shall indicate the alarm type
	 * 
	 * @param type
	 * @param device null if communication type based
	 * @param now
	 * @param groupType
	 * @param usually null, but if group is created elsewhere it should be provided here
	 * @return
	 */
	protected AlarmOngoingGroup getOrAddAlarmToGroup(String type, InstallAppDevice device, long now,
			AlarmingGroupType groupType, BaseAlarm ba, String subGroupId,
			AlarmOngoingGroupMulti result, int minimumElementsToRegister) {
		String id;
		if(device != null)
			id = device.getLocation()+"_"+type;
		else
			id = type;
		boolean isRegistered;
		if(result == null) {
			result = multiGroups.get(id);
		}
		if(result == null) {
			result = (AlarmOngoingGroupMulti) dpService.alarming().getOngoingGroup(id);
			isRegistered = result != null;
		} else
			isRegistered = dpService.alarming().getOngoingGroup(id) != null;
		if(result == null) {
			result = new AlarmOngoingGroupMulti(id, now,
					device!=null?AlarmingGroupType.DeviceFailed:groupType, appManPlus, 10000l, ba.isRelease) {
				
				@Override
				protected void sendMessage() {
					//TODO: not implemented yet e.g. resending, escalation, release
					//ba.sendMessage("Group received no values:"+device.deviceId().getValue(), "No more values received:"+device.deviceId().getValue()+
					//		": Last value received at:"+StringFormatHelper.getTimeDateInLocalTimeZone(startTime), MessagePriority.LOW); 
				}
			};
		}
		if(ba.isRelease) {
			result.removeAlarm(ba.ac);
			if(subGroupId != null)
				result.removeSubGroup(subGroupId);
		} else {
			result.addAlarm(ba.ac);
			if(subGroupId != null)
				result.addSubGroup(subGroupId);
		}

		if(!isRegistered) {
			dpService.alarming().registerOngoingAlarmGroup(result);			
		} else if(!multiGroups.containsKey(id))
			multiGroups.put(id, result);
		return result;
	}

}
