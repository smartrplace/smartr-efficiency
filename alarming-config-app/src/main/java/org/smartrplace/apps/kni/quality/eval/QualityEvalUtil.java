package org.smartrplace.apps.kni.quality.eval;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.ResourceList;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon3.std.StandardEvalAccess;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatSchedules;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatUtil;
import org.smartrplace.gateway.device.KnownIssueDataGw;
import org.smartrplace.tissue.util.resource.GatewaySyncUtil;
import org.smartrplace.tissue.util.resource.GatewayUtil;

import de.iwes.util.resource.OGEMAResourceCopyHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTiming;

/** This class organizes a timer-based Evaluation of data quality written into resources and provision of a Memory timeseries
 * for transmission and plotting on superior. Both calculations are done independently.
 * TODO: Both should be replaced by {@link StandardEvalAccess#getQualityValues(org.ogema.core.application.ApplicationManager, org.ogema.devicefinder.api.DatapointService, long, long, double)}
 * => just replace the deprecated getQualityValues methods by {@link StandardEvalAccess}
 */
public class QualityEvalUtil {
	private static final long KNI_EVAL_TIMER_INTERVAL = TimeProcUtil.MINUTE_MILLIS;
	private static final long QUALITY_EVAL_INTERVAL = 60*TimeProcUtil.MINUTE_MILLIS;

	public Timer kniEvalTimer;
	public KnownIssueDataGw kniData;
	public KnownIssueDataGw kniDataSync;
	public Timer qualityEvalTimer;
	final public TimeseriesProcUtilKni util;
	final public Datapoint virtualRootDp;
	
	public void registerDailyValues(AlarmingConfigAppController c) {
		DatapointGroup dpgQuality = c.dpService.getGroup("DataQuality");
		dpgQuality.setType("DATA_QUALITY");

		//We do not really need an input here, but for labelling etc. the virtualRootDp is used
		Datapoint quality = util.processSingle(TimeseriesProcUtilKni.QUALITY_DAILY, virtualRootDp);
		ViaHeartbeatSchedules.registerDatapointForHeartbeatDp2ScheduleWithAlias(quality, AbsoluteTiming.DAY,
				Datapoint.ALIAS_QUALITY_DAILY, Datapoint.ALIAS_QUALITY_DAILY, dpgQuality);
		
		Datapoint qualityGold = ((ProcessedReadOnlyTimeSeries3)quality.getTimeSeries()).getDependentTimeseries("goldTs");
		ViaHeartbeatSchedules.registerDatapointForHeartbeatDp2ScheduleWithAlias(qualityGold, AbsoluteTiming.DAY,
				Datapoint.ALIAS_QUALITY_DAILY_GOLD, Datapoint.ALIAS_QUALITY_DAILY_GOLD, dpgQuality);
	}

	public QualityEvalUtil(AlarmingConfigAppController c) {
		StandardEvalAccess.init(c.appManPlus);
		util = new TimeseriesProcUtilKni(c.appMan, c.dpService);
		virtualRootDp = c.dpService.getDataPointStandard("vRootDp");
		registerDailyValues(c);
		
		kniData = ResourceHelper.getEvalCollection(c.appMan).getSubResource("knownIssueDataGw", KnownIssueDataGw.class);
		kniDataSync = getSuperiorSyncData(c.appMan);
		ViaHeartbeatUtil.updateEvalResources(kniData, c.appMan);
		OGEMAResourceCopyHelper.copySubResourceIntoDestination(kniDataSync, kniData, c.appMan, true);
		kniEvalTimer = c.appMan.createTimer(KNI_EVAL_TIMER_INTERVAL, new TimerListener() {
			
			@Override
			public void timerElapsed(Timer timer) {
				ViaHeartbeatUtil.updateEvalResources(kniData, c.appMan);				
				OGEMAResourceCopyHelper.copySubResourceIntoDestination(kniDataSync, kniData, c.appMan, true);
			}
		});
		updateQualityResources(c);
		qualityEvalTimer = c.appMan.createTimer(QUALITY_EVAL_INTERVAL, new TimerListener() {
			
			@Override
			public void timerElapsed(Timer timer) {
				updateQualityResources(c);
				OGEMAResourceCopyHelper.copySubResourceIntoDestination(kniDataSync, kniData, c.appMan, true);
			}
		});
		kniData.referenceForDeviceHandler().create();
		kniData.activate(true);
	}
	
	public void updateQualityResources(AlarmingConfigAppController c) {
		//int[] qualityVals = AlarmingConfigUtil.getQualityValues(c.appMan, c.dpService);
		int[] qualityVals = StandardEvalAccess.getQualityValuesForStandardDurations(c.appMan, c.dpService);
		ValueResourceHelper.setCreate(kniData.qualityShort(), qualityVals[0]);		
		ValueResourceHelper.setCreate(kniData.qualityLong(), qualityVals[1]);		
		ValueResourceHelper.setCreate(kniData.qualityShortGold(), qualityVals[2]);		
		ValueResourceHelper.setCreate(kniData.qualityLongGold(), qualityVals[3]);		
	}
	
	public void close() {
		if(kniEvalTimer != null) {
			kniEvalTimer.destroy();
			kniEvalTimer = null;
		}
		if(qualityEvalTimer != null) {
			qualityEvalTimer.destroy();
			qualityEvalTimer = null;
		}
	}
	
	public static ResourceList<KnownIssueDataGw> getSuperiorSyncList(ApplicationManager appMan) {
		@SuppressWarnings("unchecked")
		ResourceList<KnownIssueDataGw> result = ResourceHelper.getOrCreateTopLevelResource("gatewaySuperiorData", ResourceList.class, appMan);
		if(result.exists()) {
			result.create();
			result.setElementType(KnownIssueDataGw.class);
			result.activate(true);
		}
		if(!Boolean.getBoolean("org.smartrplace.apps.subgateway"))
			GatewaySyncUtil.registerToplevelDeviceForSyncAsClient(result, appMan);
		return result;
	}
	public static String gwIdResourceName = null;
	public static KnownIssueDataGw getSuperiorSyncData(ApplicationManager appMan) {
		ResourceList<KnownIssueDataGw> list = getSuperiorSyncList(appMan);
		if(gwIdResourceName == null) {
			gwIdResourceName = ResourceUtils.getValidResourceName("gw"+ViaHeartbeatUtil.getBaseGwId(GatewayUtil.getGatewayId(appMan.getResourceAccess())));
		}
		KnownIssueDataGw result = list.getSubResource(gwIdResourceName, KnownIssueDataGw.class);
		return result;
	}
}
