package org.smartrplace.external.accessadmin.gui;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.accessadmin.api.NamedIntegerType;
import org.ogema.accessadmin.api.SubcustomerUtil;
import org.ogema.accessadmin.api.SubcustomerUtil.SubCustomerType;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accessadmin.api.UserPermissionUtil;
import org.ogema.accessadmin.api.util.RoomEditHelper;
import org.ogema.apps.roomlink.NewRoomPopupBuilder.RoomCreationListern;
import org.ogema.apps.roomlink.localisation.mainpage.RoomLinkDictionary;
import org.ogema.core.application.ApplicationManager;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigBase;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.external.accessadmin.config.SubCustomerData;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.gui.filtering.util.RoomFilteringWithGroups;
import org.smartrplace.gui.tablepages.PerMultiselectConfigPage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.directresourcegui.GUIHelperExtension;

import de.iwes.util.linkingresource.RoomHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.template.DefaultDisplayTemplate;

@SuppressWarnings("serial")
public class RoomConfigPage extends PerMultiselectConfigPage<Room, BuildingPropertyUnit, Room> {
	public final static Map<String, String> valuesToSetDefault = new HashMap<>();
	public static final String ALL_ROOMS_GROUP_NAME = "All Rooms";
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

	public RoomConfigPage(WidgetPage<?> page, AccessAdminController controller) {
		this(page, controller, false);
	}
	public RoomConfigPage(WidgetPage<?> page, AccessAdminController controller, boolean isExpert) {
		super(page, controller.appMan, ResourceHelper.getSampleResource(Room.class));
		this.controller = controller;
		this.isExpert = isExpert;
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
			} else {
				List<SubCustomerData> subcs = SubcustomerUtil.getSubcustomers(appMan);
				SubCustomerData subcustomerRef = SubcustomerUtil.getDataForRoom(object, appMan);

				TemplateDropdown<SubCustomerData> subCustDrop = new TemplateDropdown<SubCustomerData>(page, "subCustDrop"+id) {
					@Override
					public void onGET(OgemaHttpRequest req) {
						update(subcs, req);
						selectItem(subcustomerRef, req);
					}
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						SubCustomerData selected = getSelectedItem(req);
						for(SubCustomerData subc: subcs) {
							ResourceListHelper.removeReferenceOrObject(subc.roomGroup().rooms(), object);
						}
						SubcustomerUtil.addRoomToGroup(object, selected.roomGroup());
					}
					
				};
				subCustDrop.setTemplate(new DefaultDisplayTemplate<SubCustomerData>());
				row.addCell("Subcustomer", subCustDrop);
				
				if(subcustomerRef.exists()) {
					Map<String, String> valuesToSet = new HashMap<>();
					int sid = subcustomerRef.subCustomerType().getValue();
					SubCustomerType data = SubcustomerUtil.subCustomerTypes.get(sid);
					for(Entry<Integer, NamedIntegerType> roomEntry: data.roomTypes.entrySet()) {
						String label = roomEntry.getValue().labelReq(req);
						valuesToSet.put(""+roomEntry.getKey(), label);
					}
					vh.dropdown("Room Type", id, object.type(), row, valuesToSet);
				}
				
			}
		}
		
		if(Boolean.getBoolean("org.smartrplace.hwinstall.basetable.debugfiltering")) {
			vh.stringLabel("Location", id, object.getLocation(), row);
		}
	}
	
	@Override
	protected void addWidgetsAfterMultiSelect(Room object, ObjectResourceGUIHelper<Room, Room> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		GUIHelperExtension.addDeleteButton(null, object, mainTable, id, alert, row, vh, req);
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
		
		topTable.setContent(0, 1, "").setContent(0,  2, roomFilter);
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
		page.append(topTable);
		
	}
	
	@Override
	public Collection<Room> getObjectsInTable(OgemaHttpRequest req) {
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
		setGroups(object, groups);
	}

	protected void setGroups(Room object, List<BuildingPropertyUnit> groups) {
		for(BuildingPropertyUnit bu: groups) {
			ResourceListHelper.addReferenceUnique(bu.rooms(), object);
		}
		for(BuildingPropertyUnit bu: getAllGroups(null, null)) {
			if(ResourceHelper.containsLocation(groups, bu))
				continue;
			ResourceListHelper.removeReferenceOrObject(bu.rooms(), object);
		}
	}
	
	protected void initRoom(Room object) {
		BuildingPropertyUnit allRoomsGroup = null;
		for(BuildingPropertyUnit g: controller.roomGroups.getAllElements()) {
			if(ResourceUtils.getHumanReadableShortName(g).equals(ALL_ROOMS_GROUP_NAME)) {
				allRoomsGroup = g;
				break;
			}
		}
		if(allRoomsGroup == null) {
			//create
			allRoomsGroup = ResourceListHelper.createNewNamedElement(
					controller.roomGroups,
					ALL_ROOMS_GROUP_NAME, false);
			allRoomsGroup.activate(true);
			for(AccessConfigUser userPerm: controller.appConfigData.userPermissions().getAllElements()) {
				if(userPerm.isGroup().getValue() != 2)
					continue;
				switch(userPerm.name().getValue()) {
				case "User Standard":
				case "Secretary":
				case "Facility Manager":
				case "Master Administrator":
					AccessConfigBase configRes = userPerm.roompermissionData();
					UserPermissionUtil.addPermission(allRoomsGroup.getLocation(), UserPermissionService.USER_ROOM_PERM, configRes);
				}
			}
		}
		setGroups(object, Arrays.asList(new BuildingPropertyUnit[] {allRoomsGroup}));
	}
}
