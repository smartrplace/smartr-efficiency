package org.smartrplace.groupalarm.std;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.AlarmingGroupType;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** TODO: For the groups no message resending, no escalation, no update for additional alarms
 * will appear
 * TODO: Maybe this will remain a draft initially
 */
public abstract class AlarmOngoingGroupMulti extends AlarmOngoingGroupSingle {
	protected final Set<AlarmConfiguration> acs = new HashSet<>();
	protected CountDownDelayedExecutionTimer sendTimer = null;
	protected Set<String> subGroups = new HashSet<>();
	
	protected InstallAppDevice device = null;
	
	protected abstract void sendMessage();
	
	/** TODO: These methods should handle alarm resending, alarm group removal when all elements have been
	 * released etc.
	 * @param ac
	 */
	public void addAlarm(AlarmConfiguration ac) {
		acs.add(ac);
	}
	public void removeAlarm(AlarmConfiguration ac) {
		acs.remove(ac);
	}
	
	public void addSubGroup(String id) {
		subGroups.add(id);
	}
	public void removeSubGroup(String id) {
		subGroups.remove(id);
	}
	
	public AlarmOngoingGroupMulti(String subType, long startTime, AlarmingGroupType groupType,
			ApplicationManagerPlus appManPlus,
			Long sendMessageRetard) {
		super(null, subType, startTime, groupType, appManPlus);
		if(subType.contains("/")) {
			int idx = subType.lastIndexOf('_');
			Resource res;
			if(idx >= 0)
				res = appMan.getResourceAccess().getResource(subType.substring(0, idx));
			else
				res = appMan.getResourceAccess().getResource(subType);
			if(res != null && res instanceof InstallAppDevice)
				device = (InstallAppDevice) res;
		}		
		
		if(sendMessageRetard != null) {
			sendTimer = new CountDownDelayedExecutionTimer(appMan, sendMessageRetard) {
				@Override
				public void delayedExecution() {
					sendMessage();
					sendTimer = null;
				}
			};
		}
	}

	public void close() {
		if(sendTimer != null) {
			sendTimer.destroy();
			sendTimer = null;
		}
	}
	
	/** The id shall be the same for all occurences of the same type so that the resource for the group can be
	 * easily found
	 */
	@Override
	public String id() {
		return subType;
	}

	@Override
	public String label(OgemaLocale locale) {
		if(device != null)
			return device.deviceId().getValue()+"_"+StringFormatHelper.getFullTimeDateInLocalTimeZone(startTime);
		return subType+"_"+StringFormatHelper.getFullTimeDateInLocalTimeZone(startTime);
	}

	@Override
	public Collection<AlarmConfiguration> baseAlarms() {
		return acs;
	}
	
	@Override
	public int getSubGroupsFound() {
		return subGroups.size();
	}
}
