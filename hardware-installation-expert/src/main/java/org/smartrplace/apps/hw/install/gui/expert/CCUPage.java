package org.smartrplace.apps.hw.install.gui.expert;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.PercentageResource;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmInterfaceInfo;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmLogicInterface;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.alarming.escalation.util.EscalationAutoActionByAlarmingProvider;
import org.smartrplace.alarming.escalation.util.EscalationKnownIssue;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.LocalDeviceId;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.eval.hardware.HmCCUPageUtils;
import org.smartrplace.os.util.BundleRestartButton;
import org.smartrplace.os.util.OSGiBundleUtil.BundleType;
import org.smartrplace.router.model.GlitNetRouter;
import org.smartrplace.tissue.util.logconfig.PerformanceLog.GwSubResProvider;
import org.smartrplace.tissue.util.resource.GatewaySyncResourceService.RemoteStatus;
import org.smartrplace.tissue.util.resource.GatewaySyncUtil;
import org.smartrplace.tissue.util.resource.GatewayUtil;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.util.virtualdevice.ChartsUtil;
import org.smartrplace.util.virtualdevice.ChartsUtil.GetPlotButtonResult;
import org.smartrplace.util.virtualdevice.HmSetpCtrlManagerTHSetp;

import de.iwes.util.logconfig.CountdownTimerMulti2Single;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.popup.Popup;

@SuppressWarnings("serial")
public class CCUPage extends MainPage {

	private static final int CCU_MAX_FOR_ALL = 50;
	DeviceTableBase devTable;
	HardwareInstallConfig hwConfig;
	private final CountdownTimerMulti2Single hmDriverRestartTimer;
	
	private final Popup lastMessagePopup;
	private final Label lastMessageDevice;
	private final Label consoleCommand;
	private final Label ccuUrl;

	@Override
	public String getHeader() {return "CCU Page";}

	static Boolean isAllAllowed = null;
	@Override
	protected boolean isAllOptionAllowedSuper(OgemaHttpRequest req) {
		if(isAllAllowed == null) {
			int thermNum = appMan.getResourceAccess().getResources(HmInterfaceInfo.class).size();
			isAllAllowed = (thermNum <= CCU_MAX_FOR_ALL);
		}
		return isAllAllowed;
	}
	
	public CCUPage(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller, false);
		hwConfig = appMan.getResourceAccess().getResource("hardwareInstallConfig");
		hmDriverRestartTimer = new CountdownTimerMulti2Single(controller.appMan, 7*TimeProcUtil.MINUTE_MILLIS) {
			
			@Override
			public void delayedExecution() {
				BundleRestartButton.hmRestart.executeNonBlockingOnce();				
			}
		};
		finishConstructor();
		
		// popup setup
		this.lastMessagePopup = new Popup(page, "lastMessagePopup", true);
		lastMessagePopup.setDefaultTitle("Last alarm message");
		this.lastMessageDevice = new Label(page, "lastMessagePopupDevice");
		this.consoleCommand = new Label(page, "consoleCommand");
		this.ccuUrl = new Label(page, "ccuUrl");
		
		final StaticTable tab = new StaticTable(3, 2, new int[]{3, 9});
		tab.setContent(0, 0, "Device").setContent(0, 1, lastMessageDevice)
			.setContent(1, 0, "Browser URL").setContent(1,1, consoleCommand)
			.setContent(2, 0, "CCU UI URL").setContent(2,1, ccuUrl);
		final PageSnippet snip = new PageSnippet(page, "lastMessageSnip", true);
		snip.append(tab, null);
		lastMessagePopup.setBody(snip, null);
		final Button closeLastMessage = new Button(page, "lastMessageClose", "Close");
		closeLastMessage.triggerAction(lastMessagePopup, TriggeringAction.ON_CLICK, TriggeredAction.HIDE_WIDGET);
		lastMessagePopup.setFooter(closeLastMessage, null);
		page.append(lastMessagePopup);
		//

		EscalationAutoActionByAlarmingProvider<HmInterfaceInfo> ccuRestartOnAlarm = new EscalationAutoActionByAlarmingProvider<HmInterfaceInfo>(HmInterfaceInfo.class, AbsoluteTiming.DAY,
				3*TimeProcUtil.HOUR_MILLIS, 45*TimeProcUtil.MINUTE_MILLIS, "CCURestartOnConnectionLost", appManPlus) {
			@Override
			protected String performAction(InstallAppDevice iad, HmInterfaceInfo device, EscalationKnownIssue issue) {
				int result = restartCCU(device);
				return "CCUAccess result:"+result;
			}
		};
		controller.dpService.registerEscalationProvider(ccuRestartOnAlarm );
	}

	@Override
	protected void finishConstructor() {
		devTable = new DeviceTableBase(page, controller.appManPlus, alert, this, null) {
			
			@Override
			public void addWidgets(final InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, final ApplicationManager appMan) {
				final HmInterfaceInfo device;
				if(req == null)
					device = ResourceHelper.getSampleResource(HmInterfaceInfo.class);
				else
					device = (HmInterfaceInfo) object.device().getLocationResource();
				final String name;
				if(device.getLocation().toLowerCase().contains("homematic")) {
					Resource parent = device.getParent();
					String loc;
					String suffix = "";
					if(parent != null) {
						loc = parent.getLocation();
						suffix = "";
						if(loc.endsWith("_ip") || loc.endsWith("_cc")) {
							suffix = loc.substring(loc.length()-2);
							loc = loc.substring(0, loc.length()-3);
						} else if(loc.endsWith("ip") || loc.endsWith("cc")) {
							suffix = loc.substring(loc.length()-2);
							loc = loc.substring(0, loc.length()-2);
						}
					} else
						loc = device.getLocation();
					name = "CCU HM:"+ScheduleViewerOpenButtonEval.getLastCharsWithDigitsPreferred(loc, 4)+suffix;
				} else
					name = ResourceUtils.getHumanReadableShortName(device);
				vh.stringLabel("Name", id, name, row);
				vh.stringLabel("ID", id, object.deviceId().getValue(), row);
				
				if(req == null) {
					vh.registerHeaderEntry("DutyCcl");
					vh.registerHeaderEntry("DC5minMax");
					vh.registerHeaderEntry("Last Contact");
					vh.registerHeaderEntry("yellow");
					vh.registerHeaderEntry("red");
					vh.registerHeaderEntry("TeachIn");
					vh.registerHeaderEntry("clientUrl");
					vh.registerHeaderEntry("Location");
					vh.registerHeaderEntry("Comment");
					vh.registerHeaderEntry("Plot");
					vh.registerHeaderEntry("Restart");
					vh.registerHeaderEntry("Controller_Connecting");
					vh.registerHeaderEntry("UI Access");
					vh.registerHeaderEntry("RT");
					return;
				}
				Label dutyCycleLb = ChartsUtil.getDutyCycleLabel(device, object, vh, id);
				row.addCell("DutyCcl", dutyCycleLb);
				Label dutyCycleLb5MM = ChartsUtil.getDutyCycleLabel(device, "DC5minMax", object,
						vh.getParent(), vh.getReq(), id,
						object.getSubResource(HmSetpCtrlManagerTHSetp.dutyCycleMax, FloatResource.class));
				row.addCell("DC5minMax", dutyCycleLb5MM);
				Label lastContact = addLastContact(vh, id, req, row, device.dutyCycle().reading());
				vh.floatEdit("yellow", id, object.getSubResource(HmSetpCtrlManagerTHSetp.dutyCycleYellowMin, PercentageResource.class),
						row, alert, 0, 100.0f, "Only 0 to 100% allowed");
				vh.floatEdit("red", id, object.getSubResource(HmSetpCtrlManagerTHSetp.dutyCycleRedMin, PercentageResource.class),
						row, alert, 0, 100.0f, "Only 0 to 100% allowed");
				
				HmCCUPageUtils.addTechInModeButton(object, device, vh, id, req, row, appMan, hwConfig);
				
			    //final String clientUrl = "http://192.168.0.101:2010";
			    final String clientUrl = HmCCUPageUtils.addClientUrl(device, vh, id, row);
				vh.stringLabel("Location", id, object.installationLocation(), row);
				vh.stringLabel("Comment", id, object.installationComment(), row);

				dutyCycleLb.setPollingInterval(DEFAULT_POLL_RATE, req);
				dutyCycleLb5MM.setPollingInterval(DEFAULT_POLL_RATE, req);
				lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
				
				DeviceHandlerProviderDP<Resource> pe = controller.dpService.getDeviceHandlerProvider(object);
				final GetPlotButtonResult logResult = ChartsUtil.getPlotButton(id, object, appManPlus.dpService(), appMan, false, vh, row, req, pe,
						ScheduleViewerConfigProvCCUDebug.getInstance(), null);
				row.addCell("Plot", logResult.plotButton);
				
				//TODO: Provide method to check if CCU can be accessed via CCUAccess
				if(controller.hwInstApp.ccuAccess != null) {
					final IntegerResource ccuAccRes = device.getSubResource("ccuAccessResult", IntegerResource.class);
					final ButtonConfirm restartCcu = new ButtonConfirm(mainTable, "restartCCu"+id, req) {
						@Override
						public void onGET(OgemaHttpRequest req) {
							String text;
							if(hmDriverRestartTimer.isCounting()) {
								long remain = hmDriverRestartTimer.getRemainingTime();
								text = "HM-Restart in "+(remain/1000)+" sec";
							} else if(controller.hwInstApp.gwSync != null) {
								String remoteGatewayOfCcu = GatewaySyncUtil.getGatewayBaseIdIfRemoteDevice(device);
								if(remoteGatewayOfCcu != null) {
									text = "On Gateway "+remoteGatewayOfCcu;
									disable(req);
								}
								else
									text = "Reboot";
							} else
								text = "Reboot";
							
							removeStyle(ButtonData.BOOTSTRAP_ORANGE, req);
							removeStyle(ButtonData.BOOTSTRAP_GREEN, req);
							if(ccuAccRes.isActive()) {
								if(ccuAccRes.getValue() > 0) {
									addStyle(ButtonData.BOOTSTRAP_ORANGE, req);
									text += " (E:"+ccuAccRes.getValue()+")";
								} else {
									addStyle(ButtonData.BOOTSTRAP_GREEN, req);
								}
							}
							setText(text, req);
						}
						
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							restartCCU(device, ccuAccRes);
						}
					};
					if(Boolean.getBoolean("org.ogema.devicefinder.util.supportcascadedccu")) {
						String gwId = GatewaySyncUtil.getGatewayBaseIdStartingGw(device);
						restartCcu.setDefaultConfirmMsg("Currenlty you have to perform this task directly on gateway "+gwId);
					} else
						restartCcu.setDefaultConfirmMsg("Really restart Ccu and Homematic driver?");
					row.addCell("Restart", restartCcu);
				}
				
				final GlitNetRouter router = device.getSubResource("ccuController", GlitNetRouter.class);
				Map<GlitNetRouter, String> valuesToSet = new HashMap<>();
				Collection<InstallAppDevice> all = controller.dpService.managedDeviceResoures(GlitNetRouter.class);
				for(InstallAppDevice iad: all) {
					valuesToSet.put((GlitNetRouter) iad.device().getLocationResource(), iad.deviceId().getValue());
				}
				vh.referenceDropdownFixedChoice("Controller_Connecting", id, router, row, valuesToSet);
				final Button showMsg = new Button(mainTable, "msg" + id, req) {
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						int ccuNum = LocalDeviceId.getDeviceIdNumericalPart(object);
					    String gatewayId = GatewayUtil.getGatewayBaseId(appMan.getResourceAccess());
				        final int ccuHttpPort;
				        if(router.exists())
				        	ccuHttpPort = 81;
				        else
				        	ccuHttpPort = 80;
					        
				        int gwNum = Integer.parseInt(gatewayId) % 1000;
				        int gwSshPort = gwNum + 22000;
				        final URI clientURI;
				        int localForward = 30000 + gwNum * 10 + ccuNum;
						try {
							if(clientUrl != null)
								clientURI = new URI(clientUrl);
							else
								clientURI = new URI("10.168.14.x");
							String host = clientURI.getHost();
							if(host == null)
								host = clientUrl;
							String sshCommand = String.format("ssh -J user@wan01.smartrplace.de -p %d -L %d:%s:%d ogema@localhost",
					                gwSshPort, localForward, host, ccuHttpPort);
							String url = "localhost:3"+(gwNum*10+ccuNum);
							consoleCommand.setText(sshCommand, req);
							lastMessageDevice.setText(object.deviceId().getValue(), req);
							ccuUrl.setText(url, req);
						} catch (URISyntaxException e) {
							e.printStackTrace();
							throw new IllegalStateException(e);
						}
					    //System.out.printf("ssh -J user@wan01.smartrplace.de -p %d -L %d:%s:%d ogema@localhost",
					    //      gwSshPort, localForward, clientURI.getHost(), ccuHttpPort);
					}
					
				};
				showMsg.setDefaultText("CCU UI Access");
				showMsg.setDefaultToolTip("Show console command to start connection to CCU");
				showMsg.triggerAction(lastMessagePopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET, req);
				showMsg.triggerAction(lastMessageDevice,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
				showMsg.triggerAction(consoleCommand,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
				showMsg.triggerAction(ccuUrl,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
				row.addCell(WidgetHelper.getValidWidgetId("UI Access"), showMsg);

				String text = getHomematicCCUId(object.device().getLocation());
				vh.stringLabel("RT", id, text, row);
			}
			
			@Override
			protected String id() {
				return "CCUDetails";
			}
			
			@Override
			public String getTableTitleRaw() {
				return ""; //CCU Detail Data";
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return HmInterfaceInfo.class;
			}
			
			@Override
			public List<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
				List<InstallAppDevice> all = MainPage.getDevicesSelectedDefault(null, controller, roomsDrop, typeFilterDrop, req);
				List<InstallAppDevice> result = new ArrayList<InstallAppDevice>();
				for(InstallAppDevice dev: all) {
					if(dev.device() instanceof HmInterfaceInfo) {
						result.add(dev);
					}
				}
				return result;
			}
		};
		devTable.triggerPageBuild();
		typeFilterDrop.registerDependentWidget(devTable.getMainTable());
		
	}
	
	private int restartCCU(final HmInterfaceInfo device) {
		final IntegerResource ccuAccRes = device.getSubResource("ccuAccessResult", IntegerResource.class);
		return restartCCU(device, ccuAccRes);
	}
	private int restartCCU(final HmInterfaceInfo device, IntegerResource ccuAccRes) {
		final int result;
		final String remoteGatewayOfCcu;
		if(controller.hwInstApp.gwSync != null) {
			remoteGatewayOfCcu = GatewaySyncUtil.getGatewayBaseIdIfRemoteDevice(device);
		} else
			remoteGatewayOfCcu = null;
		if(remoteGatewayOfCcu != null) {
			return -9999;
			/*Resource parent = device.getParent();
			CompletionStage<RemoteStatus> cs = controller.hwInstApp.gwSync.rebootCCU((HmLogicInterface) parent, remoteGatewayOfCcu);
			CountdownTimerMulti2Single hmDriverRestartTimerRemote = new CountdownTimerMulti2Single(controller.appMan, 7*TimeProcUtil.MINUTE_MILLIS) {
				
				@Override
				public void delayedExecution() {
					controller.hwInstApp.gwSync.restartBundle(BundleType.HomematicDriver, remoteGatewayOfCcu);
					BundleRestartButton.hmRestart.executeNonBlockingOnce();				
				}
			};
			result = 0; //TODO: Get result from cs
			return result;*/
		}
		if(controller.hwInstApp.ccuAccess != null) {
			Resource parent = device.getParent();
			if(parent != null && (parent instanceof HmLogicInterface)) try {
				result = controller.hwInstApp.ccuAccess.reboot((HmLogicInterface) parent);
				if(result != 0) {
					ValueResourceHelper.setCreate(ccuAccRes, result);
				} else {
					ccuAccRes.setValue(0);
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalStateException(e);
			} else
				result = -98;
		} else
			result = -99;
		hmDriverRestartTimer.newEvent();		
		return result;
	}
}
