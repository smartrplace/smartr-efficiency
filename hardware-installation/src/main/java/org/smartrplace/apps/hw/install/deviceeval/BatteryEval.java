package org.smartrplace.apps.hw.install.deviceeval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.AppID;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.ogema.devicefinder.api.TimedJobProvider;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.DoorWindowSensor;
import org.ogema.timeseries.eval.simple.mon3.std.BatteryEvalBase3;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.TimeUtils;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.tissue.util.resource.GatewayUtil;

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

	public static TimedJobMemoryData initMonthlyBatteryEmail(final ApplicationManagerPlus  appManPlus) {
		AppID appId= appManPlus.appMan().getAppID();
		//appManPlus.getMessagingService().registerMessagingApp(appId, "MonthlyBatteryEmail");
		
		TimedJobProvider tprov = new TimedJobProvider() {
			
			@Override
			public String label(OgemaLocale locale) {
				return "Send Monthly Battery Status Email";
			}
			
			@Override
			public String id() {
				return "MonthlyBatteryEmail";
			}
			
			@Override
			public boolean initConfigResource(TimedJobConfig config) {
				ValueResourceHelper.setCreate(config.alignedInterval(), AbsoluteTiming.MONTH);
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
				sendBatteryEmail(appManPlus);							
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
		AppID appId = appMan.appMan().getAppID();
		sendWeeklyEmail(appMan, appId, "Weekly Battery Evaluation Report ", MessagePriority.MEDIUM, false);
	}
	public static void sendBatteryEmail(ApplicationManagerPlus appMan) {
		AppID appId = appMan.appMan().getAppID();
		sendWeeklyEmail(appMan, appId, "Batteriestatus - monatlicher Status ", MessagePriority.MEDIUM, true);
	}
	private static class DeviceEvalData {
		long nextEmptyDate = Long.MAX_VALUE;
		int emptyNum = 0;
		int warnNum = 0;
		int changeNum = 0;
		int unknownNum = 0;
	}
	public static void sendWeeklyEmail(ApplicationManagerPlus appMan, AppID appId,
			String titleWithoutGw, MessagePriority prio,
			boolean sendBatteryBaseReport) {
		long now = appMan.getFrameworkTime();
		
		Collection<InstallAppDevice> thermostats = appMan.dpService().managedDeviceResoures(Thermostat.class);
		List<BatteryStatusResult> evalResults = new ArrayList<>();
		List<InstallAppDevice> dNRResults = new ArrayList<>();
		List<InstallAppDevice> sigStrengthResults = new ArrayList<>();
		DeviceEvalData ded = new DeviceEvalData();
		int dnRNum = 0;
		int sigstrengthNum = 0;
		int unassignedNum = 0;
		
		addResultsForDeviceType(thermostats, now, evalResults, ded, appMan.dpService());
		if(Boolean.getBoolean("org.smartrplace.apps.hw.install.deviceeval.usewindowsensors")) {
			Collection<InstallAppDevice> windowsens = appMan.dpService().managedDeviceResoures(DoorWindowSensor.class);
			addResultsForDeviceType(windowsens, now, evalResults, ded, appMan.dpService());			
		}
		
		Collection<InstallAppDevice> allDev = appMan.dpService().managedDeviceResoures(null);
		for(InstallAppDevice iad: allDev) {
			if(iad.isTrash().getValue())
				continue;
			if(!iad.knownFault().isActive())
				continue;
			int stat = iad.knownFault().assigned().getValue();
			if(stat == AlarmingConfigUtil.ASSIGNMENT_DEVICE_NOT_REACHEABLE) {
				dNRResults.add(iad);
				dnRNum++;
			} else if(stat == AlarmingConfigUtil.ASSIGNMENT_SIGNALSTRENGTH) {
				sigstrengthNum++;
				sigStrengthResults.add(iad);
			} else if(stat == 0) {
				unassignedNum++;
			}
		}
		
		String baseUrl = ResourceHelper.getLocalGwInfo(appMan.appMan()).gatewayBaseUrl().getValue();
		String mes = buildMessageHTML(ded.emptyNum, ded.warnNum, ded.changeNum, ded.unknownNum, dnRNum, sigstrengthNum,
				unassignedNum, evalResults, dNRResults, sigStrengthResults, baseUrl, now,
				!sendBatteryBaseReport);
		String gwId = GatewayUtil.getGatewayId(appMan.getResourceAccess());
		String gwName = GatewayUtil.getGatewayNameFromURL(appMan.appMan()); //baseUrl;
		
		if(sendBatteryBaseReport)
			reallySendMessage(titleWithoutGw+gwName, mes, prio, appMan, appId);
		else
			reallySendMessage(gwId+": "+titleWithoutGw+gwName, mes, prio, appMan, appId);
	}
	
	private static void addResultsForDeviceType(Collection<InstallAppDevice> thermostats, long now,
			List<BatteryStatusResult> evalResults,
			DeviceEvalData ded, DatapointService dpService) {
		for(InstallAppDevice iad: thermostats) {
			if(iad.isTrash().getValue())
				continue;
			BatteryStatusResult status = getFullBatteryStatus(iad, now, dpService);
			if(status.status == BatteryStatus.NO_BATTERY)
				continue;
			evalResults.add(status);
			if(status.expectedEmptyDate != null && status.expectedEmptyDate < ded.nextEmptyDate)
				ded.nextEmptyDate = status.expectedEmptyDate;
			if(status.status == BatteryStatus.EMPTY)
				ded.emptyNum++;
			else if(status.status == BatteryStatus.URGENT || status.status == BatteryStatus.WARNING)
				ded.warnNum++;
			else if(status.status == BatteryStatus.CHANGE_RECOMMENDED)
				ded.changeNum++;
			else if(!(status.status == BatteryStatus.OK))
				ded.unknownNum++;
		}		
	}
	
	/** Build customer format HTML message
	 * 
	 * @param emptyNum
	 * @param warnNum
	 * @param changeNum
	 * @param unknownNum
	 * @param dnRNum
	 * @param sigstrengthNum
	 * @param unassignedNum
	 * @param evalResults
	 * @param dNRResults
	 * @param sigStrengthResults
	 * @param baseUrl
	 * @param now
	 * @param fullVersion if true a version is generated suitable only for service personal. Otherwise just the battery exchange report
	 * 		is generated.
	 * @return
	 */
	protected static String buildMessageHTML(int emptyNum, int warnNum, int changeNum, int unknownNum,
			int dnRNum, int sigstrengthNum, int unassignedNum,
			List<BatteryStatusResult> evalResults, List<InstallAppDevice> dNRResults, List<InstallAppDevice> sigStrengthResults,
			String baseUrl, long now,
			boolean fullVersion) {
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
		mes += " <tr style=\"vertical-align: top; font-size: 1.3em;\"><td>"+
				(fullVersion?"Weekly Gateway Status Report":"Batteriestatus - Report")+
				"</td></tr>\r\n";
		mes += "  <tr style=\"font-size: 0.9em\"><td>"+TimeUtils.getDateAndTimeString(now)+"</td></tr>\r\n";
		mes += " </table>\r\n";

		mes += "<table style=\"margin-top: 10px; margin-bottom: 10px; padding: 5px\">\r\n";

		if(fullVersion) {
			mes += getOverviewTableLine("Number of empty batteries: "+emptyNum);
			mes += getOverviewTableLine("Number of batteries that need to be changed soon or urgently: "+warnNum);
			mes += getOverviewTableLine("Number of batteries that shall be changed with next visit: "+changeNum);
			mes += getOverviewTableLine("Number of batteries without status: "+unknownNum);
			mes += getOverviewTableLine("Number of devices without contact (physical check required): "+dnRNum);
			mes += getOverviewTableLine("Number of devices that require repeater:"+sigstrengthNum);
			mes += getOverviewTableLine("Number of devices UNASSIGNED: "+unassignedNum);
			String link = baseUrl+"/org/smartrplace/hardwareinstall/expert/index.html";
			mes += getOverviewTableLine("Details: <a href=\""+link+"\">Device Setup and Configuration</a>");
			link = baseUrl+"/org/smartrplace/alarmingexpert/deviceknownfaults.html";
			mes += getOverviewTableLine("Issues : <a href=\""+link+"\">Known Issues</a>");
		} else {
			mes += getOverviewTableLine("Anzahl leerer Batterien: "+emptyNum);
			mes += getOverviewTableLine("Anzahl Batterien die kurzfristig gewechselt werden sollten: "+warnNum);
			mes += getOverviewTableLine("Anzahl der Batterien, für die zur Sicherheit bereits ein Wechsel empfohlen wird: "+changeNum);
			mes += getOverviewTableLine("Anzahl der Batterien, für die zur Sicherheit bereits ein Wechsel empfohlen wird: "+changeNum);
			mes += getOverviewTableLine(" ");
			String link = baseUrl+"/org/smartrplace/hardwareinstall/batteriescust.html";
			mes += getOverviewTableLine("Nach dem Wechsel können Sie die aktuelle Batteriespannung hier prüfen: <a href=\""+link+"\">Battery Status Overview</a>");			
		}
		mes += " </table>\r\n";
		mes += " <table style=\"padding: 10px; background-color: #9bc0b5; min-width: 100%; color: white;\">\r\n";
		mes += " <tr style=\"vertical-align: top; font-size: 1.3em;\"><td>"+
				(fullVersion?"Battery exchanges required:":"Erforderliche und empfohlene Batteriewechsel")+"</td></tr>\r\n";
		mes += " </table>\r\n";
		
		mes += "<table style=\"margin-top: 10px; margin-bottom: 10px; padding: 5px\">\r\n";

		BackgroundProvider headerProv = new BackgroundProvider() {
			
			@Override
			public String getBackground(String val, int index) {
				return "#9bc0b5"; //"green";
			}
		};
		
		if(fullVersion)
			mes +=getValueLineHTML(headerProv, "Device", "Room", "Status", "Exp.Empty", "Voltage");
		else
			mes +=getValueLineHTML(headerProv, "Gerät", "Raum", "Status", "Schätzung Deadline", "Spannung (V)");
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
					status.expectedEmptyDate!=null?(StringFormatHelper.getDateInLocalTimeZone(status.expectedEmptyDate)):"???", //+"("+status.expectedEmptyDate+")"
					String.format("%.1f", status.currentVoltage));
		}

		mes += " </table>\r\n";
		
		if(!fullVersion) {
			mes += "</body>\r\n";
			mes += "</html>\r\n";
			return mes;
		}
		
		mes += " <table style=\"padding: 10px; background-color: #9bc0b5; min-width: 100%; color: white;\">\r\n";
		mes += " <tr style=\"vertical-align: top; font-size: 1.3em;\"><td>Physical checks required:</td></tr>\r\n";
		mes += " </table>\r\n";
		mes += "<table style=\"margin-top: 10px; margin-bottom: 10px; padding: 5px\">\r\n";
		
		mes += getValueLineHTML(headerProv, "Device", "Room", "Status", "Since");
		for(InstallAppDevice iad: dNRResults) {
			if(iad.isTrash().getValue())
				continue;
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
