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
				return lastHigherTime + duration*TimeProcUtil.DAY_MILLIS;
			}
		}
		return getExpectedEmptyDateSimple(batRes, now);
	}
}
