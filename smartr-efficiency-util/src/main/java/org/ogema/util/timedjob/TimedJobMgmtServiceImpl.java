package org.ogema.util.timedjob;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.resourcemanager.ResourceAlreadyExistsException;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.ogema.devicefinder.api.TimedJobMgmtService;
import org.ogema.devicefinder.api.TimedJobProvider;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.eval.timedjob.TimedEvalJobConfig;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;
import org.smartrplace.autoconfig.api.InitialConfig;
import org.smartrplace.gateway.device.MemoryTimeseriesPST;
import org.smartrplace.tissue.util.logconfig.PerformanceLog;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;

public class TimedJobMgmtServiceImpl implements TimedJobMgmtService {
	private static final long MIN_SAVE2DISK_TRIGGER_DISTANCE = 5*TimeProcUtil.MINUTE_MILLIS;
	
	private Map<String, TimedJobMemoryDataImpl> knownJobs = new HashMap<>();
	private Map<String, List<TimedJobMemoryDataImpl>> knownTypes = new HashMap<>();
	private final ApplicationManager appMan;
	private final TimedJobMgmtData jobData;
	
	public static class TimedJobMgmtData {
		public final MemoryTimeseriesPST logResource;

		public TimedJobMgmtData(MemoryTimeseriesPST logResource) {
			this.logResource = logResource;
		}
	}
	
	public TimedJobMgmtServiceImpl(ApplicationManager appMan) {
		this.appMan = appMan;
		MemoryTimeseriesPST logResource = PerformanceLog.getPSTResource(appMan);
		this.jobData = new TimedJobMgmtData(logResource);
	}

	@Override
	public TimedJobMemoryData registerTimedJobProvider(TimedJobProvider prov) {
		String id = prov.id();
		synchronized (knownJobs) {
			TimedJobMemoryDataImpl result = knownJobs.get(id);
			if(result == null) {
				result = new TimedJobMemoryDataImpl(appMan, this, jobData);
				result.prov = prov;
				result.res = getOrCreateConfiguration(id, prov.evalJobType()>0);
				createPersistentIndex(result.res);
				if(!result.res.isActive()) {
					prov.initConfigResource(result.res);
					if(!result.res.disable().exists())
						ValueResourceHelper.setCreate(result.res.disable(), false);
					result.res.activate(true);
				} else {
					final String provVersion = prov.getInitVersion();
					if((provVersion != null) && provVersion.equals("XXX")) {
						prov.initConfigResource(result.res);									
					} else {
						final String shortID = (provVersion == null || provVersion.isEmpty())?id:(id+"_"+provVersion);
						LocalGatewayInformation gw = ResourceHelper.getLocalGwInfo(appMan);
						if((!InitialConfig.checkInitAndMarkAsDone(shortID, gw.initDoneStatus(), id))) {	
							prov.initConfigResource(result.res);				
						}
					}
				}
				TimedJobMemoryDataImpl existing = knownJobs.put(id, result);
				if(existing != null) {
					throw new IllegalStateException("Two TimeJobs registered with same ID:"+id);
				}
				String type = prov.getType();
				List<TimedJobMemoryDataImpl> types = knownTypes.get(type);
				if(types == null) {
					types = new ArrayList<>();
					knownTypes.put(type, types);
				}
				types.add(result);
			}
			result.startTimerIfNotStarted();
			return result;
		}
	}

	@Override
	public TimedJobMemoryData unregisterTimedJobProvider(TimedJobProvider prov) {
		TimedJobMemoryData data = getProvider(prov.id());
		if(data != null && data.isRunning()) {
			return null;
		}
		return knownJobs.remove(prov.id());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Collection<TimedJobMemoryData> getAllProviders() {
		return (Collection)knownJobs.values();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Collection<TimedJobMemoryData> getAllProviders(String type) {
		Collection<TimedJobMemoryData> result = (Collection)knownTypes.get(type);
		if(result == null)
			return Collections.emptyList();
		return result;
	}

	@Override
	public Collection<String> getAllTypes() {
		return knownTypes.keySet();
	}

	@Override
	public TimedJobMemoryData getProvider(String id) {
		return knownJobs.get(id);
	}

	protected TimedJobConfig getOrCreateConfiguration(String id, boolean isEval) {
		LocalGatewayInformation gw = ResourceHelper.getLocalGwInfo(appMan);
		try {
		if(isEval)
			return ResourceListHelper.getOrCreateNamedElementFlex(id, gw.timedJobs(), TimedEvalJobConfig.class, false);
		else
			return ResourceListHelper.getOrCreateNamedElementFlex(id, gw.timedJobs(), TimedJobConfig.class, false);
		} catch(ResourceAlreadyExistsException e) {
			System.out.println("Could not create for "+id);
			e.printStackTrace();
			return null;			
		}
	}

	public void stop() {
		// TODO Auto-generated method stub
		
	}
	
	protected int maxPIdx = -1;
	
	/** Create persistent index if it does not exist yet*/
	protected void createPersistentIndex(TimedJobConfig config) {
		ValueResourceHelper.setCreate(config.triggerTimedJobStartsWithoutDelay(), 0);
		if(config.persistentIndex().exists())
			return;
		if(maxPIdx < 0) {
			LocalGatewayInformation gw = ResourceHelper.getLocalGwInfo(appMan);
			for(TimedJobConfig tj: gw.timedJobs().getAllElements()) {
				if(tj.persistentIndex().getValue() > maxPIdx)
					maxPIdx = tj.persistentIndex().getValue();
			}
		}
		maxPIdx++;
		ValueResourceHelper.setCreate(config.persistentIndex(), maxPIdx);
	}
	
	long lastSaveToDiskTriggerd = -1;
	public boolean triggerSavetoDisk() {
		long now = appMan.getFrameworkTime();
		if(now - lastSaveToDiskTriggerd < MIN_SAVE2DISK_TRIGGER_DISTANCE)
			return false;

		if(Boolean.getBoolean("jobdebug")) System.out.println("Triggering Save2Disk from evaluation...");
		TimedJobMemoryData saveJob = getProvider("SaveVDP2Disk");
		if(saveJob != null) {
			saveJob.executeBlockingOnceOnYourOwnRisk();
			lastSaveToDiskTriggerd = now;
			return true;
		}
		return false;
	}
	
	private class JobLoadData {
		volatile long executionTimeCounter = 0;
		volatile long lastLoadReport = -1;
		
	}
	Map<String, Long> startedIncludingOverhead = new HashMap<>();
	Map<String, Long> startedWithoutOverhead = new HashMap<>();
	
	private JobLoadData jobDataIncludingOverhead = new JobLoadData();
	private JobLoadData jobDataWithoutOverhead = new JobLoadData();
	Map<String, Long> getStartedMap(boolean includesOverhead) {
		if(includesOverhead) {
			synchronized (startedIncludingOverhead) {
				return startedIncludingOverhead;
			}
		} else {
			synchronized (startedWithoutOverhead) {
				return startedWithoutOverhead;
			}
		}
	}
	JobLoadData getJobLoadData(boolean includesOverhead) {
		if(includesOverhead) {
			return jobDataIncludingOverhead;
		} else {
			return jobDataWithoutOverhead;
		}
	}
	
	String getExistingKey(Map<String, Long> stMap) {
		if(stMap.isEmpty())
			return null;
		return stMap.entrySet().iterator().next().getKey();
	}
	
	/** Every TimedJob should call this once directly before calling the execute method (withoutOverhead)
	 *  and directly at the start of the executingBlockingOnce method
	 * @param id
	 * @param includesOverhead
	 */
	public void reportEvaluationStart(String id, boolean includesOverhead) {
		Map<String, Long> stMap = getStartedMap(includesOverhead);
		if(!stMap.isEmpty()) {
			appMan.getLogger().error("Started two timedJobs in parallel, existing:"+getExistingKey(stMap)+
					", new:"+id+", size existing:"+stMap.size()+", incOv:"+includesOverhead);
		}
		long now = appMan.getFrameworkTime();
		stMap.put(id, now);
	}
	
	public void reportEvaluationEnd(String id, boolean includesOverhead) {
		Map<String, Long> stMap = getStartedMap(includesOverhead);
		Long lastRunStart = stMap.remove(id);
		if(lastRunStart == null) {
			appMan.getLogger().error("Reported timedJobs end, but not start before"+id+", size existing:"+stMap.size()+", incOv:"+includesOverhead);
			return;
		}
		
		//Report evaluation time
		long lastRunEnd = appMan.getFrameworkTime();
		
		JobLoadData jld = getJobLoadData(includesOverhead);
		
		long reportInterval = Math.min(2*TimedJobMemoryData.LOAD_REPORT_INTERVAL, lastRunEnd-jld.lastLoadReport);
		long lastRunDuration = lastRunEnd - lastRunStart;
		jld.executionTimeCounter += lastRunDuration;
		if(lastRunEnd - jld.lastLoadReport > TimedJobMemoryData.LOAD_REPORT_INTERVAL) {
			float load = (float) (((double)jld.executionTimeCounter)/reportInterval);
			ValueResourceHelper.setCreate(includesOverhead?jobData.logResource.jobLoadIncludingOverhead():jobData.logResource.jobLoadWithoutOverhead(),
					load);
			jld.lastLoadReport = lastRunEnd;
			jld.executionTimeCounter = 0;
		}
	}
}
