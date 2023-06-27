package org.smartrplace.apps.hw.install.gui.alarm;

import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownData;
import de.iwes.widgets.html.form.dropdown.DropdownOption;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.popup.Popup;
import de.iwes.widgets.resource.widget.calendar.DatepickerTimeResourceTextField;
import de.iwes.widgets.resource.widget.calendar.DatepickerTimeResourceTextField.DatepickerTimeResourceTextFieldData;

@SuppressWarnings("serial")
public class IssueDetailsPopup {
	
	private static final List<DropdownOption> REMINDER_FREQUENCY_OPTIONS = Arrays.asList(
		new DropdownOption("d", "daily", false),
		new DropdownOption("w", "weekly", false),
		new DropdownOption("m", "monthly", false)
	);
	
	private final Popup lastMessagePopup;
	private final Label lastMessageDevice;
	private final Label lastMessageRoom;
	private final Label lastMessageLocation;
	private final Label lastMessage;
	private final Label lastMessageComment;
	private final Label lastMessageTaskTracking;
	private final Label lastMessageAssigned;
	private final Label lastMessageResponsible;
	private final PopupReminderSelector lastMessageReminderDatetime;
	private final ReminderFrequencyDropdown lastMessageReminderFrequency;
	
	public IssueDetailsPopup(WidgetPage<?> page) {
		this.lastMessagePopup = new Popup(page, "lastMessagePopup", true);
		lastMessagePopup.setDefaultTitle("Last alarm message");
		this.lastMessageDevice = new Label(page, "lastMessagePopupDevice");
		this.lastMessageRoom = new Label(page, "lastMessagePopupRoom");
		this.lastMessageLocation = new Label(page, "lastMessagePopupLocation");
		this.lastMessage = new Label(page, "lastMessage");
		this.lastMessageComment = new Label(page, "lastMessagePopupComment");
		this.lastMessageTaskTracking = new Label(page, "lastMessagePopupTT");
		this.lastMessageAssigned = new Label(page, "lastMessagePopupAssigned");
		this.lastMessageResponsible = new Label(page, "lastMessagePopupResponsible");
		this.lastMessageReminderDatetime = new PopupReminderSelector(page, "lastMessageReminderTime");
		this.lastMessageReminderFrequency = new ReminderFrequencyDropdown(page, "lastMessageReiminderFrequency");
		lastMessageReminderFrequency.setDefaultOptions(REMINDER_FREQUENCY_OPTIONS);
		lastMessageReminderFrequency.setDefaultAddEmptyOption(true, "Default");
		
		
		final StaticTable tab = new StaticTable(10, 2, new int[]{3, 9});
		tab.setContent(0, 0, "Device").setContent(0, 1, lastMessageDevice)
			.setContent(1, 0, "Room").setContent(1, 1, lastMessageRoom)
			.setContent(2, 0, "Location").setContent(2, 1, lastMessageLocation)
			.setContent(3, 0, "Message").setContent(3,1, lastMessage)
			.setContent(4, 0, "Comment").setContent(4, 1, lastMessageComment)
			.setContent(5, 0, "Assigned").setContent(5, 1, lastMessageAssigned)
			.setContent(6, 0, "Responsible").setContent(6, 1, lastMessageResponsible)
			.setContent(7, 0, "Task tracking").setContent(7, 1, lastMessageTaskTracking)
			.setContent(8, 0, "Next reminder").setContent(8, 1, lastMessageReminderDatetime)
			.setContent(9, 0, "Reminder frequency").setContent(9, 1, lastMessageReminderFrequency);
		final PageSnippet snip = new PageSnippet(page, "lastMessageSnip", true);
		snip.append(tab, null);
		lastMessagePopup.setBody(snip, null);
		final Button closeLastMessage = new Button(page, "lastMessageClose", "Close") {
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				lastMessageReminderDatetime.setTableReminder(null, req);
			}
			
		};
		closeLastMessage.triggerAction(lastMessagePopup, TriggeringAction.ON_CLICK, TriggeredAction.HIDE_WIDGET);
		closeLastMessage.triggerAction(lastMessageReminderDatetime, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		lastMessagePopup.setFooter(closeLastMessage, null);
		lastMessagePopup.setMinWidth("35em", null);
		page.append(lastMessagePopup);
	}
	
	public void setTriggers(OgemaWidget showMsg) {
		showMsg.triggerAction(lastMessagePopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET);
		showMsg.triggerAction(lastMessageDevice,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		showMsg.triggerAction(lastMessageRoom,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		showMsg.triggerAction(lastMessageLocation,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		showMsg.triggerAction(lastMessageComment,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		showMsg.triggerAction(lastMessageAssigned,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		showMsg.triggerAction(lastMessageTaskTracking,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		showMsg.triggerAction(lastMessageResponsible,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		showMsg.triggerAction(lastMessageReminderDatetime,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		showMsg.triggerAction(lastMessageReminderFrequency,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		showMsg.triggerAction(lastMessage,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	
	public void setValues(AlarmGroupData issue, InstallAppDevice knownDevice, PhysicalElement device, Dropdown followupemail, OgemaHttpRequest req) {
		lastMessage.setText(issue.lastMessage().getValue(), req);
		lastMessageDevice.setText(knownDevice.deviceId().getValue(), req);
		final String room = device.location().room().isActive() ? ResourceUtils.getHumanReadableShortName(device.location().room()) : "--";
		lastMessageRoom.setText(room, req);
		lastMessageLocation.setText(getOrEmpty(knownDevice.installationLocation()), req);
		lastMessageComment.setText(getOrEmpty(issue.comment()), req);
		String role = "--";
		if (issue.assigned().isActive()) {
			final int assigned = issue.assigned().getValue();
			final String roleAssigned = AlarmingConfigUtil.ASSIGNEMENT_ROLES.get(String.valueOf(assigned));
			if (roleAssigned != null)
				role = roleAssigned;
		}
		lastMessageAssigned.setText(role, req);
		lastMessageTaskTracking.setText(getOrEmpty(issue.linkToTaskTracking()), req);
		lastMessageResponsible.setText(getOrEmpty(issue.responsibility()), req);
		lastMessageReminderDatetime.selectItem(issue.dueDateForResponsibility(), req);
		lastMessageReminderDatetime.setTableReminder(followupemail, req);
		lastMessageReminderFrequency.setAlarm(issue, req);
	}
	
	private String getOrEmpty(final StringResource res) {
		return res.isActive() ? res.getValue() : "--";
	}
	
	private static class PopupReminderSelector extends DatepickerTimeResourceTextField {

		public PopupReminderSelector(WidgetPage<?> page, String id) {
			super(page, id);
		}
		
		@Override
		public DatepickerTimeResourceTextFieldData createNewSession() {
			return new PopupReminderSelectorData(this);
		}
		
		void setTableReminder(Dropdown reminder, OgemaHttpRequest req) {
			final Dropdown oldReminder = getTableReminder(req);
			if (oldReminder != null)
				this.removeTriggerAction(oldReminder, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
			if (reminder != null)
				this.triggerAction(reminder, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
			((PopupReminderSelectorData) getData(req)).tableReminderField = reminder;
		}
		
		Dropdown getTableReminder(OgemaHttpRequest req) {
			return ((PopupReminderSelectorData) getData(req)).tableReminderField;
		}
	}
	
	
	private static class PopupReminderSelectorData extends DatepickerTimeResourceTextFieldData {
		
		Dropdown tableReminderField = null;

		public PopupReminderSelectorData(DatepickerTimeResourceTextField dtr) {
			super(dtr);
		}
		
		@Override
		public JSONObject onPOST(String data, OgemaHttpRequest req) {
			return super.onPOST(data, req);
		}
		
	}
	
	private static class ReminderFrequencyDropdown extends Dropdown {
		
		public ReminderFrequencyDropdown(WidgetPage<?> page, String id) {
			super(page, id);
			setComparator(null);
		}

		@Override
		public void onPOSTComplete(String data, OgemaHttpRequest req) {
			final AlarmGroupData alarm = ((AlarmData) getData(req)).alarm;
			if (alarm == null)
				return;
			final String selected = getSelectedValue(req);
			final int mode;
			switch(selected) {
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
			if (mode <= 0 && alarm.reminderType().exists())
				alarm.reminderType().deactivate(false);
			else {
				alarm.reminderType().<IntegerResource> create().setValue(mode);
				alarm.reminderType().activate(false);
			}
		}
		
		@Override
		public void onGET(OgemaHttpRequest req) {
			final AlarmGroupData alarm = ((AlarmData) getData(req)).alarm;
			if (alarm == null || !alarm.reminderType().isActive()) {
				selectSingleOption(DropdownData.EMPTY_OPT_ID, req);
			} else {
				final int type = alarm.reminderType().getValue();
				final String selected = type == 1 ? "d" : type == 2 ? "w" : type == 3 ? "m" : DropdownData.EMPTY_OPT_ID;
				selectSingleOption(selected, req);
			}
		}
		
		@Override
		public DropdownData createNewSession() {
			return new AlarmData(this);
		}
		
		void setAlarm(AlarmGroupData alarm, OgemaHttpRequest req) {
			((AlarmData) getData(req)).alarm = alarm;
		}
		
		class AlarmData extends DropdownData {

			AlarmGroupData alarm;
			
			public AlarmData(Dropdown dropdown) {
				super(dropdown);
			}
			
		}
		
	}
	

}
