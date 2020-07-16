package org.smartrplace.apps.hw.install.gui;

import org.ogema.apps.roomlink.localisation.mainpage.RoomLinkDictionary;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.smartrplace.apps.hw.install.HardwareInstallController;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;

public abstract class DeviceTablePageFragmentRaw<T, R extends Resource> extends DeviceTableRaw<T, R> {

	protected abstract Class<? extends Resource> getResourceType();
	protected String getHeader() {return "Smartrplace Hardware InstallationApp";}
	
	protected HardwareInstallController controller;
	private Header header;
	//private Alert alert;
	protected RoomSelectorDropdown roomsDrop;
	protected InstallationStatusFilterDropdown installFilterDrop;
	protected final boolean isParentTable;
	
	public DeviceTablePageFragmentRaw(WidgetPage<?> page, HardwareInstallController controller,
			boolean isParentTable, Alert alert) {
		super(page, controller.appMan, alert, null);
		this.controller = controller;
		this.isParentTable = isParentTable;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void addWidgetsAboveTable() {
		if(!isParentTable) return;
		header = new Header(page, "header", getHeader());
		header.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_CENTERED);
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
	
	@Override
	protected void addWidgetsBelowTable() {
	}

	@Override
	public R getResource(T object, OgemaHttpRequest req) {
		return null;
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
