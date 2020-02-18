package org.smartrplace.monbase.alarming;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric.DefaultSetModes;
import org.smartrplace.util.message.MessageImpl;
import org.sp.smarteff.monitoring.alarming.AlarmingEditPage;
import org.sp.smarteff.monitoring.alarming.AlarmingUtil;

import de.iwes.widgets.api.messaging.MessagePriority;
import extensionmodel.smarteff.monitoring.AlarmConfigBase;

public class AlarmingManagement {
	public static final String ALARMSTATUS_RES_NAME = "alarmStatus";
	
	private static final long SCHEDULE_CHECK_RATE = 60000;
	private static final long NOVALUE_CHECK_RATE = 10000;
	public static final long HOUR_MILLIS = 60*60000;
	public static final long DAY_MILLIS = 24*HOUR_MILLIS;
	public static final long MINUTE_MILLIS = 60000;
	protected final List<AlarmConfigBase> configs;
	protected final MonitoringController controller;
	public static final Integer maxMessageBeforeBulk = Integer.getInteger("org.smartrplace.util.alarming.maxMessageBeforeBulk");
	
	protected final String alarmID;
	
	protected class ValueListenerData {
		public ValueListenerData(FloatResource res) {
			this.res = res;
			this.bres = null;
		}
		public ValueListenerData(BooleanResource bres) {
			this.res = null;
			this.bres = bres;
		}
		public AlarmValueListenerI listener;
		public final FloatResource res;
		public final BooleanResource bres;
		public CountDownDelayedExecutionTimer timer = null;
		public CountDownDelayedExecutionTimer alarmReleaseTimer = null;
		public long nextTimeAlarmAllowed = -1;
		public boolean isAlarmActive = false;
		public boolean isNoValueAlarmActive = false;
		
		//supervision for last data received
		public long lastTimeOfNewData = -1;
		public long maxIntervalBetweenNewValues;
	}
	protected final List<ValueListenerData> valueListeners  =
			new ArrayList<>();
	protected final List<ValueListenerData> scheduleConfigs = new ArrayList<>();
	protected Timer scheduleTimer = null;
	protected long lastTimeStamp = -1;
	protected Timer noValueTimer = null;

	public AlarmingManagement(List<AlarmConfigBase> configs, MonitoringController controller, String alarmID) {
		this.configs = configs;
		this.controller = controller;
		this.alarmID = alarmID;	
		
		List<IntegerResource> allAlarmStats = controller.appMan.getResourceAccess().getResources(IntegerResource.class);
		for(IntegerResource intr: allAlarmStats) {
			if(!intr.getName().equals(ALARMSTATUS_RES_NAME))
				continue;
			intr.setValue(0);
		}
		
		long now = controller.appMan.getFrameworkTime();
		for(AlarmConfigBase ac: configs) {
			//configure if not existing
			//FIXME: !! Change this back after alarming init is done !!
			AlarmingEditPage.setDefaultValuesStatic(ac, DefaultSetModes.SET_IF_NEW);
			
			if(ac.supervisedTS().exists()) {
				if((!ac.sendAlarm().getValue()))
					continue;
				ValueListenerData vl = new ValueListenerData((FloatResource)null);
				Schedule sched = ac.supervisedTS().schedule();
				SampledValue val = null;
				if(sched.exists())
					val = sched.getPreviousValue(Long.MAX_VALUE);
				else
					controller.log.warn("Schedule in "+ac.supervisedTS().getLocation()+" does not exist!");
				if(val == null)
					vl.lastTimeOfNewData = now;
				else
					vl.lastTimeOfNewData = val.getTimestamp();
				vl.listener = new AlarmValueListener(ac, vl);
				scheduleConfigs.add(vl);
				if(scheduleTimer == null) {
					scheduleTimer = controller.appMan.createTimer(SCHEDULE_CHECK_RATE, new TimerListener() {
						@Override
						public void timerElapsed(Timer timer) {
							for(ValueListenerData vl: scheduleConfigs) {
								Schedule sched = ac.supervisedTS().schedule();
								try {
								SampledValue val = sched.getPreviousValue(Long.MAX_VALUE);
								if(val == null) continue;
								if(val.getTimestamp() > vl.lastTimeOfNewData) {
									float fval = val.getValue().getFloatValue();
									IntegerResource alarmStatus = getAlarmStatus(sched);
									vl.listener.resourceChanged(fval, alarmStatus, val.getTimestamp());
								}
								} catch(Exception e) {
									//TODO: we have to work on this later
									System.out.println(" !! Needs FIXING");
								}
							}
							lastTimeStamp = timer.getExecutionTime();
						}
					});
				}
			} else {
				if(!(ac.supervisedSensor().reading() instanceof FloatResource)) {
					controller.log.warn("Sensor reading not of type FloatResource:"+
							ac.supervisedSensor().reading().getLocation());
					continue;
				}
				if(ac.sendAlarm().getValue()) {
					FloatResource res = (FloatResource) ac.supervisedSensor().reading().getLocationResource();
					ValueListenerData vl = new ValueListenerData(res);
					vl.lastTimeOfNewData = now;
					AlarmValueListener mylistener = new AlarmValueListener(ac, vl);
					vl.listener = mylistener;
					valueListeners.add(vl);
					res.addValueListener(mylistener, true);
				}
				if(!ac.performAdditinalOperations().getValue()) continue;
				OnOffSwitch onOff = AlarmingUtil.getSwitchFromSensor(ac.supervisedSensor());
				if(onOff != null) {
					BooleanResource bres = onOff.stateFeedback().getLocationResource();
					ValueListenerData vl = new ValueListenerData(bres);
					AlarmValueListenerBoolean onOffListener = new AlarmValueListenerBoolean(1.0f, 1.0f, 60000, 600000, ac, vl) {
						@Override
						public void executeAlarm(AlarmConfigBase ac, float value, float upper, float lower,
								IntegerResource alarmStatus) {
							onOff.stateControl().setValue(true);
							super.executeAlarm(ac, value, upper, lower, alarmStatus);
						}
					};
					vl.listener = onOffListener;
					valueListeners.add(vl);
					bres.addValueListener(onOffListener, false);					
				}
			}
		}
		
		noValueTimer = controller.appMan.createTimer(NOVALUE_CHECK_RATE, new AlarmNoValueListener());
		
		System.out.println("New AlarmingManagement started.");
	}
	
	protected class AlarmNoValueListener implements TimerListener {
		@Override
		public void timerElapsed(Timer timer) {
			long now = timer.getExecutionTime();
			
			for(ValueListenerData vl: valueListeners) {
				//for now we do do generate alarms here if not initial value was received
				if(vl.lastTimeOfNewData < 0 || vl.maxIntervalBetweenNewValues <= 0) continue;
				long waiting = now - vl.lastTimeOfNewData;
				if(waiting > vl.maxIntervalBetweenNewValues &&(!vl.isNoValueAlarmActive)) {
					vl.isNoValueAlarmActive = true;
					if(vl.res != null) {
						IntegerResource alarmStatus = getAlarmStatus(vl.res);
						float val = getHumanValue(vl.res);
						executeNoValueAlarm(vl.listener.getAc(), val, vl.lastTimeOfNewData,
								vl.maxIntervalBetweenNewValues, alarmStatus);
						//reset value
						vl.res.setValue(Float.NaN);
					} else {
						IntegerResource alarmStatus = getAlarmStatus(vl.listener.getAc().supervisedTS().schedule());
						executeNoValueAlarm(vl.listener.getAc(), Float.NaN, vl.lastTimeOfNewData,
								vl.maxIntervalBetweenNewValues, alarmStatus);						
					}
				}
			}
		}
	}
	
	protected IntegerResource getAlarmStatus(ValueResource reading) {
		Resource parent = reading.getParent();
		IntegerResource alarmStatus = parent.getSubResource(AlarmingManagement.ALARMSTATUS_RES_NAME,
				IntegerResource.class);
		return alarmStatus.isActive()?alarmStatus:null;		
	}
	protected IntegerResource getAlarmStatus(Schedule sched) {
		IntegerResource alarmStatus = sched.getLocationResource().getSubResource(AlarmingManagement.ALARMSTATUS_RES_NAME,
				IntegerResource.class);
		return alarmStatus.isActive()?alarmStatus:null;	
	}
	
	protected interface AlarmValueListenerI {
		public void resourceChanged(float value, IntegerResource alarmStatus, long timeStamp);		
		void executeAlarm(AlarmConfigBase ac, float value, float upper, float lower,
				IntegerResource alarmStatus);
		AlarmConfigBase getAc();
		ResourceValueListener<?> getListener();
		
	}
	protected class AlarmValueListener implements ResourceValueListener<FloatResource>, AlarmValueListenerI {
		final float upper;
		final float lower;
		final int retard;
		final int resendRetard;
		final ValueListenerData vl;
		final AlarmConfigBase ac;

		/** This constructor is used for FloatResources in Sensors and for SmartEffTimeseries, e.g. manual time seroes*/
		public AlarmValueListener(AlarmConfigBase ac, ValueListenerData vl) {
			upper = ac.upperLimit().getValue();
			lower = ac.lowerLimit().getValue();
			retard = (int) (ac.maxViolationTimeWithoutAlarm().getValue()*60000);
			resendRetard = (int)(ac.alarmRepetitionTime().getValue()*60000);
			this.ac = ac;
			this.vl = vl;
			vl.maxIntervalBetweenNewValues = (long) (ac.maxIntervalBetweenNewValues().getValue()*60000l);
		}
		
		/** This constructor is used by the inherited class AlarmListenerBoolean used for OnOffSwitch supervision*/
		public AlarmValueListener(float upper, float lower, int retard, int resendRetard,
				AlarmConfigBase ac, ValueListenerData vl) {
			this.upper = upper;
			this.lower = lower;
			this.retard = retard;
			this.resendRetard = resendRetard;
			this.vl = vl;
			this.ac = ac;
			vl.maxIntervalBetweenNewValues = (long) (ac.maxIntervalBetweenNewValues().getValue()*60000l);
		}
		
		@Override
		public void resourceChanged(FloatResource resource) {
			IntegerResource alarmStatus = getAlarmStatus(resource);
			float val = getHumanValue(resource);
			//if(resource instanceof TemperatureResource)
			//	val = ((TemperatureResource)resource).getCelsius();
			//else
			//	val = resource.getValue();
			if(Float.isNaN(val)) {
				//we just got the callback after writing NaN
				if(vl.isNoValueAlarmActive)
					return;
				controller.log.warn("Received Nan value in FloatResource:"+resource.getLocation());
				val = -999;
			}
			resourceChanged(val, alarmStatus, controller.appMan.getFrameworkTime());
		}
		public void resourceChanged(float value, IntegerResource alarmStatus, long timeStamp) {
			long now = timeStamp;
			if(vl.isNoValueAlarmActive) {
				releaseAlarm(ac, value, Float.NaN, Float.NaN, alarmStatus);
				vl.isNoValueAlarmActive = false;
			}
			
			if(isViolated(value, lower, upper)) {
				if(isNewAlarmRetardPhaseAllowed(vl)) {
					vl.timer = new CountDownDelayedExecutionTimer(controller.appMan, 
							retard) {
						@Override
						public void delayedExecution() {
							executeAlarm(ac, value, upper, lower, alarmStatus);
							vl.isAlarmActive = true;
							vl.nextTimeAlarmAllowed = controller.appMan.getFrameworkTime() +
								resendRetard;
							vl.timer = null;
						}
					};
				}
			} else if(vl.timer != null) {
				vl.timer.destroy();
				vl.timer = null;
			} else if(vl.isAlarmActive && vl.alarmReleaseTimer == null) {
				vl.alarmReleaseTimer = new CountDownDelayedExecutionTimer(controller.appMan, 
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
		
		public void executeAlarm(AlarmConfigBase ac, float value, float upper, float lower,
				IntegerResource alarmStatus) {
			String title = alarmID+": Für "+controller.getTsName(ac)+" (Alarming)";
			if(upper == 1.0f && lower == 1.0f) {
				title += "(Schalter)";
			}
			String message = "Aktueller Wert: "+value+"\r\n"+"  Untere Grenze: "+lower+
					"\r\n"+"  Obere Grenze: "+upper;
			sendMessage(title, message, MessagePriority.HIGH);
			
			if(alarmStatus != null) {
				alarmStatus.setValue(ac.alarmLevel().getValue());
			}
		}
		@Override
		public AlarmConfigBase getAc() {
			return ac;
		}
		@Override
		public ResourceValueListener<?> getListener() {
			return this;
		}
	}
	
	protected class AlarmValueListenerBoolean
			implements ResourceValueListener<BooleanResource>, AlarmValueListenerI {
		protected final AlarmValueListener alarmValListenerBase;
		
		public AlarmValueListenerBoolean(float upper, float lower, int retard, int resendRetard, AlarmConfigBase ac, ValueListenerData vl) {
			alarmValListenerBase = new AlarmValueListener(upper, lower, retard, resendRetard, ac, vl);
		}
		@Override
		public void resourceChanged(BooleanResource resource) {
			IntegerResource alarmStatus = getAlarmStatus(resource);
			alarmValListenerBase.resourceChanged(resource.getValue()?1.0f:0.0f, alarmStatus, controller.appMan.getFrameworkTime());
		}

		@Override
		public void resourceChanged(float value, IntegerResource alarmStatus, long timeStamp) {
			alarmValListenerBase.resourceChanged(value, alarmStatus, timeStamp);
		}

		//TODO: override
		@Override
		public void executeAlarm(AlarmConfigBase ac, float value, float upper, float lower, IntegerResource alarmStatus) {
			alarmValListenerBase.executeAlarm(ac, value, upper, lower, alarmStatus);
		}	
		
		@Override
		public AlarmConfigBase getAc() {
			return alarmValListenerBase.ac;
		}
		@Override
		public ResourceValueListener<?> getListener() {
			return this;
		}
	}
	
	protected boolean isViolated(float value, float lower, float upper) {
		if(value < lower) return true;
		if(value > upper) return true;
		return false;
	}
	
	protected boolean isNewAlarmRetardPhaseAllowed(ValueListenerData vl) {
		if(vl.timer != null) return false;
		if(vl.nextTimeAlarmAllowed <= 0) return true;
		if(vl.nextTimeAlarmAllowed < controller.appMan.getFrameworkTime()) {
			vl.nextTimeAlarmAllowed = -1;
			return true;
		} else return false;
	}
	
	public void close() {
		for(ValueListenerData vl: valueListeners) {
			if(vl.res != null) vl.res.removeValueListener(vl.listener.getListener());
			else if(vl.bres != null) vl.bres.removeValueListener(vl.listener.getListener());
			else {
				controller.log.warn("ValueListenerData registered without res");
			}
			if(vl.timer != null) vl.timer.destroy();
		}
		if(scheduleTimer != null) {
			scheduleTimer.destroy();
			scheduleTimer = null;
		}
		if(noValueTimer != null) {
			noValueTimer.destroy();
			noValueTimer = null;
		}
		valueListeners.clear();
	}

	protected void executeNoValueAlarm(AlarmConfigBase ac, float value, long lastTime, long maxInterval,
			IntegerResource alarmStatus) {
		String tsName = controller.getTsName(ac);
		String title = alarmID+": Kein neuer Wert:"+tsName+" (Alarming)";
		String message = "Letzter Wert wurde empfangen um: "+TimeUtils.getDateAndTimeString(lastTime)+"\r\nWert: "+value+"\r\n"
				+"\r\nMaximales Intervall: "+(maxInterval/MINUTE_MILLIS)+"min";
		sendMessage(title, message, MessagePriority.HIGH);
		
		if(alarmStatus != null) {
			alarmStatus.setValue(ac.alarmLevel().getValue());
		}
	}
	
	protected void releaseAlarm(AlarmConfigBase ac, float value, float upper, float lower,
			IntegerResource alarmStatus) {
		String title = alarmID+": Release:"+ac.name().getValue()+" (Alarming)";
		String message = "Wert: "+value;
		if(!Float.isNaN(lower))
			message += "\r\n"+"  Untere Grenze: "+lower+
				"\r\n"+"  Obere Grenze: "+upper;
		sendMessage(title, message, MessagePriority.MEDIUM);
		if(alarmStatus != null) {
			alarmStatus.setValue(0);
		}
	}
	
	protected long firstSingleMessage = -1;
	protected int numSingleMessage = 0;

	protected long firstBulkMessage = -1;
	protected int numBulkMessage = 0;
	protected String bulkMessage = null;
	protected void sendMessage(String title, String message, MessagePriority prio) throws RejectedExecutionException, IllegalStateException {
		long now = controller.appMan.getFrameworkTime();

		if((maxMessageBeforeBulk != null)&&(numSingleMessage >= maxMessageBeforeBulk)) {
			if(firstBulkMessage < 0)
				firstBulkMessage = now;
			numBulkMessage++;

			if(bulkMessage == null)
				bulkMessage = title+":"+message;
			else
				bulkMessage += "\r\n"+title+":"+message;
			
			if(((now-firstBulkMessage) > HOUR_MILLIS) && (bulkMessage != null)) {
				firstBulkMessage = -1;
				title = numBulkMessage+" Alarme zusammgengefasst";
				controller.serviceAccess.messageService().sendMessage(controller.appMan.getAppID(),
						new MessageImpl(title, message, prio));
				System.out.println("         SENT BULKMESSAGE "+title+":\r\n"+bulkMessage);		
				if(numBulkMessage < 5) {
					numSingleMessage = 0;					
				}
				numBulkMessage = 0;
				bulkMessage = null;
			}
		} else {
			numSingleMessage++;
			if(firstSingleMessage < 0) {
				firstSingleMessage = now;
				//numSingleMessage = 0;
			} else if((now-firstSingleMessage)>HOUR_MILLIS) {
				firstSingleMessage = -1;
				numSingleMessage = 0;
			}
			controller.serviceAccess.messageService().sendMessage(controller.appMan.getAppID(),
					new MessageImpl(title, message, prio));
			System.out.println("         SENT MESSAGE "+title+":\r\n"+message);		
		}
		
	}
	
	//TODO: Provide this as general util method
	public static float getHumanValue(FloatResource resource) {
		float val;
		if(resource instanceof TemperatureResource)
			val = ((TemperatureResource)resource).getCelsius();
		else
			val = resource.getValue();
		return val;
	}
}
