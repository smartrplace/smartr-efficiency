package org.smartrplace.apps.hw.install.gui.alarm;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmGroupDataMajor;
import org.ogema.model.user.NaturalPerson;
import org.smartrplace.apps.alarmingconfig.release.ReleasePopup;
import org.smartrplace.apps.alarmingconfig.sync.SuperiorIssuesSyncUtils;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gateway.device.GatewaySuperiorData;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.autocomplete.Autocomplete;
import de.iwes.widgets.html.autocomplete.AutocompleteData;
import de.iwes.widgets.html.emptywidget.EmptyData;
import de.iwes.widgets.html.emptywidget.EmptyWidget;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.checkbox.Checkbox2;
import de.iwes.widgets.html.form.checkbox.CheckboxEntry;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownData;
import de.iwes.widgets.html.form.dropdown.DropdownOption;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.form.textfield.TextFieldType;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.html.html5.flexbox.JustifyContent;
import de.iwes.widgets.html.popup.Popup;

public class CreateIssuePopup {
	
	 static final Comparator<DropdownOption> RESPONSIBLES_COMPARATOR = (o1, o2) ->  { // show default roles supervision and terminvereinbarung first
		if (Objects.equals(o1, o2))
			return 0;
		final boolean comp1 = o1.id().indexOf('_') > 0;
		final boolean comp2 = o2.id().indexOf('_') > 0;
		if (comp1 == comp2)
			return o1.id().compareTo(o2.id());
		return comp1 ? 1 : -1;
	};
	private static final List<DropdownOption> REMINDER_FREQUENCY_OPTIONS = Arrays.asList(
		new DropdownOption("d", "daily", false),
		new DropdownOption("w", "weekly", false),
		new DropdownOption("m", "monthly", false)
	);
	
	private final ApplicationManager appMan;
	private final Popup createIssuePopup;
	private final Button createIssueSubmit;
	private final Checkbox2 directRelease; // may be null
	private final EmptyWidget releaseTrigger;
	
	/**
	 * @param page
	 * @param appMan
	 * @param alert may be null
	 * @param releasePopup may be null
	 */
	@SuppressWarnings("serial")
	public CreateIssuePopup(WidgetPage<?> page, ApplicationManager appMan, Alert alert, ReleasePopup releasePopup) {
		
		this.appMan = appMan;
		final Popup createIssuePopup = new Popup(page, "createIssuePopup", true);
		//final Header createHeader = new Header(page, "createIssueHeader", "Create known issue");
		//createHeader.setDefaultHeaderType(2);
		//createIssuePopup.setHeader(createHeader, null);
		createIssuePopup.setTitle("Create known issue", null);
		this.createIssuePopup = createIssuePopup;
		
		final Label createIssueDeviceLab = new Label(page, "createIssueDeviceLab", "Select device");
		final Autocomplete deviceSelector = new Autocomplete(page, "createIssueDeviceSelector") {
			
			@Override
			public DeviceSelectorData getData(OgemaHttpRequest req) {
				return (DeviceSelectorData) super.getData(req);
			}
			
			@Override
			public AutocompleteData createNewSession() {
				return new DeviceSelectorData(this);
			}
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final List<InstallAppDevice> devices = appMan.getResourceAccess().getResources(HardwareInstallConfig.class).stream()
					.flatMap(hwi -> hwi.knownDevices().getAllElements().stream())
					.filter(dev -> dev.deviceId().isActive())
					.collect(Collectors.toList());
				getData(req).setItems(devices);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				final InstallAppDevice device = getData(req).getSelectedItem();
				final boolean active = device != null;
				if (active) {
					createIssueSubmit.enable(req);
				} else {
					createIssueSubmit.disable(req);
				}
			}
			
		};
		// expensive
		deviceSelector.postponeLoading();
		
		final Label deviceFaultActiveLab = new Label(page, "createIssueFaultActiveLab", "Fault active?");
		deviceFaultActiveLab.setDefaultToolTip("Is the currently selected device in an alarm state?");
		final Label deviceFaultActive = new Label(page, "createIssueFaultActive") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final InstallAppDevice device = ((DeviceSelectorData) deviceSelector.getData(req)).getSelectedItem();
				if (device == null) {
					setText("", req);
				} else {
					final boolean active = device.knownFault().isActive();
					setText(active + "", req);
				}
			}
			
		};
		deviceFaultActive.setDefaultToolTip("Is the currently selected device in an alarm state?");
		final Label createIssueCommentLab = new Label(page, "createIssueCommentLab", "Comment");
		final TextField createIssueComment = new TextField(page, "createIssueComment") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final InstallAppDevice device = ((DeviceSelectorData) deviceSelector.getData(req)).getSelectedItem();
				if (device == null || !device.knownFault().comment().isActive()) {
					setValue("", req);
				} else {
					setValue(device.knownFault().comment().getValue(), req);
				}
			}
		};
		
		final Label createIssueAssignedLab = new Label(page, "createIssueAssignedLab", "Assigned");
		final Dropdown createIssueAssigned = new Dropdown(page, "createIssueAssigned") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final InstallAppDevice device = ((DeviceSelectorData) deviceSelector.getData(req)).getSelectedItem();
				if (device == null || !device.knownFault().assigned().isActive()) {
					selectSingleOption("0", req);
				} else {
					final int val = device.knownFault().assigned().getValue();
					selectSingleOption(val + "", req);
				}
			}
			
		};
		final Collection<DropdownOption> assignmentOpts = AlarmingConfigUtil.ASSIGNEMENT_ROLES.entrySet().stream()
			.map(entry -> new DropdownOption(entry.getKey(), entry.getValue(), "0".equals(entry.getKey())))
			.collect(Collectors.toList());
		createIssueAssigned.setDefaultOptions(assignmentOpts);
		
		final Label createIssueRespLab = new Label(page, "createIssueRespLab", "Responsible");
		final Dropdown responsibleDropdown = new Dropdown(page, "createIssueResponsible") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final GatewaySuperiorData supData = findSuperiorData();
				if (supData == null || !supData.responsibilityContacts().isActive())
					return;
				final InstallAppDevice device = ((DeviceSelectorData) deviceSelector.getData(req)).getSelectedItem();
				final String existingResp = device == null || !device.knownFault().responsibility().isActive() ? null
						: device.knownFault().responsibility().getValue();
				final List<DropdownOption> options = supData.responsibilityContacts().getAllElements().stream()
					.map(contact -> new DropdownOption(
							contact.getName(), contact.userRole().isActive() ? contact.userRole().getValue() :
							contact.firstName().getValue() + " " + contact.lastName().getValue(), 
							(existingResp != null && contact.getSubResource("emailAddress", StringResource.class).isActive() ?
								existingResp.equals(contact.getSubResource("emailAddress", StringResource.class).getValue()) : false)   
					))
					.collect(Collectors.toList());
				setOptions(options, req);
			}
				
		};
		responsibleDropdown.setDefaultAddEmptyOption(true);
		responsibleDropdown.setDefaultMinWidth("8em");
		responsibleDropdown.setDefaultToolTip("Select responsible for the issue");
		createIssueRespLab.setDefaultToolTip("Select responsible for the issue");
		responsibleDropdown.setComparator(RESPONSIBLES_COMPARATOR);
		
		final Label createIssueReminderLab = new Label(page, "createIssueReminderLab", "Next reminder");
		final TextField createIssueReminder = new TextField(page, "createIssueReminder") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final InstallAppDevice device = ((DeviceSelectorData) deviceSelector.getData(req)).getSelectedItem();
				if (device == null || !device.knownFault().dueDateForResponsibility().isActive()) {
					setValue("", req);
				} else {
					final long next = device.knownFault().dueDateForResponsibility().getValue();
					final boolean needsTime = Math.abs(appMan.getFrameworkTime()-next) < 48 * 3_600_000;
					final DateTimeFormatter formatter = needsTime ? DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm") 
							: DateTimeFormatter.ofPattern("yyyy-MM-dd");
					final ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(next), ZoneId.systemDefault());
					final String value;
					if (needsTime)
						 value = zdt.toLocalDateTime().format(formatter);
					else
						value = zdt.toLocalDate().format(formatter);
					setValue(value, req);
				}
			}
			
		};
		createIssueReminder.setDefaultType(TextFieldType.DATE);
		
		final Label createIssueReminderFreqLab = new Label(page, "createIssueReminderLFreqab", "Reminder frequency");
		final Dropdown createIssueReminderFreq = new Dropdown(page, "createIssueReminderFreq") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final InstallAppDevice device = ((DeviceSelectorData) deviceSelector.getData(req)).getSelectedItem();
				if (device == null || !device.knownFault().reminderType().isActive()) {
					selectSingleOption(DropdownData.EMPTY_OPT_ID, req);
				} else {
					final int val = device.knownFault().reminderType().getValue();
					final String selected = val == 1 ? "d" : val == 2 ? "w" : val == 3 ? "m" : DropdownData.EMPTY_OPT_ID;
					selectSingleOption(selected, req);
				}
			}
			
		};
		createIssueReminderFreq.setDefaultAddEmptyOption(true, "Default");
		createIssueReminderFreq.setComparator(null);
		createIssueReminderFreq.setDefaultOptions(REMINDER_FREQUENCY_OPTIONS);
		
		
		final Label createIssueTaskTrackingLab = new Label(page, "createIssueTaskTrackingLab", "Task tracking");
		final TextField createIssueTaskTracking = new TextField(page, "createIssueTaskTrakcing") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final InstallAppDevice device = ((DeviceSelectorData) deviceSelector.getData(req)).getSelectedItem();
				if (device == null || !device.knownFault().linkToTaskTracking().isActive()) {
					setValue("", req);
				} else {
					setValue(device.knownFault().linkToTaskTracking().getValue(), req);
				}
			}
			
		};
		
		final Label devCommentLab = new Label(page, "createIssueDevCommentLab", "Development feature");
		devCommentLab.setDefaultToolTip("Is the issue linked to a feature under developmen/a special development task?");
		final TextField devComment = new TextField(page, "createIssueDevComment") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final InstallAppDevice device = ((DeviceSelectorData) deviceSelector.getData(req)).getSelectedItem();
				if (device == null) {
					setValue("", req);
					return;
				}
				final Resource devRes = device.getSubResource("featureUnderDevelopment");
				if (!(devRes instanceof StringResource) || !devRes.isActive()) {
					setValue("", req);
				} else {
					setValue(((StringResource) devRes).getValue(), req);
				}
			}
			
		};
		int tableRows = 9;
		if (releasePopup != null)
			tableRows += 1;
		final StaticTable createPopupTable = new StaticTable(tableRows, 2, new int[] {4, 8});
		createPopupTable.setContent(0, 0, createIssueDeviceLab).setContent(0, 1, deviceSelector);
		createPopupTable.setContent(1, 0, deviceFaultActiveLab).setContent(1, 1, deviceFaultActive);
		createPopupTable.setContent(2, 0, createIssueCommentLab).setContent(2, 1, createIssueComment);
		createPopupTable.setContent(3, 0, createIssueAssignedLab).setContent(3, 1, createIssueAssigned);
		createPopupTable.setContent(4, 0, createIssueRespLab).setContent(4, 1, responsibleDropdown);
		createPopupTable.setContent(5, 0, createIssueReminderLab).setContent(5, 1, createIssueReminder);
		createPopupTable.setContent(6, 0, createIssueReminderFreqLab).setContent(6, 1, createIssueReminderFreq);
		createPopupTable.setContent(7, 0, createIssueTaskTrackingLab).setContent(7, 1, createIssueTaskTracking);
		createPopupTable.setContent(8, 0, devCommentLab).setContent(8, 1, devComment);
		if (releasePopup != null) {
			directRelease = new Checkbox2(page, "createIssueDirectRelease") {
				/*// always visible; before, only when a new major issue was to be created
				@Override
				public void onGET(OgemaHttpRequest req) {
					final int assignment = Integer.parseInt(createIssueAssigned.getSelectedValue(req));
					boolean visible = false;
					if (assignment > 0) {
						final String role = AlarmingConfigUtil.ASSIGNEMENT_ROLES.get(assignment + "");
						if (role != null && role.toLowerCase().startsWith("op"))
							visible = true;
					}
					if (!visible) {
						final String responsible = responsibleDropdown.getSelectedValue(req);
						if (responsible != null && !DropdownData.EMPTY_OPT_ID.equals(responsible))
							visible = true;
					}
					setWidgetVisibility(visible, req);
				}
				*/ 
				
				
			};
			directRelease.setDefaultCheckboxList(Collections.singleton(new CheckboxEntry("default") {
				
				@Override
				public String label(OgemaLocale arg0) {
					return "";
				}
			}));
			// responsible for opening the release popup
			releaseTrigger = new EmptyWidget(page,"createIssueReleaseTrigger") {

				@Override
				public EmptyData createNewSession() {  // only to avoid an unsupportedoperationexception... a bit annoying
					return new EmptyData(this) {
						
						@Override
						public org.json.JSONObject onPOST(String data, OgemaHttpRequest req) {
							return new org.json.JSONObject();
						}
						
					};
				}
				
			};
			releaseTrigger.triggerAction(releaseTrigger, TriggeringAction.GET_REQUEST, TriggeredAction.POST_REQUEST);
			page.append(releaseTrigger);
			
			final Label directReleaseLab = new Label(page, "createIssueDirectReleaseLab", "Direct Release") {
				/*
				@Override
				public void onGET(OgemaHttpRequest req) {
					final int assignment = Integer.parseInt(createIssueAssigned.getSelectedValue(req));
					boolean visible = false;
					if (assignment > 0) {
						final String role = AlarmingConfigUtil.ASSIGNEMENT_ROLES.get(assignment + "");
						if (role != null && role.toLowerCase().startsWith("op"))
							visible = true;
					}
					if (!visible) {
						final String responsible = responsibleDropdown.getSelectedValue(req);
						if (responsible != null && !DropdownData.EMPTY_OPT_ID.equals(responsible))
							visible = true;
					}
					setWidgetVisibility(visible, req);
				}
				*/
				
			};
			directReleaseLab.setDefaultToolTip("Immediately release the issue, i.e. create it for statistical purposes only?");
			createPopupTable.setContent(9, 0, directReleaseLab).setContent(9, 1, directRelease);
			// no longer necessary as the checkbox directRelease is always shown
			//createIssueAssigned.triggerAction(directReleaseLab, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
			//responsibleDropdown.triggerAction(directReleaseLab, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
			//deviceSelector.triggerAction(directReleaseLab, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
			//createIssueAssigned.triggerAction(directRelease, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
			//responsibleDropdown.triggerAction(directRelease, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
			//deviceSelector.triggerAction(directRelease, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
			
		} else {
			directRelease = null;
			releaseTrigger = null;
		}
		
		
		final PageSnippet bodySnippet = new PageSnippet(page, "createIssueBodySnip", true);
		bodySnippet.append(createPopupTable, null);
		createIssuePopup.setBody(bodySnippet, null);
		
		final Button cancel = new Button(page, "createIssueCancel", "Cancel");
		cancel.setDefaultToolTip("Cancel alarm generation and close this popup");
		final Button submit = new Button(page, "createIssueSubmit", "Submit") {
			
			@Override
			protected void setDefaultValues(ButtonData opt) {
				super.setDefaultValues(opt);
				opt.disable();
			}
			
			@Override
			public void onPOSTComplete(String arg0, OgemaHttpRequest req) {
				final InstallAppDevice device = ((DeviceSelectorData) deviceSelector.getData(req)).getSelectedItem();
				if (device == null) {
					if (alert != null)
						alert.showAlert("Device " + device + " not found", false, req);
					return;
				}
				try {
					final AlarmGroupData alarm = device.knownFault();
					final String comment = createIssueComment.getValue(req).trim();
					if (comment.isEmpty())
						alarm.comment().delete();
					else
						alarm.comment().<StringResource> create().setValue(comment);
					final int assignment = Integer.parseInt(createIssueAssigned.getSelectedValue(req));
					if (assignment == 0)
						alarm.assigned().delete();
					else
						alarm.assigned().<IntegerResource> create().setValue(assignment);
					final boolean isBlocking = assignment < 7000 || assignment >= 8000;
					alarm.minimumTimeBetweenAlarms().<FloatResource> create().setValue(isBlocking ? -1 : 0);
					if (!alarm.isActive())
						alarm.ongoingAlarmStartTime().<TimeResource> create().setValue(appMan.getFrameworkTime());
					final String responsible = responsibleDropdown.getSelectedValue(req);
					if (responsible != null && !DropdownData.EMPTY_OPT_ID.equals(responsible)) {
						final GatewaySuperiorData supData = findSuperiorData();
						if (supData != null && supData.responsibilityContacts().isActive()) {
							final NaturalPerson selected = supData.responsibilityContacts().getSubResource(responsible); 
							final StringResource emailRes = selected.getSubResource("emailAddress");
							final String email = emailRes.isActive() ? emailRes.getValue() : "";
							if (!email.isEmpty())
								alarm.responsibility().<StringResource> create().setValue(email);
						}
						
					}
					final String tt = createIssueTaskTracking.getValue(req).trim();
					if (!tt.isEmpty())
						alarm.linkToTaskTracking().<StringResource> create().setValue(tt);
					final String reminderDate = createIssueReminder.getValue(req);
					if (reminderDate != null && !reminderDate.isEmpty()) {
						try {
							final long nextReminder = LocalDate.parse(reminderDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay(ZoneId.systemDefault())
									.toInstant().toEpochMilli();
							alarm.dueDateForResponsibility().<TimeResource> create().setValue(nextReminder);
							final String reminderFreq = createIssueReminderFreq.getSelectedValue(req);
							if (reminderFreq != null) {
								final int mode;
								switch(reminderFreq) {
								case "d":
									mode = 1;
									break;
								case "w":
									mode = 2;
									break;
								case "m":
									mode = 3;
									break;
								default: 
									mode = -1;
								}
								if (mode > 0)
									alarm.reminderType().<IntegerResource> create().setValue(mode);
							}
							
						} catch (DateTimeParseException ignore) {}  // ok? 
					}
					final String developmentComment = devComment.getValue(req).trim();
					if (!developmentComment.isEmpty())
						alarm.addDecorator("featureUnderDevelopment", StringResource.class).setValue(developmentComment);
					alarm.activate(true);
					// only syncs if issue is eligible; else major is null
					AlarmGroupDataMajor major = SuperiorIssuesSyncUtils.syncIssueToSuperiorIfRelevant(alarm, appMan);
					final boolean doRelease = directRelease.getCheckboxList(req).stream().filter(e -> "default".equals(e.id())).findAny().get().isChecked();
					if (doRelease && major == null)
						major = SuperiorIssuesSyncUtils.syncIssueToSuperior(alarm, appMan);
					if (alert != null)
						alert.showAlert("Alarm generation succeeded for device " + device.deviceId().getValue() + " (" + device.getLocation() + ")", true, req);
					if (releasePopup != null) {
						final boolean release = major != null && doRelease;
						if (release) {
							releasePopup.selectIssue(major, req);
							releaseTrigger.triggerAction(releasePopup.popupWidget(), TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET);
						}
						else {
							releaseTrigger.removeTriggerAction(releasePopup.popupWidget(), TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET);
						}
					}
				} catch (Exception e) {
					if (alert != null)
						alert.showAlert("Alarm generation failed: " + e, false, req);
					else
						appMan.getLogger().error("Alarm generation failed", e);
				}
				// TODO reload page?
			}
			
		};
		submit.setDefaultToolTip("Create alarm");
		submit.addDefaultStyle(ButtonData.BOOTSTRAP_GREEN);
		this.createIssueSubmit = submit;
		
		final Flexbox popupFooter = new Flexbox(page, "createIssueFooter", true);
		popupFooter.setJustifyContent(JustifyContent.FLEX_RIGHT, null);
		popupFooter.addCssItem(">div", Collections.singletonMap("column-gap", "0.5em"), null);
		popupFooter.addItem(cancel, null).addItem(submit, null);
		createIssuePopup.setFooter(popupFooter, null);
		
		page.append(createIssuePopup);
		deviceSelector.triggerAction(deviceFaultActive, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		deviceSelector.triggerAction(createIssueComment, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		deviceSelector.triggerAction(createIssueAssigned, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		deviceSelector.triggerAction(responsibleDropdown, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		deviceSelector.triggerAction(createIssueTaskTracking, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		deviceSelector.triggerAction(createIssueReminder, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		deviceSelector.triggerAction(createIssueReminderFreq, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		deviceSelector.triggerAction(submit, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		cancel.triggerAction(createIssuePopup, TriggeringAction.POST_REQUEST, TriggeredAction.HIDE_WIDGET);
		submit.triggerAction(createIssuePopup, TriggeringAction.POST_REQUEST, TriggeredAction.HIDE_WIDGET);
		if (releasePopup != null)
			releasePopup.trigger(submit);
		if (releaseTrigger != null)
			submit.triggerAction(releaseTrigger, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		if (alert != null)
			submit.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	
	public void trigger(OgemaWidget opener) {
		opener.triggerAction(createIssuePopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET);
	}
	
	
	private GatewaySuperiorData findSuperiorData() {
		final Resource r = appMan.getResourceAccess().getResource("gatewaySuperiorDataRes");
		if (r instanceof GatewaySuperiorData)
			return (GatewaySuperiorData) r;
		return appMan.getResourceAccess().getResources(GatewaySuperiorData.class).stream().findAny().orElse(null);
	}

	private static class DeviceSelectorData extends AutocompleteData {
		
		private Collection<InstallAppDevice> options = Collections.emptySet();
		private InstallAppDevice selectedDevice;

		public DeviceSelectorData(Autocomplete autocomplete) {
			super(autocomplete);
			setMinLength(0);
		}
		
		public void setItems(Collection<InstallAppDevice> devices) {
			this.options = devices;
			if (this.selectedDevice != null && !devices.contains(this.selectedDevice))
				this.selectedDevice = null;
			final List<String> ids =
					Stream.concat(Stream.of(""), devices.stream().map(dev -> dev.deviceId().getValue())).collect(Collectors.toList());
			setOptions(ids);
		}
		
		public InstallAppDevice getSelectedItem() {
			return this.selectedDevice;
		}
		
		@Override
		public void setValue(String value) {
			final Optional<InstallAppDevice> device = 
					value != null ? this.options.stream().filter(app -> app.deviceId().isActive() && value.equals(app.deviceId().getValue())).findAny() : Optional.empty();
			this.selectedDevice = device.orElse(null);
			super.setValue(device.isPresent() ? value : null);
		}
		
		
	}
	
}
