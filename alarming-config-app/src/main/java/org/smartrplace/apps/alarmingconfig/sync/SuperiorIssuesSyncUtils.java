package org.smartrplace.apps.alarmingconfig.sync;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.resourcemanager.ResourceOperationException;
import org.ogema.core.resourcemanager.transaction.ResourceTransaction;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
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
	public static GatewaySuperiorData getSuperiorData(ApplicationManager appMan) throws ResourceOperationException {
		final Resource r = appMan.getResourceAccess().getResource("gatewaySuperiorDataRes");
		if (r instanceof GatewaySuperiorData)
			return (GatewaySuperiorData) r;
		final List<GatewaySuperiorData> superiors = appMan.getResourceAccess().getResources(GatewaySuperiorData.class);
		if (superiors.isEmpty())
			return null;
		return superiors.get(0);
	}
	public static AlarmGroupDataMajor syncIssueToSuperior(AlarmGroupData issue, ApplicationManager appMan) throws ResourceOperationException {
		if (issue instanceof AlarmGroupDataMajor)  // if it is synced already, do not create another issue
			return (AlarmGroupDataMajor) issue;
		GatewaySuperiorData sup = getSuperiorData(appMan);
		return syncIssueToSuperior(issue, appMan, sup);
		/*final Resource r = appMan.getResourceAccess().getResource("gatewaySuperiorDataRes");
		if (r instanceof GatewaySuperiorData)
			return syncIssueToSuperior(issue, appMan, (GatewaySuperiorData) r);
		final List<GatewaySuperiorData> superiors = appMan.getResourceAccess().getResources(GatewaySuperiorData.class);
		if (superiors.isEmpty())
			return null;
		return syncIssueToSuperior(issue, appMan, superiors.get(0));*/
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

	/** Copy issue to be synchronized to superior gateway if this is shall be done as default. If already synchronized return this resource anyways.
	 * If copy is done: Copy the content of the passed issue information into a new resource that is synchronized with a superior gateway. The original issue resource
	 * will be replaced by a reference to the new one.
	 *  
	 * Assumes that a GatewaySuperiorData configuration resource already exists, preferably at the default location "gatewaySuperiorDataRes".
	 * @param issue
	 * @param appMan
	 * @return
	 */
	public static AlarmGroupDataMajor syncIssueToSuperiorIfRelevant(AlarmGroupData issue, ApplicationManager appMan) {
		if (issue instanceof AlarmGroupDataMajor)  // if it is synced already, do not create another issue
			return (AlarmGroupDataMajor) issue;
		if(!eligibleForSync(issue))
			return null;
		return syncIssueToSuperior(issue, appMan);
	};
	
	public static boolean eligibleForSync(AlarmGroupData issue) {
		if (issue == null || !issue.isActive())
			return false;
		if (issue.assigned().isActive()) {
			final String role = AlarmingConfigUtil.ASSIGNEMENT_ROLES.get(issue.assigned().getValue() + "");
			if (role != null && role.toLowerCase().startsWith("op"))
				return true;
		}
		return issue.responsibility().isActive();
	}
	
	/**
	 * Checks known device faults below hardwareInstallConfig/knownDevices if they should be synced to superior, 
	 * and if so converts them to resources of type AlarmGroupDataMajor, below gatewaySuperiorDataRes.
	 * Note that preferably all AlarmGroupData resources should be converted to major type resources and synced to
	 * superior as soon as the relevant conditions are satisfied, so this mehtod is a cleanup operation for missed issues.
	 * @param appMan
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static long checkIssuesSyncStatus(ApplicationManager appMan) {
		final Resource knownDevices = appMan.getResourceAccess().getResource("hardwareInstallConfig/knownDevices");
		if (!(knownDevices instanceof ResourceList) || !knownDevices.isActive())
			return 0;
		return ((ResourceList<InstallAppDevice>) knownDevices).getAllElements().stream()
			.map(cfg -> cfg.knownFault())
			.filter(SuperiorIssuesSyncUtils::eligibleForSync)
			.map(issue -> SuperiorIssuesSyncUtils.syncIssueToSuperior(issue, appMan))
			.count();
		
	}
	
	
}
