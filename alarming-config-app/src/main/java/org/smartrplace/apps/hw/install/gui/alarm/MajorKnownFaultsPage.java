package org.smartrplace.apps.hw.install.gui.alarm;

import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmGroupDataMajor;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.user.NaturalPerson;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmconfig.util.AlarmMessageUtil;
import org.smartrplace.apps.alarmconfig.util.AlarmResourceUtil;
import org.smartrplace.apps.alarmingconfig.sync.SuperiorIssuesSyncUtils;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.ThermostatPage;
import org.smartrplace.gateway.device.GatewaySuperiorData;
import org.smartrplace.util.directobjectgui.ObjectGUIHelperBase.ValueResourceDropdownFlex;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.directresourcegui.GUIHelperExtension;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.util.virtualdevice.ChartsUtil;
import org.smartrplace.util.virtualdevice.ChartsUtil.GetPlotButtonResult;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownData;
import de.iwes.widgets.html.form.dropdown.DropdownOption;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.html.html5.flexbox.AlignItems;
import de.iwes.widgets.html.html5.flexbox.JustifyContent;
import de.iwes.widgets.html.popup.Popup;
import de.iwes.widgets.html.textarea.TextArea;
import de.iwes.widgets.html.textarea.TextAreaData;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;

@SuppressWarnings("serial")
public class MajorKnownFaultsPage extends ObjectGUITablePage<AlarmGroupDataMajor, AlarmGroupDataMajor> {
	private final ApplicationManagerPlus appManPlus;
	private final GatewaySuperiorData supData;
	private final HardwareInstallConfig hwInstallConfig;

	private IssueDetailsPopup lastMessagePopup;
	
	private final Popup diagnosisEditorPopup;
	private final Header diagnosisEditorHeader;
	private final DiagnosisEditor diagnosisEditor;
	private final Button diagnosisEditorSubmit; 

	public MajorKnownFaultsPage(WidgetPage<?> page, ApplicationManagerPlus appMan) {
		super(page, appMan.appMan(), AlarmGroupDataMajor.class, false);
		this.appManPlus = appMan;
		this.supData = SuperiorIssuesSyncUtils.getSuperiorData(appMan.appMan());
		hwInstallConfig = appMan.getResourceAccess().getResource("hardwareInstallConfig");
		
		this.lastMessagePopup = new IssueDetailsPopup(page);
		final Button cancel = new Button(page, "diagPopCancel", "Cancel");
		this.diagnosisEditorSubmit = new Button(page, "diagPopSubmit", "Set") {
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				diagnosisEditor.set(req);
			}
			
		};
		this.diagnosisEditorHeader = new Header(page, "diagnosisHeader") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final AlarmGroupData fault = diagnosisEditor.getFaultResource(req);
				final InstallAppDevice device = fault != null ? AlarmResourceUtil.getDeviceForKnownFault(fault) : null;
				if (device == null) {
					setText("", req);
					return;
				}
				if (device.deviceId().isActive())
					setText(device.deviceId().getValue(), req);
				else {
					setText(ResourceUtils.getHumanReadableName(device.device()), req);
				}
			}
			
		};
		diagnosisEditorHeader.setDefaultHeaderType(3);
		diagnosisEditorHeader.setDefaultColor("blue");
		
		
		this.diagnosisEditor = new DiagnosisEditor(page, "diagnosisEditor", diagnosisEditorSubmit);
		diagnosisEditor.setDefaultToolTip("Provide the final analysis result for the device issue.");
		this.diagnosisEditorPopup = new Popup(page, "diagnosisEdiorPopup", true);
		diagnosisEditorPopup.setTitle("Device issue analysis", null);
		diagnosisEditorPopup.setHeader(diagnosisEditorHeader, null);
		diagnosisEditorPopup.setBody(diagnosisEditor, null);
		diagnosisEditorSubmit.addDefaultStyle(ButtonData.BOOTSTRAP_BLUE);
		cancel.triggerAction(diagnosisEditorPopup, TriggeringAction.ON_CLICK, TriggeredAction.HIDE_WIDGET);
		diagnosisEditorSubmit.triggerAction(diagnosisEditorPopup, TriggeringAction.ON_CLICK, TriggeredAction.HIDE_WIDGET);
		
		final Flexbox diagPopFooter = new Flexbox(page, "diagFooterFlex", true);
		diagPopFooter.setJustifyContent(JustifyContent.FLEX_RIGHT, null);
		diagPopFooter.addCssItem(">div", Collections.singletonMap("column-gap", "0.5em"), null);
		diagPopFooter.addItem(cancel, null).addItem(diagnosisEditorSubmit, null);
		diagnosisEditorPopup.setFooter(diagPopFooter, null);
		
		page.append(diagnosisEditorPopup);
		

		triggerPageBuild();
	}

	@Override
	public void addWidgetsAboveTable() {
		page.append(new Header(page, "header", "8b. Major Device Issues"));	
	}

	@Override
	public void addWidgets(AlarmGroupDataMajor res,
			ObjectResourceGUIHelper<AlarmGroupDataMajor, AlarmGroupDataMajor> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		if(req == null) {
			//vh.registerHeaderEntry("Main value");
			vh.registerHeaderEntry("Name");
			vh.registerHeaderEntry("ID");
			vh.registerHeaderEntry("Started");
			vh.registerHeaderEntry("Release");
			vh.registerHeaderEntry("Diagnosis");
			vh.registerHeaderEntry("Message");
			vh.registerHeaderEntry("Details");
			vh.registerHeaderEntry("Comment");
			vh.registerHeaderEntry("Assigned");
			vh.registerHeaderEntry("Task Tracking");
			vh.registerHeaderEntry("Priority");
			vh.registerHeaderEntry("Responsible");
			vh.registerHeaderEntry("Follow-up");
			vh.registerHeaderEntry("TH-Plot");
			vh.registerHeaderEntry("Plot");
			vh.registerHeaderEntry("Delete");
			return;
		}

		InstallAppDevice object;
		if(res.parentForOngoingIssues().isActive()) {
			object = res.parentForOngoingIssues();
		} else {
			String[] vals = res.devicesRelated().getValues();
			if(vals.length == 0)
				return;
			object = appManPlus.dpService().getMangedDeviceResource(vals[0]);
		}
		PhysicalElement device = object.device().getLocationResource();
		final DeviceHandlerProviderDP<?> pe = appManPlus.dpService().getDeviceHandlerProvider(object);
		
		AlarmingDeviceTableBase.addNameWidgetStatic(object, vh, id, req, row, appManPlus.dpService(), pe, hwInstallConfig);
		//vh.stringLabel("ID", id, object.deviceId().getValue(), row);
		
		//vh.stringLabel("Finished", id, ""+res.isFinished().getValue(), row);
		// some special sensor values... priority for display: battery voltage; mainSensorValue, rssi, eq3state
		//final ValueData valueData = getValueData(object, appMan);
		final Label valueField = new Label(mainTable, "valueField" + id, req);
		AlarmMessageUtil.configureAlarmValueLabel(object, appMan, valueField, req, Locale.ENGLISH);
		//valueField.setText(valueData.message, req);
		row.addCell("value", valueField);
		//if (valueData.responsibleResource != null)
		//	valueField.setToolTip("Value resource: " + valueData.responsibleResource.getLocationResource(), req);
		
		vh.timeLabel("Started", id, res.ongoingAlarmStartTime(), row, 0);
		if(res.releaseTime().isActive())
			vh.timeLabel("Release", id, res.releaseTime(), row, 0);
		
		final Flexbox diagFlex = new Flexbox(mainTable, "diagFlex" + id, req);
		final Label diag = new Label(mainTable, "diagLabel" + id, req) {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				if (!res.finalDiagnosis().isActive()) {
					setText("", req);
					setToolTip("", req);
				} else {
					String v = res.finalDiagnosis().getValue();
					if (v.length() > 50)
						v = v.substring(0, 50) + " ...";
					setText(v, req);
					setToolTip(v, req);
				}
			}
			
		};
		//final ValueResourceLabel<StringResource> diag = new ValueResourceLabel<>(mainTable, "diagLabel" + id, req);
		final Button diagMenuOpener = new Button(mainTable, "diagMenuOpener" + id, req) {
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				diagnosisEditor.setTargets(res.finalDiagnosis(), diag, req);  // TODO update diag widget?
				
			}
			
		};
		diagMenuOpener.setDefaultText("Edit");
		diagMenuOpener.setDefaultMaxWidth("4em");
		diagMenuOpener.triggerAction(diagnosisEditorPopup, TriggeringAction.ON_CLICK, TriggeredAction.SHOW_WIDGET);
		diagMenuOpener.triggerAction(diagnosisEditor, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		diagMenuOpener.triggerAction(diagnosisEditorHeader, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		diagMenuOpener.triggerAction(diagnosisEditorSubmit, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		// TODO open diag edit popup and set resource and dependent widget on edit field
		diagFlex.addItem(diag, req).addItem(diagMenuOpener, req);
		diagFlex.addCssItem(">div", Collections.singletonMap("column-gap", "0.5em"), req);
		diagFlex.setAlignItems(AlignItems.BASELINE, req);
		row.addCell("Diagnosis", diagFlex);
		
final Dropdown followupemail = new Dropdown(mainTable, "followup" + id, req) {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final TimeResource followup = res.dueDateForResponsibility();
				final Optional<String> custom = getDropdownOptions(req).stream()
					.map(opt -> opt.id())
					.filter(opt -> opt.startsWith("custom"))
					.findAny();
				final boolean customConfigured = custom.isPresent();
				final boolean needsCustom = followup.exists() && followup.getValue() >= appMan.getFrameworkTime();
				if (!needsCustom) {
					if (customConfigured) {
						setOptions(getDropdownOptions(req).stream()
								.filter(opt -> !opt.id().startsWith("custom"))
								.collect(Collectors.toList()), req);
					}
					return;
				}
				final long target = followup.getValue();
				final String id = "custom" + target;
				if (customConfigured && custom.get().equals(id))
					return;
				final Stream<DropdownOption> standardOpts = getDropdownOptions(req).stream()
					.filter(opt -> !opt.id().startsWith("custom"));
				final long diff = target - appMan.getFrameworkTime();
				final ZonedDateTime targetZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(target), ZoneId.systemDefault());
				final boolean printTime = Math.abs(diff) < 36 * 3_600_000;
				final String dateTime = printTime ? DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(targetZdt) : DateTimeFormatter.ISO_LOCAL_DATE.format(targetZdt);
				final Stream<DropdownOption> customOpt = Stream.of(new DropdownOption(id, dateTime, true));
				final List<DropdownOption> newOptions = Stream.concat(customOpt, standardOpts).collect(Collectors.toList());
				setOptions(newOptions, req);
				selectSingleOption(followup.isActive() ? id : "__EMPTY_OPT__", req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				final String value = getSelectedValue(req);
				if (value == null || value.isEmpty()) // ?
					return;
				final TimeResource followup = res.dueDateForResponsibility();
				if ("__EMPTY_OPT__".equalsIgnoreCase(value)) {
					if (followup.isActive()) {
						followup.deactivate(false);
						if (alert != null && object.device().exists()) {
							alert.showAlert("Email reminder for device " + ResourceUtils.getHumanReadableName(object.device().getLocationResource()) 
								+ " has been cancelled", true, req);
						}
					}
					return;
				}
				final long now0 = appMan.getFrameworkTime();
				final ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now0), ZoneId.systemDefault());
				final long timestamp;
				if (value.startsWith("custom"))
					timestamp = Long.parseLong(value.substring("custom".length()));
				else if (value.endsWith("d") && value.length() < 5) {
					final int days = Integer.parseInt(value.substring(0, value.length()-1));
					timestamp = now.plusDays(days).toEpochSecond()*1000;
				} else if (value.equals("1min")) { // debug option
					timestamp = now.plusMinutes(1).toEpochSecond()*1000;
				} else if (value.equals("nextmonthend"))
					timestamp = now.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()).truncatedTo(ChronoUnit.DAYS).toEpochSecond()*1000;
				else if (value.equals("3months"))
					timestamp = now.plusMonths(3).toEpochSecond()*1000;
				else if (value.equals("august")) {
					final Month month = now.getMonth();
					ZonedDateTime t0 = now;
					if (month.compareTo(Month.AUGUST) > 0 || (month == Month.AUGUST && now.getDayOfMonth() == 31))
						t0 = t0.plusYears(1);
					timestamp = t0.with(Month.AUGUST).with(TemporalAdjusters.lastDayOfMonth()).truncatedTo(ChronoUnit.DAYS).toEpochSecond()*1000;
				} else {
					throw new IllegalArgumentException("unknown follow-up date " + value);
				}
				followup.<TimeResource> create().setValue(timestamp);
				if (timestamp > now0) {
					followup.activate(false);
					if (alert != null && object.device().exists()) {
						alert.showAlert("Email reminder for device " + ResourceUtils.getHumanReadableName(object.device().getLocationResource()) 
							+ " has been configured for " + ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()), true, req);
					}
				}
			}
			
		};
		
		final Button showMsg = new Button(mainTable, "msg" + id, req) {
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				lastMessagePopup.setValues(res, object, device, followupemail, req);
			}
			
		};
		showMsg.setDefaultText("Last message");
		showMsg.setDefaultToolTip("Show the last alarm message sent for this device, which contains some details about the source of the alarm.");
		lastMessagePopup.setTriggers(showMsg);
		row.addCell("Message", showMsg);
		
		final RedirectButton detailsRedirect = new RedirectButton(mainTable, "details" + id, "Details", 
				"/org/smartrplace/alarmingexpert/ongoingbase.html?device=" + object.deviceId().getValue(), req);
		detailsRedirect.setToolTip("View alarm details in new tab", req);
		row.addCell("Details", detailsRedirect);
		
		if(res.exists()) {
			vh.stringEdit("Comment",  id, res.comment(), row, alert, res.comment());
			ValueResourceDropdownFlex<IntegerResource> widgetPlus = new ValueResourceDropdownFlex<IntegerResource>(
					"Assigned"+id, vh, AlarmingConfigUtil.ASSIGNEMENT_ROLES) {
				public void onGET(OgemaHttpRequest req) {
					myDrop.selectItem(res.assigned(), req);
					final String role = AlarmingConfigUtil.ASSIGNEMENT_ROLES.get(String.valueOf(res.assigned().getValue()));
					if (role != null)
						myDrop.setToolTip(role, req);
				}
				@Override
				public void onPrePOST(String data, OgemaHttpRequest req) {
					IntegerResource source = res.assigned();
					if(!source.exists()) {
						source.create();
						source.activate(true);
					}
				}
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					int val = res.assigned().getValue();
					if(val >= 7000 && val < 8000) {
						//Non-Blocking
						ValueResourceHelper.setCreate(res.minimumTimeBetweenAlarms(), 0);
					} else {
						//Blocking
						ValueResourceHelper.setCreate(res.minimumTimeBetweenAlarms(), -1);
					}
				}
			};
			row.addCell("Assigned", widgetPlus.myDrop);
			
			if(!res.linkToTaskTracking().getValue().isEmpty()) {
				RedirectButton taskLink = new RedirectButton(mainTable, "taskLink"+id, "Task Tracking",
						res.linkToTaskTracking().getValue(), req);
				row.addCell(WidgetHelper.getValidWidgetId("Task Tracking"), taskLink);
			}
			final ValueResourceTextField<FloatResource> prioField 
				= new ValueResourceTextField<FloatResource>(mainTable, "prio" + id, res.getSubResource("processingOrder", FloatResource.class), req);
			prioField.setDefaultToolTip("Alarm priority, e.g. 10, 20, 30, ...");
			prioField.setDefaultWidth("4em");
			prioField.triggerAction(prioField, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
			row.addCell("Priority", prioField);
			
			// TODO do we need to display the email address as well?
			final Dropdown responsibleDropdown = new Dropdown(mainTable, "responsible"+id, req) {
				
				@Override
				public void onGET(OgemaHttpRequest req) {
					//final GatewaySuperiorData supData = findSuperiorData();
					if (supData == null || !supData.responsibilityContacts().isActive())
						return;
					final StringResource responsibility = object.knownFault().responsibility();
					final String email = responsibility.isActive() ? responsibility.getValue() : "";
					final NaturalPerson selected = email.isEmpty() ? null : supData.responsibilityContacts().getAllElements().stream()
						.filter(c -> email.equals(c.getSubResource("emailAddress", StringResource.class).getValue()))
						.findAny().orElse(null);
					final List<DropdownOption> options = supData.responsibilityContacts().getAllElements().stream()
						.map(contact -> new DropdownOption(
								contact.getName(), contact.userRole().isActive() ? contact.userRole().getValue() :
								contact.firstName().getValue() + " " + contact.lastName().getValue(), 
								contact.equalsLocation(selected)
						))
						.collect(Collectors.toList());
					setOptions(options, req);
					if (selected != null) {
						final String id = selected.userRole().isActive() ? selected.userRole().getValue() : selected.firstName().getValue() + " " + selected.lastName().getValue();
						setToolTip(id + ": " + email, req);
					} else {
						setToolTip(email.isEmpty() ? "Select responsible" :  email, req);
					}
				}
				
				@Override
				public void onPOSTComplete(String arg0, OgemaHttpRequest req) {
					//final GatewaySuperiorData supData = findSuperiorData();
					if (supData == null || !supData.responsibilityContacts().isActive())
						return;
					final String currentSelected = getSelectedValue(req);
					final StringResource responsibility = object.knownFault().responsibility();
					if (currentSelected == null || currentSelected.isEmpty() || currentSelected.equals(DropdownData.EMPTY_OPT_ID) 
								|| supData.responsibilityContacts().getSubResource(currentSelected) == null) {
						responsibility.delete();
						return;
					}
					final NaturalPerson selected = supData.responsibilityContacts().getSubResource(currentSelected); 
					final StringResource emailRes = selected.getSubResource("emailAddress");
					final String email = emailRes.isActive() ? emailRes.getValue() : "";
					if (email.isEmpty()) { // ?
						return;
					}
					responsibility.<StringResource> create().setValue(email);
					responsibility.activate(false);
				}
				
				
			};
			responsibleDropdown.setDefaultAddEmptyOption(true);
			responsibleDropdown.setDefaultMinWidth("8em");
			responsibleDropdown.setComparator((o1, o2) ->  { // show default roles supervision and terminvereinbarung first
				if (Objects.equals(o1, o2))
					return 0;
				final boolean comp1 = o1.id().indexOf('_') > 0;
				final boolean comp2 = o2.id().indexOf('_') > 0;
				if (comp1 == comp2)
					return o1.id().compareTo(o2.id());
				return comp1 ? 1 : -1;
			});
			responsibleDropdown.triggerAction(responsibleDropdown, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
			row.addCell("Responsible", responsibleDropdown);
			
		}
		
		
		followupemail.setComparator(null);
		followupemail.setDefaultOptions(Arrays.asList(
			new DropdownOption("__EMPTY_OPT__", "inactive", true),
			new DropdownOption("1d", "1 day", false),
			new DropdownOption("2d", "2 days", false),
			new DropdownOption("3d", "3 days", false),
			new DropdownOption("7d", "7 days", false),
			new DropdownOption("30d", "30 days", false),
			new DropdownOption("nextmonthend", "End of next month", false),
			new DropdownOption("3months", "3 months", false),
			new DropdownOption("august", "End of August", false)
		));
		// add a 1 minute option for debugging purposes
		if (Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.devicealarmreminder.debug")) {
			followupemail.setDefaultOptions(Stream.concat(Stream.of(new DropdownOption("1min", "1 minute", false)), followupemail.getDefaultOptions().stream())
				.collect(Collectors.toList()));
		}
		followupemail.setDefaultToolTip("Send a reminder email after the specified period");
		followupemail.setDefaultMinWidth("8em");
		followupemail.triggerAction(followupemail, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		if (alert != null)
			followupemail.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		row.addCell(WidgetHelper.getValidWidgetId("Follow-up"), followupemail);
		
		if(object.device() instanceof Thermostat) {
			Thermostat dev = (Thermostat)object.device();
			final GetPlotButtonResult logResultSpecial = ThermostatPage.getThermostatPlotButton(dev, appManPlus, vh, id, row, req, ScheduleViewerConfigProvAlarm.getInstance());
			row.addCell(WidgetHelper.getValidWidgetId("TH-Plot"), logResultSpecial.plotButton);
		}
		
		final GetPlotButtonResult logResult = ChartsUtil.getPlotButton(id, object, appManPlus.dpService(), appMan, false, vh, row, req, pe,
				ScheduleViewerConfigProvAlarm.getInstance(), null);
		row.addCell("Plot", logResult.plotButton);
		
		GUIHelperExtension.addDeleteButton(null, object, mainTable, id, alert, "Delete", row, vh, req);
	}

	@Override
	public AlarmGroupDataMajor getResource(AlarmGroupDataMajor object, OgemaHttpRequest req) {
		return object;
	}

	@Override
	public Collection<AlarmGroupDataMajor> getObjectsInTable(OgemaHttpRequest req) {
		GatewaySuperiorData sup = SuperiorIssuesSyncUtils.getSuperiorData(appMan);
		List<AlarmGroupDataMajor> majors = sup.majorKnownIssues().getAllElements();
		return majors;
	}
	
	private static class DiagnosisEditor extends TextArea {

		private final Button submitButton;
		
		public DiagnosisEditor(WidgetPage<?> page, String id, Button submitButton) {
			super(page, id);
			setDefaultWidth("100%");
			this.submitButton = submitButton;
		}
		
		@Override
		public TextAreaData createNewSession() {
			return new DiagnosisEditorData(this, submitButton);
		}
		
		void setTargets(StringResource target, OgemaWidget dependent, OgemaHttpRequest req) {
			((DiagnosisEditorData) getData(req)).setTargets(target, dependent, req);
		}
		
		AlarmGroupData getFaultResource(OgemaHttpRequest req) {
			final StringResource target = ((DiagnosisEditorData) getData(req)).targetResource;
			final Resource parent = target != null ? target.getParent() : null;
			if (!(parent instanceof AlarmGroupData))
				return null;
			return (AlarmGroupData) parent;
		}
		
		boolean set(OgemaHttpRequest req) {
			final StringResource target = ((DiagnosisEditorData) getData(req)).targetResource;

			if (target == null)
				return false;
			String text = getText(req);
			if (text != null)
				text = text.trim();
			if (text == null || text.isEmpty()) {
				target.deactivate(false);
			} else {
				target.<StringResource> create().setValue(getText(req));
				target.activate(false);
			}
			return true;
		}
		
		private static class DiagnosisEditorData extends TextAreaData {
			
			private final Button submitButton;
			StringResource targetResource;
			OgemaWidget dependentWidget;

			public DiagnosisEditorData(TextArea textArea, Button submitButton) {
				super(textArea);
				this.submitButton = submitButton;
			}
			
			void setTargets(StringResource target, OgemaWidget dependent, OgemaHttpRequest req) {
				if (!Objects.equals(target, targetResource)) {
					this.targetResource = target;
					final String newValue = target != null && target.isActive() ? target.getValue() : "";
					setText(newValue);
				} 
				if (!Objects.equals(dependent, dependentWidget)) {
					if (this.dependentWidget != null)
						submitButton.removeTriggerAction(dependentWidget, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
					if (dependent != null)
						submitButton.triggerAction(dependent, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
					this.dependentWidget = dependent;
				}
			}
			
		}
		
	}

}
