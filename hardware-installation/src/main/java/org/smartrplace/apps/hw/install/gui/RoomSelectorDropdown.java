package org.smartrplace.apps.hw.install.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.model.locations.Room;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.template.DefaultDisplayTemplate;

public class RoomSelectorDropdown extends TemplateDropdown<String> {
	private static final long serialVersionUID = 1L;
	public static final String ALL_DEVICES_ID = "allDevices";
	public static final String DEVICES_IN_ROOMS_ID = "devicesInRooms";
	public static final String DEVICES_NOT_IN_ROOMS_ID = "devicesNOTInRooms";
	private final ResourceAccess resAcc;
	private final HardwareInstallController controller;
	private final Map<String, Room> knownRooms = new HashMap<String, Room>();
	
	public RoomSelectorDropdown(WidgetPage<?> page, String id, HardwareInstallController controller) {
		super(page, id);
		this.controller = controller;
		this.resAcc = controller.appMan.getResourceAccess();
		setTemplate(new DefaultDisplayTemplate<String>() {
			@Override
			public String getLabel(String arg0, OgemaLocale arg1) {
				if(arg0.equals(ALL_DEVICES_ID))
					return "All Devices";
				if(arg0.equals(DEVICES_IN_ROOMS_ID))
					return "Devices configured for a room";
				if(arg0.equals(DEVICES_NOT_IN_ROOMS_ID))
					return "Devices NOT configured for a room";
				Room room = knownRooms.get(arg0); 
				//ResourceHelperSP.getSubResource(null, arg0, Room.class, resAcc);
				if(room == null) return ("unknown:arg0");
				return ResourceUtils.getHumanReadableShortName(room);
			}
		});
	}

	@Override
	public void onGET(OgemaHttpRequest req) {
		List<Room> rooms = KPIResourceAccess.getRealRooms(resAcc);
		List<String> items = new ArrayList<>();
		items.add(ALL_DEVICES_ID);
		items.add(DEVICES_IN_ROOMS_ID);
		items.add(DEVICES_NOT_IN_ROOMS_ID);
		for(Room room: rooms) {
			addItem(room, items);
		}
		update(items, req);
		selectItem(controller.appConfigData.room().getValue(), req);
	}
	
	@Override
	public void onPOSTComplete(String data, OgemaHttpRequest req) {
		String item = getSelectedItem(req);
		controller.appConfigData.room().setValue(item);
	}

	protected void addItem(Room room, List<String> items) {
		knownRooms.put(room.getLocation(), room);
		items.add(room.getLocation());
	}
	
	public List<InstallAppDevice> getDevicesSelected() {
		List<InstallAppDevice> devicesSelected = new ArrayList<>();
		String arg0 = controller.appConfigData.room().getValue();
		for(InstallAppDevice dev: controller.appConfigData.knownDevices().getAllElements()) {
			if(arg0.equals(ALL_DEVICES_ID))
				devicesSelected.add(dev);
			else if(arg0.equals(DEVICES_IN_ROOMS_ID)) {
				if(dev.device().location().room().exists())
					devicesSelected.add(dev);
			} else if(arg0.equals(DEVICES_NOT_IN_ROOMS_ID)) {
				if(!dev.device().location().room().exists())
					devicesSelected.add(dev);
			} else {
				if(dev.device().location().room().getLocation().equals(arg0))
					devicesSelected.add(dev);
			}
		}
		return devicesSelected;
	}
}
