package org.smartrplace.gui.filtering.util;

import java.util.ArrayList;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.ResourceList;
import org.ogema.internationalization.util.LocaleHelper;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.gui.filtering.DualFiltering2Steps;
import org.smartrplace.gui.filtering.GenericFilterFixedGroup;
import org.smartrplace.gui.filtering.GenericFilterFixedSingle;
import org.smartrplace.gui.filtering.GenericFilterOption;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public abstract class RoomFiltering2Steps<T> extends DualFiltering2Steps<Room, BuildingPropertyUnit, T> {
	private static final long serialVersionUID = 1L;
	//protected final AccessAdminController controller;
	protected final ApplicationManagerPlus appManPlus;
	protected final ResourceList<BuildingPropertyUnit> roomGroups;
	
	public RoomFiltering2Steps(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode, long optionSetUpdateRate,
			ResourceList<BuildingPropertyUnit> roomGroups, ApplicationManagerPlus appManPlus, boolean addAllOptionLeftDropdown) {
		super(page, id, saveOptionMode, optionSetUpdateRate, addAllOptionLeftDropdown);
		//this.controller = controller;
		this.appManPlus = appManPlus;
		this.roomGroups = roomGroups;
	}

	@Override
	protected List<GenericFilterOption<Room>> getOptionsDynamic(BuildingPropertyUnit group, OgemaHttpRequest req) {
		List<GenericFilterOption<Room>> result = new ArrayList<>();
		for(Room ac: KPIResourceAccess.getRealRooms(appManPlus.getResourceAccess())) { //controller.appMan.getAdministrationManager().getAllUsers()) {
			if(group != null && (!firstDropDown.isInSelection(ac, group)))
				continue;
			GenericFilterOption<Room> newOption = new GenericFilterFixedSingle<Room>(ac,
					LocaleHelper.getLabelMap(ResourceUtils.getHumanReadableName(ac))); //SingleUserOption(ac, LocaleHelper.getLabelMap(ac.getName()));
			result.add(newOption);
		}
		GenericFilterOption<Room> notSetOption = new GenericFilterFixedSingle<Room>(null,
				LocaleHelper.getLabelMap("not set")) {
			@Override
			public boolean isInSelection(Room object, OgemaHttpRequest req) {
				return object == null || (!object.exists());
			}
		}; //SingleUserOption(ac, LocaleHelper.getLabelMap(ac.getName()));
		result.add(notSetOption);
		return result;
	}

	@Override
	protected List<GenericFilterFixedGroup<Room, BuildingPropertyUnit>> getGroupOptionsDynamic() {
		List<GenericFilterFixedGroup<Room, BuildingPropertyUnit>> result = new ArrayList<>();
		for(BuildingPropertyUnit grp: roomGroups.getAllElements()) {
			GenericFilterFixedGroup<Room, BuildingPropertyUnit> newOption = getGroupOptionDynamic(grp);
			if(newOption == null)
				continue;
			result.add(newOption);			
		}
		return result;
	}
	
	//@Override
	protected GenericFilterFixedGroup<Room, BuildingPropertyUnit> getGroupOptionDynamic(BuildingPropertyUnit grp) {
		String name = grp.name().getValue();
		GenericFilterFixedGroup<Room, BuildingPropertyUnit> newOption = new GenericFilterFixedGroup<Room, BuildingPropertyUnit>(
				grp, LocaleHelper.getLabelMap(name)) {

			@Override
			public boolean isInSelection(Room object, BuildingPropertyUnit group) {
				return ResourceHelper.containsLocation(group.rooms().getAllElements(), object);
			}
		};
		return newOption;
	}

	@Override
	protected List<BuildingPropertyUnit> getGroups(Room object) {
		List<BuildingPropertyUnit> result = new ArrayList<>();
		for(BuildingPropertyUnit grp: roomGroups.getAllElements()) {
			if(ResourceHelper.containsLocation(grp.rooms().getAllElements(), object)) {
				result.add(grp);
			}
		}
		return result ;
	}
	
	@Override
	protected boolean isAttributeSinglePerDestinationObject() {
		return true;
	}
	
	public Room getSelectedRoom(OgemaHttpRequest req) {
		GenericFilterOption<Room> selected = getSelectedItem(req);
		if(selected == NONE_OPTION)
			return null;
		if(selected == null) {
			onGET(req);
			return getSelectedRoom(req);
			//return ((SingleUserOption)getItems(req).get(0)).getValue();
		}
		return ((GenericFilterFixedSingle<Room>)selected).getValue();
	}
	
	@Override
	protected long getFrameworkTime() {
		return appManPlus.appMan().getFrameworkTime();
	}

	@Override
	protected boolean isGroupEqual(BuildingPropertyUnit group1, BuildingPropertyUnit group2) {
		return group1.equalsLocation(group2);
	}
}
