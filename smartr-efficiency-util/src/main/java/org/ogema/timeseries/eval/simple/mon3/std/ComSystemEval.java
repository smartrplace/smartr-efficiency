package org.ogema.timeseries.eval.simple.mon3.std;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.api.AlarmingService;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.ogema.devicefinder.api.TimedJobProvider;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.tissue.util.logconfig.PerformanceLog;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class ComSystemEval {
	public static class ComSystemEvalData {
		public long lastTimeValueAvailable = -1;
		public SingleValueResource mainSensor;
	}
	public static final long MAX_DEVICE_OFFLINE = 3*TimeProcUtil.HOUR_MILLIS;
	public static final int MAX_FAULTS_WITHOUT_RESTART = 3;
	private static Map<String, ComSystemEvalData> evalHmData = new HashMap<>();
	
	protected static volatile TimedJobMemoryData hmEvalJobData = null;
	public static boolean evalHmQualityStart(ApplicationManagerPlus appMan) {
		DatapointService dpService = appMan.dpService();
		if(hmEvalJobData != null)
			return false;
		TimedJobProvider prov = new TimedJobProvider() {
			
			@Override
			public String label(OgemaLocale locale) {
				return "HmQuality Supervision";
			}
			
			@Override
			public String id() {
				return "HmQualitySuperVEvl";
			}
			
			@Override
			public boolean initConfigResource(TimedJobConfig config) {
				ValueResourceHelper.setCreate(config.interval(), 60);
				ValueResourceHelper.setCreate(config.performOperationOnStartUpWithDelay(), 5);
				return true;
			}
			
			@Override
			public String getInitVersion() {
				return "A";
			}
			
			@Override
			public void execute(long now, TimedJobMemoryData data) {
				int newFaultNum = evalHmQualityStep(appMan);
				if(newFaultNum > MAX_FAULTS_WITHOUT_RESTART) {
					//Restart Homematic
					TimedJobMemoryData restartJob = dpService.timedJobService().getProvider("BundleRestartButtonHomematicDriver");
					if(restartJob != null) {
						restartJob.executeBlockingOnceOnYourOwnRisk();
					}
				}
			}
			
			@Override
			public int evalJobType() {
				return 0;
			}
		};
		hmEvalJobData = dpService.timedJobService().registerTimedJobProvider(prov );
		return true;
	}
	
	public static int evalHmQualityStep(ApplicationManagerPlus appMan) {
		DatapointService dpService = appMan.dpService();
		FloatResource logResource = PerformanceLog.getPSTResource(appMan.appMan()).hmDevicesLost();
		Collection<InstallAppDevice> all0 = dpService.managedDeviceResoures(null);
		long now = appMan.getFrameworkTime();
		int countFaultDev = 0;
		for(InstallAppDevice iad: all0) {
			if(iad.isTrash().getValue())
				continue;
			if(!DeviceTableBase.isHomematic(iad.device().getLocation()))
				continue;
			ComSystemEvalData data = evalHmData.get(iad.getLocation());
			if(data == null) {
				data = new ComSystemEvalData();
				evalHmData.put(iad.getLocation(), data);
			}
			if(data.mainSensor == null) {
				DeviceHandlerProviderDP<Resource> devHand = dpService.getDeviceHandlerProvider(iad);
				data.mainSensor = devHand.getMainSensorValue(iad);
			}
			if(data.mainSensor == null)
				continue;
			IntegerResource alarmStatus = data.mainSensor.getSubResource(AlarmingService.ALARMSTATUS_RES_NAME,
					IntegerResource.class);
			if(!alarmStatus.isActive())
				continue;
			if(alarmStatus.getValue() < 1000) {
				data.lastTimeValueAvailable = now;
			} else if((now - data.lastTimeValueAvailable) < MAX_DEVICE_OFFLINE) {
				countFaultDev++;
			}
		}
		logResource.setValue(countFaultDev);
		return countFaultDev;
	}
}
