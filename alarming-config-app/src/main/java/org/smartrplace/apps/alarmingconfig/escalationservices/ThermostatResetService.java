package org.smartrplace.apps.alarmingconfig.escalationservices;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.AppID;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationLevel;
import org.smartrplace.alarming.escalation.model.AlarmingMessagingApp;
import org.smartrplace.apps.alarmingconfig.mgmt.AlarmValueListenerBasic;
import org.smartrplace.apps.alarmingconfig.mgmt.EscalationKnownIssue;
import org.smartrplace.apps.alarmingconfig.mgmt.EscalationProviderSimple;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.tissue.util.resource.GatewayUtil;
import org.smartrplace.util.message.FirebaseUtil;
import org.smartrplace.util.message.MessageImpl;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.messaging.MessagePriority;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class ThermostatResetService extends EscalationProviderSimple<EscalationKnownIssue> {
	protected final ApplicationManagerPlus appManPlus;
	
	public ThermostatResetService(ApplicationManagerPlus appManPlus) {
		this.appManPlus = appManPlus;
	}

	@Override
	public void initConfig(AlarmingEscalationLevel levelRes) {
		ValueResourceHelper.setCreate(levelRes.timedJobData().alignedInterval(), AbsoluteTiming.DAY);
		ValueResourceHelper.setCreate(levelRes.timedJobData().interval(), 400);
		ValueResourceHelper.setCreate(levelRes.standardDelay(), 1*TimeProcUtil.HOUR_MILLIS);
	}

	@Override
	public String label(OgemaLocale locale) {
		return "Thermostat Daily Reset";
	}

	@Override
	protected EscalationKnownIssue isEscalationRelevant(InstallAppDevice iad) {
		if(iad.device() instanceof Thermostat)
			return new EscalationKnownIssue();
		return null;
	}

	@Override
	protected EscalationCheckResult checkEscalation(Collection<EscalationKnownIssue> issues, List<AppID> appIDs, long now) {
		int maxFault = 0;
		String emailMessage = null;
		int countDevice = 0;
		for(EscalationKnownIssue issue: issues) {
			if(issue.knownIssue.assigned().getValue() > 0)
				continue;
			long duration = issue.knownIssue.ongoingAlarmStartTime().getValue() - now;
			if(duration < persistData.standardDelay().getValue())
				continue;
			int[] alarms = AlarmingConfigUtil.getActiveAlarms(issue.device);
			if(alarms[0] == 0) {
				//Release
				issue.knownIssue.delete();
			} else {
				if(alarms[0] > maxFault) {
					maxFault = alarms[0];
				}
				countDevice++;
				if(emailMessage == null) {
					//We put a return upfront as initial line will be filled with "Notification :" by EmailService, which disturbs when reading through the messages
					emailMessage = "\r\n"+issue.knownIssue.lastMessage().getValue();
				} else
					emailMessage += "\r\n\r\n"+issue.knownIssue.lastMessage().getValue();
			}
		}
		if(maxFault > 0) {
			MessagePriority prio = AlarmValueListenerBasic.getMessagePrio(persistData.alarmLevel().getValue());
			String gwId = GatewayUtil.getGatewayId(appManPlus.getResourceAccess());
			String title = gwId+"::"+countDevice+" thermostats still with open issues("+maxFault+")!";
			for(AppID appId: appIDs) {
				appManPlus.guiService().getMessagingService().sendMessage(appId,
						new MessageImpl(title, emailMessage, prio));		
				//AlarmingManager.reallySendMessage(title, emailMessage , prio, appId);
			}
			String roomId = ResourceUtils.getValidResourceName(appManPlus.getResourceAccess().getResources(Room.class).get(0).getLocation());
			Map<String, Object> additionalProperties = new HashMap<>();
			String message = countDevice + " unassigned thermostats, max active alarms: "+maxFault;
			for(AlarmingMessagingApp mapp: persistData.messagingApps().getAllElements()) {
				FirebaseUtil.sendMessageToUsers(title,
						message, title, message,
						additionalProperties, Arrays.asList(mapp.usersForPushMessage().getValues()),
						roomId, appManPlus,
						"Sending Unassigned thermostat morning warning message:");
			}
		}
		return new EscalationCheckResult();
	}
}