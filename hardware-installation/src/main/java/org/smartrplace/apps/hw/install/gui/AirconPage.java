package org.smartrplace.apps.hw.install.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.api.PropType;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.devices.buildingtechnology.AirConditioner;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.util.virtualdevice.ChartsUtil;
import org.smartrplace.util.virtualdevice.ChartsUtil.GetPlotButtonResult;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;
import de.iwes.widgets.html.form.textfield.TextField;

@SuppressWarnings("serial")
public class AirconPage extends MainPage {

	DeviceTableBase devTable;
	
	public enum AirconPageType {
		STANDARD_VIEW_ONLY
	}
	protected final AirconPageType type;
	
	@Override
	public String getHeader() {
		switch(type) {
		case STANDARD_VIEW_ONLY:
			return "Airconditioner Page";
		}
		throw new IllegalStateException("Unknown type:"+type);
	}

	static Boolean isAllAllowed = null;
	@Override
	protected boolean isAllOptionAllowedSuper(OgemaHttpRequest req) {
		if(isAllAllowed == null) {
			int thermNum = appMan.getResourceAccess().getResources(AirConditioner.class).size();
			isAllAllowed = (thermNum <= ThermostatPage.THERMOSTAT_MAX_FOR_ALL);
		}
		return isAllAllowed;
	}
	
	public AirconPage(WidgetPage<?> page, HardwareInstallController controller,
			AirconPageType type) {
		super(page, controller, false);
		this.type = type;
		finishConstructor();		
	}

	@Override
	protected void finishConstructor() {
		page.append(alert).linebreak();
		StaticTable secondTopTable = new StaticTable(1, 5);
		ButtonConfirm updateAll = new ButtonConfirm(page, "updateallFbFaulty", "Resend OperationMode") {
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				List<InstallAppDevice> all = MainPage.getDevicesSelectedDefault(null, controller, roomsDrop, typeFilterDrop, req);
				int count = resendOperationMode(all, null);
				alert.showAlert("Sent operationMode update to "+count+" aircons", count>0, req);
			}
		};
		//updateAll.triggerOnPOST(alert);
		updateAll.registerDependentWidget(alert);
		updateAll.setDefaultConfirmMsg("Really re-send emptyPos value to all selected thermostats with pending feedback?");
		secondTopTable.setContent(0, 0, updateAll);
		page.append(secondTopTable);

		devTable = new DeviceTableBase(page, controller.appManPlus, alert, this, null) {
			private Label addControlFeedbackLabel(String widgetId, SingleValueResource control, SingleValueResource feedback,
					String lastContactWidgetId,
					ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row) {
				if(req == null) {
					vh.registerHeaderEntry(widgetId);
					vh.registerHeaderEntry(lastContactWidgetId);
					return null;
				}
				if(!(control.exists() || feedback.exists()))
					return null;
				ControlFeedbackFormatter formatter;
				if(control instanceof FloatResource) {
					formatter = new ControlFeedbackFormatter((FloatResource)control, (FloatResource)feedback, controller.dpService);
				} else if(control instanceof StringResource) {
					formatter = new ControlFeedbackFormatter((StringResource)control, (StringResource)feedback, controller.dpService);
				} else if(control instanceof BooleanResource) {
					formatter = new ControlFeedbackFormatter((BooleanResource)control, (BooleanResource)feedback, controller.dpService);
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
					String id, OgemaHttpRequest req, Row row,
					final float minAllowed, final float maxAllowed) {
				TextField setpointSet = null;
				if(req != null) {
					setpointSet = new TextField(mainTable, widgetId+id, req) {
						private static final long serialVersionUID = 1L;
						@Override
						public void onGET(OgemaHttpRequest req) {
							setValue(String.format("%.1f", control.getValue()), req);
						}
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							String val = getValue(req);
							val = val.replaceAll("[^\\d.]", "");
							try {
								float value  = Float.parseFloat(val);
								if((value < minAllowed || value > maxAllowed)) {
									alert.showAlert("Outside allowed range", false, req);
								} else
									control.setValue(value);
							} catch (NumberFormatException | NullPointerException e) {
								if(alert != null) alert.showAlert("Entry "+val+" could not be processed!", false, req);
								return;
							}
						}
					};
					row.addCell(WidgetHelper.getValidWidgetId(widgetId), setpointSet);
				} else
					vh.registerHeaderEntry(widgetId);
				return setpointSet;
			}

			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, final ApplicationManager appMan) {
				//if(!(object.device() instanceof Thermostat) && (req != null)) return null;
				final AirConditioner device;
				if(req == null)
					device = ResourceHelper.getSampleResource(AirConditioner.class);
				else
					device = (AirConditioner) object.device().getLocationResource();
				//if(!(object.device() instanceof AirConditioner)) return;
				final String name;
				if(device.getLocation().toLowerCase().contains("homematic")) {
					name = "AirConditioner HM:"+ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
				} else
					name = ResourceUtils.getHumanReadableShortName(device);
				vh.stringLabel("Name", id, name, row);
				vh.stringLabel("ID", id, object.deviceId().getValue(), row);
				//final Label setpointFB;
				//setpointFB = vh.floatLabel("Setpoint", id, device.temperatureSensor().deviceFeedback().setpoint(), row, "%.1f");
				//Label lastContactFB = addLastContact("Last FB", vh, "FB"+id, req, row,device.temperatureSensor().deviceFeedback().setpoint());
				
				addControlFeedbackLabel("Setp", device.temperatureSensor().settings().setpoint(), device.temperatureSensor().deviceFeedback().setpoint(),
						"LastSetp", vh, id, req, row);
				addSetpEditField("EditSetp", device.temperatureSensor().settings().setpoint(), vh, id, req, row, 273.15f, 313.15f);

				addControlFeedbackLabel("OnOff", device.onOffSwitch().stateControl(), device.onOffSwitch().stateFeedback(),
						"LastOnOff", vh, id, req, row);
				vh.booleanEdit("EditOnOff", id, device.onOffSwitch().stateControl(), row);

				addControlFeedbackLabel("Fan Speed", device.fan().setting().stateControl(), device.fan().setting().stateFeedback(),
						"LastFan", vh, id, req, row);
				addSetpEditField("EditSpeed", device.fan().setting().stateControl(), vh, id, req, row, 0, 999);
				addControlFeedbackLabel("Operation Mode", device.operationMode().stateControl(), device.operationMode().stateFeedback(),
						"LastOp", vh, id, req, row);
				addSetpEditField("EditOp", device.operationMode().stateControl(), vh, id, req, row, 0, 7);
				
				/*if(req != null) {
					setpointFB.setPollingInterval(DEFAULT_POLL_RATE, req);
					lastContactFB.setPollingInterval(DEFAULT_POLL_RATE, req);
				}*/
				
				if(req != null) {
					final GetPlotButtonResult logResultSpecial = getAirConditionerPlotButton(device, appManPlus, vh, id, row, req,
							ScheduleViewerConfigProvThermDebug.getInstance());
					row.addCell(WidgetHelper.getValidWidgetId("TH-Plot"), logResultSpecial.plotButton);

					DeviceHandlerProviderDP<Resource> pe = controller.dpService.getDeviceHandlerProvider(object);
					final GetPlotButtonResult logResult = ChartsUtil.getPlotButton(id, object, appManPlus.dpService(), appMan, false, vh, row, req, pe,
							ScheduleViewerConfigProvThermDebug.getInstance(), null);
					row.addCell("Plot", logResult.plotButton);
				} else {
					vh.registerHeaderEntry("TH-Plot");
					vh.registerHeaderEntry("Plot");
				}
			}
			
			@Override
			protected String id() {
				return "AirConditionerDetails";
			}
			
			@Override
			public String getTableTitleRaw() {
				return "";
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return AirConditioner.class;
			}
			
			@Override
			public List<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
				List<InstallAppDevice> all = MainPage.getDevicesSelectedDefault(null, controller, roomsDrop, typeFilterDrop, req);
				//List<InstallAppDevice> all = appSelector.getDevicesSelected();
				List<InstallAppDevice> result = new ArrayList<InstallAppDevice>();
				for(InstallAppDevice dev: all) {
					if(dev.device() instanceof AirConditioner) {
						result.add(dev);
					}
				}
				return filterByIdRange(all, result);
				//return result;
			}
		};
		devTable.triggerPageBuild();
		typeFilterDrop.registerDependentWidget(devTable.getMainTable());
		
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
	
	public static Datapoint addDpToChart(SingleValueResource sres, List<Datapoint> plotTHDps, DatapointService dpService) {
		Datapoint dp = dpService.getDataPointAsIs(sres);
		if(dp != null)
			plotTHDps.add(dp);
		return dp;
	}
	
	public static GetPlotButtonResult getAirConditionerPlotButton(AirConditioner dev, ApplicationManagerPlus appManPlus,
			ObjectResourceGUIHelper<?, ?> vh, String lineId, Row row, OgemaHttpRequest req,
			DefaultScheduleViewerConfigurationProviderExtended schedViewProv) {
		DatapointService dpService = appManPlus.dpService();
		List<Datapoint> plotTHDps = new ArrayList<>();
		dev = dev.getLocationResource();
		addDpToChart(dev.temperatureSensor().reading(), plotTHDps, dpService);
		addDpToChart(dev.temperatureSensor().settings().setpoint(), plotTHDps, dpService);
		addDpToChart(dev.temperatureSensor().deviceFeedback().setpoint(), plotTHDps, dpService);
		addDpToChart(dev.operationMode().stateControl(), plotTHDps, dpService);
		addDpToChart(dev.operationMode().stateFeedback(), plotTHDps, dpService);

		VoltageResource batteryVoltage = DeviceHandlerBase.getBatteryVoltage(dev);
		if(batteryVoltage != null)
			addDpToChart(batteryVoltage, plotTHDps, dpService);
		
		final GetPlotButtonResult logResultSpecial = ChartsUtil.getPlotButton("_THSpec_"+lineId, null, appManPlus.dpService(), appManPlus.appMan(), false, vh, row, req, null,
				schedViewProv, null, plotTHDps);
		return logResultSpecial;
	}
	
	public static int resendOperationMode(Collection<InstallAppDevice> all, DatapointService dpService) {
		if(all == null)
			all = dpService.managedDeviceResoures(AirConditioner.class);
		int count = 0;
		for(InstallAppDevice dev: all) {
			if(dev.device() instanceof AirConditioner) {
				AirConditioner device = (AirConditioner) dev.device().getLocationResource();
				final FloatResource control = device.operationMode().stateControl();
				final FloatResource feedback = device.operationMode().stateFeedback();
				if(!(control.isActive() && feedback.exists()))
					continue;
				float ctVal = control.getValue();
				float fbVal = feedback.getValue();
				if(ValueResourceHelper.isAlmostEqual(ctVal, fbVal))
					continue;
				control.setValue(ctVal);
				count++;
			}
		}
		return count;
	}
}
