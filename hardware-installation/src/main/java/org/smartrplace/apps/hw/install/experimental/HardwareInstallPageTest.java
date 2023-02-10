package org.smartrplace.apps.hw.install.experimental;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.widgets.api.widgets.LazyWidgetPage;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.html.multiselect.TemplateMultiselect;
import de.iwes.widgets.resource.widget.dropdown.ResourceListDropdown;
import de.iwes.widgets.template.DisplayTemplate;

/*
@Component(
		service=LazyWidgetPage.class,
		property= {
				LazyWidgetPage.BASE_URL + "=/org/smartrplace/hardwareinstall-test",
				LazyWidgetPage.RELATIVE_URL + "=index.html",
				LazyWidgetPage.MENU_ENTRY + "=Hardware installation (test)"
		}
)
*/
public class HardwareInstallPageTest /*implements LazyWidgetPage*/ {
	
	private final Collection<DeviceHandlerProvider<?>> deviceHandlers = /*new ConcurrentSkipListSet<DeviceHandlerProvider<?>>()*/
			Collections.synchronizedCollection(new ArrayList<DeviceHandlerProvider<?>>());
	
	/*
	@Reference(
			//target="(" + DEVICE_MNGMT_PROP + "=*)",
			cardinality=ReferenceCardinality.MULTIPLE,
			service=DeviceHandlerProvider.class,
			policy=ReferencePolicy.DYNAMIC,
			policyOption=ReferencePolicyOption.GREEDY,
			bind="addDeviceHandler",
			unbind="removeDeviceHandler"
	)
	*/
	public void addDeviceHandler(DeviceHandlerProvider<?> handler) {
		deviceHandlers.add(handler);
	}
	
	public void removeDeviceHandler(DeviceHandlerProvider<?> handler) {
		deviceHandlers.remove(handler);
	}
	
	public HardwareInstallPageTest(ApplicationManager am, WidgetPage<?> page) {
		final ResourceAccess ra = am.getResourceAccess();

		page.showOverlay(true);
		final Header header = new Header(page, "header", "Device setup and configuration (test)");
		//header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_CENTERED);
		header.setDefaultColor("darkblue");
		
		final ResourceListDropdown<BuildingPropertyUnit> buildings = new ResourceListDropdown<BuildingPropertyUnit>(page,  "buildingsSelector", false);
		buildings.setDefaultList(ra.<ResourceList<BuildingPropertyUnit>>getResource("accessAdminConfig/roomGroups"));
		buildings.setDefaultSelectByUrlParam("roomgroup");
		final String buildingsTooltip = "Select a building or group of rooms, as a filter for the rooms selection";
		buildings.setDefaultToolTip(buildingsTooltip);
		final TemplateMultiselect<Room> rooms = new TemplateMultiselect<Room>(page, "roomSelector") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final ResourceList<InstallAppDevice> knownDevices = ra.getResource("hardwareInstallConfig/knownDevices");
				if (knownDevices == null || !knownDevices.exists())
					return;
				// filtered by selected building and for non-device-empty rooms
				Collection<Room> rooms = knownDevices.getAllElements().stream()
						.filter(dev -> dev.device().location().room().exists())
						.map(dev -> dev.device().location().room().<Room>getLocationResource())
						.collect(Collectors.toSet());
				final BuildingPropertyUnit building = buildings.getSelectedItem(req);
				if (building != null && building.rooms().exists()) {
					final List<Room> filteredRooms = building.rooms().getAllElements();
					rooms = rooms.stream()
							.filter(room -> filteredRooms.stream().filter(room2 -> room2.equalsLocation(room)).findAny().isPresent()).collect(Collectors.toList());
				}
				update(rooms, req);
			}
			
		};
		rooms.setTemplate(new DisplayTemplate<Room>() {
			
			@Override
			public String getLabel(Room room, OgemaLocale arg1) {
				return room.name().isActive() ? room.name().getValue() : room.getLocation();
			}
			
			@Override
			public String getId(Room room) {
				return room.getLocation();
			}
		});
		rooms.setDefaultSelectByUrlParam("room");
		final String roomsTooltip = "Filter devices by room";
		rooms.setDefaultToolTip(roomsTooltip);
		final TemplateMultiselect<DeviceHandlerProvider<?>> deviceTypes = new TemplateMultiselect<DeviceHandlerProvider<?>>(page, "deviceHandlers") {
			
			// filtered by selected room and availability of devices
			public void onGET(OgemaHttpRequest req) {
				final ResourceList<InstallAppDevice> knownDevices = ra.getResource("hardwareInstallConfig/knownDevices");
				if (knownDevices == null || !knownDevices.exists())
					return;
				final List<Room> room = rooms.getSelectedItems(req);
				Stream<InstallAppDevice> handlerStream = knownDevices.getAllElements().stream();
				if (room != null && !room.isEmpty()) {
					handlerStream = handlerStream.filter(dev ->  {
						final Room deviceRoom = dev.device().location().room();
						if (deviceRoom == null)
							return false;
						return room.stream().filter(r -> r.equalsLocation(deviceRoom)).findAny().isPresent();
					});
				}
				final List<String> applicableHandlers = handlerStream
					.map(dev -> dev.devHandlerInfo())
					.filter(Resource::isActive)
					.map(StringResource::getValue)
					.collect(Collectors.toList());
				final Collection<DeviceHandlerProvider<?>> handlers;
				synchronized (deviceHandlers) {
					handlers = deviceHandlers.stream()
						.filter(dev -> applicableHandlers.contains(dev.id()))
						.collect(Collectors.toList());
				}
				update(handlers, req);
			}
			
		};
		deviceTypes.setDefaultSelectByUrlParam("device");
		final String devicesTooltip = "Filter device type";
		deviceTypes.setDefaultToolTip(devicesTooltip);
		final Button searchForDevices = new Button(page, "searchForDevices", true) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				final BooleanResource installActive = ra.getResource("hardwareInstallConfig/isInstallationActive");
				final boolean active = installActive != null && installActive.isActive() && installActive.getValue();
				setStyles(Collections.singleton(active ? ButtonData.BOOTSTRAP_GREEN : ButtonData.BOOTSTRAP_DEFAULT), req);
			}
			
			@Override
			public void onPOSTComplete(String arg0, OgemaHttpRequest req) {
				final BooleanResource installActive = ra.getResource("hardwareInstallConfig/isInstallationActive");
				if (installActive != null && (!installActive.getValue() || !installActive.isActive())) {
					installActive.<BooleanResource>create().setValue(true);
					installActive.activate(false);					
				}
			}
		};
		searchForDevices.setText("Search for devices", null);
		final DynamicDeviceTablesTest tables = new DynamicDeviceTablesTest(page, "devicesTable", deviceTypes, rooms, () -> ra.getResource("hardwareInstallConfig/knownDevices"));
				
		final Label buildingLabel =  new Label(page, "blab", "Building:", true);
		buildingLabel.setDefaultToolTip(buildingsTooltip);
		final Label roomLabel =  new Label(page, "rlab", " Room:", true);
		roomLabel.setDefaultToolTip(roomsTooltip);
		final Label deviceLabel = new Label(page, "devlab", " Device:", true);
		deviceLabel.setDefaultToolTip(devicesTooltip);
		Arrays.stream(new Label[] {buildingLabel, roomLabel, deviceLabel}).forEach(label -> {
			label.setColor("darkblue", null);
			label.addCssStyle("font-weight", "bold", null);
		});
		final Flexbox selectorsRow = new Flexbox(page, "selectorsRow", true);
		//selectorsRow.setAlignItems(AlignItems.BASELINE, null);
		selectorsRow.setColumnGap("1em", null);
		selectorsRow.setBackgroundColor("lightgrey", null);
		selectorsRow.addCssStyle("padding", "0.5em", null);
		selectorsRow
			.addItem(buildingLabel, null)
			.addItem(buildings, null)
			.addItem(roomLabel, null)
			.addItem(rooms, null)
			.addItem(deviceLabel, null)
			.addItem(deviceTypes, null)
			.addItem(searchForDevices, null);
		
		
		
		page.append(header).append(selectorsRow).linebreak().append(tables); // TODO more
		
		buildings.triggerAction(rooms, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		rooms.triggerAction(deviceTypes, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		rooms.triggerAction(deviceTypes, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		
		rooms.triggerAction(tables, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST); // FIXME this is not working...
		rooms.triggerAction(tables, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		deviceTypes.triggerAction(tables, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		searchForDevices.triggerAction(searchForDevices, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		searchForDevices.triggerAction(rooms, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		
	}
	

}