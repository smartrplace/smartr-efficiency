package org.smartrplace.apps.alarmingconfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.messaging.basic.services.config.ReceiverPageBuilder;
import org.ogema.messaging.basic.services.config.localisation.MessageSettingsDictionary;
import org.ogema.messaging.basic.services.config.localisation.MessageSettingsDictionary_de;
import org.ogema.messaging.basic.services.config.localisation.MessageSettingsDictionary_en;
import org.ogema.messaging.basic.services.config.model.ReceiverConfiguration;
import org.ogema.messaging.configuration.PageInit;
import org.ogema.messaging.configuration.localisation.SelectConnectorDictionary;
import org.ogema.model.sensors.Sensor;
import org.smartrplace.app.monbase.RoomLabelProvider;
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

import com.iee.app.evaluationofflinecontrol.util.ExportBulkData;

import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.messaging.model.MessagingApp;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;
import extensionmodel.smarteff.monitoring.AlarmConfigBase;

// here the controller logic is implemented
public class AlarmingConfigAppController implements AlarmingUpdater, RoomLabelProvider {
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
	public final PageBuilderSimple messagePage;
	public final PageInit forwardingPage;
	public final ReceiverPageBuilder receiverPage;
	WidgetApp widgetApp;

	protected AlarmingManager alarmMan = null;
	
	public String getAlarmingDomain() {
		return "SRCA";
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
		
		initDemands();
		
		appManPlus.getMessagingService().registerMessagingApp(appMan.getAppID(), getAlarmingDomain()+"_Alarming");

		initAlarmingResources();
		updateAlarming();
		MainPage.alarmingUpdater = this;

		WidgetPage<?> pageRes10 = initApp.widgetApp.createWidgetPage("mainpage.html", true);
		Resource base = appMan.getResourceAccess().getResource("master");
		mainPage = new MainPage(pageRes10, appMan, base);
		initApp.menu.addEntry("Room Setup", pageRes10);
		initApp.configMenuConfig(pageRes10.getMenuConfiguration());

		WidgetPage<MessagesDictionary> pageRes11 = initApp.widgetApp.createWidgetPage("messages.html", false);
		pageRes11.registerLocalisation(MessagesDictionary_en.class).registerLocalisation(MessagesDictionary_de.class).registerLocalisation(MessagesDictionary_fr.class);
		messagePage = new PageBuilderSimple(pageRes11, initApp.mr, appMan);
		initApp.menu.addEntry("Alarm Messages", pageRes11);
		initApp.configMenuConfig(pageRes11.getMenuConfiguration());
		
		WidgetPage<SelectConnectorDictionary> pageRes2 = initApp.widgetApp.createWidgetPage("forwarding.html", false);
		@SuppressWarnings("unchecked")
		final ResourceList<MessagingApp> appList = appMan.getResourceManagement().createResource("messagingApps", ResourceList.class);
		appList.setElementType(MessagingApp.class);
		forwardingPage = new PageInit(pageRes2, appMan, appList, initApp.mr);
		initApp.menu.addEntry("Message Forwarding Configuration", pageRes2);
		initApp.configMenuConfig(pageRes2.getMenuConfiguration());

		WidgetPage<MessageSettingsDictionary> pageRes3 = initApp.widgetApp.createWidgetPage("receiver.html", false);
		pageRes3.registerLocalisation(MessageSettingsDictionary_de.class).registerLocalisation(MessageSettingsDictionary_en.class);
		receiverPage = new ReceiverPageBuilder(pageRes3, appMan);
		appMan.getResourceAccess().addResourceDemand(ReceiverConfiguration.class, receiverPage);
		initApp.menu.addEntry("Message Receiver Configuration", pageRes3);
		initApp.configMenuConfig(pageRes3.getMenuConfiguration());
	}

     protected void initAlarmingResources() {
 		SmartEffUserDataNonEdit user = appMan.getResourceAccess().getResource("master");
 		if(user == null) return;
 		
 		List<String> done = new ArrayList<>();
 		Map<String, List<String>> roomSensors = new HashMap<>();
 		for(Sensor sens: appMan.getResourceAccess().getResources(Sensor.class)) {
 			if(sens.getLocation().contains("valve/connection/powerSensor"))
 				continue;
 			ValueResource vr = sens.reading();
 			if(!(vr instanceof SingleValueResource))
 				continue;
 			SingleValueResource reading = (SingleValueResource) vr;
			AlarmingManager.initValueResourceAlarming(reading, roomSensors, done, this, appMan);
 		}
 		AlarmingManager.finishInitSensors(user, roomSensors, done, appMan);
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

	@Override
	public void updateAlarming() {
		if(alarmMan != null) {
			alarmMan.close();
		}
		List<AlarmConfigBase> configs = appMan.getResourceAccess().getResources(AlarmConfigBase.class);
		alarmMan = new AlarmingManager(configs, appManPlus, this, getAlarmingDomain());
	}

	@Override
	public String getTsName(AlarmConfigBase ac) {
		return RoomLabelProvider.getTsNameDefault(ac);
	}

	@Override
	public String getRoomLabel(String resLocation, OgemaLocale locale) {
		Resource res = appMan.getResourceAccess().getResource(resLocation);
		if(res == null)
			return null;
		//TODO: handle res==null
		String result = RoomLabelProvider.getDatapointShortLabelDefault(res, false, this);
		if(result == null) {
			return ExportBulkData.getDeviceShortId(resLocation);
		} else
			return result;
	}

	@Override
	public String getLabel(AlarmConfigBase ac, boolean isOverall) {
		Datapoint dp = dpService.getDataPointStandard(ac.supervisedSensor().reading());
		return dp.label(null);
		//String shortLab = RoomLabelProvider.getDatapointShortLabelDefault(ac.supervisedSensor().getLocationResource(), false, this);
		//return shortLab+"-Temperature";
	}
}
