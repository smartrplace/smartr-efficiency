package org.smartrplace.apps.alarmingconfig.escalationservices;

import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.AppID;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationLevel;
import org.smartrplace.alarming.escalation.util.EscalationKnownIssue;
import org.smartrplace.alarming.escalation.util.EscalationProviderSimple;
import org.smartrplace.alarming.escalation.util.EscalationProviderSimple.EscalationCheckResult;
import org.smartrplace.apps.alarmconfig.util.AlarmType;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gateway.device.GatewayDevice;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class GatewayEscalationProvider extends EscalationProviderSimple<EscalationKnownIssue> {

	private final ApplicationManagerPlus appManPlus;
	private final String baseUrl;
	private final LocalGatewayInformation gwRes;
	
	public GatewayEscalationProvider(ApplicationManagerPlus appManPlus) {
		this.appManPlus = appManPlus;
		this.gwRes = ResourceHelper.getLocalGwInfo(appManPlus.appMan());
		this.baseUrl = gwRes.gatewayBaseUrl().getValue();
	}

	@Override
	public String label(OgemaLocale locale) {
		return "Gateway Warning";
	}

	@Override
	public void initConfig(AlarmingEscalationLevel levelRes) {
		ValueResourceHelper.setCreate(levelRes.timedJobData().alignedInterval(), AbsoluteTiming.DAY);
		ValueResourceHelper.setCreate(levelRes.timedJobData().interval(), 360);
		ValueResourceHelper.setCreate(levelRes.standardDelay(), 15*TimeProcUtil.MINUTE_MILLIS);
	}

	@Override
	protected EscalationKnownIssue isEscalationRelevant(InstallAppDevice iad) {
		if(iad.device() instanceof GatewayDevice)
			return new EscalationKnownIssue();
		return null;
	}

	@Override
	protected EscalationCheckResult checkEscalation(Collection<EscalationKnownIssue> issues, List<AppID> appIDs, long now) {
		int maxFault = 0;
		String emailMessage = ThermostatResetService.getMessageHeaderLinks(baseUrl, gwRes);
		//if(baseUrl == null)
		//	emailMessage = null;
		//else
		//	emailMessage = "Known issues: "+baseUrl+"/org/smartrplace/alarmingexpert/deviceknownfaults.html";
		int countDevice = 0;
		for(EscalationKnownIssue issue: issues) {
			if(issue.knownIssue.assigned().getValue() > 0)
				continue;
			long duration = now - issue.knownIssue.ongoingAlarmStartTime().getValue();
			if(duration < persistData.standardDelay().getValue())
				continue;
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
			//if(Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.escalationservices.debugsource")) {
			//	emailMessage += "\r\n SOURCE DEBUG: "+issue.knownIssue.getLocation();
			//}
			String link = AlarmType.getFullLink("ac101");
			emailMessage += "\r\n"+"Further information: "+link;
			//}
		}
		if(maxFault > 0) {
			ThermostatResetService.sendDeviceSpecificMessage(emailMessage, countDevice, maxFault, "Gateway",
					appIDs, persistData, appManPlus);
		}
		return new EscalationCheckResult();
	}
}
