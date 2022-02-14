package org.smartrplace.apps.hw.install.gui.expert;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.LastContactLabel;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;
import de.iwes.widgets.html.form.textfield.TextField;

@SuppressWarnings("serial")
public class ThermostatPage extends MainPage {

	DeviceTableBase devTable;
	
	@Override
	protected String getHeader() {return "Thermostat Page";}

	public ThermostatPage(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller, false);
		finishConstructor();
	}

	@Override
	protected void finishConstructor() {
		devTable = new DeviceTableBase(page, controller.appManPlus, alert, this, null) {
			/*@Override
			public String getLineId(InstallAppDevice object) {
				super.getLineId(object);
				Room deviceRoom = object.device().location().room();
				if(deviceRoom.exists())
					return ResourceUtils.getHumanReadableShortName(deviceRoom)+object.getName()+"_THERMOSTAT";
				return object.getName()+"_THERMOSTAT";
			}*/
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				//if(!(object.device() instanceof Thermostat) && (req != null)) return null;
				final Thermostat device;
				if(req == null)
					device = ResourceHelper.getSampleResource(Thermostat.class);
				else
					device = (Thermostat) object.device().getLocationResource();
				//if(!(object.device() instanceof Thermostat)) return;
				final String name;
				if(device.getLocation().toLowerCase().contains("homematic")) {
					name = "Thermostat HM:"+ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
				} else
					name = ResourceUtils.getHumanReadableShortName(device);
				vh.stringLabel("Name", id, name, row);
				vh.stringLabel("ID", id, object.deviceId().getValue(), row);
				Label setpointFB = vh.floatLabel("Setpoint", id, device.temperatureSensor().deviceFeedback().setpoint(), row, "%.1f");
				Label lastContactFB = addLastContact("Last FB", vh, "FB"+id, req, row,device.temperatureSensor().deviceFeedback().setpoint());
				TextField setpointSet = null;
				if(req != null) {
					setpointSet = new TextField(mainTable, "setpointSet"+id, req) {
						private static final long serialVersionUID = 1L;
						@Override
						public void onGET(OgemaHttpRequest req) {
							setValue(String.format("%.1f", device.temperatureSensor().settings().setpoint().getCelsius()), req);
						}
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							String val = getValue(req);
							val = val.replaceAll("[^\\d.]", "");
							try {
								float value  = Float.parseFloat(val);
								if(value < 4.5f || value> 30.5f) {
									alert.showAlert("Allowed range: 4.5 to 30°C", false, req);
								} else
									device.temperatureSensor().settings().setpoint().setCelsius(value);
							} catch (NumberFormatException | NullPointerException e) {
								if(alert != null) alert.showAlert("Entry "+val+" could not be processed!", false, req);
								return;
							}
						}
					};
					row.addCell("SetpointSet", setpointSet);
				} else
					vh.registerHeaderEntry("SetpointSet");
				Label tempmes = vh.floatLabel("Measurement", id, device.temperatureSensor().reading(), row, "%.1f#min:-200");
				Label lastContact = null;
				if(req != null) {
					lastContact = new LastContactLabel(device.temperatureSensor().reading(), appMan, mainTable, "lastContact"+id, req);
					row.addCell(WidgetHelper.getValidWidgetId("Last Measurment"), lastContact);
				} else
					vh.registerHeaderEntry("Last Measurment");

				Label valvePos = vh.floatLabel("Valve", id, device.valve().setting().stateFeedback().getValue()*100f, row, "%.1f");
				Label lastContactValve = addLastContact("Last Valve", vh, "Valve"+id, req, row, device.valve().setting().stateFeedback());
				
				final FloatResource valveError = device.valve().getSubResource("eq3state", FloatResource.class);
				if(valveError.exists() || (req == null)) {
					if(req == null) {
						vh.registerHeaderEntry("VErr");
						vh.registerHeaderEntry("Last VErr");
					} else {
						//Label valveErrL = vh.floatLabel("VErr", id, valveError, row, "%.0f");
						Label valveErrL = new Label(mainTable, "Verr"+id, req) {
							boolean hasStyle = false;
							
							@Override
							public void onGET(OgemaHttpRequest req) {
								float val = valveError.getValue();
								if(val < 4) {
									addStyle(LabelData.BOOTSTRAP_ORANGE, req);
									hasStyle = true;
								} else if(val > 4) {
									addStyle(LabelData.BOOTSTRAP_RED, req);
									hasStyle = true;
								} else if(hasStyle){
									removeStyle(LabelData.BOOTSTRAP_ORANGE, req);
									removeStyle(LabelData.BOOTSTRAP_RED, req);
									addStyle(LabelData.BOOTSTRAP_GREEN, req);
								}
								setText(String.format("%.1f", val), req);
							}
						};
						row.addCell(WidgetHelper.getValidWidgetId("VErr"), valveErrL);
						Label lastContactValveErr = addLastContact("Last VErr", vh, id, req, row, valveError);
						valveErrL.setPollingInterval(DEFAULT_POLL_RATE, req);
						lastContactValveErr.setPollingInterval(DEFAULT_POLL_RATE, req);
					}
				}
				if(req == null) {
					vh.registerHeaderEntry("Com/Err");
					vh.registerHeaderEntry("Last Err");
					vh.registerHeaderEntry("CtrlMode");
				} else {
					final IntegerResource errorCode = ResourceHelper.getSubResourceOfSibbling(device,
							"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "errorCode", IntegerResource.class);
if(device.getLocation().contains("homematicgl_ar300_144cb1_local_ip/devices/HM_HmIP_eTRV_C_2_002CDD89930302"))
System.out.println("Fzufoiude");
					final BooleanResource configPending = ResourceHelper.getSubResourceOfSibbling(device,
							"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "configPending", BooleanResource.class);
					final IntegerResource controlMode = device.getSubResource("controlMode", IntegerResource.class);
					final IntegerResource controlModeFeedback = device.getSubResource("controlModeFeedback", IntegerResource.class);
					Label errLabel = new Label(mainTable, "errLabel"+id, req) {
						boolean hasStyle = false;

						@Override
						public void onGET(OgemaHttpRequest req) {
							String text = "";
							int error = 0;
							BooleanResource comDisturbed = ResourceHelper.getSubResourceOfSibbling(device,
									"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "communicationStatus/communicationDisturbed", BooleanResource.class);
							if(comDisturbed != null && comDisturbed.exists() && comDisturbed.getValue()) {
								text += "CD";
								error = 2;
							}
							if(configPending != null && configPending.exists()) {
								if(configPending.getValue()) {
									text += " CfP";
									error = 2;
								}
							} else {
								text += " NCfP";
								if(error == 0) error = 1;
							}
							if(errorCode != null && errorCode.exists()) {
								if(errorCode.getValue() > 0) {
									text += " EC"+errorCode.getValue();
									error = 2;
								}
							} else {
								text += " NEC";
								if(error == 0) error = 1;
							}
							if(error == 1) {
								addStyle(LabelData.BOOTSTRAP_ORANGE, req);
								hasStyle = true;
							} else if(error == 2) {
								addStyle(LabelData.BOOTSTRAP_RED, req);
								hasStyle = true;
							} else {
								text = "OK";
								if(hasStyle) {
									removeStyle(LabelData.BOOTSTRAP_ORANGE, req);
									removeStyle(LabelData.BOOTSTRAP_RED, req);
									addStyle(LabelData.BOOTSTRAP_GREEN, req);
								}
							}
							setText(text, req);
						}
					};
					row.addCell(WidgetHelper.getValidWidgetId("Com/Err"), errLabel);
					errLabel.setPollingInterval(DEFAULT_POLL_RATE, req);
					
					if(errorCode != null && errorCode.exists()) {
						Label lastContactErrcode = addLastContact("Last Err", vh, id, req, row, errorCode);
						lastContactErrcode.setPollingInterval(DEFAULT_POLL_RATE, req);
					}

					Label ctrlModeLb = new Label(mainTable, "ctrlModeLb"+id, req) {
						@Override
						public void onGET(OgemaHttpRequest req) {
							String text;
							if(controlMode != null && controlMode.exists())
								text = ""+controlMode.getValue()+" / ";
							else
								text = "- / ";
							if(controlModeFeedback != null && controlModeFeedback.exists())
								text += controlModeFeedback.getValue();
							else
								text += "-";
							boolean isFaulty = (!controlMode.exists()) || (!controlModeFeedback.exists()) ||
									(controlMode.getValue() != controlModeFeedback.getValue());
							setText(text, req);
							if(isFaulty) {
								addStyle(LabelData.BOOTSTRAP_ORANGE, req);
							} else {
								removeStyle(LabelData.BOOTSTRAP_ORANGE, req);
							}
						}
					};
					row.addCell(WidgetHelper.getValidWidgetId("CtrlMode"), ctrlModeLb);
					ctrlModeLb.setPollingInterval(DEFAULT_POLL_RATE, req);
				
				}

				// TODO addWidgetsCommon(object, vh, id, req, row, appMan, device.location().room());
				Room deviceRoom = device.location().room();
				//addRoomWidget(vh, id, req, row, appMan, deviceRoom);
				//addSubLocation(object, vh, id, req, row);
				String roomSubLoc = ResourceUtils.getHumanReadableShortName(deviceRoom);
				if(object.installationLocation().exists() && !object.installationLocation().getValue().isEmpty()) {
					roomSubLoc += "-"+object.installationLocation().getValue();
				}
				vh.stringLabel("Room-Loc", id, roomSubLoc, row);
				
				if(req != null) {
					DeviceHandlerProviderDP<Resource> pe = controller.dpService.getDeviceHandlerProvider(object);
					final GetPlotButtonResult logResult = MainPage.getPlotButton(id, object, appManPlus.dpService(), appMan, false, vh, row, req, pe,
							ScheduleViewerConfigProvThermDebug.getInstance(), null);
					row.addCell("Plot", logResult.plotButton);

					String text = getHomematicCCUId(object.device().getLocation());
					vh.stringLabel("RT", id, text, row);
				} else {
					vh.registerHeaderEntry("Plot");
					vh.registerHeaderEntry("RT");
				}
				
				
				
				if(req != null) {
					tempmes.setPollingInterval(DEFAULT_POLL_RATE, req);
					setpointFB.setPollingInterval(DEFAULT_POLL_RATE, req);
					lastContactFB.setPollingInterval(DEFAULT_POLL_RATE, req);
					setpointSet.setPollingInterval(DEFAULT_POLL_RATE, req);
					lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
					valvePos.setPollingInterval(DEFAULT_POLL_RATE, req);
					lastContactValve.setPollingInterval(DEFAULT_POLL_RATE, req);
				}
			}
			
			@Override
			protected String id() {
				return "ThermostatDetails";
			}
			
			@Override
			public String getTableTitleRaw() {
				return "Current Thermostat Measurements";
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return Thermostat.class;
			}
			
			@Override
			public List<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
				List<InstallAppDevice> all = MainPage.getDevicesSelectedDefault(null, controller, roomsDrop, typeFilterDrop, req);
				//List<InstallAppDevice> all = appSelector.getDevicesSelected();
				List<InstallAppDevice> result = new ArrayList<InstallAppDevice>();
				for(InstallAppDevice dev: all) {
					if(dev.device() instanceof Thermostat) {
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
