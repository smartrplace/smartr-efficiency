package org.smartrplace.apps.alarmingconfig;

import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.ResourceList;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.messaging.basic.services.config.ReceiverPageBuilder;
import org.ogema.messaging.basic.services.config.localisation.MessageSettingsDictionary;
import org.ogema.messaging.basic.services.config.localisation.MessageSettingsDictionary_de;
import org.ogema.messaging.basic.services.config.localisation.MessageSettingsDictionary_en;
import org.ogema.messaging.basic.services.config.model.ReceiverConfiguration;
import org.ogema.messaging.configuration.MessagePriorityDropdown;
import org.ogema.messaging.configuration.PageInit;
import org.ogema.messaging.configuration.localisation.SelectConnectorDictionary;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.smartrplace.apps.alarmingconfig.gui.DeviceTypePage;
import org.smartrplace.apps.alarmingconfig.gui.MainPage;
import org.smartrplace.apps.alarmingconfig.gui.MainPage.AlarmingUpdater;
import org.smartrplace.apps.alarmingconfig.gui.PageBuilderSimple;
import org.smartrplace.apps.alarmingconfig.message.reader.dictionary.MessagesDictionary;
import org.smartrplace.apps.alarmingconfig.message.reader.dictionary.MessagesDictionary_de;
import org.smartrplace.apps.alarmingconfig.message.reader.dictionary.MessagesDictionary_en;
import org.smartrplace.apps.alarmingconfig.message.reader.dictionary.MessagesDictionary_fr;
import org.smartrplace.apps.alarmingconfig.mgmt.AlarmingManager;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.messaging.listener.MessageListener;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.messaging.model.MessagingApp;

// here the controller logic is implemented
public class AlarmingConfigAppController implements AlarmingUpdater { //, RoomLabelProvider {
	private HardwareInstallConfig hardwareConfig = null;
	public HardwareInstallConfig getHardwareConfig() {
		if(hardwareConfig == null) {
			hardwareConfig = appMan.getResourceAccess().getResource("hardwareInstallConfig");
		}
		return hardwareConfig;
	}

	public final OgemaLogger log;
    public final ApplicationManager appMan;
    /** This will not be available in the constructor*/
    public final UserPermissionService userPermService;
    public final DatapointService dpService;
    
	public AccessAdminConfig appConfigData;
	public AlarmingConfigApp accessAdminApp;
    public final ApplicationManagerPlus appManPlus;
	
	public MainPage mainPage;
	public DeviceTypePage devicePage;
	public final PageBuilderSimple messagePage;
	public final PageInit forwardingPage;
	public final ReceiverPageBuilder receiverPage;
	WidgetApp widgetApp;

	protected AlarmingManager alarmMan = null;
	
	public String getAlarmingDomain() {
		LocalGatewayInformation ogGw = ResourceHelper.getLocalGwInfo(appMan.getResourceAccess());
		if(ogGw != null && ogGw.id().exists())
			return ogGw.id().getValue();
		return System.getProperty("org.smartrplace.remotesupervision.gateway.id", "SRCA");
	}
	
	public AlarmingConfigAppController(ApplicationManager appMan, AlarmingConfigApp initApp) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.accessAdminApp = initApp;
		this.userPermService = initApp.userAccService;
		this.dpService = initApp.dpService;
		this.appManPlus = new ApplicationManagerPlus(appMan);
		appManPlus.setPermMan(initApp.permMan);
		appManPlus.setUserPermService(userPermService);
		appManPlus.setGuiService(initApp.guiService);
		appManPlus.setDpService(dpService);
		
		appManPlus.getMessagingService().registerMessagingApp(appMan.getAppID(), getAlarmingDomain()+"_Alarming");

		//initAlarmingResources();
		updateAlarming();
		MainPage.alarmingUpdater = this;

		WidgetPage<?> pageRes10 = initApp.widgetApp.createWidgetPage("mainpage.html", true);
		//Resource base = appMan.getResourceAccess().getResource("master");
		mainPage = new MainPage(pageRes10, appManPlus); //, base);
		initApp.menu.addEntry("Alarming Configuration", pageRes10);
		initApp.configMenuConfig(pageRes10.getMenuConfiguration());

		WidgetPage<?> pageRes12 = initApp.widgetApp.createWidgetPage("devices.html", true);
		devicePage = new DeviceTypePage(pageRes12, appManPlus, true);
		initApp.menu.addEntry("Alarming Template Devices Configuration", pageRes12);
		initApp.configMenuConfig(pageRes12.getMenuConfiguration());

		@SuppressWarnings("unchecked")
		final ResourceList<MessagingApp> appList = appMan.getResourceManagement().createResource("messagingApps", ResourceList.class);
		appList.setElementType(MessagingApp.class);
		if(Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.showFullAlarmiing")) {
			WidgetPage<MessagesDictionary> pageRes11 = initApp.widgetApp.createWidgetPage("messages.html", false);
			pageRes11.registerLocalisation(MessagesDictionary_en.class).registerLocalisation(MessagesDictionary_de.class).registerLocalisation(MessagesDictionary_fr.class);
			messagePage = new PageBuilderSimple(pageRes11, initApp.mr, appMan);
			initApp.menu.addEntry("Alarm Messages", pageRes11);
			initApp.configMenuConfig(pageRes11.getMenuConfiguration());
			
			WidgetPage<SelectConnectorDictionary> pageRes2 = initApp.widgetApp.createWidgetPage("forwarding.html", false);
			forwardingPage = new PageInit(pageRes2, appMan, appList, initApp.mr);
			initApp.menu.addEntry("Message Forwarding Configuration", pageRes2);
			initApp.configMenuConfig(pageRes2.getMenuConfiguration());
		} else {
			messagePage = null;
			forwardingPage = null;
		}
		WidgetPage<MessageSettingsDictionary> pageRes3 = initApp.widgetApp.createWidgetPage("receiver.html", false);
		pageRes3.registerLocalisation(MessageSettingsDictionary_de.class).registerLocalisation(MessageSettingsDictionary_en.class);
		de.iwes.widgets.messaging.MessagingApp app1 = null;
		for(de.iwes.widgets.messaging.MessagingApp mapp: initApp.mr.getMessageSenders()) {
			if(mapp.getMessagingId().equals("DEV18410X_Alarming") || "Alarming Configuration".equals(mapp.getName())) {
				app1 = mapp;
				break;
			}
		}
		de.iwes.widgets.messaging.MessagingApp app = app1;
		receiverPage = new ReceiverPageBuilder(pageRes3, appMan) {
			@Override
			protected void addAdditionalColumns(Map<String, Object> receiverHeader) {
				receiverHeader.put("alarmingAppForwardingEmail", "Alarm-level Email:");		
				receiverHeader.put("alarmingAppForwardingSMS", "Alarm-level SMS:");							
			}
			
			@Override
			protected void addAdditionalRowWidgets(ReceiverConfiguration config, String id, Row row,
					OgemaHttpRequest req) {
				if(app == null)
					return;
				String userName = config.userName().getValue();
				List<MessageListener> userListeners = PageInit.getListenersForUser(userName, initApp.mr);
				
				String messageListenerName = "Email-connector";
				MessageListener l = initApp.mr.getMessageListeners().get(messageListenerName);
				if(userListeners.contains(l)) {
					MessagePriorityDropdown alarmingPrioDrop = new MessagePriorityDropdown(pageRes3,
							WidgetHelper.getValidWidgetId("alarmingDrop"+id+userName+messageListenerName),
							l, userName, appList, app);
					row.addCell("alarmingAppForwardingEmail", alarmingPrioDrop);
				}

				messageListenerName = "Sms-connector";
				l = initApp.mr.getMessageListeners().get(messageListenerName);
				if(userListeners.contains(l)) {
					MessagePriorityDropdown alarmingPrioDropSMS = new MessagePriorityDropdown(pageRes3,
							WidgetHelper.getValidWidgetId("alarmingDrop"+id+userName+messageListenerName),
							l, userName, appList, app);
					row.addCell("alarmingAppForwardingSMS", alarmingPrioDropSMS);
				}
			}
		};
		appMan.getResourceAccess().addResourceDemand(ReceiverConfiguration.class, receiverPage);
		initApp.menu.addEntry("Message Receiver Configuration", pageRes3);
		initApp.configMenuConfig(pageRes3.getMenuConfiguration());
		
		if(Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.showFullAlarmiing")) {
			initApp.menu.addEntry("Battery State Alarming", "/de/iee/ogema/batterystatemonitoring/index.html");
			initApp.menu.addEntry("Window Open Alarming", "/de/iwes/ogema/apps/windowopeneddetector/index.html");
		}

		initDemands();		
	}

	
     /*protected void initAlarmingResources() {
  		List<String> done = new ArrayList<>();
  		Map<String, List<String>> roomSensors = new HashMap<>();
  		for(Sensor sens: allSensors) {
 			if(sens.getLocation().contains("valve/connection/powerSensor"))
 				continue;
 			ValueResource vr = sens.reading();
 			if(!(vr instanceof SingleValueResource))
 				continue;
 			SingleValueResource reading = (SingleValueResource) vr;
			AlarmingManager.initValueResourceAlarming(reading, roomSensors, done, this, appMan);
 		}
 		AlarmingManager.finishInitSensors(user, roomSensors, done, appMan);
 		SmartEffUserDataNonEdit user = appMan.getResourceAccess().getResource("master");
 		if(user == null) return;
 		
 		List<String> done = new ArrayList<>();
 		Map<String, List<String>> roomSensors = new HashMap<>();
 		//List<Sensor> allSensors;
 		List<SingleValueResource> allSensors;
 		if(Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.tempsensonly"))
 			allSensors = new ArrayList<Sensor>(appMan.getResourceAccess().getResources(TemperatureSensor.class));
 		else
 			allSensors = appMan.getResourceAccess().getResources(Sensor.class);
 		for(Sensor sens: allSensors) {
 			if(sens.getLocation().contains("valve/connection/powerSensor"))
 				continue;
 			ValueResource vr = sens.reading();
 			if(!(vr instanceof SingleValueResource))
 				continue;
 			SingleValueResource reading = (SingleValueResource) vr;
			AlarmingManager.initValueResourceAlarming(reading, roomSensors, done, this, appMan);
 		}
 		AlarmingManager.finishInitSensors(user, roomSensors, done, appMan);
 	}*/

	/*
     * register ResourcePatternDemands. The listeners will be informed about new and disappearing
     * patterns in the OGEMA resource tree
     */
    public void initDemands() {
    }

	public void close() {
		if(alarmMan != null) {
			alarmMan.close();
			alarmMan = null;
		}
	}

	@Override
	public void updateAlarming() {
		if(alarmMan != null) {
			alarmMan.close();
		}
		List<AlarmConfiguration> configs = appMan.getResourceAccess().getResources(AlarmConfiguration.class);
		alarmMan = new AlarmingManager(configs, appManPlus, getAlarmingDomain());
	}

	/*@Override
	public String getTsName(AlarmConfiguration ac) {
		return RoomLabelProvider.getTsNameDefault(ac);
	}

	@Override
	public String getRoomLabel(String resLocation, OgemaLocale locale) {
		Resource res = appMan.getResourceAccess().getResource(resLocation);
		if(res == null)
			return null;
		//TODO: handle res==null
		String result = RoomLabelProvider.getDatapointShortLabelDefault(res, false, this);
		return result;
		//if(result == null) {
		//	return ExportBulkData.getDeviceShortId(resLocation);
		//} else
		//	return result;
	}

	@Override
	public String getLabel(AlarmConfiguration ac, boolean isOverall) {
		Datapoint dp = dpService.getDataPointStandard(ac.sensorVal());
		return dp.label(OgemaLocale.ENGLISH);
		//String shortLab = RoomLabelProvider.getDatapointShortLabelDefault(ac.supervisedSensor().getLocationResource(), false, this);
		//return shortLab+"-Temperature";
	}*/
}
