package org.smartrplace.apps.alarmconfig.util;

import org.ogema.core.model.Resource;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmGroupDataMajor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class AlarmResourceUtil {

	private AlarmResourceUtil() {}

	/**
	 * Find the device (InstallAppDevice resource) associated to a known device issue (AlarmGroupData resource).
	 * @param issue
	 * @return should not normally return null, but it can happen in case of misconfigured resources, 
	 * 	 	so the caller is advised to handle this case. 
	 */
	public static InstallAppDevice getDeviceForKnownFault(AlarmGroupData issue) {
		if (issue instanceof AlarmGroupDataMajor && ((AlarmGroupDataMajor) issue).parentForOngoingIssues().isActive())
			return ((AlarmGroupDataMajor) issue).parentForOngoingIssues().getLocationResource();
		final Resource parent = issue.getParent();
		if (parent instanceof InstallAppDevice)
			return (InstallAppDevice) parent;
		return null;
	}
	
}
