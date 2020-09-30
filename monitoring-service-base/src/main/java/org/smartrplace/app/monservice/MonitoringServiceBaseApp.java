package org.smartrplace.app.monservice;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DriverHandlerProvider;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.ogema.devicefinder.service.DatapointServiceImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.smartrplace.driverhandler.devices.DeviceHandler_PVPlant;
import org.smartrplace.driverhandler.devices.DriverHandlerJMBus;
import org.smartrplace.driverhandler.devices.DriverHandlerKNX_IP;
import org.smartrplace.driverhandler.devices.DriverHandlerMQTTBroker;
import org.smartrplace.driverhandler.more.BacnetDeviceHandler;
import org.smartrplace.driverhandler.more.DeviceHandlerDpRes;
import org.smartrplace.driverhandler.more.GhlWaterPondDeviceHandler;
import org.smartrplace.homematic.devicetable.DeviceHandlerDoorWindowSensor;
import org.smartrplace.homematic.devicetable.DeviceHandlerThermostat;
import org.smartrplace.homematic.devicetable.TemperatureOrHumiditySensorDeviceHandler;
import org.smartrplace.mqtt.devicetable.DeviceHandlerMQTT_Aircond;
import org.smartrplace.mqtt.devicetable.DeviceHandlerMQTT_ElecConnBox;
import org.smartrplace.mqtt.devicetable.DeviceHandlerMQTT_SingleSwBox;
import org.smartrplace.mqtt.devicetable.DeviceHandlerMQTT_SmartDimmer;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;  

@References({
	@Reference(
		name="tableProviders",
		referenceInterface=DeviceHandlerProvider.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
		bind="addTableProvider",
		unbind="removeTableProvider"),
	@Reference(
		name="propertyServices",
		referenceInterface=OGEMADriverPropertyService.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
		bind="addDriverPropertyProvider",
		unbind="removeDriverPropertyProvider")
})

@Component(specVersion = "1.2", immediate = true)
@Service(Application.class)
public class MonitoringServiceBaseApp implements Application {
	//public static final String urlPath = "/org/sp/app/monapp";
	//public static final String urlPathServlet = "/org/sp/app/monappserv";
	//public static WidgetPageFormatter STANDARD_PAGE_FORMATTER = new WidgetPageFormatter();

    private OgemaLogger log;
    private ApplicationManager appMan;
    private MonitoringServiceBaseController controller;

	public WidgetApp widgetApp;
	public NavigationMenu menu;

	@Reference
	OgemaGuiService guiService;
	
	@Reference
	ConfigurationAdmin configAdmin;
	
	//@Reference
	//DatapointService dpService;
	
	private final Map<String, DeviceHandlerProvider<?>> tableProviders = Collections.synchronizedMap(new LinkedHashMap<String,DeviceHandlerProvider<?>>());
	public Map<String, DeviceHandlerProvider<?>> getTableProviders() {
		synchronized (tableProviders) {
			return new LinkedHashMap<>(tableProviders);
		}
	}

	private final Map<String,OGEMADriverPropertyService<?>> dPropertyProviders = Collections.synchronizedMap(new LinkedHashMap<String,OGEMADriverPropertyService<?>>());
	public LinkedHashMap<String, OGEMADriverPropertyService<?>> getDPropertyProviders() {
		synchronized (dPropertyProviders) {
			return new LinkedHashMap<>(dPropertyProviders);
		}
	}

	private BundleContext bc;
	protected ServiceRegistration<DatapointService> srDpservice = null;
	DatapointServiceImpl dpService;

	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srAircond = null;
	private DeviceHandlerMQTT_Aircond devHandAircond;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srElecConn = null;
	private DeviceHandlerMQTT_ElecConnBox devHandElecConn;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srSwBox = null;
	private DeviceHandlerMQTT_SingleSwBox devHandSwBox;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srSmartDim = null;
	private DeviceHandlerMQTT_SmartDimmer devHandSmartDim;

	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srDoorWindowSensor = null;
	private DeviceHandlerDoorWindowSensor devHandDoorWindowSensor;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srThermostat = null;
	private DeviceHandlerThermostat devHandThermostat;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srTempHumSens = null;
	private TemperatureOrHumiditySensorDeviceHandler devHandTempHumSens;
	
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srVirtDpRes = null;
	private DeviceHandlerDpRes devVirtDpRes;

	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srPv = null;
	private DeviceHandler_PVPlant devHandPv;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srGhl = null;
	private GhlWaterPondDeviceHandler devHandGhl;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srBacnet = null;
	private BacnetDeviceHandler devHandBacnet;

	protected ServiceRegistration<DriverHandlerProvider> jmbusDriver = null;
	private DriverHandlerJMBus jmbusConfig;
	protected ServiceRegistration<DriverHandlerProvider> mqttBrokerDriver = null;
	private DriverHandlerMQTTBroker mqttBrokerConfig;
	protected ServiceRegistration<DriverHandlerProvider> knxDriver = null;
	private DriverHandlerKNX_IP knxConfig;
	


	
	@Activate
	   void activate(BundleContext bc) {
	    this.bc = bc;
	 }
	
    /*
     * This is the entry point to the application.
     */
 	@Override
    public void start(ApplicationManager appManager) {

        // Remember framework references for later.
        appMan = appManager;
        log = appManager.getLogger();

        // 
        dpService = new DatapointServiceImpl(appMan) {

			@Override
			protected Map<String, DeviceHandlerProvider<?>> getTableProviders() {
				return MonitoringServiceBaseApp.this.getTableProviders();
			}
			
			@Override
			public Map<String, OGEMADriverPropertyService<?>> driverpropertyServices() {
				return MonitoringServiceBaseApp.this.getDPropertyProviders();
			}
		   
	   };
	   controller = new MonitoringServiceBaseController(appMan, this);
	   
	   srDpservice = bc.registerService(DatapointService.class, dpService, null);
	   
	   devHandAircond = new DeviceHandlerMQTT_Aircond(controller.appManPlus);
	   srAircond = bc.registerService(DeviceHandlerProvider.class, devHandAircond, null);
	   devHandElecConn = new DeviceHandlerMQTT_ElecConnBox(controller.appManPlus);
	   srElecConn = bc.registerService(DeviceHandlerProvider.class, devHandElecConn, null);
	   devHandSwBox = new DeviceHandlerMQTT_SingleSwBox(controller.appManPlus);
	   srSwBox = bc.registerService(DeviceHandlerProvider.class, devHandSwBox, null);
	   devHandSmartDim = new DeviceHandlerMQTT_SmartDimmer(controller.appManPlus);
	   srSmartDim = bc.registerService(DeviceHandlerProvider.class, devHandSmartDim, null);

	   devHandThermostat = new DeviceHandlerThermostat(controller.appManPlus);
	   srThermostat = bc.registerService(DeviceHandlerProvider.class, devHandThermostat, null);
	   
	   devHandTempHumSens = new TemperatureOrHumiditySensorDeviceHandler(controller.appManPlus);
	   srTempHumSens = bc.registerService(DeviceHandlerProvider.class, devHandTempHumSens, null);

	   devHandDoorWindowSensor = new DeviceHandlerDoorWindowSensor(controller.appManPlus);
	   srDoorWindowSensor = bc.registerService(DeviceHandlerProvider.class, devHandDoorWindowSensor, null);
	   
	   devVirtDpRes = new DeviceHandlerDpRes(controller.appManPlus);
	   srVirtDpRes = bc.registerService(DeviceHandlerProvider.class, devVirtDpRes, null);

	   devHandPv = new DeviceHandler_PVPlant(controller.appManPlus);
	   srPv = bc.registerService(DeviceHandlerProvider.class, devHandPv, null);
	   devHandGhl = new GhlWaterPondDeviceHandler(controller.appManPlus);
	   srGhl = bc.registerService(DeviceHandlerProvider.class, devHandGhl, null);
	   devHandBacnet = new BacnetDeviceHandler(controller.appManPlus);
	   srBacnet = bc.registerService(DeviceHandlerProvider.class, devHandBacnet, null);

	   jmbusConfig = new DriverHandlerJMBus(controller.appManPlus, configAdmin);
	   jmbusDriver = bc.registerService(DriverHandlerProvider.class, jmbusConfig, null);
	   mqttBrokerConfig = new DriverHandlerMQTTBroker(controller.appManPlus, configAdmin);
	   mqttBrokerDriver = bc.registerService(DriverHandlerProvider.class, mqttBrokerConfig, null);
	   knxConfig = new DriverHandlerKNX_IP(controller.appManPlus, configAdmin);
	   knxDriver = bc.registerService(DriverHandlerProvider.class, knxConfig, null);
	}
 	
     /*
     * Callback called when the application is going to be stopped.
     */
    @Override
    public void stop(AppStopReason reason) {
    	if (widgetApp != null) widgetApp.close();

    	if (srDpservice != null) srDpservice.unregister();
    	
    	if (srAircond != null) srAircond.unregister();
    	if (srElecConn != null) srElecConn.unregister();
    	if (srSwBox != null) srSwBox.unregister();
    	if (srPv != null) srPv.unregister();
    	if (srGhl != null) srGhl.unregister();
    	if (srBacnet != null) srGhl.unregister();
    	if (srVirtDpRes != null) srVirtDpRes.unregister();
    	if (srDoorWindowSensor != null) srDoorWindowSensor.unregister();
    	if (srThermostat != null) srThermostat.unregister();
    	if (srTempHumSens != null) srTempHumSens.unregister();
    	
    	if (jmbusDriver != null) jmbusDriver.unregister();
    	if (mqttBrokerDriver != null) mqttBrokerDriver.unregister();
    	if (knxDriver != null) knxDriver.unregister();

		if (controller != null)
    		controller.close();
        log.info("{} stopped", getClass().getName());
    }
    
    protected void addTableProvider(DeviceHandlerProvider<?> provider) {
    	tableProviders.put(provider.id(), provider);
     }
    protected void removeTableProvider(DeviceHandlerProvider<?> provider) {
    	tableProviders.remove(provider.id());
    }
    
    protected void addDriverPropertyProvider(OGEMADriverPropertyService<?>  provider) {
    	dPropertyProviders.put(provider.id(), provider);
    }
    protected void removeDriverPropertyProvider(OGEMADriverPropertyService<?> provider) {
    	dPropertyProviders.remove(provider.id());
    }

}
