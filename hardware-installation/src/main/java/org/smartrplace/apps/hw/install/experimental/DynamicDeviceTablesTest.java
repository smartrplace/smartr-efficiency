package org.smartrplace.apps.hw.install.experimental;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.ogema.core.model.ResourceList;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.model.locations.Room;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.extended.html.bricks.PageSnippetData;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.HtmlItem;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.multiselect.TemplateMultiselect;

public class DynamicDeviceTablesTest extends PageSnippet {
	
	static class DynamicDeviceTablesTestData extends PageSnippetData {
		
		final List<DeviceTableTest> tables = new ArrayList<>();

		public DynamicDeviceTablesTestData(DynamicDeviceTablesTest snippet) {
			super(snippet);
		}
		
		@Override
		protected Collection<OgemaWidget> getSubWidgets() {
			return super.getSubWidgets();
		}
		
	}
	
	//private final Collection<DeviceHandlerProvider<?>> deviceHandlers;
	private final Supplier<ResourceList<InstallAppDevice>> knownDevices;
	private final TemplateMultiselect<DeviceHandlerProvider<?>> deviceTypes;
	private final TemplateMultiselect<Room> roomSelector;

	public DynamicDeviceTablesTest(WidgetPage<?> page, String id, 
			TemplateMultiselect<DeviceHandlerProvider<?>> deviceTypes,
			TemplateMultiselect<Room> roomSelector,
			Supplier<ResourceList<InstallAppDevice>> knownDevices
		
			) {
		super(page, id);
		this.deviceTypes = deviceTypes;
		this.roomSelector = roomSelector;
		//this.deviceHandlers = deviceHandlers;
		this.knownDevices = knownDevices;
		// required for careful handling of subwidgets; otherwise they could get destroyed arbitrarily in an update
		super.setDefaultUpdateMode(1); 
	}
	
	@Override
	public DynamicDeviceTablesTestData createNewSession() {	// must be overridden by derived class if generics parameter S differs from PageSnippetOptions
		return new DynamicDeviceTablesTestData(this);
	}
	
	@Override
	public DynamicDeviceTablesTestData getData(OgemaHttpRequest req) {
		return (DynamicDeviceTablesTestData) super.getData(req);
	}

	@Override
	public void onGET(OgemaHttpRequest req) { 
		final Collection<DeviceTableTest> tables = this.getData(req).tables;
		//final Collection<OgemaWidget> oldWidgets= this.getData(req).getSubWidgets();
		final List<String> oldIds = tables.stream().map(table -> table.handlerId).collect(Collectors.toList());
		final Collection<DeviceHandlerProvider<?>> deviceHandlers = this.deviceTypes.getSelectedItems(req);
		final List<String> newIds = deviceHandlers.stream()
				.map(h -> h.id())				
				.collect(Collectors.toList());
		//final List<String> forRemoval = oldIds.stream().filter(id -> !newIds.contains(id)).collect(Collectors.toList());
		final List<DeviceTableTest> forRemoval = tables.stream().filter(table -> !newIds.contains(table.handlerId)).collect(Collectors.toList());
		final List<OgemaWidget> subwidgets = forRemoval.stream().flatMap(table -> table.getSubwidgets().stream()).collect(Collectors.toList());
		final List<HtmlItem> subitems = forRemoval.stream().map(DeviceTableTest::getSubItem).collect(Collectors.toList());
		if (!forRemoval.isEmpty()) {
			tables.removeAll(forRemoval);
			this.removeWidgets(subwidgets, req);
			this.removeItems(subitems, req);
		}
		// this is necessary because there are sometimes multiple handlers with the same id
		tables.addAll(new HashSet<>(newIds).stream()
			.filter(id -> !oldIds.contains(id))
			.map(id -> deviceHandlers.stream().filter(h -> h.id().equals(id)).findFirst().get())
			.map(handler -> new DeviceTableTest(this, handler, roomSelector, knownDevices, req))
			.collect(Collectors.toList()));
	}
	
}
