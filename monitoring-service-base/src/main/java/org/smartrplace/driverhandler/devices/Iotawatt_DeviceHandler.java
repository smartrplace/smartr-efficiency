package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.EnergyResource;
import org.ogema.core.model.units.PhysicalUnit;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.sensors.ElectricEnergySensor;
import org.ogema.recordeddata.DataRecorder;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil.MeterReference;
import org.ogema.timeseries.eval.simple.mon.TimeSeriesServlet;
import org.ogema.timeseries.eval.simple.mon3.TimeseriesSimpleProcUtil3;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.autoconfig.api.DeviceTypeProvider;
import org.smartrplace.iotawatt.ogema.resources.IotaWattElectricityConnection;
import org.smartrplace.tissue.util.logconfig.LogConfigSP;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIDataBase;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIMgmt;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

@SuppressWarnings("serial")
public class Iotawatt_DeviceHandler extends DeviceHandlerSimple<IotaWattElectricityConnection> {

	public static final long VIRTUAL_SENSOR_UPDATE_RATE = 3*TimeProcUtil.MINUTE_MILLIS;

	private final VirtualSensorKPIMgmt utilAggSubPhases;

	protected final ApplicationManagerPlus appManPlus;
	
	public Iotawatt_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
		this.appManPlus = appMan;
		utilAggSubPhases = new VirtualSensorKPIMgmt(
				new TimeseriesSimpleProcUtil3(appMan.appMan(), appMan.dpService(), 2, VIRTUAL_SENSOR_UPDATE_RATE),
				appMan.getLogger(), appMan.dpService()) {
			
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
				Integer absoluteTiming;
				if(newSubResName.toLowerCase().contains("hour")) {
					sums = VirtualSensorKPIMgmt.registerEnergySumDatapointOverSubPhases(conn, AggregationMode.Meter2Meter, tsProcUtil, dpService,
							TimeProcUtil.SUM_PER_HOUR_EVAL);
					absoluteTiming = AbsoluteTiming.HOUR;
				} else {
					sums = VirtualSensorKPIMgmt.registerEnergySumDatapointOverSubPhases(conn, AggregationMode.Meter2Meter, tsProcUtil, dpService,
							TimeProcUtil.SUM_PER_DAY_EVAL);					
					absoluteTiming = AbsoluteTiming.DAY;
				}
				
				if(!sums.isEmpty()) {
					mapData.evalDp = sums.get(0);
					mapData.absoluteTiming = absoluteTiming;
				}
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
		// We have to sychronize with reading remote slotsdb and setting up the time series for mirror devices here
		VirtualSensorKPIMgmt.waitForCollectingGatewayServerInit(appMan.getResourceAccess());
		/*if(mirrorList != null) {
			IntegerResource initStatus = mirrorList.getSubResource("initStatus", IntegerResource.class);
			while(initStatus.isActive() && (initStatus.getValue() < 2) && Boolean.getBoolean("org.smartrplace.app.srcmon.iscollectinggateway")) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}*/
		
		List<Datapoint> result = new ArrayList<>();
		List<Datapoint> energy = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		for(ElectricityConnection ec: device.elConn().subPhaseConnections().getAllElements()) {
			String ph = ec.getName();
			Datapoint dpenergy = addDatapoint(ec.energySensor().reading(), result, ph, dpService);
			energy.add(dpenergy);
			addDatapoint(ec.powerSensor().reading(), result, ph, dpService);
			addDatapoint(ec.reactivePowerSensor().reading(), result, ph, dpService);
		}

		//final VirtualSensorKPIDataBase mapData1 = utilAggSubPhases.getDatapointDataAccumulation(energy, "total"+device.getName(), device,
		//		15*TimeProcUtil.MINUTE_MILLIS, false, true, result);
		if(Boolean.getBoolean("virtualSensors.disable")) {
			System.out.println("   *** Disabling Iotawatt virtualSensors based on energySensor: "+device.getLocation());
			return result;
		}
		if(energy.isEmpty()) {
			System.out.println("   !!! WARNING: Iotawatt without energySensor to use: "+device.getLocation());
			return result;
		}
long start = dpService.getFrameworkTime();
System.out.println("   *** IOTAwatt virtual datapoints starting for "+device.getLocation());
		VirtualSensorKPIDataBase dpHourly = utilAggSubPhases.addVirtualDatapoint(energy, "energySumHourly", device,
				15*TimeProcUtil.MINUTE_MILLIS, false, true, result);
		utilAggSubPhases.addVirtualDatapoint(Arrays.asList(new Datapoint[] {dpHourly.evalDp}), "energySumDaily", device,
				15*TimeProcUtil.MINUTE_MILLIS, false, true, result);

long end = dpService.getFrameworkTime();
System.out.println("   *** IOTAwatt virtual datapoints took "+(end-start)+" msec for "+device.getLocation());
		
		return result;
	}

	@Override
	public void addMoreWidgetsExpert(InstallAppDevice object,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		if(req == null) {
			vh.registerHeaderEntry("RefTimeCounter");
			vh.registerHeaderEntry("EnergyAsKwh");
			return;
		}
		IotaWattElectricityConnection device = (IotaWattElectricityConnection)object.device();

		vh.floatEdit("RefTimeCounter", id, device.getSubResource("refTimeCounter", FloatResource.class), row, null,
				0, Float.MAX_VALUE, "Reference counter must be grater zero!", 3);
		
		ButtonConfirm setToKwH = new ButtonConfirm(vh.getParent(), "setTokWh"+id, req) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				int nonkWh = 0;
				for(ElectricityConnection ec: device.elConn().subPhaseConnections().getAllElements()) {
					ElectricEnergySensor energy = ec.energySensor();
					if(energy.reading().getUnit() != PhysicalUnit.KILOWATT_HOURS)
						nonkWh++;
				}
				if(nonkWh > 0) {
					setText("Set "+nonkWh+" phs kWh", req);
					enable(req);
				} else {
					setText("Recalc kWh", req);
					disable(req);
				}
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				int nonkWh = 0;
				for(ElectricityConnection ec: device.elConn().subPhaseConnections().getAllElements()) {
					ElectricEnergySensor energy = ec.energySensor();
					if(energy.reading().getUnit() == PhysicalUnit.KILOWATT_HOURS)
						continue;
					nonkWh++;
					RecordedData power = ec.powerSensor().reading().getHistoricalData();
					long now = appMan.getFrameworkTime();
					MeterReference ref = TimeSeriesServlet.getDefaultMeteringReference(power, null, appMan);
					List<SampledValue> newData = TimeSeriesServlet.getMeterFromConsumption(power, 0, now, ref, AggregationMode.Power2Meter);
					DataRecorder dataRecorder = appManPlus.dataRecorder();
					//RecordedDataStorage recStor = LogConfigSP.getRecordedData(energy.reading(), dataRecorder, null);
					LogConfigSP.storeData(newData, energy.reading(), dataRecorder);
					energy.reading().setUnit(PhysicalUnit.KILOWATT_HOURS);
				}
				System.out.println("Update "+nonkWh+" phases to kwH.");
			}
		};
		setToKwH.setConfirmMsg("Stop the Iotawatt driver. Then delete on console: find . -wholename \"*Iota*energySensor%2Freading/*slots\", see https://github.com/smartrplace/fendodb/wiki/Data-Manipulation-and-Import.", req);
		row.addCell("EnergyAsKwh", setToKwH);
	}
	
	@Override
	public String getTableTitle() {
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
