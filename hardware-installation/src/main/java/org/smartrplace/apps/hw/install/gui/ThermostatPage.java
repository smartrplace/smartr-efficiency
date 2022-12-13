package org.smartrplace.apps.hw.install.gui;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.api.PropType;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.devicefinder.util.LastContactLabel;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.actors.MultiSwitch;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.devices.buildingtechnology.ThermostatProgram;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.ogema.util.extended.eval.widget.IntegerResourceMultiButton;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.LabelFormatter;
import org.smartrplace.util.directobjectgui.LabelFormatterFloatRes;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.util.virtualdevice.ChartsUtil;
import org.smartrplace.util.virtualdevice.ChartsUtil.GetPlotButtonResult;
import org.smartrplace.util.virtualdevice.HmSetpCtrlManager;
import org.smartrplace.util.virtualdevice.HmSetpCtrlManagerTHControlMode;
import org.smartrplace.util.virtualdevice.HmSetpCtrlManagerTHIntTrigger;
import org.smartrplace.util.virtualdevice.HmSetpCtrlManagerTHSetp;
import org.smartrplace.util.virtualdevice.SensorData;
import org.smatrplace.apps.hw.install.gui.mainexpert.MainPageExpert;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.WidgetStyle;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;
import de.iwes.widgets.html.form.textfield.TextField;

@SuppressWarnings("serial")
public class ThermostatPage extends MainPage {

	private static final int THERMOSTAT_MAX_FOR_ALL = 500;
	DeviceTableBase devTable;
	
	public enum ThermostatPageType {
		STANDARD,
		AUTO_MODE,
		VALVE_ONLY,
		BATTERY_WINDOW,
		STANDARD_VIEW_ONLY
	}
	protected final ThermostatPageType type;
	
	@Override
	public String getHeader() {
		switch(type) {
		case STANDARD:
			return "Thermostat Special Selection";
		case AUTO_MODE:
			return "Thermostat Auto-Mode and Valve Adapt Management";
		case VALVE_ONLY:
			return "Valve Adapt Management Only";
		case BATTERY_WINDOW:
			return "Thermostat Battery and Window Management";
		case STANDARD_VIEW_ONLY:
			return "Thermostat Page";
		}
		throw new IllegalStateException("Unknown type:"+type);
	}

	static Boolean isAllAllowed = null;
	@Override
	protected boolean isAllOptionAllowedSuper(OgemaHttpRequest req) {
		if(isAllAllowed == null) {
			int thermNum = appMan.getResourceAccess().getResources(Thermostat.class).size();
			isAllAllowed = (thermNum <= THERMOSTAT_MAX_FOR_ALL);
		}
		return isAllAllowed;
	}
	
	public ThermostatPage(WidgetPage<?> page, HardwareInstallController controller,
			ThermostatPageType type) {
		super(page, controller, false);
		this.type = type;
		finishConstructor();
	}

	@Override
	protected void finishConstructor() {
		if(type == ThermostatPageType.BATTERY_WINDOW) {
			page.append(alert).linebreak();
			StaticTable secondTopTable = new StaticTable(1, 5);
			ButtonConfirm updateAll = new ButtonConfirm(page, "updateallFbFaulty", "Resend WinMode") {
				@Override
				public void onPrePOST(String data, OgemaHttpRequest req) {
					List<InstallAppDevice> all = MainPage.getDevicesSelectedDefault(null, controller, roomsDrop, typeFilterDrop, req);
					int count = 0;
					for(InstallAppDevice dev: all) {
						if(dev.device() instanceof Thermostat) {
							Thermostat device = (Thermostat) dev.device().getLocationResource();
							ResourceList<?> master = device.getSubResource("HmParametersMaster", ResourceList.class);
							if(master.exists()) {
								IntegerResource control = master.getSubResource("TEMPERATUREFALL_MODUS", IntegerResource.class);
								IntegerResource feedback = master.getSubResource("TEMPERATUREFALL_MODUS_FEEDBACK", IntegerResource.class);
								if(!(control.isActive() && feedback.exists()))
									continue;
								int ctVal = control.getValue();
								int fbVal = feedback.getValue();
								if(ctVal == fbVal)
									continue;
								control.setValue(ctVal);
								count++;
							}
						}
					}
					alert.showAlert("Sent winMode update to "+count+" thermostats", count>0, req);
				}
			};
			//updateAll.triggerOnPOST(alert);
			updateAll.registerDependentWidget(alert);
			updateAll.setDefaultConfirmMsg("Really re-send winMode value to all selected thermostats with pending feedback?");
			secondTopTable.setContent(0, 0, updateAll);
			page.append(secondTopTable);
		}
		
		devTable = new DeviceTableBase(page, controller.appManPlus, alert, this, null) {
			private Label addControlFeedbackLabel(String widgetId, SingleValueResource control, SingleValueResource feedback,
					String lastContactWidgetId,
					ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row) {
				if(!(control.exists() || feedback.exists()))
					return null;
				ControlFeedbackFormatter formatter;
				if(control instanceof FloatResource) {
					formatter = new ControlFeedbackFormatter((FloatResource)control, (FloatResource)feedback, controller.dpService);
				} else if(control instanceof StringResource) {
					formatter = new ControlFeedbackFormatter((StringResource)control, (StringResource)feedback, controller.dpService);
				} else
					formatter = new ControlFeedbackFormatter((IntegerResource)control, (IntegerResource)feedback, controller.dpService);
				Label winMode = vh.stringLabel(widgetId, id, formatter, row);
				winMode.setPollingInterval(DeviceTableRaw.DEFAULT_POLL_RATE, req);
				if(lastContactWidgetId != null) {
					Label lastWinMode = addLastContact(lastContactWidgetId, vh, id, req, row, feedback);
					lastWinMode.setPollingInterval(DeviceTableRaw.DEFAULT_POLL_RATE, req);
				}
				return winMode;
			}
			
			private TextField addSetpEditField(String widgetId, final FloatResource control,
					ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row) {
				TextField setpointSet = null;
				if(req != null) {
					setpointSet = new TextField(mainTable, widgetId+id, req) {
						private static final long serialVersionUID = 1L;
						@Override
						public void onGET(OgemaHttpRequest req) {
							if(control instanceof TemperatureResource)
								setValue(String.format("%.1f", ((TemperatureResource)control).getCelsius()), req);
							else
								setValue(String.format("%.1f", control.getValue()), req);
						}
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							String val = getValue(req);
							val = val.replaceAll("[^\\d.]", "");
							try {
								float value  = Float.parseFloat(val);
								if((Boolean.getBoolean("org.smartrplace.apps.heatcontrol.relativesetpoints") && (value < -3f || value > 3f)) ||
										((!Boolean.getBoolean("org.smartrplace.apps.heatcontrol.relativesetpoints")) && (value < 4.5f || value> 30.5f))) {
									alert.showAlert("Outside allowed range", false, req);
								} else if(control instanceof TemperatureResource)
									((TemperatureResource)control).setCelsius(value);
								else
									control.setValue(value);
							} catch (NumberFormatException | NullPointerException e) {
								if(alert != null) alert.showAlert("Entry "+val+" could not be processed!", false, req);
								return;
							}
						}
					};
					row.addCell(WidgetHelper.getValidWidgetId(widgetId), setpointSet);
					setpointSet.setPollingInterval(DEFAULT_POLL_RATE, req);
				} else
					vh.registerHeaderEntry(widgetId);
				return setpointSet;
			}
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, final ApplicationManager appMan) {
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
				final Label setpointFB;
				if(type == ThermostatPageType.STANDARD)
					setpointFB = vh.floatLabel("Setpoint", id, device.temperatureSensor().deviceFeedback().setpoint(), row, "%.1f");
				else {
					setpointFB = vh.floatLabel("Setpoint", id, device.temperatureSensor().deviceFeedback().setpoint(), row, "%.1f",
							new LabelFormatterFloatRes() {
								@Override
								public int getState(float value, OgemaHttpRequest req) {
									float valFb = device.temperatureSensor().deviceFeedback().setpoint().getValue();
									float valSetp = device.temperatureSensor().settings().setpoint().getValue();
									float diff = valFb - valSetp;
									if(diff < -0.3f)
										return 2;
									else if(diff > 0.3f)
										return 0;
									else
										return 1;
								}
					});
				}
				Label lastContactFB = addLastContact("Last FB", vh, "FB"+id, req, row,device.temperatureSensor().deviceFeedback().setpoint());
				if(type == ThermostatPageType.STANDARD) {
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
									if((Boolean.getBoolean("org.smartrplace.apps.heatcontrol.relativesetpoints") && (value < -3f || value > 3f)) ||
											((!Boolean.getBoolean("org.smartrplace.apps.heatcontrol.relativesetpoints")) && (value < 4.5f || value> 30.5f))) {
										alert.showAlert("Outside allowed range", false, req);
									} else
										device.temperatureSensor().settings().setpoint().setCelsius(value);
								} catch (NumberFormatException | NullPointerException e) {
									if(alert != null) alert.showAlert("Entry "+val+" could not be processed!", false, req);
									return;
								}
							}
						};
						row.addCell("SetpointSet", setpointSet);
						setpointSet.setPollingInterval(DEFAULT_POLL_RATE, req);
					} else
						vh.registerHeaderEntry("SetpointSet");
				}
				if((type != ThermostatPageType.BATTERY_WINDOW)) {
					Label tempmes = vh.floatLabel("Measurement", id, device.temperatureSensor().reading(), row, "%.1f#min:-200");
					Label lastContact = null;
					if(req != null) {
						lastContact = new LastContactLabel(device.temperatureSensor().reading(), appMan, mainTable, "lastContact"+id, req);
						row.addCell(WidgetHelper.getValidWidgetId("Last Measurment"), lastContact);
						tempmes.setPollingInterval(DEFAULT_POLL_RATE, req);
						lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
					} else
						vh.registerHeaderEntry("Last Measurment");
				}
				if(type == ThermostatPageType.STANDARD || type == ThermostatPageType.STANDARD_VIEW_ONLY) {
					if(req == null) {
						vh.registerHeaderEntry("Valve");
						vh.registerHeaderEntry("Last Valve");
					} else {
						Label valvePos = new Label(mainTable, "Valve"+id, req) {
							@Override
							public void onGET(OgemaHttpRequest req) {
								String text = String.format("%.0f%%",  device.valve().setting().stateFeedback().getValue()*100f);
								setText(text, req);
							}
						};
						valvePos.setPollingInterval(DEFAULT_POLL_RATE, req);
						row.addCell("Valve", valvePos);
						
						//vh.floatLabel("Valve", id, device.valve().setting().stateFeedback().getValue()*100f, row, "%.1f");
						Label lastContactValve = addLastContact("Last Valve", vh, "Valve"+id, req, row, device.valve().setting().stateFeedback());
						lastContactValve.setPollingInterval(DEFAULT_POLL_RATE, req);
					}
				}
				if(type == ThermostatPageType.STANDARD) {
					final FloatResource errorRun = device.valve().getSubResource("errorRunPosition", MultiSwitch.class).stateControl();
					final FloatResource errorRunFb = device.valve().getSubResource("errorRunPosition", MultiSwitch.class).stateFeedback();
					if(errorRun.exists() || (req == null)) {
						if(req == null) {
							vh.registerHeaderEntry("EmptyPos");
							vh.registerHeaderEntry("Last EP");
						} else {
							//Label valveErrL = vh.floatLabel("VErr", id, valveError, row, "%.0f");
							Label valveErrL = vh.stringLabel("EmptyPos", id, new LabelFormatter() {
								
								@Override
								public OnGETData getData(OgemaHttpRequest req) {
									float val = errorRun.getValue();
									float valFb = errorRunFb.getValue();
									int state = ValueResourceHelper.isAlmostEqual(val, valFb)?1:0;
									return new OnGETData(String.format("%.0f / %.0f", val*100, valFb*100), state);
								}
							}, row);
							Label lastContactValveErr = addLastContact("Last EP", vh, id, req, row, errorRunFb);
							valveErrL.setPollingInterval(DEFAULT_POLL_RATE, req);
							lastContactValveErr.setPollingInterval(DEFAULT_POLL_RATE, req);
						}
					}					
				} else if(type == ThermostatPageType.BATTERY_WINDOW) {
					if(req == null) {
						vh.registerHeaderEntry(DeviceTableRaw.BATTERY_VOLTAGE_HEADER);
						vh.registerHeaderEntry("Last Voltage");
						vh.registerHeaderEntry("Bat.Low");
						vh.registerHeaderEntry("Last Status");
						vh.registerHeaderEntry("WinMode");
						vh.registerHeaderEntry("LastMode");
						vh.registerHeaderEntry("TempFallDelta");
						vh.registerHeaderEntry("LastDelta");
						vh.registerHeaderEntry("TempFallTemp");
						vh.registerHeaderEntry("WinDuration");
					} else {
						AddBatteryVoltageResult voltageLab = addBatteryVoltage(vh, id, req, row, device);
						Label lastContactVoltage = null;
						Label lastContactStatus = null;
						if(voltageLab != null)
							lastContactVoltage = addLastContact("Last Voltage", vh, "LV"+id, req, row, voltageLab.reading);
						AddBatteryVoltageResult statusLab = addBatteryStatus(vh, id, req, row, device);
						if(statusLab != null)
							lastContactStatus = addLastContact("Last Status", vh, "LStat"+id, req, row, statusLab.reading);
						if(voltageLab != null)
							voltageLab.label.setPollingInterval(DEFAULT_POLL_RATE, req);
						if(statusLab != null)
							statusLab.label.setPollingInterval(DEFAULT_POLL_RATE, req);
						if(lastContactVoltage != null)
							lastContactVoltage.setPollingInterval(DEFAULT_POLL_RATE, req);
						if(lastContactStatus != null)
							lastContactStatus.setPollingInterval(DEFAULT_POLL_RATE, req);

						ResourceList<?> master = device.getSubResource("HmParametersMaster", ResourceList.class);
						if(master.exists()) {
							addControlFeedbackLabel("WinMode", master.getSubResource("TEMPERATUREFALL_MODUS", IntegerResource.class),
									master.getSubResource("TEMPERATUREFALL_MODUS_FEEDBACK", IntegerResource.class), "LastMode",
									vh, id, req, row);
							addControlFeedbackLabel("TempFallDelta", master.getSubResource("TEMPERATUREFALL_VALUE", FloatResource.class),
									master.getSubResource("TEMPERATUREFALL_VALUE_FEEDBACK", FloatResource.class), "LastDelta",
									vh, id, req, row);
							addControlFeedbackLabel("TempFallTemp", master.getSubResource("TEMPERATURE_WINDOW_OPEN", FloatResource.class),
									master.getSubResource("TEMPERATURE_WINDOW_OPEN_FEEDBACK", FloatResource.class), null,
									vh, id, req, row);
							addControlFeedbackLabel("WinDuration", master.getSubResource("TEMPERATUREFALL_WINDOW_OPEN_TIME_PERIOD", IntegerResource.class),
									master.getSubResource("TEMPERATUREFALL_WINDOW_OPEN_TIME_PERIOD_FEEDBACK", IntegerResource.class), null,
									vh, id, req, row);
						}
					}					
				} else {
					final FloatResource valveError = device.valve().getSubResource("eq3state", FloatResource.class);
					if(valveError.exists() || (req == null)) {
						if(req == null) {
							vh.registerHeaderEntry("VErr");
							vh.registerHeaderEntry("Last VErr");
							if(type == ThermostatPageType.AUTO_MODE || type == ThermostatPageType.VALVE_ONLY)
								vh.registerHeaderEntry("Start Adapt");
							if(type == ThermostatPageType.VALVE_ONLY) {
								vh.registerHeaderEntry("Weekly Decalc");
								vh.registerHeaderEntry("LastWeekly");
								vh.registerHeaderEntry("Weekly Now");
								vh.registerHeaderEntry("Weekly Postpone");
							}
						} else {
							//Label valveErrL = vh.floatLabel("VErr", id, valveError, row, "%.0f");
							Label valveErrL = vh.stringLabel("VErr", id, new LabelFormatter() {
								
								@Override
								public OnGETData getData(OgemaHttpRequest req) {
									float val = valveError.getValue();
									int state;
									if(val < 4)
										state = 0;
									else if(val > 4)
										state = 2;
									else
										state = 1;
									return new OnGETData(String.format("%.1f", val), state);
								}
							}, row);
							Label lastContactValveErr = addLastContact("Last VErr", vh, id, req, row, valveError);
							valveErrL.setPollingInterval(DEFAULT_POLL_RATE, req);
							lastContactValveErr.setPollingInterval(DEFAULT_POLL_RATE, req);
							
							final BooleanResource ada = device.valve().getSubResource("startAdaption", BooleanResource.class);
							if((type == ThermostatPageType.AUTO_MODE || type == ThermostatPageType.VALVE_ONLY)
									&& (ada.exists())) {
								Button startAdapt = new Button(mainTable, "startAdapt"+id, req) {
									@Override
									public void onPOSTComplete(String data, OgemaHttpRequest req) {
										ada.setValue(true);
									}
								};
								startAdapt.setDefaultText("Start ADA");
								row.addCell(WidgetHelper.getValidWidgetId("Start Adapt"), startAdapt);
							}
							if(type == ThermostatPageType.VALVE_ONLY) {
								addControlFeedbackLabel("Weekly Decalc", device.valve().getSubResource("DECALCIFICATION", StringResource.class),
										device.valve().getSubResource("DECALCIFICATION_FEEDBACK", StringResource.class), "LastWeekly",
										vh, id, req, row);
								Button decalcNowBut = new Button(mainTable, "decalcNowBut"+id, req) {
									@Override
									public void onPOSTComplete(String data, OgemaHttpRequest req) {
										long now = appMan.getFrameworkTime();
										setDecalcTime(device, now+5*TimeProcUtil.MINUTE_MILLIS);										
									}
								};
								decalcNowBut.setDefaultText("Decalc Now");
								row.addCell(WidgetHelper.getValidWidgetId("Weekly Now"), decalcNowBut);
								
								Button decalcPostponeBut = new Button(mainTable, "decalcPostponeBut"+id, req) {
									@Override
									public void onPOSTComplete(String data, OgemaHttpRequest req) {
										long now = appMan.getFrameworkTime();
										setDecalcTime(device, now+6*TimeProcUtil.DAY_MILLIS);										
									}
								};
								decalcPostponeBut.setDefaultText("Decalc Shift Max");
								row.addCell(WidgetHelper.getValidWidgetId("Weekly Postpone"), decalcPostponeBut);
							}
						}
					}
				}
				if(type != ThermostatPageType.BATTERY_WINDOW) {
					if(req == null) {
						vh.registerHeaderEntry("Com/Err");
						vh.registerHeaderEntry("Last Err");
						if(type != ThermostatPageType.VALVE_ONLY)
							vh.registerHeaderEntry("ManuMode");
						else {
							vh.registerHeaderEntry("VveMax");
							vh.registerHeaderEntry("EditMax");
						}
					} else {
						final IntegerResource errorCode = ResourceHelper.getSubResourceOfSibbling(device,
								"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "errorCode", IntegerResource.class);
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
						row.addCell(WidgetHelper.getValidWidgetId("ManuMode"), ctrlModeLb);
						ctrlModeLb.setPollingInterval(DEFAULT_POLL_RATE, req);
					
					}
				}
				if(type == ThermostatPageType.VALVE_ONLY) {
					addParamLabel(device, PropType.THERMOSTAT_VALVE_MAXPOSITION, "VveMax", vh, id, req, row);
					//final FloatResource sres = (FloatResource) PropType.getHmParam(device, PropType.THERMOSTAT_VALVE_MAXPOSITION, true);
					final FloatResource sresCt = (FloatResource) PropType.getHmParam(device, PropType.THERMOSTAT_VALVE_MAXPOSITION, false);
					//addControlFeedbackLabel("VveMax", sresCt, sres, null, vh, id, req, row);
					addSetpEditField("EditMax", sresCt, vh, id, req, row);
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
				
				if(req != null && (type == ThermostatPageType.AUTO_MODE)) {
					if(DeviceHandlerBase.getHmThermProgram(device) == null) {
						vh.stringLabel("Allow Auto", id, "No Program", row);
					} else {
						IntegerResource autoThermostatModeSingle = DeviceHandlerBase.getAutoThermostatModeSingle(device);
						@SuppressWarnings("unchecked")
						IntegerResourceMultiButton autoModeButton = new IntegerResourceMultiButton(mainTable,
								"autoModeButton"+id, req, autoThermostatModeSingle,
								new WidgetStyle[] {ButtonData.BOOTSTRAP_LIGHTGREY, ButtonData.BOOTSTRAP_GREEN,
										ButtonData.BOOTSTRAP_RED, ButtonData.BOOTSTRAP_LIGHT_BLUE}) {
							
							@Override
							protected String getText(int state, OgemaHttpRequest req) {
								int overallState = 	controller.hwTableData.appConfigData.autoThermostatMode().getValue();
								if(overallState == 3)
									return "(Off Forced)";
								switch(state) {
								case 0:
									return "("+AlarmingUtiH.getAutoThermostatModeShort(overallState)+")";
								case 1:
									return "Allow";
								case 2:
									return "Off";
								case 3:
									return AlarmingUtiH.getAutoThermostatModeShort(0);
								default:
									return "UNKNOWN ST:"+state;
								}
							}
						};
						row.addCell(WidgetHelper.getValidWidgetId("Allow Auto"), autoModeButton);
					}
				}
				if(req != null && ((type == ThermostatPageType.AUTO_MODE) || type == ThermostatPageType.STANDARD_VIEW_ONLY)) {
					Label sendManLb = vh.stringLabel("SendMan", id, new LabelFormatter() {

						@Override
						public OnGETData getData(OgemaHttpRequest req) {
							String status = "";
							HmSetpCtrlManagerTHSetp setpMan = HmSetpCtrlManagerTHSetp.getInstance(appManPlus);
							SensorData sens = setpMan.getSensorData(device.temperatureSensor().settings().setpoint());
							status += "|"+SensorData.getStatus(sens);
							HmSetpCtrlManagerTHControlMode setpManCM = HmSetpCtrlManagerTHControlMode.getInstance(appManPlus);
							sens = setpManCM.getSensorData(device.getSubResource("controlMode", IntegerResource.class));
							status += "|"+SensorData.getStatus(sens);
							status += "|";
							HmSetpCtrlManagerTHIntTrigger setpManAuto = HmSetpCtrlManagerTHIntTrigger.getInstance(appManPlus);
							sens = setpManAuto.getSensorData(device.getSubResource("program", ThermostatProgram.class).update());
							status += "|"+SensorData.getStatus(sens);
							//int state = (status.equals("|ok|ok|ok|"))?1:0;
							int state = (status.startsWith("|ok|"))?1:0;
							if(state == 0 && (!status.matches(".*\\d.*")))
								state = 2;
							OnGETData result = new OnGETData(status, state);
							return result;
						}					
					}, row);
					sendManLb.setDefaultPollingInterval(DEFAULT_POLL_RATE);
				}
				if(req != null && (type == ThermostatPageType.STANDARD_VIEW_ONLY)) {
					MainPageExpert.addKniStatus(object, devHand, vh, id, req, row, appManPlus);
				}
				if(req != null) {
					DeviceHandlerProviderDP<Resource> pe = controller.dpService.getDeviceHandlerProvider(object);
					final GetPlotButtonResult logResult = ChartsUtil.getPlotButton(id, object, appManPlus.dpService(), appMan, false, vh, row, req, pe,
							ScheduleViewerConfigProvThermDebug.getInstance(), null);
					row.addCell("Plot", logResult.plotButton);

					String text = getHomematicCCUId(object.device().getLocation());
					if((type != ThermostatPageType.AUTO_MODE) && (type != ThermostatPageType.BATTERY_WINDOW)
							&& (type != ThermostatPageType.VALVE_ONLY))
						vh.stringLabel("RT", id, text, row);
					InstallAppDevice ccuIad = HmSetpCtrlManager.getCCU(device, controller.dpService);
					if(ccuIad != null)
						vh.stringLabel("CCU", id, ccuIad.deviceId().getValue(), row);
				} else {
					if(type == ThermostatPageType.AUTO_MODE) {
						vh.registerHeaderEntry("Allow Auto");
						vh.registerHeaderEntry("SendMan");
					} else if(type == ThermostatPageType.STANDARD_VIEW_ONLY) {
						vh.registerHeaderEntry("KniStatus");
						vh.registerHeaderEntry("SendMan");						
					}
					vh.registerHeaderEntry("Plot");
					if((type != ThermostatPageType.AUTO_MODE) && (type != ThermostatPageType.BATTERY_WINDOW)
							&& (type != ThermostatPageType.VALVE_ONLY))
						vh.registerHeaderEntry("RT");
					vh.registerHeaderEntry("CCU");
				}
				
				if(req != null) {
					setpointFB.setPollingInterval(DEFAULT_POLL_RATE, req);
					lastContactFB.setPollingInterval(DEFAULT_POLL_RATE, req);
				}
			}
			
			@Override
			protected String id() {
				return "ThermostatDetails";
			}
			
			@Override
			public String getTableTitleRaw() {
				return "";
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
	
	public static String setDecalcTime(Thermostat device, long destTime) {
		StringResource res = device.valve().getSubResource("DECALCIFICATION", StringResource.class);
		String val = getDecalcString(destTime);
		if(val != null)
			ValueResourceHelper.setCreate(res, val);
		return val;
	}
	
	public static String[] dayOfWeekStr = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
	public static String getDecalcString(long destTime) {
		long curWeekStart = AbsoluteTimeHelper.getIntervalStart(destTime, AbsoluteTiming.WEEK);
		long timeInWeek = destTime - curWeekStart;
		long dayOfWeekIdx = timeInWeek / TimeProcUtil.DAY_MILLIS;
		if(dayOfWeekIdx > 6)
			dayOfWeekIdx = 6;
		String result = dayOfWeekStr[(int) dayOfWeekIdx];
		
		long inDayTime = timeInWeek % TimeProcUtil.DAY_MILLIS;
		StringFormatHelper.getTimeOfDayInLocalTimeZone(destTime);
		long hourOfDay = inDayTime / TimeProcUtil.HOUR_MILLIS;
		if(hourOfDay > 23)
			hourOfDay = 23;
		long inHourTime = inDayTime - hourOfDay*TimeProcUtil.HOUR_MILLIS;
		if(inHourTime == 0)
			result += " "+hourOfDay+":00";
		else if(inHourTime <= 30*TimeProcUtil.MINUTE_MILLIS)
			result += " "+hourOfDay+":30";
		else if(hourOfDay == 23) {
			dayOfWeekIdx++;
			if(dayOfWeekIdx > 6)
				dayOfWeekIdx = 0;
			result = dayOfWeekStr[(int) dayOfWeekIdx]+" "+hourOfDay+":30";
		} else {
			result += " "+(hourOfDay+1)+":00";
		} return result;
	}
	
	public static Label addParamLabel(PhysicalElement device, PropType type, String colName,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
			String id, OgemaHttpRequest req, Row row) {
		if(req == null) {
			vh.registerHeaderEntry(colName);
			return null;
		}
		final SingleValueResource sres = PropType.getHmParam(device, type, true);
		final SingleValueResource sresCt = PropType.getHmParam(device, type, false);
		Label result = new Label(vh.getParent(), WidgetHelper.getValidWidgetId("paramLabel"+type.toString()+id), req) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String text = ValueResourceUtils.getValue(sres);
				setText(text, req);
				String textCt = ValueResourceUtils.getValue(sresCt);
				if(!text.equals(textCt))
					addStyle(LabelData.BOOTSTRAP_ORANGE, req);
				else
					removeStyle(LabelData.BOOTSTRAP_ORANGE, req);
			}
		};
		result.setPollingInterval(DeviceTableRaw.DEFAULT_POLL_RATE, req);
		row.addCell(WidgetHelper.getValidWidgetId(colName), result);
		return result;
	}
}
