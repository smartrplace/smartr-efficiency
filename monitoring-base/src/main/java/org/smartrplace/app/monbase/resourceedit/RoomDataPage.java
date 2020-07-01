package org.smartrplace.app.monbase.resourceedit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.DatapointInfo;
import org.ogema.devicefinder.api.DatapointInfo.UtilityType;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DPRoomImpl;
import org.ogema.eval.timeseries.simple.smarteff.KPIResourceAccessSmarEff;
import org.ogema.model.locations.Room;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;
import extensionmodel.smarteff.api.common.BuildingUnit;

public class RoomDataPage extends ObjectGUITablePage<DPRoom, Room>{
	protected final DatapointService dpService;
	
	/** Override if necessary*/
	protected UtilityType[] getTypes(OgemaHttpRequest req) {
		return DatapointInfo.defaultSRCTypes;		
	}
	
	public RoomDataPage(WidgetPage<?> page, ApplicationManager appMan, DatapointService dpService) {
		super(page, appMan, new DPRoomImpl(DPRoom.BUILDING_OVERALL_ROOM_LABEL), false);
		this.dpService = dpService;
		triggerPageBuild();
	}

	@Override
	public void addWidgets(DPRoom object, ObjectResourceGUIHelper<DPRoom, Room> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		//if(req == null) {
		//	vh.registerHeaderEntry("Name");
		//	return;
		//}
		OgemaLocale locale = req!=null?req.getLocale():null;
		String roomId = ResourceUtils.getValidResourceName(object.label(locale));
		vh.stringLabel("Name", id, object.label(locale), row);
		BuildingUnit bu = KPIResourceAccessSmarEff.getRoomConfigResource(roomId, appMan);
		if(bu != null) {
			vh.floatEdit("Area(m2)", id, bu.groundArea(), row, alert, 0, Float.MAX_VALUE, "Area must be positive !");			
		}
		if(roomId == null || roomId.equals(DPRoom.BUILDING_OVERALL_ROOM_LABEL)) for(UtilityType util: getTypes(req)) {
			FloatResource refCon = KPIResourceAccess.getDefaultYearlyConsumptionReferenceResource(util, roomId, appMan);
			if(refCon == null) {
				continue;
			}
			vh.floatEdit("Ref. consumption "+DatapointInfo.getDefaultShortLabel(util)+" ("+
					DatapointInfo.getDefaultUnit(util)+")", id, refCon, row, alert, 0, Float.MAX_VALUE, "Only positive consumption values allowed!");			
		}
	}

	@Override
	public Room getResource(DPRoom object, OgemaHttpRequest req) {
		return object.getResource();
	}

	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "headerUtilPage", "Building Master Data");
		page.append(header);
	}

	@Override
	public Collection<DPRoom> getObjectsInTable(OgemaHttpRequest req) {
		List<Room> rooms = KPIResourceAccess.getRealRooms(appMan.getResourceAccess());
		List<DPRoom> result = new ArrayList<>();
		for(Room room: rooms) {
			DPRoom dproom = dpService.getRoom(room.getLocation());
			if(dproom.getResource() == null)
				dproom.setResource(room);
			result.add(dproom);
		}
		result.add(dpService.getRoom(DPRoom.BUILDING_OVERALL_ROOM_LABEL));
		return result ;
		//return dpService.getAllRooms();
	}

}
