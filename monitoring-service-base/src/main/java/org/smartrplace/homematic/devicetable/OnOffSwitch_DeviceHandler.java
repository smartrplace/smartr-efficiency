package org.smartrplace.homematic.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.locations.Room;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;


public class OnOffSwitch_DeviceHandler extends DeviceHandlerBase<OnOffSwitch> {
	private final ApplicationManagerPlus appMan;
	
	public OnOffSwitch_DeviceHandler(ApplicationManagerPlus appMan) {
		this.appMan = appMan;
	}
	
	@Override
	public Class<OnOffSwitch> getResourceType() {
		return OnOffSwitch.class;
	}

	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert,
			InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector, this) {
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

				final OnOffSwitch box = (OnOffSwitch) addNameWidget(object, vh, id, req, row, appMan);

				Room deviceRoom = box.location().room();

				//int nSwitchboxes = box.switchboxes().size();
				//Label switchboxCount = vh.stringLabel("Switchbox Count", id, Integer.toString(nSwitchboxes), row);

				Label stateFB = vh.booleanLabel("StateFB", id, box.stateFeedback(), row, 0);
				Label lastFB = addLastContact("Last FB", vh, "LFB"+id, req, row, box.stateFeedback());
				vh.booleanEdit("Control", id, box.stateControl(), row);
				
				addRoomWidget(vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object, vh, id, req, row);
				addInstallationStatus(object, vh, id, req, row);
				addComment(object, vh, id, req, row);

				appSelector.addWidgetsExpert(object, vh, id, req, row, appMan);
				
				if(stateFB != null)
					stateFB.setDefaultPollingInterval(DEFAULT_POLL_RATE);

				if(lastFB != null)
					lastFB.setDefaultPollingInterval(DEFAULT_POLL_RATE);
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return OnOffSwitch_DeviceHandler.this.getResourceType();
			}
			
			@Override
			protected String id() {
				return OnOffSwitch_DeviceHandler.this.id();
			}

			@Override
			protected String getTableTitle() {
				return "OnOffSwitches without SwitchBoxes";
			}
		};
	}

	@Override
	protected Class<? extends ResourcePattern<OnOffSwitch>> getPatternClass() {
		return OnOffSwitchPattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}
	
	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice appDevice, DatapointService dpService) {
		OnOffSwitch dev = (OnOffSwitch) appDevice.device();
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(dev.stateControl(), result, dpService);
		addDatapoint(dev.stateFeedback(), result, dpService);
		addtStatusDatapointsHomematic(dev, dpService, result);
		return result;
	}
	
	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		OnOffSwitch device = (OnOffSwitch) appDevice.device();
		AlarmingUtiH.setTemplateValues(appDevice, device.stateFeedback(),
			0.0f, 1.0f, 1, 20);
		AlarmingUtiH.addAlarmingHomematic(device, appDevice);
	}
}
