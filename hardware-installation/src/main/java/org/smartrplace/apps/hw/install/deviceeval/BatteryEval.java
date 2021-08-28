package org.smartrplace.apps.hw.install.deviceeval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.AppID;
import org.ogema.devicefinder.api.TimedJobProvider;
import org.ogema.devicefinder.util.BatteryEvalBase;
import org.ogema.devicefinder.util.TimedJobMemoryData;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.TimeUtils;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.messaging.MessagePriority;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class BatteryEval extends BatteryEvalBase {
	
	public static TimedJobMemoryData initWeeklyEmail(final ApplicationManagerPlus  appManPlus) {
		AppID appId= appManPlus.appMan().getAppID();
		appManPlus.getMessagingService().registerMessagingApp(appId, "DailyStatusEmail");
		
		TimedJobProvider tprov = new TimedJobProvider() {
			
			@Override
			public String label(OgemaLocale locale) {
				return "Send Weekly Gateway Status Email";
			}
			
			@Override
			public String id() {
				return "WeeklyGwEvalEmail";
			}
			
			@Override
			public boolean initConfigResource(TimedJobConfig config) {
				ValueResourceHelper.setCreate(config.alignedInterval(), AbsoluteTiming.WEEK);
				ValueResourceHelper.setCreate(config.interval(), 600);
				ValueResourceHelper.setCreate(config.disable(), false);
				ValueResourceHelper.setCreate(config.performOperationOnStartUpWithDelay(), -1);
				return true;
			}
			
			@Override
			public String getInitVersion() {
				return "A";
			}
			
			@Override
			public void execute(long now, TimedJobMemoryData data) {
				sendWeelyEmail(appManPlus);							
			}
			
			@Override
			public int evalJobType() {
				return 0;
			}
		};
		return appManPlus.dpService().timedJobService().registerTimedJobProvider(tprov );
		
	}

	public static final int COLUMN_WIDTH_1 = 30;
	public static final int COLUMN_WIDTH = 10;
	public static void sendWeelyEmail(ApplicationManagerPlus appMan) {
		long now = appMan.getFrameworkTime();
		
		Collection<InstallAppDevice> thermostats = appMan.dpService().managedDeviceResoures(Thermostat.class);
		List<BatteryStatusResult> evalResults = new ArrayList<>();
		List<InstallAppDevice> dNRResults = new ArrayList<>();
		List<InstallAppDevice> sigStrengthResults = new ArrayList<>();
		long nextEmptyDate = Long.MAX_VALUE;
		int emptyNum = 0;
		int warnNum = 0;
		int changeNum = 0;
		int unknownNum = 0;
		int dnRNum = 0;
		int sigstrengthNum = 0;
		int unassignedNum = 0;
		
		for(InstallAppDevice iad: thermostats) {
			BatteryStatusResult status = getFullBatteryStatus(iad, now);
			evalResults.add(status);
			if(status.expectedEmptyDate != null && status.expectedEmptyDate < nextEmptyDate)
				nextEmptyDate = status.expectedEmptyDate;
			if(status.status == BatteryStatus.EMPTY)
				emptyNum++;
			else if(status.status == BatteryStatus.URGENT || status.status == BatteryStatus.WARNING)
				warnNum++;
			else if(status.status == BatteryStatus.CHANGE_RECOMMENDED)
				changeNum++;
			else if(!(status.status == BatteryStatus.OK))
				unknownNum++;
		}
		
		Collection<InstallAppDevice> allDev = appMan.dpService().managedDeviceResoures(null);
		for(InstallAppDevice iad: allDev) {
			if(!iad.knownFault().assigned().exists())
				continue;
			int stat = iad.knownFault().assigned().getValue();
			if(stat == 2150) {
				dNRResults.add(iad);
				dnRNum++;
			} else if(stat == 2200) {
				sigstrengthNum++;
				sigStrengthResults.add(iad);
			} else if(stat == 0) {
				unassignedNum++;
			}
		}
		
		String mes = "Time of message creation: "+TimeUtils.getDateAndTimeString(now)+"\r\n";
		mes += "Number of empty batteries:"+emptyNum +"\r\n";
		mes += "Number of batteries that need to be changed soon or urgently:"+warnNum +"\r\n";
		mes += "Number of batteries that shall be changed with next visit:"+changeNum +"\r\n";
		mes += "Number of batteries without status:"+unknownNum;
		mes += "Number of devices without contact (physical check required):"+dnRNum +"\r\n";
		mes += "Number of devices that require repeater:"+sigstrengthNum +"\r\n";
		mes += "Number of devices UNASSIGNED:"+unassignedNum +"\r\n";
		mes += "\r\n";
		mes += "Battery exchanges required:\r\n";
		mes += getLeftAlignedString("Device", COLUMN_WIDTH_1)+" | " + getLeftAlignedString("Room", COLUMN_WIDTH_1)+" | " + getRightAlignedString("Status", COLUMN_WIDTH)+" | "+ getRightAlignedString("Exp.Empty", COLUMN_WIDTH)+" | "+ getRightAlignedString("Voltage", COLUMN_WIDTH)+" | "+"\r\n";
		for(BatteryStatusResult status: evalResults) {
			if(status.status == BatteryStatus.OK)
				continue;
			mes += getValueLine(status.iad.deviceId().getValue(), ResourceUtils.getDeviceLocationRoom(status.iad.device().getLocationResource()),
					""+status.status, StringFormatHelper.getDateInLocalTimeZone(status.expectedEmptyDate), String.format("%.1f", status.currentVoltage));
		}
		mes += "\r\n";
		mes += "Physical checks required:\r\n";
		mes += getLeftAlignedString("Device", COLUMN_WIDTH_1)+" | " + getLeftAlignedString("Room", COLUMN_WIDTH_1)+" | " + getRightAlignedString("Status", COLUMN_WIDTH)+" | "+ getRightAlignedString("Since", COLUMN_WIDTH)+"\r\n";
		for(InstallAppDevice iad: dNRResults) {
			mes += getValueLine(iad.deviceId().getValue(), ResourceUtils.getDeviceLocationRoom(iad.device().getLocationResource()),
					StringFormatHelper.getDateInLocalTimeZone(iad.knownFault().ongoingAlarmStartTime().getValue()));
		}
		mes += "\r\n";
		mes += "Devices that require repeater:\r\n";
		mes += getLeftAlignedString("Device", COLUMN_WIDTH_1)+" | " + getLeftAlignedString("Room", COLUMN_WIDTH_1)+" | " + getRightAlignedString("Status", COLUMN_WIDTH)+" | "+ getRightAlignedString("Since", COLUMN_WIDTH)+"\r\n";
		for(InstallAppDevice iad: sigStrengthResults) {
			mes += getValueLine(iad.deviceId().getValue(), ResourceUtils.getDeviceLocationRoom(iad.device().getLocationResource()),
					StringFormatHelper.getDateInLocalTimeZone(iad.knownFault().ongoingAlarmStartTime().getValue()));
		}
		mes += "\r\n";
		String baseUrl = ResourceHelper.getLocalGwInfo(appMan.appMan()).gatewayBaseUrl().getValue();
		String link = baseUrl+"/org/smartrplace/hardwareinstall/expert/index.html";
		mes += "Details: "+link+" \r\n";
		reallySendMessage("Weekly Battery Evaluation Report", mes, MessagePriority.MEDIUM, appMan);
	}

	public static String getValueLine(String name, Room room, String... vals) {
		String result = getLeftAlignedString(name, COLUMN_WIDTH_1)+" | " +
				getLeftAlignedString(ResourceUtils.getHumanReadableShortName(room), COLUMN_WIDTH_1)+" | ";
		for(String val: vals) {
			result += getRightAlignedString(val, COLUMN_WIDTH)+" | ";			
		}
		return result;
	}

}
