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
import org.ogema.devicefinder.service.DatapointServiceImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.smartrplace.devicetable.DeviceHandlerDoorWindowSensor;
import org.smartrplace.devicetable.DeviceHandlerThermostat;
import org.smartrplace.driverhandler.devices.DeviceHandler_PVPlant;
import org.smartrplace.driverhandler.devices.DriverHandlerJMBus;
import org.smartrplace.driverhandler.devices.DriverHandlerKNX_IP;
import org.smartrplace.driverhandler.devices.DriverHandlerMQTTBroker;
import org.smartrplace.driverhandler.more.DeviceHandlerDpRes;
import org.smartrplace.mqtt.devicetable.DeviceHandlerMQTT_Aircond;
import org.smartrplace.mqtt.devicetable.DeviceHandlerMQTT_ElecConnBox;
import org.smartrplace.mqtt.devicetable.DeviceHandlerMQTT_MultiSwBox;

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
	private DeviceHandlerMQTT_MultiSwBox devHandSwBox;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srDoorWindowSensor = null;
	private DeviceHandlerDoorWindowSensor devHandDoorWindowSensor;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srThermostat = null;
	private DeviceHandlerThermostat devHandThermostat;
	
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srVirtDpRes = null;
	private DeviceHandlerDpRes devVirtDpRes;

	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srPv = null;
	private DeviceHandler_PVPlant devHandPv;

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
		   
	   };
	   controller = new MonitoringServiceBaseController(appMan, this);
	   
	   srDpservice = bc.registerService(DatapointService.class, dpService, null);
	   
	   devHandAircond = new DeviceHandlerMQTT_Aircond(appMan);
	   srAircond = bc.registerService(DeviceHandlerProvider.class, devHandAircond, null);
	   devHandElecConn = new DeviceHandlerMQTT_ElecConnBox(appMan);
	   srElecConn = bc.registerService(DeviceHandlerProvider.class, devHandElecConn, null);
	   devHandSwBox = new DeviceHandlerMQTT_MultiSwBox(appMan);
	   srSwBox = bc.registerService(DeviceHandlerProvider.class, devHandSwBox, null);

	   devHandThermostat = new DeviceHandlerThermostat(appMan);
	   srThermostat = bc.registerService(DeviceHandlerProvider.class, devHandThermostat, null);

	   devHandDoorWindowSensor = new DeviceHandlerDoorWindowSensor(appMan);
	   srDoorWindowSensor = bc.registerService(DeviceHandlerProvider.class, devHandDoorWindowSensor, null);
	   
	   devVirtDpRes = new DeviceHandlerDpRes(appMan);
	   srVirtDpRes = bc.registerService(DeviceHandlerProvider.class, devVirtDpRes, null);

	   devHandPv = new DeviceHandler_PVPlant(appMan);
	   srPv = bc.registerService(DeviceHandlerProvider.class, devHandPv, null);

	   jmbusConfig = new DriverHandlerJMBus(appManager, configAdmin);
	   jmbusDriver = bc.registerService(DriverHandlerProvider.class, jmbusConfig, null);
	   mqttBrokerConfig = new DriverHandlerMQTTBroker(appManager, configAdmin);
	   mqttBrokerDriver = bc.registerService(DriverHandlerProvider.class, mqttBrokerConfig, null);
	   knxConfig = new DriverHandlerKNX_IP(appManager, configAdmin);
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
    	if (srDoorWindowSensor != null) srDoorWindowSensor.unregister();
    	if (srThermostat != null) srThermostat.unregister();

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
}
