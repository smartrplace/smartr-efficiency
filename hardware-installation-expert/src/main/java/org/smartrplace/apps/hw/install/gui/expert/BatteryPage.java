package org.smartrplace.apps.hw.install.gui.expert;

import java.util.ArrayList;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.model.actors.MultiSwitch;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.util.directobjectgui.LabelFormatter;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.util.virtualdevice.ChartsUtil;
import org.smartrplace.util.virtualdevice.ChartsUtil.GetPlotButtonResult;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Label;

public class BatteryPage extends MainPage {

	DeviceTableBase devTable;
	
	@Override
	public String getHeader() {return "Battery Overview";}

	public BatteryPage(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller, false);
		finishConstructor();
		
		/*StaticTable configTable = new StaticTable(3, 2);
		TimeResourceTextField retardEdit = new TimeResourceTextField(page, "retardEdit", Interval.hours);
		BatteryAlarmExtensionData config = ResourceListHelper.getOrCreateNamedElementFlex(controller.appConfigData.alarmingConfig(),
				BatteryAlarmExtensionData.class);
		retardEdit.selectDefaultItem(config.alarmRetard());
		configTable.setContent(0, 0, "Low time before sending alarm (hours)").setContent(0, 1, retardEdit);
		page.append(configTable);*/
	}

	@Override
	protected void finishConstructor() {
		devTable = new DeviceTableBase(page, controller.appManPlus, alert, this, null) {
			//@Override
			//public String getLineId(InstallAppDevice object) {
			//	return object.getName()+"_BAT";
			//}
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				final PhysicalElement device2 = addNameWidget(object, vh, id, req, row, appMan).getLocationResource();
				AddBatteryVoltageResult voltageLab = addBatteryVoltage(vh, id, req, row, device2);
				Label lastContactVoltage = null;
				Label lastContactStatus = null;
				if(req == null)
					vh.registerHeaderEntry("Last Voltage");
				else if(voltageLab != null)
					lastContactVoltage = addLastContact("Last Voltage", vh, "LV"+id, req, row, voltageLab.reading);
				AddBatteryVoltageResult statusLab = addBatteryStatus(vh, id, req, row, device2);
				if(req == null)
					vh.registerHeaderEntry("Last Status");
				else if(statusLab != null)
					lastContactStatus = addLastContact("Last Status", vh, "LStat"+id, req, row, statusLab.reading);
				if(req == null) {
					vh.registerHeaderEntry("Room-Loc");
					vh.registerHeaderEntry("EmptyPos");
					vh.registerHeaderEntry("Last EP");
					vh.registerHeaderEntry("Resend EmptyPos");
					vh.registerHeaderEntry("Plot");
				} else {
					Room deviceRoom = device2.location().room();
					//addRoomWidget(vh, id, req, row, appMan, deviceRoom);
					String roomSubLoc = ResourceUtils.getHumanReadableShortName(deviceRoom);
					if(object.installationLocation().exists() && !object.installationLocation().getValue().isEmpty()) {
						roomSubLoc += "-"+object.installationLocation().getValue();
					}
					vh.stringLabel("Room-Loc", id, roomSubLoc, row);
					
					DeviceHandlerProviderDP<Resource> pe = controller.dpService.getDeviceHandlerProvider(object);
					final GetPlotButtonResult logResult = ChartsUtil.getPlotButton(id, object, appManPlus.dpService(), appMan, false, vh, row, req, pe,
							ScheduleViewerConfigProvBattery.getInstance(), null);
					row.addCell("Plot", logResult.plotButton);

					if(object.device() instanceof Thermostat) {
						Thermostat device = (Thermostat)object.device();
						final FloatResource errorRun = device.valve().getSubResource("errorRunPosition", MultiSwitch.class).stateControl();
						final FloatResource errorRunFb = device.valve().getSubResource("errorRunPosition", MultiSwitch.class).stateFeedback();
						if(errorRun.exists()) {
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
							
							@SuppressWarnings("serial")
							Button resend = new Button(mainTable, "resendBut"+id, req) {
								@Override
								public void onPOSTComplete(String data, OgemaHttpRequest req) {
									errorRun.setValue(errorRun.getValue());
								}
							};
							resend.setDefaultText("Resend");
							row.addCell(WidgetHelper.getValidWidgetId("Resend EmptyPos"), resend);
						}
					}
				}
				
				//addSubLocation(object, vh, id, req, row);
				if(req != null) {
					String text = getHomematicCCUId(object.device().getLocation());
					vh.stringLabel("RT", id, text, row);
				} else
					vh.registerHeaderEntry("RT");	
				
				if(voltageLab != null)
					voltageLab.label.setPollingInterval(DEFAULT_POLL_RATE, req);
				if(statusLab != null)
					statusLab.label.setPollingInterval(DEFAULT_POLL_RATE, req);
				if(lastContactVoltage != null)
					lastContactVoltage.setPollingInterval(DEFAULT_POLL_RATE, req);
				if(lastContactStatus != null)
					lastContactStatus.setPollingInterval(DEFAULT_POLL_RATE, req);
			}
			
			@Override
			public PhysicalElement addNameWidget(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				return addNameWidgetStatic(object, vh, id, req, row, appManPlus);
			}	

			@Override
			protected String id() {
				return "BatteryDetails";
			}
			
			@Override
			public String getTableTitleRaw() {
				return "Battery State Overview";
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return PhysicalElement.class;
			}
			
			@Override
			public List<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
				List<InstallAppDevice> all = MainPage.getDevicesSelectedDefault(null, controller, roomsDrop, typeFilterDrop, req);
				//List<InstallAppDevice> all = appSelector.getDevicesSelected();
				List<InstallAppDevice> result = new ArrayList<InstallAppDevice>();
				for(InstallAppDevice dev: all) {
					PhysicalElement device2 = dev.device().getLocationResource();
					VoltageResource batteryVoltage = DeviceHandlerBase.getBatteryVoltage(device2); //ResourceHelper.getSubResourceOfSibbling(device2,
							//"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "battery/internalVoltage/reading", VoltageResource.class);
					if(batteryVoltage != null && (!device2.getLocation().contains("HM_HmIP_HAP"))
							&& (!device2.getLocation().contains("HM_HmIP_BS"))
							&& (!device2.getLocation().contains("HM_HmIP_SCTH230"))
							&& (!device2.getLocation().contains("HM_HmIP_FSM")))
						result.add(dev);
				}
				return result;
			}
		};
		devTable.triggerPageBuild();
		typeFilterDrop.registerDependentWidget(devTable.getMainTable());
		
	}
	
	@Override
	public boolean sortByRoom() {
		return true;
	}

	public static PhysicalElement addNameWidgetStatic(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManagerPlus appManPlus) {
		final PhysicalElement device;
		if(req == null) {
			device = ResourceHelper.getSampleResource(SensorDevice.class);
		} else
			device = object.device().getLocationResource();
		final String name;
		name = DeviceTableRaw.getName(object, appManPlus); //ResourceUtils.getHumanReadableShortName(device);
		vh.stringLabel("Name", id, name, row);
		vh.stringLabel("ID", id, object.deviceId().getValue(), row);
		return device;
	}
}
