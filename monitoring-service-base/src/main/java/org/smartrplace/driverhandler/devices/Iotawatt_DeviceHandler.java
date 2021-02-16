package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.EnergyResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.sensors.ElectricEnergySensor;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSimpleProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.autoconfig.api.DeviceTypeProvider;
import org.smartrplace.iotawatt.ogema.resources.IotaWattElectricityConnection;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIDataBase;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIMgmt;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class Iotawatt_DeviceHandler extends DeviceHandlerSimple<IotaWattElectricityConnection> {

	private final VirtualSensorKPIMgmt utilAggSubPhases;

	public Iotawatt_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
		utilAggSubPhases = new VirtualSensorKPIMgmt(
				new TimeseriesSimpleProcUtil(appMan.appMan(), appMan.dpService()), appMan.getLogger(), appMan.dpService()) {
			
			@Override
			public SingleValueResource getAndConfigureValueResourceSingle(Datapoint dpSource, VirtualSensorKPIDataBase mapData,
					String newSubResName, Resource device) {
				throw new IllegalStateException("Multi-input required!");
				
			}
			@Override
			public SingleValueResource getAndConfigureValueResource(List<Datapoint> dpSource, VirtualSensorKPIDataBase mapData,
					String newSubResName, Resource device) {
				IotaWattElectricityConnection iota = (IotaWattElectricityConnection) device;
				ElectricityConnection conn = iota.elConn();
	
				EnergyResource energyDailyRealAgg = conn.getSubResource(newSubResName, ElectricEnergySensor.class).reading();
				energyDailyRealAgg.getSubResource("unit", StringResource.class).<StringResource>create().setValue("kWh");
				energyDailyRealAgg.getParent().activate(true);

				List<Datapoint> sums;
				if(newSubResName.toLowerCase().contains("hour")) {
					sums = VirtualSensorKPIMgmt.registerEnergySumDatapointOverSubPhases(conn, AggregationMode.Meter2Meter, tsProcUtil, dpService,
							TimeProcUtil.SUM_PER_HOUR_EVAL);
				} else {
					sums = VirtualSensorKPIMgmt.registerEnergySumDatapointOverSubPhases(conn, AggregationMode.Meter2Meter, tsProcUtil, dpService,
							TimeProcUtil.SUM_PER_DAY_EVAL);					
				}
				
				mapData.evalDp = sums.get(0);
				return energyDailyRealAgg;
			}
		};
	}

	@Override
	public Class<IotaWattElectricityConnection> getResourceType() {
		return IotaWattElectricityConnection.class;
	}

	@Override
	protected SingleValueResource getMainSensorValue(IotaWattElectricityConnection device,
			InstallAppDevice deviceConfiguration) {
		return device.elConn().voltageSensor().reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(IotaWattElectricityConnection device,
			InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		List<Datapoint> energy = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		for(ElectricityConnection ec: device.elConn().subPhaseConnections().getAllElements()) {
			String ph = ec.getName();
			Datapoint dpenergy = addDatapoint(ec.energySensor().reading(), result, ph, dpService);
			energy.add(dpenergy);
			addDatapoint(ec.powerSensor().reading(), result, ph, dpService);
		}

		//final VirtualSensorKPIDataBase mapData1 = utilAggSubPhases.getDatapointDataAccumulation(energy, "total"+device.getName(), device,
		//		15*TimeProcUtil.MINUTE_MILLIS, false, true, result);
		
		utilAggSubPhases.addVirtualDatapoint(energy, "energySumHourly", device,
				15*TimeProcUtil.MINUTE_MILLIS, false, true, result);
		utilAggSubPhases.addVirtualDatapoint(energy, "energySumDaily", device,
				15*TimeProcUtil.MINUTE_MILLIS, false, true, result);

		return result;
	}

	@Override
	protected String getTableTitle() {
		return "Iotwatt 3-phase Measurement";
	}

	@Override
	protected Class<? extends ResourcePattern<IotaWattElectricityConnection>> getPatternClass() {
		return IotawattPattern.class;
	}

	protected class IotaWattDeviceType implements DeviceTypeProvider<IotaWattElectricityConnection> {
		public IotaWattDeviceType(int firstInput, ApplicationManager appMan) {
			this.firstInput = firstInput;
			this.appMan = appMan;
		}

		protected final int firstInput;
		protected final ApplicationManager appMan;
		
		@Override
		public String id() {
			return "3ph-"+firstInput+"to"+(firstInput+2);
		}

		@Override
		public String label(OgemaLocale arg0) {
			return "Iotawatt:3-phase measurement on inputs "+firstInput+"-"+(firstInput+2);
		}

		@Override
		public Class<IotaWattElectricityConnection> getDeviceType() {
			return IotaWattElectricityConnection.class;
		}

		@Override
		public String description(OgemaLocale locale) {
			return "no password required, configuration first provides update interval (connections to the device) as ISO8601 duration string. " + 
					"Recommended is {@code PT2m}, i.e. 2 minutes";
		}

		@Override
		public CreateAndConfigureResult<IotaWattElectricityConnection> addAndConfigureDevice(
				DeviceTypeConfigData<IotaWattElectricityConnection> configData) {
			
			CreateAndConfigureResult<IotaWattElectricityConnection> result = new CreateAndConfigureResult<IotaWattElectricityConnection>();

			if(configData.address.isEmpty()) {
				result.resultMessage = "Empty address cannot be processed!";
				return result;
			}
			String updIntv;
			//String readIntv;
			try {
				int idx = configData.configuration.indexOf("UI:");
				int end = configData.configuration.indexOf("_");
				if(idx < 0)
					updIntv = configData.configuration;
				else if(end < 0)
					updIntv = configData.configuration.substring(idx+3);
				else
					updIntv = configData.configuration.substring(idx+3, end);
				//idx = configData.configuration.indexOf("RI:");
				//readIntv = configData.configuration.substring(idx+3);
			} catch(StringIndexOutOfBoundsException e) {
				result.resultMessage = "Could not process configuration: "+configData.configuration;
				return result;
			}
			if(configData.governingResource == null) {
				ResourceList<?> iotaWattConnections = appMan.getResourceManagement().createResource("IotaWattConnections", ResourceList.class);
				configData.governingResource = iotaWattConnections.addDecorator(ResourceUtils.getValidResourceName(
						"iota_"+configData.address+"_S"+firstInput),
						IotaWattElectricityConnection.class);
			}
			String uri = "http://"+configData.address+"/query";
			ValueResourceHelper.setCreate(configData.governingResource.uri(), uri);
			ValueResourceHelper.setCreate(configData.governingResource.updateInterval(), updIntv);
			//ValueResourceHelper.setCreate(configData.governingResource.readingInterval(), readIntv);
			String[] phases = new String[] {"L1:Input_"+firstInput, "L2:Input_"+(firstInput+1), "L3:Input_"+(firstInput+2)};
			ValueResourceHelper.setCreate(configData.governingResource.phases(), phases);
			ValueResourceHelper.setCreate(configData.governingResource.voltage(), "Input_0");
			configData.governingResource.activate(true);
			
			result.resultConfig = configData;
			result.resultMessage = "Created Iotawatt device on "+configData.governingResource.getLocation();
			return result ;
		}

		@Override
		public Collection<DeviceTypeConfigData<IotaWattElectricityConnection>> getKnownConfigs() {
			List<DeviceTypeConfigData<IotaWattElectricityConnection>> result = new ArrayList<>();
			List<IotaWattElectricityConnection> allRes = appMan.getResourceAccess().getResources(IotaWattElectricityConnection.class);
			for(IotaWattElectricityConnection con: allRes) {
				String[] phases = con.phases().getValues();
				if((phases.length != 3) || (!phases[0].startsWith("L1:Input_")))
					continue;
				if(!phases[0].substring("L1:Input_".length()).equals(""+firstInput))
					continue;
				String uri = con.uri().getValue();
				String address;
				if(uri.startsWith("http://"))
					address = uri.substring("http://".length());
				else
					address = uri;
				if(address.endsWith("/query"))
					address = address.substring(0, address.length()-"/query".length());
				DeviceTypeConfigData<IotaWattElectricityConnection> config = new DeviceTypeConfigData<IotaWattElectricityConnection>(address,
						null, "UI:"+con.updateInterval().getValue()); //+"_RI:"+con.readingInterval().getValue());
				config.governingResource = con;
				config.dtbProvider = this;
				result.add(config);
			}
			return result;
		}

		@Override
		public boolean deleteConfig(DeviceTypeConfigData<IotaWattElectricityConnection> configData) {
			configData.governingResource.delete();
			return true;
		}

		@Override
		public DeviceTypeConfigDataBase getPlaceHolderData() {
			return new DeviceTypeConfigDataBase("192.168.0.99", null, "UI:PT2m");
		}		
	}
	
	@Override
	public Collection<DeviceTypeProvider<?>> getDeviceTypeProviders() {
		List<DeviceTypeProvider<?>> result = new ArrayList<>();
		DeviceTypeProvider<IotaWattElectricityConnection> prov = new IotaWattDeviceType(1, appMan.appMan());
		result.add(prov);
		prov = new IotaWattDeviceType(4, appMan.appMan());
		result.add(prov);
		prov = new IotaWattDeviceType(7, appMan.appMan());
		result.add(prov);
		prov = new IotaWattDeviceType(10, appMan.appMan());
		result.add(prov);
		return result;
	}
}
