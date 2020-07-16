package org.smartrplace.apps.hw.install.gui;

import java.util.HashMap;
import java.util.Map;

import org.ogema.apps.roomlink.localisation.mainpage.RoomLinkDictionary;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.model.locations.Room;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;

public abstract class DeviceTablePageFragment extends DeviceTableBase implements InstalledAppsSelector { //extends ObjectGUITablePage<InstallAppDevice,InstallAppDevice> {
	public static final long DEFAULT_POLL_RATE = 5000;
	
	protected abstract Class<? extends Resource> getResourceType();
	protected String getHeader() {return "Device Setup and Configuration";}
	
	protected HardwareInstallController controller;
	private Header header;
	//private Alert alert;
	protected RoomSelectorDropdown roomsDrop;
	protected InstallationStatusFilterDropdown installFilterDrop;
	protected final InstalledAppsSelector instAppsSelector;
	protected final boolean isParentTable;
	
	public DeviceTablePageFragment(WidgetPage<?> page, HardwareInstallController controller,
			InstalledAppsSelector instAppsSelector, Alert alert) {
			//RoomSelectorDropdown roomsDrop, Alert alert) {
		//super(page, controller.appMan, null, null, InstallAppDevice.class, false, true, alert);
		super(page, controller.appMan, null, instAppsSelector);
		this.controller = controller;
		if(instAppsSelector != null) {
			this.instAppsSelector = instAppsSelector;
			isParentTable = false;
		} else {
			this.instAppsSelector = this;
			isParentTable = true;
		}
		//this.roomsDrop = roomsDrop;
		//retardationOnGET = 2000;
		
		//triggerPageBuild();
	}

	protected void addWidgetsCommon(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		addRoomWidget(object, vh, id, req, row, appMan, deviceRoom);
		addSubLocation(object, vh, id, req, row, appMan, deviceRoom);
		
		Map<String, String> valuesToSet = new HashMap<>();
		valuesToSet.put("0", "unknown");
		valuesToSet.put("1", "Device installed physically");
		valuesToSet.put("10", "Physical installation done including all on-site tests");
		valuesToSet.put("20", "All configuration finished, device is in full operation");
		valuesToSet.put("-10", "Error in physical installation and/or testing (explain in comment)");
		valuesToSet.put("-20", "Error in configuration, device cannot be used/requires action for real usage");
		vh.dropdown("Status", id, object.installationStatus(), row, valuesToSet );
		
		vh.stringEdit("Comment", id, object.installationComment(), row, alert);
		if(req != null) {
			String text = getHomematicCCUId(object.device().getLocation());
			vh.stringLabel("RT", id, text, row);
		} else
			vh.registerHeaderEntry("RT");		
	}
	
	protected IntegerResource addWidgetsCommonExpert(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		//vh.stringEdit("Location", id, object.installationLocation(), row, alert);
		IntegerResource source = ResourceHelper.getSubResourceOfSibbling(object.device().getLocationResource(),
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiDevice", IntegerResource.class);
		vh.intLabel("RSSI", id, source, row, 0);
		return source;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void addWidgetsAboveTable() {
		if(!isParentTable) return;
		header = new Header(page, "header", getHeader());
		header.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_LEFT);
		page.append(header).linebreak();
		
		StaticTable topTable = new StaticTable(1, 6, new int[] {2, 2, 2, 2, 2, 2});
		BooleanResourceButton installMode = new BooleanResourceButton(page, "installMode", "Installation Mode",
				controller.appConfigData.isInstallationActive()) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				super.onPrePOST(data, req);
				controller.checkDemands();
			}
		};
		roomsDrop = new RoomSelectorDropdown(page, "roomsDrop", controller);
		installFilterDrop = new InstallationStatusFilterDropdown(page, "installFilterDrop", controller);
		
		//RedirectButton roomLinkButton = new RedirectButton(page, "roomLinkButton", "Room Administration", "/de/iwes/apps/roomlink/gui/index.html");
		
		RedirectButton calendarConfigButton = new RedirectButton(page, "calendarConfigButton",
				"Calendar Configuration", "/org/smartrplace/apps/smartrplaceheatcontrolv2/extensionpage.html");
		
		topTable.setContent(0, 0, roomsDrop)
				.setContent(0, 1, installFilterDrop)
				.setContent(0, 2, installMode);//setContent(0, 2, roomLinkButton).
		RedirectButton addRoomLink = new RedirectButton(page, "addRoomLink", "Add room", "/org/smartrplace/external/actionadmin/roomconfig.html");
		topTable.setContent(0, 3, addRoomLink);
		//RoomEditHelper.addButtonsToStaticTable(topTable, (WidgetPage<RoomLinkDictionary>) page,
		//		alert, appMan, 0, 3);
		topTable.setContent(0, 5, calendarConfigButton);
		page.append(topTable);
	}
	
	/*@Override
	public List<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
		List<InstallAppDevice> all = roomsDrop.getDevicesSelected();
		if (installFilterDrop != null)  // FIXME seems to always be null here
			all = installFilterDrop.getDevicesSelected(all);
		List<InstallAppDevice> result = new ArrayList<InstallAppDevice>();
		for(InstallAppDevice dev: all) {
			if(getResourceType().isAssignableFrom(dev.device().getResourceType())) {
				result.add(dev);
			}
		}
		return result;
	}*/
	
	@Override
	protected void addWidgetsBelowTable() {
	}

	@Override
	public InstallAppDevice getResource(InstallAppDevice object, OgemaHttpRequest req) {
		return object;
	}
	
	public static String getHomematicCCUId(String location) {
		String[] parts = location.split("/");
		String tail;
		if(parts[0].toLowerCase().startsWith("homematicip")) {
			tail = parts[0].substring("homematicip".length());
		} else {
			if(!parts[0].startsWith("homematic")) return "n/a";
			tail = parts[0].substring("homematic".length());			
		}
		if(tail.isEmpty()) return "102";
		else return tail;
	}


	@Override
	protected String id() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getTableTitle() {
		// TODO Auto-generated method stub
		return null;
	}
}
