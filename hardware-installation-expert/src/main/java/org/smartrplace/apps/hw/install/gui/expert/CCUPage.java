package org.smartrplace.apps.hw.install.gui.expert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.eval.hardware.HmCCUPageUtils;
import org.smartrplace.os.util.BundleRestartButton;
import org.smartrplace.tissue.util.resource.GatewaySyncUtil;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.virtualdevice.ChartsUtil;
import org.smartrplace.util.virtualdevice.ChartsUtil.GetPlotButtonResult;
import org.smartrplace.util.virtualdevice.HmSetpCtrlManagerTHSetp;

import de.iwes.util.logconfig.CountdownTimerMulti2Single;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.label.Label;

@SuppressWarnings("serial")
public class CCUPage extends MainPage {

	private static final int CCU_MAX_FOR_ALL = 50;
	DeviceTableBase devTable;
	HardwareInstallConfig hwConfig;
	private final CountdownTimerMulti2Single hmDriverRestartTimer;
	
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
	}

	@Override
	protected void finishConstructor() {
		devTable = new DeviceTableBase(page, controller.appManPlus, alert, this, null) {
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
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
				
				HmCCUPageUtils.addClientUrl(device, vh, id, row);
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
							if(controller.hwInstApp.ccuAccess != null) {
								Resource parent = device.getParent();
								if(parent != null && (parent instanceof HmLogicInterface)) try {
									int result = controller.hwInstApp.ccuAccess.reboot((HmLogicInterface) parent);
									if(result != 0) {
										ValueResourceHelper.setCreate(ccuAccRes, result);
									} else
										ccuAccRes.setValue(0);
								} catch (IOException e) {
									e.printStackTrace();
									throw new IllegalStateException(e);
								}
							}
							hmDriverRestartTimer.newEvent();
						}
					};
					if(Boolean.getBoolean("org.ogema.devicefinder.util.supportcascadedccu")) {
						String gwId = GatewaySyncUtil.getGatewayBaseIdStartingGw(device);
						restartCcu.setDefaultConfirmMsg("Currenlty you have to perform this task directly on gateway "+gwId);
					} else
						restartCcu.setDefaultConfirmMsg("Really restart Ccu and Homematic driver?");
					row.addCell("Restart", restartCcu);
				}
				
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
	
}
