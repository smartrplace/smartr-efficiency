package org.smartrplace.apps.hw.install.gui.alarm;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmconfig.util.AlarmMessageUtil;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.hwinstall.basetable.DeviceHandlerAccess;

import com.google.common.base.Objects;
import com.google.common.base.Supplier;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirmData;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.DynamicTableData;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownOption;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.html.html5.flexbox.AlignItems;
import de.iwes.widgets.html.multiselect.TemplateMultiselect;
import de.iwes.widgets.html.popup.Popup;
import de.iwes.widgets.resource.widget.dropdown.ResourceListDropdown;
import de.iwes.widgets.resource.widget.label.TimeResourceLabel;
import de.iwes.widgets.resource.widget.label.ValueResourceLabel;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;
import de.iwes.widgets.template.DisplayTemplate;

@SuppressWarnings("serial")
public class DeviceKnownFaultsInstallationPage {
	
	private final WidgetPage<?> page;
	private final ApplicationManager appMan;
	private final DeviceHandlerAccess deviceHandlers;
	private Popup lastMessagePopup;
	private Label lastMessageDevice;
	private Label lastMessage;
	
	public DeviceKnownFaultsInstallationPage(WidgetPage<?> page, ApplicationManager appMan, DeviceHandlerAccess deviceHandlers) {
		this.page = page;
		this.appMan = appMan;
		this.deviceHandlers = deviceHandlers;
		this.buildPage();
	}
	
	private final void buildPage() {
		
		final Map<String, String> subFlexCss = new HashMap<>(4);
		subFlexCss.put("column-gap", "1em");
		subFlexCss.put("flex-wrap", "nowrap");
		final Map<String, String> subFlexFirstCss = new HashMap<>(4);
		subFlexFirstCss.put("color", "darkblue");
		subFlexFirstCss.put("font-weight", "bold");
		final Map<String, String> filterFlexCss = new HashMap<>(8);
		filterFlexCss.put("column-gap", "3em");
		filterFlexCss.put("row-gap", "1em");
		filterFlexCss.put("flex-wrap", "wrap");
		filterFlexCss.put("padding", "0.5em");
		filterFlexCss.put("background-color", "lightgray");
		
		
		final Flexbox filterFlex = new Flexbox(page, "filterflex", true);
		filterFlex.addCssItem(">div", filterFlexCss, null);
		filterFlex.setAlignItems(AlignItems.CENTER, null);
		final Dropdown statusFilter = new Dropdown(page, "statusFilter");
		statusFilter.setDefaultOptions(Arrays.asList(
				new DropdownOption("all", "Alle Alarme", false),
				new DropdownOption("unresolved", "Offene Alarme", true)
		));
		statusFilter.setDefaultToolTip("Als erledigt markierte Alarme anzeigen oder ausblenden?");
		
		// filter stuff copied from  HardwareInstall test page
		final ResourceListDropdown<BuildingPropertyUnit> buildings = new ResourceListDropdown<BuildingPropertyUnit>(page,  "buildingsSelector", false) {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				// TODO add: unassigned & all
				setList(appMan.getResourceAccess().<ResourceList<BuildingPropertyUnit>>getResource("accessAdminConfig/roomGroups"), req);
				final Map<String, String[]> params = page.getPageParameters(req);
				final boolean hasBuildingParam = params != null && params.containsKey("roomgroup");
				if (!hasBuildingParam) {
					getList(req).getAllElements().stream()
						.filter(b -> b.name().isActive() && "all rooms".equalsIgnoreCase(b.name().getValue()))
						.findAny().ifPresent(b -> selectItem(b, req));
				}
			}
			
		};
		buildings.setDefaultSelectByUrlParam("roomgroup");
		final String buildingsTooltip = "Gebäude/Gruppe von Räumen auswählen um Räume zu filtern";
		buildings.setDefaultToolTip(buildingsTooltip);
		
		final KnownDevicesWidget knownDevices = new KnownDevicesWidget(page, "knowndevices", appMan.getResourceAccess());
		
		final TemplateMultiselect<Room> rooms = new TemplateMultiselect<Room>(page, "roomSelector") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				Collection<Room> rooms = knownDevices.getKnownDevices(req).stream()
					.filter(dev -> dev.device().location().room().exists())
					.map(dev -> dev.device().location().room().<Room>getLocationResource())
					.collect(Collectors.toSet());
				final BuildingPropertyUnit building = buildings.getSelectedItem(req);
				if (building != null && building.rooms().exists()) {
					final List<Room> filteredRooms = building.rooms().getAllElements();
					rooms = rooms.stream()
							.filter(room -> filteredRooms.stream().filter(room2 -> room2.equalsLocation(room)).findAny().isPresent()).collect(Collectors.toList());
				}
				update(rooms, req);
			}
			
		};
		rooms.setTemplate(new DisplayTemplate<Room>() {
			
			@Override
			public String getLabel(Room room, OgemaLocale arg1) {
				return room.name().isActive() ? room.name().getValue() : room.getLocation();
			}
			
			@Override
			public String getId(Room room) {
				return room.getLocation();
			}
		});
		rooms.setDefaultSelectByUrlParam("room");
		final String roomsTooltip = "Alarme nach Raum filtern";
		rooms.setDefaultToolTip(roomsTooltip);
		
		final TemplateMultiselect<DeviceHandlerProvider<?>> deviceTypes = new TemplateMultiselect<DeviceHandlerProvider<?>>(page, "deviceHandlers") {
			
			// filtered by selected room and availability of devices
			public void onGET(OgemaHttpRequest req) {
				final List<InstallAppDevice> devices = knownDevices.getKnownDevices(req);
				final List<Room> room = rooms.getSelectedItems(req);
				Stream<InstallAppDevice> handlerStream = devices.stream();
				if (room != null && !room.isEmpty()) {
					handlerStream = handlerStream.filter(dev ->  {
						final Room deviceRoom = dev.device().location().room();
						if (deviceRoom == null)
							return false;
						return room.stream().filter(r -> r.equalsLocation(deviceRoom)).findAny().isPresent();
					});
				}
				final Set<String> applicableHandlers = handlerStream
					.map(dev -> dev.devHandlerInfo())
					.filter(Resource::isActive)
					.map(StringResource::getValue)
					.collect(Collectors.toSet());
				
				final Collection<DeviceHandlerProvider<?>> handlers = deviceHandlers.getTableProviders().values().stream()
					.filter(dev -> applicableHandlers.contains(dev.id()))
					.collect(Collectors.toList());
				update(handlers, req);
			}
			
		};
		deviceTypes.setDefaultSelectByUrlParam("device");
		final String devicesTooltip = "Filter device type";
		deviceTypes.setDefaultToolTip(devicesTooltip);

		final AtomicInteger subCnt = new AtomicInteger();
		final Supplier<Flexbox> subFlexSupplier = () -> {
			final Flexbox sub  = new Flexbox(page, "filterflex_sub" + subCnt.getAndIncrement(), true);
			sub.addCssItem(">div", subFlexCss, null);
			sub.addCssItem(">div>div:first-child", subFlexFirstCss, null);
			sub.setAlignItems(AlignItems.CENTER, null);
			filterFlex.addItem(sub, null);
			return sub;
		};
		subFlexSupplier.get().addItem(new Label(page, "filterDoneLab", "Erledigte anzeigen?"), null)
			.addItem(statusFilter, null);
		subFlexSupplier.get().addItem(new Label(page, "filterBuildingLab", "Gebäude:"), null)
			.addItem(buildings, null);
		subFlexSupplier.get().addItem(new Label(page, "filterRoomLab", "Räume:"), null)
			.addItem(rooms, null);
		subFlexSupplier.get().addItem(new Label(page, "filterDevTypeLab", "Gerätetypen:"), null)
			.addItem(deviceTypes, null);
		
		final Collection<Integer> doneStatuses = Stream.of(1, 11).collect(Collectors.toList());		
		final DynamicTable<InstallAppDevice> table = new DynamicTable<InstallAppDevice>(page, "devicesTable") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final boolean filterForReleased = "unresolved".equals(statusFilter.getSelectedValue(req));
				Stream<InstallAppDevice> deviceStream =  knownDevices.getKnownDevices(req).stream()
					.filter(cfg -> !cfg.isTrash().isActive() || !cfg.isTrash().getValue())
					.filter(cfg -> cfg.knownFault().isActive());
				if (filterForReleased) {
					deviceStream = deviceStream.filter(cfg -> !cfg.knownFault().forRelease().isActive() 
							|| !doneStatuses.contains(cfg.knownFault().forRelease().getValue()));
				}
				final List<Room> selectedRooms = rooms.getSelectedItems(req);
				if (!selectedRooms.isEmpty()) {
					deviceStream = deviceStream.filter(cfg -> selectedRooms.stream().filter(r -> 
							cfg.device().location().room().equalsLocation(r)).findAny().isPresent()
					);
				}
				final List<DeviceHandlerProvider<?>> handlers = deviceTypes.getSelectedItems(req);
				if (!handlers.isEmpty()) {
					final List<String> ids = handlers.stream().map(DeviceHandlerProvider::id).collect(Collectors.toList());
					deviceStream = deviceStream.filter(cfg -> cfg.devHandlerInfo().isActive() && ids.contains(cfg.devHandlerInfo().getValue()));
				}
				final List<InstallAppDevice> devices = deviceStream.collect(Collectors.toList());
				updateRows(devices, req);
			}
			
		};
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mmZ");
		table.setRowTemplate(new RowTemplate<InstallAppDevice>() {

			@Override
			public Row addRow(InstallAppDevice device, OgemaHttpRequest req) {
				final Row row = new Row();
				final String id = ResourceUtils.getValidResourceName(getLineId(device));
				final Label name = new Label(table, id + "_name", req);
				name.addDefaultCssItem("", Collections.singletonMap("word-wrap", "break-word"));
				name.addDefaultCssItem("", Collections.singletonMap("max-width", "10em"));
				final PhysicalElement actualDevice = device.device().getLocationResource();
				name.setDefaultText(actualDevice.isActive() ? ResourceUtils.getHumanReadableShortName(actualDevice) : "");
				row.addCell("name", name);
				
				final ValueResourceLabel<StringResource> devLabel = new ValueResourceLabel<>(table, id + "_deviceid", req);
				devLabel.selectDefaultItem(device.deviceId());
				row.addCell("device", devLabel);
				
				final Room room = actualDevice.location().room();
				if (room.isActive()) {
					final Label rm = new Label(table, id + "_room", req);
					rm.setDefaultText(ResourceUtils.getHumanReadableShortName(room.getLocationResource()));
					row.addCell("room", rm);
				}
				final ValueResourceLabel<StringResource> loc =new ValueResourceLabel<>(table, id + "_loc", req);
				loc.selectDefaultItem(device.installationLocation());
				row.addCell("location", loc);
				
				final ValueResourceTextField<StringResource> comment = new ValueResourceTextField<StringResource>(table, id + "_comment", 
						device.knownFault().comment(), req);
				if (device.knownFault().comment().isActive())
					comment.setDefaultToolTip(device.knownFault().comment().getValue());
				row.addCell("comment", comment);
				
				
				final ValueResourceTextField<FloatResource> prioLabel = new ValueResourceTextField<FloatResource>(table, id + "_prio", 
						device.knownFault().getSubResource("processingOrder", FloatResource.class), req) { // optional element processingOrder is rather new
					
					private static final float EPSILON = 0.00001F;
					
					@Override
					protected String format(FloatResource resource, java.util.Locale locale) {
						if (!resource.isActive() || Float.isNaN(resource.getValue()))
							return "";
						final float value = resource.getValue();
						final boolean isInt = Math.abs(Math.round(value) - value) < EPSILON;
						if (isInt)
							return String.valueOf(Math.round(value));
						final float tenTimes = 10*value;
						final boolean hasSingleDigit = Math.abs(Math.round(tenTimes) - tenTimes) < EPSILON;
						if (hasSingleDigit)
							return String.format(Locale.ENGLISH, "%.1f", value);
						return String.format(Locale.ENGLISH, "%.2f", value);
					}
					
					@Override
					protected void setResourceValue(FloatResource resource, String value, OgemaHttpRequest req) {
						if (value == null || value.trim().isEmpty() || "nan".equalsIgnoreCase(value.trim())) {
							resource.delete();
							return;
						}
						final float val = Float.parseFloat(value);
						resource.<FloatResource> create().setValue(val);
						resource.activate(false);
					}
					
				};
				prioLabel.setDefaultToolTip("Priorität angeben. Empfohlene Werte sind 10, 20, 30, ...");
				prioLabel.setDefaultPlaceholder("Priorität (10, 20, 30, ...)");
				row.addCell("prio", prioLabel);
				// changing the priority can change the order in the table
				prioLabel.triggerAction(table, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
				final TimeResourceLabel activeSince = new TimeResourceLabel(table, id + "_activesince", req) {
					
					@Override
					protected String format(TimeResource resource, Locale locale) {
						if (!resource.isActive())
							return "";
						final ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(resource.getValue()), ZoneId.systemDefault());
						return formatter.format(zdt);
					}
					
					
				};
				activeSince.selectDefaultItem(device.knownFault().ongoingAlarmStartTime());
				row.addCell("activesince", activeSince);

				final RedirectButton detailsRedirect = new RedirectButton(table, id + "_details", "Details", 
						"/org/smartrplace/alarmingexpert/ongoingbase.html?device=" + device.deviceId().getValue(), req);
				detailsRedirect.setToolTip("Alarmdetails in neuem Tab anzeigen", req);
				row.addCell("details", detailsRedirect);
				
				final Dropdown assigned = new Dropdown(table, id + "_assigned", req) {
					
					@Override
					public void onGET(OgemaHttpRequest req) {
						final int assigned = device.knownFault().assigned().isActive() ? device.knownFault().assigned().getValue() : 0;
						if (assigned == 0) {
							selectSingleOption("0", req);
							setToolTip("Verantortlichkeit festlegen", req);
						} else {
							selectSingleOption(assigned + "", req);
							final String role = AlarmingConfigUtil.ASSIGNEMENT_ROLES.get(assigned + "");
							if (role != null)
								setToolTip(role, req);
						}
					}
					
					@Override
					public void onPOSTComplete(String arg0, OgemaHttpRequest req) {
						final String selectedStr = getSelectedValue(req);
						final int selected = selectedStr != null && !selectedStr.isEmpty() ? Integer.parseInt(selectedStr) : 0;
						if (selected == 0)
							device.knownFault().assigned().delete();
						else {
							device.knownFault().assigned().<IntegerResource> create().setValue(selected);
							device.knownFault().assigned().activate(false);
						}
						
						
					}
					
				};
				final Collection<DropdownOption> assignmentOpts = AlarmingConfigUtil.ASSIGNEMENT_ROLES.entrySet().stream()
					.map(entry -> new DropdownOption(entry.getKey(), entry.getValue(), "0".equals(entry.getKey())))
					.collect(Collectors.toList());
				assigned.setDefaultOptions(assignmentOpts);
				assigned.setDefaultToolTip("Verantortlichkeit festlegen");
				assigned.triggerAction(assigned, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
				row.addCell("assigned", assigned);
				
				final Button showMsg = new Button(table, id + "_msg", req) {
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						lastMessage.setText(device.knownFault().lastMessage().getValue(), req);
						lastMessageDevice.setText(device.deviceId().getValue(), req);
					}
					
				};
				showMsg.setDefaultText("Anzeigen");
				showMsg.setDefaultToolTip("Letzte Alarm-Benachrichtigung für dieses Gerät anzeigen.");
				showMsg.triggerAction(lastMessagePopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET, req);
				showMsg.triggerAction(lastMessageDevice,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
				showMsg.triggerAction(lastMessage,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
				row.addCell("message", showMsg);
				
				final ButtonConfirm doneBtn = new ButtonConfirm(table, id + "_done", req) {
					
					@Override
					public void onGET(OgemaHttpRequest req) {
						final boolean done = device.knownFault().forRelease().isActive() && doneStatuses.contains(device.knownFault().forRelease().getValue());
						if (done) {
							disable(req);
							setText("erledigt", req);
							setToolTip("Dieser Alarm wurde als erledigt markiert", req);
							setConfirmMsg("Bereits erledigt", req);
							setConfirmBtnMsg("??", req);
						} else {
							enable(req);
							setText("fertigstellen", req);
							setToolTip("Diesen Alarm als erledigt markieren", req);
							setConfirmMsg("Diesen Alarm als erledigt markieren?", req);
							setConfirmBtnMsg("Erledigt", req);
						}
					
					}
					
					@Override
					public void onPOSTComplete(String arg0, OgemaHttpRequest req) {
						final IntegerResource release = device.knownFault().forRelease().create();
						release.setValue(11);
						release.activate(false);
					}
				};
				doneBtn.setDefaultConfirmPopupTitle("Bestätigen");
				doneBtn.setDefaultCancelBtnMsg("Abbrechen");
				doneBtn.addDefaultStyle(ButtonConfirmData.CONFIRM_GREEN);
				doneBtn.addDefaultStyle(ButtonConfirmData.CANCEL_ORANGE);
				doneBtn.triggerAction(doneBtn, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
				row.addCell("resolve", doneBtn);
				
				final Label valueField = new Label(table, id + "_valueField", req);
				AlarmMessageUtil.configureAlarmValueLabel(device, appMan, valueField, req, Locale.GERMAN);
				row.addCell("value", valueField);
				
				return row;
			}

			@Override
			public Map<String, Object> getHeader() {
				final Map<String, Object> header = new LinkedHashMap<String, Object>();
				header.put("name", "Gerätename");
				header.put("device", "Geräte-Id");
				header.put("room", "Raum");
				header.put("location", "Ort");
				header.put("prio", "Reihenfolge");
				header.put("value", "Wert/Letzter Kontakt");
				header.put("activesince", "Aktiv seit");
				header.put("comment", "Kommentar");
				header.put("details", "Details");
				header.put("assigned", "Verantwortlich");
				header.put("message", "Fehlerbericht");
				header.put("resolve", "Erledigt?");
				return header;
			}

			@Override
			public String getLineId(InstallAppDevice dev) {
				// model is rather new
				final FloatResource processingOrder = dev.knownFault() /*.processingOrder() */ .getSubResource("processingOrder", FloatResource.class);
				final float prio = processingOrder.isActive() ? processingOrder.getValue() : Float.NaN;
				final String prioString = String.valueOf(prio).replace(".", "__dot__").replace("-", "__minus__").replace("+", "__plus__");
				return prioString + "__priosep__" + dev.getPath();
			}
			
		});
		table.setDefaultRowIdComparator((String id1, String id2) -> {
			if (Objects.equal(id1, id2))
				return 0;
			if (DynamicTable.HEADER_ROW_ID.equals(id1))
				return -1;
			if (DynamicTable.HEADER_ROW_ID.equals(id2))
				return 1;
			final float prio1 = prioForRowId(id1);
			final float prio2 = prioForRowId(id2);
			if (prio1 == prio2)
				return 0;
			final boolean nan1 = Float.isNaN(prio1);
			final boolean nan2 = Float.isNaN(prio2);
			if (nan1)
				return nan2 ? 0 : 1;
			if (nan2)
				return -1;
			return Float.compare(prio1, prio2);
		});
		table.addDefaultStyle(DynamicTableData.BOLD_HEADER);
		table.addDefaultCssItem(">div>div>table>tbody>tr:first-child", Collections.singletonMap("color", "darkblue"));
		table.addDefaultCssItem(">div>div>table>tbody>tr:first-child", Collections.singletonMap("background-color", "lightgray"));

		
		final Header header = new Header(page, "title", "9. Gerätefehler (Installationssicht)");
		header.setDefaultHeaderType(1);
		header.setDefaultColor("darkblue");
		final Header filterHeader = new Header(page, "filterHeader", "Filter");
		filterHeader.setDefaultHeaderType(3);
		filterHeader.setDefaultColor("darkblue");
		final Header alarmsHeader = new Header(page, "alarmsHeader", "Alarme");
		alarmsHeader.setDefaultHeaderType(3);
		alarmsHeader.setDefaultColor("darkblue");
		page.append(header).append(filterHeader)
			.append(filterFlex).append(alarmsHeader).append(table);
		
		// popup for message display // copied over from DeviceKnownFaultsPage
		lastMessagePopup = new Popup(page, "lastMessagePopup", true);
		lastMessagePopup.setDefaultTitle("Letzter Alarm");
		lastMessageDevice = new Label(page, "lastMessagePopupDevice");
		lastMessage = new Label(page, "lastMessage");
		
		final StaticTable tab = new StaticTable(2, 2, new int[]{3, 9});
		tab.setContent(0, 0, "Gerät").setContent(0, 1, lastMessageDevice)
			.setContent(1, 0, "Nachricht").setContent(1,1, lastMessage);
		final PageSnippet snip = new PageSnippet(page, "lastMessageSnip", true);
		snip.append(tab, null);
		lastMessagePopup.setBody(snip, null);
		final Button closeLastMessage = new Button(page, "lastMessageClose", "Schließen");
		closeLastMessage.triggerAction(lastMessagePopup, TriggeringAction.ON_CLICK, TriggeredAction.HIDE_WIDGET);
		lastMessagePopup.setFooter(closeLastMessage, null);
		page.append(lastMessagePopup);
		//
		
		statusFilter.triggerAction(table, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		
		knownDevices.triggerAction(rooms, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		knownDevices.triggerAction(deviceTypes, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		
		buildings.triggerAction(rooms, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		buildings.triggerAction(deviceTypes, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
		buildings.triggerAction(table, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 2);
		rooms.triggerAction(deviceTypes, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		rooms.triggerAction(table, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
		deviceTypes.triggerAction(table, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	
	private static float prioForRowId(String rowId) {
		 return Float.parseFloat(rowId.substring(0, rowId.indexOf("__priosep__")).replace("__dot__", ".").replace("__minus__", "-").replace("__plus__", "+"));
	}
	
	

}