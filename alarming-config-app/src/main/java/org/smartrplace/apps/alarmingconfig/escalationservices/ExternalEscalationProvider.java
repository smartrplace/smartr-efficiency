package org.smartrplace.apps.alarmingconfig.escalationservices;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.AppID;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.util.timedjob.TimedJobMemoryDataImpl;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationLevel;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationSettings;
import org.smartrplace.alarming.escalation.util.EscalationKnownIssue;
import org.smartrplace.alarming.escalation.util.EscalationProviderSimple;
import org.smartrplace.alarming.escalation.util.EscalationProviderSimple.EscalationCheckResult;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.tissue.util.resource.GatewayUtil;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class ExternalEscalationProvider extends EscalationProviderSimple<EscalationKnownIssue> {

	private static final long MAX_ESCTIME_UPDATERATE = 10000;
	protected final ApplicationManagerPlus appManPlus;
	protected final String baseUrl;
	private final LocalGatewayInformation gwRes;
	private final Set<String> knownIssuesProcessed = new HashSet<>();
	private StringArrayResource knownIssuesProcessedRes;
	
	protected final String name;
		
	/** Index for identification from persistent storage. The index shall not change even
	 * if the name changes.
	 */
	public final int index;
		
	/** Overwrite this method to escalate not ALL SingleFaul 
	 * Provide information which faults shall be escalated when
	 * 
	 * @param iad device containing knownFault. Shall support null argument providing
	 * 		default escalation time.
	 * @return null if not escalated by this provider. Provide zero if escalated immediately.
	 * 	If positive value escalation will be done when still existing and not assigned after
	 *  the time specified in milliseconds (TODO: Later escalation may not be implemented
	 *  initially)
	 */
	protected Long shallReceiveMessageEscalated(InstallAppDevice iad) {
		if(iad == null)
			return 0l;
		if(!iad.externalEscalationProviderIds().isActive())
			return null;
		long[] vals = iad.externalEscalationProviderIds().getValues();
		if(vals.length <= index)
			return null;
		return vals[index];
		//if(!iad.deviceId().getValue().startsWith("org.smartrplace.driverhandler.devices.FaultSingleDeviceHandler"))
		//	return null;
		//return 0l;
	};
	
	public ExternalEscalationProvider(ApplicationManagerPlus appManPlus, String name, int index) {
		this.appManPlus = appManPlus;
		this.gwRes = ResourceHelper.getLocalGwInfo(appManPlus.appMan());
		this.baseUrl = gwRes.gatewayBaseUrl().getValue();
		this.name = name;
		this.index = index;
		
	}

	@Override
	public Boolean initProvider(AlarmingEscalationLevel persistData, AlarmingEscalationSettings settings,
			List<InstallAppDevice> knownIssueDevices) {
		Boolean result = super.initProvider(persistData, settings, knownIssueDevices);
		knownIssuesProcessedRes = persistData.getSubResource("knownIssuesProcessed", StringArrayResource.class);
		if(knownIssuesProcessedRes.isActive())
			knownIssuesProcessed.addAll(Arrays.asList(knownIssuesProcessedRes.getValues()));
		return result;
	}
	
	@Override
	public String id() {
		return this.getClass().getSimpleName()+WidgetHelper.getValidWidgetId(name);
	}
	
	@Override
	public String label(OgemaLocale locale) {
		return name+" (FaultDEsc)";
	}

	@Override
	public void initConfig(AlarmingEscalationLevel levelRes) {
		//ValueResourceHelper.setCreate(levelRes.timedJobData().alignedInterval(), AbsoluteTiming.DAY);
		String prop = System.getProperty("org.ogema.util.timedjob.shortintervalallowed");
		if(prop != null && prop.equals("ExternalEscalationProvider"))
			ValueResourceHelper.setCreate(levelRes.timedJobData().interval(), 0.5f);
		else
			ValueResourceHelper.setCreate(levelRes.timedJobData().interval(), TimedJobMemoryDataImpl.MINIMUM_MINUTES_FOR_TIMER_START+0.1f);
		Long delay = shallReceiveMessageEscalated(null);
		if(delay == null)
			ValueResourceHelper.setCreate(levelRes.standardDelay(), -1);
		else
			ValueResourceHelper.setCreate(levelRes.standardDelay(), delay);
	}

	@Override
	protected EscalationKnownIssue isEscalationRelevant(InstallAppDevice iad) {
		long now = appManPlus.getFrameworkTime();
		if(getEscalationTime(iad, now) != null)
			return new EscalationKnownIssue();
		return null;
	}

	@Override
	protected EscalationCheckResult checkEscalation(Collection<EscalationKnownIssue> issues, List<AppID> appIDs, long now) {
		int maxFault = 0;
		String emailMessage = ""; // = ThermostatResetService.getMessageHeaderLinks(baseUrl, gwRes);
		//if(baseUrl == null)
		//	emailMessage = null;
		//else
		//	emailMessage = "Known issues: "+baseUrl+"/org/smartrplace/alarmingexpert/deviceknownfaults.html";
		int countDevice = 0;
		Set<String> issueLocs = new HashSet<>();
		boolean changedKnownIssuesProcessed = false;
		synchronized(knownIssuesProcessed) {
			for(EscalationKnownIssue issue: issues) {
				String loc = issue.knownIssue.getLocation();
				issueLocs.add(loc);
				if(knownIssuesProcessed.contains(loc))
					continue;
				if(issue.knownIssue.assigned().getValue() > 0)
					continue;
				Long escTime = getEscalationTime(issue.device, now);
				if(escTime == null || (escTime < 0)) { //null should not occur
					continue;
				}
				if(escTime > 0) {
					long duration = now - issue.knownIssue.ongoingAlarmStartTime().getValue();
					if(duration < escTime)
						continue;				
				}
				int[] alarms = AlarmingConfigUtil.getActiveAlarms(issue.device);
				//For now we do not release, but we shall check what's going on
				//if(alarms[0] == 0) {
					//Release
				//	issue.knownIssue.delete();
				//} else {
				if(alarms[0] > maxFault) {
					maxFault = alarms[0];
				}
				countDevice++;
				if(emailMessage == null) {
					//We put a return upfront as initial line will be filled with "Notification :" by EmailService, which disturbs when reading through the messages
					emailMessage = "\r\n"+issue.knownIssue.lastMessage().getValue();
				} else
					emailMessage += "\r\n\r\n"+issue.knownIssue.lastMessage().getValue();
				knownIssuesProcessed.add(loc);
				changedKnownIssuesProcessed = true;
			}
			for(String loc2: knownIssuesProcessed) {
				if(!issueLocs.contains(loc2)) {
					changedKnownIssuesProcessed = true;
					knownIssuesProcessed.remove(loc2);
				}
			}
			if(changedKnownIssuesProcessed) {
				ValueResourceHelper.setCreate(knownIssuesProcessedRes, knownIssuesProcessed.toArray(new String[0]));
			}
		}
		if(countDevice > 0) {
			//ThermostatResetService.sendDeviceSpecificMessage(emailMessage, countDevice, maxFault, "New Fault Message for "+name,
			//		appIDs, persistData, appManPlus);
			String gwId = GatewayUtil.getGatewayId(appManPlus.getResourceAccess());
			String title = gwId+"::"+countDevice+" new fault message(s) for "+name;
			String message = "Detected "+countDevice + " new alarms: ";
			String firebaseDebugInfoMessage = message;
			ThermostatResetService.sendEscalationMessage(title, emailMessage, message,
					firebaseDebugInfoMessage,
					appIDs, persistData, appManPlus);

		}
		return new EscalationCheckResult();
	}
	
	/** Cached value*/
	private class DeviceStatusCache {
		Long escalationTime;
		long lastUpdate;
	}
	/** IAD-Location -> data*/
	private final Map<String, DeviceStatusCache> escalationTimeCache = new HashMap<>();
	private Long getEscalationTime(InstallAppDevice iad, long now) {
		DeviceStatusCache data = escalationTimeCache.get(iad.getLocation());
		if(data == null || (now - data.lastUpdate > MAX_ESCTIME_UPDATERATE)) {
			if(data == null)
				data = new DeviceStatusCache();
			data.escalationTime = shallReceiveMessageEscalated(iad);
			data.lastUpdate = now;
		}
		return data.escalationTime;
	}
	

}
