package org.smartrplace.apps.kni.quality.eval;

import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatSchedules;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatUtil;
import org.smartrplace.gateway.device.KnownIssueDataGw;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTiming;

public class QualityEvalUtil {
	private static final long KNI_EVAL_TIMER_INTERVAL = TimeProcUtil.MINUTE_MILLIS;
	private static final long QUALITY_EVAL_INTERVAL = 60*TimeProcUtil.MINUTE_MILLIS;

	public Timer kniEvalTimer;
	public KnownIssueDataGw kniData;
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
		util = new TimeseriesProcUtilKni(c.appMan, c.dpService);
		virtualRootDp = c.dpService.getDataPointStandard("vRootDp");
		registerDailyValues(c);
		
		kniData = ResourceHelper.getEvalCollection(c.appMan).getSubResource("knownIssueDataGw", KnownIssueDataGw.class);
		ViaHeartbeatUtil.updateEvalResources(kniData, c.appMan);				
		kniEvalTimer = c.appMan.createTimer(KNI_EVAL_TIMER_INTERVAL, new TimerListener() {
			
			@Override
			public void timerElapsed(Timer timer) {
				ViaHeartbeatUtil.updateEvalResources(kniData, c.appMan);				
			}
		});
		updateQualityResources(c);
		qualityEvalTimer = c.appMan.createTimer(QUALITY_EVAL_INTERVAL, new TimerListener() {
			
			@Override
			public void timerElapsed(Timer timer) {
				updateQualityResources(c);
			}
		});
		kniData.referenceForDeviceHandler().create();
		kniData.activate(true);
	}
	
	public void updateQualityResources(AlarmingConfigAppController c) {
		int[] qualityVals = AlarmingConfigUtil.getQualityValues(c.appMan, c.dpService);
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
}
