package org.smartrplace.apps.hw.install.deviceeval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.AppID;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.ogema.devicefinder.api.TimedJobProvider;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.timeseries.eval.simple.mon3.std.BatteryEvalBase3;
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

public class BatteryEval extends BatteryEvalBase3 {
	
	public static TimedJobMemoryData initWeeklyEmail(final ApplicationManagerPlus  appManPlus) {
		AppID appId= appManPlus.appMan().getAppID();
		appManPlus.getMessagingService().registerMessagingApp(appId, "WeeklyStatusEmail");
		
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
				sendWeeklyEmail(appManPlus);							
			}
			
			@Override
			public int evalJobType() {
				return 0;
			}
		};
		return appManPlus.dpService().timedJobService().registerTimedJobProvider(tprov);
		
	}

	public static final int COLUMN_WIDTH_1 = 30;
	public static final int COLUMN_WIDTH = 10;
	public static void sendWeeklyEmail(ApplicationManagerPlus appMan) {
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
			if(iad.isTrash().getValue())
				continue;
			BatteryStatusResult status = getFullBatteryStatus(iad, now, appMan.dpService());
			if(status.status == BatteryStatus.NO_BATTERY)
				continue;
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
		
		String baseUrl = ResourceHelper.getLocalGwInfo(appMan.appMan()).gatewayBaseUrl().getValue();
		String mes = buildMessageHTML(emptyNum, warnNum, changeNum, unknownNum, dnRNum, sigstrengthNum,
				unassignedNum, evalResults, dNRResults, sigStrengthResults, baseUrl, unknownNum);
		
		reallySendMessage("Weekly Battery Evaluation Report", mes, MessagePriority.MEDIUM, appMan);
	}
	
	protected static String buildMessageHTML(int emptyNum, int warnNum, int changeNum, int unknownNum,
			int dnRNum, int sigstrengthNum, int unassignedNum,
			List<BatteryStatusResult> evalResults, List<InstallAppDevice> dNRResults, List<InstallAppDevice> sigStrengthResults,
			String baseUrl, long now) {
		String mes = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\r\n";
		mes += "<html xmlns=\"http://www.w3.org/1999/xhtml\">\r\n";
		mes += "  <head>\r\n";
		mes += "      <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\r\n";
		mes += "      <title>smartrplace.de</title>\r\n";
		mes += "      <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\r\n";
		mes += "      <link href=\"https://fonts.googleapis.com/css2?family=Roboto&display=swap\" rel=\"stylesheet\">\r\n";
		mes += "   </head>\r\n";
		mes += "   <body style=\"font-size: 1.2em; font-family: 'Roboto', sans-serif; color: black; margin: 0; padding: 0; background-color: white;\">\r\n";
		mes += " <table style=\"padding: 10px; background-color: #9bc0b5; min-width: 100%; color: white;\">\r\n";
		mes += " <tr style=\"vertical-align: top; font-size: 1.3em;\"><td>Weekly Gateway Status Report</td></tr>\r\n";
		mes += "  <tr style=\"font-size: 0.9em\"><td>"+TimeUtils.getDateAndTimeString(now)+"</td></tr>\r\n";
		mes += " </table>\r\n";

		mes += "<table style=\"margin-top: 10px; margin-bottom: 10px; padding: 5px\">\r\n";

		mes += getOverviewTableLine("Number of empty batteries: "+emptyNum);
		mes += getOverviewTableLine("Number of batteries that need to be changed soon or urgently: "+warnNum);
		mes += getOverviewTableLine("Number of batteries that shall be changed with next visit: "+changeNum);
		mes += getOverviewTableLine("Number of batteries without status: "+unknownNum);
		mes += getOverviewTableLine("Number of devices without contact (physical check required): "+dnRNum);
		mes += getOverviewTableLine("Number of devices that require repeater:"+sigstrengthNum);
		mes += getOverviewTableLine("Number of devices UNASSIGNED: "+unassignedNum);
		String link = baseUrl+"/org/smartrplace/hardwareinstall/expert/index.html";
		mes += getOverviewTableLine("Details: <a href=\""+link+"\">Device Setup and Configuration</a>");

		mes += " </table>\r\n";
		mes += " <table style=\"padding: 10px; background-color: #9bc0b5; min-width: 100%; color: white;\">\r\n";
		mes += " <tr style=\"vertical-align: top; font-size: 1.3em;\"><td>Battery exchanges required:</td></tr>\r\n";
		mes += " </table>\r\n";
		
		mes += "<table style=\"margin-top: 10px; margin-bottom: 10px; padding: 5px\">\r\n";

		BackgroundProvider headerProv = new BackgroundProvider() {
			
			@Override
			public String getBackground(String val, int index) {
				return "#9bc0b5"; //"green";
			}
		};
		
		mes +=getValueLineHTML(headerProv, "Device", "Room", "Status", "Exp.Empty", "Voltage");
		for(final BatteryStatusResult status: evalResults) {
			if(status.status == BatteryStatus.OK)
				continue;
			BackgroundProvider batProv = new BackgroundProvider() {
				
				@Override
				public String getBackground(String val, int index) {
					return getBatteryHTMLColor(status.status);
				}
			};
			mes += getValueLineHTML(batProv, status.iad.deviceId().getValue(),
					getRoomName(status.iad),
					""+status.status,
					status.expectedEmptyDate!=null?StringFormatHelper.getDateInLocalTimeZone(status.expectedEmptyDate):"???",
					String.format("%.1f", status.currentVoltage));
		}

		mes += " </table>\r\n";
		
		mes += " <table style=\"padding: 10px; background-color: #9bc0b5; min-width: 100%; color: white;\">\r\n";
		mes += " <tr style=\"vertical-align: top; font-size: 1.3em;\"><td>Physical checks required:</td></tr>\r\n";
		mes += " </table>\r\n";
		mes += "<table style=\"margin-top: 10px; margin-bottom: 10px; padding: 5px\">\r\n";
		
		mes += getValueLineHTML(headerProv, "Device", "Room", "Status", "Since");
		for(InstallAppDevice iad: dNRResults) {
			String status = AlarmingConfigUtil.assignedText(iad.knownFault().assigned().getValue());
			mes += getValueLineHTML(null, iad.deviceId().getValue(), getRoomName(iad), status,
					StringFormatHelper.getDateInLocalTimeZone(iad.knownFault().ongoingAlarmStartTime().getValue()));
		}

		mes += " </table>\r\n";

		mes += " <table style=\"padding: 10px; background-color: #9bc0b5; min-width: 100%; color: white;\">\r\n";
		mes += " <tr style=\"vertical-align: top; font-size: 1.3em;\"><td>Devices that require repeater:</td></tr>\r\n";
		mes += " </table>\r\n";
		mes += "<table style=\"margin-top: 10px; margin-bottom: 10px; padding: 5px\">\r\n";

		mes += getValueLineHTML(headerProv, "Device", "Room", "Status", "Since");
		for(InstallAppDevice iad: sigStrengthResults) {
			String status = AlarmingConfigUtil.assignedText(iad.knownFault().assigned().getValue());
			mes += getValueLineHTML(null, iad.deviceId().getValue(), getRoomName(iad), status,
					StringFormatHelper.getDateInLocalTimeZone(iad.knownFault().ongoingAlarmStartTime().getValue()));
		}

		mes += " </table>\r\n";

		mes += "</body>\r\n";
		mes += "</html>\r\n";

		return mes;
	}
	private static String getOverviewTableLine(String text) {
		return " <tr><td style=\"padding: 5px\">"+text+"</td></tr>\r\n";
	}

	protected static String buildMessageV1(int emptyNum, int warnNum, int changeNum, int unknownNum,
			int dnRNum, int sigstrengthNum, int unassignedNum,
			List<BatteryStatusResult> evalResults, List<InstallAppDevice> dNRResults, List<InstallAppDevice> sigStrengthResults,
			String baseUrl, long now) {
		String mes = "Time of message creation: "+TimeUtils.getDateAndTimeString(now)+"\r\n";
		mes += "Number of empty batteries:"+emptyNum +"\r\n";
		mes += "Number of batteries that need to be changed soon or urgently:"+warnNum +"\r\n";
		mes += "Number of batteries that shall be changed with next visit:"+changeNum +"\r\n";
		mes += "Number of batteries without status:"+unknownNum+"\r\n";
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
					""+status.status,
					status.expectedEmptyDate!=null?StringFormatHelper.getDateInLocalTimeZone(status.expectedEmptyDate):"???",
					String.format("%.1f", status.currentVoltage));
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
		String link = baseUrl+"/org/smartrplace/hardwareinstall/expert/index.html";
		mes += "Details: "+link+" \r\n";
		
		return mes;
	}

	public static String getValueLine(String name, Room room, String... vals) {
		String result = getLeftAlignedString(name, COLUMN_WIDTH_1)+" | " +
				getLeftAlignedString(room!=null?ResourceUtils.getHumanReadableShortName(room):"NRI", COLUMN_WIDTH_1)+" | ";
		for(String val: vals) {
			result += getRightAlignedString(val, COLUMN_WIDTH)+" | ";			
		}
		result += "\r\n";
		return result;
	}

	public static interface BackgroundProvider {
		String getBackground(String val, int index);
	}
	public static String getValueLineHTML(BackgroundProvider prov, String... vals) {
		String result = " <tr>";
		int index = 0;
		for(String val: vals) {
			String background = (prov != null)?prov.getBackground(val, index):null;
			if(background != null)
				result += "<td style=\"padding 5px; background-color: "+background+";\">"+val+"</td>";			
			else
				result += "<td style=\"padding 5px;\">"+val+"</td>";
			index++;
		}
		result += "</tr>\r\n";
		return result;
	}
	
	public static String getRoomName(InstallAppDevice iad) {
		Room room = ResourceUtils.getDeviceLocationRoom(iad.device().getLocationResource());
		return room!=null?ResourceUtils.getHumanReadableShortName(room):"NRI";
	}
}
