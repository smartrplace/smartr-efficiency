package org.ogema.timeseries.eval.simple.mon3.std;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
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
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.api.TimeseriesUpdateListener;
import org.ogema.timeseries.eval.simple.mon3.std.TimeseriesProcAlarming.SetpReactInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.apps.alarmingconfig.model.eval.EvaluationByAlarmConfig;
import org.smartrplace.apps.alarmingconfig.model.eval.EvaluationByAlarmingOption;
import org.smartrplace.apps.alarmingconfig.model.eval.ThermPlusConfig;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatSchedules;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;

public class StandardEvalAccess {
	public static final String BASE_MEASUREMENT_GAP_POSTFIX = "/$$dpMesGap";
	public static final String BASE_FEEDBACK_GAP_POSTFIX = "/$$dpFbGap";
	public static final String BASE_SETPOINT_CHANGENUM_POSTFIX = "/$$dpSetpReqRealChange";
	public static final String SETP_REACT_POSTFIX = "/$$dpSetpReact";
	public static final String BATTERY_POSTFIX = "/$$dpBatDuration";
	public static final String BATTERY_VOLTMIN_POSTFIX = "/$$dpBatVoltMin";

	public static enum StandardDeviceEval {
		/** GAP is defined for SingleValueResources (Datapoints) and for devices. For devices this is the
		 * main datapoint.*/
		BASE_MEASUREMENT_GAP,
		/** Only defined for SingleValueResources */
		BASE_MEASUREMENT_OUT,
		/** The standard counter-based evals are generated hourly, daily, monthly. Different sets with a
		 * different base can be defined (currently 15-minute based).<br>
		 * TODO: For now this is always counter to interval-value, the input mode is set by the util framework.
		 * There could also be solutions for power to interval-value including summing up phases or meters
		 * in the future.*/
		COUNTER_TO_HOURLY,
		COUNTER_TO_DAILY,
		COUNTER_TO_MONTHLY,
		COUNTER_TO_15MIN,
		COUNTER_TO_DAILY_B15,
		COUNTER_TO_MONTHLY_B15,
		
		/** Only defined for devices. Note that currently only the first setpoint(=actory) of
		 * each device is used for the result*/
		BASE_FEEDBACK_GAP,
		/** Only defined for devices*/
		BASE_SETPOINT_CHANGENUM,
		/** Only defined for devices*/
		SETP_REACT,
		
		BATTERY_VOLTAGE_MINIMAL,
		BATTERY_REMAINING
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
	
	//TODO: Check if getAlias is really required. Usually one should directly request the
	//result datapoint
	/*public static String getAlias(InstallAppDevice iad, StandardDeviceEval type) {
		
	}
	public static String getAlias(PhysicalElement dev, StandardDeviceEval type) {
		
	}*/
	public static String getAlias(SingleValueResource sourceRes, StandardDeviceEval type,
			DatapointService dpService) {
		Datapoint dpIn = dpService.getDataPointStandard(sourceRes);
		return getAlias(dpIn, type);
	}
	public static String getAlias(Datapoint dpIn, StandardDeviceEval type) {
		switch(type) {
		case COUNTER_TO_MONTHLY:
			return dpIn.getLocation()+TimeProcUtil.PER_MONTH_SUFFIX;
		case COUNTER_TO_DAILY:
			return dpIn.getLocation()+TimeProcUtil.PER_DAY_SUFFIX;
		case COUNTER_TO_HOURLY:
			return dpIn.getLocation()+TimeProcUtil.PER_HOUR_SUFFIX;
		case COUNTER_TO_MONTHLY_B15:
			return dpIn.getLocation()+TimeProcUtil.PER_MONTH_SUFFIX;
		case COUNTER_TO_DAILY_B15:
			return dpIn.getLocation()+TimeProcUtil.PER_DAY_SUFFIX;
		case COUNTER_TO_15MIN:
			return dpIn.getLocation()+TimeProcUtil.PER_15M_SUFFIX;
		default:
			throw new IllegalStateException("Only non-device types supported without parameters here as input, found:"+type);
		}
	}
	
	public static Datapoint getDatapointBaseEval(Datapoint dpIn, StandardDeviceEval type,
			DatapointService dpService) {
		synchronized (dpIn) {
			Datapoint dpDay = null;
			Datapoint dpMonth = null;
			switch(type) {
			case COUNTER_TO_MONTHLY:
				dpMonth = dpService.getDataPointAsIs(getAlias(dpIn, StandardDeviceEval.COUNTER_TO_MONTHLY));
				if(dpMonth != null)
					return dpMonth;
			case COUNTER_TO_DAILY:
				dpDay = dpService.getDataPointAsIs(getAlias(dpIn, StandardDeviceEval.COUNTER_TO_DAILY));
				if(dpDay != null)
					return dpDay;
				Datapoint dpBase60 = dpService.getDataPointAsIs(getAlias(dpIn, StandardDeviceEval.COUNTER_TO_HOURLY));
				Datapoint dpBase15 = dpService.getDataPointAsIs(getAlias(dpIn, StandardDeviceEval.COUNTER_TO_15MIN));
				if((dpBase15 != null && dpBase60 == null) 
						|| (Boolean.getBoolean("standardeval.15minutedefaultforhour") && dpBase60 == null)) {
					if(type == StandardDeviceEval.COUNTER_TO_DAILY)
						return getDatapointBaseEval(dpIn, StandardDeviceEval.COUNTER_TO_DAILY_B15, dpService);
					else
						return getDatapointBaseEval(dpIn, StandardDeviceEval.COUNTER_TO_MONTHLY_B15, dpService);
				}
			case COUNTER_TO_HOURLY:
				Datapoint dpBase = dpService.getDataPointAsIs(getAlias(dpIn, StandardDeviceEval.COUNTER_TO_HOURLY));
				if(dpBase == null) {
					dpBase = util().processSingle(TimeProcUtil.PER_HOUR_EVAL, dpIn);
				}
				if(type != StandardDeviceEval.COUNTER_TO_HOURLY) {
					if(dpDay == null) {
						dpDay = util().processSingle(TimeProcUtil.PER_DAY_EVAL, dpBase);
					}
					if(type == StandardDeviceEval.COUNTER_TO_MONTHLY) {
						if(dpMonth == null) {
							dpMonth = util().processSingle(TimeProcUtil.PER_MONTH_EVAL, dpDay);
						}
						return dpMonth;
					}
					return dpDay;
				}
				return dpBase;
			//Only fallback is different below
			case COUNTER_TO_MONTHLY_B15:
				dpMonth = dpService.getDataPointAsIs(getAlias(dpIn, StandardDeviceEval.COUNTER_TO_MONTHLY_B15));
				if(dpMonth != null)
					return dpMonth;
			case COUNTER_TO_DAILY_B15:
				dpDay = dpService.getDataPointAsIs(getAlias(dpIn, StandardDeviceEval.COUNTER_TO_DAILY_B15));
				if(dpDay != null)
					return dpDay;
			case COUNTER_TO_15MIN:
				dpBase = dpService.getDataPointAsIs(getAlias(dpIn, StandardDeviceEval.COUNTER_TO_15MIN));
				if(dpBase == null) {
					dpBase = util().processSingle(TimeProcUtil.PER_15M_EVAL, dpIn);
				}
				if(type != StandardDeviceEval.COUNTER_TO_15MIN) {
					if(dpDay == null) {
						dpDay = util().processSingle(TimeProcUtil.PER_DAY_EVAL, dpBase);
					}
					if(type == StandardDeviceEval.COUNTER_TO_MONTHLY_B15) {
						if(dpMonth == null) {
							dpMonth = util().processSingle(TimeProcUtil.PER_MONTH_EVAL, dpDay);
						}
						return dpMonth;
					}
					return dpDay;
				}
				return dpBase;
			default:
				throw new IllegalStateException("Only non-device types supported without parameters here as input, found:"+type);
			}
		}		
		
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
		return getDeviceBaseEvalForInit(device, type, dpService, resAcc, null);
	}
	public static Datapoint getDeviceBaseEvalForInit(PhysicalElement device, StandardDeviceEval type,
			DatapointService dpService, ResourceAccess resAcc,
			DeviceHandlerProviderDP<Resource> devHand) {
		InstallAppDevice iad = dpService.getMangedDeviceResource(device);
		return getDeviceBaseEvalForInit(iad, type, dpService, resAcc, devHand);
	}
	public static Datapoint getDeviceBaseEval(InstallAppDevice iad, StandardDeviceEval type,
			DatapointService dpService, ResourceAccess resAcc) {
		return getDeviceBaseEvalForInit(iad, type, dpService, resAcc, null);
	}
	@SuppressWarnings("incomplete-switch")
	public static Datapoint getDeviceBaseEvalForInit(InstallAppDevice iad, StandardDeviceEval type,
			DatapointService dpService, ResourceAccess resAcc,
			DeviceHandlerProviderDP<Resource> devHand) {
		PhysicalElement device = iad.device().getLocationResource();
		
		if(devHand == null)
			devHand = dpService.getDeviceHandlerProvider(iad);
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
			dpIn = getDeviceBaseEvalForInit(iad, StandardDeviceEval.BASE_SETPOINT_CHANGENUM, dpService, null, devHand);
			SetpReactInput args = (SetpReactInput) (input = new SetpReactInput());
						
			args.config = (ThermPlusConfig) allConfig.get(0);
			args.setpFb = dpService.getDataPointStandard(allSetp.get(0).stateFeedback).getTimeSeries();
			break;
		case BATTERY_VOLTAGE_MINIMAL:
			String depLocation = device.getLocation()+BATTERY_VOLTMIN_POSTFIX;
			Datapoint depResult = dpService.getDataPointAsIs(depLocation);
			if(depResult != null)
				return depResult;
		case BATTERY_REMAINING:
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
		String depLocation;
		switch(type) {
		case BATTERY_REMAINING:
		case BATTERY_VOLTAGE_MINIMAL:
			Datapoint depResult = ((ProcessedReadOnlyTimeSeries3)result.getTimeSeries()).getDependentTimeseries(
					TimeseriesProcAlarming.BAT_VOLT_MIN_ID);
			depLocation = device.getLocation()+BATTERY_VOLTMIN_POSTFIX;
			depResult.addAlias(depLocation);
			if(type == StandardDeviceEval.BATTERY_VOLTAGE_MINIMAL)
				return depResult;
		}
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
			return new float[]{-1f, 0.0f};
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
		float[] res1 = getQualityValuesPerDevice(iad, appManPlus, startShort, now);
		long startLong = now - 28*TimeProcUtil.DAY_MILLIS;
		float[] res2 = getQualityValuesPerDevice(iad, appManPlus, startLong, now);
		return new float[] {res1[0], res1[1], res2[0], res2[1]}; 
	}
	
	public static float[] getSetpReactValuesPerDeviceStandard(InstallAppDevice iad,
			ApplicationManager appMan, ApplicationManagerPlus appManPlus) {
		long now = appMan.getFrameworkTime();
		long startShort = now - 4*TimeProcUtil.DAY_MILLIS;
		float[] shortRes = getSetpReactValuesPerDevice(iad, appManPlus, startShort, now);
		long startLong = now - 28*TimeProcUtil.DAY_MILLIS;
		float[] longRes = getSetpReactValuesPerDevice(iad, appManPlus, startLong, now);
		return new float[] {shortRes[0], longRes[0]};
	}
	
	public static SingleValueResource getVirtualDeviceResource(StandardDeviceEval type, PhysicalElement device) {
		return getVirtualDeviceResource(type, device, null);
	}
	public static SingleValueResource getVirtualDeviceResource(StandardDeviceEval type, PhysicalElement device,
			SingleValueResource sourceForName) {
		switch(type) {
		case BASE_MEASUREMENT_GAP:
		case BASE_MEASUREMENT_OUT:
		case BASE_FEEDBACK_GAP:
			throw new IllegalStateException("GAP and OUT shall not be established as virtual resources (not required for alarming, would grow too fast!)");
		case BASE_SETPOINT_CHANGENUM:
			return device.getSubResource("setpointChangenum", IntegerResource.class);
		case SETP_REACT:
			return device.getSubResource("setpointReactGaps", FloatResource.class);
		case BATTERY_REMAINING:
			return device.getSubResource("remainingBatteryLifeDays", FloatResource.class);
		case BATTERY_VOLTAGE_MINIMAL:
			return device.getSubResource("batteryVoltageFewVal", FloatResource.class);
		case COUNTER_TO_HOURLY:
			String name;
			if(sourceForName != null)
				name = sourceForName.getName()+"-hourly";
			else
				name = ResourceHelper.getUniqueNameForNewSubResource(device, "meter-hourly");
			return device.getSubResource(name, FloatResource.class);
		case COUNTER_TO_DAILY:
		case COUNTER_TO_DAILY_B15:
			if(sourceForName != null)
				name = sourceForName.getName()+"-daily";
			else
				name = ResourceHelper.getUniqueNameForNewSubResource(device, "meter-daily");
			return device.getSubResource(name, FloatResource.class);
		case COUNTER_TO_MONTHLY:
		case COUNTER_TO_MONTHLY_B15:
			if(sourceForName != null)
				name = sourceForName.getName()+"-monthly";
			else
				name = ResourceHelper.getUniqueNameForNewSubResource(device, "meter-monthly");
			return device.getSubResource(name, FloatResource.class);
		case COUNTER_TO_15MIN:
			if(sourceForName != null)
				name = sourceForName.getName()+"-per15min";
			else
				name = ResourceHelper.getUniqueNameForNewSubResource(device, "meter-per15min");
			return device.getSubResource(name, FloatResource.class);
		default:
			throw new IllegalStateException("Unknown StandardDeviceEval:"+type);
		}
	}
	
	public static class RemoteScheduleData {
		Integer absoluteTiming;
	}
	
	public static Datapoint addVirtualDatapoint(InstallAppDevice iad, StandardDeviceEval type,
			DatapointService dpService, ResourceAccess resAcc,
			boolean registerRemoteScheduleViaHeartbeat,
			List<Datapoint> result) {
		Datapoint dp = getDeviceBaseEval(iad, type, dpService, resAcc);
		PhysicalElement device = iad.device().getLocationResource();
		SingleValueResource destRes = getVirtualDeviceResource(type, device);
		return addVirtualDatapoint(destRes, dp, false, registerRemoteScheduleViaHeartbeat, 
				null, null, dpService, result);
	}
	public static Datapoint addVirtualDatapoint(SingleValueResource destRes, Datapoint evalDp,
			 Long minimumWriteIntervalForMaxValue,
			 DatapointService dpService,
			List<Datapoint> result) {
		return addVirtualDatapoint(destRes, evalDp, false, false, null, minimumWriteIntervalForMaxValue, dpService, result);
	}
	public static Datapoint addVirtualDatapoint(SingleValueResource destRes, Datapoint evalDp,
			DatapointService dpService,
			List<Datapoint> result) {
		return addVirtualDatapoint(destRes, evalDp, false, false, null, null, dpService, result);
	}
	public static Datapoint addVirtualDatapoint(SingleValueResource destRes, Datapoint evalDp,
			boolean registerRemoteScheduleViaHeartbeat,
			DatapointService dpService,
			List<Datapoint> result) {
		return addVirtualDatapoint(destRes, evalDp, false, registerRemoteScheduleViaHeartbeat, null, null, dpService, result);
	}
	/**
	 * 
	 * @param destRes
	 * @param evalDp
	 * @param registerGovernedSchedule
	 * @param registerRemoteScheduleViaHeartbeat
	 * @param absoluteTiming
	 * @param minimumWriteIntervalForMaxValue if not null then the maximum value during each unaligned period is
	 * 		written instead of aligned writing. This is especially useful for
	 * 		virtual datapoints with a long absoluteTiming that are used for alarming
	 * @param dpService
	 * @param result
	 * @return
	 */
	public static Datapoint addVirtualDatapoint(SingleValueResource destRes, Datapoint evalDp,
			boolean registerGovernedSchedule, boolean registerRemoteScheduleViaHeartbeat,
			Integer absoluteTiming, Long minimumWriteIntervalForMaxValue,
			DatapointService dpService,
			List<Datapoint> result) {
	
		long intervalToStayBehindNow = TimeProcUtil.MINUTE_MILLIS;
		if(absoluteTiming == null) {
			if(evalDp == null || evalDp.getTimeSeries() == null)
				return null;
			absoluteTiming = ((ProcessedReadOnlyTimeSeries3)evalDp.getTimeSeries()).absoluteTiming();
		}
		if(registerGovernedSchedule) {
			dpService.virtualScheduleService().addDefaultSchedule(evalDp, intervalToStayBehindNow);
		} else
			dpService.virtualScheduleService().add(evalDp, null, intervalToStayBehindNow);
		
		if(destRes == null) {
			if(registerRemoteScheduleViaHeartbeat) {
				ViaHeartbeatSchedules.registerDatapointForHeartbeatDp2Schedule(
						evalDp, null, absoluteTiming);
			}			
			return evalDp;
		}
		
		ReadOnlyTimeSeries accTs = evalDp.getTimeSeries();
		Datapoint resourceDp = dpService.getDataPointStandard(destRes);
		resourceDp.setTimeSeries(accTs);
		logger.trace("   Starting VirtualDatapoint for:"+destRes.getLocation()+ " DP size:"+accTs.size());
		if(registerRemoteScheduleViaHeartbeat) {
			ViaHeartbeatSchedules schedProv = ViaHeartbeatSchedules.registerDatapointForHeartbeatDp2Schedule(
					evalDp, null, absoluteTiming);
			resourceDp.setParameter(Datapoint.HEARTBEAT_STRING_PROVIDER_PARAM, schedProv);
		}
		if(Boolean.getBoolean("suppress.addSourceResourceListenerFloat"))
			return resourceDp;


		if(!(accTs instanceof ProcessedReadOnlyTimeSeries3))
			return resourceDp;
		final Integer absoluteTimingFinal = absoluteTiming;
if(Boolean.getBoolean("debugsetpreact"))
System.out.println("addVirtualDatapoint Timer"+" mWiFMv:"+minimumWriteIntervalForMaxValue+" destRes:"+destRes.getLocation());
		TimeseriesUpdateListener listener = new TimeseriesUpdateListener() {
			long lastWriteTime = -1;
			Float maxValue = null;
			
			@Override
			public void updated(List<SampledValue> newVals) {
				long nowReal = dpService.getFrameworkTime();
				long nowItvStart;
				if(absoluteTimingFinal != null)
					nowItvStart = AbsoluteTimeHelper.getIntervalStart(nowReal, absoluteTimingFinal);
				else 
					nowItvStart = nowReal;
				
				long lastWritten = destRes.getLastUpdateTime();

				if(minimumWriteIntervalForMaxValue != null) {
if(Boolean.getBoolean("debugsetpreact"))
System.out.println("SETPREACT: Updated:"+destRes.getLocation()+" mWiFMv:"+minimumWriteIntervalForMaxValue+" newVals#"+newVals.size());
					if(!newVals.isEmpty()) {
						float val = newVals.get(newVals.size()-1).getValue().getFloatValue();
						if(maxValue == null || (Math.abs(val) > Math.abs(maxValue)))
							maxValue = val;
					}
if(Boolean.getBoolean("debugsetpreact") && (maxValue != null)) System.out.println("SETPREACT: maxValue after:"+maxValue+" write:"+((nowReal - lastWriteTime) > minimumWriteIntervalForMaxValue)+
		" last:"+StringFormatHelper.getTimeDateInLocalTimeZone(lastWriteTime));
					if((nowReal - lastWriteTime) > minimumWriteIntervalForMaxValue) {
						lastWriteTime = nowReal;
						if(maxValue != null) {
							performWrite(maxValue);
							maxValue = null;
						}
					}
					return;
				}
				//If we have already written once during the current interval then we do not have
				//to write again
				if(lastWritten > nowItvStart)
					return;

				SampledValue lastSv = null;
				ReadOnlyTimeSeries accTs = evalDp.getTimeSeries();
				lastSv = accTs.getPreviousValue(nowItvStart);
				if(lastSv == null)
					return;
				performWrite(lastSv.getValue().getFloatValue());
			}
			
			protected void performWrite(float value) {
				if(Float.isNaN(value))
					return;
				if(destRes instanceof FloatResource)
					ValueResourceHelper.setCreate(((FloatResource)destRes), value);
				else if(destRes instanceof IntegerResource)
					ValueResourceHelper.setCreate(((IntegerResource)destRes), (int)value);
				else if(destRes instanceof BooleanResource)
					ValueResourceHelper.setCreate(((BooleanResource)destRes), value>0.5f);				
			}
		};
		((ProcessedReadOnlyTimeSeries3)evalDp.getTimeSeries()).listener = listener;
		if(result != null)
			result.add(resourceDp);
		return resourceDp;
	}
	
	public static Datapoint addMemoryDatapoint(InstallAppDevice iad, StandardDeviceEval type,
			DatapointService dpService, ResourceAccess resAcc,
			boolean registerRemoteScheduleViaHeartbeat,
			boolean removeVirtualDpResource,
			String dpLabel,
			List<Datapoint> result) {
		return addMemoryDatapointForInit(iad, type, dpService, resAcc, registerRemoteScheduleViaHeartbeat, removeVirtualDpResource, dpLabel, result,
				null);
	}
	public static Datapoint addMemoryDatapointForInit(InstallAppDevice iad, StandardDeviceEval type,
			DatapointService dpService, ResourceAccess resAcc,
			boolean registerRemoteScheduleViaHeartbeat,
			boolean removeVirtualDpResource,
			String dpLabel,
			List<Datapoint> result,
			DeviceHandlerProviderDP<Resource> devHand) {
		Datapoint dp = getDeviceBaseEvalForInit(iad, type, dpService, resAcc, devHand);
		if(dp == null)
			return null;
		if(removeVirtualDpResource) {
			PhysicalElement device = iad.device().getLocationResource();
			SingleValueResource destRes = getVirtualDeviceResource(type, device);
			if(destRes.exists())
				destRes.delete();
			if(dpLabel != null) {
				dp.setLabelDefault(dpLabel);
				dp.setLabel(dpLabel, null);
			}
		}
		addVirtualDatapoint(null, dp, false, registerRemoteScheduleViaHeartbeat, 
				null, null, dpService, result);
		return dp;
	}

}
