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
import org.ogema.eval.timeseries.simple.smarteff.KPIResourceAccessSmarEff;
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
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;
import de.iwes.widgets.html.form.label.Label;
import extensionmodel.smarteff.api.common.BuildingUnit;

public class MainPage implements InstalledAppsSelector { //extends DeviceTablePageFragment
	protected final HardwareInstallController controller;
	protected final WidgetPage<?> page;
	protected final Alert alert;
	protected final ApplicationManager appMan;

	private Header header;
	protected RoomSelectorDropdown roomsDrop;
	protected InstallationStatusFilterDropdown installFilterDrop;
	protected final InstalledAppsSelector instAppsSelector;
	protected final StaticTable topTable;

	class SubTableData {
		DeviceHandlerProvider<?> pe;
		DeviceTableBase table;
		public SubTableData(DeviceHandlerProvider<?> pe, DeviceTableBase table) {
			this.pe = pe;
			this.table = table;
		}
	}
	protected final List<SubTableData> subTables = new ArrayList<>();
	
	protected String pid() {
		return WidgetHelper.getValidWidgetId(this.getClass().getName());
	}
	protected String getHeader() {return "Device Setup and Configuration";}

	public MainPage(WidgetPage<?> page, final HardwareInstallController controller) {
		this.page = page;
		this.appMan = controller.appMan;
		this.controller = controller;
		//init all widgets
		this.alert = new Alert(page, WidgetHelper.getValidWidgetId("alert"+pid()), "");
		
		this.instAppsSelector = this;

		header = new Header(page, "header", getHeader());
		header.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_LEFT);
		page.append(header).linebreak();
		
		OgemaWidget installMode;
		if(controller.hwInstApp == null) {
			installMode = new Label(page, "installModeLab", "Search for new devices: "+
					(controller.appConfigData.isInstallationActive().getValue()?"active":"inactive"));
		} else {
			installMode = new BooleanResourceButton(page, "installMode", "Search for new devices",
					controller.appConfigData.isInstallationActive()) {
				private static final long serialVersionUID = 1L;
	
				@Override
				public void updateDependentWidgets(OgemaHttpRequest req) {
					for(SubTableData tabData: subTables) {
						if(isObjectsInTableEmpty(tabData.pe)) {
							tabData.table.getMainTable().setWidgetVisibility(false, req);
							tabData.table.getHeaderWidget().setWidgetVisibility(false, req);
						} else {
							tabData.table.getMainTable().setWidgetVisibility(true, req);						
							tabData.table.getHeaderWidget().setWidgetVisibility(true, req);
						}
					}
				}
	
				@Override
				public void onPrePOST(String data, OgemaHttpRequest req) {
					super.onPrePOST(data, req);
					controller.checkDemands();
				}
			};
		}
		roomsDrop = new RoomSelectorDropdown(page, "roomsDrop", controller);
		installFilterDrop = new InstallationStatusFilterDropdown(page, "installFilterDrop", controller);
		
		//RedirectButton roomLinkButton = new RedirectButton(page, "roomLinkButton", "Room Administration", "/de/iwes/apps/roomlink/gui/index.html");
		
		//RedirectButton calendarConfigButton = new RedirectButton(page, "calendarConfigButton",
		//		"Calendar Configuration", "/org/smartrplace/apps/smartrplaceheatcontrolv2/extensionpage.html");
		
		topTable = new StaticTable(1, 6, new int[] {2, 2, 2, 2, 2, 2});
		topTable.setContent(0, 0, roomsDrop)
				.setContent(0, 1, installFilterDrop)
				.setContent(0, 2, installMode);//setContent(0, 2, roomLinkButton).
		RedirectButton addRoomLink = new RedirectButton(page, "addRoomLink", "Add room", "/org/smartrplace/external/actionadmin/roomconfig.html");
		topTable.setContent(0, 3, addRoomLink);
		//RoomEditHelper.addButtonsToStaticTable(topTable, (WidgetPage<RoomLinkDictionary>) page,
		//		alert, appMan, 0, 3);
		//topTable.setContent(0, 5, calendarConfigButton);
		page.append(topTable);
		
		finishConstructor();
	}
	
	protected void finishConstructor() {
		updateTables();
	}
	
	Set<String> tableProvidersDone = new HashSet<>();
	public void updateTables() {
		synchronized(tableProvidersDone) {
		if(controller.hwInstApp != null) for(DeviceHandlerProvider<?> pe: controller.hwInstApp.getTableProviders().values()) {
			//if(isObjectsInTableEmpty(pe))
			//	continue;
			String id = pe.id();
			if(tableProvidersDone.contains(id))
				continue;
			tableProvidersDone.add(id);
			DeviceTableBase tableLoc = pe.getDeviceTable(page, alert, this);
			tableLoc.triggerPageBuild();
			installFilterDrop.registerDependentWidget(tableLoc.getMainTable());
			subTables.add(new SubTableData(pe, tableLoc));
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
	/*protected  List<InstallAppDevice> getObjectsInTable(DeviceHandlerProvider<?> pe) {
		List<InstallAppDevice> all = getDevicesSelected();
		List<InstallAppDevice> result = new ArrayList<InstallAppDevice>();
		for(InstallAppDevice dev: all) {
			if(pe.getResourceType().isAssignableFrom(dev.device().getResourceType())) {
				result.add(dev);
			}
		}
		return result;
	}*/

	@Override
	public List<InstallAppDevice> getDevicesSelected() {
		List<InstallAppDevice> all = roomsDrop.getDevicesSelected();
		if (installFilterDrop != null)  // FIXME seems to always be null here
			all = installFilterDrop.getDevicesSelected(all);
		List<InstallAppDevice> allNonTrash = new ArrayList<>();
		for(InstallAppDevice dev: all) {
			if(!dev.isTrash().getValue())
				allNonTrash.add(dev);
		}
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
	@Override
	public void addWidgetsExpert(InstallAppDevice object,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		//do nothing in base page
	}
}


