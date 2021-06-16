package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.sensors.VolumeAccumulatedSensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class GasEnergyCam_DeviceHandler extends DeviceHandlerSimple<SensorDevice> {

	public GasEnergyCam_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<SensorDevice> getResourceType() {
		return SensorDevice.class;
	}

	@Override
	protected SingleValueResource getMainSensorValue(SensorDevice device, InstallAppDevice deviceConfiguration) {
		return device.getSubResource("VOLUME_0_0", VolumeAccumulatedSensor.class).reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(SensorDevice device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();

		// We have to sychronize with reading remote slotsdb and setting up the time series for mirror devices here
		Resource mirrorList = appMan.getResourceAccess().getResource("serverMirror");
		if(mirrorList != null) {
			IntegerResource initStatus = mirrorList.getSubResource("initStatus", IntegerResource.class);
			while(initStatus.isActive() && (initStatus.getValue() < 2) && Boolean.getBoolean("org.smartrplace.app.srcmon.iscollectinggateway")) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Gas Energy Cams";
	}

	@Override
	protected Class<? extends ResourcePattern<SensorDevice>> getPatternClass() {
		return GasEnergyCam_SensorDevicePattern.class;
	}
	
	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "GEC";
	}

}
