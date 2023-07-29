package org.smartrplace.apps.hw.install.gui.alarm;

import java.util.List;
import java.util.stream.Collectors;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.user.NaturalPerson;
import org.smartrplace.apps.alarmconfig.util.AlarmResourceUtil;
import org.smartrplace.apps.alarmingconfig.sync.SuperiorIssuesSyncUtils;
import org.smartrplace.gateway.device.GatewaySuperiorData;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownData;
import de.iwes.widgets.html.form.dropdown.DropdownOption;

@SuppressWarnings("serial")
public class ResponsibleDropdown extends Dropdown {

	private final ApplicationManager appMan;
	private final AlarmGroupData res;
	private final Runnable majorTrafoCallback;
	
	public ResponsibleDropdown(OgemaWidget parent, String id, OgemaHttpRequest req,
			ApplicationManager appMan, AlarmGroupData alarm, Runnable majorTrafoCallback) {
		super(parent, id, req);
		this.appMan = appMan;
		this.res = alarm;
		this.majorTrafoCallback = majorTrafoCallback;
		this.setDefaultAddEmptyOption(true);
		this.setDefaultMinWidth("8em");
		this.setComparator(CreateIssuePopup.RESPONSIBLES_COMPARATOR);
		this.triggerAction(this, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	 
	@Override
	public void onGET(OgemaHttpRequest req) {
		final GatewaySuperiorData supData = AlarmResourceUtil.findSuperiorData(appMan);
		if (supData == null || !supData.responsibilityContacts().isActive())
			return;
		final StringResource responsibility = res.responsibility();
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
		final GatewaySuperiorData supData = AlarmResourceUtil.findSuperiorData(appMan);
		if (supData == null || !supData.responsibilityContacts().isActive())
			return;
		final String currentSelected = getSelectedValue(req);
		final StringResource responsibility = res.responsibility();
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
		if (SuperiorIssuesSyncUtils.syncIssueToSuperiorIfRelevant(res, appMan) != null && majorTrafoCallback != null) {
			majorTrafoCallback.run();
			// delete old release button and replace by new one...
			//updateReleaseBtn(res, releaseBtnRef, releaseCnt, releaseBtnSnippet, id, req);
		}
	}
	

}
