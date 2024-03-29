package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH.DestType;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.metering.special.FlowProbe;
import org.ogema.model.sensors.GenericFloatSensor;
import org.ogema.model.sensors.Sensor;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class FlowScopeDevHandler extends DeviceHandlerSimple<FlowProbe> {

	public FlowScopeDevHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<FlowProbe> getResourceType() {
		return FlowProbe.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(FlowProbe device, InstallAppDevice deviceConfiguration) {
		return device.temperature().reading();
		//return device.flow().reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(FlowProbe device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		//addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		List<Sensor> allSens = device.getSubResources(Sensor.class, false);
		for(Sensor ec: allSens) {
			ValueResource reading = ec.reading();
			if(!(reading instanceof SingleValueResource))
				continue;
			String ch = ec.getName();
			addDatapoint((SingleValueResource) reading, result, ch, dpService);
		}
		return result;
	}

	@Override
	public String getTableTitle() {
		return "FlowProbe Devices";
	}

	@Override
	protected Class<? extends ResourcePattern<FlowProbe>> getPatternClass() {
		return FlowScopePattern.class;
	}

	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		FlowProbe device = (FlowProbe) appDevice.device();
		
		if(Boolean.getBoolean("org.smartrplace.project.smFac1")) {
			for(AlarmConfiguration alarm: appDevice.alarms().getAllElements()) {
				alarm.sendAlarm().setValue(false);
			}
		}
		
		AlarmingUtiH.setTemplateValues(appDevice, device.temperature().reading(),
				200f, 350f, 10, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES, 2880, DestType.CUSTOMER_SP_SAME, 2);
		AlarmingUtiH.setTemplateValues(appDevice, device.getSubResource("flowInSCFM", GenericFloatSensor.class).reading(),
				-100f, 999999.0f, 30, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES, 2880, DestType.CUSTOMER_SP_SAME, 2);
		AlarmingUtiH.setTemplateValues(appDevice, device.getSubResource("temperatureInC", GenericFloatSensor.class).reading(),
				0f, 100f, 30, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES, 2880, DestType.CUSTOMER_SP_SAME, 2);
		AlarmingUtiH.setTemplateValues(appDevice, device.pressure().reading(),
				-1f, 10f, 30, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES, 2880, DestType.CUSTOMER_SP_SAME, 2);
	}

	@Override
	public String getInitVersion() {
		return "C";
	}
	
	@Override
	public ComType getComType() {
		return ComType.IP;
	}
}
