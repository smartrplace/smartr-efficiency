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
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DriverPropertySuccessHandler;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.ogema.devicefinder.api.PropType;
import org.ogema.devicefinder.api.PropertyService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.LastContactLabel;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
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
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;

//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class DeviceHandlerDoorWindowSensor extends DeviceHandlerSimple<DoorWindowSensor> {

	private final ApplicationManagerPlus appMan;
	//private final OGEMADriverPropertyService<Resource> hmPropService;
	
	public DeviceHandlerDoorWindowSensor(ApplicationManagerPlus appMan) {
		super(appMan, false);
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
				id = id + "_DeviceHandlerDoorWindowSensor";  // avoid duplicates for now
				addWidgetsInternal(object, vh, id, req, row, appMan);
				appSelector.addWidgetsExpert(null, object, vh, id, req, row, appMan);
			}

			@Override
			protected Class<? extends Resource> getResourceType() {
				return DeviceHandlerDoorWindowSensor.this.getResourceType();
			}

			@Override
			protected String id() {
				return DeviceHandlerDoorWindowSensor.this.id();
			}

			@Override
			public String getTableTitle() {
				return "Window and Door Opening Sensors";
			}

			public DoorWindowSensor addWidgetsInternal(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				final DoorWindowSensor device = addNameWidget(object, vh, id, req, row, appMan);
				Label state = vh.booleanLabel("Measured State", id, device.reading(), row, 0);
				addBatteryStatus(vh, id, req, row, device);
				//vh.floatLabel("Battery", id, device.battery().internalVoltage().reading(), row, "%.1f#min:0.1");
				Label lastContact = null;
				if(req != null) {
					lastContact = new LastContactLabel(device.reading(), appMan, mainTable, "lastContact"+id, req);
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
					state.setPollingInterval(DEFAULT_POLL_RATE, req);
					lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
				}
				return device;
			}
			public DoorWindowSensor addNameWidget(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				//if(!(object.device() instanceof DoorWindowSensor) && (req != null)) return null;
				final DoorWindowSensor device;
				if(req == null)
					device = ResourceHelper.getSampleResource(DoorWindowSensor.class);
				else
					device = (DoorWindowSensor) object.device().getLocationResource();
				//if(!(object.device() instanceof Thermostat)) return;
				final String name;
				if(device.getLocation().toLowerCase().contains("homematic")) {
					name = "WindowSens HM:"+ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
				} else
					name = ResourceUtils.getHumanReadableShortName(device);
				vh.stringLabel("Name", id, name, row);
				vh.stringLabel("ID", id, object.deviceId().getValue(), row);
				return device;
			}	

		};
	}

	@Override
	public Class<DoorWindowSensor> getResourceType() {
        return DoorWindowSensor.class;
	}

	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService) {
		List<Datapoint> result = new ArrayList<>();
		DoorWindowSensor dev = (DoorWindowSensor) installDeviceRes.device();
		if (null == dev) return result;
		result.add(dpService.getDataPointStandard(dev.reading()));
		//if(dev.battery().internalVoltage().reading().isActive())
		//	result.add(dpService.getDataPointStandard(dev.battery().internalVoltage().reading()));
		addtStatusDatapointsHomematic(dev, dpService, result);
		return result;
	}

	@Override
	protected Class<? extends ResourcePattern<DoorWindowSensor>> getPatternClass() {
		return DoorWindowSensorPattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}

	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		DoorWindowSensor device = (DoorWindowSensor) appDevice.device().getLocationResource();
		AlarmingUtiH.setTemplateValues(appDevice, device.reading(), 0.0f, 1.0f, 1, AlarmingUtiH.DEFAULT_NOVALUE_FORHOURLY_MINUTES);
System.out.println("     ####### Setting window duration to :"+AlarmingUtiH.DEFAULT_NOVALUE_FORHOURLY_MINUTES+" : "+device.reading().getLocation());
		//TODO: This is not considered in template
		//boolean has2batteries = device.getLocation().contains("_SWDM_");
		//AlarmingUtiH.addAlarmingHomematic(device, appDevice, has2batteries?2:1);
		AlarmingUtiH.addAlarmingHomematic(device, appDevice, 0);
		//IntegerResource rssiDevice = ResourceHelper.getSubResourceOfSibbling(device,
		//		"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiDevice", IntegerResource.class);
		//special setting for window sensors
		//if(rssiDevice != null && rssiDevice.exists())
		//	AlarmingUtiH.setTemplateValues(appDevice, rssiDevice,
		//			-94f, -10f, 10, 720);
		/*IntegerResource rssiDevice = ResourceHelper.getSubResourceOfSibbling(device,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiDevice", IntegerResource.class);
		if(rssiDevice != null && rssiDevice.exists())
			AlarmingUtiH.setTemplateValues(appDevice, rssiDevice,
					-30f, -94f, 10, 300);
		IntegerResource rssiPeer = ResourceHelper.getSubResourceOfSibbling(device,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiPeer", IntegerResource.class);
		if(rssiPeer != null && rssiPeer.exists())
			AlarmingUtiH.setTemplateValues(appDevice, rssiPeer,
					-30f, -94f, 10, 300);
		*/
	}

	@Override
	public String getInitVersion() {
		return "L";
	}
	
	protected int valueRequestsentNum = 0;
	protected int valueRequestSkippedNum = 0;
	
	@Override
	public List<RoomInsideSimulationBase> startSupportingLogicForDevice(InstallAppDevice device,
			DoorWindowSensor deviceResource, SingleRoomSimulationBase roomSimulation, DatapointService dpService) {
		List<RoomInsideSimulationBase> result = new ArrayList<>();

		if(deviceResource.getLocation().toLowerCase().contains("homematicip") ||
				(!deviceResource.getLocation().toLowerCase().contains("homematic")))
			return result;
		
		//start aesMode setter
		Timer timer = appMan.appMan().createTimer(30*TimeProcUtil.MINUTE_MILLIS, new TimerListener() {
				
			@Override
			public void timerElapsed(Timer arg0) {
				getPropertyService();
				if(propService == null)
					return;
				//Resource propDev = getMainChannelPropRes(deviceResource);
				propService.setProperty(deviceResource, PropType.ENCRYPTION_ENABLED, "false", null);
			}
		});
		result.add(new DeviceHandlerMQTT_Aircond.TimerSimSimple(timer));
		
		Timer timerValue = appMan.appMan().createTimer(1*TimeProcUtil.MINUTE_MILLIS, new TimerListener() {
			
			@Override
			public void timerElapsed(Timer arg0) {
				getPropertyService();
				if(propService == null)
					return;
				long now = appMan.getFrameworkTime();
				long lastWrite = deviceResource.reading().getLastUpdateTime();
				if((now - lastWrite) > (TimeProcUtil.HOUR_MILLIS+5*TimeProcUtil.MINUTE_MILLIS)) {
					int skipBeforeSend;
					if(valueRequestsentNum > 100)
						skipBeforeSend = 60;
					else if(valueRequestsentNum > 10)
						skipBeforeSend = 5;
					else
						skipBeforeSend = 0;
					if(valueRequestSkippedNum < skipBeforeSend) {
						skipBeforeSend++;
						return;
					}
					String label = dpService.getDataPointStandard(deviceResource.reading()).label(null);
					appMan.getLogger().warn("Request value property #"+valueRequestsentNum+" for DoowWindowSensor "+label+" ("+deviceResource.getLocation()+")");
					propService.getProperty(deviceResource, PropType.CURRENT_SENSOR_VALUE, 
							new DriverPropertySuccessHandler<Resource>() {
								@Override
								public void operationFinished(Resource anchorResource, String propertyId,
										boolean success, String message) {
									long duration = appMan.getFrameworkTime() - now;
									if(success) {
										String value = DriverPropertyUtils.getPropertyValue(anchorResource, propertyId);
										try {
											boolean bval = Boolean.parseBoolean(value);
											if((!bval) && (!value.toLowerCase().equals("false")))
												appMan.getLogger().warn("Received value property not boolean:"+value);
											else {
												appMan.getLogger().warn("Received value property:"+bval);
												deviceResource.reading().setValue(bval);
											}
										} catch(NumberFormatException e) {
											appMan.getLogger().warn("Received value property not parsable:"+value);											
										}
									}
									if(message != null)
										appMan.getLogger().warn("Reading value property #"+valueRequestsentNum+" took "+duration+" msec, received message: "+message);
									else
										appMan.getLogger().warn("Reading value property #"+valueRequestsentNum+" took "+duration+" msec");
								}
							});
					valueRequestsentNum ++;
					valueRequestSkippedNum= 0;
				} else
					valueRequestsentNum = 0;
			}
		});
		result.add(new DeviceHandlerMQTT_Aircond.TimerSimSimple(timerValue));

		return result;
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
					return new PropAccessDataHm(getAnchorResource(devRes, "HM_SHUTTER_CONTACT"), "MASTER/AES_ACTIVE:BOOL(7)");
				else if(propType.id.equals(PropType.CURRENT_SENSOR_VALUE.id))
					return new PropAccessDataHm(getAnchorResource(devRes, "HM_SHUTTER_CONTACT"), "VALUES/STATE:BOOL(5)");
				//These are not really used yet
				else if(propType.id.equals(PropType.DEVICE_ERROR.id))
					return new PropAccessDataHm(getAnchorResource(devRes, "HM_SHUTTER_CONTACT"), "VALUES/ERROR:ENUM(5)");
				else if(propType.id.equals(PropType.TRANSMIT_TRY_MAX.id))
					return new PropAccessDataHm(getAnchorResource(devRes, "HM_SHUTTER_CONTACT"), "MASTER/TRANSMIT_TRY_MAX:INTEGER(7)");
				else if(propType.id.equals(PropType.EXPECT_AES.id))
					return new PropAccessDataHm(getAnchorResource(devRes, "HM_SHUTTER_CONTACT"), "LINK/EXPECT_AES:BOOL(7)");
				else if(propType.id.equals(PropType.PEER_NEEDS_BURST.id))
					return new PropAccessDataHm(getAnchorResource(devRes, "HM_SHUTTER_CONTACT"), "LINK/PEER_NEEDS_BURST:BOOL(7)");
				
				//These are in HM_Maintenance
				else if(propType.id.equals(PropType.LOCAL_RESET_DISABLE.id))
					return new PropAccessDataHm(getAnchorResource(devRes, "HM_MAINTENANCE"), "MASTER/LOCAL_RESET_DISABLE:BOOL(7)");
				else if(propType.id.equals(PropType.TRANSMIT_DEV_TRY_MAX.id))
					return new PropAccessDataHm(getAnchorResource(devRes, "HM_MAINTENANCE"), "MASTER/TRANSMIT_DEV_TRY_MAX:INTEGER(7)");

				return null;
			}
		};
		return propService;
	}
	
	protected static final List<PropType> PROPS_SUPPORTED = Arrays.asList(new PropType[] {PropType.ENCRYPTION_ENABLED,
			PropType.CURRENT_SENSOR_VALUE, PropType.DEVICE_ERROR, PropType.TRANSMIT_TRY_MAX, PropType.TRANSMIT_DEV_TRY_MAX, PropType.EXPECT_AES,
			PropType.PEER_NEEDS_BURST, PropType.LOCAL_RESET_DISABLE});

	@Override
	protected SingleValueResource getMainSensorValue(DoorWindowSensor device, InstallAppDevice deviceConfiguration) {
		throw new IllegalStateException("Should not be used yet!");
		//return null;
	}

	@Override
	protected Collection<Datapoint> getDatapoints(DoorWindowSensor device, InstallAppDevice deviceConfiguration) {
		throw new IllegalStateException("Should not be used yet!");
		//return null;
	}

	@Override
	public String getTableTitle() {
		return "Window and Door Opening Sensors";
	}
}