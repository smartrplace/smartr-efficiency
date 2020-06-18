package org.smartrplace.gui.filtering.util;

import org.ogema.core.model.ResourceList;
import org.ogema.internationalization.util.LocaleHelper;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.gui.filtering.GenericFilterOption;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public abstract class RoomFilteringWithGroups<T> extends RoomFilteringByType<T> {
	private static final long serialVersionUID = 1L;

	public RoomFilteringWithGroups(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode,
			ResourceList<BuildingPropertyUnit> roomGroups) {
		super(page, id, saveOptionMode);
		for(BuildingPropertyUnit bu: roomGroups.getAllElements()) {
			GenericFilterOption<Room> other = new GenericFilterOption<Room>() {
	
				@Override
				public boolean isInSelection(Room object, OgemaHttpRequest req) {
					return bu.rooms().contains(object);
				}
				
			};
			addOption(other, LocaleHelper.getLabelMap(ResourceUtils.getHumanReadableShortName(bu)));
			setDefaultItems(filteringOptions);
		}
	}

}
