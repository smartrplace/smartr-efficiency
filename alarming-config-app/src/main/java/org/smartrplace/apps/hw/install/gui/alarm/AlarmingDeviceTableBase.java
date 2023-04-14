package org.smartrplace.apps.hw.install.gui.alarm;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmingconfig.gui.MainPage;
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
	
	protected void addAdditionalWidgets(final InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, final ApplicationManager appMan,
			PhysicalElement device, final InstallAppDevice template) {}

	public AlarmingDeviceTableBase(WidgetPage<?> page, ApplicationManagerPlus appMan, Alert alert,
			final String pageTitle,	final HardwareTableData resData, Button commitButton,
			InstalledAppsSelector appSelector, DeviceHandlerProvider<?> devHand) {
		this(page, appMan, alert, pageTitle, resData, commitButton, appSelector, devHand, true);
	}
	
	public AlarmingDeviceTableBase(WidgetPage<?> page, ApplicationManagerPlus appMan, Alert alert,
			final String pageTitle,	final HardwareTableData resData, Button commitButton,
			InstalledAppsSelector appSelector, DeviceHandlerProvider<?> devHand, boolean showAlarmCtrl) {
		super(page, appMan, alert, appSelector, devHand);
		this.pageTitle = pageTitle;
		this.resData = resData;
		this.commitButton = commitButton;
		this.showAlarmCtrl = showAlarmCtrl;
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

		addNameWidget(object, vh, id, req, row, appMan);
		PhysicalElement device = object.device();
		final InstallAppDevice template;
		if(req == null) {
			vh.registerHeaderEntry("Active Alarms");
			if (this.showAlarmCtrl)
				vh.registerHeaderEntry("Alarm Control");
			template = null;
		} else {
			int[] alNum = AlarmingConfigUtil.getActiveAlarms(object);
			vh.stringLabel("Active Alarms", id, String.format("%d / %d", alNum[0], alNum[1]), row);

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
		Room deviceRoom = device.location().room();
		if(device == null || (!device.exists()))
			vh.stringLabel("Room", id, "--", row);
		else
			vh.stringLabel("Room", id, ResourceUtils.getHumanReadableShortName(deviceRoom), row);
		//addRoomWidget(vh, id, req, row, appMan, deviceRoom);
		
		//addSubLocation(object, vh, id, req, row);
		vh.registerHeaderEntry("Location");
		final ValueResourceLabel<StringResource> locationLabel = new ValueResourceLabel<StringResource>(page, "location" + id);
		locationLabel.selectDefaultItem(object.installationLocation());
		row.addCell("Location", locationLabel);
		
		addAdditionalWidgets(object, vh, id, req, row, appMan, deviceRoom, template);
		
		return device;
	}
	
	public PhysicalElement addNameWidget(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		final PhysicalElement device;
		if(req == null)
			device = ResourceHelper.getSampleResource(PhysicalElement.class);
		else
			device = object.device().getLocationResource();
		DatapointGroup devGrp = DpGroupUtil.getDeviceGroup(device.getLocation(), appManPlus.dpService(), false);
		String name;
		if(devGrp != null)
			name = devGrp.label(null);
		else
			name = ResourceUtils.getHumanReadableShortName(device);
		if(!InitialConfig.isInitDone(object.deviceId().getValue()+devHand.getInitVersion(), resData.appConfigData.initDoneStatus()))
			name += "*";
		vh.stringLabel("Name", id, name, row);
		vh.stringLabel("ID", id, object.deviceId().getValue(), row);
		return device;
	}	

}
