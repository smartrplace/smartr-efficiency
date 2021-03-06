package org.smartrplace.external.accessadmin.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.external.accessadmin.gui.UserTaggedTbl.RoomGroupTbl;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.directresourcegui.GUIHelperExtension;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.alert.AlertData;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;

@SuppressWarnings("serial")
public class RoomSetupPage extends ObjectGUITablePageNamed<RoomGroupTbl, BooleanResource> {
	protected static final String ROOM_GROUP_MAPPING_LINK = "/org/smartrplace/external/accessadmin/roomconfig.html";

	protected final AccessAdminController controller;
	
	protected ResourceList<AccessConfigUser> userPerms;
	
	public RoomSetupPage(WidgetPage<?> page, AccessAdminController controller) {
		super(page, controller.appMan, new RoomGroupTbl(ResourceHelper.getSampleResource(BuildingPropertyUnit.class), null));
		this.controller = controller;
		userPerms = controller.appConfigData.userPermissions();
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "1. Room Attribute Configuration";
	}
	
	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Room Attribute";
	}

	@Override
	protected String getLabel(RoomGroupTbl obj, OgemaHttpRequest req) {
		return ResourceUtils.getHumanReadableShortName(obj.roomGrp);
	}

	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		StaticTable topTable = new StaticTable(2, 5);
		
		Button addRoomGroup = new Button(page, "addRoomGroup", "Add Room Attribute") {

			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				BuildingPropertyUnit grp = ResourceListHelper.createNewNamedElement(
						controller.appConfigData.roomGroups(),
						"New Room Attribute", false);
				grp.activate(true);
			}
		};
		addRoomGroup.registerDependentWidget(mainTable);

		topTable.setContent(1, 1, addRoomGroup); //.setContent(1, 2, userAdminLink);
		page.append(topTable);
		//dualFiltering = new DualFiltering<String, Room, Room>(
		//		userFilter, roomFilter);
		Alert info = new Alert(page, "description","Explanation") {

			@Override
	    	public void onGET(OgemaHttpRequest req) {
	    		String text = "Change the label of room attributes here. The mapping of individual rooms to the attributes can be set on the page "
	    				+ "<a href=\"" + ROOM_GROUP_MAPPING_LINK + "\"><b>Room Configuration</b></a>.";
				setHtml(text, req);
	    		allowDismiss(true, req);
	    		autoDismiss(-1, req);
	    	}
	    	
	    };
	    info.addDefaultStyle(AlertData.BOOTSTRAP_INFO);
	    info.setDefaultVisibility(true);
	    page.append(info);
	}
	
	@Override
	protected void addNameLabel(RoomGroupTbl object,
			ObjectResourceGUIHelper<RoomGroupTbl, BooleanResource> vh, String id, Row row,
			OgemaHttpRequest req) {
		vh.valueEdit(getTypeName(null), id, object.roomGrp.name(), row, alert);
	}

	@Override
	public Collection<RoomGroupTbl> getObjectsInTable(OgemaHttpRequest req) {
		List<BuildingPropertyUnit> all = controller.appConfigData.roomGroups().getAllElements();
		//List<Room> result = roomFilter.getFiltered(all, req);
		all.sort(new Comparator<BuildingPropertyUnit>() {

			@Override
			public int compare(BuildingPropertyUnit o1, BuildingPropertyUnit o2) {
				return o1.name().getValue().compareTo(o2.name().getValue());
			}
		});
		
		List<RoomGroupTbl> result = new ArrayList<>();
		for(BuildingPropertyUnit room: all) {
			result.add(new RoomGroupTbl(room, "noUser"));
		}
		
		return result;
	}

	@Override
	public void addWidgets(RoomGroupTbl object, ObjectResourceGUIHelper<RoomGroupTbl, BooleanResource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		addNameLabel(object, vh, id, row, req);
		GUIHelperExtension.addDeleteButton(null, object.roomGrp, mainTable, id, alert, row,
				vh, req);
	}
	
	@Override
	public String getLineId(RoomGroupTbl object) {
		return object.index+super.getLineId(object)+object.userName;
	}
}
