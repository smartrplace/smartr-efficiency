package org.smartrplace.apps.hw.install.gui.alarm;

import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmconfig.util.AlarmMessageUtil;
import org.smartrplace.apps.alarmconfig.util.AlarmMessageUtil.ReminderFrequency;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownOption;

@SuppressWarnings("serial")
public class FollowUpDropdown extends Dropdown {
	
	private static List<DropdownOption> DEFAULT_OPTIONS = Arrays.asList(
			new DropdownOption("__EMPTY_OPT__", "inactive", true),
			new DropdownOption("1d", "1 day", false),
			new DropdownOption("+1d", "Daily", false),
			new DropdownOption("2d", "2 days", false),
			new DropdownOption("3d", "3 days", false),
			new DropdownOption("7d", "7 days", false),
			new DropdownOption("+7d", "Weekly", false),
			new DropdownOption("30d", "30 days", false),
			new DropdownOption("nextmonthend", "End of next month", false),
			new DropdownOption("+30d", "Monthly", false),
			new DropdownOption("3months", "3 months", false),
			new DropdownOption("august", "End of August", false)
		);

	private final ApplicationManager appMan;
	private final Alert alert;
	private final InstallAppDevice device;  // may be null (in create issue popup)
  	private final AlarmGroupData alarm;     // may be null
	
  	public FollowUpDropdown(WidgetPage<?> page, String id, ApplicationManager appMan, Alert alert) {
		super(page, id);
		this.appMan = appMan;
		this.alert = alert;
		this.device = null;
		this.alarm = null;
		setComparator(null);
		setDefaultOptions(DEFAULT_OPTIONS);
		// add a 1 minute option for debugging purposes
		if (Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.devicealarmreminder.debug")) {
			setDefaultOptions(Stream.concat(Stream.of(new DropdownOption("1min", "1 minute", false)), getDefaultOptions().stream())
				.collect(Collectors.toList()));
		}
		setDefaultToolTip("Send a reminder email after the specified period");
		setDefaultMinWidth("8em");
		this.triggerAction(this, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		if (alert != null)
			this.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
  	}
  	
	public FollowUpDropdown(OgemaWidget parent, String id, OgemaHttpRequest req,
			ApplicationManager appMan, Alert alert, InstallAppDevice object) {
		super(parent, id, req);
		this.appMan = appMan;
		this.alert = alert;
		this.device = object;
		this.alarm = object != null ? object.knownFault() : null;
		setComparator(null);
		setDefaultOptions(DEFAULT_OPTIONS);
		// add a 1 minute option for debugging purposes
		if (Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.devicealarmreminder.debug")) {
			setDefaultOptions(Stream.concat(Stream.of(new DropdownOption("1min", "1 minute", false)), getDefaultOptions().stream())
				.collect(Collectors.toList()));
		}
		setDefaultToolTip("Send a reminder email after the specified period");
		setDefaultMinWidth("8em");
		this.triggerAction(this, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		if (alert != null)
			this.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}

	@Override
	public void onGET(OgemaHttpRequest req) {
		final TimeResource followup = alarm != null ?  alarm.dueDateForResponsibility() : null;
		final Optional<String> custom = getDropdownOptions(req).stream()
			.map(opt -> opt.id())
			.filter(opt -> opt.startsWith("custom"))
			.findAny();
		final boolean customConfigured = custom.isPresent();
		final boolean needsCustom = followup != null && followup.exists() && followup.getValue() >= appMan.getFrameworkTime();
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
		if (alarm == null || device == null)  // in create popup
			return;
		final String value = getSelectedValue(req);
		if (value == null || value.isEmpty()) // ?
			return;
		final FollowUpSettings settings = getSelectedTimestamp(req);
		AlarmMessageUtil.setNextReminder(settings.timestamp, settings.reminderType, alarm, device, 
				appMan, alert, req);
		
		final TimeResource followup = alarm.dueDateForResponsibility();
		if (settings.timestamp == -1l) {
			if (followup.isActive()) {
				followup.deactivate(false);
				if (alert != null && device.device().exists()) {
					alert.showAlert("Email reminder for device " + ResourceUtils.getHumanReadableName(device.device().getLocationResource()) 
						+ " has been cancelled", true, req);
				}
			}
			return;
		}
		followup.<TimeResource> create().setValue(settings.timestamp);
		final long now0 = appMan.getFrameworkTime();
		if (settings.timestamp > now0) {
			followup.activate(false);
			if (alert != null && device.device().exists()) {
				alert.showAlert("Email reminder for device " + ResourceUtils.getHumanReadableName(device.device().getLocationResource()) 
					+ " has been configured for " + ZonedDateTime.ofInstant(Instant.ofEpochMilli(settings.timestamp), ZoneId.systemDefault()), true, req);
			}
		}
		if (settings.reminderType != ReminderFrequency.NONE)  
			ValueResourceHelper.setCreate(alarm.reminderType(), settings.reminderType.getCode());
		else
			alarm.reminderType().delete();
	}
	
	/**
	 * 
	 * @return -1 for empty selection, unix timestamp else
	 */
	public FollowUpSettings getSelectedTimestamp(OgemaHttpRequest req) {
		final String value = getSelectedValue(req);
		if (value == null || value.isEmpty() || "__EMPTY_OPT__".equalsIgnoreCase(value))
			return new FollowUpSettings();
		final long now0 = appMan.getFrameworkTime();
		final ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now0), ZoneId.systemDefault());
		if (value.startsWith("custom"))
			return new FollowUpSettings(Long.parseLong(value.substring("custom".length())));
		final long timestamp;
		if (value.endsWith("d") && value.length() < 5) {
			final int days;
			final ReminderFrequency reminderType;
			if(value.startsWith("+")) {
				days = Integer.parseInt(value.substring(1, value.length()-1));
				if(days < 6)
					reminderType=ReminderFrequency.DAILY;
				else if(days < 25)
					reminderType= ReminderFrequency.WEEKLY;
				else
					reminderType=ReminderFrequency.MONTHLY;
			} else {
				days = Integer.parseInt(value.substring(0, value.length()-1));
				reminderType = ReminderFrequency.DEFAULT;
			}			
			long startOfDay0 = AbsoluteTimeHelper.getIntervalStart(now0, AbsoluteTiming.DAY);
			ZonedDateTime startOfDay = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startOfDay0), ZoneId.systemDefault());
			timestamp = startOfDay.plusDays(days).plusMinutes(4*60).toEpochSecond()*1000;
			return new FollowUpSettings(timestamp, reminderType);
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
		return new FollowUpSettings(timestamp);
	}
	
	public static class FollowUpSettings {
	
		/**
		 * -1: not selected
		 */
		public final long timestamp;
		
		public final ReminderFrequency reminderType;

		public FollowUpSettings() {
			this(-1);
		}
		
		public FollowUpSettings(long timestamp) {
			this(timestamp, ReminderFrequency.NONE);
		}
		
		public FollowUpSettings(long timestamp, ReminderFrequency frequency) {
			this.timestamp = timestamp;
			this.reminderType = frequency;
		}		
		
	}
	
}
