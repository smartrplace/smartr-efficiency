package org.smartrplace.gui.filtering.util;

import java.util.List;

import org.ogema.core.model.ResourceList;
import org.ogema.internationalization.util.LocaleHelper;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.gui.filtering.GenericFilterBase;
import org.smartrplace.gui.filtering.GenericFilterOption;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public abstract class RoomFilteringWithGroups<T> extends RoomFilteringByType<T> {
	private static final long serialVersionUID = 1L;
	protected final ResourceList<BuildingPropertyUnit> roomGroups;
	
	public RoomFilteringWithGroups(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode,
			long updateChoicesRate, ResourceList<BuildingPropertyUnit> roomGroups) {
		super(page, id, saveOptionMode, updateChoicesRate);
		this.roomGroups = roomGroups;
	}

	@Override
	protected List<GenericFilterOption<Room>> getOptionsDynamic(OgemaHttpRequest req) {
		List<GenericFilterOption<Room>> result = super.getOptionsDynamic(req);
		for(BuildingPropertyUnit bu: roomGroups.getAllElements()) {
			GenericFilterOption<Room> other = new GenericFilterBase<Room>(LocaleHelper.getLabelMap(ResourceUtils.getHumanReadableShortName(bu))) {
	
				@Override
				public boolean isInSelection(Room object, OgemaHttpRequest req) {
					return bu.rooms().contains(object);
				}
				
			};
			result.add(other);
		}
		return result;
	}
}
