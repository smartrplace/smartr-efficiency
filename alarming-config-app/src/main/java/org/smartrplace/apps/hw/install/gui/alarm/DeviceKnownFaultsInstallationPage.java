package org.smartrplace.apps.hw.install.gui.alarm;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import com.google.common.base.Objects;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
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
import de.iwes.widgets.html.popup.Popup;
import de.iwes.widgets.resource.widget.label.TimeResourceLabel;
import de.iwes.widgets.resource.widget.label.ValueResourceLabel;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;

@SuppressWarnings("serial")
public class DeviceKnownFaultsInstallationPage {
	
	private final WidgetPage<?> page;
	private final ApplicationManager appMan;
	private Popup lastMessagePopup;
	private Label lastMessageDevice;
	private Label lastMessage;
	
	public DeviceKnownFaultsInstallationPage(WidgetPage<?> page, ApplicationManager appMan) {
		this.page = page;
		this.appMan = appMan;
		this.buildPage();
	}
	
	private final void buildPage() {
		
		final DynamicTable<InstallAppDevice> table = new DynamicTable<InstallAppDevice>(page, "devicesTable") {
			
			// TODO filter for issues not marked as done, depending on selection
			@Override
			public void onGET(OgemaHttpRequest req) {
				final List<InstallAppDevice> devices=  appMan.getResourceAccess().getResources(HardwareInstallConfig.class).stream()
					.flatMap(cfg -> cfg.knownDevices().getAllElements().stream())
					.filter(cfg -> !cfg.isTrash().isActive() || !cfg.isTrash().getValue())
					.filter(cfg -> cfg.knownFault().isActive())
					.collect(Collectors.toList());
				updateRows(devices, req);
			}
			
		};
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mmZ");
		final Collection<Integer> doneStatuses = Stream.of(1, 11).collect(Collectors.toList());
		table.setRowTemplate(new RowTemplate<InstallAppDevice>() {

			@Override
			public Row addRow(InstallAppDevice device, OgemaHttpRequest req) {
				final Row row = new Row();
				final String id = ResourceUtils.getValidResourceName(getLineId(device));
				final Label name = new Label(table, id + "_name", req);
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
				showMsg.setDefaultText("Nachricht anzeigen");
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
		page.append(header).linebreak().append(table);
		
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
		
		
	}
	
	private static float prioForRowId(String rowId) {
		 return Float.parseFloat(rowId.substring(0, rowId.indexOf("__priosep__")).replace("__dot__", ".").replace("__minus__", "-").replace("__plus__", "+"));
	}
	
	

}
