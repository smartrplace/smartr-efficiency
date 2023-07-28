package org.smartrplace.apps.alarmconfig.util;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmGroupDataMajor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gateway.device.GatewaySuperiorData;

public class AlarmResourceUtil {

	/**
	 * Subresource of the responsible NaturalPerson, that determines whether or not to aggregate for this recipient.
	 * Type: StringResource
	 */
	public static final String EMAIL_AGGREGATION_SUBRESOURCE = "emailAggregation";
	
	/**
	 * Subresource of AlarmGroupData. This is an internal subresource, no need to specify a model for it.
	 * Type: PendingEmail
	 */
	public static final String PENDING_REMINDER_EMAIL_SUBRESOURCE = "pendingReminder";
	
	private AlarmResourceUtil() {}

	/**
	 * Find the device (InstallAppDevice resource) associated to a known device issue (AlarmGroupData resource).
	 * @param issue
	 * @return should not normally return null, but it can happen in case of misconfigured resources, 
	 * 	 	so the caller is advised to handle this case. 
	 */
	public static InstallAppDevice getDeviceForKnownFault(AlarmGroupData issue) {
		if (issue == null)
			return null;
		if (issue instanceof AlarmGroupDataMajor && ((AlarmGroupDataMajor) issue).parentForOngoingIssues().isActive())
			return ((AlarmGroupDataMajor) issue).parentForOngoingIssues().getLocationResource();
		final Resource parent = issue.getParent();
		if (parent instanceof InstallAppDevice)
			return (InstallAppDevice) parent;
		return null;
	}
	
	
	/**
	 * Deletes the issue, unless this is a major issue (type AlarmGroupDataMajor) synced to superior, 
	 * in which case only the reference from the device is deleted (InstallAppDevice#knownFault()).
	 * @param issue
	 * @param now
	 */
	public static void release(AlarmGroupData issue, long now) {
		if (issue instanceof AlarmGroupDataMajor) {
			// these are major issues that are also synced to superior
			// delete reference from InstallAppDevice to this one, but keep the main, synchronized resource
			final AlarmGroupDataMajor major = issue.getLocationResource();
			if (!major.releaseTime().isActive() || major.releaseTime().getValue() <= 0) {
				major.releaseTime().<TimeResource> create().setValue(now);
				major.releaseTime().activate(false);
			}
			major.getReferencingNodes(false).stream().filter(r -> r.getParent() instanceof InstallAppDevice).forEach(Resource::delete);
		} else {
			issue.delete();
		}
		TimeResource followup = issue.dueDateForResponsibility();
		if (followup.isActive())
			followup.deactivate(false);
	}
	
	public static boolean isReleased(AlarmGroupData issue) {
		return issue instanceof AlarmGroupDataMajor 
				&& ((AlarmGroupDataMajor) issue).releaseTime().isActive() 
				&& ((AlarmGroupDataMajor) issue).releaseTime().getValue() > 0;
	}
	
	public static GatewaySuperiorData findSuperiorData(ApplicationManager appMan) {
		final Resource r = appMan.getResourceAccess().getResource("gatewaySuperiorDataRes");
		if (r instanceof GatewaySuperiorData)
			return (GatewaySuperiorData) r;
		return appMan.getResourceAccess().getResources(GatewaySuperiorData.class).stream().findAny().orElse(null);
	}
	
}
