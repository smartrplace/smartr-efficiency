package org.smartrplace.apps.alarmingconfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.core.application.AppID;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH.AlarmingUpdater;
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
import org.ogema.timeseries.eval.simple.api.AlarmingStartedService;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon3.std.TimeseriesProcAlarming;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.osgi.framework.ServiceRegistration;
import org.smartrplace.apps.alarmconfig.util.AppIDImpl;
import org.smartrplace.apps.alarmingconfig.gui.DeviceTypePage;
import org.smartrplace.apps.alarmingconfig.gui.MainPage;
import org.smartrplace.apps.alarmingconfig.gui.PageBuilderSimple;
import org.smartrplace.apps.alarmingconfig.message.reader.dictionary.MessagesDictionary;
import org.smartrplace.apps.alarmingconfig.message.reader.dictionary.MessagesDictionary_de;
import org.smartrplace.apps.alarmingconfig.message.reader.dictionary.MessagesDictionary_en;
import org.smartrplace.apps.alarmingconfig.message.reader.dictionary.MessagesDictionary_fr;
import org.smartrplace.apps.alarmingconfig.mgmt.AlarmingManager;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.kni.quality.eval.QualityEvalUtil;
import org.smartrplace.gateway.device.VirtualTestDevice;
import org.smartrplace.hwinstall.basetable.HardwareTableData;
import org.smartrplace.hwinstall.basetable.HardwareTablePage;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.messaging.listener.MessageListener;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.messaging.MessageReader;
import de.iwes.widgets.messaging.model.MessagingApp;

// here the controller logic is implemented
@SuppressWarnings("serial")
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
	//public DeviceAlarmingPage deviceOverviewPage;
	public final PageBuilderSimple messagePage;
	public final PageInit forwardingPage;
	public ReceiverPageBuilder receiverPage;
	WidgetApp widgetApp;
	boolean isGw = false;

	public final TimeseriesProcAlarming tsProcAl;
	public final ResourceList<MessagingApp> appList;
	public final MessageReader mr;
	
	protected ServiceRegistration<AlarmingStartedService> srStarted = null;

	//location of device resource->InstallAppDevice resouce
	private Map<String, InstallAppDevice> iads = new HashMap<>();
	public InstallAppDevice getIAD(String devLocation) {
		InstallAppDevice result = iads.get(devLocation);
		if(result != null && result.device().getLocation().equals(devLocation))
			return result;
		List<InstallAppDevice> all = hwTableData.appConfigData.knownDevices().getAllElements();
		for(InstallAppDevice iad: all) {
			if(iad.device().getLocation().equals(devLocation)) {
				iads.put(devLocation, iad);
				return iad;
			}
		}
		return null;
	}
	
	protected AlarmingManager alarmMan = null;
	ResourceValueListener<BooleanResource> alarmingActiveListener = null;
	private final Map<String, AppID> appsToSend;
	public List<HardwareTablePage> mainPageExts = new ArrayList<>();
	
	public final QualityEvalUtil qualityEval;
	
	public static final Map<String, String> ALARM_APP_TYPE_EN = new HashMap<>();

	static {
		ALARM_APP_TYPE_EN.put(AlarmingUtiH.SP_SUPPORT_FIRST, AlarmingUtiH.SP_SUPPORT_FIRST);
		ALARM_APP_TYPE_EN.put(AlarmingUtiH.CUSTOMER_FIRST, AlarmingUtiH.CUSTOMER_FIRST);
		ALARM_APP_TYPE_EN.put(AlarmingUtiH.CUSTOMER_SP_SAME, AlarmingUtiH.CUSTOMER_SP_SAME);
	}
	protected void registerMessagingApp(String typeName, String suffix) {
		AppID appId;
		String idExt;
		if(suffix == null) {
			idExt = "_Alarming";
			appId = appMan.getAppID();
		} else {
			idExt = "_Alarming_"+suffix;
			appId = new AppIDImpl(appMan.getAppID(), suffix);
		}
		appManPlus.getMessagingService().registerMessagingApp(appId, getAlarmingDomain()+idExt, "Alarming: "+typeName);
		appsToSend.put(typeName, appId);		
	}
	
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

		@Override
		public String descriptionReceivers() {
			return " Die Absenderadresse kann konfiguriert werden: <a href=\""
					+  SENDER_LINK + "\"><b>Message Sender Page</b></a>.\n"
					+ "Alle Nachrichten die erschickt wurden können auch im Browser angesehen werden: "
					+ "<a href=\"" + MESSAGE_READER_LINK + "\"><b>Message Reader</b></a>.";
     	}
	}
	public static class MessageSettingsDictAlarming_en extends MessageSettingsDictionary_en {
		@Override
		public String headerReceivers() {
			return messageSettingsHeader();
		}
		
		@Override
		public String descriptionReceivers() {
			return "The sender address can be configured <a href=\""
					+  SENDER_LINK + "\"><b>on this page</b></a>.\n"
					+ "Messages sent can also be viewed in the browser: "
					+ "<a href=\"" + MESSAGE_READER_LINK + "\"><b>Message Reader</b></a>.";
     	}
	}
	
	public static String messageSettingsHeader() {
		if(Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway")) {
			if(Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.minimalview"))
				return "2. Message Receiver Configuration";
			else
				return "2. Message Receiver Configuration";
		} else
			return "1. Message Receiver Configuration";		
	}
	
	@SuppressWarnings("unchecked")
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
		
		this.appsToSend = new HashMap<String, AppID>();
		registerMessagingApp(AlarmingUtiH.SP_SUPPORT_FIRST, null);
		registerMessagingApp(AlarmingUtiH.CUSTOMER_FIRST, "CF");
		registerMessagingApp(AlarmingUtiH.CUSTOMER_SP_SAME, "SAM");

		tsProcAl = new TimeseriesProcAlarming(appMan, dpService);
		
		hwTableData = new HardwareTableData(appMan);
		cleanupAlarming();
		//initAlarmingResources();
		alarmingActiveListener = new ResourceValueListener<BooleanResource>() {

			@Override
			public void resourceChanged(BooleanResource resource) {
				updateAlarming();
			}
		};
		updateAlarming();
		hwTableData.appConfigData.isAlarmingActive().create().activate(false);
		if(Boolean.getBoolean("org.smartrplace.apps.hw.install.init.alarmtesting.forcestartalarming")) {
			//we set this true later on
			hwTableData.appConfigData.isAlarmingActive().setValue(false);
		}
		
		hwTableData.appConfigData.isAlarmingActive().addValueListener(alarmingActiveListener, false);
		
		MainPage.alarmingUpdater = this;
		HardwareInstallController.alarmingUpdater = this;

		if(alarmMan != null)
			srStarted = initApp.bc.registerService(AlarmingStartedService.class, alarmMan, null);
		else
			srStarted = initApp.bc.registerService(AlarmingStartedService.class, new AlarmingStartedService() {
				
				@Override
				public boolean isAlarmingStarted() {
					return false;
				}
			}, null);

		
		if(Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway")) {
			WidgetPage<?> pageRes12 = initApp.widgetApp.createWidgetPage("templateconfig.html", true); //initApp.widgetApp.createWidgetPage("devices.html");
			devicePage = new DeviceTypePage(pageRes12, appManPlus, true, this, false, false);
			initApp.menu.addEntry("1. Device Template Alarming Configuration", pageRes12);
			initApp.configMenuConfig(pageRes12.getMenuConfiguration());
	
			/*if(!Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.minimalview")) {
				WidgetPage<?> pageRes10 = initApp.widgetApp.createWidgetPage("mainpage.html");
				//Resource base = appMan.getResourceAccess().getResource("master");
				mainPage = new MainPage(pageRes10, appManPlus); //, base);
				initApp.menu.addEntry("3. Alarming Configuration Details", pageRes10);
				initApp.configMenuConfig(pageRes10.getMenuConfiguration());
			}*/
			isGw = true;
		}

		appList = appMan.getResourceManagement().createResource("messagingApps", ResourceList.class);
		appList.setElementType(MessagingApp.class);
		mr = initApp.mr;
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
		
		new CountDownDelayedExecutionTimer(appMan, 10000) {
			
			@Override
			public void delayedExecution() {
				WidgetPage<MessageSettingsDictionary> pageRes3 = initApp.widgetApp.createWidgetPage("receiver.html", !isGw);
				setupMessageReceiverConfiguration(initApp.mr, appList, pageRes3, false);
				initApp.menu.addEntry(messageSettingsHeader(), pageRes3);
				initApp.configMenuConfig(pageRes3.getMenuConfiguration());		
			}
		};
		
		if(Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.showFullAlarmiing")) {
			initApp.menu.addEntry("Battery State Alarming", "/de/iee/ogema/batterystatemonitoring/index.html");
			initApp.menu.addEntry("Window Open Alarming", "/de/iwes/ogema/apps/windowopeneddetector/index.html");
		}

		initDemands();
		
		qualityEval = new QualityEvalUtil(this);
	}

	public void setupMessageReceiverConfiguration(MessageReader mr, final ResourceList<MessagingApp> appList,
			WidgetPage<MessageSettingsDictionary> pageRes3, boolean showSuperAdmin) {
		de.iwes.widgets.messaging.MessagingApp app1 = null;
		de.iwes.widgets.messaging.MessagingApp app1_cf = null;
		de.iwes.widgets.messaging.MessagingApp app1_bt = null;
		for(de.iwes.widgets.messaging.MessagingApp mapp: mr.getMessageSenders()) {
			if(isGw) {
				/*if(mapp.getMessagingId().equals("DEV18410X_Alarming") || "Alarming Configuration".equals(mapp.getName())) {
					app1 = mapp;
					break;
				}*/
				if(mapp.getMessagingId().equals("DEV18410X_Alarming") || "Alarming: Smartrplace Support First".equals(mapp.getName())) {
					app1 = mapp;
				} else if(mapp.getMessagingId().equals("DEV18410X_Alarming") || "Alarming: Customer First".equals(mapp.getName())) {
					app1_cf = mapp;
				} else if(mapp.getMessagingId().equals("DEV18410X_Alarming") || "Alarming: Both together".equals(mapp.getName())) {
					app1_bt = mapp;
				}
			} else {
				if(mapp.getMessagingId().equals("DEV18410X_Alarming") || "Remote gateway heartbeat server".equals(mapp.getName())) {
					app1 = mapp;
					break;
				}				
			}
		}
		de.iwes.widgets.messaging.MessagingApp app = app1;
		de.iwes.widgets.messaging.MessagingApp app_cf = app1_cf;
		de.iwes.widgets.messaging.MessagingApp app_bt = app1_bt;
		pageRes3.registerLocalisation(MessageSettingsDictAlarming_de.class).
				registerLocalisation(MessageSettingsDictAlarming_en.class);
		receiverPage = new ReceiverPageBuilder(pageRes3, appMan) {
			
			@Override
			public void resourceAvailable(ReceiverConfiguration receiver) {
				if(!showSuperAdmin) {
					//String name = receiver.userName().getValue();
					//if(name != null && name.contains("Test Alarm"))
					//	return;
					String email = receiver.email().getValue();
					if(email != null && (email.contains("@smartrplace.de") || email.contains("@smartrplace.com")))
						return;
				}
				super.resourceAvailable(receiver);
			}
			
			@Override
			protected boolean showOnlyMainListeners() {
				return !showSuperAdmin;
			}
			
			@Override
			protected void addAdditionalColumns(Map<String, Object> receiverHeader) {
				if(isGw && showSuperAdmin) {
					receiverHeader.put("alarmingAppForwardingEmail_SF", "Level SP Support First:");		
					receiverHeader.put("alarmingAppForwardingEmail_CF", "Level Customer First:");		
					receiverHeader.put("alarmingAppForwardingEmail_BT", "Level Both together");
				} else if(isGw) {
					receiverHeader.put("alarmingAppForwardingEmail_SF", "Priority at least:");		
				} else {
					receiverHeader.put("alarmingAppForwardingEmail", "Alarm-level Email:");		
					receiverHeader.put("alarmingAppForwardingSMS", "Alarm-level SMS:");
				}
			}
			
			@Override
			protected void addAdditionalRowWidgets(ReceiverConfiguration config, String id, Row row,
					OgemaHttpRequest req) {
				if(config.getName().equals("testButtonReceiver")) {
					addTestButtonColumn(app, "alarmingAppForwardingEmail_SF", config, id, row, req);
					addTestButtonColumn(app_cf, "alarmingAppForwardingEmail_CF", config, id, row, req);
					addTestButtonColumn(app_bt, "alarmingAppForwardingEmail_BT", config, id, row, req);
					return;
				}
				if(isGw && showSuperAdmin) {
					addEmailConfigColumn(app, "alarmingAppForwardingEmail_SF", config, id, row, req);
					addEmailConfigColumn(app_cf, "alarmingAppForwardingEmail_CF", config, id, row, req);
					addEmailConfigColumn(app_bt, "alarmingAppForwardingEmail_BT", config, id, row, req);
					return;
				} else if(isGw) {
					addEmailConfigColumnDual(app_cf, app_bt, "alarmingAppForwardingEmail_SF", config, id, row, req);
					return;
				}
				if(app == null)
					return;
				String userName = config.userName().getValue();
				List<MessageListener> userListeners = PageInit.getListenersForUser(userName, mr);
				
				String messageListenerName = "Email-connector";
				MessageListener l = mr.getMessageListeners().get(messageListenerName);
				if(userListeners.contains(l)) {
					MessagePriorityDropdown alarmingPrioDrop = new MessagePriorityDropdown(pageRes3,
							WidgetHelper.getValidWidgetId("alarmingDrop"+id+userName+messageListenerName),
							l, userName, appList, app);
					row.addCell("alarmingAppForwardingEmail", alarmingPrioDrop);
				}

				messageListenerName = "Sms-connector";
				l = mr.getMessageListeners().get(messageListenerName);
				if(userListeners.contains(l)) {
					MessagePriorityDropdown alarmingPrioDropSMS = new MessagePriorityDropdown(pageRes3,
							WidgetHelper.getValidWidgetId("alarmingDrop"+id+userName+messageListenerName),
							l, userName, appList, app);
					row.addCell("alarmingAppForwardingSMS", alarmingPrioDropSMS);
				}
			}
			
			protected void addEmailConfigColumn(de.iwes.widgets.messaging.MessagingApp appLoc, String col,
					ReceiverConfiguration config, String id, Row row,
					OgemaHttpRequest req) {
				if(appLoc == null)
					return;
				String userName = config.userName().getValue();
				List<MessageListener> userListeners = PageInit.getListenersForUser(userName, mr);
				
				String messageListenerName = "Email-connector";
				MessageListener l = mr.getMessageListeners().get(messageListenerName);
				if(userListeners.contains(l)) {
					MessagePriorityDropdown alarmingPrioDrop = new MessagePriorityDropdown(pageRes3,
							WidgetHelper.getValidWidgetId("alarmingDrop"+col+id+userName+messageListenerName),
							l, userName, appList, appLoc);
					row.addCell(col, alarmingPrioDrop);
				}
				
			}
			
			protected void addEmailConfigColumnDual(de.iwes.widgets.messaging.MessagingApp appLoc1,
					de.iwes.widgets.messaging.MessagingApp appLoc2,
					String col,
					ReceiverConfiguration config, String id, Row row,
					OgemaHttpRequest req) {
				if(appLoc1 == null)
					return;
				String userName = config.userName().getValue();
				List<MessageListener> userListeners = PageInit.getListenersForUser(userName, mr);
				
				String messageListenerName = "Email-connector";
				MessageListener l = mr.getMessageListeners().get(messageListenerName);
				if(userListeners.contains(l)) {
					MessagePriorityDropdown alarmingPrioDrop = new MessagePriorityDropdown(pageRes3,
							WidgetHelper.getValidWidgetId("alarmingDrop"+col+id+userName+messageListenerName),
							l, userName, appList, new de.iwes.widgets.messaging.MessagingApp[] {appLoc1, appLoc2});
					row.addCell(col, alarmingPrioDrop);
				}
				
			}

			protected void addTestButtonColumn(de.iwes.widgets.messaging.MessagingApp appLoc, String col,
					ReceiverConfiguration config, String id, Row row,
					OgemaHttpRequest req) {
				if(appLoc == null)
					return;
				
				Button testButton = new Button(pageRes3, WidgetHelper.getValidWidgetId("testButton"+col+id), "Test Alarm") {
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						VirtualTestDevice testDevice = ResourceHelper.getSampleResource(VirtualTestDevice.class);
						FloatResource reading;
						if(col.endsWith("_SF"))
							reading = testDevice.sensor_SF().reading();
						else if(col.endsWith("_CF"))
							reading = testDevice.sensor_CF().reading();
						else
							reading = testDevice.sensor_BT().reading();
						reading.setValue(200);
						new CountDownDelayedExecutionTimer(appMan, 2*TimeProcUtil.MINUTE_MILLIS) {
							
							@Override
							public void delayedExecution() {
								reading.setValue(2);							
							}
						};
					}
				};
				testButton.addDefaultStyle(ButtonData.BOOTSTRAP_GREEN);
				row.addCell(col, testButton);
				
			}

		};
		ReceiverConfiguration testButtonReceiver = ResourceHelper.getEvalCollection(appMan).
				getSubResource("testButtonReceiver", ReceiverConfiguration.class);
	    testButtonReceiver.deactivate(true);
	    testButtonReceiver.userName().create();
	    testButtonReceiver.userName().setValue("Test Alarm");
		receiverPage.receiverTable.addItem(testButtonReceiver, null);
		appMan.getResourceAccess().addResourceDemand(ReceiverConfiguration.class, receiverPage);
	}
	
	/*
     * register ResourcePatternDemands. The listeners will be informed about new and disappearing
     * patterns in the OGEMA resource tree
     */
    public void initDemands() {
    }

	public void close() {
    	if (srStarted != null) srStarted.unregister();
		if(alarmMan != null) {
			alarmMan.close();
			alarmMan = null;
		}
		if(qualityEval != null) {
			qualityEval.close();
		}
	}

	public void cleanupAlarming() {
		if(hwTableData.appConfigData == null)
			return;
		for(InstallAppDevice dev: hwTableData.appConfigData.knownDevices().getAllElements()) {
			cleanupAlarming(dev);
		}
		ValueResourceHelper.setIfNew(hwTableData.appConfigData.basicEvalInterval(), 7*TimeProcUtil.DAY_MILLIS);
	}
	
	public void cleanupAlarming(InstallAppDevice dev) {
		Set<String> knownSensors = new HashSet<>();
		for(AlarmConfiguration ac: dev.alarms().getAllElements()) {
			if(knownSensors.contains(ac.sensorVal().getLocation())) {
				log.warn(" Found double alarming entry for "+ac.sensorVal().getLocation()+ " in "+ac.getLocation());
				ac.delete();
			} else if(!ac.sensorVal().exists()) {
				log.warn(" Found empty alarming entry for "+ac.sensorVal().getLocation()+ " in "+ac.getLocation());
				ac.delete();
			} else {
				knownSensors.add(ac.sensorVal().getLocation());
				//if(ValueResourceHelper.setIfNew(ac.alarmingAppId(), SP_SUPPORT_FIRST))
				//	ac.alarmingAppId().activate(false);
			}
		}
	}
	
	@Override
	public void updateAlarming() {
		if(alarmMan != null) {
			alarmMan.close();
		}
		if(hwTableData.appConfigData.isAlarmingActive().getValue()) {
			List<InstallAppDevice> iads = hwTableData.appConfigData.knownDevices().getAllElements();
			//List<AlarmConfiguration> configs = appMan.getResourceAccess().getResources(AlarmConfiguration.class);
			alarmMan = new AlarmingManager(iads, appManPlus, appsToSend, getAlarmingDomain());
		} else
			alarmMan = null;
	}
	
	volatile boolean allowRestartWithNewCall = true;
	volatile CountDownDelayedExecutionTimer alUpdTimer = null;
	@Override
	public void updateAlarming(long maximumRetard, boolean restartWithNewCall) {
		synchronized (this) {
			if(alUpdTimer == null) {
				initAlUpdTimer(maximumRetard);
				allowRestartWithNewCall = restartWithNewCall;
			} else if(allowRestartWithNewCall)
				allowRestartWithNewCall = restartWithNewCall;
			if(allowRestartWithNewCall) {
				alUpdTimer.destroy();
				initAlUpdTimer(maximumRetard);
			}
		}
		
	}
	private void initAlUpdTimer(long maximumRetard) {
		alUpdTimer = new CountDownDelayedExecutionTimer(appMan, maximumRetard) {
			
			@Override
			public void delayedExecution() {
				updateAlarming();
				alUpdTimer = null;
			}
		};
	}
	
	public DeviceHandlerProvider<?> getDeviceHandler(InstallAppDevice appDevice) {
		return getDeviceHandler(appDevice.devHandlerInfo().getValue());
	}
	public DeviceHandlerProvider<?> getDeviceHandler(String id) {
		return accessAdminApp.getTableProviders().get(id);
	}
}
