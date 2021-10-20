package org.smartrplace.external.accessadmin.gui;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.util.RoomEditHelper;
import org.ogema.apps.roomlink.localisation.mainpage.RoomLinkDictionary;
import org.ogema.core.application.ApplicationManager;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.gui.filtering.util.RoomFilteringWithGroups;
import org.smartrplace.gui.tablepages.PerMultiselectConfigPage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.linkingresource.RoomHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.RedirectButton;

public class RoomConfigPage extends PerMultiselectConfigPage<Room, BuildingPropertyUnit, Room> {
	public final static Map<String, String> valuesToSet = new HashMap<>();
	static {
		int[] ks = RoomHelper.getRoomTypeKeys();
		for(int type: ks) {
			String label = RoomHelper.getRoomTypeString(type, OgemaLocale.ENGLISH);
			valuesToSet.put(""+type, label);
		}
	}
	
	protected final AccessAdminController controller;
	protected RoomFilteringWithGroups<Room> roomFilter;

	public RoomConfigPage(WidgetPage<?> page, AccessAdminController controller) {
		super(page, controller.appMan, ResourceHelper.getSampleResource(Room.class));
		this.controller = controller;
		triggerPageBuild();
	}

	@Override
	public Room getResource(Room object, OgemaHttpRequest req) {
		return object;
	}

	@Override
	protected void addWidgetsBeforeMultiSelect(Room object, ObjectResourceGUIHelper<Room, Room> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		//vh.dropdown("Room Type", id, object.type(), row, valuesToSet);
		if(Boolean.getBoolean("org.smartrplace.hwinstall.basetable.debugfiltering")) {
			vh.stringLabel("Location", id, object.getLocation(), row);
		}
	}
	
	@Override
	protected String getHeader(OgemaLocale locale) {
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
					alert, appMan, 0, 2);
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

	/*@Override
	protected TemplateMultiselect<AccessConfigUser> getMultiselect(UserDataTbl object, String lineId,
			OgemaHttpRequest req) {
		TemplateMultiselect<AccessConfigUser> groupSelect = new TemplateMultiselect<AccessConfigUser>(mainTable, "groupSelect"+id, req) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				List<AccessConfigUser> selected = object.accessConfig.superGroups().getAllElements();
				List<AccessConfigUser> all = controller.getUserGroups(false);
				update(all, req);
				selectItems(selected, req);
			}
		};
		groupSelect.setTemplate(new DefaultDisplayTemplate<AccessConfigUser>() {
			@Override
			public String getLabel(AccessConfigUser object, OgemaLocale locale) {
				return ResourceUtils.getHumanReadableShortName(object);
			}
		});
		return groupSelect;
	}*/

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
		for(BuildingPropertyUnit bu: groups) {
			ResourceListHelper.addReferenceUnique(bu.rooms(), object);
		}
		for(BuildingPropertyUnit bu: getAllGroups(null, null)) {
			if(ResourceHelper.containsLocation(groups, bu))
				continue;
			ResourceListHelper.removeReferenceOrObject(bu.rooms(), object);
		}
	}

}
