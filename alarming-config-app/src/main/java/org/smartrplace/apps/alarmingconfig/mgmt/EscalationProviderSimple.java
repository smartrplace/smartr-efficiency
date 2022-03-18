package org.smartrplace.apps.alarmingconfig.mgmt;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.core.application.AppID;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.EscalationData;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationLevel;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationSettings;
import org.smartrplace.alarming.escalation.util.EscalationProvider;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.messaging.model.MessagingApp;

/** Template for the implementation of EscalationProviders
 * 
 * @param <T> configuration data per issue processed. Non persistent data per knownIssue for the application. Persistent data
 * 		can be added as decorators to the {@link AlarmingEscalationLevel} resource of the provider or to {@link EscalationData}
 * 		for per-device data.
 */
public abstract class EscalationProviderSimple<T extends EscalationKnownIssue> implements EscalationProvider {
	Map<String, T> ongoingIssues = new HashMap<>();
	Set<String> knownIssuesNotRelevant = new HashSet<>();
	
	protected AlarmingEscalationLevel persistData;
	protected AlarmingEscalationSettings settings;
	
	/**Override if necessary. initStart is called first, then devices are checked*/
	protected void initStart() {};
	
	/** Check if a certain known issue is relevant for the provider. This implies checking the device and the 
	 * 
	 * @param iad containing a knownIssue
	 * @return if null the knownIssue will never be shown to the provider again until it is released or the app is restarted
	 * 		(not stored persistently)
	 */
	protected abstract T isEscalationRelevant(InstallAppDevice iad);

	public static class EscalationCheckResult {
		/** If this is returned then no more calls are made until the time is reached. Also stored persistently*/
		public Long blockedUntil = null;
		/** May be null. If messages to escalation levels are sent then the levels shall be
		 * listed in the result*/
		//public List<AlarmingEscalationLevel> messagesSent = null;
	}
	/** Check if escalation services are performed by the provider
	 * TODO: Add parameter data so that the provider can send out messages to the {@link AlarmingEscalationLevel}s
	 * relevant for it.
	 * TODO: Maybe also additional input data is required, check when implementing
	 * 
	 * @param iad the device that shall be checked. The provider can assume that
	 * 		{@link InstallAppDevice#knownFault()} is active on the device and provides most of the
	 * 		necessary information.
	 * @return if true the escalation is done and does not need to be called for this EscalationLevel
	 * 		and this provider anymore
	 * 		TODO: Can/Need we handle this with persistence? => Yes, shall be stored in initDone field
	 *      in {@link AlarmGroupData}(known issue), so will be deleted when knownIssue is deleted.
	 */
	protected abstract EscalationCheckResult checkEscalation(Collection<T> issues, List<AppID> appIDs, long now);
	
	@Override
	public String id() {
		return this.getClass().getSimpleName();
	}

	@Override
	public boolean initProvider(AlarmingEscalationLevel persistData, AlarmingEscalationSettings settings,
			List<InstallAppDevice> knownIssueDevices) {
		this.persistData = persistData;
		this.settings = settings;
		initStart();
		for(InstallAppDevice iad: knownIssueDevices) {
			knownIssueNotification(iad);
		}
		return true;
	}

	@Override
	public void knownIssueNotification(InstallAppDevice iad) {
		if(knownIssuesNotRelevant.contains(iad.getLocation()))
			return;
		T result = isEscalationRelevant(iad);
		if(result == null) {
			knownIssuesNotRelevant.add(iad.getLocation());
			return;
		}
		if(result.device == null)
			result.device = iad;
		if(result.knownIssue == null)
			result.knownIssue = iad.knownFault();
		ongoingIssues.put(iad.getLocation(), result);
	}
	
	@Override
	public void execute(long now, TimedJobMemoryData data, List<AppID> appIDs) {
		if(persistData.blockedUntil().getValue() > 0) {
			if((now <= persistData.blockedUntil().getValue())) {
				return;
			} else {
				persistData.blockedUntil().setValue(-1);
			}
		}
		EscalationCheckResult eres = checkEscalation(ongoingIssues.values(), appIDs, now);
		if(eres.blockedUntil != null) {
			ValueResourceHelper.setCreate(persistData.blockedUntil(), eres.blockedUntil);
		}
	}
}
