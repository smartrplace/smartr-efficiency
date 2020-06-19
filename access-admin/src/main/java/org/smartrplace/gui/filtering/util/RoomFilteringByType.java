package org.smartrplace.gui.filtering.util;

import java.util.Arrays;
import java.util.List;

import org.ogema.internationalization.util.LocaleHelper;
import org.ogema.model.locations.Room;
import org.smartrplace.gui.filtering.GenericFilterOption;
import org.smartrplace.gui.filtering.SingleFiltering;

import de.iwes.util.linkingresource.RoomHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public abstract class RoomFilteringByType<T> extends SingleFiltering<Room, T> {
	private static final long serialVersionUID = 1L;

	protected class RoomByTypeFilter implements GenericFilterOption<Room> {
		protected final List<Integer> fixedValues;
		
		@Override
		public boolean isInSelection(Room object, OgemaHttpRequest req) {
			return fixedValues.contains(object.type().getValue());
		}

		public RoomByTypeFilter(Integer[] fixedValues) {
			this.fixedValues = Arrays.asList(fixedValues);
		}
		
	}
	
	public RoomFilteringByType(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode) {
		super(page, id, saveOptionMode);
		RoomHelper.getRoomTypeKeys();
		addOption(new RoomByTypeFilter(new Integer[] {100}), LocaleHelper.getLabelMap("Offices"));
		addOption(new RoomByTypeFilter(new Integer[] {101}), LocaleHelper.getLabelMap("Meeting Rooms"));
		addOption(new RoomByTypeFilter(new Integer[] {5,6,7}), LocaleHelper.getLabelMap("Functional Areas"));
		addOption(new RoomByTypeFilter(new Integer[] {0}), LocaleHelper.getLabelMap("Outside"));
		GenericFilterOption<Room> other = new GenericFilterOption<Room>() {

			@Override
			public boolean isInSelection(Room room, OgemaHttpRequest req) {
				if(!room.exists()) return true;
				int object = room.type().getValue();
				//if(object == null) return true;
				if(object == 0) return false;
				if(object < 5) return true;
				if(object > 101) return true;
				if(object > 7 && object < 100) return true;
				return false;
			}
			
		};
		addOption(other, LocaleHelper.getLabelMap("Other"));
		finishOptionsSetup();
	}

	@Override
	protected boolean isAttributeSinglePerDestinationObject() {
		return true;
	}

	@Override
	protected abstract Room getAttribute(T object);
}
