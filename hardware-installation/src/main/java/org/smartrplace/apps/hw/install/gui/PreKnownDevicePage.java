package org.smartrplace.apps.hw.install.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.PreKnownDeviceData;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.directresourcegui.GUIHelperExtension;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.OGEMAResourceCopyHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;
import de.iwes.widgets.html.form.textfield.TextField;

@SuppressWarnings("serial")
public class PreKnownDeviceTable extends ObjectGUITablePage<PreKnownDeviceData, PreKnownDeviceData> {
	protected final HardwareInstallController controller;
	protected static final String SERIAL_NUMBER_HEAD = "Last 4 digits of serial number";
	protected static final String DEVICE_ID_HEAD = "DeviceId number";
	protected static final String ROOM_HEADER = "Room (if known)";
	protected static final String LOCATION_HEADER = "Location (if known)";
	
	public PreKnownDeviceTable(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller.appMan, PreKnownDeviceData.class, false);
		this.controller = controller;
		triggerPageBuild();
	}

	@Override
	public void addWidgets(final PreKnownDeviceData object,
			ObjectResourceGUIHelper<PreKnownDeviceData, PreKnownDeviceData> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		if(req == null) {
			vh.registerHeaderEntry(SERIAL_NUMBER_HEAD);
			vh.registerHeaderEntry(DEVICE_ID_HEAD);
			vh.registerHeaderEntry(ROOM_HEADER);
			vh.registerHeaderEntry(LOCATION_HEADER);
			vh.registerHeaderEntry("Comment");
			vh.registerHeaderEntry("Add/delete");
			return;
		}
		//vh.stringEdit(SERIAL_NUMBER_HEAD, id, object.deviceEndCode(), row, alert);
		
		TextField serialEdit = new TextField(mainTable, "serialEdit"+id, req) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				setValue(object.deviceEndCode().getValue(), req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				String val = getValue(req).toUpperCase();
				if(!object.deviceEndCode().exists()) {
					object.deviceEndCode().create();
					object.deviceEndCode().setValue(val);
					object.deviceEndCode().activate(true);
				} else {
					object.deviceEndCode().setValue(val);
				}
				if(alert != null) alert.showAlert("New value: " + val, true, req);
				
			}
		};
		serialEdit.registerDependentWidget(serialEdit);
		row.addCell(WidgetHelper.getValidWidgetId(SERIAL_NUMBER_HEAD), serialEdit);

		
		TextField deviceIdEdit = new TextField(mainTable, "deviceIdEdit"+id, req) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				setValue(object.deviceIdNumber().getValue(), req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				String val = getValue(req);
				if(val.length() < 4) {
					try {
						int devNum = Integer.parseInt(val);
						if(devNum >= 0) {
							val = String.format("%04d", devNum);
						}
					} catch(NumberFormatException e) {}
				}
				if(!object.deviceIdNumber().exists()) {
					object.deviceIdNumber().create();
					object.deviceIdNumber().setValue(val);
					object.deviceIdNumber().activate(true);
				} else {
					object.deviceIdNumber().setValue(val);
				}
				if(alert != null) alert.showAlert("New value: " + val, true, req);
				
			}
		};
		deviceIdEdit.registerDependentWidget(deviceIdEdit);
		row.addCell(WidgetHelper.getValidWidgetId(DEVICE_ID_HEAD), deviceIdEdit);
		//vh.stringEdit(DEVICE_ID_HEAD, id, object.deviceIdNumber(), row, alert);
		DeviceTableRaw.addRoomWidgetStatic(vh, id, req, row, controller.appMan, object.room(), ROOM_HEADER);
		vh.stringEdit(LOCATION_HEADER, id, object.installationLocation(), row, alert);
		vh.stringEdit("Comment", id, object.comment(), row, alert);
		if(object.getLocation().startsWith("EvalCollection")) {
			Button addButton = new Button(mainTable, "addButton"+id, "Add", req) {
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					String serialNumber = object.deviceEndCode().getValue().trim();
					if(serialNumber.length() < 4) {
						alert.showAlert("Serial number has less than 4 digits!", false, req);
						return;
					}
					String deviceId = object.deviceIdNumber().getValue().trim();
					if(deviceId.isEmpty()) {
						alert.showAlert("DeviceId cannot be empty!", false, req);
						return;												
					}
					Integer devNum = null;
					if(deviceId.length() <= 4) {
						try {
							devNum = Integer.parseInt(deviceId);
							if(devNum >= 0) {
								deviceId = String.format("%04d", devNum);
								object.deviceIdNumber().setValue(deviceId);
							}
						} catch(NumberFormatException e) {}
					}
					ResourceList<PreKnownDeviceData> preResList = controller.appConfigData.preKnownDevices();
					List<PreKnownDeviceData> allExist = preResList.getAllElements();
					for(PreKnownDeviceData pre: allExist) {
						if(serialNumber.equals(pre.deviceEndCode().getValue())) {
							alert.showAlert("Serial number already exists: "+serialNumber, false, req);
							return;													
						}
						if(deviceId.equals(pre.deviceIdNumber().getValue())) {
							alert.showAlert("DeviceId already exists: "+deviceId, false, req);
							return;													
						}
					}
					
					PreKnownDeviceData newEl = ResourceListHelper.getOrCreateNamedElementFlex(serialNumber, preResList, true, false);
					OGEMAResourceCopyHelper.copySubResourceIntoDestination(newEl, object,
							controller.appMan, true);
					
					object.deviceEndCode().setValue("");
					if(devNum == null)
						object.deviceIdNumber().setValue("");
					else
						object.deviceIdNumber().setValue(String.format("%04d", devNum+1));
					//if(object.comment().exists())
					//	object.comment().delete();
					//if(object.room().isReference(false))
					//	object.room().delete();
				}
			};
			row.addCell(WidgetHelper.getValidWidgetId("Add/delete"), addButton);
			addButton.registerDependentWidget(alert);
			addButton.registerDependentWidget(mainTable);
			addButton.registerDependentWidget(serialEdit);
			addButton.registerDependentWidget(deviceIdEdit);
			return;
		} else {
			GUIHelperExtension.addDeleteButton(null, object, mainTable, id, alert, "Add/delete",
					row, vh, req);
		}
		
	}

	@Override
	public PreKnownDeviceData getResource(PreKnownDeviceData object, OgemaHttpRequest req) {
		return object;
	}

	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "header", "Preknown Devices");
		header.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_LEFT);
		page.append(header).linebreak();
		
		DeviceTypeFilterDropdown typeFilterDrop = new DeviceTypeFilterDropdown(page, "devTypeFilterDrop",
				OptionSavingMode.PER_USER, appMan, controller.dpService);
		StaticTable topTable = new StaticTable(1, 7, new int[] {2, 2, 1, 2, 1, 2, 2});
		topTable.setContent(0, 0, "")
				.setContent(0, 1, "")
				.setContent(0, 3, typeFilterDrop);
		page.append(topTable);
	}	

	@Override
	public Collection<PreKnownDeviceData> getObjectsInTable(OgemaHttpRequest req) {
		List<PreKnownDeviceData> result = new ArrayList<>(controller.appConfigData.preKnownDevices().getAllElements());
		PreKnownDeviceData addEl = ResourceHelper.getSampleResource(PreKnownDeviceData.class);
		result.add(0, addEl);
		//result.add(addEl);
		return result;
	}

	@Override
	public String getLineId(PreKnownDeviceData object) {
		if(object.getLocation().startsWith("EvalCollection"))
			return "0000"+super.getLineId(object);
		return object.deviceIdNumber().getValue()+super.getLineId(object);
	}
}
