package org.smartrplace.apps.alarmingconfig.mgmt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.devicefinder.api.AlarmingService;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.AlarmingExtensionBase.ValueListenerDataBase;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.tools.resource.util.TimeUtils;
import org.smartrplace.apps.alarmingconfig.gui.MainPage;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric.DefaultSetModes;
import org.smartrplace.util.message.MessageImpl;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.widgets.api.messaging.MessagePriority;

public class AlarmingManager {
	
	//private static final long SCHEDULE_CHECK_RATE = 60000;
	private static final long NOVALUE_CHECK_RATE = 10000;
	public static final long HOUR_MILLIS = 60*60000;
	public static final long DAY_MILLIS = 24*HOUR_MILLIS;
	public static final long MINUTE_MILLIS = 60000;
	protected final List<AlarmConfiguration> configs;
	//protected final MonitoringController controller;
	protected final ApplicationManagerPlus appManPlus;
	//protected final RoomLabelProvider tsNameProv;
	public static final Integer maxMessageBeforeBulk = Integer.getInteger("org.smartrplace.util.alarming.maxMessageBeforeBulk");
	
	protected final String alarmID;
	
	protected class ValueListenerData extends ValueListenerDataBase {
		public ValueListenerData(FloatResource res) {
			super(res);
		}
		public ValueListenerData(BooleanResource bres) {
			super(bres);
		}
		public AlarmValueListenerI listener;
	}
	protected final List<ValueListenerData> valueListeners  =
			new ArrayList<>();
	protected final List<ValueListenerData> scheduleConfigs = new ArrayList<>();
	protected Timer scheduleTimer = null;
	protected long lastTimeStamp = -1;
	protected Timer noValueTimer = null;

	public AlarmingManager(List<AlarmConfiguration> configs, ApplicationManagerPlus appManPlus,
			String alarmID) {
		this.configs = configs;
		//this.controller = controller;
		this.appManPlus = appManPlus;
		//this.tsNameProv = tsNameProv;
		this.alarmID = alarmID;	
		
		/*List<IntegerResource> allAlarmStats = appManPlus.appMan().getResourceAccess().getResources(IntegerResource.class);
		for(IntegerResource intr: allAlarmStats) {
			if(!intr.getName().equals(ALARMSTATUS_RES_NAME))
				continue;
			intr.setValue(0);
		}*/
		
		long now = appManPlus.appMan().getFrameworkTime();
		for(AlarmConfiguration ac: configs) {
			//configure if not existing
			//FIXME: !! Change this back after alarming init is done !!
			AlarmingUtiH.setDefaultValuesStatic(ac, DefaultSetModes.SET_IF_NEW);
			
			/*if(ac.supervisedTS().exists()) {
				if((!ac.sendAlarm().getValue()))
					continue;
				ValueListenerData vl = new ValueListenerData((FloatResource)null);
				Schedule sched = ac.supervisedTS().schedule();
				SampledValue val = null;
				if(sched.exists())
					val = sched.getPreviousValue(Long.MAX_VALUE);
				else
					appManPlus.appMan().getLogger().warn("Schedule in "+ac.supervisedTS().getLocation()+" does not exist!");
				if(val == null)
					vl.lastTimeOfNewData = now;
				else
					vl.lastTimeOfNewData = val.getTimestamp();
				vl.listener = new AlarmValueListener(ac, vl, appManPlus, tsNameProv);
				scheduleConfigs.add(vl);
				if(scheduleTimer == null) {
					scheduleTimer = appManPlus.appMan().createTimer(SCHEDULE_CHECK_RATE, new TimerListener() {
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
			} else {*/
			//if(ac.supervisedSensor().reading() instanceof BooleanResource) {
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
				
				IntegerResource alarmStatus = getAlarmStatus(res);
				if(alarmStatus == null)
					return;
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
				if((waiting > vl.maxIntervalBetweenNewValues) &&(!vl.isNoValueAlarmActive)) {
					vl.isNoValueAlarmActive = true;
					if(vl.res != null) {
						IntegerResource alarmStatus = getAlarmStatus(vl.res);
						float val = getHumanValue(vl.res);
						executeNoValueAlarm(vl.listener.getAc(), val, vl.lastTimeOfNewData,
								vl.maxIntervalBetweenNewValues, alarmStatus);
						//reset value
						if(!Boolean.getBoolean("org.smartrplace.monbase.alarming.suppressSettingNaNInAlarmedResources"))
							vl.res.setValue(Float.NaN);
					} else 	if(vl.bres != null) {
						//TODO: OnOffSwitches do not have alarmStatus yet. This should not be activated yet
						IntegerResource alarmStatus = getAlarmStatus(vl.res);
						//if(alarmStatus == null)
						//	alarmStatus = getAlarmStatus(vl.listener.getAc().supervisedTS().schedule());
						float val = vl.bres.getValue()?1.0f:0.0f;
						executeNoValueAlarm(vl.listener.getAc(), val, vl.lastTimeOfNewData,
								vl.maxIntervalBetweenNewValues, alarmStatus);
						//reset value
						//if(!Boolean.getBoolean("org.smartrplace.monbase.alarming.suppressSettingNaNInAlarmedResources"))
						//	vl.bres.setValue(Float.NaN);
					} else {
						//IntegerResource alarmStatus = getAlarmStatus(vl.listener.getAc().supervisedTS().schedule());
						//executeNoValueAlarm(vl.listener.getAc(), Float.NaN, vl.lastTimeOfNewData,
						//		vl.maxIntervalBetweenNewValues, alarmStatus);						
					}
				}
			}
		}
	}
	
	public static IntegerResource getAlarmStatus(ValueResource reading) {
		Resource parent = reading.getParent();
		IntegerResource alarmStatus = parent.getSubResource(AlarmingService.ALARMSTATUS_RES_NAME,
				IntegerResource.class);
		return alarmStatus.isActive()?alarmStatus:null;		
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
			super(ac, vl, AlarmingManager.this.alarmID, appManPlus, dp);
		}
		
		// This constructor is used by the inherited class AlarmListenerBoolean used for OnOffSwitch supervision
		public AlarmValueListener(float upper, float lower, int retard, int resendRetard,
				AlarmConfiguration ac, ValueListenerData vl, ApplicationManagerPlus appManPlus, Datapoint dp) {
			super(upper, lower, retard, resendRetard, ac, vl, AlarmingManager.this.alarmID, appManPlus, dp);
		}

		@Override
		protected void releaseAlarm(AlarmConfiguration ac, float value, float upper, float lower,
				IntegerResource alarmStatus) {
			AlarmingManager.this.releaseAlarm(ac, value, upper, lower, alarmStatus);		
		}

		@Override
		protected void sendMessage(String title, String message, MessagePriority prio)
				throws RejectedExecutionException, IllegalStateException {
			AlarmingManager.this.sendMessage(title, message, prio);
		}

		@Override
		protected float getHumanValue(FloatResource resource) {
			return AlarmingManager.getHumanValue(resource);
		}
	}
	protected class AlarmValueListenerBoolean extends AlarmValueListenerBasic<BooleanResource> {
		public AlarmValueListenerBoolean(AlarmConfiguration ac, ValueListenerData vl, ApplicationManagerPlus appManPlus, Datapoint dp) {
			super(ac, vl, AlarmingManager.this.alarmID, appManPlus, dp);
		}
		
		// This constructor is used by the inherited class AlarmListenerBoolean used for OnOffSwitch supervision
		public AlarmValueListenerBoolean(float upper, float lower, int retard, int resendRetard,
				AlarmConfiguration ac, ValueListenerData vl, ApplicationManagerPlus appManPlus, Datapoint dp) {
			super(upper, lower, retard, resendRetard, ac, vl, AlarmingManager.this.alarmID, appManPlus, dp);
		}

		@Override
		protected void releaseAlarm(AlarmConfiguration ac, float value, float upper, float lower,
				IntegerResource alarmStatus) {
			AlarmingManager.this.releaseAlarm(ac, value, upper, lower, alarmStatus);		
		}

		@Override
		protected void sendMessage(String title, String message, MessagePriority prio)
				throws RejectedExecutionException, IllegalStateException {
			AlarmingManager.this.sendMessage(title, message, prio);
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
			IntegerResource alarmStatus = getAlarmStatus(resource);
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
	
	protected static boolean isViolated(float value, float lower, float upper) {
		if(value < lower) return true;
		if(value > upper) return true;
		return false;
	}
	
	protected static boolean isNewAlarmRetardPhaseAllowed(ValueListenerData vl,
			ApplicationManager appMan) {
		if(vl.timer != null) return false;
		if(vl.nextTimeAlarmAllowed <= 0) return true;
		if(vl.nextTimeAlarmAllowed < appMan.getFrameworkTime()) {
			vl.nextTimeAlarmAllowed = -1;
			return true;
		} else return false;
	}
	
	public void close() {
		for(ValueListenerData vl: valueListeners) {
			if(vl.res != null) vl.res.removeValueListener(vl.listener.getListener());
			else if(vl.bres != null) vl.bres.removeValueListener(vl.listener.getListener());
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
		String title = alarmID+": Kein neuer Wert:"+tsName+" (Alarming)";
		String message = "Letzter Wert wurde empfangen um: "+TimeUtils.getDateAndTimeString(lastTime)+"\r\nWert: "+value+"\r\n"
				+"\r\nMaximales Intervall: "+(maxInterval/MINUTE_MILLIS)+"min";
		MessagePriority prio = AlarmValueListenerBasic.getMessagePrio(ac.alarmLevel().getValue());
		if(prio != null)
			sendMessage(title, message, prio);
		
		if(alarmStatus != null) {
			alarmStatus.setValue(ac.alarmLevel().getValue()+1000);
		}
	}
	
	protected void releaseAlarm(AlarmConfiguration ac, float value, float upper, float lower,
			IntegerResource alarmStatus) {
		String title = alarmID+": Release:"+ac.name().getValue()+" (Alarming)";
		String message = "Wert: "+value;
		if(!Float.isNaN(lower))
			message += "\r\n"+"  Untere Grenze: "+lower+
				"\r\n"+"  Obere Grenze: "+upper;
		MessagePriority prio = AlarmValueListenerBasic.getMessagePrio(ac.alarmLevel().getValue());
		if(prio != null)
			sendMessage(title, message, prio);
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
		long now = appManPlus.appMan().getFrameworkTime();

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
				appManPlus.guiService().getMessagingService().sendMessage(appManPlus.appMan().getAppID(),
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
			appManPlus.guiService().getMessagingService().sendMessage(appManPlus.appMan().getAppID(),
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
	
	private void debugPrintingForNoValueAlarm(ValueListenerData vl, long waiting) {
		//if(vl.res != null && vl.res.getLocation().contains("JMBUS_BASE/_22009444/USER_DEFINED_0_0")) {
		//	System.out.println("Last A-CO2 time:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(vl.lastTimeOfNewData)+"   Diff:"+waiting/1000+"    Max:"+vl.maxIntervalBetweenNewValues/1000+"   Active:"+vl.isNoValueAlarmActive);
		//} else 
		if((waiting > (vl.maxIntervalBetweenNewValues/2)) &&(!vl.isNoValueAlarmActive)) {
			System.out.println("Last "+vl.listener.getAc().getLocation()+" time:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(vl.lastTimeOfNewData)+"   Diff:"+waiting/1000+"    Max:"+vl.maxIntervalBetweenNewValues/1000+"   Active:"+vl.isNoValueAlarmActive);	
		}
	}
	
	/*public static void initValueResourceAlarming(SingleValueResource reading, Map<String, List<String>> roomSensors,
			List<String> done, RoomLabelProvider roomLabelProv, ApplicationManager appMan) {
		if(done.contains(reading.getLocation())) {
			System.out.println("Already in done:"+reading.getLocation());
			return;
		} else done.add(reading.getLocation());
		System.out.println("Added to done:"+reading.getLocation());
		//Sensor sensor = ResourceHelper.getFirstParentOfType(reading, Sensor.class);
		String room = roomLabelProv.getRoomLabel(reading.getLocation(), null);
		BuildingUnit bu = KPIResourceAccessSmarEff.getRoomConfigResource(room, appMan, true); //InitUtil.getBuildingUnitByRoom(room, user.editableData());
		if(bu == null) {
			if(room.toLowerCase().equals("gesamt"))
				return;
			bu = KPIResourceAccessSmarEff.getRoomConfigResource(null, appMan, true); //InitUtil.getBuildingUnitByRoom("Gesamt", user.editableData());
			if(bu == null)
				return;
		}
		List<String> buSensors = roomSensors.get(bu.getLocation());
		if(buSensors == null) {
			buSensors = new ArrayList<>();
			roomSensors.put(bu.getLocation(), buSensors);
		}
		//buSensors.add(sensor.getLocation());
		//InitUtil.initAlarmForSensor(sensor, bu, roomLabelProv);		
		buSensors.add(reading.getLocation());
		InitUtil.initAlarmForSensor2(reading, bu, roomLabelProv);		
	}*/
	
	/*public static void finishInitSensorVals(SmartEffUserDataNonEdit user, Map<String, List<String>> roomSensorVals,
			List<String> done, ApplicationManager appMan) {
		//clean up sensor entries
		for(BuildingData build: user.editableData().buildingData().getAllElements()) {
			for(BuildingUnit bu: build.buildingUnit().getAllElements()) {
				@SuppressWarnings("unchecked")
				ResourceList<AlarmConfiguration> alarms = bu.getSubResource("alarmConfigs", ResourceList.class);
				for(AlarmConfiguration ac: alarms.getAllElements()) {
					//if(ac.supervisedTS().exists()) continue;
					if(!ac.sensorVal().exists())
						throw new IllegalStateException("Sensor for alarm does not exist!");
					List<String> buSensors = roomSensorVals.get(bu.getLocation());
					if(buSensors == null) {
						ac.delete();
						continue;
					}
					if(!buSensors.contains(ac.sensorVal().getLocation())) {
						ac.delete();
					}
				}
			}
		}
		
	}*/


}
