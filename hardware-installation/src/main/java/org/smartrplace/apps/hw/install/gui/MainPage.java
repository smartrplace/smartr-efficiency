package org.smartrplace.apps.hw.install.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.apps.roomsim.service.api.RoomSimConfig;
import org.ogema.apps.roomsim.service.api.util.RoomSimConfigPatternI;
import org.ogema.apps.roomsim.service.api.util.SingleRoomSimulationBaseImpl;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.LastContactLabel;
import org.ogema.eval.timeseries.simple.smarteff.KPIResourceAccessSmarEff;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.simulation.service.apiplus.SimulationConfigurationModel;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;
import extensionmodel.smarteff.api.common.BuildingUnit;

public class MainPage extends DeviceTablePageFragment implements InstalledAppsSelector {

	public MainPage(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller, null, null);
		super.addWidgetsAboveTable();
		finishConstructor();
	}
	
	protected void finishConstructor() {
		updateTables();
		DoorWindowSensorTable winSensTable = new DoorWindowSensorTable(page, controller, this, alert);
		winSensTable.triggerPageBuild();
		triggerPageBuild();		
	}
	
	Set<String> tableProvidersDone = new HashSet<>();
	public void updateTables() {
		synchronized(tableProvidersDone) {
		if(controller.hwInstApp != null) for(DeviceHandlerProvider<?> pe: controller.hwInstApp.getTableProviders().values()) {
			if(isObjectsInTableEmpty(pe))
				continue;
			String id = pe.id();
			if(tableProvidersDone.contains(id))
				continue;
			tableProvidersDone.add(id);
			DeviceTableBase tableLoc = pe.getDeviceTable(page, alert, this);
			tableLoc.triggerPageBuild();
		}
		}
	}
	
	protected boolean isObjectsInTableEmpty(DeviceHandlerProvider<?> pe) {
		List<InstallAppDevice> all = getDevicesSelected();
		for(InstallAppDevice dev: all) {
			if(pe.getResourceType().isAssignableFrom(dev.device().getResourceType())) {
				return false;
			}
		}
		return true;
	}
	protected  List<InstallAppDevice> getObjectsInTable(DeviceHandlerProvider<?> pe) {
		List<InstallAppDevice> all = getDevicesSelected();
		List<InstallAppDevice> result = new ArrayList<InstallAppDevice>();
		for(InstallAppDevice dev: all) {
			if(pe.getResourceType().isAssignableFrom(dev.device().getResourceType())) {
				result.add(dev);
			}
		}
		return result;
	}

	
	@Override
	protected Class<? extends Resource> getResourceType() {
		return Thermostat.class;
	}
	
	@Override
	public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		addWidgetsInternal(object, vh, id, req, row, appMan);
	}
	public Thermostat addWidgetsInternal(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		//if(!(object.device() instanceof Thermostat) && (req != null)) return null;
		final Thermostat device;
		if(req == null)
			device = ResourceHelper.getSampleResource(Thermostat.class);
		else
			device = (Thermostat) object.device();
		//if(!(object.device() instanceof Thermostat)) return;
		final String name;
		if(device.getLocation().toLowerCase().contains("homematic")) {
			name = "Thermostat HM:"+ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
		} else
			name = ResourceUtils.getHumanReadableShortName(device);
		vh.stringLabel("Name", id, name, row);
		vh.stringLabel("ID", id, object.deviceId().getValue(), row);
		Label setpointFB = vh.floatLabel("Setpoint", id, device.temperatureSensor().deviceFeedback().setpoint(), row, "%.1f");
		if(req != null) {
			TextField setpointSet = new TextField(mainTable, "setpointSet"+id, req) {
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
			row.addCell("Set", setpointSet);
		} else
			vh.registerHeaderEntry("Set");
		Label tempmes = vh.floatLabel("Measurement", id, device.temperatureSensor().reading(), row, "%.1f#min:-200");
		vh.floatLabel("Battery", id, device.battery().internalVoltage().reading(), row, "%.1f#min:0.1");
		Label lastContact = null;
		if(req != null) {
			lastContact = new LastContactLabel(device.temperatureSensor().reading(), appMan, mainTable, "lastContact"+id, req);
			row.addCell(WidgetHelper.getValidWidgetId("Last Contact"), lastContact);
		} else
			vh.registerHeaderEntry("Last Contact");
		
		addWidgetsCommon(object, vh, id, req, row, appMan, device.location().room());
		if(req != null) {
			tempmes.setPollingInterval(DEFAULT_POLL_RATE, req);
			setpointFB.setPollingInterval(DEFAULT_POLL_RATE, req);
			lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
		}
		return device;
	}
	
	@Override
	public void addWidgetsAboveTable() {
		//super.addWidgetsAboveTable();
		Header headerThermostat = new Header(page, "headerThermostat", "Thermostats");
		headerThermostat.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_LEFT);
		page.append(headerThermostat);
	}

	@Override
	public List<InstallAppDevice> getDevicesSelected() {
		List<InstallAppDevice> all = roomsDrop.getDevicesSelected();
		if (installFilterDrop != null)  // FIXME seems to always be null here
			all = installFilterDrop.getDevicesSelected(all);
		return all;
	}

	@Override
	public InstallAppDevice getInstallResource(Resource device) {
		for(InstallAppDevice dev: controller.appConfigData.knownDevices().getAllElements()) {
			if(dev.device().equalsLocation(device))
				return dev;
		}
		return null;
	}
	
	@Override
	public <T extends Resource> InstallAppDevice addDeviceIfNew(T model, DeviceHandlerProvider<T> tableProvider) {
		return controller.addDeviceIfNew(model, tableProvider);
	}

	@Override
	public <T extends Resource> InstallAppDevice removeDevice(T model) {
		return controller.removeDevice(model);
	}

	//@Override
	//public ApplicationManager getAppManForSimulationStart() {
	//	return appMan;
	//}

	@Override
	public <T extends Resource> void startSimulation(DeviceHandlerProvider<T> tableProvider, T device) {
		controller.startSimulation(tableProvider, device);
	}
	
	Map<String, SingleRoomSimulationBaseImpl> roomSimulations = new HashMap<>();
	Timer simTimer = null;
	long lastTime;
	//Map<String, Timer> roomSimTimers = new HashMap<>();
	@Override
	public <T extends Resource> SingleRoomSimulationBase getRoomSimulation(T model) {
		Room room = ResourceUtils.getDeviceLocationRoom(model);
		if(room == null)
			return null;
		SingleRoomSimulationBaseImpl roomSim = roomSimulations.get(room.getLocation());
		if(roomSim != null)
			return roomSim;

		//TODO: Provide real implementation
		@SuppressWarnings("unchecked")
		ResourceList<SimulationConfigurationModel> simConfig = ResourceHelper.getTopLevelResource("OGEMASimulationConfiguration",
				ResourceList.class, appMan.getResourceAccess());
		final RoomSimConfig roomConfigRes;
		RoomSimConfig unfinal = null;
		for(SimulationConfigurationModel sim: simConfig.getAllElements()) {
			if(!(sim instanceof RoomSimConfig))
				continue;
			RoomSimConfig rsim = (RoomSimConfig)sim;
			if(rsim.target().equalsLocation(room)) {
				unfinal = rsim;
				break;
			}
		}
		if(unfinal != null)
			roomConfigRes = unfinal;
		else {
			roomConfigRes = simConfig.addDecorator(ResourceUtils.getValidResourceName(ResourceUtils.getHumanReadableShortName(room)),
					RoomSimConfig.class);
			roomConfigRes.target().setAsReference(room);
		}
		if(!roomConfigRes.simulatedHumidity().isActive()) {
			ValueResourceHelper.setCreate(roomConfigRes.simulatedHumidity(), 0.55f);
			roomConfigRes.simulatedHumidity().activate(false);
		}
		if(!roomConfigRes.simulatedTemperature().isActive()) {
			ValueResourceHelper.setCreate(roomConfigRes.simulatedTemperature(), 293.15f);
			roomConfigRes.simulatedTemperature().activate(false);
		}
		if(!roomConfigRes.personInRoomNonPersistent().isActive()) {
			ValueResourceHelper.setCreate(roomConfigRes.personInRoomNonPersistent(), 0);
			roomConfigRes.personInRoomNonPersistent().activate(false);
		}
		ValueResourceHelper.setCreate(roomConfigRes.simulationProviderId(), "hardware-installation-roomsim");
		
		
		RoomSimConfigPatternI configPattern = new RoomSimConfigPatternI() {
			
			@Override
			public TemperatureResource simulatedTemperature() {
				return getNonNanFromResource(roomConfigRes.simulatedTemperature(), 293.15f);
			}
			
			@Override
			public FloatResource simulatedHumidity() {
				return getNonNanFromResource(roomConfigRes.simulatedHumidity(), 293.15f);
			}
			
			@Override
			public IntegerResource personInRoomNonPersistent() {
				return roomConfigRes.personInRoomNonPersistent();
			}
		};
		String roomId = room.getLocation();
		final BuildingUnit bu = KPIResourceAccessSmarEff.getRoomConfigResource(roomId, appMan);
		roomSim =  new SingleRoomSimulationBaseImpl(room, configPattern , appMan.getLogger(), false) {

			@Override
			public float getVolume() {
				if(bu != null && bu.groundArea().isActive())
					return bu.groundArea().getValue()*2.8f;
				return 50f;
			}
		};
		roomSimulations.put(room.getLocation(), roomSim);
		if(simTimer == null) {
			simTimer = appMan.createTimer(5000, new TimerListener() {
				
				@Override
				public void timerElapsed(Timer arg0) {
					long now = appMan.getFrameworkTime();
					for(SingleRoomSimulationBaseImpl sim: roomSimulations.values()) {
						sim.step(now, now - lastTime);
					}
					lastTime = now;					
				}
			});
			lastTime = appMan.getFrameworkTime();
		}
		return roomSim;
		//return null;
	}
	
	public static <T extends FloatResource> T getNonNanFromResource(T res, float defaultValue) {
		float val = res.getValue();
		if(Float.isNaN(val)) {
			res.setValue(defaultValue);
		}
		return res;
	}
}


