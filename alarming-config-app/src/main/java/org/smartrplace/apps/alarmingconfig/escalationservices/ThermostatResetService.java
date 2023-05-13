package org.smartrplace.apps.alarmingconfig.escalationservices;

import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.AppID;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.devices.buildingtechnology.ElectricLight;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.model.sensors.CO2Sensor;
import org.ogema.model.sensors.DoorWindowSensor;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationLevel;
import org.smartrplace.alarming.escalation.util.EscalationKnownIssue;
import org.smartrplace.alarming.escalation.util.EscalationProviderSimple;
import org.smartrplace.apps.alarmconfig.util.AlarmMessageUtil;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.tissue.util.resource.GatewayUtil;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class ThermostatResetService extends EscalationProviderSimple<EscalationKnownIssue> {
	private final ApplicationManagerPlus appManPlus;
	private final String baseUrl;
	private final HardwareInstallConfig hwInstall;
	private final LocalGatewayInformation gwRes;
	
	public ThermostatResetService(ApplicationManagerPlus appManPlus, HardwareInstallConfig hwInstall) {
		this.appManPlus = appManPlus;
		this.gwRes = ResourceHelper.getLocalGwInfo(appManPlus.appMan());
		this.baseUrl = gwRes.gatewayBaseUrl().getValue();
		this.hwInstall = hwInstall;
	}

	@Override
	public void initConfig(AlarmingEscalationLevel levelRes) {
		ValueResourceHelper.setCreate(levelRes.timedJobData().alignedInterval(), AbsoluteTiming.DAY);
		ValueResourceHelper.setCreate(levelRes.timedJobData().interval(), 400);
		ValueResourceHelper.setCreate(levelRes.standardDelay(), 1*TimeProcUtil.HOUR_MILLIS);
	}

	@Override
	public String label(OgemaLocale locale) {
		return "ThermostatPlus Daily Reset";
	}

	@Override
	protected EscalationKnownIssue isEscalationRelevant(InstallAppDevice iad) {
		if(iad.device() instanceof Thermostat)
			return new EscalationKnownIssue();
		if(iad.device() instanceof DoorWindowSensor)
			return new EscalationKnownIssue();
		if(iad.device() instanceof CO2Sensor)
			return new EscalationKnownIssue();
		if(iad.device() instanceof ElectricLight)
			return new EscalationKnownIssue();
		if(iad.devHandlerInfo().getValue().endsWith("TemperatureOrHumiditySensorDeviceHandler"))
			return new EscalationKnownIssue();			
		if(iad.devHandlerInfo().getValue().endsWith("FaultSingleDeviceHandler"))
			return new EscalationKnownIssue();
		if(iad.devHandlerInfo().getValue().endsWith("FaultSingleDeviceIntegerHandler"))
			return new EscalationKnownIssue();
		return null;
	}
	
	@Override
	protected EscalationCheckResult checkEscalation(Collection<EscalationKnownIssue> issues, List<AppID> appIDs, long now) {
		int maxFault = 0;
		String emailMessage = getMessageHeaderLinks(baseUrl, gwRes);
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
			if(alarms[0] == 0) {
				//Release
				issue.knownIssue.delete();
				continue;
			}
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
		if(maxFault > 0 && hwInstall.alarmingReductionLevel().getValue() >= 0) {
			sendDeviceSpecificMessage(emailMessage, countDevice, maxFault, "thermostats/roomcontrols (morning)",
					appIDs, persistData, appManPlus);
		}
		return new EscalationCheckResult();
	}
	
	public static String getMessageForKnownIssues(List<EscalationKnownIssue> issues, String baseUrl,
			String title_afterDeviceNum,
			List<AppID> appIDs, 
			AlarmingEscalationLevel persistData,
			ApplicationManagerPlus appManPlus) {
		return sendMessageForKnownIssues(issues, baseUrl, null, title_afterDeviceNum, appIDs, persistData, appManPlus);
	}
	public static String sendMessageForKnownIssues(List<EscalationKnownIssue> issues, String baseUrl, LocalGatewayInformation gwRes,
			String title_afterDeviceNum,
			List<AppID> appIDs, 
			AlarmingEscalationLevel persistData,
			ApplicationManagerPlus appManPlus) {
		return sendMessageForKnownIssues(issues, baseUrl, gwRes, title_afterDeviceNum, appIDs, persistData, appManPlus, false);
	}
	public static String sendMessageForKnownIssues(List<EscalationKnownIssue> issues, String baseUrl, LocalGatewayInformation gwRes,
			String title_afterDeviceNum,
			List<AppID> appIDs, 
			AlarmingEscalationLevel persistData,
			ApplicationManagerPlus appManPlus,
			boolean addAlarmDocumentationLink) {
		String emailMessage = getMessageHeaderLinks(baseUrl, gwRes);
		for(EscalationKnownIssue issue: issues) {
			String message = issue.knownIssue.lastMessage().getValue();
			if(emailMessage == null) {
				//We put a return upfront as initial line will be filled with "Notification :" by EmailService, which disturbs when reading through the messages
				emailMessage = "\r\n"+message;
			} else
				emailMessage += "\r\n\r\n"+message;
			if(addAlarmDocumentationLink) {
				String link = AlarmMessageUtil.getAlarmGuideLink(message);
				if(link != null)
					emailMessage += "\r\n"+"Further information: "+link;
			}
		}
		int countDevice = issues.size();
		
		String gwId = GatewayUtil.getGatewayId(appManPlus.getResourceAccess());
		String title = gwId+"::"+countDevice+" "+title_afterDeviceNum+"!";
		String firebaseMessage = countDevice + " " + title_afterDeviceNum;
		String firebaseDebugInfoMessage= "Sending Unassigned warning message:"+title_afterDeviceNum;
		sendEscalationMessage(title, emailMessage, firebaseMessage,
				firebaseDebugInfoMessage,
				appIDs, persistData, appManPlus);
		return emailMessage;
		
	}
}
