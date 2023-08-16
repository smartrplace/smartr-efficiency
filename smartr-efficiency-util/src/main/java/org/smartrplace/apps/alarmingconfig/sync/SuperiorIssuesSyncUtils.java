package org.smartrplace.apps.alarmingconfig.sync;

import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.resourcemanager.ResourceOperationException;
import org.ogema.core.resourcemanager.transaction.ResourceTransaction;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmGroupDataMajor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gateway.device.GatewaySuperiorData;

public class SuperiorIssuesSyncUtils {
	/** Number of devices types evaluated in detail for superior synchronization*/
	public static final int DEVICE_TYPE_NUM = 16;
	
	public static final String GW_SUPERIOR_DATA_RESOURCE = "gatewaySuperiorDataRes";
	
	public static GatewaySuperiorData getSuperiorData(ApplicationManager appMan) throws ResourceOperationException {
		final Resource r = appMan.getResourceAccess().getResource(GW_SUPERIOR_DATA_RESOURCE);
		if (r instanceof GatewaySuperiorData)
			return (GatewaySuperiorData) r;
		final List<GatewaySuperiorData> superiors = appMan.getResourceAccess().getResources(GatewaySuperiorData.class);
		if (superiors.isEmpty())
			return null;
		return superiors.get(0);
	}
	
	/**
	 * Also applicable on superior
	 * @param alarm
	 * @param appMan
	 * @return
	 * @throws ResourceOperationException
	 */
	public static GatewaySuperiorData getSuperiorData(AlarmGroupData alarm, ApplicationManager appMan) throws ResourceOperationException {
		if (alarm instanceof AlarmGroupDataMajor && alarm.getParent() != null) {
			final Resource parent = alarm.getParent().getParent();
			if (parent instanceof GatewaySuperiorData)
				return (GatewaySuperiorData) parent;
		}
		final Resource r = appMan.getResourceAccess().getResource(GW_SUPERIOR_DATA_RESOURCE);
		if (r instanceof GatewaySuperiorData)
			return (GatewaySuperiorData) r;
		// this is questionable
		final List<GatewaySuperiorData> superiors = appMan.getResourceAccess().getResources(GatewaySuperiorData.class);
		if (superiors.isEmpty())
			return null;
		return superiors.get(0);
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
	public static AlarmGroupDataMajor syncIssueToSuperior(AlarmGroupData issue, ApplicationManagerPlus appMan) throws ResourceOperationException {
		if (issue instanceof AlarmGroupDataMajor)  // if it is synced already, do not create another issue
			return (AlarmGroupDataMajor) issue;
		GatewaySuperiorData sup = getSuperiorData(appMan.appMan());
		return syncIssueToSuperior(issue, appMan, sup);
	}
	
	/**
	 * Not for use with gateway-related issues on superior.
	 * @param appMan
	 * @return
	 */
	public static AlarmGroupDataMajor newMajorIssue(ApplicationManager appMan, InstallAppDevice device) {
		final GatewaySuperiorData sup = getSuperiorData(appMan);
		if (sup == null)
			return null;
		final ResourceList<AlarmGroupDataMajor> major = sup.majorKnownIssues();
		if (!major.isActive())
			major.create().activate(false);
		final AlarmGroupDataMajor alarm = major.add();
		if (device != null)
			alarm.parentForOngoingIssues().setAsReference(device);
		return alarm;
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
	public static AlarmGroupDataMajor syncIssueToSuperior(AlarmGroupData issue, ApplicationManagerPlus appMan, GatewaySuperiorData superiorConfig) 
				throws ResourceOperationException {
		if (issue instanceof AlarmGroupDataMajor)  // if it is synced already, do not create another issue
			return (AlarmGroupDataMajor) issue;
		final AlarmGroupDataMajor major = superiorConfig.majorKnownIssues().<ResourceList<AlarmGroupDataMajor>> create().add();
		if (!superiorConfig.majorKnownIssues().isActive())
			superiorConfig.majorKnownIssues().activate(false);
		final ResourceTransaction transaction = ResourceUtils.prepareCopy(issue, major, appMan.getResourceAccess());
		final Resource pp = issue.getParent();
		if (pp instanceof InstallAppDevice) {
			transaction.setAsReference(major.parentForOngoingIssues(), pp);
			transaction.setStringArray(major.devicesRelated(), new String[] {((InstallAppDevice) pp).deviceId().getValue()});
			String firstDeviceName = DeviceTableBase.getName((InstallAppDevice) pp, appMan);
			transaction.setString(major.firstDeviceName(), firstDeviceName);
		}
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
	public static AlarmGroupDataMajor syncIssueToSuperiorIfRelevant(AlarmGroupData issue, ApplicationManagerPlus appMan) {
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
	public static long checkIssuesSyncStatus(ApplicationManagerPlus appMan) {
		final Resource knownDevices = appMan.getResourceAccess().getResource("hardwareInstallConfig/knownDevices");
		if (!(knownDevices instanceof ResourceList) || !knownDevices.isActive())
			return 0;
		return ((ResourceList<InstallAppDevice>) knownDevices).getAllElements().stream()
			.map(cfg -> cfg.knownFault())
			.filter(SuperiorIssuesSyncUtils::eligibleForSync)
			.map(issue -> SuperiorIssuesSyncUtils.syncIssueToSuperior(issue, appMan))
			.count();
		
	}
	
	// 
	public static String findGatewayId(final GatewaySuperiorData res, boolean isSuperior) {
		if (res.isTopLevel())
			return "Superior"; 
		return res.getParent().getName();
	}
	
	public static String findGatewayId(final AlarmGroupDataMajor alarm, boolean isSuperior) {
		// we expect those resource in a resource list GatewaySuperiorData#majorKnownIssues()
		if (alarm == null || alarm.getParent() == null)
			return null;
		final Resource parent = alarm.getParent().getParent();
		if (!(parent instanceof GatewaySuperiorData))
			return null;
		return findGatewayId((GatewaySuperiorData) parent, isSuperior);
	}
	
}
