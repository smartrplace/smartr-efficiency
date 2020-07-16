package org.smartrplace.apps.hw.install.gui;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.LastContactLabel;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.sensors.DoorWindowSensor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;
import de.iwes.widgets.html.form.label.Label;

@Deprecated
public class DoorWindowSensorTable extends DeviceTablePageFragment {

	public DoorWindowSensorTable(WidgetPage<?> page, HardwareInstallController controller,
			InstalledAppsSelector instAppsSelector, Alert alert) {
			//RoomSelectorDropdown roomsDrop, Alert alert) {
		super(page, controller, instAppsSelector, alert);
	}
	
	@Override
	protected Class<? extends Resource> getResourceType() {
		return DoorWindowSensor.class;
	}
	
	@Override
	public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		addWidgetsInternal(object, vh, id, req, row, appMan);
	}
	public DoorWindowSensor addWidgetsInternal(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		final DoorWindowSensor device = addNameWidget(object, vh, id, req, row, appMan);
		Label state = vh.booleanLabel("Measured State", id, device.reading(), row, 0);
		vh.floatLabel("Battery", id, device.battery().internalVoltage().reading(), row, "%.1f#min:0.1");
		Label lastContact = null;
		if(req != null) {
			lastContact = new LastContactLabel(device.reading(), appMan, mainTable, "lastContact"+id, req);
			row.addCell(WidgetHelper.getValidWidgetId("Last Contact"), lastContact);
		} else
			vh.registerHeaderEntry("Last Contact");
		addWidgetsCommon(object, vh, id, req, row, appMan, device.location().room());
		if(req != null) {
			state.setPollingInterval(DEFAULT_POLL_RATE, req);
			lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
		}
		return device;
	}
	public DoorWindowSensor addNameWidget(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		//if(!(object.device() instanceof DoorWindowSensor) && (req != null)) return null;
		final DoorWindowSensor device;
		if(req == null)
			device = ResourceHelper.getSampleResource(DoorWindowSensor.class);
		else
			device = (DoorWindowSensor) object.device();
		//if(!(object.device() instanceof Thermostat)) return;
		final String name;
		if(device.getLocation().toLowerCase().contains("homematic")) {
			name = "WindowSens HM:"+ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
		} else
			name = ResourceUtils.getHumanReadableShortName(device);
		vh.stringLabel("Name", id, name, row);
		vh.stringLabel("ID", id, object.deviceId().getValue(), row);
		return device;
	}
	@Override
	public void addWidgetsAboveTable() {
		Header headerWinSens = new Header(page, "headerWinSens", "Window and Door Opening Sensors");
		headerWinSens.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_LEFT);
		page.append(headerWinSens);
	}

	@Override
	public List<InstallAppDevice> getDevicesSelected() {
		return instAppsSelector.getDevicesSelected();
	}

	@Override
	public InstallAppDevice getInstallResource(Resource device) {
		return instAppsSelector.getInstallResource(device);
	}
	
	@Override
	public <T extends Resource> InstallAppDevice addDeviceIfNew(T model, DeviceHandlerProvider<T> tableProvider) {
		return instAppsSelector.addDeviceIfNew(model, tableProvider);
	}

	@Override
	public <T extends Resource> InstallAppDevice removeDevice(T model) {
		return instAppsSelector.removeDevice(model);
	}

	@Override
	public <T extends Resource> void startSimulation(DeviceHandlerProvider<T> tableProvider, T device) {
		instAppsSelector.startSimulation(tableProvider, device);
	}
}