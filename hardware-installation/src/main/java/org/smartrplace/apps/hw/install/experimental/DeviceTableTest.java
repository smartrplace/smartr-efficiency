package org.smartrplace.apps.hw.install.experimental;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownOption;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.multiselect.TemplateMultiselect;

class DeviceTableTest {
	
	private final Header header;
	private final DynamicTable<ResourcePattern> table;
	final String handlerId;
	
	DeviceTableTest(PageSnippet parent, DeviceHandlerProvider handler, TemplateMultiselect<Room> roomSelector,
			Supplier<ResourceList<InstallAppDevice>> knownDevices, OgemaHttpRequest req) {
		this.handlerId = handler.id();
		final String providerId = ResourceUtils.getValidResourceName(handler.id()).replaceAll("\\$", "_");
		this.header = new Header(parent, providerId + "_header", req);
		header.setDefaultColor("darkblue");
		this.header.setText(handler.label(req.getLocale()), req); // handler.getTableTitle()?
		this.table = new DynamicTable<ResourcePattern>(parent, providerId + "_table", req) {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final List<Room> rooms = roomSelector.getSelectedItems(req);
				final boolean roomSelected = rooms != null && !rooms.isEmpty();
				List<ResourcePattern> filteredPatterns = handler.getAllPatterns();
				if (roomSelected) {
					filteredPatterns = filteredPatterns.stream().filter(pattern -> {
						final Room room = ResourceUtils.getDeviceLocationRoom(pattern.model);
						if (room == null)
							return false;
						return rooms.stream().filter(r -> r.equalsLocation(room)).findAny().isPresent();
					}).collect(Collectors.toList());
				}
				updateRows(filteredPatterns, req);
			}
			
		};
		table.setComposite(15_000);
		parent.triggerAction(header, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST, req);
		parent.triggerAction(table, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST, req); // TODO test
		
		this.table.setRowTemplate(new RowTemplate<ResourcePattern>() {

			@Override
			public Row addRow(ResourcePattern pattern, OgemaHttpRequest req) {
				final ResourceList<InstallAppDevice> devices = knownDevices.get();
				if (devices == null || !devices.exists()) {
					return null;
				}
				final Optional<InstallAppDevice> config = devices.getAllElements().stream().filter(dev -> dev.device().equalsLocation(pattern.model)).findAny();
				if (!config.isPresent())
					return null;
				final SingleValueResource value = handler.getMainSensorValue(pattern.model, config.get());
				final String idPrefix = providerId + "__" + ResourceUtils.getValidResourceName(pattern.model.getLocation()).replaceAll("\\$", "_"); //XXX 
				final Row row = new Row();
				final Label name = new Label(table, idPrefix + "_name", req);
				name.setText(DeviceTableTest.getDeviceName(config.get(), handler), req); // an approximation to the original app
				row.addCell("name", name);
				final Label id = new Label(table, idPrefix + "_id", req);
				id.setText(config.isPresent() ? config.get().deviceId().getValue() : "unknown", req);
				row.addCell("id", id);
				final Label valueLabel = new Label(table, idPrefix + "_value", req) {
					
					public void onGET(OgemaHttpRequest req) {
						if (value == null) {
							setText("", req);
							return;
						}
						setText(value instanceof FloatResource ? ValueResourceUtils.getValue((FloatResource) value, 2) : ValueResourceUtils.getValue(value), req);
					}
					
				};
				valueLabel.setPollingInterval(15_000, req);
				row.addCell("value", valueLabel);
				final Label lastContact = new Label(table, idPrefix + "_lastContact", req) {
					
					public void onGET(OgemaHttpRequest req) {
						if (value == null) {
							setText("", req);
							return;
						}
						final Date last = new Date(value.getLastUpdateTime());
						final Date now = new Date();
						final long diff = now.getTime() - last.getTime();								
						setText((diff/1000/60) + " min" , req);
					}
					
				};
				lastContact.setPollingInterval(15_000, req);
				row.addCell("lastContact", lastContact);
				final TemplateDropdown<Room> room = new TemplateDropdown<Room>(table, idPrefix + "_room", req) {
					public void onGET(OgemaHttpRequest req) {
						update(roomSelector.getItems(req), req);
						if (((PhysicalElement) pattern.model).location().room().exists())
							selectItem(((PhysicalElement) pattern.model).location().room().getLocationResource(), req);
						else 
							selectItem(null, req);
					}
				};
				//room.setDefaultItems(rooms);
				row.addCell("room", room);
				final TextField location = new TextField(table, idPrefix + "_location", req) {
					public void onGET(OgemaHttpRequest req) {
						setValue(config.isPresent() ? config.get().installationLocation().getValue() : "", req);
					}
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						if (config.isPresent()) {
							final StringResource loc = config.get().installationLocation().create();
							loc.setValue(getValue(req));
							loc.activate(false);
						}
					}
					
				};
				row.addCell("location", location);
				final Dropdown statusDrop = new Dropdown(table, idPrefix + "_status",  req) {
					
					public void onGET(OgemaHttpRequest req) {
						final int status = config.isPresent() ?	config.get().installationStatus().getValue() : 0;
						selectSingleOption(status + "", req);
					}
					
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						try {
							final int status = Integer.parseInt(getSelectedValue(req));
							if (config.isPresent()) {
								final IntegerResource statusRes = config.get().installationStatus().create();
								statusRes.setValue(status);
								statusRes.activate(false);
							}
						} catch (NumberFormatException e) {}
					}
					
				};
				final List<DropdownOption> options = DeviceTableRaw.valuesToSetInstall.entrySet().stream()
					.map(entry -> new DropdownOption(entry.getKey(), entry.getValue(), false))
					.collect(Collectors.toList());
				statusDrop.setDefaultOptions(options);
				
				row.addCell("status", statusDrop);
				
				final TextField comment = new TextField(table, idPrefix + "_comment", req) {
					public void onGET(OgemaHttpRequest req) {
						setValue(config.isPresent() ? config.get().installationComment().getValue() : "", req);
					}
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						if (config.isPresent()) {
							final StringResource comment = config.get().installationComment().create();
							comment.setValue(getValue(req));
							comment.activate(false);
						}
					}
				};
				row.addCell("comment", comment);
				// FIXME both are likely incorrect
				final String configId = config.isPresent() ? config.get().deviceId().getValue() : ResourceUtils.getValidResourceName(pattern.model.getLocation()); 
				final RedirectButton plot = new RedirectButton(table, idPrefix + "_plot", "Plot", 
						"/de/iwes/tools/schedule/viewer-basic-example/index.html?providerId="+ handler.id() +"&configId=" + configId, req);
				row.addCell("plot", plot);
				return row;
			}

			@Override
			public Map<String, Object> getHeader() {
				final Map<String, Object> header = new LinkedHashMap<>();
				header.put("name", "Name");
				header.put("id", "ID");
				header.put("value", "Value");
				header.put("lastContact", "Last Contact");
				header.put("room", "Room");
				header.put("location", "Location");
				header.put("status", "Status");
				header.put("comment", "Comment");
				header.put("plot", "Plot");
				return header;
			}

			@Override
			public String getLineId(ResourcePattern pattern) {
				return providerId + "__" + providerId + "__" + ResourceUtils.getValidResourceName(pattern.model.getLocation()).replaceAll("$", "_");
			}
			
			
		});
		parent.append(header, req).append(table, req).linebreak(req); // FIXME can we remove the linebreak? later on?
	}

	List<OgemaWidget> getSubwidgets() {
		return Arrays.asList(this.header, this.table);
	}
	
	private static String getDeviceName(InstallAppDevice config, DeviceHandlerProvider<?> handler) {
		String deviceTypeShort;
		try {
			deviceTypeShort = handler.getDeviceTypeShortId(null);
		} catch (NullPointerException e) {
			String[] els = config.deviceId().getValue().split("-");
			deviceTypeShort = els[0];
		}
		String subLoc;
		if (config.installationLocation().isActive()) {
			subLoc = deviceTypeShort + "-" + config.installationLocation().getValue();
		} else {
			String devName = config.deviceId().getValue();
			if (devName.length() < 4)
				devName = "9999";
			try {
				devName = String.valueOf(Integer.parseInt(devName.substring(devName.length()-4)));
			} catch(NumberFormatException e) {
				devName = "9998";
			}
			subLoc = deviceTypeShort + devName;
		}
		final Room room = config.device().location().room();
		if (room.isActive()) {
			final String readableRoom = ResourceUtils.getHumanReadableName(room);
			return readableRoom + "-" + subLoc;
		}
		return subLoc;
		
	}
	
}
