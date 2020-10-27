package org.smartrplace.apps.alarmingconfig.mgmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.AppID;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.devicefinder.api.AlarmingService;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.AlarmingExtensionBase.AlarmListenerDataBase;
import org.ogema.devicefinder.util.AlarmingExtensionBase.ValueListenerDataBase;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.recordreplay.testing.RecReplayAlarmingBaseObserver;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.apps.alarmingconfig.gui.MainPage;
import org.smartrplace.hwinstall.basetable.HardwareTableData;
import org.smartrplace.util.message.MessageImpl;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.messaging.MessagePriority;

public class AlarmingManager {
	
	//private static final long SCHEDULE_CHECK_RATE = 60000;
	private static final long NOVALUE_CHECK_RATE = 10000;
	//public static final long HOUR_MILLIS = 60*60000;
	//public static final long DAY_MILLIS = 24*HOUR_MILLIS;
	public static final long MINUTE_MILLIS = 60000;
	protected final List<AlarmConfiguration> configs;
	//protected final MonitoringController controller;
	protected final ApplicationManagerPlus appManPlus;
	protected final Map<String, AppID> appsForSending;
	//protected final RoomLabelProvider tsNameProv;
	
	public final Integer maxMessageBeforeBulk;
	public final long bulkMessageInterval;

	protected final String alarmID;
	protected final String baseUrl;
	
	protected class ValueListenerData extends ValueListenerDataBase {
		public ValueListenerData(FloatResource res) {
			super(res);
		}
		public ValueListenerData(BooleanResource bres) {
			super(bres);
		}
		public ValueListenerData(IntegerResource ires) {
			super(ires);
		}
		public AlarmValueListenerI listener;
	}
	protected final List<ValueListenerData> valueListeners  =
			new ArrayList<>();
	protected final List<ValueListenerData> scheduleConfigs = new ArrayList<>();
	protected Timer scheduleTimer = null;
	protected long lastTimeStamp = -1;
	protected Timer noValueTimer = null;

	protected final RecReplayAlarmingBaseObserver recReplay;
	
	public AlarmingManager(List<AlarmConfiguration> configs, ApplicationManagerPlus appManPlus,
			Map<String, AppID> appsForSending,
			String alarmID) {
		this.configs = configs;
		//this.controller = controller;
		this.appManPlus = appManPlus;
		this.appsForSending = appsForSending;
		//this.tsNameProv = tsNameProv;
		this.alarmID = alarmID;	
		this.baseUrl = ResourceHelper.getLocalGwInfo(appManPlus.appMan()).gatewayBaseUrl().getValue();
		
		HardwareTableData hwTableData = new HardwareTableData(appManPlus.appMan());
		maxMessageBeforeBulk = Math.min(2, Integer.getInteger("org.smartrplace.util.alarming.maxMessageBeforeBulk", hwTableData.appConfigData.maxMessageNumBeforeBulk().getValue()));
		if(hwTableData.appConfigData.bulkMessageIntervalDuration().isActive())
			bulkMessageInterval = Long.getLong("org.smartrplace.util.alarming.bulkMessageInterval", hwTableData.appConfigData.bulkMessageIntervalDuration().getValue());
		else
			bulkMessageInterval = Long.getLong("org.smartrplace.util.alarming.bulkMessageInterval", TimeProcUtil.HOUR_MILLIS);
		/*List<IntegerResource> allAlarmStats = appManPlus.appMan().getResourceAccess().getResources(IntegerResource.class);
		for(IntegerResource intr: allAlarmStats) {
			if(!intr.getName().equals(ALARMSTATUS_RES_NAME))
				continue;
			intr.setValue(0);
		}*/
		
		List<AlarmConfiguration> activeAlarms = new ArrayList<>();
		
		long now = appManPlus.appMan().getFrameworkTime();
		for(AlarmConfiguration ac: configs) {
			if((!ac.sendAlarm().getValue()))
				continue;
			activeAlarms.add(ac);
			Datapoint dp = MainPage.getDatapoint(ac, appManPlus.dpService());
			if(ac.sensorVal() instanceof BooleanResource) {
				BooleanResource res = (BooleanResource) ac.sensorVal().getLocationResource();
				ValueListenerData vl = new ValueListenerData(res);
				vl.lastTimeOfNewData = now;
				AlarmValueListenerBoolean mylistener = new AlarmValueListenerBoolean(ac, vl, appManPlus, dp);
				vl.listener = mylistener;
				valueListeners.add(vl);
				res.addValueListener(mylistener, true);
				continue;
			}
			if(ac.sensorVal() instanceof IntegerResource) {
				IntegerResource res = (IntegerResource) ac.sensorVal().getLocationResource();
				ValueListenerData vl = new ValueListenerData(res);
				vl.lastTimeOfNewData = now;
				AlarmValueListenerInteger mylistener = new AlarmValueListenerInteger(ac, vl, appManPlus, dp);
				vl.listener = mylistener;
				valueListeners.add(vl);
				res.addValueListener(mylistener, true);
				continue;
			}
			//if(!(ac.supervisedSensor().reading() instanceof FloatResource)) {
			if(!(ac.sensorVal() instanceof FloatResource)) {
				appManPlus.appMan().getLogger().warn("Sensor reading not of type FloatResource:"+
						ac.sensorVal().getLocation());
				continue;
			}
			if(ac.sendAlarm().getValue()) {
				FloatResource res = (FloatResource) ac.sensorVal().getLocationResource();
				ValueListenerData vl = new ValueListenerData(res);
				vl.lastTimeOfNewData = now;
				AlarmValueListener mylistener = new AlarmValueListener(ac, vl, appManPlus, dp);
				vl.listener = mylistener;
				valueListeners.add(vl);
				
				IntegerResource alarmStatus = AlarmingConfigUtil.getAlarmStatus(res);
				if(alarmStatus == null)
					continue;
				if(alarmStatus.getValue() > 1000)
					vl.isNoValueAlarmActive = true;
				
				res.addValueListener(mylistener, true);
			}
			if(!ac.performAdditinalOperations().getValue()) continue;
			OnOffSwitch onOff = AlarmingUtiH.getSwitchFromSensor(ac.sensorVal());
			if(onOff != null) {
				BooleanResource bres = onOff.stateFeedback().getLocationResource();
				ValueListenerData vl = new ValueListenerData(bres);
				AlarmValueListenerBooleanOnOff onOffListener = new AlarmValueListenerBooleanOnOff(1.0f, 1.0f, 60000, 600000, ac, vl,
						appManPlus, dp) {
					@Override
					public void executeAlarm(AlarmConfiguration ac, float value, float upper, float lower,
							IntegerResource alarmStatus) {
						onOff.stateControl().setValue(true);
						super.executeAlarm(ac, value, upper, lower, alarmStatus);
					}
				};
				vl.listener = onOffListener;
				valueListeners.add(vl);
				bres.addValueListener(onOffListener, false);					
			}
//			}
		}
		
		if(Boolean.getBoolean("org.ogema.recordreplay.testing.alarmingbase")) {
			recReplay = new RecReplayAlarmingBaseObserver(activeAlarms, appManPlus.appMan());
			//recReplay.checkInitialReplay();
			appManPlus.dpService().alarming().registerRecReplayObserver(recReplay);
		} else
			recReplay = null;

		noValueTimer = appManPlus.appMan().createTimer(NOVALUE_CHECK_RATE, new AlarmNoValueListener());
	
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
//debugPrintingForNoValueAlarm(vl, waiting);
				if((waiting > vl.maxIntervalBetweenNewValues) &&
						((!vl.isNoValueAlarmActive) || (now > vl.nextTimeNoValueAlarmAllowed))) {
					vl.isNoValueAlarmActive = true;
					vl.nextTimeNoValueAlarmAllowed = appManPlus.appMan().getFrameworkTime() +
							vl.resendRetard;
					if(vl.res != null) {
						IntegerResource alarmStatus = AlarmingConfigUtil.getAlarmStatus(vl.res);
						float val = getHumanValue(vl.res);
						executeNoValueAlarm(vl.listener.getAc(), val, vl.lastTimeOfNewData,
								vl.maxIntervalBetweenNewValues, alarmStatus);
						//reset value
						//if(!Boolean.getBoolean("org.smartrplace.monbase.alarming.suppressSettingNaNInAlarmedResources"))
						//	vl.res.setValue(Float.NaN);
					} else 	if(vl.bres != null) {
						IntegerResource alarmStatus = AlarmingConfigUtil.getAlarmStatus(vl.bres);
						float val = vl.bres.getValue()?1.0f:0.0f;
						executeNoValueAlarm(vl.listener.getAc(), val, vl.lastTimeOfNewData,
								vl.maxIntervalBetweenNewValues, alarmStatus);
					} else 	if(vl.ires != null) {
						IntegerResource alarmStatus = AlarmingConfigUtil.getAlarmStatus(vl.ires);
						float val = vl.ires.getValue();
						executeNoValueAlarm(vl.listener.getAc(), val, vl.lastTimeOfNewData,
								vl.maxIntervalBetweenNewValues, alarmStatus);
					} else {
						//IntegerResource alarmStatus = getAlarmStatus(vl.listener.getAc().supervisedTS().schedule());
						//executeNoValueAlarm(vl.listener.getAc(), Float.NaN, vl.lastTimeOfNewData,
						//		vl.maxIntervalBetweenNewValues, alarmStatus);						
					}
				}
			}
		}
	}
	
	protected IntegerResource getAlarmStatus(Schedule sched) {
		IntegerResource alarmStatus = sched.getLocationResource().getSubResource(AlarmingService.ALARMSTATUS_RES_NAME,
				IntegerResource.class);
		return alarmStatus.isActive()?alarmStatus:null;	
	}
	
	protected interface AlarmValueListenerI {
		public void resourceChanged(float value, IntegerResource alarmStatus, long timeStamp);		
		void executeAlarm(AlarmConfiguration ac, float value, float upper, float lower,
				IntegerResource alarmStatus);
		AlarmConfiguration getAc();
		ResourceValueListener<?> getListener();
		
	}
	protected class AlarmValueListener extends AlarmValueListenerBasic<FloatResource> {
		public AlarmValueListener(AlarmConfiguration ac, ValueListenerData vl, ApplicationManagerPlus appManPlus, Datapoint dp) {
			super(ac, vl, AlarmingManager.this.alarmID, appManPlus,
					dp, AlarmingManager.this.baseUrl);
		}
		
		// This constructor is used by the inherited class AlarmListenerBoolean used for OnOffSwitch supervision
		public AlarmValueListener(float upper, float lower, int retard, int resendRetard,
				AlarmConfiguration ac, ValueListenerData vl, ApplicationManagerPlus appManPlus, Datapoint dp) {
			super(upper, lower, retard, resendRetard, ac, vl, AlarmingManager.this.alarmID, appManPlus,
					dp, AlarmingManager.this.baseUrl);
		}

		@Override
		protected void releaseAlarm(AlarmConfiguration ac, float value, float upper, float lower,
				IntegerResource alarmStatus) {
			AlarmingManager.this.releaseAlarm(ac, value, upper, lower, alarmStatus);		
		}

		@Override
		protected void sendMessage(String title, String message, MessagePriority prio)
				throws RejectedExecutionException, IllegalStateException {
			AlarmingManager.this.sendMessage(title, message, prio, ac.alarmingAppId());
		}

		@Override
		protected float getHumanValue(FloatResource resource) {
			return AlarmingManager.getHumanValue(resource);
		}
	}
	protected class AlarmValueListenerBoolean extends AlarmValueListenerBasic<BooleanResource> {
		public AlarmValueListenerBoolean(AlarmConfiguration ac, ValueListenerData vl, ApplicationManagerPlus appManPlus, Datapoint dp) {
			super(ac, vl, AlarmingManager.this.alarmID, appManPlus,
					dp, AlarmingManager.this.baseUrl);
		}
		
		// This constructor is used by the inherited class AlarmListenerBoolean used for OnOffSwitch supervision
		public AlarmValueListenerBoolean(float upper, float lower, int retard, int resendRetard,
				AlarmConfiguration ac, ValueListenerData vl, ApplicationManagerPlus appManPlus, Datapoint dp) {
			super(upper, lower, retard, resendRetard, ac, vl, AlarmingManager.this.alarmID, appManPlus,
					dp, AlarmingManager.this.baseUrl);
		}

		@Override
		protected void releaseAlarm(AlarmConfiguration ac, float value, float upper, float lower,
				IntegerResource alarmStatus) {
			AlarmingManager.this.releaseAlarm(ac, value, upper, lower, alarmStatus);		
		}

		@Override
		protected void sendMessage(String title, String message, MessagePriority prio)
				throws RejectedExecutionException, IllegalStateException {
			AlarmingManager.this.sendMessage(title, message, prio, ac.alarmingAppId());
		}

		@Override
		protected float getHumanValue(BooleanResource resource) {
			return resource.getValue()?1:0;
		}
	}

	protected class AlarmValueListenerBooleanOnOff
			implements ResourceValueListener<BooleanResource>, AlarmValueListenerI {
		protected final AlarmValueListener alarmValListenerBase;
		
		public AlarmValueListenerBooleanOnOff(float upper, float lower, int retard, int resendRetard, AlarmConfiguration ac, ValueListenerData vl,
				ApplicationManagerPlus appManPlus, Datapoint dp) {
			alarmValListenerBase = new AlarmValueListener(upper, lower, retard, resendRetard, ac, vl, appManPlus, dp);
		}
		@Override
		public void resourceChanged(BooleanResource resource) {
			IntegerResource alarmStatus = AlarmingConfigUtil.getAlarmStatus(resource);
			alarmValListenerBase.resourceChanged(resource.getValue()?1.0f:0.0f, alarmStatus, appManPlus.appMan().getFrameworkTime());
		}

		@Override
		public void resourceChanged(float value, IntegerResource alarmStatus, long timeStamp) {
			alarmValListenerBase.resourceChanged(value, alarmStatus, timeStamp);
		}

		//TODO: override
		@Override
		public void executeAlarm(AlarmConfiguration ac, float value, float upper, float lower, IntegerResource alarmStatus) {
			alarmValListenerBase.executeAlarm(ac, value, upper, lower, alarmStatus);
		}	
		
		@Override
		public AlarmConfiguration getAc() {
			return alarmValListenerBase.ac;
		}
		@Override
		public ResourceValueListener<?> getListener() {
			return this;
		}
	}
	
	protected class AlarmValueListenerInteger extends AlarmValueListenerBasic<IntegerResource> {
		public AlarmValueListenerInteger(AlarmConfiguration ac, ValueListenerData vl, ApplicationManagerPlus appManPlus, Datapoint dp) {
			super(ac, vl, AlarmingManager.this.alarmID, appManPlus,
					dp, AlarmingManager.this.baseUrl);
		}
		
		// This constructor is used by the inherited class AlarmListenerInteger used for OnOffSwitch supervision
		public AlarmValueListenerInteger(float upper, float lower, int retard, int resendRetard,
				AlarmConfiguration ac, ValueListenerData vl, ApplicationManagerPlus appManPlus, Datapoint dp) {
			super(upper, lower, retard, resendRetard, ac, vl, AlarmingManager.this.alarmID, appManPlus,
					dp, AlarmingManager.this.baseUrl);
		}

		@Override
		protected void releaseAlarm(AlarmConfiguration ac, float value, float upper, float lower,
				IntegerResource alarmStatus) {
			AlarmingManager.this.releaseAlarm(ac, value, upper, lower, alarmStatus);		
		}

		@Override
		protected void sendMessage(String title, String message, MessagePriority prio)
				throws RejectedExecutionException, IllegalStateException {
			AlarmingManager.this.sendMessage(title, message, prio, ac.alarmingAppId());
		}

		@Override
		protected float getHumanValue(IntegerResource resource) {
			return resource.getValue();
		}
	}

	protected static boolean isViolated(float value, float lower, float upper) {
		if(value < lower) return true;
		if(value > upper) return true;
		return false;
	}
	
	protected static boolean isNewAlarmRetardPhaseAllowed(AlarmListenerDataBase vl,
			ApplicationManager appMan) {
		if(vl.timer != null) return false;
		if(vl.nextTimeAlarmAllowed <= 0) return true;
		if(vl.nextTimeAlarmAllowed < appMan.getFrameworkTime()) {
			vl.nextTimeAlarmAllowed = -1;
			return true;
		} else return false;
	}
	
	public void close() {
		for(MessagePriority prio: MessagePriority.values()) {
			SendMessageData sd = sendDataInternal.get(prio);
			if(sd != null && sd.bulkTimer != null) {
				sd.bulkTimer.destroy();
				sd.bulkTimer.delayedExecution();
			}
		}
		for(ValueListenerData vl: valueListeners) {
			if(vl.res != null) vl.res.removeValueListener(vl.listener.getListener());
			else if(vl.bres != null) vl.bres.removeValueListener(vl.listener.getListener());
			else if(vl.ires != null) vl.ires.removeValueListener(vl.listener.getListener());
			else {
				appManPlus.appMan().getLogger().warn("ValueListenerData registered without res");
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

	protected void executeNoValueAlarm(AlarmConfiguration ac, float value, long lastTime, long maxInterval,
			IntegerResource alarmStatus) {
		Datapoint dp = MainPage.getDatapoint(ac, appManPlus.dpService());
		String tsName = dp.label(null); //tsNameProv.getTsName(ac);
		String title = alarmID+": No more values received:"+tsName+" (Alarming)";
		String message = "Last value received at: "+TimeUtils.getDateAndTimeString(lastTime)+"\r\nValue: "+value+"\r\n"
				+"\r\nMaximum interval: "+(maxInterval/MINUTE_MILLIS)+"min";
		if(baseUrl != null)
			message +="\r\nSee also: "+baseUrl+"/org/smartrplace/hardwareinstall/expert/index.html";
		MessagePriority prio = AlarmValueListenerBasic.getMessagePrio(ac.alarmLevel().getValue());
		if(prio != null)
			sendMessage(title, message, prio, ac.alarmingAppId());
		
		if(alarmStatus != null) {
			alarmStatus.setValue(ac.alarmLevel().getValue()+1000);
		}
		if(Boolean.getBoolean("org.ogema.recordreplay.testing.alarmingbase"))
			recReplay.recordNewAlarm(ac, appManPlus.getFrameworkTime());
	}
	
	protected void releaseAlarm(AlarmConfiguration ac, float value, float upper, float lower,
			IntegerResource alarmStatus) {
		Datapoint dp = MainPage.getDatapoint(ac, appManPlus.dpService());
		String tsName = dp.label(null); //tsNameProv.getTsName(ac);
		String title = alarmID+": Release:"+tsName+" (Alarming)";
		String message = "Value: "+value;
		if(!Float.isNaN(lower))
			message += "\r\n"+"  Lower limit: "+lower+
				"\r\n"+"  Upper limit: "+upper;
		if(baseUrl != null)
			message +="\r\nSee also: "+baseUrl+"/org/smartrplace/hardwareinstall/expert/index.html";
		MessagePriority prio = AlarmValueListenerBasic.getMessagePrio(ac.alarmLevel().getValue());
		if(prio != null)
			sendMessage(title, message, prio, ac.alarmingAppId());
		if(alarmStatus != null) {
			alarmStatus.setValue(0);
		}
	}
	
	protected class SendMessageData {
		protected long firstSingleMessage = -1;
		protected int numSingleMessage = 0;
	
		protected long firstBulkMessage = -1;
		protected int numBulkMessage = 0;
		protected String bulkMessage = null;
		protected CountDownDelayedExecutionTimer bulkTimer = null;
	}
	private Map<MessagePriority, SendMessageData> sendDataInternal = new HashMap<>();
	protected SendMessageData sendData(MessagePriority prio) {
		SendMessageData result = sendDataInternal.get(prio);
		if(result == null) {
			result = new SendMessageData();
			sendDataInternal.put(prio, result);
		}
		return result;
	}
	protected void sendMessage(String title, String message, MessagePriority prio,
			StringResource appToUse) throws RejectedExecutionException, IllegalStateException {
		long now = appManPlus.appMan().getFrameworkTime();
		SendMessageData sd = sendData(prio);
		if((maxMessageBeforeBulk != null)&&(sd.numSingleMessage >= this.maxMessageBeforeBulk)) {
			if(sd.firstBulkMessage < 0) {
				sd.firstBulkMessage = now;
				sd.bulkTimer = new CountDownDelayedExecutionTimer(appManPlus.appMan(), bulkMessageInterval) {
					
					@Override
					public void delayedExecution() {
						//if(isTimeToSendBulkMessages(now)) {
						if(sd.bulkMessage != null)
							sendBulkMessages(prio);
						sd.bulkTimer = null;
						//}
					}
				};
			}
			sd.numBulkMessage++;
System.out.println("Bulk messages aggregated: "+sd.numBulkMessage);
			if(sd.bulkMessage == null)
				sd.bulkMessage = title+":"+message;
			else
				sd.bulkMessage += "\r\n"+title+":"+message;
			
			if(isTimeToSendBulkMessages(now, prio)) {
				sendBulkMessages(prio);
			} else if(sd.numBulkMessage == 2*maxMessageBeforeBulk) {
				title = "More alarms occured...";
				String infoMessage = "Number of alarms aggregated by now: "+(sd.numBulkMessage);
				reallySendMessage(title, infoMessage , prio, appToUse);
				System.out.println("         SENT BULKINFOMESSAGE "+title+":\r\n"+infoMessage);						
			}
		} else {
			sd.numSingleMessage++;
			if(sd.firstSingleMessage < 0) {
				sd.firstSingleMessage = now;
			} else if((now-sd.firstSingleMessage)>bulkMessageInterval) {
				sd.firstSingleMessage = -1;
				sd.numSingleMessage = 0;
			}
			if(sd.numSingleMessage >= (maxMessageBeforeBulk))
				message += "\r\n (More messages may be aggregated, will be sent after "+(bulkMessageInterval/TimeProcUtil.MINUTE_MILLIS)+" minutes!)";
			reallySendMessage(title, message, prio, appToUse);
			System.out.println("         SENT MESSAGE "+title+":\r\n"+message);		
		}
		
	}
	
	protected void reallySendMessage(String title, String message, MessagePriority prio, StringResource appToUse) {
		AppID appId;
		if(appToUse.isActive())
			appId = appsForSending.get(appToUse.getValue());
		else
			appId = appManPlus.appMan().getAppID();
		appManPlus.guiService().getMessagingService().sendMessage(appId,
				new MessageImpl(title, message, prio));		
	}
	
	protected boolean isTimeToSendBulkMessages(long now, MessagePriority prio) {
		SendMessageData sd = sendData(prio);
		return ((now-sd.firstBulkMessage) > bulkMessageInterval) && (sd.bulkMessage != null);
	}
	
	protected void sendBulkMessages(MessagePriority prio) {
		SendMessageData sd = sendData(prio);
		sd.firstBulkMessage = -1;
		String title = sd.numBulkMessage+" Aggregated alarms: ";
		appManPlus.guiService().getMessagingService().sendMessage(appManPlus.appMan().getAppID(),
				new MessageImpl(title, sd.bulkMessage, prio));
		System.out.println("         SENT BULKMESSAGE "+title+":\r\n"+sd.bulkMessage);		
		if(sd.numBulkMessage < 5) {
			sd.numSingleMessage = 0;					
		}
		sd.numBulkMessage = 0;
		sd.bulkMessage = null;		
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
