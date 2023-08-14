package org.smartrplace.apps.kni.quality.eval;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.devices.buildingtechnology.AirConditioner;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.devices.sensoractordevices.SingleSwitchBox;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.DoorWindowSensor;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon3.std.StandardEvalAccess;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatSchedules;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatUtil;
import org.smartrplace.gateway.device.GatewaySuperiorData;
import org.smartrplace.gateway.device.KnownIssueDataGw;
import org.smartrplace.tissue.util.resource.GatewaySyncUtil;

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
	private static final long KNI_EVAL_TIMER_INTERVAL = 60*TimeProcUtil.MINUTE_MILLIS;
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
				performEval(c);
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
	
	public void performEval(AlarmingConfigAppController c) {
		ViaHeartbeatUtil.updateEvalResources(kniData, c.appMan);				
		OGEMAResourceCopyHelper.copySubResourceIntoDestination(kniDataSync, kniData, c.appMan, true);
		updateActiveAlarms(kniDataSync, c.appMan.getResourceAccess());		
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
	
	//public static ResourceList<KnownIssueDataGw> getSuperiorSyncList(ApplicationManager appMan) {
	public static GatewaySuperiorData getSuperiorSyncList(ApplicationManager appMan) {
		Resource firstGeneration = appMan.getResourceAccess().getResource("gatewaySuperiorData");
		if(firstGeneration != null && (firstGeneration instanceof ResourceList))
			firstGeneration.delete();
		
		GatewaySuperiorData result = ResourceHelper.getOrCreateTopLevelResource("gatewaySuperiorDataRes", GatewaySuperiorData.class, appMan);
		
		/*@SuppressWarnings("unchecked")
		ResourceList<KnownIssueDataGw> result = ResourceHelper.getOrCreateTopLevelResource("gatewaySuperiorData", ResourceList.class, appMan);
		if(result.exists()) {
			result.create();
			result.setElementType(KnownIssueDataGw.class);
			result.activate(true);
		}*/
		if(!Boolean.getBoolean("org.smartrplace.apps.subgateway"))
			GatewaySyncUtil.registerToplevelDeviceForSyncAsClient(result, appMan);
		return result;
	}
	public static String gwIdResourceName = null;
	/*public static KnownIssueDataGw getSuperiorSyncData(ApplicationManager appMan) {
		ResourceList<KnownIssueDataGw> list = getSuperiorSyncList(appMan);
		if(gwIdResourceName == null) {
			gwIdResourceName = ResourceUtils.getValidResourceName("gw"+ViaHeartbeatUtil.getBaseGwId(GatewayUtil.getGatewayId(appMan.getResourceAccess())));
		}
		KnownIssueDataGw result = list.getSubResource(gwIdResourceName, KnownIssueDataGw.class);
		return result;
	}*/
	public static KnownIssueDataGw getSuperiorSyncData(ApplicationManager appMan) {
		GatewaySuperiorData list = getSuperiorSyncList(appMan);
		KnownIssueDataGw result = list.knownIssueStatistics();
		return result;
	}
	
	public static final int DEVICE_TYPE_NUM = 16;
	/** Version of {@link AlarmingConfigUtil#getActiveAlarms(ResourceAccess) }for data directly evaluated into {@link KnownIssueDataGw}
	 * 
	 * @param resAcc
	 * @return
	 */
	public static void updateActiveAlarms(KnownIssueDataGw kni, ResourceAccess resAcc) {
		HardwareInstallConfig hwInstall = ResourceHelper.getTopLevelResource(HardwareInstallConfig.class, resAcc);
		int[] devs = new int[DEVICE_TYPE_NUM];
		int[] datapoints = new int[DEVICE_TYPE_NUM];
		int[] dpConfiguredForAlarm = new int[DEVICE_TYPE_NUM];
		int[] devsWithActiveAlarm = new int[DEVICE_TYPE_NUM];
		int[] issues = new int[DEVICE_TYPE_NUM];
		int[] issuesNone = new int[DEVICE_TYPE_NUM];
		int[] issuesSupDevUrgent = new int[DEVICE_TYPE_NUM];
		int[] issuesSupDevStd = new int[DEVICE_TYPE_NUM];
		int[] issuesOpUrgent = new int[DEVICE_TYPE_NUM];
		int[] issuesOpStd = new int[DEVICE_TYPE_NUM];
		int[] issuesManufacturer = new int[DEVICE_TYPE_NUM];
		int[] issuesCustomer = new int[DEVICE_TYPE_NUM];
		int[] issuesDependent = new int[DEVICE_TYPE_NUM];
		for(InstallAppDevice iad: hwInstall.knownDevices().getAllElements()) {
			if(iad.isTrash().getValue())
				continue;
			PhysicalElement dev = iad.device();
			int idx;
			if(iad.devHandlerInfo().getValue().toLowerCase().contains("virtual"))
				idx = 1;
			else if(iad.devHandlerInfo().getValue().equals("org.smartrplace.homematic.devicetable.WallThermostatHandler"))
				idx = 4;
			else if(iad.devHandlerInfo().getValue().equals("org.smartrplace.app.drivermonservice.devicehandler.HomematicCCUHandler"))
				idx = 10;
			else if(iad.devHandlerInfo().getValue().equals("org.smartrplace.app.drivermonservice.devicehandler.HomematicHAPHandler"))
				idx = 11;
			else if(iad.devHandlerInfo().getValue().equals("org.smartrplace.app.drivermonservice.devicehandler.GliNetRouterHandler"))
				idx = 12;
			else if(iad.devHandlerInfo().getValue().equals("org.smartrplace.homematic.devicetable.CO2SensorHmHandler"))
				idx = 13;
			else if(iad.devHandlerInfo().getValue().equals("org.smartrplace.driverhandler.devices.LightWLANDevHandler"))
				idx = 14;
			else if(dev instanceof Thermostat)
				idx = 3;
			else if(dev instanceof AirConditioner)
				idx = 5;
			else if(dev instanceof DoorWindowSensor)
				idx = 6;
			else if(dev instanceof OnOffSwitch || dev instanceof SingleSwitchBox)
				idx = 8;
			else if(dev instanceof SensorDevice)
				idx = 7;
			else if(dev instanceof Room)
				idx = 9;
			else if(iad.devHandlerInfo().getValue().toLowerCase().contains("fault") ||
					iad.devHandlerInfo().getValue().toLowerCase().equals("org.smartrplace.driverhandler.more.MemoryTsPSTHandler") ||
					iad.devHandlerInfo().getValue().toLowerCase().contains("gateway"))
				idx = 2;
			//else {if(iad.devHandlerInfo().getValue().toLowerCase().contains("meter"))
			//	idx = 8;
			else
				idx = 0;
			//boolean isRealDevice = (idx != 1)&&(idx != 2)&&(idx != 9);
			devs[idx]++;
			int[] devNum = AlarmingConfigUtil.getActiveAlarms(iad);
			if(devNum[0] > 9)
				devsWithActiveAlarm[idx]++;
			dpConfiguredForAlarm[idx] += devNum[1];
			datapoints[idx] += iad.dpNum().getValue();
			if(iad.knownFault().exists()) {
				issues[idx]++;
				AlarmGroupData issue = iad.knownFault();
				int ass = issue.assigned().getValue();
				String resp = issue.responsibility().getValue();
				if(ass == 0)
					issuesNone[idx]++;
				else if(ass == AlarmingConfigUtil.ASSIGNMENT_DEPDENDENT ||
						ass == AlarmingConfigUtil.ASSIGNMENT_BACKLOG ||
						ass == AlarmingConfigUtil.ASSIGNMENT_SPECIALSETS)
					issuesDependent[idx]++;
				else if(ass == AlarmingConfigUtil.ASSIGNMENT_CUSTOMER)
					issuesCustomer[idx]++;
				else if(ass == AlarmingConfigUtil.ASSIGNMENT_OPERATRION_EXTERNAL)
					issuesManufacturer[idx]++;
				else if(!issue.responsibility().isActive() || resp == null || resp.isEmpty())
					issuesNone[idx]++;
				else {
					boolean urgent = issue.reminderType().getValue() == 1;
					if(resp.startsWith("onsite") || resp.startsWith("support")) {
						if(urgent)
							issuesOpUrgent[idx]++;
						else
							issuesOpStd[idx]++;
					} else {
						if(urgent)
							issuesSupDevUrgent[idx]++;
						else
							issuesSupDevStd[idx]++;						
					}
				}
			}
		}
		ValueResourceHelper.setCreate(kni.devicesByType(), devs);
		ValueResourceHelper.setCreate(kni.datapointsByType(), datapoints);
		ValueResourceHelper.setCreate(kni.datapointsByTypeConfiguredForAlarm(), dpConfiguredForAlarm);
		ValueResourceHelper.setCreate(kni.devicesByTypeInIssueState(), issues);
		ValueResourceHelper.setCreate(kni.devicesByTypeWithActiveAlarms(), devsWithActiveAlarm);
		
		ValueResourceHelper.setCreate(kni.devicesByTypeIssuesNone(), issuesNone);
		ValueResourceHelper.setCreate(kni.devicesByTypeIssuesSupDevUrgent(), issuesSupDevUrgent);
		ValueResourceHelper.setCreate(kni.devicesByTypeIssuesSupDevStd(), issuesSupDevStd);
		ValueResourceHelper.setCreate(kni.devicesByTypeIssuesOpUrgent(), issuesOpUrgent);
		ValueResourceHelper.setCreate(kni.devicesByTypeIssuesOpStd(), issuesOpStd);
		ValueResourceHelper.setCreate(kni.devicesByTypeIssuesManufacturer(), issuesManufacturer);
		ValueResourceHelper.setCreate(kni.devicesByTypeIssuesCustomer(), issuesCustomer);
	}
}
