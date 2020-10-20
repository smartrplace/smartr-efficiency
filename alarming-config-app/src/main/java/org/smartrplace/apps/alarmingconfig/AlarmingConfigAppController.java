package org.smartrplace.apps.alarmingconfig;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.alarm.DeviceAlarmingPage;
import org.smartrplace.hwinstall.basetable.HardwareTableData;
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
    
	//public final AccessAdminConfig appConfigData;
	public final HardwareTableData hwTableData;
	
	public AlarmingConfigApp accessAdminApp;
    public final ApplicationManagerPlus appManPlus;
	
	public MainPage mainPage;
	public DeviceTypePage devicePage;
	public DeviceAlarmingPage deviceOverviewPage;
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
	
	public static class MessageSettingsDictAlarming_de extends MessageSettingsDictionary_de {
		@Override
		public String headerReceivers() {
			return messageSettingsHeader();
		}
	}
	public static class MessageSettingsDictAlarming_en extends MessageSettingsDictionary_en {
		@Override
		public String headerReceivers() {
			return messageSettingsHeader();
		}
	}
	
	public static String messageSettingsHeader() {
		if(Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway"))
			return "4. Message Receiver Configuration";
		else
			return "1. Message Receiver Configuration";		
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

		hwTableData = new HardwareTableData(appMan);
		cleanupAlarming();
		//initAlarmingResources();
		updateAlarming();
		MainPage.alarmingUpdater = this;

		boolean isGw = false;
		if(Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway")) {
			WidgetPage<?> pageRes12 = initApp.widgetApp.createStartPage(); //initApp.widgetApp.createWidgetPage("devices.html");
			devicePage = new DeviceTypePage(pageRes12, appManPlus, true);
			initApp.menu.addEntry("1. Device Template Alarming Configuration", pageRes12);
			initApp.configMenuConfig(pageRes12.getMenuConfiguration());
	
			WidgetPage<?> pageRes9 = initApp.widgetApp.createWidgetPage("devicealarm.html");
			//Resource base = appMan.getResourceAccess().getResource("master");
			deviceOverviewPage = new DeviceAlarmingPage(pageRes9, this); //, base);
			initApp.menu.addEntry("2. Device Alarming Overview", pageRes9);
			initApp.configMenuConfig(pageRes9.getMenuConfiguration());

			WidgetPage<?> pageRes10 = initApp.widgetApp.createWidgetPage("mainpage.html");
			//Resource base = appMan.getResourceAccess().getResource("master");
			mainPage = new MainPage(pageRes10, appManPlus); //, base);
			initApp.menu.addEntry("3. Alarming Configuration Details", pageRes10);
			initApp.configMenuConfig(pageRes10.getMenuConfiguration());
			isGw = true;
		}
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
		WidgetPage<MessageSettingsDictionary> pageRes3 = initApp.widgetApp.createWidgetPage("receiver.html", !isGw);
		de.iwes.widgets.messaging.MessagingApp app1 = null;
		for(de.iwes.widgets.messaging.MessagingApp mapp: initApp.mr.getMessageSenders()) {
			if(isGw) {
				if(mapp.getMessagingId().equals("DEV18410X_Alarming") || "Alarming Configuration".equals(mapp.getName())) {
					app1 = mapp;
					break;
				}
			} else {
				if(mapp.getMessagingId().equals("DEV18410X_Alarming") || "Remote gateway heartbeat server".equals(mapp.getName())) {
					app1 = mapp;
					break;
				}				
			}
		}
		de.iwes.widgets.messaging.MessagingApp app = app1;
		pageRes3.registerLocalisation(MessageSettingsDictAlarming_de.class).
				registerLocalisation(MessageSettingsDictAlarming_en.class);
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
		initApp.menu.addEntry(messageSettingsHeader(), pageRes3);
		initApp.configMenuConfig(pageRes3.getMenuConfiguration());
		
		if(Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.showFullAlarmiing")) {
			initApp.menu.addEntry("Battery State Alarming", "/de/iee/ogema/batterystatemonitoring/index.html");
			initApp.menu.addEntry("Window Open Alarming", "/de/iwes/ogema/apps/windowopeneddetector/index.html");
		}

		initDemands();		
	}

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

	public void cleanupAlarming() {
		for(InstallAppDevice dev: hwTableData.appConfigData.knownDevices().getAllElements()) {
			cleanupAlarming(dev);
		}
	}
	
	public void cleanupAlarming(InstallAppDevice dev) {
		Set<String> knownSensors = new HashSet<>();
		for(AlarmConfiguration ac: dev.alarms().getAllElements()) {
			if(knownSensors.contains(ac.sensorVal().getLocation())) {
				log.warn(" Found double alarming entry for "+ac.sensorVal().getLocation()+ " in "+ac.getLocation());
				ac.delete();
			} else {
				knownSensors.add(ac.sensorVal().getLocation());
			}
		}
	}
	
	@Override
	public void updateAlarming() {
		if(alarmMan != null) {
			alarmMan.close();
		}
		if(hwTableData.appConfigData.isAlarmingActive().getValue()) {
			List<AlarmConfiguration> configs = appMan.getResourceAccess().getResources(AlarmConfiguration.class);
			alarmMan = new AlarmingManager(configs, appManPlus, getAlarmingDomain());
		} else
			alarmMan = null;
	}
}
