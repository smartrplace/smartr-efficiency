package org.ogema.timeseries.eval.simple.mon3.std;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP.SetpointData;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.BatteryEvalBase.BatteryStatus;
import org.ogema.devicefinder.util.BatteryEvalBase.BatteryStatusPlus;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon3.std.TimeseriesProcAlarming.SetpReactInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.apps.alarmingconfig.model.eval.EvaluationByAlarmConfig;
import org.smartrplace.apps.alarmingconfig.model.eval.EvaluationByAlarmingOption;
import org.smartrplace.apps.alarmingconfig.model.eval.ThermPlusConfig;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.resource.ResourceHelper;

public class StandardEvalAccess {
	public static final String BASE_MEASUREMENT_GAP_POSTFIX = "/$$dpMesGap";
	public static final String BASE_FEEDBACK_GAP_POSTFIX = "/$$dpFbGap";
	public static final String BASE_SETPOINT_CHANGENUM_POSTFIX = "/$$dpSetpReqRealChange";
	public static final String SETP_REACT_POSTFIX = "/$$dpSetpReact";
	public static final String BATTERY_POSTFIX = "/$$dpBatDuration";

	public static enum StandardDeviceEval {
		/** GAP is defined for SingleValueResources (Datapoints) and for devices. For devices this is the
		 * main datapoint.*/
		BASE_MEASUREMENT_GAP,
		/** Only defined for SingleValueResources */
		BASE_MEASUREMENT_OUT,
		
		/** Only defined for devices. Note that currently only the first setpoint(=actory) of
		 * each device is used for the result*/
		BASE_FEEDBACK_GAP,
		/** Only defined for devices*/
		BASE_SETPOINT_CHANGENUM,
		/** Only defined for devices*/
		SETP_REACT,
		
		BATTERY_EVAL
	}
	
	private static TimeseriesProcAlarming util = null;
	protected static Logger logger = LoggerFactory.getLogger(StandardEvalAccess.class);
	protected static TimeseriesProcAlarming util() {
		return util(null, null);
	}
	protected static TimeseriesProcAlarming util(ApplicationManager appMan, DatapointService dpService) {
		if(util == null) {
			if(appMan == null || dpService == null)
				throw new IllegalStateException("Please init the class with appMan and dpService before using"
						+ " methods just taking DpService as argument!");
			util = new TimeseriesProcAlarming(appMan, dpService);
		}
		return util;
	}
	
	public static void init(ApplicationManagerPlus appManPlus) {
		util(appManPlus.appMan(), appManPlus.dpService());
	}
	
	/** Get standard evaluation datapoint
	 * 
	 * @param device
	 * @param type
	 * @param dpService
	 * @param resAcc only required for SETP_REACT, otherwise null can be given
	 * @return
	 */
	public static Datapoint getDeviceBaseEval(PhysicalElement device, StandardDeviceEval type,
			DatapointService dpService, ResourceAccess resAcc) {
		InstallAppDevice iad = dpService.getMangedDeviceResource(device);
		return getDeviceBaseEval(iad, type, dpService, resAcc);
	}
	public static Datapoint getDeviceBaseEval(InstallAppDevice iad, StandardDeviceEval type,
			DatapointService dpService, ResourceAccess resAcc) {
		PhysicalElement device = iad.device().getLocationResource();
		
		DeviceHandlerProviderDP<Resource> devHand = dpService.getDeviceHandlerProvider(iad);
		String location;

		//Only relevant for gap
		SingleValueResource sres = null;
		
		//Only relevant for device setpoint
		String evalName = null;
		Datapoint dpIn = null;
		Object input = null;
		
		switch(type) {
		case BASE_MEASUREMENT_GAP:
			sres = devHand.getMainSensorValue(device, iad);
			location = device.getLocation()+BASE_MEASUREMENT_GAP_POSTFIX;
			break;
		case BASE_FEEDBACK_GAP:
			List<SetpointData> allSetp = devHand.getSetpointData(device, iad);
			if(allSetp == null || allSetp.isEmpty())
				return null;
			sres = allSetp.get(0).stateFeedback;
			location = device.getLocation()+BASE_FEEDBACK_GAP_POSTFIX;
			break;
		case BASE_SETPOINT_CHANGENUM:
			allSetp = devHand.getSetpointData(device, iad);
			if(allSetp == null || allSetp.isEmpty())
				return null;
			dpIn = dpService.getDataPointStandard(allSetp.get(0).stateControl);
			location = device.getLocation()+BASE_SETPOINT_CHANGENUM_POSTFIX;
			evalName = TimeseriesProcAlarming.VALUECHANGED_EVAL;
			input = null;
			break;
		case SETP_REACT:
			HardwareInstallConfig hardwareInstallConfig = resAcc.getResource("hardwareInstallConfig");
			EvaluationByAlarmConfig evalData = hardwareInstallConfig.getSubResource("evalData", EvaluationByAlarmConfig.class);
			List<EvaluationByAlarmingOption> allConfig = evalData.configOptionsToTest().getAllElements();
			if(allConfig.isEmpty())
				return null;

			if(devHand == null)
				//This should occur only during startup, could occur if handler is lost or changed ID
				return null;
			allSetp = devHand.getSetpointData(device, iad);
			if(allSetp == null || allSetp.isEmpty())
				return null;
			
			location = device.getLocation()+SETP_REACT_POSTFIX;
			evalName = TimeseriesProcAlarming.SETPREACT_EVAL;
			dpIn = getDeviceBaseEval(iad, StandardDeviceEval.BASE_SETPOINT_CHANGENUM, dpService, null);
			SetpReactInput args = (SetpReactInput) (input = new SetpReactInput());
						
			args.config = (ThermPlusConfig) allConfig.get(0);
			args.setpFb = dpService.getDataPointStandard(allSetp.get(0).stateFeedback).getTimeSeries();
			break;
		case BATTERY_EVAL:
			long now = dpService.getFrameworkTime();
			BatteryStatusPlus data = BatteryEvalBase3.getBatteryStatusPlus(iad, false, now);
			if(data.status == BatteryStatus.NO_BATTERY || data.batRes == null)
				return null;
			dpIn = dpService.getDataPointStandard(data.batRes);
			evalName = TimeseriesProcAlarming.BATTERY_EVAL;
			location = device.getLocation()+BATTERY_POSTFIX;
			input = null;
			break;
		case BASE_MEASUREMENT_OUT:
			throw new IllegalStateException("OutValue not supported as device standard type:"+type);
		default:
			throw new IllegalStateException("Unknown StandardDeviceEval:"+type);
		}
		Datapoint result = dpService.getDataPointAsIs(location);
		if(result != null)
			return result;
		
		if(type == StandardDeviceEval.BASE_MEASUREMENT_GAP ||
				type == StandardDeviceEval.BASE_FEEDBACK_GAP) {
			AlarmConfiguration ac = AlarmingUtiH.getAlarmConfig(sres, iad.alarms());
			result = getGapAnalysis(ac, type, dpService);
			result.addAlias(location);
			return result;
		}
		
		//Device-based evaluations
		synchronized (dpIn) {
			result = util().processSingle(evalName, dpIn , input);			
		}
		result.addAlias(location);
		return result;
	}
	
	/** Apply to all suitable datapoints in the device*/
	public static List<Datapoint> getDeviceBaseEvalMulti(PhysicalElement device, StandardDeviceEval type,
			DatapointService dpService) {
		InstallAppDevice iad = dpService.getMangedDeviceResource(device);
		return getDeviceBaseEvalMulti(iad, type, dpService);
		
	}
	public static List<Datapoint> getDeviceBaseEvalMulti(InstallAppDevice iad, StandardDeviceEval type,
			DatapointService dpService) {
		switch(type) {
		case BASE_MEASUREMENT_GAP:
		case BASE_MEASUREMENT_OUT:
			return getDeviceBaseEvalMulti(iad, type, dpService, null);
		default:
			throw new IllegalStateException("Only gap, out supported as GapAnalysis-MULTI Standard type:"+type);
		}
	}
	public static List<Datapoint> getDeviceBaseEvalMulti(InstallAppDevice iad, StandardDeviceEval type,
			DatapointService dpService, ResourceAccess resAcc) {
		
		List<AlarmConfiguration> inputRes = getDeviceDatapointsForStandardEval(iad, dpService);
		
		List<Datapoint> result = new ArrayList<>();
		for(AlarmConfiguration ac: inputRes) {
			switch(type) {
			case BASE_MEASUREMENT_GAP:
			case BASE_MEASUREMENT_OUT:
				Datapoint dp = getGapAnalysis(ac, type, dpService);
				result.add(dp);
				break;
			default:
				dp = getDeviceBaseEval(iad, type, dpService, resAcc);
				return Arrays.asList(new Datapoint[] {dp});
				//throw new IllegalStateException("Only gap, out supported as GapAnalysis-MULTI Standard type:"+type);
			}
		}
		return result;
	}
	
	public static List<AlarmConfiguration> getDeviceDatapointsForStandardEval(InstallAppDevice iad,	DatapointService dpService) {
		List<AlarmConfiguration> inputRes = new ArrayList<>();
		for(AlarmConfiguration ac: iad.alarms().getAllElements()) {
			if(!ac.sendAlarm().getValue())
				continue;
			SingleValueResource sens = ac.sensorVal().getLocationResource();
			if(!sens.exists())
				continue;
			float maxGapSize = ac.maxIntervalBetweenNewValues().getValue();
			if(maxGapSize < 0)
				continue;
			Datapoint dp = dpService.getDataPointAsIs(sens);
			if(dp == null)
				continue; //should not occur
			ReadOnlyTimeSeries ts = dp.getTimeSeries();
			if(ts == null) {
				logger.trace("No timeseries for datapoint configured for alarming:"+sens.getLocation());
				continue;
			}
			inputRes.add(ac);
		}
		return inputRes;
	}
	
	public static Datapoint getGapAnalysis(AlarmConfiguration ac, StandardDeviceEval type, DatapointService dpService) {
		Datapoint dpIn = dpService.getDataPointStandard(ac.sensorVal());
		synchronized (dpIn) {
			switch(type) {
			case BASE_MEASUREMENT_GAP:
				Datapoint dpGap = dpService.getDataPointAsIs(ac.sensorVal().getLocation()+TimeProcUtil.ALARM_GAP_SUFFIX);
				if(dpGap == null) {
					dpGap = util().processSingle(TimeseriesProcAlarming.GAP_EVAL, dpIn, ac.maxIntervalBetweenNewValues().getValue());
				}
				return dpGap;
			case BASE_MEASUREMENT_OUT:
				Datapoint dpOut = dpService.getDataPointAsIs(ac.sensorVal().getLocation()+TimeProcUtil.ALARM_OUTVALUE_SUFFIX);
				if(dpOut == null) {
					dpOut = util().processSingle(TimeseriesProcAlarming.OUTVALUE_EVAL, dpIn, ac);
				}
				return dpOut;
			default:
				throw new IllegalStateException("Only gap, out supported as GapAnalysis Standard type:"+type);
			}
		}		
	}
	
	/** 
	 * 
	 * @param dev
	 * @param dpService
	 * @param startTime
	 * @param endTime
	 * @return [0]: average quality, [1]: share of data rows meeting standard criteria
	 */
	public static float[] getQualityValuesPerDevice(InstallAppDevice dev, ApplicationManagerPlus appManPlus,
			long startTime, long endTime) {
		return getQualityValuesPerDeviceFlex(dev, appManPlus, startTime, endTime, StandardDeviceEval.BASE_MEASUREMENT_GAP);
	}
	public static float[] getSetpReactValuesPerDevice(InstallAppDevice dev, ApplicationManagerPlus appManPlus,
			long startTime, long endTime) {
		return getQualityValuesPerDeviceFlex(dev, appManPlus, startTime, endTime, StandardDeviceEval.SETP_REACT);
	}
	public static float[] getQualityValuesPerDeviceFlex(InstallAppDevice dev, ApplicationManagerPlus appManPlus,
			long startTime, long endTime, StandardDeviceEval type) {
		List<Datapoint> allGaps = getDeviceBaseEvalMulti(dev, type, appManPlus.dpService(), appManPlus.getResourceAccess());
		double sumTot = 0;
		int count = 0;
		int countOK = 0;

		double durationMinutes = ((double)(endTime - startTime))/TimeProcUtil.MINUTE_MILLIS;
		double QUALITY_MAX_MINUTES = durationMinutes*(1-AlarmingConfigUtil.QUALITY_TIME_SHARE_LIMIT);
		
		if(allGaps.size() == 1 && allGaps.get(0) == null)
			return new float[]{Float.NaN, Float.NaN};
		
		for(Datapoint dp: allGaps) {
			if(dp == null)
				continue;
			ReadOnlyTimeSeries ts = dp.getTimeSeries();
			try {
				List<SampledValue> gaps = ts.getValues(startTime, endTime);
				double sum = AlarmingConfigUtil.getValueSum(gaps);
				if(sum > durationMinutes)
					sum = durationMinutes;
				sumTot += sum;
				count++;
				if(sum <= QUALITY_MAX_MINUTES) {
					countOK++;
				}
			} catch(OutOfMemoryError e) {
				logger.error("OutOfMemory for "+dp.getLocation());
				e.printStackTrace();
			}
		}
		if(count == 0)
			return new float[]{0.0f, 0.0f};
		double sumTotAv = sumTot / count;
		double rel = 1.0 - sumTotAv / durationMinutes;
		float share = ((float)countOK)/count;
		return new float[]{(float) rel, share};		
	}
	
	public static int[] getQualityValues(ApplicationManager appMan, DatapointService dpService,
			long startTime, long endTime, double QUALITY_MAX_MINUTES) {
		ResourceAccess resAcc = appMan.getResourceAccess();
		HardwareInstallConfig hwInstall = ResourceHelper.getTopLevelResource(HardwareInstallConfig.class, resAcc);
		int[] result = new int[] {0,0};
		int countShortOk = 0;
		int countShortOkGold = 0;
		int countEval = 0;
		int countEvalGold =0;
		for(InstallAppDevice dev: hwInstall.knownDevices().getAllElements()) {
			if(dev.isTrash().getValue())
				continue;
			boolean isAssigned = (dev.knownFault().assigned().isActive() && (dev.knownFault().assigned().getValue() > 0));
			List<Datapoint> allGaps = getDeviceBaseEvalMulti(dev, StandardDeviceEval.BASE_MEASUREMENT_GAP, dpService);
			for(Datapoint dp: allGaps) {
				ReadOnlyTimeSeries ts = dp.getTimeSeries();
				if(ts == null)
					throw new IllegalStateException("No gap timeseries in "+dp.getLocation());
				try {
					List<SampledValue> gaps = ts.getValues(startTime, endTime);
					double sum = AlarmingConfigUtil.getValueSum(gaps);
					if(sum <= QUALITY_MAX_MINUTES) {
						countShortOkGold++;
						if(!isAssigned)
							countShortOk++;
					} else if(Boolean.getBoolean("qualitydebug") && (!isAssigned))
						System.out.println("Gaps found for "+QUALITY_MAX_MINUTES+" for "+dp.getLocation());
					countEvalGold++;
					if(!isAssigned)
						countEval++;
				} catch(OutOfMemoryError e) {
					logger.error("OutOfMemory for "+dp.getLocation());
					e.printStackTrace();
				}
			}
		}
		if(countEval == 0)
			result[0] = 100;
		else
			result[0] = (int) (((float)countShortOk) / countEval * 100);
		if(countEvalGold == 0)
			result[1] = 100;
		else
			result[1] = (int) (((float)countShortOkGold) / countEvalGold * 100);
		return result;	
	}
	
	/** 0: qualityShort, [1] qualityLong, [2]: qualityShort V2, [3]: qualityLong V2*/
	public static int[] getQualityValuesForStandardDurations(ApplicationManager appMan, DatapointService dpService) {
		long now = appMan.getFrameworkTime();
		long startShort = now - 4*TimeProcUtil.DAY_MILLIS;
		long startLong = now - 28*TimeProcUtil.DAY_MILLIS;
		int[] resShort = getQualityValues(appMan, dpService, startShort, now, AlarmingConfigUtil.QUALITY_SHORT_MAX_MINUTES);
		int[] resLong = getQualityValues(appMan, dpService, startLong, now, AlarmingConfigUtil.QUALITY_LONG_MAX_MINUTES);
		return new int[] {resShort[0], resShort[1], resLong[0], resLong[1]};
	}
	
	public static float[] getQualityValuesPerDeviceStandard(InstallAppDevice iad,
			ApplicationManager appMan, ApplicationManagerPlus appManPlus) {
		long now = appMan.getFrameworkTime();
		long startShort = now - 4*TimeProcUtil.DAY_MILLIS;
		return getQualityValuesPerDevice(iad, appManPlus, startShort, now);
	}
	
	public static float[] getSetpReactValuesPerDeviceStandard(InstallAppDevice iad,
			ApplicationManager appMan, ApplicationManagerPlus appManPlus) {
		long now = appMan.getFrameworkTime();
		long startShort = now - 4*TimeProcUtil.DAY_MILLIS;
		return getSetpReactValuesPerDevice(iad, appManPlus, startShort, now);
	}
}
