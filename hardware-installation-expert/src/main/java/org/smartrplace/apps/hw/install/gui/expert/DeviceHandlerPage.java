package org.smartrplace.apps.hw.install.gui.expert;

import java.io.File;
import java.util.Collection;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceHandlerBase.DeviceByEndcodeResult;
import org.ogema.model.prototypes.PhysicalElement;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.csv.download.generic.CSVRoomExporter;
import org.smartrplace.csv.download.generic.CSVUploadListenerRoom;
import org.smartrplace.csv.upload.generic.CSVUploadWidgets;
import org.smartrplace.csv.upload.generic.CSVUploadWidgets.CSVUploadListener;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.smarteff.defaultservice.TSManagementPage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.logconfig.LogHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.filedownload.FileDownload;
import de.iwes.widgets.html.filedownload.FileDownloadData;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.html5.Flexbox;

@SuppressWarnings("serial")
public class DeviceHandlerPage extends ObjectGUITablePageNamed<DeviceHandlerProviderDP<?>, Resource> {
	private final ApplicationManagerPlus appManPlus;
	private final HardwareInstallController controller;
	private final DatapointService dpService;
	private final HardwareInstallConfig appConfigData;
	
	public final CSVRoomExporter csvRoomExporterFull;
	public final CSVRoomExporter csvRoomExporterFullIncludeInactive;

	public DeviceHandlerPage(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller.appMan, null);
		this.controller = controller;
		this.appManPlus = controller.appManPlus;
		this.dpService = appManPlus.dpService();
		this.appConfigData = appMan.getResourceAccess().getResource("hardwareInstallConfig");

		csvRoomExporterFull = new CSVRoomExporter(true, appManPlus);
		csvRoomExporterFullIncludeInactive = new CSVRoomExporter(true, true, appManPlus);

		triggerPageBuild();
	}

	@Override
	public String getHeader(OgemaLocale locale) {
		return "Device Type Handler Overview";
	}
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		//StaticTable topTable = new StaticTable(1, 6);
		final FileDownload download;
	    download = new FileDownload(page, "downloadcsv", appMan.getWebAccessManager(), true);
	    download.triggerAction(download, TriggeringAction.GET_REQUEST, FileDownloadData.STARTDOWNLOAD);
	    page.append(download);

	    Button exportCSVFull = new Button(page, "exportCSV", "Export CSV (Rooms and Devices)") {
			@Override
	    	public void onPrePOST(String data, OgemaHttpRequest req) {
	    		download.setDeleteFileAfterDownload(true, req);
				String fileStr = csvRoomExporterFull.exportToFile(req);
	    		File csvFile = new File(fileStr);
				download.setFile(csvFile, "rooms_devices.csv", req);
	    	}
		};
		exportCSVFull.triggerAction(download, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);  // GET then triggers download start
		exportCSVFull.triggerOnPOST(alert);

	    Button exportCSVFullIncludeInactive = new Button(page, "exportCSVInclInactive", "Export CSV (incl. inactive/no-handler)") {
			@Override
	    	public void onPrePOST(String data, OgemaHttpRequest req) {
	    		download.setDeleteFileAfterDownload(true, req);
				String fileStr = csvRoomExporterFullIncludeInactive.exportToFile(req);
	    		File csvFile = new File(fileStr);
				download.setFile(csvFile, "rooms_devices_plus.csv", req);
	    	}
		};
		exportCSVFullIncludeInactive.triggerAction(download, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);  // GET then triggers download start
		exportCSVFullIncludeInactive.triggerOnPOST(alert);

		CSVUploadListener listener = new CSVUploadListenerRoom(controller.appConfigData, appManPlus) {
			@Override
			protected DeviceByEndcodeResult<? extends PhysicalElement> getDevice(String serialEndCode, String typeId, String dbLocation) {
				if(dbLocation != null) {
					Resource device = appMan.getResourceAccess().getResource(dbLocation);
					if((device != null) && (device instanceof PhysicalElement))
						return DeviceHandlerBase.getDeviceHandler((PhysicalElement) device, appManPlus);
				}
				return DeviceHandlerBase.getDeviceByEndcode(serialEndCode, typeId, appManPlus);
			}
			
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			protected InstallAppDevice createInstallAppDevice(String serialEndCode, String typeId,
					DeviceByEndcodeResult<? extends PhysicalElement> deviceRes, String proposedDeviceId) {
				if(deviceRes == null)
					return null;
				return controller.addDeviceIfNew(deviceRes.device, (DeviceHandlerProvider)deviceRes.devHand, proposedDeviceId);
			}
		};
		CSVUploadListener listenerIadOnly = new CSVUploadListenerRoom(controller.appConfigData, appManPlus) {
			@Override
			protected DeviceByEndcodeResult<? extends PhysicalElement> getDevice(String serialEndCode, String typeId, String dbLocation) {
				if(dbLocation != null) {
					Resource device = appMan.getResourceAccess().getResource(dbLocation);
					if((device != null) && (device instanceof PhysicalElement))
						return DeviceHandlerBase.getDeviceHandler((PhysicalElement) device, appManPlus);
				}
				return DeviceHandlerBase.getDeviceByEndcode(serialEndCode, typeId, appManPlus);
			}
			
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			protected InstallAppDevice createInstallAppDevice(String serialEndCode, String typeId,
					DeviceByEndcodeResult<? extends PhysicalElement> deviceRes, String proposedDeviceId) {
				if(deviceRes == null)
					return null;
				InstallAppDevice iad = controller.appConfigData.knownDevices().add();
				controller.initializeDevice(iad, deviceRes.device, (DeviceHandlerProvider)deviceRes.devHand,
						controller.validateProposedDeviceId(proposedDeviceId));
				return iad;
			}
		};
		
		CSVUploadWidgets uploadCSV = new CSVUploadWidgets(page, alert, pid(),
				"Import CSV", listener , appMan);
		uploadCSV.uploader.getFileUpload().setDefaultPadding("1em", false, true, false, true);

		CSVUploadWidgets uploadCSVIadOnly = new CSVUploadWidgets(page, alert, pid()+"iadOnly",
				"Create IAD-Resource Only", listenerIadOnly , appMan);
		uploadCSVIadOnly.uploader.getFileUpload().setDefaultPadding("1em", false, true, false, true);

		Label sepLabel = new Label(page, "sepLabel", "-----");
		Flexbox flexLineCSV = TSManagementPage.getHorizontalFlexBox(page, "csvFlex"+pid(),
				exportCSVFull, exportCSVFullIncludeInactive, uploadCSV.csvButton, uploadCSV.uploader.getFileUpload(),
				sepLabel, uploadCSVIadOnly.csvButton, uploadCSVIadOnly.uploader.getFileUpload());
		page.append(flexLineCSV);

		//page.append(topTable);
	}
	
	@Override
	public void addWidgets(DeviceHandlerProviderDP<?> object,
			ObjectResourceGUIHelper<DeviceHandlerProviderDP<?>, Resource> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		addNameLabel(object, vh, id, row, req);
		
		if(req == null) {
			vh.registerHeaderEntry("Title");
			vh.registerHeaderEntry("HandlerID");
			vh.registerHeaderEntry("# Devices");
			vh.registerHeaderEntry("# Trash");
			vh.registerHeaderEntry("Reset Devices");
			return;
		}
		vh.stringLabel("Title", id, object.getTableTitle(), row);
		vh.stringLabel("HandlerID", id, object.id(), row);
		int devNum = 0;
		int trashNum = 0;
		final Collection<InstallAppDevice> devices = dpService.managedDeviceResoures(object.id(), false, true);
		for(InstallAppDevice iad: devices) {
			if(iad.isTrash().getValue())
				trashNum++;
			else
				devNum++;
		}
		vh.stringLabel("# Devices", id, ""+devNum, row);
		vh.stringLabel("# Trash", id, ""+trashNum, row);
		
		final boolean status = appConfigData.blockAutoResetOfDeviceIds().getValue() ||
				(appConfigData.deviceIdManipulationUntil().getValue()==0);
		if(!status) {
			ButtonConfirm autoResetDeviceIds = new ButtonConfirm(mainTable, "autoResetDeviceIds"+id, req) {
				@Override
				public void onGET(OgemaHttpRequest req) {
					
					if(status) {
						setText("Auto-reset blocked", req);
						disable(req);
						return;
					} else {
						setText("Auto-reset devices", req);
					}
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					if(status)
						return;
					for(InstallAppDevice iad: devices) {
						iad.delete();
					}
				}
			};
			autoResetDeviceIds.setDefaultConfirmMsg("Really delete all device knownDevice entries? Please stop and start Search Devices afterwards.");
			row.addCell(WidgetHelper.getValidWidgetId("Reset Devices"), autoResetDeviceIds);
		}
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "ShortID";
	}

	@Override
	protected String getLabel(DeviceHandlerProviderDP<?> obj, OgemaHttpRequest req) {
		return obj.getDeviceTypeShortId(dpService);
	}

	@Override
	public Collection<DeviceHandlerProviderDP<?>> getObjectsInTable(OgemaHttpRequest req) {
		return dpService.getDeviceHandlerProviders();
	}

}
