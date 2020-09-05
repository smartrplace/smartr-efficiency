package org.smartrplace.driverhandler.more;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.model.locations.Room;
import org.ogema.simulation.shared.api.RoomInsideSimulationBase;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;
import de.smartrplace.ghl.ogema.resources.GhlWaterPond;
import de.smartrplace.ghl.ogema.resources.PondSensorReading;

//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class GhlWaterPondDeviceHandler extends DeviceHandlerBase<GhlWaterPond> {

	private final ApplicationManagerPlus appMan;

	public GhlWaterPondDeviceHandler(ApplicationManagerPlus appMan) {
		this.appMan = appMan;
		appMan.getLogger().info("{} created :)", this.getClass().getSimpleName());
	}
	
	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert, InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector, this) {

			@Override
			public void addWidgets(InstallAppDevice object,
					ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req,
					Row row, ApplicationManager appMan) {
				id = id + "_DeviceHandlerThermostat";  // avoid duplicates for now
				addWidgetsInternal(object, vh, id, req, row, appMan);
				appSelector.addWidgetsExpert(object, vh, id, req, row, appMan);
			}

			@Override
			protected Class<? extends Resource> getResourceType() {
				return GhlWaterPondDeviceHandler.this.getResourceType();
			}

			@Override
			protected String id() {
				return GhlWaterPondDeviceHandler.this.id();
			}

			@Override
			protected String getTableTitle() {
				return "GHL Profilux Devices";
			}
			
			public GhlWaterPond addWidgetsInternal(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				//if(!(object.device() instanceof Thermostat) && (req != null)) return null;
				final GhlWaterPond device;
				if(req == null)
					device = ResourceHelper.getSampleResource(GhlWaterPond.class);
				else
					device = (GhlWaterPond) object.device();
				//if(!(object.device() instanceof Thermostat)) return;
				final String name;
				name = ResourceUtils.getHumanReadableShortName(device);
				vh.stringLabel("Name", id, name, row);
				vh.stringLabel("ID", id, object.deviceId().getValue(), row);
				if(req == null) {
					vh.registerHeaderEntry("PH");
					vh.registerHeaderEntry("Last PH");
					vh.registerHeaderEntry("Room");
					vh.registerHeaderEntry("Location");
				} else {
					FloatResource ph = device.readings().getSubResource("pH_Wert_1", PondSensorReading.class).sensor().reading();
					if(ph != null && ph.exists()) {
						Label tempmes = vh.floatLabel("PH", id, ph, row, "%.1f#min:-200");
						Label lastContact = addLastContact("Last PH", vh, id, req, row, ph);
						tempmes.setPollingInterval(DEFAULT_POLL_RATE, req);
						lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);

						Room deviceRoom = device.readings().getSubResource("pH_Wert_1", PondSensorReading.class).location().room();
						addRoomWidget(vh, id, req, row, appMan, deviceRoom);
						addSubLocation(object, vh, id, req, row);
					}
				}
				addInstallationStatus(object, vh, id, req, row);
				addComment(object, vh, id, req, row);
				
				return device;
			}
			
		};
	}

	@Override
	public Class<GhlWaterPond> getResourceType() {
		return GhlWaterPond.class;
	}

	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService) {
		List<Datapoint> result = new ArrayList<>();
		GhlWaterPond dev = (GhlWaterPond) installDeviceRes.device();
		if (null == dev) return result;
		for(PondSensorReading sensor: dev.readings().getAllElements()) {
			result.add(dpService.getDataPointStandard(sensor.sensor().reading()));
		}
		return result;
	}

	@Override
	protected Class<? extends ResourcePattern<GhlWaterPond>> getPatternClass() {
		return GhlWaterPondPattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}

	@Override
	public List<RoomInsideSimulationBase> startSupportingLogicForDevice(InstallAppDevice device, GhlWaterPond dev,
			SingleRoomSimulationBase roomSimulation, DatapointService dpService) {
		
		if (null == dev)
			return super.startSimulationForDevice(device, dev, roomSimulation, dpService);;
		for(PondSensorReading sensor: dev.readings().getAllElements()) {
			FloatResource valRes = sensor.sensor().reading();
			if(valRes.isActive() && (valRes.getValue() == 0)) {
				SampledValue sv = valRes.getHistoricalData().getPreviousValue(Long.MAX_VALUE);
				if(sv != null)
					valRes.setValue(sv.getValue().getFloatValue());
			}
		}

		return super.startSimulationForDevice(device, dev, roomSimulation, dpService);
	}	
}
