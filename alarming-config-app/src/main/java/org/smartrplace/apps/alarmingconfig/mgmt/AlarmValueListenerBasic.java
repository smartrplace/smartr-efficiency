package org.smartrplace.apps.alarmingconfig.mgmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.devicefinder.api.AlarmingExtension;
import org.ogema.devicefinder.api.AlarmingExtensionListener;
import org.ogema.devicefinder.api.AlarmingExtensionListener.AlarmResult;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.AlarmingExtensionBase.AlarmListenerDataBase;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.timeseries.eval.simple.mon.TimeSeriesServlet;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.alarmingconfig.mgmt.AlarmingManager.AlarmValueListenerI;
import org.smartrplace.apps.alarmingconfig.mgmt.AlarmingManager.ValueListenerData;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.messaging.MessagePriority;

public abstract class AlarmValueListenerBasic<T extends SingleValueResource> implements ResourceValueListener<T>, AlarmValueListenerI {
	final float upper;
	final float lower;
	final int retard;
	//final int resendRetard;
	final ValueListenerData vl;
	final AlarmConfiguration ac;
	final List<AlarmingExtensionListener> extensions = new ArrayList<>();

	final ApplicationManagerPlus appManPlus;
	protected final AlarmingConfigAppController controller;
	
	//protected final RoomLabelProvider tsNameProv2;
	protected final Datapoint dp;
	final String alarmID;
	protected final String baseUrl;
	
	protected abstract void releaseAlarm(AlarmConfiguration ac, float value, float upper, float lower,
			IntegerResource alarmStatus, boolean forceSendMessage);
	protected abstract void sendMessage(String title, Integer alarmValue, String message, MessagePriority prio,
			AlarmingExtension extSource)
			throws RejectedExecutionException, IllegalStateException;
	protected abstract float getHumanValue(T resource);
	
	protected final long maxValueAlarmReleaseRetard = Long.getLong("org.smartrplace.apps.alarmingconfig.mgmt.maxValueReleaseRetard", 5*60000);
	
	/**
	 * 
	 * @param ac
	 * @param devTac
	 * @param minNoValue in minutes
	 * @return in minutes
	 */
	public static float getMinNoValueDatapoint(AlarmConfiguration ac, AlarmConfiguration devTac, Float minNoValue) {
		float dpResult;
		if(devTac != null)
			dpResult = devTac.maxIntervalBetweenNewValues().getValue();
		else
			dpResult = ac.maxIntervalBetweenNewValues().getValue();
		if(minNoValue != null) {
			//long minNoValueMSec = (long) (minNoValue*60000l);
			if(minNoValue < 0)
				return minNoValue;
			if((dpResult < minNoValue) && (dpResult > 0))
				return minNoValue;
		}
		return dpResult;
	}
	
	/** This constructor is used for FloatResources in Sensors and for SmartEffTimeseries, e.g. manual time series
	 * 
	 * @param ac
	 * @param vl
	 * @param alarmID
	 * @param appManPlus
	 * @param dp
	 * @param baseUrl
	 * @param devTac
	 * @param minNoValue device minimum value for time duration after which a NoValue alarm is generated. If the datapoint-specific limit is larger
	 * 		or no NoValue alarms shall be generated the larger value or no-alarm setting is maintained. Set paramter as null if
	 * 		no device minimum value is active. PROVIDED IN MINUTES.
	 * @param controller
	 */
	public AlarmValueListenerBasic(AlarmConfiguration ac, ValueListenerData vl,
			String alarmID, ApplicationManagerPlus appManPlus, Datapoint dp, String baseUrl,
			AlarmConfiguration devTac, Float minNoValue,
			final AlarmingConfigAppController controller) {
		this.appManPlus = appManPlus;
		this.controller = controller;
		this.dp = dp;
		this.alarmID = alarmID;
		this.baseUrl = baseUrl;
		if(devTac != null)
			upper = devTac.upperLimit().getValue();
		else
			upper = ac.upperLimit().getValue();
		if(devTac != null)
			lower = devTac.lowerLimit().getValue();
		else
			lower = ac.lowerLimit().getValue();
		if(devTac != null)
			retard = (int) (devTac.maxViolationTimeWithoutAlarm().getValue()*60000);
		else
			retard = (int) (ac.maxViolationTimeWithoutAlarm().getValue()*60000);
		int resendRetardLoc = (int)(ac.alarmRepetitionTime().getValue()*60000);
		vl.init(ac, resendRetardLoc);
		this.ac = ac;
		this.vl = vl;
		vl.maxIntervalBetweenNewValues = (long)(getMinNoValueDatapoint(ac, devTac, minNoValue)*60000l);

		String[] exts = ac.alarmingExtensions().getValues();
		if(ac.sensorVal().exists() ) {
			//Should always be the case here
			if(exts != null) for(String ext: exts) {
				AlarmingExtension extDef = appManPlus.dpService().alarming().getAlarmingExtension(ext);
				if(extDef == null)
					return;
				AlarmingExtensionListener extListener = extDef.getListener(ac.sensorVal(), ac);
				extensions.add(extListener);
			}
		}
	}
	
	/** This constructor is used by the inherited class AlarmListenerBoolean used for OnOffSwitch supervision*/
	public AlarmValueListenerBasic(float upper, float lower, int retard, int resendRetard,
			AlarmConfiguration ac, ValueListenerData vl,
			String alarmID, ApplicationManagerPlus appManPlus, Datapoint dp, String baseUrl,
			final AlarmingConfigAppController controller) {
		this.appManPlus = appManPlus;
		this.controller = controller;
		this.dp = dp;
		this.alarmID = alarmID;
		this.baseUrl = baseUrl;
		this.upper = upper;
		this.lower = lower;
		this.retard = retard;
		//vl.resendRetard = resendRetard;
		vl.init(ac, resendRetard);
		this.vl = vl;
		this.ac = ac;
		vl.maxIntervalBetweenNewValues = (long) (ac.maxIntervalBetweenNewValues().getValue()*60000l);
	}
	
	protected Map<String, AlarmListenerDataBase> extData = new HashMap<>();
	protected AlarmListenerDataBase getAlarmData(String listenerId) {
		AlarmListenerDataBase result = extData.get(listenerId);
		if(result == null) {
			result = new AlarmListenerDataBase();
			extData.put(listenerId, result);
		}
		return result;
	}
	
	@Override
	public void resourceChanged(T resource) {
		IntegerResource alarmStatus = AlarmingConfigUtil.getAlarmStatus(resource);
		float val = getHumanValue(resource);
		if(Float.isNaN(val)) {
			//we just got the callback after writing NaN
			if(vl.isNoValueAlarmActive)
				return;
			appManPlus.appMan().getLogger().warn("Received Nan value in FloatResource:"+resource.getLocation());
			val = -999;
		}
		long now = appManPlus.appMan().getFrameworkTime();
		resourceChanged(val, alarmStatus, now, false);
		for(AlarmingExtensionListener ext: extensions) {
			AlarmResult result = ext.resourceChanged(resource, val, now);
			if(result != null) {
				AlarmListenerDataBase data = getAlarmData(ext.id());
				if(result.isRelease()) {
					if(data.timer != null) {
						data.timer.destroy();
						data.timer = null;
					} else {
						executeAlarm(ac, result.message(), alarmStatus, result.alarmValue(), ext.sourceExtension());						
					}
				} else if(AlarmingManager.isNewAlarmRetardPhaseAllowed(data, appManPlus.appMan())) {
					data.timer = new CountDownDelayedExecutionTimer(appManPlus.appMan(), 
							result.retard()!=null?result.retard():this.retard) {
						@Override
						public void delayedExecution() {
							executeAlarm(ac, result.message(), alarmStatus, result.alarmValue(), ext.sourceExtension());
							//data.isAlarmActive = true;
							data.nextTimeAlarmAllowed = appManPlus.appMan().getFrameworkTime() +
								vl.resendRetard();
							data.timer = null;
						}
					};
				}
			}
		}
	}
	@Override
	public void resourceChanged(float value, IntegerResource alarmStatus, long timeStamp, boolean isValueCheckForOldValue) {
		long now = timeStamp;
		if(vl.knownDeviceFault == null)
			vl.knownDeviceFault = AlarmingManager.getDeviceKnownAlarmState(vl.listener.getAc());
		if(vl.isNoValueAlarmActive) {
			releaseAlarm(ac, value, Float.NaN, Float.NaN, alarmStatus, false);
			vl.isNoValueAlarmActive = false;
		} else if(vl.knownDeviceFault.exists() && (vl.knownDeviceFault.forRelease().getValue() == 0)) { 
			int assigned = vl.knownDeviceFault.assigned().getValue();
			if((vl.maxIntervalBetweenNewValues > 0 && vl.maxIntervalBetweenNewValues < 40000) &&
					(assigned == AlarmingConfigUtil.ASSIGNMENT_DEVICE_NOT_REACHEABLE)) {
				ValueResourceHelper.setCreate(vl.knownDeviceFault.forRelease(), 1);
				releaseAlarm(ac, value, Float.NaN, Float.NaN, alarmStatus, true);
			} else if((lower > 0.5f && lower < 2.8f && upper > 1.5f && upper < 4.0f)
					&& (assigned == AlarmingConfigUtil.ASSIGNMENT_BATTERYLOW && (value > (lower+0.3f)))) {
				//we assume a battery
				ValueResourceHelper.setCreate(vl.knownDeviceFault.forRelease(), 1);
				releaseAlarm(ac, value, Float.NaN, Float.NaN, alarmStatus, true);
			}
		}
		if(TimeSeriesServlet.isViolated(value, lower, upper)) {
			if(AlarmingManager.isNewAlarmRetardPhaseAllowed(vl, appManPlus.appMan())) {
				vl.timer = new CountDownDelayedExecutionTimer(appManPlus.appMan(), 
						retard) {
					@Override
					public void delayedExecution() {
						boolean noMessage = sendValueLimitMessageOrRelease(vl, now, false);
						executeAlarm(ac, value, upper, lower, retard, alarmStatus, noMessage,
								vl.knownDeviceFault);
						vl.isAlarmActive = true;
						vl.nextTimeAlarmAllowed = appManPlus.appMan().getFrameworkTime() +
							vl.resendRetard();
						vl.timer = null;
					}
				};
			}
		} else if(vl.timer != null) {
			vl.timer.destroy();
			vl.timer = null;
		} else if(vl.isAlarmActive && vl.alarmReleaseTimer == null) {
			long retardLoc = Math.min(retard, maxValueAlarmReleaseRetard);
			vl.alarmReleaseTimer = new CountDownDelayedExecutionTimer(appManPlus.appMan(), 
					retardLoc) {
				@Override
				public void delayedExecution() {
					releaseAlarm(ac, value, upper, lower, alarmStatus, false);
					vl.isAlarmActive = false;
					vl.alarmReleaseTimer = null;
				}
			};
		}
		
		if((!isValueCheckForOldValue) && (!Float.isNaN(value)))
			vl.lastTimeOfNewData = now;
	}
	
	protected boolean sendValueLimitMessageOrRelease(ValueListenerData vl, long now, boolean isRelease) {
		boolean noMessage;
		if(vl.knownDeviceFault == null) {
			vl.knownDeviceFault = AlarmingManager.getDeviceKnownAlarmState(vl.listener.getAc());
			if(vl.knownDeviceFault == null)
				throw new IllegalStateException("No Known Default for:"+vl.listener.getAc().getPath());
		}
		if((!isRelease) && (!vl.knownDeviceFault.exists()) ) {
			ValueResourceHelper.setCreate(vl.knownDeviceFault.ongoingAlarmStartTime(), now);
			vl.knownDeviceFault.activate(true);
		}

		if(vl.knownDeviceFault.assigned().getValue() == 0)
			noMessage = false;
		else if(vl.knownDeviceFault.minimumTimeBetweenAlarms().getValue() < 0)
			//do not generate new messages here, releases are generated with AlarmValueListener
			noMessage = true;
		else if(vl.knownDeviceFault.minimumTimeBetweenAlarms().getValue() > 0) {
			if((vl.lastMessageTime > 0) && ((now - vl.lastMessageTime) < vl.knownDeviceFault.minimumTimeBetweenAlarms().getValue()*60000))
				noMessage = true;
			else
				noMessage = false;
		} else
			noMessage = false;
		if((!noMessage) && (!isRelease))
			vl.lastMessageTime = now;
		return noMessage;
	}

	@Override
	public void executeAlarm(AlarmConfiguration ac, float value, float upper, float lower, int retard,
			IntegerResource alarmStatus, boolean noMessage, AlarmGroupData knownDeviceFault) {
		String title = AlarmingManager.getTsName(ac, dp)+" (Alarming Wert)"; //dp.label(null)+" (Alarming Wert)";
		if(upper == 1.0f && lower == 1.0f) {
			title += "(Schalter)";
		}
		String message = "Current value: "+value+"\r\n"+"  Lower limit: "+lower+
				"\r\n"+"  Upper limit: "+upper+"\r\n"+"  Retard min: "+String.format("%,1f", retard/60000f);
		//if(baseUrl != null)
		//	message +="\r\nSee also: "+baseUrl+"/org/smartrplace/hardwareinstall/expert/index.html";
		int status = ac.alarmLevel().getValue();
		if(alarmStatus != null) {
			alarmStatus.setValue(status);
		}
		if(noMessage)
			return;
		MessagePriority prio = AlarmingConfigUtil.getMessagePrio(ac.alarmLevel().getValue());
		controller.escMan.knownIssueNotification(vl.knownDeviceFault, title, message);
		if(prio != null)
			sendMessage(title, status, message, prio, null);		
	}
	public void executeAlarm(AlarmConfiguration ac, String message,
			IntegerResource alarmStatus, int alarmValue,
			AlarmingExtension extSource) {
		String title = AlarmingManager.getTsName(ac, dp)+" : "+ message;
		if(upper == 1.0f && lower == 1.0f) {
			title += "(switch)";
		}
		if(alarmStatus != null) {
			alarmStatus.setValue(alarmValue);
		}

		MessagePriority prio = AlarmingConfigUtil.getMessagePrio(ac.alarmLevel().getValue());
		if(prio != null)
			sendMessage(title, alarmValue, message, prio, extSource);
	}
	@Override
	public AlarmConfiguration getAc() {
		return ac;
	}
	@Override
	public ResourceValueListener<?> getListener() {
		return this;
	}
}
