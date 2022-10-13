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
import org.smartrplace.driverhandler.devices.Burner_DeviceHandler;
import org.smartrplace.driverhandler.devices.ChargingPointDevHandler;
import org.smartrplace.driverhandler.devices.DeviceHandlerWMBus_SensorDevice;
import org.smartrplace.driverhandler.devices.DeviceHandler_PVPlant;
import org.smartrplace.driverhandler.devices.DriverHandlerJMBus;
import org.smartrplace.driverhandler.devices.DriverHandlerKNX_IP;
import org.smartrplace.driverhandler.devices.DriverHandlerMQTTBroker;
import org.smartrplace.driverhandler.devices.ESE_ElConnBoxDeviceHandler;
import org.smartrplace.driverhandler.devices.EventPushButtonHandler;
import org.smartrplace.driverhandler.devices.FaultMessageDeviceHandler;
import org.smartrplace.driverhandler.devices.FaultSingleDeviceHandler;
import org.smartrplace.driverhandler.devices.FaultSingleDeviceIntegerHandler;
import org.smartrplace.driverhandler.devices.FlowScopeDevHandler;
import org.smartrplace.driverhandler.devices.GasEnergyCam_DeviceHandler;
import org.smartrplace.driverhandler.devices.HMGas_MeterDeviceHandler;
import org.smartrplace.driverhandler.devices.HMIEC_ElConnDeviceHandler;
import org.smartrplace.driverhandler.devices.HeatCostAllocatorHandler;
import org.smartrplace.driverhandler.devices.HeatMeter2_DeviceHandler;
import org.smartrplace.driverhandler.devices.HeatMeter_DeviceHandler;
import org.smartrplace.driverhandler.devices.HeatingLabFlowSens_DeviceHandler;
import org.smartrplace.driverhandler.devices.HeatingLabGeneralData_DeviceHandler;
import org.smartrplace.driverhandler.devices.HeatingLabHumiditySens_DeviceHandler;
import org.smartrplace.driverhandler.devices.HeatingLabTempSens_DeviceHandler;
import org.smartrplace.driverhandler.devices.HeatingLabThermalValve_DeviceHandler;
import org.smartrplace.driverhandler.devices.IotawattSimple_DeviceHandler;
import org.smartrplace.driverhandler.devices.Iotawatt_DeviceHandler;
import org.smartrplace.driverhandler.devices.LightWLANDevHandler;
import org.smartrplace.driverhandler.devices.MechanicalFan_DeviceHandler;
import org.smartrplace.driverhandler.devices.MultiSwitchHandler;
import org.smartrplace.driverhandler.devices.Pump_DeviceHandler;
import org.smartrplace.driverhandler.devices.SmartProtect_DeviceHandler;
import org.smartrplace.driverhandler.devices.SmokeDetectorMBusHandler;
import org.smartrplace.driverhandler.devices.ThermalMixingCirc_DeviceHandler;
import org.smartrplace.driverhandler.devices.ThermalStorage_DeviceHandler;
import org.smartrplace.driverhandler.devices.WaterMeter_DeviceHandler;
import org.smartrplace.driverhandler.devices.WiredMBusMasterHandler;
import org.smartrplace.mqtt.devicetable.DeviceHandlerMQTT_Aircond;
import org.smartrplace.mqtt.devicetable.DeviceHandlerMQTT_ElecConnBox;
import org.smartrplace.mqtt.devicetable.DeviceHandlerMQTT_SingleSwBox;
import org.smartrplace.mqtt.devicetable.DeviceHandlerMQTT_SmartDimmer;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import org.smartrplace.driverhandler.devices.HeatPumpDevHandler;

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
	Collection<ServiceRegistration<?>> deviceHandlerRegistrations = Collections.newSetFromMap(new ConcurrentHashMap<>());

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

		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new DeviceHandlerMQTT_Aircond(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new DeviceHandlerMQTT_ElecConnBox(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new DeviceHandlerMQTT_SingleSwBox(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new DeviceHandlerMQTT_SmartDimmer(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new LightWLANDevHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new DeviceHandler_PVPlant(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new ESE_ElConnBoxDeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new ChargingPointDevHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new BatteryDevHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new SmartProtect_DeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new WaterMeter_DeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new HeatMeter_DeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new HeatMeter2_DeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new ThermalMixingCirc_DeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new Pump_DeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new Burner_DeviceHandler(controller.appManPlus), null));

		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new HeatPumpDevHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new GasEnergyCam_DeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new Iotawatt_DeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new IotawattSimple_DeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new FlowScopeDevHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new HeatingLabFlowSens_DeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new HeatingLabHumiditySens_DeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new HeatingLabTempSens_DeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new HeatingLabThermalValve_DeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new HeatingLabGeneralData_DeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new DeviceHandlerWMBus_SensorDevice(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new HeatCostAllocatorHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new SmokeDetectorMBusHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new WiredMBusMasterHandler(controller.appManPlus), null));

		DriverHandlerJMBus jmbusConfig = new DriverHandlerJMBus(controller.appManPlus, configAdmin);
		deviceHandlerRegistrations.add(bc.registerService(DriverHandlerProvider.class, jmbusConfig, null));
		deviceHandlerRegistrations.add(bc.registerService(DriverHandlerProvider.class, new DriverHandlerMQTTBroker(controller.appManPlus, configAdmin), null));
		deviceHandlerRegistrations.add(bc.registerService(DriverHandlerProvider.class, new DriverHandlerKNX_IP(controller.appManPlus, configAdmin), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new BluetoothBeaconHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new MultiSwitchHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new HMIEC_ElConnDeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new HMGas_MeterDeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new FaultMessageDeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new FaultSingleDeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new ThermalStorage_DeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new FaultSingleDeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new FaultSingleDeviceIntegerHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new MechanicalFan_DeviceHandler(controller.appManPlus), null));
		deviceHandlerRegistrations.add(bc.registerService(DeviceHandlerProvider.class, new EventPushButtonHandler(controller.appManPlus), null));

	}
 	
     /*
     * Callback called when the application is going to be stopped.
     */
    @Override
    public void stop(AppStopReason reason) {
    	if (widgetApp != null) widgetApp.close();

    	if (srDpservice != null) srDpservice.unregister();
    	
		deviceHandlerRegistrations.forEach(sr -> {
			sr.unregister();
		});

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
