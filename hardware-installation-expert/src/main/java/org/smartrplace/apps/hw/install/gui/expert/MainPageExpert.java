package org.smartrplace.apps.hw.install.gui.expert;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP.ComType;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP.SetpointData;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.timeseries.eval.simple.mon3.std.StandardEvalAccess;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.config.InstallAppDeviceBase;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import de.iwes.util.collectionother.IPNetworkHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.WidgetStyle;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.filedownload.FileDownload;
import de.iwes.widgets.html.filedownload.FileDownloadData;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;

@SuppressWarnings("serial")
public class MainPageExpert extends MainPage {

	private static final String LOG_ALL = "Log All";
	private static final String LOG_NONE = "Log None";
	private static final String DELETE = "Delete";
	private static final String RESET = "Reset";
	private static final String TRASH = "Mark as Trash";
	private static final String TRASH_RESET = "Back from Trash";
	private static final String MAKE_TEMPLATE = "Make Template";
	private static final String APPLY_TEMPLATE = "Apply Template To Devices";
	private static final String APPLY_DEFAULT_ALARM = "Apply Default Alarm Settings";
	private static final List<String> ACTIONS = Arrays.asList(new String[] {LOG_ALL, LOG_NONE, DELETE, RESET, TRASH, MAKE_TEMPLATE, APPLY_DEFAULT_ALARM});
	private static final List<String> ACTIONS_TEMPLATE = Arrays.asList(new String[] {LOG_ALL, LOG_NONE, DELETE, RESET, TRASH, APPLY_TEMPLATE, APPLY_DEFAULT_ALARM});
	private static final List<String> ACTIONS_TRASH = Arrays.asList(new String[] {LOG_ALL, LOG_NONE, DELETE, RESET, TRASH_RESET, MAKE_TEMPLATE, APPLY_DEFAULT_ALARM});
	private static final String GAPS_LABEL = "Qual4d_perTs_Qual28d";
	private static final String SETPREACT_LABEL = "SetpReact_perTs";

	protected final boolean isTrashPage;
	protected final ShowModeHw showMode;
	
	@Override
	protected boolean showOnlyBaseColsHWT() {
		if(showMode == ShowModeHw.STANDARD)
			return false;
		return true;
	}
	
	@Override
	public String getHeader() {
		if(showMode==ShowModeHw.NETWORK)
			return "Device Setup and Configuration Expert (Network)";
		return "Device Setup and Configuration Expert "+((showMode==ShowModeHw.STANDARD)?"(Locations)":"(Known Issues/Gaps)");
	}

	public MainPageExpert(WidgetPage<?> page, final HardwareInstallController controller, ShowModeHw showModeHw) {
		this(page, controller, false, showModeHw);
	}
	public MainPageExpert(WidgetPage<?> page, final HardwareInstallController controller, boolean isTrashPage,
			ShowModeHw showModeHw) {
		super(page, controller, false);
		StandardEvalAccess.init(controller.appManPlus);
		this.isTrashPage = isTrashPage;
		this.showMode = showModeHw;
		if(!isTrashPage)
			finishConstructor();
		
		if(showModeHw == ShowModeHw.NETWORK) {
			String link = System.getProperty("org.smartrplace.apps.hw.install.gui.expert.wikipagelink");
			if(link != null) {
				RedirectButton wikiPage = new RedirectButton(page, "wikiPageBut", "Wiki IP Debugging",
						link);
				topTable.setContent(0, 4, wikiPage);
			}
		} else {
			Button updateDatapoints = new Button(page, "updateDatapoints", "Update Datapoints") {
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					for(InstallAppDevice iad: controller.appConfigData.knownDevices().getAllElements()) {
						DeviceHandlerProvider<?> tableProvider = controller.getDeviceHandler(iad);
						if(tableProvider != null)
							controller.updateDatapoints(tableProvider, iad);
					}
				}
			};
			topTable.setContent(0, 4, updateDatapoints);
		}
		
		//StaticTable bottomTable = new StaticTable(1, 4);
		//bottomTable.setContent(0, 0, exportCSV);
		//page.append(bottomTable);
	}
	
	@Override
	protected void finishConstructor() {
		StaticTable secondTable = new StaticTable(1, 6);
		RedirectButton stdCharts = new RedirectButton(page, "stdCharts", "Chart Config", "/org/sp/app/srcmonexpert/rssioverview.html");
		RedirectButton homeScreen = new RedirectButton(page, "homeScreen", "Other Apps", "/org/smartrplace/apps/apps-overview/index.html") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String user = GUIUtilHelper.getUserLoggedIn(req);
				if(user.equals("master"))
					setUrl("/ogema/index.html", req);
			}
			
		};
		RedirectButton alarming = new RedirectButton(page, "alarming", "Alarming Configuration", "/org/smartrplace/alarmingconfig/templateconfig.html") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String user = GUIUtilHelper.getUserLoggedIn(req);
				if(user.equals("master"))
					setUrl("/org/smartrplace/alarmingsuper/index.html", req);
			}
		};
		RedirectButton alarmingExpert = new RedirectButton(page, "alarmExpert", "Alarming Expert", "/org/smartrplace/alarmingexpert/deviceknownfaults.html");
		
		boolean isIndex = page.getUrl().startsWith("index");
		RedirectButton otherMainPageBut = new RedirectButton(page, "otherMainPageBut",
				(showMode==ShowModeHw.STANDARD)?"KnI/Gap Eval":"Locations",
				isIndex?"/org/smartrplace/hardwareinstall/expert/mainExpert2.html":"/org/smartrplace/hardwareinstall/expert/index.html");

		final FileDownload download;
	    download = new FileDownload(page, "downloadcsv", appMan.getWebAccessManager(), true);
	    download.triggerAction(download, TriggeringAction.GET_REQUEST, FileDownloadData.STARTDOWNLOAD);
	    page.append(download);
		Button exportCSV = new Button(page, "exportCSV", "Export CSV") {
			/*@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				String fileStr = controller.csvExport.exportToFile();
				if(fileStr != null)
					alert.showAlert("Exported to "+fileStr, true, req);
			}*/

			@Override
	    	public void onPrePOST(String data, OgemaHttpRequest req) {
	    		download.setDeleteFileAfterDownload(true, req);
				String fileStr = controller.csvExport.exportToFile();
	    		File csvFile = new File(fileStr);
				download.setFile(csvFile, "devices.csv", req);
	    	}
		};
		exportCSV.triggerAction(download, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);  // GET then triggers download start
		exportCSV.triggerOnPOST(alert);
		
		secondTable.setContent(0, 0, stdCharts).setContent(0, 1, homeScreen).setContent(0, 2, alarming).setContent(0, 3, alarmingExpert)
		.setContent(0, 4, exportCSV).setContent(0, 5, otherMainPageBut);
		if(showMode == ShowModeHw.NETWORK) {
			Label scanForIp = new Label(page, "scanForIp") {
				@Override
				public void onGET(OgemaHttpRequest req) {
					String localIp = IPNetworkHelper.getNonVPNAddress();
					String netw24 = localIp.substring(0, localIp.lastIndexOf('.')+1);
					String ipText = String.format("sudo nmap -sn %s1/24 |grep -B 2 XX:XX", netw24);
					setText(ipText, req);					
				}
			};
			secondTable.setContent(0, 4, scanForIp);
		}
		page.append(secondTable);
		super.finishConstructor();
	}
	
	@Override
	protected DefaultScheduleViewerConfigurationProviderExtended getScheduleViewerExtended() {
		return ScheduleViewerConfigProvHWI.getInstance();
	}
	
	@Override
	public void addWidgetsExpert(DeviceHandlerProvider<?> tableProvider, final InstallAppDevice object,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		if(tableProvider != null)
			tableProvider.addMoreWidgetsExpert(object, vh, id, req, row, appMan);
		
		if(showMode == ShowModeHw.STANDARD) {
			vh.stringLabel("IAD", id, object.getName(), row);
			vh.stringLabel("ResLoc", id, object.device().getLocation(), row);
		} else if(showMode == ShowModeHw.NETWORK) {
			//vh.stringLabel("IAD", id, object.getName(), row);
			//vh.stringLabel("ResLoc", id, object.device().getLocation(), row);
			vh.stringEdit("Identifier_IP_VPN_MAC_Sub_Bridge", id, object.networkIdentifier(), row, alert);
			vh.stringEdit("MAC_Address_If_Possible", id, object.macAddress(), row, alert);
			vh.stringEdit("Last_IP_Address_Optional", id, object.lastAddress(), row, alert);
		} else if(req == null) {
			vh.registerHeaderEntry("KniStatus");
			vh.registerHeaderEntry(GAPS_LABEL);
			if(tableProvider != null) {
				List<SetpointData> setp = tableProvider.getSetpointData(object);
				if(setp != null)
					vh.registerHeaderEntry(SETPREACT_LABEL);					
			}
		} else {
			KniStatus kniStat = getStatus(object);
			Label kniLabel = vh.stringLabel("KniStatus", id, kniStat.text, row);
			if(kniStat.style != null)
				kniLabel.addStyle(kniStat.style, req);
			float[] gapData = StandardEvalAccess.getQualityValuesPerDeviceStandard(object, appMan, controller.appManPlus);
			String gapText;
			if(gapData[0] == -1) {
				gapText = " ---";
				gapData[0] = 999;
			} else
				gapText = String.format("%.1f", gapData[0]*100)+" / "+String.format("%.1f", gapData[1]*100);
			if(gapData[2] != -1) {
				gapText += " / " + String.format("%.1f", gapData[2]*100)+" / "+String.format("%.1f", gapData[3]*100);				
			}
			if(tableProvider != null) {
				List<SetpointData> setp = tableProvider.getSetpointData(object);
				if(setp != null && (!setp.isEmpty())) {
					float[] setpReactData = StandardEvalAccess.getSetpReactValuesPerDeviceStandard(object, appMan, controller.appManPlus);
					if(!Float.isNaN(setpReactData[0])) {
						String setpRText = String.format("%.1f", setpReactData[0]*100)+" / "+String.format("%.1f", setpReactData[1]*100);
						Label setpRLabel = vh.stringLabel(SETPREACT_LABEL, id, setpRText, row);
						if(setpReactData[0] < 0.9f)
							setpRLabel.addStyle(LabelData.BOOTSTRAP_RED, req);
					}
				}
			}
			Label gapLabel = vh.stringLabel(GAPS_LABEL, id, gapText, row);
			if(gapData[0] < 0.9f)
				gapLabel.addStyle(LabelData.BOOTSTRAP_RED, req);
		}
		if(req == null) {
			if(showMode == ShowModeHw.NETWORK) {
				vh.registerHeaderEntry("Plot");
			} else {
				vh.registerHeaderEntry(DATAPOINT_INFO_HEADER);
				vh.registerHeaderEntry("Plot");
				vh.registerHeaderEntry("Action");
				vh.registerHeaderEntry("Perform");
			}
			return;
		}

		DeviceHandlerProvider<?> devHandForTrash = null;
		if(isTrashPage)
			devHandForTrash = controller.getDeviceHandlerForTrash(object);
		
		final GetPlotButtonResult logResult = getPlotButton(id, object, controller, true, vh, row, req, devHandForTrash);
		if(logResult.devHand != null) {
			row.addCell("Plot", logResult.plotButton);
			if(showMode == ShowModeHw.NETWORK)
				return;
			
			final boolean isTemplate = DeviceTableRaw.isTemplate(object, logResult.devHand);
			final TemplateDropdown<String> actionDrop = new TemplateDropdown<String>(vh.getParent(), "actionDrop"+id, req) {
				@Override
				public void onGET(OgemaHttpRequest req) {
					if(isTrashPage)
						update(ACTIONS_TRASH, req);
					else if(isTemplate) {
						update(ACTIONS_TEMPLATE, req);
					} else {
						update(ACTIONS, req);
					}
				}
			};
			actionDrop.setDefaultItems(ACTIONS);
			row.addCell("Action", actionDrop);
			ButtonConfirm performButton = new ButtonConfirm(vh.getParent(), WidgetHelper.getValidWidgetId("delBut"+id), req) {
				@Override
				public void onGET(OgemaHttpRequest req) {
					String sel = actionDrop.getSelectedItem(req);
					switch(sel) {
					case LOG_ALL:
						setConfirmMsg("Really log all datapoints for "+object.device().getLocation()+" ?", req);
						break;
					case LOG_NONE:
						setConfirmMsg("Really log no datapoints for "+object.device().getLocation()+" ?", req);
						break;
					case DELETE:
						setConfirmMsg("Really delete "+object.device().getLocation()+" ?", req);
						break;
					case RESET:
						setConfirmMsg("Really delete installation&setup configuration for "+object.device().getLocation()+
								" ? Search for new devices to recreate clean configuration.", req);
						break;
					case TRASH:
					case TRASH_RESET:
						setConfirmMsg(getTrashConfirmation(object), req);
						break;
					case MAKE_TEMPLATE:
						setConfirmMsg("Really move template status to "+object.device().getLocation()+" ?", req);
						break;
					case APPLY_TEMPLATE:
						setConfirmMsg("Really apply settings of "+object.device().getLocation()+
								" to all devices of the same type? Note that all settings will be overwritten without further confirmation!", req);
						break;
					case APPLY_DEFAULT_ALARM:
						setConfirmMsg("Really overwrite settings of " + object.device().getLocation() +
									"with default alarming settings?", req);
						break;
					}
					setText(sel, req);
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					String sel = getText(req);
					switch(sel) {
					case LOG_ALL:
						controller.activateLogging(logResult.devHand, object, false, false);
						break;
					case LOG_NONE:
						controller.activateLogging(logResult.devHand, object, false, true);
						break;
					case DELETE:
						object.device().getLocationResource().delete();
						object.delete();
						break;
					case RESET:
						object.delete();
						break;
					case TRASH_RESET:
					case TRASH:
						performTrashOperation(object, logResult.devHand);
						break;
					case MAKE_TEMPLATE:
						InstallAppDevice currentTemplate = controller.getTemplateDevice(logResult.devHand);
						if(currentTemplate != null)
							DeviceTableRaw.setTemplateStatus(currentTemplate, null, false);
							//currentTemplate.isTemplate().deactivate(false);
						DeviceTableRaw.setTemplateStatus(object, logResult.devHand, true);
						//ValueResourceHelper.setCreate(object.isTemplate(), logResult.devHand.id());
						//if(!object.isTemplate().isActive())
						//	object.isTemplate().activate(false);
						break;
					case APPLY_TEMPLATE:
						for(InstallAppDevice dev: controller.getDevices(logResult.devHand)) {
							if(dev.equalsLocation(object))
								continue;
							AlarmingConfigUtil.copySettings(object, dev, controller.appMan);
						}
						break;
					case APPLY_DEFAULT_ALARM:
						DeviceHandlerProvider<?> tableProvider = controller.getDeviceHandler(object);
						if(tableProvider != null)
							tableProvider.initAlarmingForDevice(object, controller.appConfigData);
						break;
					}
				}
			};
			row.addCell("Perform", performButton);
			actionDrop.registerDependentWidget(performButton, req);
			
		}
		
	}
	
	protected String getTrashConfirmation(InstallAppDevice object) {
		return "Really mark "+object.device().getLocation()+" as trash?";
	}

	public void performTrashOperation(InstallAppDevice object, final DeviceHandlerProvider<?> devHand) {
		//deactivate logging
		if(devHand != null)
			controller.activateLogging(devHand, object, false, true);
		//remove all alarming
		for(AlarmConfiguration alarm: object.alarms().getAllElements()) {
			IntegerResource status = AlarmingConfigUtil.getAlarmStatus(alarm.sensorVal(), false);
			if(status != null && status.exists())
				status.delete();
			alarm.delete();
		}
		object.device().getLocationResource().deactivate(true);
		ValueResourceHelper.setCreate(object.isTrash(), true);		
	}
	
	public static class KniStatus {
		public KniStatus(WidgetStyle<Label> style, String text) {
			this.style = style;
			this.text = text;
		}
		WidgetStyle<Label> style;
		String text;
	}
	public static KniStatus getStatus(InstallAppDeviceBase iad) {
		AlarmGroupData kni = iad.knownFault();
		if(!kni.exists())
			return new KniStatus(LabelData.BOOTSTRAP_GREEN, "No Open");
		int kniVal = kni.assigned().getValue();
		switch(kniVal) {
		case 0:
			return new KniStatus(LabelData.BOOTSTRAP_RED, "Unassigned");
		case AlarmingConfigUtil.ASSIGNMENT_BATTERYLOW:
			return new KniStatus(LabelData.BOOTSTRAP_ORANGE, "Battery");
		case AlarmingConfigUtil.ASSIGNMENT_DEVICE_NOT_REACHEABLE:
			return new KniStatus(LabelData.BOOTSTRAP_LIGHT_BLUE, "NoReach");
		case AlarmingConfigUtil.ASSIGNMENT_SIGNALSTRENGTH:
			return new KniStatus(LabelData.BOOTSTRAP_BLUE, "Signal Strength");
		case AlarmingConfigUtil.ASSIGNMENT_OPERATION:
			return new KniStatus(LabelData.BOOTSTRAP_BLUE, "Operation Other");
		case AlarmingConfigUtil.ASSIGNMENT_OPERATRION_EXTERNAL:
			return new KniStatus(LabelData.BOOTSTRAP_BLUE, "Operation (Ext)");
		case AlarmingConfigUtil.ASSIGNMENT_CUSTOMER:
			return new KniStatus(LabelData.BOOTSTRAP_LIGHT_BLUE, "Customer");
		case AlarmingConfigUtil.ASSIGNMENT_DEVELOPMENT:
			return new KniStatus(null, "Development");
		case AlarmingConfigUtil.ASSIGNMENT_DEVELOPMENT_EXTERNAL:
			return new KniStatus(LabelData.BOOTSTRAP_GREY, "Dev (Ext)");
		case AlarmingConfigUtil.ASSIGNMENT_SPECIALSETS:
			return new KniStatus(LabelData.BOOTSTRAP_GREY, "Special");
		case AlarmingConfigUtil.ASSIGNMENT_OTHER:
		case AlarmingConfigUtil.ASSIGNMENT_BACKLOG:
		case AlarmingConfigUtil.ASSIGNMENT_DEPDENDENT:
			return new KniStatus(LabelData.BOOTSTRAP_GREY, "Other");
		default:
			throw new IllegalStateException("Unknown KniStatus:"+kniVal+" for "+kni.getLocation());
		}
	}
	
	@Override
	public List<InstallAppDevice> getDevicesSelected(DeviceHandlerProvider<?> devHand, OgemaHttpRequest req) {
		if(showMode != ShowModeHw.NETWORK)
			return super.getDevicesSelected(devHand, req);
		if(devHand.getComType() != ComType.IP)
			return Collections.emptyList();
		return super.getDevicesSelected(devHand, req);
	}

}
