package org.ogema.timeseries.eval.simple.mon3.std;

import org.apache.commons.lang3.StringUtils;
import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.AppID;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.BatteryEvalBase;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon3.std.StandardEvalAccess.StandardDeviceEval;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.message.MessageImpl;

import de.iwes.widgets.api.messaging.MessagePriority;

public class BatteryEvalBase3 extends BatteryEvalBase {
	public static String getRightAlignedString(String in, int len) {
		if(in.length() >= len) return in.substring(0, len);
		return StringUtils.repeat(' ', len-in.length())+in;
	}
	public static String getLeftAlignedString(String in, int len) {
		if(in.length() >= len) return in.substring(0, len);
		return in+StringUtils.repeat(' ', len-in.length());
	}
	
	protected static void reallySendMessage(String title, String message, MessagePriority prio,
			ApplicationManagerPlus appManPlus) {
		AppID appId = appManPlus.appMan().getAppID();
		appManPlus.guiService().getMessagingService().sendMessage(appId,
				new MessageImpl(title, message, prio));		
	}
	
	public static BatteryStatusResult getFullBatteryStatus(InstallAppDevice iad, long now,
			DatapointService dpService) {
		//if(!(iad.device() instanceof Thermostat))
		//	return null;
		//Thermostat thermostat = (Thermostat) iad.device();
		BatteryStatusResult result = new BatteryStatusResult();
		BatteryStatusPlus plus = getBatteryStatusPlus(iad, true, now);
		result.status = plus.status;
		result.iad = iad;
		if(result.status == BatteryStatus.NO_BATTERY)
			return result;
		result.currentVoltage = plus.voltage;
		result.expectedEmptyDate = getExpectedEmptyDate(iad, plus.batRes, now, dpService);
		return result ;
	}
	
	public static BatteryStatus getBatteryStatus(float val, boolean changeInfoRelevant) {
		if(Float.isNaN(val))
			return BatteryStatus.UNKNOWN;
		if(val <= DEFAULT_BATTERY_URGENT_VOLTAGE)
			return BatteryStatus.OK;
		else if(val <= DEFAULT_BATTERY_WARN_VOLTAGE)
			return BatteryStatus.WARNING;
		else if(changeInfoRelevant && (val <= DEFAULT_BATTERY_CHANGE_VOLTAGE))
			return BatteryStatus.URGENT;
		return BatteryStatus.OK;
	}
	
	public static BatteryStatus getBatteryStatus(VoltageResource batRes, boolean changeInfoRelevant, Long now) {
		return getBatteryStatusPlus(batRes, changeInfoRelevant, now).status;
	}
	public static BatteryStatusPlus getBatteryStatusPlus(VoltageResource batRes, boolean changeInfoRelevant, Long now) {
		BatteryStatusPlus result = new BatteryStatusPlus();
		result.batRes = batRes;
		if(batRes == null) {
			result.status = BatteryStatus.NO_BATTERY;
			return result;
		}
		result.voltage = batRes.getValue();
		Long lastTs = null;
		if(Float.isNaN(result.voltage)) {
			RecordedData ts = batRes.getHistoricalData();
			if(ts != null) {
				SampledValue sv = ts.getPreviousValue(now!=null?now:Long.MAX_VALUE);
				if(sv != null) {
					result.voltage = sv.getValue().getFloatValue();
					lastTs = sv.getTimestamp();
				}
			}
		} else
			lastTs = batRes.getLastUpdateTime();
		result.status = getBatteryStatus(result.voltage, changeInfoRelevant);
		if(result.status != BatteryStatus.URGENT)
			return result;
		if(lastTs != null && now != null && (now - lastTs) > TIME_TO_ASSUME_EMPTY)
			result.status = BatteryStatus.EMPTY;
		return result;
	}
	
	/**
	 * 
	 * @param iad
	 * @param changeInfoRelevant
	 * @param now may be null, only required to find last value before now and to detect EMPTY by duration without value
	 * @return
	 */
	public static BatteryStatus getBatteryStatus(InstallAppDevice iad, boolean changeInfoRelevant, Long now) {
		return getBatteryStatusPlus(iad, changeInfoRelevant, now).status;
	}
	public static BatteryStatusPlus getBatteryStatusPlus(InstallAppDevice iad, boolean changeInfoRelevant, Long now) {
		VoltageResource sres = DeviceHandlerBase.getBatteryVoltage(iad.device().getLocationResource());
		if(iad.knownFault().assigned().exists()) {
			int kni = iad.knownFault().assigned().getValue();
			if(kni == 2100) {
				BatteryStatusPlus result = new BatteryStatusPlus();
				result.status = BatteryStatus.EMPTY;
				result.batRes = sres;
				return result;
			}
		}
		return getBatteryStatusPlus(sres, changeInfoRelevant, now);
	}
	
	//TODO: We need improved implementation that is efficient and finds last step down
	public static Long getExpectedEmptyDate(InstallAppDevice iad, VoltageResource batRes, long now,
			DatapointService dpService) {
		RecordedData ts = batRes.getHistoricalData();
		if(ts == null)
			return null;
		SampledValue svFirst = ts.getNextValue(0);
		if(svFirst == null)
			return null;
		
		Datapoint batEval = StandardEvalAccess.getDeviceBaseEval(iad, StandardDeviceEval.BATTERY_REMAINING, dpService, null);
		if(batEval != null) {
			SampledValue sv = batEval.getTimeSeries().getPreviousValue(now);
			if(sv != null) {
				long lastHigherTime = sv.getTimestamp();
				long duration = (long) (((double)sv.getValue().getFloatValue())*TimeProcUtil.DAY_MILLIS);
				return lastHigherTime + duration;
			}
		}
		return getExpectedEmptyDateSimple(batRes, now);
		
		/*float curVal = ts.getPreviousValue(now).getValue().getFloatValue();
		long testTs = now - 7*TimeProcUtil.DAY_MILLIS;
		long lastHigherTime = testTs;
		long lastHigherTime;
		while(true) {
			SampledValue sv = ts.getPreviousValue(testTs-1);
			if(sv == null) {
				lastHigherTime = testTs - 7*TimeProcUtil.DAY_MILLIS;
				break;
			}
			if(sv.getValue().getFloatValue() > curVal) {
				lastHigherTime = sv.getTimestamp();
				break;		
			}
			testTs = sv.getTimestamp();
		}*/
		//if(Float.isNaN(curVal))
		//	return null;
		//long duration = getRemainingLifeTimeEstimation(curVal);
	}
}
