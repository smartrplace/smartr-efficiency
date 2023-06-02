package org.smartrplace.apps.alarmingconfig.sync;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.resourcemanager.ResourceOperationException;
import org.ogema.core.resourcemanager.transaction.ResourceTransaction;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmGroupDataMajor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gateway.device.GatewaySuperiorData;

public class SuperiorIssuesSyncUtils {
	
	/**
	 * Copy the content of the passed issue information into a new resource that is synchronized with a superior gateway. The original issue resource
	 * will be replaced by a reference to the new one.
	 *  
	 * Assumes that a GatewaySuperiorData configuration resource already exists, preferably at the default location "gatewaySuperiorDataRes".
	 * @param issue
	 * @param appMan
	 * @return
	 */
	public static AlarmGroupDataMajor syncIssueToSuperior(AlarmGroupData issue, ApplicationManager appMan) throws ResourceOperationException {
		if (issue instanceof AlarmGroupDataMajor)  // if it is synced already, do not create another issue
			return (AlarmGroupDataMajor) issue;
		final Resource r = appMan.getResourceAccess().getResource("gatewaySuperiorDataRes");
		if (r instanceof GatewaySuperiorData)
			return syncIssueToSuperior(issue, appMan, (GatewaySuperiorData) r);
		final List<GatewaySuperiorData> superiors = appMan.getResourceAccess().getResources(GatewaySuperiorData.class);
		if (superiors.isEmpty())
			return null;
		return syncIssueToSuperior(issue, appMan, superiors.get(0));
	}
	
	/**
	 * Copy the content of the passed issue information into a new resource that is synchronized with a superior gateway. The original issue resource
	 * will be replaced by a reference to the new one.
	 *  
	 * Assumes that a GatewaySuperiorData configuration resource already exists, preferably at the default location "gatewaySuperiorDataRes".
	 * @param issue
	 * @param appMan
	 * @return
	 */
	public static AlarmGroupDataMajor syncIssueToSuperior(AlarmGroupData issue, ApplicationManager appMan, GatewaySuperiorData superiorConfig) 
				throws ResourceOperationException {
		if (issue instanceof AlarmGroupDataMajor)  // if it is synced already, do not create another issue
			return (AlarmGroupDataMajor) issue;
		final AlarmGroupDataMajor major = superiorConfig.majorKnownIssues().<ResourceList<AlarmGroupDataMajor>> create().add();
		if (!superiorConfig.majorKnownIssues().isActive())
			superiorConfig.majorKnownIssues().activate(false);
		final ResourceTransaction transaction = ResourceUtils.prepareCopy(issue, major, appMan.getResourceAccess());
		final Resource pp = issue.getParent();
		if (pp instanceof InstallAppDevice)
			transaction.setAsReference(major.parentForOngoingIssues(), pp);
		transaction.setAsReference(issue, major);
		transaction.commit();
		return major;
	}
	

}
