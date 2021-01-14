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
import org.ogema.timeseries.eval.simple.mon.TimeSeriesServlet;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.apps.alarmingconfig.mgmt.AlarmingManager.AlarmValueListenerI;
import org.smartrplace.apps.alarmingconfig.mgmt.AlarmingManager.ValueListenerData;

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
	//protected final RoomLabelProvider tsNameProv2;
	protected final Datapoint dp;
	final String alarmID;
	protected final String baseUrl;
	
	protected abstract void releaseAlarm(AlarmConfiguration ac, float value, float upper, float lower,
			IntegerResource alarmStatus);
	protected abstract void sendMessage(String title, Integer alarmValue, String message, MessagePriority prio,
			AlarmingExtension extSource)
			throws RejectedExecutionException, IllegalStateException;
	protected abstract float getHumanValue(T resource);
	
	protected final long maxValueAlarmReleaseRetard = Long.getLong("org.smartrplace.apps.alarmingconfig.mgmt.maxValueReleaseRetard", 5*60000);
	
	/** This constructor is used for FloatResources in Sensors and for SmartEffTimeseries, e.g. manual time seroes*/
	public AlarmValueListenerBasic(AlarmConfiguration ac, ValueListenerData vl,
			String alarmID, ApplicationManagerPlus appManPlus, Datapoint dp, String baseUrl) {
		this.appManPlus = appManPlus;
		this.dp = dp;
		this.alarmID = alarmID;
		this.baseUrl = baseUrl;
		upper = ac.upperLimit().getValue();
		lower = ac.lowerLimit().getValue();
		retard = (int) (ac.maxViolationTimeWithoutAlarm().getValue()*60000);
		vl.resendRetard = (int)(ac.alarmRepetitionTime().getValue()*60000);
		this.ac = ac;
		this.vl = vl;
		vl.maxIntervalBetweenNewValues = (long) (ac.maxIntervalBetweenNewValues().getValue()*60000l);
		String[] exts = ac.alarmingExtensions().getValues();
		if(ac.sensorVal().exists() ) { //&& ac.supervisedSensor().reading() instanceof SingleValueResource) {
			//SingleValueResource target = ac.sensorVal();
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
			String alarmID, ApplicationManagerPlus appManPlus, Datapoint dp, String baseUrl) {
		this.appManPlus = appManPlus;
		this.dp = dp;
		this.alarmID = alarmID;
		this.baseUrl = baseUrl;
		this.upper = upper;
		this.lower = lower;
		this.retard = retard;
		vl.resendRetard = resendRetard;
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
		resourceChanged(val, alarmStatus, now);
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
								vl.resendRetard;
							data.timer = null;
						}
					};
				}
			}
		}
	}
	public void resourceChanged(float value, IntegerResource alarmStatus, long timeStamp) {
		long now = timeStamp;
		if(vl.isNoValueAlarmActive) {
			releaseAlarm(ac, value, Float.NaN, Float.NaN, alarmStatus);
			vl.isNoValueAlarmActive = false;
		}
		
		if(TimeSeriesServlet.isViolated(value, lower, upper)) {
			if(AlarmingManager.isNewAlarmRetardPhaseAllowed(vl, appManPlus.appMan())) {
				vl.timer = new CountDownDelayedExecutionTimer(appManPlus.appMan(), 
						retard) {
					@Override
					public void delayedExecution() {
						executeAlarm(ac, value, upper, lower, alarmStatus);
						vl.isAlarmActive = true;
						vl.nextTimeAlarmAllowed = appManPlus.appMan().getFrameworkTime() +
							vl.resendRetard;
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
					releaseAlarm(ac, value, upper, lower, alarmStatus);
					vl.isAlarmActive = false;
					vl.alarmReleaseTimer = null;
				}
			};
		}
		
		if(!Float.isNaN(value))
			vl.lastTimeOfNewData = now;
	}
	
	@Override
	public void executeAlarm(AlarmConfiguration ac, float value, float upper, float lower,
			IntegerResource alarmStatus) {
		String title = dp.label(null)+" (Alarming Wert)";
		if(upper == 1.0f && lower == 1.0f) {
			title += "(Schalter)";
		}
		String message = "Current value: "+value+"\r\n"+"  Lower limit: "+lower+
				"\r\n"+"  Upper limit: "+upper;
		if(baseUrl != null)
			message +="\r\nSee also: "+baseUrl+"/org/smartrplace/hardwareinstall/expert/index.html";
		int status = ac.alarmLevel().getValue();
		if(alarmStatus != null) {
			alarmStatus.setValue(status);
		}
		MessagePriority prio = getMessagePrio(ac.alarmLevel().getValue());
		if(prio != null)
			sendMessage(title, status, message, prio, null);		
	}
	public void executeAlarm(AlarmConfiguration ac, String message,
			IntegerResource alarmStatus, int alarmValue,
			AlarmingExtension extSource) {
		String title = dp.label(null)+" : "+ message;
		if(upper == 1.0f && lower == 1.0f) {
			title += "(switch)";
		}
		if(alarmStatus != null) {
			alarmStatus.setValue(alarmValue);
		}

		MessagePriority prio = getMessagePrio(ac.alarmLevel().getValue());
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
	
	public static MessagePriority getMessagePrio(int resourceValue) {
		switch(resourceValue) {
		case 1:
			return MessagePriority.LOW;
		case 2:
			return MessagePriority.MEDIUM;
		case 3:
			return MessagePriority.HIGH;
		default:
			return null;
		}
	}

}
