package org.smartrplace.homematic.devicetable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DriverPropertySuccessHandler;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.ogema.devicefinder.api.PropType;
import org.ogema.devicefinder.api.PropertyService;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.LastContactLabel;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.DoorWindowSensor;
import org.ogema.simulation.shared.api.RoomInsideSimulationBase;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.prop.DriverPropertyUtils;
import org.smartrplace.mqtt.devicetable.DeviceHandlerMQTT_Aircond;
import org.smartrplace.mqtt.devicetable.DeviceHandlerMQTT_Aircond.SetpointToFeedbackSimSimple;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;

//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class DeviceHandlerThermostat extends DeviceHandlerBase<Thermostat> {

	protected static final String MAIN_PROP = "HM_CLIMATECONTROL_RT_TRANSCEIVER";
	private final ApplicationManagerPlus appMan;

	public DeviceHandlerThermostat(ApplicationManagerPlus appMan) {
		this.appMan = appMan;
		appMan.getLogger().info("{} created :)", this.getClass().getSimpleName());
	}
	
	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert, InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector, this) {

			@Override
			public void addWidgets(InstallAppDevice object,
					ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req,
					Row row, ApplicationManager appMan) {
				id = id + "_DeviceHandlerThermostat";  // avoid duplicates for now
				addWidgetsInternal(object, vh, id, req, row, appMan);
				appSelector.addWidgetsExpert(null, object, vh, id, req, row, appMan);
			}

			@Override
			protected Class<? extends Resource> getResourceType() {
				return DeviceHandlerThermostat.this.getResourceType();
			}

			@Override
			protected String id() {
				return DeviceHandlerThermostat.this.id();
			}

			@Override
			protected String getTableTitle() {
				return "Thermostats";
			}
			
			public Thermostat addWidgetsInternal(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				//if(!(object.device() instanceof Thermostat) && (req != null)) return null;
				final Thermostat device;
				if(req == null)
					device = ResourceHelper.getSampleResource(Thermostat.class);
				else
					device = (Thermostat) object.device();
				//if(!(object.device() instanceof Thermostat)) return;
				final String name;
				if(device.getLocation().toLowerCase().contains("homematic")) {
					name = "Thermostat HM:"+ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
				} else
					name = ResourceUtils.getHumanReadableShortName(device);
				vh.stringLabel("Name", id, name, row);
				vh.stringLabel("ID", id, object.deviceId().getValue(), row);
				Label setpointFB = vh.floatLabel("Setpoint", id, device.temperatureSensor().deviceFeedback().setpoint(), row, "%.1f");
				if(req != null) {
					TextField setpointSet = new TextField(mainTable, "setpointSet"+id, req) {
						private static final long serialVersionUID = 1L;
						@Override
						public void onGET(OgemaHttpRequest req) {
							setValue(String.format("%.1f", device.temperatureSensor().settings().setpoint().getCelsius()), req);
						}
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							String val = getValue(req);
							val = val.replaceAll("[^\\d.]", "");
							try {
								float value  = Float.parseFloat(val);
								if(value < 4.5f || value> 30.5f) {
									alert.showAlert("Allowed range: 4.5 to 30°C", false, req);
								} else
									device.temperatureSensor().settings().setpoint().setCelsius(value);
							} catch (NumberFormatException | NullPointerException e) {
								if(alert != null) alert.showAlert("Entry "+val+" could not be processed!", false, req);
								return;
							}
						}
					};
					row.addCell("Set", setpointSet);
				} else
					vh.registerHeaderEntry("Set");
				Label tempmes = vh.floatLabel("Measurement", id, device.temperatureSensor().reading(), row, "%.1f#min:-200");
				vh.floatLabel("Battery", id, device.battery().internalVoltage().reading(), row, "%.1f#min:0.1");
				Label lastContact = null;
				if(req != null) {
					lastContact = new LastContactLabel(device.temperatureSensor().reading(), appMan, mainTable, "lastContact"+id, req);
					row.addCell(WidgetHelper.getValidWidgetId("Last Contact"), lastContact);
				} else
					vh.registerHeaderEntry("Last Contact");
				
				// TODO addWidgetsCommon(object, vh, id, req, row, appMan, device.location().room());
				Room deviceRoom = device.location().room();
				addRoomWidget(vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object, vh, id, req, row);
				addInstallationStatus(object, vh, id, req, row);
				addComment(object, vh, id, req, row);
				if(req != null) {
					String text = getHomematicCCUId(object.device().getLocation());
					vh.stringLabel("RT", id, text, row);
				} else
					vh.registerHeaderEntry("RT");	
				
				
				
				if(req != null) {
					tempmes.setPollingInterval(DEFAULT_POLL_RATE, req);
					setpointFB.setPollingInterval(DEFAULT_POLL_RATE, req);
					lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
				}
				return device;
			}
			
		};
	}

	@Override
	public Class<Thermostat> getResourceType() {
		return Thermostat.class;
	}

	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService) {
		List<Datapoint> result = new ArrayList<>();
		Thermostat dev = (Thermostat) installDeviceRes.device();
		if (null == dev) return result;
		result.add(dpService.getDataPointStandard(dev.temperatureSensor().reading()));
		result.add(dpService.getDataPointStandard(dev.temperatureSensor().settings().setpoint()));
		result.add(dpService.getDataPointStandard(dev.temperatureSensor().deviceFeedback().setpoint()));
		result.add(dpService.getDataPointStandard(dev.valve().setting().stateFeedback()));
		if(dev.battery().internalVoltage().reading().isActive())
			result.add(dpService.getDataPointStandard(dev.battery().internalVoltage().reading()));
		addtStatusDatapointsHomematic(dev, dpService, result);
		return result;
	}

	@Override
	protected Class<? extends ResourcePattern<Thermostat>> getPatternClass() {
		return ThermostatPattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}

	@Override
	public List<RoomInsideSimulationBase> startSupportingLogicForDevice(InstallAppDevice device,
			Thermostat deviceResource, SingleRoomSimulationBase roomSimulation, DatapointService dpService) {
		List<RoomInsideSimulationBase> result = new ArrayList<>();

		//start controlMode setter
		IntegerResource setManualMode = deviceResource.getSubResource("controlMode", IntegerResource.class);
		if(setManualMode.isActive()) {
			//ThermostatPattern pat = (ThermostatPattern) getPattern(deviceResource);
			//if((pat != null) && (pat.controlModeTimer == null)) {
			Timer timer = appMan.appMan().createTimer(30*TimeProcUtil.MINUTE_MILLIS, new TimerListener() {
				
				@Override
				public void timerElapsed(Timer arg0) {
					setManualMode.setValue(1);					
				}
			});
			result.add(new DeviceHandlerMQTT_Aircond.TimerSimSimple(timer));
			//}
		}
		return result;
	}
	
	@Override
	public List<RoomInsideSimulationBase> startSimulationForDevice(InstallAppDevice device, Thermostat deviceResource,
			SingleRoomSimulationBase roomSimulation, DatapointService dpService) {
		List<RoomInsideSimulationBase> result = new ArrayList<>();

		//Return value is currently not used anyways
		if(roomSimulation != null) {
			result.add(new SetpointToFeedbackSimSimple(roomSimulation.getTemperature(),
					deviceResource.temperatureSensor().reading(), appMan, roomSimulation));
		}
		result.add(new SetpointToFeedbackSimSimple(deviceResource.temperatureSensor().settings().setpoint(),
				deviceResource.temperatureSensor().deviceFeedback().setpoint(), appMan, null));
		return result;
	}
	
	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		Thermostat device = (Thermostat) appDevice.device();
		AlarmingUtiH.setTemplateValues(appDevice, device.temperatureSensor().reading(), 5.0f, 35.0f, 15, 20);
		AlarmingUtiH.setTemplateValues(appDevice, device.temperatureSensor().settings().setpoint(),
				4.5f, 30.5f, 1, 1500);
		AlarmingUtiH.setTemplateValues(appDevice, device.temperatureSensor().deviceFeedback().setpoint(),
				4.5f, 30.5f, 1, 20);
		AlarmingUtiH.setTemplateValues(appDevice, device.valve().setting().stateFeedback(),
				0f, 100f, 1, 20);
		AlarmingUtiH.addAlarmingHomematic(device, appDevice);
		/*AlarmingUtiH.setTemplateValues(appDevice, device.battery().internalVoltage().reading(),
				1.5f, 3.5f, 1, 70);
		//BooleanResource comDisturbed = ResourceHelper.getSubResourceOfSibbling(device,
		//		"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "communicationStatus/communicationDisturbed", BooleanResource.class);
		//if(comDisturbed != null && comDisturbed.exists())
		//	AlarmingUtiH.setTemplateValues(appDevice, comDisturbed,
		//			0.0f, 1.0f, 60, -1);
		/*IntegerResource rssiDevice = ResourceHelper.getSubResourceOfSibbling(device,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiDevice", IntegerResource.class);
		if(rssiDevice != null && rssiDevice.exists())
			AlarmingUtiH.setTemplateValues(appDevice, rssiDevice,
					-30f, -94f, 10, 300);
		IntegerResource rssiPeer = ResourceHelper.getSubResourceOfSibbling(device,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiPeer", IntegerResource.class);
		if(rssiPeer != null && rssiPeer.exists())
			AlarmingUtiH.setTemplateValues(appDevice, rssiPeer,
					-30f, -94f, 10, 300);*/
		appDevice.alarms().activate(true);
	}
	
	PropertyService propService = null;
	@Override
	public PropertyService getPropertyService() {
		if(propService != null)
			return propService;
		@SuppressWarnings("unchecked")
		OGEMADriverPropertyService<Resource> hmPropService = (OGEMADriverPropertyService<Resource>)
				appMan.dpService().driverpropertyServices().get("HmPropertyServiceProvider");
		if(hmPropService == null)
			return null;
		propService = new PropertyService() {
			
			@Override
			public void setProperty(Resource deviceResource, PropType propType, String value,
					DriverPropertySuccessHandler<?> successHandler, String... argument) {
				//Resource propDev = getMainChannelPropRes(deviceResource);
				PropAccessDataHm accData = getPropAccess(deviceResource, propType);
				writePropertyHm(accData.propId, accData.anchorRes, value, successHandler, hmPropService, appMan.getLogger());
System.out.println("  ++++ Wrote Property "+propType.id()+" for "+accData.anchorRes.getLocation()+ " value:"+value);
				/*if(propDev == null)
					return;
				String propertyId = getPropId(propType);
				hmPropService.writeProperty(propDev, propertyId , appMan.getLogger(), value,
						(DriverPropertySuccessHandler<Resource>)successHandler);*/
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public String getProperty(Resource deviceResource, PropType propType,
					DriverPropertySuccessHandler<?> successHandler, String... arguments) {
				//Resource propDev = getMainChannelPropRes(deviceResource);
				PropAccessDataHm accData = getPropAccess(deviceResource, propType);
				if(accData == null)
					return null;
				//String propertyId = getPropId(propType);
				//if(propertyId == null)
				//	return null;
				if(successHandler != null)
					hmPropService.updateProperty(accData.anchorRes, accData.propId , appMan.getLogger(),
						(DriverPropertySuccessHandler<Resource>)successHandler);
				return DriverPropertyUtils.getPropertyValue(accData.anchorRes, accData.propId);
			}

			@Override
			public List<PropType> getPropTypesSupported() {
				return PROPS_SUPPORTED;
			}

			@Override
			public PropAccessDataHm getPropAccess(Resource devDataResource, PropType propType) {
				//These are in HM_SHUTTER_CONTACT
				if(!(devDataResource instanceof DoorWindowSensor))
					return null;
				DoorWindowSensor devRes = (DoorWindowSensor) devDataResource;
				if(propType.id.equals(PropType.ENCRYPTION_ENABLED.id))
					return new PropAccessDataHm(getAnchorResource(devRes, MAIN_PROP), "MASTER/AES_ACTIVE:BOOL(7)");
				else if(propType.id.equals(PropType.CURRENT_SENSOR_VALUE.id))
					return new PropAccessDataHm(getAnchorResource(devRes, MAIN_PROP), "VALUES/ACTUAL_TEMPERATURE:FLOAT(5)");
				//These are not really used yet
				else if(propType.id.equals(PropType.BURST_RX.id))
					return new PropAccessDataHm(getAnchorResource(devRes, MAIN_PROP), "MASTER/BURST_RX:BOOL(7)");
				
				//These are in HM_Maintenance
				else if(propType.id.equals(PropType.LOCAL_RESET_DISABLE.id))
					return new PropAccessDataHm(getAnchorResource(devRes, "HM_MAINTENANCE"), "MASTER/LOCAL_RESET_DISABLE:BOOL(7)");
				else if(propType.id.equals(PropType.THERMOSTAT_WINDOWOPEN_MODE.id))
					return new PropAccessDataHm(getAnchorResource(devRes, "HM_MAINTENANCE"), "MASTER/TEMPERATUREFALL_MODUS:ENUM(7)");
				else if(propType.id.equals(PropType.THERMOSTAT_WINDOWOPEN_TEMPERATURE.id))
					return new PropAccessDataHm(getAnchorResource(devRes, "HM_MAINTENANCE"), "MASTER/TEMPERATUREFALL_WINDOW_OPEN:FLOAT(7)");
				else if(propType.id.equals(PropType.THERMOSTAT_WINDOWOPEN_MINUTES.id))
					return new PropAccessDataHm(getAnchorResource(devRes, "HM_MAINTENANCE"), "MASTER/TEMPERATUREFALL_WINDOW_OPEN_TIME_PERIOD:INTEGER(7)");

				else if(propType.id.equals(PropType.THERMOSTAT_BOOST_MINUTES))
					return new PropAccessDataHm(getAnchorResource(devRes, "HM_MAINTENANCE"), "MASTER/VALVE_MAXIMUM_POSITION:INTEGER(7)");
				else if(propType.id.equals(PropType.THERMOSTAT_BOOST_POSITION))
					return new PropAccessDataHm(getAnchorResource(devRes, "HM_MAINTENANCE"), "MASTER/BOOST_POSITION:INTEGER(7)");
				else if(propType.id.equals(PropType.THERMOSTAT_VALVE_MAXPOSITION))
					return new PropAccessDataHm(getAnchorResource(devRes, "HM_MAINTENANCE"), "MASTER/BOOST_TIME_PERIOD:ENUM(7)");
				
				return null;
			}
		};
		return propService;
	}
	
	protected static final List<PropType> PROPS_SUPPORTED = Arrays.asList(new PropType[] {PropType.ENCRYPTION_ENABLED,
			PropType.CURRENT_SENSOR_VALUE,
			PropType.BURST_RX, PropType.LOCAL_RESET_DISABLE,
			PropType.THERMOSTAT_OPERATION_MODE,
			PropType.THERMOSTAT_WINDOWOPEN_MODE, PropType.THERMOSTAT_WINDOWOPEN_TEMPERATURE, PropType.THERMOSTAT_WINDOWOPEN_MINUTES,
			PropType.THERMOSTAT_BOOST_MINUTES, PropType.THERMOSTAT_BOOST_POSITION, PropType.THERMOSTAT_VALVE_MAXPOSITION});

	/** Weitere interessante Paramter:
	 * MASTER/ADAPTIVE_REGULATION:ENUM(7):
	 * Vermutlich das Lernen der passenden Valve Position für unterschiedliche Zieltemperaturen
	 * ADAPTIVE_REGULATION=
	{
		DEFAULT=2
		FLAGS=1
		ID=ADAPTIVE_REGULATION
		MAX=2
		MIN=0
		OPERATIONS=7
		TAB_ORDER=0
		TYPE=ENUM
		UNIT=
		VALUE_LIST=
		[
			OFF with default values
			OFF with determined values
			ON
		]
	}
	 */
}
