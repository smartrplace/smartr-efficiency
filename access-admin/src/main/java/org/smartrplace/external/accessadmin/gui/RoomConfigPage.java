package org.smartrplace.external.accessadmin.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.NamedIntegerType;
import org.ogema.accessadmin.api.SubcustomerUtil;
import org.ogema.accessadmin.api.SubcustomerUtil.SubCustomerType;
import org.ogema.accessadmin.api.util.RoomEditHelper;
import org.ogema.apps.roomlink.NewRoomPopupBuilder.RoomCreationListern;
import org.ogema.apps.roomlink.localisation.mainpage.RoomLinkDictionary;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.csv.download.generic.CSVRoomExporter;
import org.smartrplace.csv.download.generic.CSVUploadListenerRoom;
import org.smartrplace.csv.upload.generic.CSVUploadWidgets;
import org.smartrplace.csv.upload.generic.CSVUploadWidgets.CSVUploadListener;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.SubCustomerData;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.gui.filtering.util.RoomFilteringWithGroups;
import org.smartrplace.gui.tablepages.PerMultiselectConfigPage;
import org.smartrplace.smarteff.defaultservice.TSManagementPage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.directresourcegui.GUIHelperExtension;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;

import de.iwes.util.linkingresource.RoomHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.filedownload.FileDownload;
import de.iwes.widgets.html.filedownload.FileDownloadData;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.template.DefaultDisplayTemplate;

@SuppressWarnings("serial")
public class RoomConfigPage extends PerMultiselectConfigPage<Room, BuildingPropertyUnit, Room> {
	public final static Map<String, String> valuesToSetDefault = new HashMap<>();
	static {
		int[] ks = RoomHelper.getRoomTypeKeys();
		for(int type: ks) {
			String label = RoomHelper.getRoomTypeString(type, OgemaLocale.ENGLISH);
			valuesToSetDefault.put(""+type, label);
		}
	}
	
	protected final AccessAdminController controller;
	protected RoomFilteringWithGroups<Room> roomFilter;
	protected final boolean isExpert;
	protected final boolean showLocations;

	private class CSVRoomExporterFiltered extends CSVRoomExporter {
		public CSVRoomExporterFiltered(boolean isFullExport, ApplicationManagerPlus appMan) {
			super(isFullExport, appMan);
		}

		@Override
		protected List<Room> getRooms(OgemaHttpRequest req) {
			return getObjectsInTable(req);
		}
	}
	
	public final CSVRoomExporter csvRoomExporterBase;
	public final CSVRoomExporter csvRoomExporterFull;
	
	public RoomConfigPage(WidgetPage<?> page, AccessAdminController controller) {
		this(page, controller, false);
	}
	public RoomConfigPage(WidgetPage<?> page, AccessAdminController controller, boolean isExpert) {
		this(page, controller, isExpert, false);
	}
	public RoomConfigPage(WidgetPage<?> page, AccessAdminController controller, boolean isExpert,
			boolean showLocations) {
		super(page, controller.appMan, ResourceHelper.getSampleResource(Room.class));
		this.controller = controller;
		this.isExpert = isExpert;
		this.showLocations = showLocations;
		
		csvRoomExporterBase = new CSVRoomExporterFiltered(false, controller.appManPlus);
		csvRoomExporterFull = new CSVRoomExporterFiltered(true, controller.appManPlus);
		
		triggerPageBuild();
	}

	@Override
	public Room getResource(Room object, OgemaHttpRequest req) {
		return object;
	}

	@Override
	protected void addWidgetsBeforeMultiSelect(Room object, ObjectResourceGUIHelper<Room, Room> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		
		if(isExpert) {
			if(req == null) {
				vh.registerHeaderEntry("Subcustomer");
				vh.registerHeaderEntry("Room Type");
				vh.registerHeaderEntry("ID");
			} else {
				List<SubCustomerData> subcsAll = SubcustomerUtil.getSubcustomers(appMan);
				List<SubCustomerData> subcs = new ArrayList<>();
				for(SubCustomerData sub: subcsAll) {
					if(sub.aggregationType().getValue() <= 0) {
						subcs.add(sub);
					}
				}
				SubCustomerData subcustomerRef = SubcustomerUtil.getDataForRoom(object, appMan, false);
				final SubCustomerData subcustomerRefFin = subcustomerRef;

				TemplateDropdown<SubCustomerData> subCustDrop = new TemplateDropdown<SubCustomerData>(mainTable, "subCustDrop"+id, req) {
					@Override
					public void onGET(OgemaHttpRequest req) {
						update(subcs, req);
						selectItem(subcustomerRefFin, req);
					}
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						SubCustomerData selected = getSelectedItem(req);
						for(SubCustomerData subc: subcs) {
							if(subc.aggregationType().getValue() > 0)
								continue;
							ResourceListHelper.removeReferenceOrObject(subc.roomGroup().rooms(), object);
						}
						if(selected == null)
							return;
						SubcustomerUtil.addRoomToGroup(object, selected.roomGroup());
					}
					
				};
				subCustDrop.setTemplate(new DefaultDisplayTemplate<SubCustomerData>());
				subCustDrop.setAddEmptyOption(true, "not set", req);
				row.addCell("Subcustomer", subCustDrop);
				
				if(subcustomerRef == null)
					subcustomerRef = SubcustomerUtil.getDataForRoom(object, appMan, true);
				if(subcustomerRef != null && subcustomerRef.exists()) {
					Map<String, String> valuesToSet = new LinkedHashMap<>();
					int sid = subcustomerRef.subCustomerType().getValue();
					SubCustomerType data = SubcustomerUtil.subCustomerTypes.get(sid);
					for(Entry<Integer, NamedIntegerType> roomEntry: data.roomTypes.entrySet()) {						
						String label = roomEntry.getValue().labelReq(req);
						valuesToSet.put(""+roomEntry.getKey(), label);
					}
					if(!valuesToSet.isEmpty())
						vh.dropdown("Room Type", id, object.type(), row, valuesToSet);
					else
						controller.log.warn("Empty valuesToSet for "+subcustomerRef.getLocation());
				}
				
				String roomId = ServletPageProvider.getNumericalIdString(object.getLocation(), true);
				vh.stringLabel("ID", id, roomId, row);
				
			}
		}
		
		//if(Boolean.getBoolean("org.smartrplace.hwinstall.basetable.debugfiltering")) {
		//	vh.stringLabel("Location", id, object.getLocation(), row);
		//}
	}
	
	@Override
	protected void addWidgetsAfterMultiSelect(Room object, ObjectResourceGUIHelper<Room, Room> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		GUIHelperExtension.addDeleteButton(null, object, mainTable, id, alert, row, vh, req);
		if(showLocations) {
			vh.stringLabel("Location", id, object.getLocation(), row);
			if(req == null)
				vh.registerHeaderEntry("CMS ID");
			else {
				IntegerResource cmsIdRes = object.getSubResource("cmsId", IntegerResource.class);
				if(cmsIdRes != null && cmsIdRes.isActive())
					vh.intLabel("CMS ID", id, cmsIdRes.getValue(), row, 0);
			}
		}
	}
	
	@Override
	protected String getHeader(OgemaLocale locale) {
		if(isExpert)
			return "2. Room Configuration Expert";
		return "2. Room Configuration";
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		StaticTable topTable = new StaticTable(1, 5);
		roomFilter = new RoomFilteringWithGroups<Room>(page, "roomFilter",
				OptionSavingMode.PER_USER, 5000, controller.appConfigData.roomGroups(), false, appMan) {
			private static final long serialVersionUID = 1L;

			@Override
			protected Room getAttribute(Room object) {
				return object;
			}
		};
		
		roomFilter.registerDependentWidget(mainTable);
		
		final FileDownload download;
	    download = new FileDownload(page, "downloadcsv", appMan.getWebAccessManager(), true);
	    download.triggerAction(download, TriggeringAction.GET_REQUEST, FileDownloadData.STARTDOWNLOAD);
	    page.append(download);
		Button exportCSVBase = new Button(page, "exportCSVBase", "Export CSV (Rooms)") {
			@Override
	    	public void onPrePOST(String data, OgemaHttpRequest req) {
	    		download.setDeleteFileAfterDownload(true, req);
				String fileStr = csvRoomExporterBase.exportToFile(req);
	    		File csvFile = new File(fileStr);
				download.setFile(csvFile, "rooms_devices.csv", req);
	    	}
		};
		exportCSVBase.triggerAction(download, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);  // GET then triggers download start
		exportCSVBase.triggerOnPOST(alert);

		Button exportCSVFull = new Button(page, "exportCSVFull", "Export CSV (Rooms and Devices)") {
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

		topTable.setContent(0, 1, "");
		if(isExpert) {
			topTable.setContent(0, 0, exportCSVBase).setContent(0, 1, exportCSVFull);
			//topTable.setContent(0, 1, importRoomAssignmentsButton);
		}
		topTable.setContent(0,  2, roomFilter);
		if(!Boolean.getBoolean("org.smartrplace.external.accessadmin.gui.suppresscreateroom")) {
			RoomEditHelper.addButtonsToStaticTable(topTable, (WidgetPage<RoomLinkDictionary>) page,
					alert, appMan, 0, 2, new RoomCreationListern() {
						
				@Override
				public void roomCreated(Room room) {
					initRoom(room);
				}
			});
			RedirectButton calendarConfigButton = new RedirectButton(page, "calendarConfigButton",
					"Calendar Configuration", "/org/smartrplace/apps/smartrplaceheatcontrolv2/extensionpage.html");
			topTable.setContent(0, 4, calendarConfigButton);
		}
		
		CSVUploadListener listener = new CSVUploadListenerRoom(controller.hwInstallConfig, controller.appManPlus); /*new CSVUploadListener() {
			//Probably not thread-safe (if more than one upload at the same time)
			String previousType = null;
			
			@Override
			public boolean fileUploaded(String filePath, OgemaHttpRequest req) {
				return true;
			}
			
			@Override
			public void newLineAvailable(String filePath, CSVRecord record, OgemaHttpRequest req) {
				String typeId =  readLine(record, "Type");
				if(typeId == null)
					typeId = previousType;
				else
					previousType = typeId;
				if(typeId == null)
					return;
				String deviceId = readLine(record, "DeviceId number");
				if(deviceId == null)
					deviceId = readLine(record, "ID");
				if(deviceId == null)
					return;
				InstallAppDevice iad = InitialConfig.getDeviceByNumericalIdString(deviceId, typeId, controller.hwInstallConfig, 0);
				if(iad == null) {
					//try to create IAD
					String endCode = readLine(record, "serialEndCode");
					if(endCode == null)
						return;
					
					PhysicalElement device = DeviceHandlerBase.getDeviceByEndcode(endCode, typeId, controller.appManPlus);
					if(device == null)
						return;
					DeviceHandlerProvider<T> tableProvider = null;
					iad = HardwareInstallController.addDeviceIfNew(device, tableProvider);
					if(iad == null)
						return;
				}
				
				String installationLocation = readLine(record, "Location (if known)");
				if(installationLocation != null)
					ValueResourceHelper.setCreate(iad.installationLocation(), installationLocation);
				String comment = readLine(record, "comment");
				if(comment != null)
					ValueResourceHelper.setCreate(iad.installationComment(), comment);
				try {
					String roomName = record.get("Room");
					Room room = KPIResourceAccess.getRealRoomAlsoByLocation(roomName, appMan.getResourceAccess());
					if(room != null)
						iad.device().location().room().setAsReference(room);
				} catch(IllegalArgumentException e) {
					//no room
				}
			}
			
			protected String readLine(CSVRecord record, String col) {
				try {
					return record.get(col);
				} catch(IllegalArgumentException e) {
					return null;
				}
			}
		};*/
		CSVUploadWidgets uploadCSV = new CSVUploadWidgets(page, alert, pid(),
				"Import Device Room Data Base as CSV", listener , appMan);
		uploadCSV.uploader.getFileUpload().setDefaultPadding("1em", false, true, false, true);

		Flexbox flexLineCSV = TSManagementPage.getHorizontalFlexBox(page, "csvFlex"+pid(),
				uploadCSV.csvButton, uploadCSV.uploader.getFileUpload());
		page.append(flexLineCSV);

		page.append(topTable);
		
	}
	
	@Override
	public List<Room> getObjectsInTable(OgemaHttpRequest req) {
		List<Room> all = KPIResourceAccess.getRealRooms(controller.appMan.getResourceAccess()); //.getToplevelResources(Room.class);
		List<Room> result = roomFilter.getFiltered(all, req);
		return result;
	}

	@Override
	protected String getGroupColumnLabel() {
		return "Room Attributes";
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Room name";
	}

	@Override
	protected String getLabel(Room obj, OgemaHttpRequest req) {
		return ResourceUtils.getHumanReadableShortName(obj);
	}

	@Override
	protected List<BuildingPropertyUnit> getAllGroups(Room object, OgemaHttpRequest req) {
		return controller.roomGroups.getAllElements();
	}

	@Override
	protected List<BuildingPropertyUnit> getGroups(Room object, OgemaHttpRequest req) {
		return controller.getGroups(object);
	}

	@Override
	protected String getGroupLabel(BuildingPropertyUnit object, OgemaLocale locale) {
		return ResourceUtils.getHumanReadableShortName(object);
	}

	@Override
	protected void setGroups(Room object, List<BuildingPropertyUnit> groups, OgemaHttpRequest req) {
		setGroups(object, groups, getAllGroups(null, null));
	}

	/** Make sure a room is part of exactly a certain set of groups
	 * 
	 * @param object
	 * @param groups
	 * @param allGroups check all room groups whether room is referenced. If not in groups then reference is removed.
	 */
	protected static void setGroups(Room object, List<BuildingPropertyUnit> groups, List<BuildingPropertyUnit> allGroups) {
		for(BuildingPropertyUnit bu: groups) {
			ResourceListHelper.addReferenceUnique(bu.rooms(), object);
		}
		for(BuildingPropertyUnit bu: allGroups) { //getAllGroups(null, null)
			if(ResourceHelper.containsLocation(groups, bu))
				continue;
			ResourceListHelper.removeReferenceOrObject(bu.rooms(), object);
		}
	}
	
	protected void initRoom(Room object) {
		SubcustomerUtil.initRoom(object, controller.roomGroups, controller.appConfigData, controller.appMan);
	}
}
