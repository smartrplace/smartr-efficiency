package org.smartrplace.apps.alarmingconfig.sync;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.resourcemanager.ResourceDemandListener;
import org.ogema.core.resourcemanager.ResourceStructureEvent;
import org.ogema.core.resourcemanager.ResourceStructureEvent.EventType;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.core.resourcemanager.ResourceStructureListener;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmGroupDataMajor;

/**
 * This service is responsible for synchronizing known device issues with a superior gateway. It does so by
 * 	 - monitoring all known device issues for synchronization eligibility, which is determined by 
 * 		a) whether they are assigned to operations, or
 *      b) a responsibility is set
 *   - replacing an issue of type AlarmGroupData eligible for snychronization by another one of type AlarmGroupDataMajor, and 
 *   	setting a reference from the existing issue to the new one
 */
public class SynchronizableIssueListener implements ResourceDemandListener<AlarmGroupData>, AutoCloseable {

	private final ResourceValueListener<IntegerResource> assignmentListener = new ResourceValueListener<IntegerResource>() {

		@Override
		public void resourceChanged(IntegerResource assigned) {
			final AlarmGroupData issue = assigned.getParent();
			if (eligibleForSync(issue))
				trigger(issue);
		}
		
	};
	
	private final ResourceStructureListener structureListener = new ResourceStructureListener() {
		
		@Override
		public void resourceStructureChanged(ResourceStructureEvent event) {
			if (event.getType() != EventType.RESOURCE_ACTIVATED)
				return;
			final AlarmGroupData issue = event.getSource().getParent();
			if (eligibleForSync(issue))
				trigger(issue);
		}
	};
	
	private final ApplicationManager appMan;
	private final Timer timer;
	
	public SynchronizableIssueListener(ApplicationManager appMan) {
		this.appMan = appMan;
		long startupDelay = 170_000; // avoid delaying the startup process by registering listeners immediately
		try {
			startupDelay = Long.getLong("org.smartrplace.apps.alarmingconfig.synchronizationlistener.delay");
		} catch (Exception ignore) {}
		this.timer = appMan.createTimer(startupDelay, this::init);
		
	}
	
	private void init(Timer timer) {
		timer.destroy();
		appMan.getResourceAccess().addResourceDemand(AlarmGroupData.class, this);
	}
	
	@Override
	public void close() {
		try {
			this.timer.destroy();
			appMan.getResourceAccess().removeResourceDemand(AlarmGroupData.class, this);
		} catch (Exception ignore) {}
	}
	

	@Override
	public void resourceAvailable(AlarmGroupData knownIssue) {
		if (knownIssue instanceof AlarmGroupDataMajor)
			return;
		if (eligibleForSync(knownIssue)) {
			trigger(knownIssue);
			return;
		}
		knownIssue.responsibility().addStructureListener(structureListener);
		knownIssue.assigned().addStructureListener(structureListener);
		knownIssue.assigned().addValueListener(assignmentListener);
	}

	@Override
	public void resourceUnavailable(AlarmGroupData knownIssue) {
		knownIssue.responsibility().removeStructureListener(structureListener);
		knownIssue.assigned().removeStructureListener(structureListener);
		knownIssue.assigned().removeValueListener(assignmentListener);
	}
	
	private AlarmGroupDataMajor trigger(AlarmGroupData issue) {
		final AlarmGroupDataMajor newIssue = SuperiorIssuesSyncUtils.syncIssueToSuperior(issue, appMan);
		if (newIssue == null) {
			appMan.getLogger().warn("Failed to synchronize device issue {} with superior", issue);
		} else {
			appMan.getLogger().info("Device issue {} is now being synchronized with superior as new issue {}", issue, newIssue);
		}
		resourceUnavailable(issue); // remove listeners once issue has been made a major one
		return newIssue;
	}
	
	private static boolean eligibleForSync(AlarmGroupData issue) {
		if (issue == null || !issue.isActive())
			return false;
		if (issue.assigned().isActive()) {
			final String role = AlarmingConfigUtil.ASSIGNEMENT_ROLES.get(issue.assigned().getValue() + "");
			if (role != null && role.toLowerCase().startsWith("op"))
				return true;
		}
		return issue.responsibility().isActive();
	}
	

}
