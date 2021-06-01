package org.smartrplace.apps.alarmingconfig.mgmt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.AppID;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.Resource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceNotFoundException;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.devicefinder.api.AlarmingExtension;
import org.ogema.devicefinder.api.AlarmingExtension.AlarmNotificationResult;
import org.ogema.devicefinder.api.AlarmingExtension.BaseAlarm;
import org.ogema.devicefinder.api.AlarmingExtension.BaseAlarmI;
import org.ogema.devicefinder.api.AlarmingExtension.MessageDestination;
import org.ogema.devicefinder.api.AlarmingService;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.AlarmingExtensionBase.AlarmListenerDataBase;
import org.ogema.devicefinder.util.AlarmingExtensionBase.ValueListenerDataBase;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.recordreplay.testing.RecReplayAlarmingBaseObserver;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.apps.alarmingconfig.gui.MainPage;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gateway.device.VirtualTestDevice;
import org.smartrplace.hwinstall.basetable.HardwareTableData;
import org.smartrplace.util.message.MessageImpl;

import de.iwes.util.format.StringFormatHelper;
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
		
		VirtualTestDevice testDevice = ResourceHelper.getSampleResource(VirtualTestDevice.class);
		testDevice.sensor_SF().reading().create();
		testDevice.sensor_SF().reading().setValue(1);
		testDevice.sensor_CF().reading().create();
		testDevice.sensor_CF().reading().setValue(1);
		testDevice.sensor_BT().reading().create();
		testDevice.sensor_BT().reading().setValue(1);
		testDevice.activate(true);
		
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
			if(!ac.sensorVal().exists())
				continue; //we perform cleanup somewhere else
			if((!ac.sendAlarm().getValue())) {
				try  {
					IntegerResource alarmStatus = AlarmingConfigUtil.getAlarmStatus(ac.sensorVal());
					if(alarmStatus != null)
						alarmStatus.setValue(0);
					continue;
				} catch(ResourceNotFoundException e) {
					e.printStackTrace();
					continue;
				}
			}
			activeAlarms.add(ac);
			Datapoint dp = MainPage.getDatapoint(ac, appManPlus.dpService());
			final SingleValueResource sres;
			final ValueListenerData vl;
			
			if(ac.sensorVal() instanceof BooleanResource) {
				BooleanResource res = (BooleanResource) ac.sensorVal().getLocationResource();
				sres = res;
				vl = new ValueListenerData(res);
				vl.lastTimeOfNewData = now;
				AlarmValueListenerBoolean mylistener = new AlarmValueListenerBoolean(ac, vl, appManPlus, dp);
				vl.listener = mylistener;
				valueListeners.add(vl);
				res.addValueListener(mylistener, true);
				//continue;
			} else if(ac.sensorVal() instanceof IntegerResource) {
				IntegerResource res = (IntegerResource) ac.sensorVal().getLocationResource();
				sres = res;
				vl = new ValueListenerData(res);
				vl.lastTimeOfNewData = now;
				AlarmValueListenerInteger mylistener = new AlarmValueListenerInteger(ac, vl, appManPlus, dp);
				vl.listener = mylistener;
				valueListeners.add(vl);
				res.addValueListener(mylistener, true);
				//continue;
			} else if(ac.sensorVal() instanceof FloatResource) {
				FloatResource res = (FloatResource) ac.sensorVal().getLocationResource();
				sres = res;
				vl = new ValueListenerData(res);
				vl.lastTimeOfNewData = now;
				AlarmValueListener mylistener = new AlarmValueListener(ac, vl, appManPlus, dp);
				vl.listener = mylistener;
				valueListeners.add(vl);
				res.addValueListener(mylistener, true);

				if(ac.performAdditinalOperations().getValue()) {
					OnOffSwitch onOff = AlarmingUtiH.getSwitchFromSensor(ac.sensorVal());
					if(onOff != null) {
						BooleanResource bres = onOff.stateFeedback().getLocationResource();
						ValueListenerData vlb = new ValueListenerData(bres);
						AlarmValueListenerBooleanOnOff onOffListener = new AlarmValueListenerBooleanOnOff(1.0f, 1.0f, 60000, 600000, ac, vlb,
								appManPlus, dp) {
							@Override
							public void executeAlarm(AlarmConfiguration ac, float value, float upper, float lower,
									IntegerResource alarmStatus) {
								onOff.stateControl().setValue(true);
								super.executeAlarm(ac, value, upper, lower, alarmStatus);
							}
						};
						vlb.listener = onOffListener;
						valueListeners.add(vlb);
						bres.addValueListener(onOffListener, false);					
					}
				}
				
			} else {
				vl = null;
				sres = null;
			}
			if(sres == null) {
				appManPlus.appMan().getLogger().warn("Sensor reading not of suppored type: "+ac.sensorVal().getResourceType().getName()+
						ac.sensorVal().getLocation());
				continue;
			}
				
			IntegerResource alarmStatus = AlarmingConfigUtil.getAlarmStatus(sres);
			if(alarmStatus == null)
				continue;
			if(alarmStatus.getValue() > 1000)
				vl.isNoValueAlarmActive = true;
			else if(alarmStatus.getValue() > 0)
				vl.isAlarmActive = true;
		}
		
		BaseAlarmI baseAlarm = new BaseAlarmI() {
			@Override
			public void sendMessage(String title2, String message2, MessagePriority prio2, MessageDestination md) {
				String appToUseLoc;
				if(md != null)
					appToUseLoc = getDestinationString(md);
				else
					appToUseLoc = null;
				sendMessageIntern(title2, message2, prio2, appToUseLoc);
			}
		};
		Collection<AlarmingExtension> exts = appManPlus.dpService().alarming().getAlarmingExtensions();
		for(AlarmingExtension ext: exts) {
			ext.onStartAlarming(baseAlarm);
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
	
	public static AlarmGroupData getDeviceKnownAlarmState(AlarmConfiguration ac) {
		Resource aclist = ac.getParent();
		if(aclist == null)
			return null;
		Resource acparentRaw = aclist.getParent();
		if(acparentRaw instanceof InstallAppDevice) {
			InstallAppDevice acparent = (InstallAppDevice)acparentRaw;
			return acparent.knownFault();
		}
		return null;
	}

	protected boolean sendNoValueMessageOrRelease(ValueListenerData vl, long now, boolean isRelease) {
		boolean noMessage;
		if(vl.knownDeviceFault == null) {
			vl.knownDeviceFault = getDeviceKnownAlarmState(vl.listener.getAc());
			if(vl.knownDeviceFault == null)
				throw new IllegalStateException("No Known Default for:"+vl.listener.getAc().getPath());
		}
		if(vl.knownDeviceFault.minimumTimeBetweenAlarms().getValue() < 0)
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
	protected class AlarmNoValueListener implements TimerListener {
		@Override
		public void timerElapsed(Timer timer) {
			long now = timer.getExecutionTime();
			
			for(ValueListenerData vl: valueListeners) {
				//for now we do do generate alarms here if not initial value was received
				if(vl.lastTimeOfNewData < 0 || vl.maxIntervalBetweenNewValues <= 0) continue;
				long waiting = now - vl.lastTimeOfNewData;
//debugPrintingForNoValueAlarm(vl, waiting);
				
				if(waiting <= vl.maxIntervalBetweenNewValues) continue;

				if((!vl.isNoValueAlarmActive) || (now > vl.nextTimeNoValueAlarmAllowed)) {
					vl.isNoValueAlarmActive = true;
					vl.nextTimeNoValueAlarmAllowed = now +	vl.resendRetard();
					boolean noMessage = sendNoValueMessageOrRelease(vl, now, false);
					if(vl.res != null) {
						IntegerResource alarmStatus = AlarmingConfigUtil.getAlarmStatus(vl.res);
						float val = getHumanValue(vl.res);
						executeNoValueAlarm(vl.listener.getAc(), val, vl.lastTimeOfNewData,
								vl.maxIntervalBetweenNewValues, alarmStatus, noMessage);
						//reset value
						//if(!Boolean.getBoolean("org.smartrplace.monbase.alarming.suppressSettingNaNInAlarmedResources"))
						//	vl.res.setValue(Float.NaN);
					} else 	if(vl.bres != null) {
						IntegerResource alarmStatus = AlarmingConfigUtil.getAlarmStatus(vl.bres);
						float val = vl.bres.getValue()?1.0f:0.0f;
						executeNoValueAlarm(vl.listener.getAc(), val, vl.lastTimeOfNewData,
								vl.maxIntervalBetweenNewValues, alarmStatus, noMessage);
					} else 	if(vl.ires != null) {
						IntegerResource alarmStatus = AlarmingConfigUtil.getAlarmStatus(vl.ires);
						float val = vl.ires.getValue();
						executeNoValueAlarm(vl.listener.getAc(), val, vl.lastTimeOfNewData,
								vl.maxIntervalBetweenNewValues, alarmStatus, noMessage);
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
			//TODO: For now we block releases for limit alarms and noValue-alarms
			long now = appManPlus.getFrameworkTime();
			boolean noMessage = sendNoValueMessageOrRelease(vl, now, true);
			AlarmingManager.this.releaseAlarm(ac, value, upper, lower, alarmStatus, noMessage);		
		}

		@Override
		protected void sendMessage(String title, Integer status, String message, MessagePriority prio, AlarmingExtension ext)
				throws RejectedExecutionException, IllegalStateException {
			AlarmingManager.this.sendMessage(ac, status, title, message, prio, ac.alarmingAppId(), ext);
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
			long now = appManPlus.getFrameworkTime();
			boolean noMessage = sendNoValueMessageOrRelease(vl, now, true);
			AlarmingManager.this.releaseAlarm(ac, value, upper, lower, alarmStatus, noMessage);		
		}

		@Override
		protected void sendMessage(String title, Integer status, String message, MessagePriority prio, AlarmingExtension ext)
				throws RejectedExecutionException, IllegalStateException {
			AlarmingManager.this.sendMessage(ac, status, title, message, prio, ac.alarmingAppId(), ext);
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
			long now = appManPlus.getFrameworkTime();
			boolean noMessage = sendNoValueMessageOrRelease(vl, now, true);
			AlarmingManager.this.releaseAlarm(ac, value, upper, lower, alarmStatus, noMessage);		
		}

		@Override
		protected void sendMessage(String title, Integer status, String message, MessagePriority prio, AlarmingExtension ext)
				throws RejectedExecutionException, IllegalStateException {
			AlarmingManager.this.sendMessage(ac, status, title, message, prio, ac.alarmingAppId(), ext);
		}

		@Override
		protected float getHumanValue(IntegerResource resource) {
			return resource.getValue();
		}
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

	public static String getTsName(AlarmConfiguration ac, Datapoint dp) {
		InstallAppDevice iad = ResourceHelper.getFirstParentOfType(ac, InstallAppDevice.class);
		String label = dp.label(null);
		if(iad == null)
			return label;
		String devId = iad.deviceId().getValue();
		if(label.contains(devId))
			return label;
		return label+"("+devId+")";
	}
	
	protected void executeNoValueAlarm(AlarmConfiguration ac, float value, long lastTime, long maxInterval,
			IntegerResource alarmStatus, boolean noMessage) {
		int status = ac.alarmLevel().getValue()+1000;
		if(alarmStatus != null) {
			alarmStatus.setValue(status);
		}
		if(noMessage)
			return;
		Datapoint dp = MainPage.getDatapoint(ac, appManPlus.dpService());
		String tsName = getTsName(ac, dp); //dp.label(null); //tsNameProv.getTsName(ac);
		String title = "No more values received:"+tsName+" (Alarming)";
		String message = "Last value received at: "+TimeUtils.getDateAndTimeString(lastTime)+"\r\nValue: "+value+"\r\n"
				+"\r\nMaximum interval: "+(maxInterval/MINUTE_MILLIS)+"min";
		if(baseUrl != null)
			message +="\r\nSee also: "+baseUrl+"/org/smartrplace/hardwareinstall/expert/index.html";
		MessagePriority prio = AlarmValueListenerBasic.getMessagePrio(ac.alarmLevel().getValue());
		if(prio != null)
			sendMessage(ac, status, title, message, prio, ac.alarmingAppId());
	}
	
	protected void releaseAlarm(AlarmConfiguration ac, float value, float upper, float lower,
			IntegerResource alarmStatus, boolean noMessage) {
		if(alarmStatus != null) {
			alarmStatus.setValue(0);
		}
		if(noMessage)
			return;
		Datapoint dp = MainPage.getDatapoint(ac, appManPlus.dpService());
		String tsName = getTsName(ac, dp); //dp.label(null); //tsNameProv.getTsName(ac);
		String title = "Release:"+tsName+" (Alarming)";
		String message = "Value: "+value;
		if(!Float.isNaN(lower))
			message += "\r\n"+"  Lower limit: "+lower+
				"\r\n"+"  Upper limit: "+upper;
		if(baseUrl != null)
			message +="\r\nSee also: "+baseUrl+"/org/smartrplace/hardwareinstall/expert/index.html";
		MessagePriority prio = AlarmValueListenerBasic.getMessagePrio(ac.alarmLevel().getValue());
		if(prio != null)
			sendMessage(ac, 0, title, message, prio, ac.alarmingAppId());
	}
	
	protected class SendMessageData {
		protected long firstSingleMessage = -1;
		protected int numSingleMessage = 0;
	
		protected long firstBulkMessage = -1;
		protected int numBulkMessage = 0;
		protected String bulkMessage = null;
		protected CountDownDelayedExecutionTimer bulkTimer = null;
	}
	
	protected void sendMessage(AlarmConfiguration ac, Integer status, String title, String message, MessagePriority prio,
			StringResource appToUse) {
		sendMessage(ac, status, title, message, prio, appToUse, null);
	}
	protected void sendMessage(AlarmConfiguration ac, Integer status, String title, String message, MessagePriority prio,
			StringResource appToUseREs,
			AlarmingExtension extSource) {
		String appToUse;
		if(appToUseREs.isActive())
			appToUse = appToUseREs.getValue();
		else appToUse = null;
		if(Boolean.getBoolean("org.ogema.recordreplay.testing.alarmingbase"))
			recReplay.recordNewAlarm(ac, appManPlus.getFrameworkTime());
		BaseAlarm baseAlarm = new BaseAlarm() {
			@Override
			public void sendMessage(String title2, String message2, MessagePriority prio2, MessageDestination md) {
				String appToUseLoc;
				if(md != null)
					appToUseLoc = getDestinationString(md);
				else
					appToUseLoc = appToUse;
				sendMessageIntern(title2, message2, prio2, appToUseLoc);
			}
		};
		baseAlarm.isNoValueAlarm = (status != null) && (status >= 1000);
		baseAlarm.isRelease =  (status != null) && (status == 0);
		baseAlarm.message = message;
		baseAlarm.title = title;
		baseAlarm.ac = ac;
		baseAlarm.source = extSource;
		boolean isConsumed = false;
		for(AlarmingExtension ext: appManPlus.dpService().alarming().getAlarmingExtensions()) {
			if(ext.getAlarmNotifications()) {
				AlarmNotificationResult notResult = ext.newAlarmNotification(baseAlarm);
				if(notResult != null && (!notResult.sendAlarmDirectly))
					isConsumed = true;
			}
		}
		if(!isConsumed)
			sendMessageIntern(title, message, prio, appToUse);
		else
			throw new UnsupportedOperationException("Messages shall not be consumed by now! Message sending in groups not implemented yet.");
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
	protected void sendMessageIntern(String title, String message, MessagePriority prio,
			String appToUse) throws RejectedExecutionException, IllegalStateException {
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
			String text = StringFormatHelper.getTimeDateInLocalTimeZone(now)+" : "+title+
					"\r\n"+message;
			if(sd.bulkMessage == null) {
				//We put a return upfront as initial line will be filled with "Notification :" by EmailService, which disturbs when reading through the messages
				sd.bulkMessage = "\r\n"+text;
			} else
				sd.bulkMessage += "\r\n\r\n"+text;
			
			if(isTimeToSendBulkMessages(now, prio)) {
				sendBulkMessages(prio);
			} else if(sd.numBulkMessage == 2*maxMessageBeforeBulk) {
				title = alarmID+": More "+sd.numBulkMessage+" alarms occured...";
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
			reallySendMessage(alarmID+": "+title, message, prio, appToUse);
			System.out.println("         SENT MESSAGE "+alarmID+": "+title+":\r\n"+message);		
		}
		
	}
	
	protected void reallySendMessage(String title, String message, MessagePriority prio, String appToUse) {
		AppID appId;
		if(appToUse != null)
			appId = appsForSending.get(appToUse);
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
		String title = alarmID+": "+sd.numBulkMessage+" Aggregated alarms: ";
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
	
	public String getDestinationString(MessageDestination md) {
		switch(md) {
		case PROVIDER_FIRST:
			return AlarmingUtiH.SP_SUPPORT_FIRST;
		case CUSTOMER_FIRST:
			return AlarmingUtiH.CUSTOMER_FIRST;
		case BOTH_IMMEDIATELY:
			return AlarmingUtiH.CUSTOMER_SP_SAME;
		}
		throw new IllegalStateException("Unknown MessageDestination:"+md);
	}
}
