package org.smartrplace.app.monservice;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferencePolicyOption;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DriverHandlerProvider;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.ogema.devicefinder.api.TimedJobMgmtService;
import org.ogema.devicefinder.service.DatapointServiceImpl;
import org.ogema.messaging.api.MessageTransport;
import org.ogema.recordeddata.DataRecorder;
import org.ogema.tools.app.useradmin.api.UserDataAccess;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.smartrplace.driverhandler.devices.BatteryDevHandler;
import org.smartrplace.driverhandler.devices.BluetoothBeaconHandler;
import org.smartrplace.driverhandler.devices.ChargingPointDevHandler;
import org.smartrplace.driverhandler.devices.DeviceHandler_PVPlant;
import org.smartrplace.driverhandler.devices.DriverHandlerJMBus;
import org.smartrplace.driverhandler.devices.DriverHandlerKNX_IP;
import org.smartrplace.driverhandler.devices.DriverHandlerMQTTBroker;
import org.smartrplace.driverhandler.devices.ESE_ElConnBoxDeviceHandler;
import org.smartrplace.driverhandler.devices.FlowScopeDevHandler;
import org.smartrplace.driverhandler.devices.GasEnergyCam_DeviceHandler;
import org.smartrplace.driverhandler.devices.HMIEC_ElConnDeviceHandler;
import org.smartrplace.driverhandler.devices.HeatMeter_DeviceHandler;
import org.smartrplace.driverhandler.devices.HeatingLabFlowSens_DeviceHandler;
import org.smartrplace.driverhandler.devices.HeatingLabTempSens_DeviceHandler;
import org.smartrplace.driverhandler.devices.HeatingLabThermalValve_DeviceHandler;
import org.smartrplace.driverhandler.devices.IotawattSimple_DeviceHandler;
import org.smartrplace.driverhandler.devices.Iotawatt_DeviceHandler;
import org.smartrplace.driverhandler.devices.LightWLANDevHandler;
import org.smartrplace.driverhandler.devices.MultiSwitchHandler;
import org.smartrplace.driverhandler.devices.OpenWeatherMapBigBlueRoom_DeviceHandler;
import org.smartrplace.driverhandler.devices.SmartProtect_DeviceHandler;
import org.smartrplace.driverhandler.devices.WaterMeter_DeviceHandler;
import org.smartrplace.homematic.devicetable.CO2SensorHmHandler;
import org.smartrplace.homematic.devicetable.DeviceHandlerDoorWindowSensor;
import org.smartrplace.homematic.devicetable.DeviceHandlerThermostat;
import org.smartrplace.homematic.devicetable.MotionSensorHandler;
import org.smartrplace.homematic.devicetable.OnOffSwitch_DeviceHandler;
import org.smartrplace.homematic.devicetable.SmokeDetector_DeviceHandler;
import org.smartrplace.homematic.devicetable.TemperatureOrHumiditySensorDeviceHandler;
import org.smartrplace.homematic.devicetable.WallThermostatHandler;
import org.smartrplace.homematic.devicetable.WeatherStation_DeviceHandler;
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
        policyOption = ReferencePolicyOption.GREEDY,
		bind="addTableProvider",
		unbind="removeTableProvider"),
	@Reference(
		name="propertyServices",
		referenceInterface=OGEMADriverPropertyService.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY,
		bind="addDriverPropertyProvider",
		unbind="removeDriverPropertyProvider"),
	@Reference(
		name="userServices",
		referenceInterface=UserDataAccess.class,
		cardinality=ReferenceCardinality.OPTIONAL_UNARY,
		policy=ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY,
		bind="addUserDataService",
		unbind="removeUserDataService"),
	@Reference(
		name="messageServices",
		referenceInterface=MessageTransport.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY,
		bind="addMessageTransportProvider",
		unbind="removeMessageTransportProvider"),
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
	
    @Reference
    DataRecorder dataRecorder;

    @Reference
    TimedJobMgmtService timedJobApp;
    
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

	private UserDataAccess userDataAccess = null;
	public UserDataAccess getUserDataAccess() {
    	return userDataAccess;
	}

	private final Map<String,MessageTransport> messageTransports = Collections.synchronizedMap(new LinkedHashMap<String,MessageTransport>());
	public LinkedHashMap<String, MessageTransport> getMessageTransportProviders() {
		synchronized (messageTransports) {
			return new LinkedHashMap<>(messageTransports);
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
	protected ServiceRegistration<DeviceHandlerProvider> srOnOff = null;
	private OnOffSwitch_DeviceHandler devHandOnOff;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srSmartDim = null;
	private DeviceHandlerMQTT_SmartDimmer devHandSmartDim;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srLight = null;
	private LightWLANDevHandler devHandLight;

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
	protected ServiceRegistration<DeviceHandlerProvider> srSmoke = null;
	private SmokeDetector_DeviceHandler devHandSmoke;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srWeather = null;
	private WeatherStation_DeviceHandler devHandWeather;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srOpenWeather = null;
	private OpenWeatherMapBigBlueRoom_DeviceHandler devHandOpenWeather;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srMotion = null;
	private MotionSensorHandler devHandMotion;
	
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srPv = null;
	private DeviceHandler_PVPlant devHandPv;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srEnServ = null;
	private ESE_ElConnBoxDeviceHandler devHandEnServ;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srCharge = null;
	private ChargingPointDevHandler devHandCharge;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srBat = null;
	private BatteryDevHandler devHandBat;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srSmartProtect = null;
	private SmartProtect_DeviceHandler devHandSmartProtect;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srWater = null;
	private WaterMeter_DeviceHandler devHandWater;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srHeat = null;
	private HeatMeter_DeviceHandler devHandHeat;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srGas = null;
	private GasEnergyCam_DeviceHandler devHandGas;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srIota = null;
	private Iotawatt_DeviceHandler devHandIota;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srIotaSimple = null;
	private IotawattSimple_DeviceHandler devHandIotaSimple;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srFlowProbe = null;
	private FlowScopeDevHandler devHandFlowProbe;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srFlowSensSingle = null;
	private HeatingLabFlowSens_DeviceHandler devHandFlowSensSingle;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srTempSensSingle = null;
	private HeatingLabTempSens_DeviceHandler devHandTempSensSingle;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srThValve = null;
	private HeatingLabThermalValve_DeviceHandler devHandThValve;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srWall = null;
	private WallThermostatHandler devHandWall;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srCO2Hm = null;
	private CO2SensorHmHandler devHandCO2Hm;

	protected ServiceRegistration<DriverHandlerProvider> jmbusDriver = null;
	private DriverHandlerJMBus jmbusConfig;
	protected ServiceRegistration<DriverHandlerProvider> mqttBrokerDriver = null;
	private DriverHandlerMQTTBroker mqttBrokerConfig;
	protected ServiceRegistration<DriverHandlerProvider> knxDriver = null;
	private DriverHandlerKNX_IP knxConfig;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srBeacon = null;
	private BluetoothBeaconHandler devHandBeacon;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srMSwitch = null;
	private MultiSwitchHandler devHandMSwitch;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DeviceHandlerProvider> srIECMeter = null;
	private HMIEC_ElConnDeviceHandler devHandIECMeter;
	
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
        dpService = new DatapointServiceImpl(appMan, configAdmin, timedJobApp, this) {

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
	   devHandOnOff = new OnOffSwitch_DeviceHandler(controller.appManPlus);
	   srOnOff = bc.registerService(DeviceHandlerProvider.class, devHandOnOff, null);
	   devHandSmartDim = new DeviceHandlerMQTT_SmartDimmer(controller.appManPlus);
	   srSmartDim = bc.registerService(DeviceHandlerProvider.class, devHandSmartDim, null);
	   devHandLight = new LightWLANDevHandler(controller.appManPlus);
	   srLight = bc.registerService(DeviceHandlerProvider.class, devHandLight, null);

	   devHandThermostat = new DeviceHandlerThermostat(controller.appManPlus);
	   srThermostat = bc.registerService(DeviceHandlerProvider.class, devHandThermostat, null);
	   
	   devHandTempHumSens = new TemperatureOrHumiditySensorDeviceHandler(controller.appManPlus);
	   srTempHumSens = bc.registerService(DeviceHandlerProvider.class, devHandTempHumSens, null);

	   devHandDoorWindowSensor = new DeviceHandlerDoorWindowSensor(controller.appManPlus);
	   srDoorWindowSensor = bc.registerService(DeviceHandlerProvider.class, devHandDoorWindowSensor, null);
	   devHandSmoke = new SmokeDetector_DeviceHandler(controller.appManPlus);
	   srSmoke = bc.registerService(DeviceHandlerProvider.class, devHandSmoke, null);
	   devHandWeather = new WeatherStation_DeviceHandler(controller.appManPlus);
	   srWeather = bc.registerService(DeviceHandlerProvider.class, devHandWeather, null);
	   devHandOpenWeather = new OpenWeatherMapBigBlueRoom_DeviceHandler(controller.appManPlus);
	   srOpenWeather = bc.registerService(DeviceHandlerProvider.class, devHandOpenWeather, null);
	   devHandMotion = new MotionSensorHandler(controller.appManPlus);
	   srMotion = bc.registerService(DeviceHandlerProvider.class, devHandMotion, null);
	   
	   devHandPv = new DeviceHandler_PVPlant(controller.appManPlus);
	   srPv = bc.registerService(DeviceHandlerProvider.class, devHandPv, null);
	   devHandEnServ = new ESE_ElConnBoxDeviceHandler(controller.appManPlus);
	   srEnServ = bc.registerService(DeviceHandlerProvider.class, devHandEnServ, null);
	   devHandCharge = new ChargingPointDevHandler(controller.appManPlus);
	   srCharge = bc.registerService(DeviceHandlerProvider.class, devHandCharge, null);
	   devHandBat = new BatteryDevHandler(controller.appManPlus);
	   srBat = bc.registerService(DeviceHandlerProvider.class, devHandBat, null);
	   devHandSmartProtect = new SmartProtect_DeviceHandler(controller.appManPlus);
	   srSmartProtect = bc.registerService(DeviceHandlerProvider.class, devHandSmartProtect, null);
	   devHandWater = new WaterMeter_DeviceHandler(controller.appManPlus);
	   srWater = bc.registerService(DeviceHandlerProvider.class, devHandWater, null);
	   devHandHeat = new HeatMeter_DeviceHandler(controller.appManPlus);
	   srHeat = bc.registerService(DeviceHandlerProvider.class, devHandHeat, null);
	   devHandGas = new GasEnergyCam_DeviceHandler(controller.appManPlus);
	   srGas = bc.registerService(DeviceHandlerProvider.class, devHandGas, null);
	   devHandIota = new Iotawatt_DeviceHandler(controller.appManPlus);
	   srIota = bc.registerService(DeviceHandlerProvider.class, devHandIota, null);
	   devHandIotaSimple = new IotawattSimple_DeviceHandler(controller.appManPlus);
	   srIotaSimple = bc.registerService(DeviceHandlerProvider.class, devHandIotaSimple, null);
	   devHandFlowProbe = new FlowScopeDevHandler(controller.appManPlus);
	   srFlowProbe = bc.registerService(DeviceHandlerProvider.class, devHandFlowProbe, null);
	   
	   devHandFlowSensSingle = new HeatingLabFlowSens_DeviceHandler(controller.appManPlus);
	   srFlowSensSingle = bc.registerService(DeviceHandlerProvider.class, devHandFlowSensSingle, null);
	   devHandTempSensSingle = new HeatingLabTempSens_DeviceHandler(controller.appManPlus);
	   srTempSensSingle = bc.registerService(DeviceHandlerProvider.class, devHandTempSensSingle, null);
	   devHandThValve = new HeatingLabThermalValve_DeviceHandler(controller.appManPlus);
	   srThValve = bc.registerService(DeviceHandlerProvider.class, devHandThValve, null);
	   devHandWall = new WallThermostatHandler(controller.appManPlus);
	   srWall = bc.registerService(DeviceHandlerProvider.class, devHandWall, null);
	   devHandCO2Hm = new CO2SensorHmHandler(controller.appManPlus);
	   srCO2Hm = bc.registerService(DeviceHandlerProvider.class, devHandCO2Hm, null);

	   jmbusConfig = new DriverHandlerJMBus(controller.appManPlus, configAdmin);
	   jmbusDriver = bc.registerService(DriverHandlerProvider.class, jmbusConfig, null);
	   mqttBrokerConfig = new DriverHandlerMQTTBroker(controller.appManPlus, configAdmin);
	   mqttBrokerDriver = bc.registerService(DriverHandlerProvider.class, mqttBrokerConfig, null);
	   knxConfig = new DriverHandlerKNX_IP(controller.appManPlus, configAdmin);
	   knxDriver = bc.registerService(DriverHandlerProvider.class, knxConfig, null);
	   devHandBeacon = new BluetoothBeaconHandler(controller.appManPlus);
	   srBeacon = bc.registerService(DeviceHandlerProvider.class, devHandBeacon, null);
	   devHandMSwitch = new MultiSwitchHandler(controller.appManPlus);
	   srMSwitch = bc.registerService(DeviceHandlerProvider.class, devHandMSwitch, null);
	   devHandIECMeter = new HMIEC_ElConnDeviceHandler(controller.appManPlus);
	   srIECMeter = bc.registerService(DeviceHandlerProvider.class, devHandIECMeter, null);
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
    	if (srOnOff != null) srOnOff.unregister();
    	if (srSmartDim != null) srSmartDim.unregister();
    	if (srLight != null) srLight.unregister();    	
    	
    	if (srPv != null) srPv.unregister();
    	if (srEnServ != null) srEnServ.unregister();
    	if (srCharge != null) srCharge.unregister();
    	if (srBat != null) srBat.unregister();
       	if (srSmartProtect != null) srSmartProtect.unregister();
       	if (srWater!= null) srWater.unregister();
       	if (srHeat != null) srHeat.unregister();
       	if (srGas != null) srGas.unregister();
      	if (srIota != null) srIota.unregister();
      	if (srIotaSimple!= null) srIotaSimple.unregister();
     	if (srFlowProbe!= null) srFlowProbe.unregister();
     	if (srFlowSensSingle!= null) srFlowSensSingle.unregister();
     	if (srTempSensSingle!= null) srTempSensSingle.unregister();
     	if (srThValve!= null) srThValve.unregister();
    	if (srWall!= null) srWall.unregister();
    	if (srCO2Hm!= null) srCO2Hm.unregister();

    	if (srDoorWindowSensor != null) srDoorWindowSensor.unregister();
    	if (srThermostat != null) srThermostat.unregister();
    	if (srTempHumSens != null) srTempHumSens.unregister();
    	if (srSmoke != null) srSmoke.unregister();
    	if (srWeather != null) srWeather.unregister();
    	if (srOpenWeather != null) srOpenWeather.unregister();
       	if (srMotion != null) srMotion.unregister();
    	
    	if (jmbusDriver != null) jmbusDriver.unregister();
    	if (mqttBrokerDriver != null) mqttBrokerDriver.unregister();
    	if (knxDriver != null) knxDriver.unregister();
       	if (srBeacon != null) srBeacon.unregister();
       	if (srMSwitch != null) srMSwitch.unregister();
      	if (srIECMeter != null) srIECMeter.unregister();

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

    protected void addUserDataService(UserDataAccess provider) {
    	userDataAccess = provider;
    }
    protected void removeUserDataService(UserDataAccess provider) {
    	userDataAccess = null;
    }

    protected void addMessageTransportProvider(MessageTransport provider) {
    	messageTransports.put(provider.getAddressType(), provider);
    }
    protected void removeMessageTransportProvider(MessageTransport provider) {
    	messageTransports.remove(provider.getAddressType());
    }
}
