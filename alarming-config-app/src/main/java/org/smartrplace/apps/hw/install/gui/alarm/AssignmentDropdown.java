package org.smartrplace.apps.hw.install.gui.alarm;

import java.util.Set;
import java.util.stream.Collectors;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.smartrplace.apps.alarmingconfig.sync.SuperiorIssuesSyncUtils;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownData;
import de.iwes.widgets.html.form.dropdown.DropdownOption;

@SuppressWarnings("serial")
public class AssignmentDropdown extends Dropdown {
	
	private static final Set<DropdownOption> options = AlarmingConfigUtil.ASSIGNEMENT_ROLES.entrySet().stream()
		.map(entry -> new DropdownOption(entry.getKey(), entry.getValue(), false))
		.collect(Collectors.toSet());

	private final AlarmGroupData alarm;
	private final ApplicationManager appMan;
	
    public AssignmentDropdown(OgemaWidget parent, String id, OgemaHttpRequest req, AlarmGroupData alarm, ApplicationManager appMan) {
		super(parent, id, options, req);
		this.alarm = alarm;
		this.appMan = appMan;
		this.triggerAction(this, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}

	@Override
	public void onGET(OgemaHttpRequest req) {
		String value = alarm.assigned().isActive() ? alarm.assigned().getValue() + "" : DropdownData.EMPTY_OPT_ID;
		selectSingleOption(value, req);
		final String role = AlarmingConfigUtil.ASSIGNEMENT_ROLES.get(value);
		if (role != null)
			setToolTip(role, req);
	}
	
	@Override
	public void onPOSTComplete(String data, OgemaHttpRequest req) {
		final String value = getSelectedValue(req);
		if (DropdownData.EMPTY_OPT_ID.equals(value)) {
			alarm.assigned().delete();
			return;
		}
		try {
			final int valueInt = Integer.parseInt(value);
			alarm.assigned().<IntegerResource> create().setValue(valueInt);
			if(valueInt >= 7000 && valueInt < 8000) { // copied from DeviceKnownFaultsPage
				//Non-Blocking
				ValueResourceHelper.setCreate(alarm.minimumTimeBetweenAlarms(), 0);
			} else {
				//Blocking
				ValueResourceHelper.setCreate(alarm.minimumTimeBetweenAlarms(), -1);
			}
			alarm.assigned().activate(false);
		} catch (Exception e) {
			return;
		} 
		SuperiorIssuesSyncUtils.syncIssueToSuperiorIfRelevant(alarm, appMan); 		
	}
	
	
}
