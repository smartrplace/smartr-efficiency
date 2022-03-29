package org.smartrplace.apps.alarmingconfig.escalationservices;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.AppID;
import org.ogema.model.locations.Room;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationLevel;
import org.smartrplace.alarming.escalation.model.AlarmingMessagingApp;
import org.smartrplace.apps.alarmingconfig.mgmt.AlarmValueListenerBasic;
import org.smartrplace.apps.alarmingconfig.mgmt.EscalationKnownIssue;
import org.smartrplace.apps.alarmingconfig.mgmt.EscalationProviderSimple;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.deviceeval.BatteryEval;
import org.smartrplace.tissue.util.resource.GatewayUtil;
import org.smartrplace.util.message.FirebaseUtil;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.messaging.MessagePriority;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class NotAssignedEscalationProvider extends EscalationProviderSimple<EscalationKnownIssue> {

	protected final ApplicationManagerPlus appManPlus;
	
	public NotAssignedEscalationProvider(ApplicationManagerPlus appManPlus) {
		this.appManPlus = appManPlus;
	}

	@Override
	public String label(OgemaLocale locale) {
		return "Unassigned Issue Warning";
	}

	@Override
	public void initConfig(AlarmingEscalationLevel levelRes) {
		//ValueResourceHelper.setCreate(levelRes.timedJobData().alignedInterval(), AbsoluteTiming.DAY);
		ValueResourceHelper.setCreate(levelRes.timedJobData().interval(), 60);
		ValueResourceHelper.setCreate(levelRes.standardDelay(), 36*TimeProcUtil.HOUR_MILLIS);
	}

	@Override
	protected EscalationKnownIssue isEscalationRelevant(InstallAppDevice iad) {
		return new EscalationKnownIssue();
	}

	@Override
	protected EscalationCheckResult checkEscalation(Collection<EscalationKnownIssue> issues, List<AppID> appIDs,
			long now) {
		boolean foundTooLate = false;
		long maxUnassigned = 0;
		String maxMessage = null;
		int count = 0;
		for(EscalationKnownIssue issue: issues) {
			if(issue.knownIssue.assigned().getValue() > 0)
				continue;
			long duration = now - issue.knownIssue.ongoingAlarmStartTime().getValue();
			if(duration < persistData.standardDelay().getValue())
				continue;
			if(duration > maxUnassigned) {
				maxUnassigned = duration;
				maxMessage = issue.knownIssue.lastMessage().getValue();
			}
			count++;
			foundTooLate = true;
		}
		
		EscalationCheckResult result = new EscalationCheckResult();
		if(foundTooLate) {
			MessagePriority prio = AlarmValueListenerBasic.getMessagePrio(persistData.alarmLevel().getValue());
			String gwId = GatewayUtil.getGatewayId(appManPlus.getResourceAccess());
			String title = gwId+"::"+count+" devices unassigened for up to "+(maxUnassigned/TimeProcUtil.HOUR_MILLIS)+" h !";
			for(AppID appId: appIDs) {
				BatteryEval.sendWeeklyEmail(appManPlus, appId, title, prio);								
			}
			result.blockedUntil = now + persistData.standardDelay().getValue();
			String roomId = ResourceUtils.getValidResourceName(appManPlus.getResourceAccess().getResources(Room.class).get(0).getLocation());
			Map<String, Object> additionalProperties = new HashMap<>();
			for(AlarmingMessagingApp mapp: persistData.messagingApps().getAllElements()) {
				String message;
				if(maxMessage == null)
					message = "(no message)";
				else
					message = maxMessage.split("::")[0];
				FirebaseUtil.sendMessageToUsers(title,
						message, title, message,
						additionalProperties, Arrays.asList(mapp.usersForPushMessage().getValues()),
						roomId, appManPlus,
						"Sending Not in-time assigned warning message:");
			}
		}
		return result;
	}
}
