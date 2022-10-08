package org.smartrplace.apps.hw.install.gui;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.eval.timeseries.simple.smarteff.KPIResourceAccessSmarEff;
import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.simulation.service.apiplus.SimulationConfigurationModel;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gui.filtering.DualFiltering;
import org.smartrplace.gui.filtering.SingleFilteringDirect;
import org.smartrplace.gui.filtering.util.RoomFiltering2Steps;
import org.smartrplace.hwinstall.basetable.HardwareTablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.virtualdevice.ChartsUtil;
import org.smartrplace.util.virtualdevice.ChartsUtil.GetPlotButtonResult;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import extensionmodel.smarteff.api.common.BuildingUnit;

public class MainPage extends HardwareTablePage { //extends DeviceTablePageFragment
	protected final HardwareInstallController controller;

	public String getHeader() {return "Device Setup and Configuration";}

	public static enum ShowModeHw {
		STANDARD,
		KNI,
		NETWORK
	}
	
	public MainPage(WidgetPage<?> page, final HardwareInstallController controller, boolean triggerFinishConstructorAutomatically) {
		super(page, controller.appManPlus, controller.hwInstApp, controller.hwTableData, triggerFinishConstructorAutomatically);
		this.controller = controller;
	}

	protected DefaultScheduleViewerConfigurationProviderExtended getScheduleViewerExtended() {
		return ScheduleViewerConfigProvHW.getInstance();
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
	public <T extends PhysicalElement> InstallAppDevice addDeviceIfNew(T model, DeviceHandlerProvider<T> tableProvider) {
		return controller.addDeviceIfNew(model, tableProvider);
	}

	@Override
	public <T extends Resource> InstallAppDevice removeDevice(T model) {
		return controller.removeDevice(model);
	}

	@Override
	public <T extends PhysicalElement> void startSimulation(DeviceHandlerProvider<T> tableProvider, T device) {
		controller.startSimulation(tableProvider, device);
	}
	
	private Map<String, SingleRoomSimulationBaseImpl> roomSimulations = new HashMap<>();
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
		synchronized (roomSimulations) {
			roomSimulations.put(room.getLocation(), roomSim);			
		}
		if(simTimer == null) {
			simTimer = appMan.createTimer(5000, new TimerListener() {
				
				@Override
				public void timerElapsed(Timer arg0) {
					synchronized (roomSimulations) {
						long now = appMan.getFrameworkTime();
						for(SingleRoomSimulationBaseImpl sim: roomSimulations.values()) {
							sim.step(now, now - lastTime);
						}
						lastTime = now;	
					}
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
	@Override
	public void addWidgetsExpert(DeviceHandlerProvider<?> tableProvider, InstallAppDevice object,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		if(req == null) {
			vh.registerHeaderEntry("Plot");
			return;
		}
		final GetPlotButtonResult logResult = getPlotButton(id, object, controller, false, vh, row, req, null);
		if(logResult.devHand != null) {
			row.addCell("Plot", logResult.plotButton);
		}
	}
	
	public static List<InstallAppDevice> getDevicesSelectedDefault(DeviceHandlerProvider<?> devHand,
			HardwareInstallController controller,
			RoomFiltering2Steps<InstallAppDevice> roomsDrop,
			SingleFilteringDirect<InstallAppDevice> installFilterDrop,
			OgemaHttpRequest req) {
		Collection<InstallAppDevice> all = controller.getDevices(devHand);
		DualFiltering<Room, InstallAppDevice, InstallAppDevice> finalFilterLoc = new DualFiltering<Room, InstallAppDevice, InstallAppDevice>(roomsDrop, installFilterDrop);
		return finalFilterLoc.getFiltered(all, req);
	}
	
	/** Create widgets for a plotButton. The dataPointInfoLabel is directly added to the row if requested,
	 * the plotButton needs to be added to the row by separate operation
	 * 
	 * @param id
	 * @param controller
	 * @param vh
	 * @param req
	 * @return
	 */
	public static GetPlotButtonResult getPlotButton(String id, InstallAppDevice object,
			final HardwareInstallController controller,
			boolean addDataPointInfoLabel,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, Row row, OgemaHttpRequest req,
			DeviceHandlerProvider<?> devHandForTrash) {
		DeviceHandlerProviderDP<?> devHand;
		if(devHandForTrash != null)
			devHand = devHandForTrash;
		else
			devHand = (DeviceHandlerProvider<?>) controller.getDeviceHandler(object);
		return ChartsUtil.getPlotButton(id, object, controller.dpService, controller.appMan,
				addDataPointInfoLabel, vh, row, req, devHand,
				ScheduleViewerConfigProvHW.getInstance(), controller.datalogs);
	}
}