package org.smartrplace.groupalarm.std;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.AlarmOngoingGroup;
import org.ogema.devicefinder.api.AlarmingExtension;
import org.ogema.devicefinder.api.AlarmingGroupType;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmingData;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** TODO: For the groups no message resending, no escalation, no update for additional alarms
 * will appear
 * TODO: Maybe this will remain a draft initially
 */
public abstract class AlarmOngoingGroupMulti implements AlarmOngoingGroup {//extends AlarmOngoingGroupSingle {
	protected final String subType;
	protected final long startTime;
	protected final AlarmingGroupType groupType;
	
	protected final DatapointService dpService;
	protected final ApplicationManager appMan;

	protected AlarmGroupData resource = null;
	
	protected boolean isFinished = false;
	
	public void setFinished() {
		isFinished = true;
	}

	protected final Set<AlarmConfiguration> acs = new HashSet<>();
	protected CountDownDelayedExecutionTimer sendTimer = null;
	protected Set<String> subGroups = new HashSet<>();
	
	protected InstallAppDevice device = null;
	
	/** Overwrite if appclicable*/
	@Override
	public AlarmingExtension sourceOfGroup() {
		return null;
	}
	
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
			Long sendMessageRetard, boolean isRelease) {
		//super(null, subType, startTime, groupType, appManPlus);
		this.subType = subType;
		this.startTime = startTime;
		this.groupType = groupType;
		this.dpService = appManPlus.dpService();
		this.appMan = appManPlus.appMan();
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
		
		if(isRelease)
			return;
		AlarmGroupData perData = getResource(true);
		if(ValueResourceHelper.setIfNew(perData.ongoingAlarmStartTime(), startTime))
			perData.ongoingAlarmStartTime().activate(false);
		ValueResourceHelper.setCreate(perData.minimumTimeBetweenAlarms(), -1);
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
	public AlarmGroupData getResource(boolean forceToCreate) {
		if(resource == null) {
			if(!forceToCreate)
				return null;
			if(device != null) {
				resource = device.knownFault().create();
				resource.activate(true);
			} else {
				AlarmingData ad = ResourceHelper.getOrCreateTopLevelResource(AlarmingData.class, appMan);
				resource = ResourceListHelper.getOrCreateNamedElementFlex(id(), ad.ongoingGroups());
			}
		}
		return resource;
	}

	@Override
	public AlarmingGroupType getType() {
		return groupType;
	}

	@Override
	public boolean isFinished() {
		return isFinished;
	}
	
	@Deprecated //for compatibility
	public int getSubGroupsFound() {
		return 0;
	}
}
