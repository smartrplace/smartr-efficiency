package org.smartrplace.apps.hw.install.gui.alarm;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.model.extended.alarming.AlarmGroupDataMajor;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmconfig.util.AlarmResourceUtil;
import org.smartrplace.apps.alarmingconfig.gui.MainPage;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.autoconfig.api.InitialConfig;
import org.smartrplace.hwinstall.basetable.HardwareTableData;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.resource.widget.label.ValueResourceLabel;

public class AlarmingDeviceTableBase extends DeviceTableBase {
	protected final String pageTitle;
	protected final HardwareTableData resData;
	protected final Button commitButton;
	protected final boolean showAlarmCtrl;
	protected final boolean showRoomAndLocation;
	
	protected void addAdditionalWidgets(final InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, final ApplicationManager appMan,
			PhysicalElement device, final InstallAppDevice template) {}

	protected void addFrontWidgets(final InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, final ApplicationManager appMan,
			PhysicalElement device) {}

	public AlarmingDeviceTableBase(WidgetPage<?> page, ApplicationManagerPlus appMan, Alert alert,
			final String pageTitle,	final HardwareTableData resData, Button commitButton,
			InstalledAppsSelector appSelector, DeviceHandlerProvider<?> devHand) {
		this(page, appMan, alert, pageTitle, resData, commitButton, appSelector, devHand, true, true);
	}
	
	public AlarmingDeviceTableBase(WidgetPage<?> page, ApplicationManagerPlus appMan, Alert alert,
			final String pageTitle,	final HardwareTableData resData, Button commitButton,
			InstalledAppsSelector appSelector, DeviceHandlerProvider<?> devHand, boolean showAlarmCtrl, boolean showRoomAndLocation) {
		super(page, appMan, alert, appSelector, devHand);
		this.pageTitle = pageTitle;
		this.resData = resData;
		this.commitButton = commitButton;
		this.showAlarmCtrl = showAlarmCtrl;
		this.showRoomAndLocation = showRoomAndLocation;
	}
	
	@Override
	protected String pid() {
		return WidgetHelper.getValidWidgetId(devHand.id());
	}
	
	@Override
	public void addWidgets(InstallAppDevice object,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		id = id + pid();  // avoid duplicates for now
		addWidgetsInternal(object, vh, id, req, row, appMan);
		//appSelector.addWidgetsExpert(object, vh, id, req, row, appMan);
	}

	@Override
	protected Class<? extends Resource> getResourceType() {
		return devHand.getResourceType();
	}

	@Override
	protected String id() {
		return devHand.id();
	}

	/*@Override
	public String getTableTitleInternal() {
		if(devHand instanceof DeviceHandlerSimple) {
			return ((DeviceHandlerSimple<?>)devHand).getTableTitle();
		}
		return pageTitle;
	}*/

	public PhysicalElement addWidgetsInternal(final InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, final ApplicationManager appMan) {

		PhysicalElement device = object.device();
		addFrontWidgets(object, vh, id, req, row, appMan, device);
		addNameWidget(object, vh, id, req, row, appMan);
		final InstallAppDevice template;
		if(req == null) {
			vh.registerHeaderEntry("Active Alarms");
			if (this.showAlarmCtrl)
				vh.registerHeaderEntry("Alarm Control");
			template = null;
		} else {
			int[] alNum = AlarmingConfigUtil.getActiveAlarms(object);
			String activeAlarms = String.format("%d / %d", alNum[0], alNum[1]);
			String tooltip = null;
			if (object != null) {
				// XXX not very efficient to retrieve the AlarmGroupDataMajors again in every row... 
				// but also not so easy to avoid with all this nested structure...
				final long released = appMan.getResourceAccess().getResources(AlarmGroupDataMajor.class).stream()
					.filter(a -> object.equalsLocation(AlarmResourceUtil.getDeviceForKnownFault(a)))
					.filter(a -> !object.knownFault().equals(a))
					.filter(AlarmResourceUtil::isReleased)
					.count();
				if (released > 1) {
					activeAlarms += " **";
					tooltip = "Multiple released major issues exist for this device";
				} else if (released > 0) {
					activeAlarms += " *";
					tooltip = "A released major issue exists for this device";
				}
			}
			final Label alarm = vh.stringLabel("Active Alarms", id, activeAlarms, row);
			if (tooltip != null)
				alarm.setDefaultToolTip(tooltip);

			template = AlarmingConfigUtil.getTemplate(object, appManPlus);
			if(row == null) {
				Room deviceRoom = device.location().room();
				addAdditionalWidgets(object, vh, id, req, row, appMan, deviceRoom, template);
				return device;
			}
			if(template != null && this.showAlarmCtrl) {
				Boolean templateStatus = AlarmingConfigUtil.getAlarmingStatus(template, template);
				if(templateStatus == null)
					throw new IllegalStateException("Template status cannot be null!");
				Button perm;
				if(object.equalsLocation(template)) {
					ButtonConfirm permConf = new ButtonConfirm(mainTable, WidgetHelper.getValidWidgetId("perm_"+id), req) {
						private static final long serialVersionUID = 1L;
						@Override
						public void onGET(OgemaHttpRequest req) {
							Boolean status = AlarmingConfigUtil.getAlarmingStatus(template, object);
							if(status == null)
								throw new IllegalStateException("Template status cannot be null!");
							setText(status ? "✓ Template": "✕ Templ.inactive", req);
							if (status) {
								setStyle(ButtonData.BOOTSTRAP_GREEN, req);
								enable(req);
							} else {
								setStyle(ButtonData.BOOTSTRAP_RED, req);
								disable(req);
							}
							setToolTip("Template is " + (status ? "active" : "inactive") + ".", req);
						}
						
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							Boolean status = AlarmingConfigUtil.getAlarmingStatus(template, object);
							MainPage.hasOpenCommits = true;
							if(status == null || status) {
								//disable all
								AlarmingConfigUtil.disAbleAllOfTemplateType(devHand.id(), appManPlus);
								AlarmingConfigUtil.deactivateAlarms(object);
							}
						}
					};
					permConf.setDefaultConfirmMsg("Really set all devices of the type to inactive? Otherwise select another template first.");
					perm = permConf;
				} else {
					perm = new Button(mainTable, WidgetHelper.getValidWidgetId("perm_"+id), "", req) {
						private static final long serialVersionUID = 1L;
						@Override
						public void onGET(OgemaHttpRequest req) {
							Boolean status = AlarmingConfigUtil.getAlarmingStatus(template, object);
							if (status == null) {
								setStyle(ButtonData.BOOTSTRAP_DEFAULT, req);
								setText("(✓ special)", req);
								setToolTip("Alarming is " + "special.", req);
								
							} else {
								//setGlyphicon(status ? Glyphicons.CHECK : Glyphicons.OFF, req);
								setText(status ? "✓ active": "✕ inactive", req);
								if (status) {
									setStyle(ButtonData.BOOTSTRAP_GREEN, req);
								} else {
									setStyle(ButtonData.BOOTSTRAP_RED, req);
									if(templateStatus)
										enable(req);
									else
										disable(req);
								}
								setToolTip("Alarming is " + (status ? "active" : "inactive") + ".", req);
							}
						}
						
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							Boolean status = AlarmingConfigUtil.getAlarmingStatus(template, object);
							MainPage.hasOpenCommits = true;
							if(status == null || status) {
								AlarmingConfigUtil.deactivateAlarms(object);
							} else {
								AlarmingConfigUtil.copySettings(template, object, appManPlus.appMan(), false);							
							}
						}
					};
				}
				row.addCell(WidgetHelper.getValidWidgetId("Alarm Control"), perm);
				perm.registerDependentWidget(perm);
				perm.registerDependentWidget(commitButton);
			}
		}	
		if (showRoomAndLocation) {
			Room deviceRoom = device.location().room();
			if(deviceRoom == null || (!deviceRoom.exists()))
				vh.stringLabel("Room", id, "--", row);
			else
				vh.stringLabel("Room", id, ResourceUtils.getHumanReadableShortName(deviceRoom), row);
			//addRoomWidget(vh, id, req, row, appMan, deviceRoom);
			
			//addSubLocation(object, vh, id, req, row);
			if(req == null)
				vh.registerHeaderEntry("Location");
			else {
				final ValueResourceLabel<StringResource> locationLabel = new ValueResourceLabel<StringResource>(mainTable, "location" + id, req);
				locationLabel.selectDefaultItem(object.installationLocation());
				row.addCell("Location", locationLabel);
			}
		}
		
		addAdditionalWidgets(object, vh, id, req, row, appMan, device, template);
		
		return device;
	}
	
	public PhysicalElement addNameWidget(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		return addNameWidgetStatic(object, vh, id, req, row, appManPlus.dpService(), devHand, resData.appConfigData);
	}
	public static <T extends Resource> PhysicalElement addNameWidgetStatic(InstallAppDevice object, ObjectResourceGUIHelper<T,T> vh, String id,
			OgemaHttpRequest req, Row row, DatapointService dpService,
			DeviceHandlerProviderDP<?> devHand, HardwareInstallConfig appConfigData) {
		final PhysicalElement device;
		if(req == null)
			device = ResourceHelper.getSampleResource(PhysicalElement.class);
		else
			device = object.device().getLocationResource();
		DatapointGroup devGrp = DpGroupUtil.getDeviceGroup(device.getLocation(), dpService, false);
		String name;
		if(devGrp != null)
			name = devGrp.label(null);
		else
			name = ResourceUtils.getHumanReadableShortName(device);
		if(devHand == null || !InitialConfig.isInitDone(object.deviceId().getValue()+devHand.getInitVersion(), appConfigData.initDoneStatus()))
			name += "*";
		vh.stringLabel("Name", id, name, row);
		vh.stringLabel("ID", id, object.deviceId().getValue(), row);
		return device;
	}	
	
	public static String getDeviceName(InstallAppDevice object, String id,
			DatapointService dpService,
			DeviceHandlerProviderDP<?> devHand, HardwareInstallConfig appConfigData) {
		final PhysicalElement device = object.device().getLocationResource();
		DatapointGroup devGrp = DpGroupUtil.getDeviceGroup(device.getLocation(), dpService, false);
		String name;
		if(devGrp != null)
			name = devGrp.label(null);
		else
			name = ResourceUtils.getHumanReadableShortName(device);
		if(devHand == null || !InitialConfig.isInitDone(object.deviceId().getValue()+devHand.getInitVersion(), appConfigData.initDoneStatus()))
			name += "*";
		return name;
	}	

}
