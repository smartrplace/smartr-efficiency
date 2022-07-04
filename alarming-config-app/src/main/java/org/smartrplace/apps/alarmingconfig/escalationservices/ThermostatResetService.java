package org.smartrplace.apps.alarmingconfig.escalationservices;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.AppID;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.devices.buildingtechnology.ElectricLight;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.CO2Sensor;
import org.ogema.model.sensors.DoorWindowSensor;
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

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.messaging.MessagePriority;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class ThermostatResetService extends EscalationProviderSimple<EscalationKnownIssue> {
	protected final ApplicationManagerPlus appManPlus;
	protected final String baseUrl;
	
	public ThermostatResetService(ApplicationManagerPlus appManPlus) {
		this.appManPlus = appManPlus;
		this.baseUrl = ResourceHelper.getLocalGwInfo(appManPlus.appMan()).gatewayBaseUrl().getValue();
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
		return null;
	}
	
	@Override
	protected EscalationCheckResult checkEscalation(Collection<EscalationKnownIssue> issues, List<AppID> appIDs, long now) {
		int maxFault = 0;
		String emailMessage;
		if(baseUrl == null)
			emailMessage = null;
		else
			emailMessage = "Known issues: "+baseUrl+"/org/smartrplace/alarmingexpert/deviceknownfaults.html";
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
		if(maxFault > 0) {
			sendDeviceSpecificMessage(emailMessage, countDevice, maxFault, "thermostats/roomcontrols (morning)",
					appIDs, persistData, appManPlus);
			/*MessagePriority prio = AlarmValueListenerBasic.getMessagePrio(persistData.alarmLevel().getValue());
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
			}*/
		}
		return new EscalationCheckResult();
	}
	
	public static String sendMessageForKnownIssues(List<EscalationKnownIssue> issues, String baseUrl,
			String title_afterDeviceNum,
			List<AppID> appIDs, 
			AlarmingEscalationLevel persistData,
			ApplicationManagerPlus appManPlus) {
		String emailMessage;
		if(baseUrl == null)
			emailMessage = null;
		else
			emailMessage = "Known issues: "+baseUrl+"/org/smartrplace/alarmingexpert/deviceknownfaults.html";
		for(EscalationKnownIssue issue: issues) {
			if(emailMessage == null) {
				//We put a return upfront as initial line will be filled with "Notification :" by EmailService, which disturbs when reading through the messages
				emailMessage = "\r\n"+issue.knownIssue.lastMessage().getValue();
			} else
				emailMessage += "\r\n\r\n"+issue.knownIssue.lastMessage().getValue();
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

	/** Send messages to all configured. This is to report unassigned messages for certain device
	 * types
	 * 
	 * @param title
	 * @param emailMessage
	 * @param firebaseMessage
	 * @param countDevice
	 * @param maxFault
	 * @param prio
	 * @param appIDs
	 * @param persistData
	 * @param appManPlus
	 * @return firebase short message
	 */
	public static String sendDeviceSpecificMessage(String emailMessage,
			int countDevice, int maxFault, String deviceTypeTypePluralString,
			List<AppID> appIDs, 
			AlarmingEscalationLevel persistData,
			ApplicationManagerPlus appManPlus) {
		String gwId = GatewayUtil.getGatewayId(appManPlus.getResourceAccess());
		String title = gwId+"::"+countDevice+" "+deviceTypeTypePluralString+" still with open issues("+maxFault+")!";
		String message = countDevice + " unassigned "+deviceTypeTypePluralString+", max active alarms: "+maxFault;
		String firebaseDebugInfoMessage = "Sending Unassigned "+deviceTypeTypePluralString+" warning message:";
		sendEscalationMessage(title, emailMessage, message,
				firebaseDebugInfoMessage,
				appIDs, persistData, appManPlus);
		return message;
	}
	
	/** Send messages to all configured. This is mainly relevant if the email message is NOT
	 * sent in the WeeklyEmail format
	 * 
	 * @param title message email and firebase message title
	 * @param emailMessage email message
	 * @param firebaseMessage short message sent via firebase if relevant. If null no
	 * 		firebase message is sent.
	 * @param countDevice number of devices unassigned (only relevant for firebase)
	 * @param maxFault
	 * @param prio
	 * @param appIDs
	 * @param persistData
	 * @param appManPlus
	 * @return
	 */
	public static void sendEscalationMessage(String title, String emailMessage,
			String firebaseMessage,
			String firebaseDebugInfoMessage,
			List<AppID> appIDs, 
			AlarmingEscalationLevel persistData,
			ApplicationManagerPlus appManPlus) {
		MessagePriority prio = AlarmValueListenerBasic.getMessagePrio(persistData.alarmLevel().getValue());
		for(AppID appId: appIDs) {
			appManPlus.guiService().getMessagingService().sendMessage(appId,
					new MessageImpl(title, emailMessage, prio));		
			//AlarmingManager.reallySendMessage(title, emailMessage , prio, appId);
		}
		String roomId = ResourceUtils.getValidResourceName(appManPlus.getResourceAccess().getResources(Room.class).get(0).getLocation());
		Map<String, Object> additionalProperties = new HashMap<>();
		if(firebaseMessage == null)
			return;
		for(AlarmingMessagingApp mapp: persistData.messagingApps().getAllElements()) {
			FirebaseUtil.sendMessageToUsers(title,
					firebaseMessage, title, firebaseMessage,
					additionalProperties, Arrays.asList(mapp.usersForPushMessage().getValues()),
					roomId, appManPlus,
					firebaseDebugInfoMessage);
		}
	}
}
