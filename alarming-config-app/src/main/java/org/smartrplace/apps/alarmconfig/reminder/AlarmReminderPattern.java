package org.smartrplace.apps.alarmconfig.reminder;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.model.extended.alarming.AlarmGroupData;

public class AlarmReminderPattern extends ResourcePattern<AlarmGroupData> {

	public AlarmReminderPattern(Resource match) {
		super(match);
	}

	public final TimeResource dueDate = model.dueDateForResponsibility();
	
	@Existence(required = CreateMode.OPTIONAL)
	public final IntegerResource reminderType = model.reminderType();

	@Existence(required = CreateMode.OPTIONAL)
	public final StringResource responsible = model.responsibility();
	
	@Existence(required = CreateMode.OPTIONAL)
	public final IntegerResource releaseStatus = model.forRelease();
	
	public boolean isActive() {
		if (!dueDate.isActive())  // should not normally be required, just a safeguard
			return false;
		// see AlarmGroupDataMajor#releaseTime()
		final Resource releaseTime = model.getSubResource("releaseTime");
		if (releaseTime instanceof TimeResource && releaseTime.isActive() && ((TimeResource) releaseTime).getValue() > 0)
			return false;
		return true;
	}
	
	
}
