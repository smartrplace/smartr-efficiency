package org.smatrplace.apps.hw.install.gui.mainexpert;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.devicefinder.api.DatapointInfo;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP.ComType;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP.SetpointData;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.timeseries.eval.simple.mon3.std.StandardEvalAccess;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.config.InstallAppDeviceBase;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.external.accessadmin.config.SubCustomerData;
import org.smartrplace.hwinstall.basetable.HardwareTablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.util.virtualdevice.ChartsUtil;
import org.smartrplace.util.virtualdevice.ChartsUtil.GetPlotButtonResult;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import de.iwes.util.collectionother.IPNetworkHelper;
import de.iwes.util.performanceeval.ExecutionTimeLogger;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
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
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.resource.widget.dropdown.ValueResourceDropdown;

@SuppressWarnings("serial")
public class MainPageExpert extends MainPage {
	
	private static final String LOG_ALL = "Log All";
	private static final String LOG_NONE = "Log None";
	private static final String DELETE = "Delete";
	private static final String RESET = "Reset";
	private static final String TRASH = "Mark as Trash";
	private static final String TRASH2DELETE = "Set Trash for Delete";
	private static final String TRASH_RESET = "Back from Trash";
	private static final String MAKE_TEMPLATE = "Make Template";
	private static final String APPLY_TEMPLATE = "Apply Template To Devices";
	private static final String APPLY_DEFAULT_ALARM = "Apply Default Alarm Settings";
	public static final List<String> ACTIONS = Arrays.asList(new String[] {LOG_ALL, LOG_NONE, DELETE, TRASH2DELETE, RESET, TRASH, MAKE_TEMPLATE, APPLY_DEFAULT_ALARM});
	private static final List<String> ACTIONS_TEMPLATE = Arrays.asList(new String[] {LOG_ALL, LOG_NONE, DELETE, TRASH2DELETE, RESET, TRASH, APPLY_TEMPLATE, APPLY_DEFAULT_ALARM});
	public static final List<String> ACTIONS_TRASH = Arrays.asList(new String[] {LOG_ALL, LOG_NONE, DELETE, RESET, TRASH_RESET, MAKE_TEMPLATE, APPLY_DEFAULT_ALARM});
	private static final String GAPS_LABEL = "Qual4d_perTs_Qual28d";
	private static final String SETPREACT_LABEL = "SetpReact_perTs";
	private static final Map<String, String> utilityOptions = new HashMap<>();
	public static String defaultActionAfterReload = LOG_ALL;
	public static String defaultTrashActionAfterReload = LOG_ALL;

	/** See also {@link DatapointInfo#getUtilityType()}*/
	static {
		utilityOptions.put("", "--");
		utilityOptions.put("electricity", "Electricity");
		utilityOptions.put("gas", "Gas");
		utilityOptions.put("oil", "Oil");
		utilityOptions.put("heatEnergy", "Heat Energy");
		utilityOptions.put("waterCold", "Cold Water (or temperature unknown)");
		utilityOptions.put("waterWarm", "Warm water");
	}
	
	protected final boolean isTrashPage;
	protected final ShowModeHw showMode;
	
	protected ExecutionTimeLogger etl = null;
	
	@Override
	protected boolean showOnlyBaseColsHWT() {
		if(showMode == ShowModeHw.STANDARD)
			return false;
		return true;
	}
	
	@Override
	public String getHeader() {
		if(showMode == ShowModeHw.API_DATA)
			return "Device Setup and Configuration Expert (API-Data)";
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
			
			TextField startIdEdit = new TextField(page, "startIdEdit") {
				@Override
				public void onGET(OgemaHttpRequest req) {
					setValue(idRangeStart==null?"":""+idRangeStart, req);
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					HardwareTablePage.idRangeLastEdit = appMan.getFrameworkTime();
					if(data.isEmpty()) {
						HardwareTablePage.idRangeStart = null;
						return;
					}
					try {
						String sval = getValue(req);
						int val = Integer.parseInt(sval);
						HardwareTablePage.idRangeStart = val;
					} catch(NumberFormatException e) {
						HardwareTablePage.idRangeStart = null;
					}
				}
			};
			startIdEdit.registerDependentWidget(startIdEdit);
			
			TextField endIdEdit = new TextField(page, "endIdEdit") {
				@Override
				public void onGET(OgemaHttpRequest req) {
					setValue(idRangeEnd==null?"":""+idRangeEnd, req);
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					HardwareTablePage.idRangeLastEdit = appMan.getFrameworkTime();
					if(data.isEmpty()) {
						HardwareTablePage.idRangeEnd = null;
						return;
					}
					try {
						String sval = getValue(req);
						int val = Integer.parseInt(sval);
						HardwareTablePage.idRangeEnd = val;
					} catch(NumberFormatException e) {
						HardwareTablePage.idRangeEnd = null;
					}
				}
			};
			endIdEdit.registerDependentWidget(endIdEdit);

			TextField maxNoChangeEdit = new TextField(page, "maxNoChangeEdit") {
				@Override
				public void onGET(OgemaHttpRequest req) {
					setValue(idRangeMaxNoLimit==null?"":""+idRangeMaxNoLimit, req);
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					HardwareTablePage.idRangeLastEdit = appMan.getFrameworkTime();
					if(data.isEmpty()) {
						HardwareTablePage.idRangeMaxNoLimit = null;
						return;
					}
					try {
						String sval = getValue(req);
						int val = Integer.parseInt(sval);
						HardwareTablePage.idRangeMaxNoLimit = val;
					} catch(NumberFormatException e) {
						HardwareTablePage.idRangeMaxNoLimit = null;
					}
				}
			};
			maxNoChangeEdit.registerDependentWidget(maxNoChangeEdit);

			topTable.setContent(0, 0, "Range of DeviceIds to be shown:");
			topTable.setContent(0, 1, startIdEdit);
			topTable.setContent(0, 2, endIdEdit);
			topTable.setContent(0, 3, "Maximum number devices NOT limted:");
			topTable.setContent(0, 5, maxNoChangeEdit);
		} else if(!isTrashPage) {
			Button updateDatapoints = new Button(page, "updateDatapoints", "Update Datapoints") {
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					for(InstallAppDevice iad: controller.appConfigData.knownDevices().getAllElements()) {
						DeviceHandlerProviderDP<?> tableProvider = controller.getDeviceHandler(iad);
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
	    		List<InstallAppDevice> shown = new ArrayList<>();
	    		
	     		List<DeviceHandlerProviderDP<?>> types = controller.dpService.getDeviceHandlerProviders();
	    		for(DeviceHandlerProviderDP<?> type: types) {
		     		shown.addAll(MainPage.getDevicesSelectedDefault((DeviceHandlerProvider<?>) type, controller, roomsDrop, typeFilterDrop, req));	    			
	    		}
	    		controller.csvExport.setResources(shown);
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
	public void addWidgetsExpert(final DeviceHandlerProvider<?> tableProvider, final InstallAppDevice object,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			final ApplicationManager appMan) {
		if(tableProvider != null) {
			//etl().intermediateStep("Start addWidgetsExp:"+tableProvider.getTableTitle());
			tableProvider.addMoreWidgetsExpert(object, vh, id, req, row, appMan);
		} //else
		//	etl().intermediateStep("Start addWidgetsExp:"+object.getName());
		
		if(showMode == ShowModeHw.STANDARD) {
			vh.stringLabel("IAD", id, object.getName(), row);
			vh.stringLabel("ResLoc", id, object.device().getLocation(), row);
		} else if(showMode == ShowModeHw.API_DATA) {
			ValueResourceDropdown<StringResource> drop = vh.dropdown("Utility_for_API_Selection", id, object.getSubResource("deviceUtility", StringResource.class), row,
					utilityOptions, 3);
			vh.stringEdit("Application_for_API_Freetext", id, object.getSubResource("apiApplication", StringResource.class), row, alert);
			vh.stringEdit("Device_Display_Name_Freetext", id, object.getSubResource("deviceDisplayName", StringResource.class), row, alert);
			//ValueResourceDropdown<IntegerResource> dropInt = vh.dropdown("Aggregation", id, object.getSubResource("aggregationType",IntegerResource.class), row,
			//		DpUpdateAPI.aggregationOptions, 2);
			
			SubCustomerData subc = object.device().location().getSubResource("tenant", SubCustomerData.class);
			if(subc != null) {
				String tenantName = ResourceUtils.getHumanReadableShortName(subc);
				vh.stringLabel("Tenant", id, tenantName, row);
			}
			if(req != null)
				vh.stringLabel("InternalName", id, object.device().getLocationResource().getName(), row);
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
			//etl().intermediateStep("End addWidgetsExp(2.BEFKNI):"+tableProvider.getTableTitle());
			addKniStatus(object, tableProvider, vh, id, req, row, appManPlus);
		}
		if(req == null) {
			if(showMode == ShowModeHw.NETWORK || showMode == ShowModeHw.API_DATA) {
				vh.registerHeaderEntry("Plot");
			} else {
				vh.registerHeaderEntry(ChartsUtil.DATAPOINT_INFO_HEADER);
				vh.registerHeaderEntry("Plot");
				vh.registerHeaderEntry("Action");
				vh.registerHeaderEntry("Perform");
			}
			return;
		}

		DeviceHandlerProvider<?> devHandForTrash = null;
		if(isTrashPage)
			devHandForTrash = controller.getDeviceHandlerForTrash(object);
		
		//etl().intermediateStep("End addWidgetsExp(2.1):"+object.getName());
		final GetPlotButtonResult logResult = getPlotButton(id, object, controller, true, vh, row, req, devHandForTrash);
		//etl().intermediateStep("End addWidgetsExp(2.2):"+object.getName());
		if(logResult.devHand != null) {
			row.addCell("Plot", logResult.plotButton);
			if(showMode == ShowModeHw.NETWORK || showMode == ShowModeHw.API_DATA)
				return;
			
			final boolean isTemplate = DeviceTableRaw.isTemplate(object, logResult.devHand);
			final Button performButton;
			final TemplateDropdown<String> actionDrop = new TemplateDropdown<String>(vh.getParent(), "actionDrop"+id, req) {

				@Override
				public void onGET(OgemaHttpRequest req) {
					//etl().intermediateStep("ONGET ACD:"+object.getName());
					if(isTrashPage) {
						update(ACTIONS_TRASH, req);
						selectItem(defaultTrashActionAfterReload, req);
					} else if(isTemplate) {
						update(ACTIONS_TEMPLATE, req);
						selectItem(defaultActionAfterReload, req);
					} else {
						update(ACTIONS, req);
						selectItem(defaultActionAfterReload, req);
					}
				}
			};
			actionDrop.setDefaultItems(ACTIONS);
			row.addCell("Action", actionDrop);
			
			final boolean status = controller.appConfigData.disableConfirmationUntil().getValue()==0;
			if(!status) {
				performButton = new Button(vh.getParent(), WidgetHelper.getValidWidgetId("delBut"+id), req) {
					boolean init = false;
					
					@Override
					public void onGET(OgemaHttpRequest req) {
						if(!init) {
							actionDrop.onGET(req);
							init = true;
						}
						String sel = actionDrop.getSelectedItem(req);
						setText(sel, req);
					}
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						String sel = getText(req);
						performAction(sel, object, logResult.devHand);
					}
				};
			} else {
				ButtonConfirm performButton2 = new ButtonConfirm(vh.getParent(), WidgetHelper.getValidWidgetId("delBut"+id), req) {
					boolean init = false;

					@Override
					public void onGET(OgemaHttpRequest req) {
						if(!init) {
							actionDrop.onGET(req);
							init = true;
						}
						String sel = actionDrop.getSelectedItem(req);
						switch(sel) {
						case LOG_ALL:
							setConfirmMsg("Really log all datapoints for "+object.device().getLocation()+" ?", req);
							break;
						case LOG_NONE:
							setConfirmMsg("Really log no datapoints for "+object.device().getLocation()+" ?", req);
							break;
						case DELETE:
							setConfirmMsg("Really delete "+object.device().getLocation()+" and remove from CCU if applicable ?", req);
							break;
						case TRASH2DELETE:
							setConfirmMsg("Really set to trash and delete "+object.device().getLocation()+" when trash is cleared ?", req);
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
						performAction(sel, object, logResult.devHand);
					}
				};
				performButton = performButton2;
			}

			row.addCell("Perform", performButton);
			actionDrop.registerDependentWidget(performButton, req);
			
			//etl().intermediateStep("End addWidgetsExp(2F):"+object.getName());
		} //if(logResult.devHand != null)
	}
	
	protected String getTrashConfirmation(InstallAppDevice object) {
		return "Really mark "+object.device().getLocation()+" as trash?";
	}

	public void performTrashOperation(InstallAppDevice object, final DeviceHandlerProviderDP<?> devHand) {
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
		if(devHand == null) {
			if(controller.hwInstApp.gwSync != null) {
				controller.hwInstApp.gwSync.deactivateResource(object.device().getLocationResource(), true);
				System.out.println("Deactivate VIA SYNC: "+object.device().getLocation());
			} else {
				object.device().getLocationResource().deactivate(true);
				System.out.println("Deactivate LOCAL ONLY: "+object.device().getLocation());
			}
		} else 	for(Resource dev: devHand.devicesControlled(object)) {
			if(controller.hwInstApp.gwSync != null) {
				controller.hwInstApp.gwSync.deactivateResource(dev.getLocationResource(), true);
				System.out.println("Deactivate VIA SYNC: "+dev.getLocation());
			} else {
				dev.getLocationResource().deactivate(true);					
				System.out.println("Deactivate LOCAL ONLY: "+dev.getLocation());
			}
		}
		//object.device().getLocationResource().deactivate(true);
		//if(controller.hwInstApp.gwSync != null)
		//	setCreate(object.isTrash(), true, controller.hwInstApp.gwSync);
		//else
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
			return new KniStatus(LabelData.BOOTSTRAP_GREY, "ValveErr");
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
		//etl().intermediateStep("GetDevs(0):"+devHand.getTableTitle());
		if(showMode != ShowModeHw.NETWORK) {
			List<InstallAppDevice> result = super.getDevicesSelected(devHand, req);
			//etl().intermediateStep("GetDevs(1):"+devHand.getTableTitle());
			return result;
		}
		if(devHand.getComType() != ComType.IP)
			return Collections.emptyList();
		List<InstallAppDevice> result = super.getDevicesSelected(devHand, req);
		//etl().intermediateStep("GetDevs(2):"+devHand.getTableTitle());
		return result;
	}

	public static Label addKniStatus(InstallAppDevice object, DeviceHandlerProvider<?> tableProvider,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
			String id, OgemaHttpRequest req, Row row,
			ApplicationManagerPlus appManPlus) {
		KniStatus kniStat = getStatus(object);
		Label kniLabel = vh.stringLabel("KniStatus", id, kniStat.text, row);
		if(kniStat.style != null)
			kniLabel.addStyle(kniStat.style, req);
		float[] gapData = StandardEvalAccess.getQualityValuesPerDeviceStandard(object, appManPlus.appMan(), appManPlus);
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
				float[] setpReactData = StandardEvalAccess.getSetpReactValuesPerDeviceStandard(object, appManPlus.appMan(), appManPlus);
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
		return kniLabel;
	}
	
	/*protected ExecutionTimeLogger etl() {
		if(etl != null)
			return etl;
		etl = new ExecutionTimeLogger("LOAD HWINSTEXP", appMan);
		etl.logAlwaysToConsole = true; //Boolean.getBoolean("org.smatrplace.apps.hw.install.gui.mainexpert.extensivelogging");
		return etl;
	}*/
	
	private void performAction(String sel, InstallAppDevice object, DeviceHandlerProviderDP<?> devHand) {
		switch(sel) {
		case LOG_ALL:
			controller.activateLogging(devHand, object, false, false);
			break;
		case LOG_NONE:
			controller.activateLogging(devHand, object, false, true);
			break;
		case DELETE:
			DeviceTableRaw.deleteDevice(object, controller.hwInstApp.gwSync);
			break;
		case RESET:
			object.delete();
			break;
		case TRASH2DELETE:
			long dayEnd = AbsoluteTimeHelper.getNextStepTime(appMan.getFrameworkTime(), AbsoluteTiming.DAY);
			ValueResourceHelper.setCreate(controller.appConfigData.nextTimeToDeleteMarkedDevices(), dayEnd);
			ValueResourceHelper.setCreate(object.trashStatus(), 1);
		case TRASH_RESET:
		case TRASH:
			performTrashOperation(object, devHand);
			break;
		case MAKE_TEMPLATE:
			InstallAppDevice currentTemplate = controller.getTemplateDevice(devHand);
			if(currentTemplate != null)
				DeviceTableRaw.setTemplateStatus(currentTemplate, false);
				//currentTemplate.isTemplate().deactivate(false);
			DeviceTableRaw.setTemplateStatus(object, true);
			//ValueResourceHelper.setCreate(object.isTemplate(), logResult.devHand.id());
			//if(!object.isTemplate().isActive())
			//	object.isTemplate().activate(false);
			break;
		case APPLY_TEMPLATE:
			for(InstallAppDevice dev: controller.getDevices(devHand)) {
				if(dev.equalsLocation(object))
					continue;
				AlarmingConfigUtil.copySettings(object, dev, controller.appMan);
			}
			break;
		case APPLY_DEFAULT_ALARM:
			DeviceHandlerProviderDP<?> tableProvider = controller.getDeviceHandler(object);
			if(tableProvider != null)
				tableProvider.initAlarmingForDevice(object, controller.appConfigData);
			break;
		}		
	}
}
