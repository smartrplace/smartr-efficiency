package org.smartrplace.driverhandler.devices;

import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.devices.generators.PVPlant;
import org.ogema.model.sensors.ElectricPowerSensor;
import org.ogema.model.sensors.PowerSensor;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class DeviceHandler_PVPlant extends DeviceHandlerBase<PVPlant> {
	private final ApplicationManagerPlus appMan;
	
	public DeviceHandler_PVPlant(ApplicationManagerPlus appMan) {
		this.appMan = appMan;
	}
	
	@Override
	public Class<PVPlant> getResourceType() {
		return PVPlant.class;
	}

	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert,
			InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector, this) {
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

				final PVPlant box =
						(PVPlant) addNameWidget(object, vh, id, req, row, appMan);
				ElectricPowerSensor sampleSensor = box.electricityConnection().powerSensor();
				FloatResource reading = sampleSensor.reading();
				
				if(sampleSensor != null) {
					Label value = vh.floatLabel("Power",
								id, reading, row, "%.1f");
					Label lastContact = addLastContact(vh, id, req, row, reading);
					if (req != null) {
						value.setPollingInterval(DEFAULT_POLL_RATE, req);
						lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
					}
				}
				addSubLocation(object, vh, id, req, row);
				addInstallationStatus(object, vh, id, req, row);
				addComment(object, vh, id, req, row);

				appSelector.addWidgetsExpert(null, object, vh, id, req, row, appMan);
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return DeviceHandler_PVPlant.this.getResourceType();
			}
			
			@Override
			protected String id() {
				return DeviceHandler_PVPlant.this.id();
			}

			@Override
			protected String getTableTitle() {
				return "PV Plants";
			}
		};
	}

	@Override
	protected Class<? extends ResourcePattern<PVPlant>> getPatternClass() {
		return PVPlantPattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}

	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice appDevice, DatapointService dpService) {
		PVPlant dev = (PVPlant) appDevice.device();
		List<Datapoint> result = ESE_ElConnBoxDeviceHandler.getDatapointsStatic(dev.electricityConnection(), dpService);
		Datapoint dp = addDatapoint(dev.getSubResource("sma_type", IntegerResource.class), result, dpService);
		if(dp != null) {
			dp.addToSubRoomLocationAtomic(null, null, "smaType", false);
			setBuildingAsRoom(dp, dpService);
		}
		dp = addDatapoint(dev.getSubResource("initControl", IntegerResource.class), result, dpService);
		if(dp != null) {
			dp.addToSubRoomLocationAtomic(null, null, "initControl", false);
			setBuildingAsRoom(dp, dpService);
		}
		dp = addDatapoint(dev.getSubResource("ggc", IntegerResource.class), result, dpService);
		if(dp != null) {
			dp.addToSubRoomLocationAtomic(null, null, "ggc", false);
			setBuildingAsRoom(dp, dpService);
		}
		dp = addDatapoint(dev.getSubResource("ggc_fb", IntegerResource.class), result, dpService);
		if(dp != null) {
			dp.addToSubRoomLocationAtomic(null, null, "ggc_fb", false);
			setBuildingAsRoom(dp, dpService);
		} 
		dp = addDatapoint(dev.electricityConnection().reactivePowerSensor().settings().setpoint(), result, dpService);
		if(dp != null) {
			dp.addToSubRoomLocationAtomic(null, null, "reactivePowerSetpoint", false);
			setBuildingAsRoom(dp, dpService);
		}
		dp = addDatapoint(dev.electricityConnection().reactivePowerSensor().getSubResource("qpercent", FloatResource.class),
				result, dpService);
		if(dp != null) {
			dp.addToSubRoomLocationAtomic(null, null, "qpercent", false);
			setBuildingAsRoom(dp, dpService);
		}

		//addDatapoint(dev.electricityConnection().powerSensor().reading(), result, dpService);
		//addDatapoint(dev.electricityConnection().energySensor().reading(), result, dpService);
		
		PowerSensor apparentPowerSens = dev.getSubResource("apparentPowerSensor", PowerSensor.class);
		dp = addDatapoint(apparentPowerSens.deviceSettings().setpoint(), result, dpService);
		if(dp != null) {
			dp.addToSubRoomLocationAtomic(null, null, "apparentPowerSensor", false);
			setBuildingAsRoom(dp, dpService);
		}
		dp = addDatapoint(apparentPowerSens.ratedValues().upperLimit(), result, dpService);
		if(dp != null) {
			dp.addToSubRoomLocationAtomic(null, null, "apparentPowerLimit", false);
			setBuildingAsRoom(dp, dpService);
		}
		checkDpSubLocations(appDevice, result);
		return result;
	}
	
	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		PVPlant device = (PVPlant) appDevice.device();
		AlarmingUtiH.setTemplateValues(appDevice, device.electricityConnection().powerSensor().reading(),
				0.0f, 9999999f, 1, 20);
		AlarmingUtiH.setTemplateValues(appDevice, device.electricityConnection().energySensor().reading(),
				0f, Float.MAX_VALUE, 1, 20);
		AlarmingUtiH.addAlarmingMQTT(device, appDevice);
	}
	
	/** We are using this for configuration*/
	@Override
	public String getDeviceTypeShortId(InstallAppDevice device, DatapointService dpService) {
		if((device != null) &&
				((!device.installationLocation().exists()) || device.installationLocation().getValue().isEmpty())) {			
			String devName = device.device().getLocationResource().getName();
			//String defaultSubLoc;
			if(devName.contains("_")) {
				int idx = devName.lastIndexOf('_');
				String defaultSubLoc = devName.substring(idx); //"PVPlant"+devName.substring(idx);
				setInstallationLocation(device, defaultSubLoc, dpService);
			} //else
			//	defaultSubLoc = "PVPlant";				
			//ValueResourceHelper.setCreate(device.installationLocation(), defaultSubLoc);
		}
		return "PVP";
	}
}
