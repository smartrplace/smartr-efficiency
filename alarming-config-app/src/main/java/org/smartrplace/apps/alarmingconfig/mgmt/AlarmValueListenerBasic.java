package org.smartrplace.apps.alarmingconfig.mgmt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.devicefinder.api.AlarmingExtension;
import org.ogema.devicefinder.api.AlarmingExtensionListener;
import org.ogema.devicefinder.api.AlarmingExtensionListener.AlarmResult;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.apps.alarmingconfig.mgmt.AlarmingManager.AlarmValueListenerI;
import org.smartrplace.apps.alarmingconfig.mgmt.AlarmingManager.ValueListenerData;
import org.smatrplace.apps.alarmconfig.util.RoomLabelProvider;

import de.iwes.widgets.api.messaging.MessagePriority;

public abstract class AlarmValueListenerBasic<T extends SingleValueResource> implements ResourceValueListener<T>, AlarmValueListenerI {
	final float upper;
	final float lower;
	final int retard;
	final int resendRetard;
	final ValueListenerData vl;
	final AlarmConfiguration ac;
	final List<AlarmingExtensionListener> extensions = new ArrayList<>();

	final ApplicationManagerPlus appManPlus;
	//protected final RoomLabelProvider tsNameProv2;
	protected final Datapoint dp;
	final String alarmID;
	
	protected abstract void releaseAlarm(AlarmConfiguration ac, float value, float upper, float lower,
			IntegerResource alarmStatus);
	protected abstract void sendMessage(String title, String message, MessagePriority prio)
			throws RejectedExecutionException, IllegalStateException;
	protected abstract float getHumanValue(T resource);
	
	/** This constructor is used for FloatResources in Sensors and for SmartEffTimeseries, e.g. manual time seroes*/
	public AlarmValueListenerBasic(AlarmConfiguration ac, ValueListenerData vl,
			String alarmID, ApplicationManagerPlus appManPlus, Datapoint dp) {
		this.appManPlus = appManPlus;
		this.dp = dp;
		this.alarmID = alarmID;
		upper = ac.upperLimit().getValue();
		lower = ac.lowerLimit().getValue();
		retard = (int) (ac.maxViolationTimeWithoutAlarm().getValue()*60000);
		resendRetard = (int)(ac.alarmRepetitionTime().getValue()*60000);
		this.ac = ac;
		this.vl = vl;
		vl.maxIntervalBetweenNewValues = (long) (ac.maxIntervalBetweenNewValues().getValue()*60000l);
	}
	
	/** This constructor is used by the inherited class AlarmListenerBoolean used for OnOffSwitch supervision*/
	public AlarmValueListenerBasic(float upper, float lower, int retard, int resendRetard,
			AlarmConfiguration ac, ValueListenerData vl,
			String alarmID, ApplicationManagerPlus appManPlus, Datapoint dp) {
		this.appManPlus = appManPlus;
		this.dp = dp;
		this.alarmID = alarmID;
		this.upper = upper;
		this.lower = lower;
		this.retard = retard;
		this.resendRetard = resendRetard;
		this.vl = vl;
		this.ac = ac;
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
	
	@Override
	public void resourceChanged(T resource) {
		IntegerResource alarmStatus = AlarmingManager.getAlarmStatus(resource);
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
				executeAlarm(ac, result.message(), alarmStatus, result.alarmValue());
				vl.isAlarmActive = true;				
			}
		}
	}
	public void resourceChanged(float value, IntegerResource alarmStatus, long timeStamp) {
		long now = timeStamp;
		if(vl.isNoValueAlarmActive) {
			releaseAlarm(ac, value, Float.NaN, Float.NaN, alarmStatus);
			vl.isNoValueAlarmActive = false;
		}
		
		if(AlarmingManager.isViolated(value, lower, upper)) {
System.out.println("Detected alarming violation:"+value+" for "+ac.getLocation()+", starting time for "+String.format("%.1f seconds", retard*0.001f));			
			if(AlarmingManager.isNewAlarmRetardPhaseAllowed(vl, appManPlus.appMan())) {
				vl.timer = new CountDownDelayedExecutionTimer(appManPlus.appMan(), 
						retard) {
					@Override
					public void delayedExecution() {
						executeAlarm(ac, value, upper, lower, alarmStatus);
						vl.isAlarmActive = true;
						vl.nextTimeAlarmAllowed = appManPlus.appMan().getFrameworkTime() +
							resendRetard;
						vl.timer = null;
					}
				};
			}
		} else if(vl.timer != null) {
			vl.timer.destroy();
			vl.timer = null;
		} else if(vl.isAlarmActive && vl.alarmReleaseTimer == null) {
			vl.alarmReleaseTimer = new CountDownDelayedExecutionTimer(appManPlus.appMan(), 
					retard) {
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
	
	public void executeAlarm(AlarmConfiguration ac, float value, float upper, float lower,
			IntegerResource alarmStatus) {
		String title = alarmID+": "+dp.label(null)+" (Alarming Wert)";
		if(upper == 1.0f && lower == 1.0f) {
			title += "(Schalter)";
		}
		String message = "Aktueller Wert: "+value+"\r\n"+"  Untere Grenze: "+lower+
				"\r\n"+"  Obere Grenze: "+upper;
		MessagePriority prio = getMessagePrio(ac.alarmLevel().getValue());
		if(prio != null)
			sendMessage(title, message, prio);
		
		if(alarmStatus != null) {
			alarmStatus.setValue(ac.alarmLevel().getValue());
		}
	}
	public void executeAlarm(AlarmConfiguration ac, String message,
			IntegerResource alarmStatus, int alarmValue) {
		String title = alarmID+": "+dp.label(null)+" : "+ message;
		if(upper == 1.0f && lower == 1.0f) {
			title += "(Schalter)";
		}
		MessagePriority prio = getMessagePrio(ac.alarmLevel().getValue());
		if(prio != null)
			sendMessage(title, message, prio);
		
		if(alarmStatus != null) {
			alarmStatus.setValue(alarmValue);
		}
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
