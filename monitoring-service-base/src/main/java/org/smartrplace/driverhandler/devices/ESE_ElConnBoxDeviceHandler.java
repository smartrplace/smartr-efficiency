package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.EnergyResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.ElectricEnergySensor;
import org.ogema.recordeddata.RecordedDataStorage;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSimpleProcUtil;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIDataBase;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIMgmt;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class ESE_ElConnBoxDeviceHandler extends DeviceHandlerBase<ElectricityConnectionBox> {
	private final ApplicationManagerPlus appMan;
	//private final TimeseriesSimpleProcUtil util;
	private final VirtualSensorKPIMgmt utilAggFull;
	
	//TODO: We have to close the listeners when the calling bundle closes
	public static class EnergyAccumulationDpDataBase {
		public ResourceValueListener<EnergyResource> aggListener;
		public Datapoint evalDp;		
		public Datapoint resourceDp;
	}
	public static class EnergyAccumulationDpData extends EnergyAccumulationDpDataBase {
		public RecordedDataStorage recStor;
		
	}
	/*public static class EnergyAccumulationData {
		public EnergyAccumulationData(TimeseriesSimpleProcUtil util, DataRecorder dataRecorder, Logger logger) {
			this.util = util;
			this.dataRecorder = dataRecorder;
			this.logger = logger;
		}
		private final TimeseriesSimpleProcUtil util;
		private final DataRecorder dataRecorder;
		private final Logger logger;
		/// Source EnergyResource location -> data for accumulation
		private Map<String, EnergyAccumulationDpDataBase> dpData = new HashMap<>();
	}*/
	
	public ESE_ElConnBoxDeviceHandler(ApplicationManagerPlus appMan) {
		this.appMan = appMan;
		utilAggFull = new VirtualSensorKPIMgmt(
				new TimeseriesSimpleProcUtil(appMan.appMan(), appMan.dpService()), appMan.getLogger(), appMan.dpService()) {
			@Override
			public VirtualSensorKPIDataBase getDatapointDataAccumulationSingle(Datapoint dpSource,
					String newSubResName, Resource device,
					Long intervalToStayBehindNow,
					boolean registerGovernedSchedule,
					boolean registerRemoteScheduleViaHeartbeat,
					List<Datapoint> result) {
				VirtualSensorKPIDataBase mapData = getDatapointData(dpSource, newSubResName, device,
						intervalToStayBehindNow>=0?intervalToStayBehindNow:15*TimeProcUtil.MINUTE_MILLIS,
						registerGovernedSchedule, registerRemoteScheduleViaHeartbeat, result);
				return mapData;
			}
			
			@Override
			public SingleValueResource getAndConfigureValueResourceSingle(Datapoint dpSource, VirtualSensorKPIDataBase mapData,
					String newSubResName, Resource device) {
				ElectricityConnection conn = (ElectricityConnection) device;
				EnergyResource energyDailyRealAgg = conn.getSubResource(newSubResName, ElectricEnergySensor.class).reading();
				energyDailyRealAgg.getSubResource("unit", StringResource.class).<StringResource>create().setValue("kWh");
				energyDailyRealAgg.getParent().activate(true);
				
				dpSource.info().setAggregationMode(AggregationMode.Consumption2Meter);
				mapData.evalDp = tsProcUtil.processSingle(TimeProcUtil.METER_EVAL, dpSource);
				//If the datapoint requires absoluteTiming, set it here
				mapData.absoluteTiming = null;
				
				return energyDailyRealAgg;
			}
		};
				//new TimeseriesSimpleProcUtil(appMan.appMan(), appMan.dpService()), appMan.dataRecorder(), appMan.getLogger(), appMan.dpService());
	}
	
	@Override
	public Class<ElectricityConnectionBox> getResourceType() {
		return ElectricityConnectionBox.class;
	}

	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert,
			InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector, this) {
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

				final ElectricityConnectionBox box =
						(ElectricityConnectionBox) addNameWidget(object, vh, id, req, row, appMan);

				Room deviceRoom = box.location().room();
				//Label lastContact = null;

				ElectricityConnection cc = box.connection();
				EnergyResource energyDaily = cc.getSubResource("energyDaily", ElectricEnergySensor.class).reading();
				if(energyDaily.isActive() && req != null) {
					//addPowerEnergySensor(cc, vh, id, req, row);
					Label power = vh.floatLabel("Energy15min", // (" + ResourceUtils.getHumanReadableShortName(c) + ")",
							id, energyDaily, row, "%.1f");
					Label lastContact = addLastContact(vh, id, req, row,
							energyDaily);
					power.setPollingInterval(DEFAULT_POLL_RATE, req);
					lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
					vh.floatEdit("RefTimeCounter", id, energyDaily.getSubResource("refTimeCounter", FloatResource.class), row, alert,
							0, Float.MAX_VALUE, "Reference counter must be grater zero!", 3);
				} else {
					vh.registerHeaderEntry("Energy15min");
					vh.registerHeaderEntry("Last Contact");
					vh.registerHeaderEntry("RefTimeCounter");
				}
				//else for (ElectricityConnection c : box.getSubResources(ElectricityConnection.class, true)) {
				//	if(c.powerSensor().isActive()) {
				//		addPowerEnergySensor(c, vh, id, req, row);
				//		break;
				//	}
				//}

				addRoomWidget(vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object, vh, id, req, row);
				addInstallationStatus(object, vh, id, req, row);
				addComment(object, vh, id, req, row);

				appSelector.addWidgetsExpert(null, object, vh, id, req, row, appMan);
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return ESE_ElConnBoxDeviceHandler.this.getResourceType();
			}
			
			@Override
			protected String id() {
				return ESE_ElConnBoxDeviceHandler.this.id();
			}

			@Override
			public String getTableTitle() {
				return "Energy Server Electricity Meters";
			}
			
			/*protected void addPowerEnergySensor(ElectricityConnection c, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row) {   
				Label voltage = vh.floatLabel("Voltage", // (" + ResourceUtils.getHumanReadableShortName(c) + ")",
						id, c.voltageSensor().reading(), row, "%.1f");
				Label power = vh.floatLabel("Power", // (" + ResourceUtils.getHumanReadableShortName(c) + ")",
						id, c.powerSensor().reading(), row, "%.1f");
				Label lastContact = addLastContact(vh, id, req, row,
							c.voltageSensor().reading());
				if (req != null) {
					voltage.setPollingInterval(DEFAULT_POLL_RATE, req);
					power.setPollingInterval(DEFAULT_POLL_RATE, req);
					lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
				}

			}*/
		};
	}
	

	@Override
	protected Class<? extends ResourcePattern<ElectricityConnectionBox>> getPatternClass() {
		return ESE_ElConnBoxPattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}

	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService) {
		ElectricityConnectionBox dev = (ElectricityConnectionBox) installDeviceRes.device();
		return getDatapointsStatic(dev.connection(), dpService, utilAggFull);
	}
	public static List<Datapoint> getDatapointsStatic(ElectricityConnection connection, DatapointService dpService) {
		return getDatapointsStatic(connection, dpService, null);
	}
	/** Search for datapoints in an {@link ElectricityConnection}
	 * 
	 * @param connection
	 * @param dpService
	 * @param util set this to null if a special resource energyDaily shall NOT be summed up into a real
	 * 		metering resource (see {@link #provideAccumulatedDatapoint(String, EnergyResource, Datapoint, List, ElectricityConnection, DatapointService, EnergyAccumulationData)}).
	 * 		Not relevant if such a special decorator sub resource does not exist.
	 * @return
	 */
	public static List<Datapoint> getDatapointsStatic(ElectricityConnection connection, DatapointService dpService,
			VirtualSensorKPIMgmt util) {
		List<Datapoint> result = new ArrayList<>();
		addConnDatapoints(result, connection, dpService, util);
		for(ElectricityConnection subConn: connection.subPhaseConnections().getAllElements()) {
			addConnDatapoints(result, subConn, subConn.getName(), dpService);			
		}
		//TODO: Workaround
		for(int i=1; i<=3; i++) {
			ElectricityConnection subConn = connection.getSubResource("L"+i, ElectricityConnection.class);
			addConnDatapoints(result, subConn, subConn.getName(), dpService);			
		}
		
		return result;
	}
	
	protected static void addConnDatapoints(List<Datapoint> result, ElectricityConnection conn, DatapointService dpService,
			VirtualSensorKPIMgmt util) {
		//addDatapoint(conn.voltageSensor().reading(), result, dpService);
		addDatapoint(conn.powerSensor().reading(), result, dpService);
		addDatapoint(conn.energySensor().reading(), result, dpService);
		EnergyResource energyDaily = conn.getSubResource("energyDaily", ElectricEnergySensor.class).reading();
		Datapoint dp = addDatapoint(energyDaily, result, dpService);
		provideAccumulatedDatapoint2("energyDailyAccumulatedFull", dp, result, conn, dpService, util);
		//dp = addDatapoint(conn.getSubResource("energyReactiveDaily", ElectricEnergySensor.class).reading(), result, dpService);
		//provideAccumulatedDatapoint("energyReactiveAccumulatedDailyFull", energyDaily, dp, result, conn, dpService, util);
		addDatapoint(conn.getSubResource("energyAccumulatedDaily", ElectricEnergySensor.class).reading(), result, dpService);
		addDatapoint(conn.getSubResource("energyReactiveAccumulatedDaily", ElectricEnergySensor.class).reading(), result, dpService);
		addDatapoint(conn.getSubResource("billedEnergy", ElectricEnergySensor.class).reading(), result, dpService);
		addDatapoint(conn.getSubResource("billedEnergyReactive", ElectricEnergySensor.class).reading(), result, dpService);
		addDatapoint(conn.currentSensor().reading(), result, dpService);
		addDatapoint(conn.voltageSensor().reading(), result, dpService);
		addDatapoint(conn.frequencySensor().reading(), result, dpService);		
		addDatapoint(conn.reactivePowerSensor().reading(), result, dpService);
		addDatapoint(conn.reactiveAngleSensor().reading(), result, dpService);
	}
	
	/** Generate a new datapoint AND FloatResource for an evaluation result.<br>
	 * The method can be considered a template and provides the functionality of adding up
	 * single consumption values (that follow the definition of Consumption2Meter) into a real
	 * meter value with a constantly increading meter value
	 * @param newSubResName
	 * @param energyDailySource
	 * @param dpSource
	 * @param result
	 * @param conn
	 * @param dpService
	 * @param util set util.dataRecorder to null if historical result data shall not directly be written into
	 * 		the slotsDB of the new EnergyResource
	 */
	protected static void provideAccumulatedDatapoint2(String newSubResName,
			Datapoint dpSource,
			List<Datapoint> result, ElectricityConnection conn, DatapointService dpService,
			VirtualSensorKPIMgmt util) {
		if(dpSource != null && util != null) {
			final VirtualSensorKPIDataBase mapData1 = util.getDatapointDataAccumulationSingle(dpSource, newSubResName, conn,
					15*TimeProcUtil.MINUTE_MILLIS, false, true, result);
		}
	}
	
/*	protected static void provideAccumulatedDatapoint(String newSubResName,
			EnergyResource energyDailySource, Datapoint dpSource,
			List<Datapoint> result, ElectricityConnection conn, DatapointService dpService,
			EnergyAccumulationData util) {
		if(dpSource != null && util != null) {
			final EnergyAccumulationDpData mapData1 = (EnergyAccumulationDpData) util.dpData.get(energyDailySource.getLocation());
			if(mapData1 != null) {
				result.add(mapData1.resourceDp);
				return;
			}
			final EnergyAccumulationDpData mapData = new EnergyAccumulationDpData();
			util.dpData.put(energyDailySource.getLocation(), mapData);
			//add fully accumulated
			dpSource.info().setAggregationMode(AggregationMode.Consumption2Meter);
			mapData.evalDp = util.util.processSingle(TimeProcUtil.METER_EVAL, dpSource);
			EnergyResource energyDailyRealAgg = conn.getSubResource(newSubResName, ElectricEnergySensor.class).reading();
			energyDailyRealAgg.getSubResource("unit", StringResource.class).<StringResource>create().setValue("kWh");
			energyDailyRealAgg.getParent().activate(true);
			mapData.resourceDp = dpService.getDataPointStandard(energyDailyRealAgg);
			//final ReadOnlyTimeSeries accTs = mapData.accRes1.getTimeSeries();
			//accRes.setTimeSeries(accTs);
			result.add(mapData.resourceDp);
			//final RecordedDataStorage recStor;
			if(util.dataRecorder != null) {
util.logger.info("   Starting Accumlated full for:"+energyDailyRealAgg.getLocation());
				mapData.recStor = LogConfigSP.getRecordedData(energyDailyRealAgg, util.dataRecorder, null);
				if(mapData.recStor != null) {
util.logger.info("   Starting Accumlated full Recstor size(1):"+mapData.recStor.size());
List<SampledValue> allVals = mapData.recStor.getValues(0);
					long now = dpService.getFrameworkTime();
					SampledValue lastVal = mapData.recStor.getPreviousValue(now+1);
util.logger.info("   Starting Accumlated found previous accFull slotsDB value: "+
	((lastVal != null)?StringFormatHelper.getFullTimeDateInLocalTimeZone(lastVal.getTimestamp()):"NONE"));
					final long start;
					if(lastVal == null)
						start = 0;
					else if(lastVal.getTimestamp() < now)
						start = lastVal.getTimestamp()+1;
					else
						start = -1;
					if(start >= 0) {
						LoggingUtils.activateLogging(mapData.recStor, -2);
						ReadOnlyTimeSeries accTs = mapData.evalDp.getTimeSeries();
						((ProcessedReadOnlyTimeSeries2)accTs).setUpdateLastTimestampInSourceOnEveryCall(true);
						List<SampledValue> values = accTs.getValues(start, now+1);
util.logger.info("   Found new vals:"+values.size()+" Checked from "+StringFormatHelper.getFullTimeDateInLocalTimeZone(start));
if(!values.isEmpty())
util.logger.info("   Before Inserting "+values.size()+" slotsDB values...");
						LogConfigSP.storeData(values, mapData.recStor);
util.logger.info("   Inserted "+values.size()+" slotsDB values, last:"+(values.isEmpty()?"NONE":""+values.get(values.size()-1).getValue().getFloatValue()));
					}
				}
			} else
				mapData.recStor = null;
util.logger.info("   Starting Accumlated full Recstor size(2):"+mapData.recStor.size());
			mapData.aggListener = new ResourceValueListener<EnergyResource>() {
				//long lastVal = 0;
				@Override
				public void resourceChanged(EnergyResource resource) {
util.logger.info("   In EnergyServer energyDaily onValueChanged:"+resource.getLocation());
					//we just have to perform a read to trigger an update
					long nowReal = dpService.getFrameworkTime();
					SampledValue lastSv = null;
					//if(lastVal <= 0) {
					long lastVal = 0;
					lastSv = mapData.recStor.getPreviousValue(nowReal+1);
					if(lastSv != null)
						lastVal = lastSv.getTimestamp();  
					//}
					ReadOnlyTimeSeries accTs = mapData.evalDp.getTimeSeries();
					((ProcessedReadOnlyTimeSeries2)accTs).setUpdateLastTimestampInSourceOnEveryCall(true);
					
					// We have to go back a little bit with the end to make sure the value is really in. Otherwise it will not be read
					// again as the logic assumes that the time series does not change after now.
					//
					long now = nowReal - 15*TimeProcUtil.MINUTE_MILLIS;
					if(lastVal >= now)
						return;
					List<SampledValue> svs = accTs.getValues(lastVal+1, now+1);
util.logger.info("   In EnergyServer energyDaily onValueChanged: Found new vals:"+svs.size()+" Checked from "+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastVal));
if(!svs.isEmpty())
	util.logger.info("   Last value written at: "+StringFormatHelper.getFullTimeDateInLocalTimeZone(svs.get(svs.size()-1).getTimestamp()));
					SampledValue lastTs = LogConfigSP.storeData(svs, mapData.recStor, 20*TimeProcUtil.MINUTE_MILLIS);
					if(lastTs != null) {
						((ProcessedReadOnlyTimeSeries2)accTs).resetKnownEnd(lastTs.getTimestamp(), false);
						svs = accTs.getValues(lastTs.getTimestamp()+1, now+1);
						SampledValue lastTs2 = LogConfigSP.storeData(svs, mapData.recStor, 20*TimeProcUtil.MINUTE_MILLIS);
						if(lastTs2 != null) {
							util.logger.warn("   Could not fill gap behind: "+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastTs2.getTimestamp()));
							if((now - lastTs2.getTimestamp()) > 2*TimeProcUtil.HOUR_MILLIS) {
								//store anyways to bridge a gap that will not fill anymore
								LogConfigSP.storeData(svs, mapData.recStor, Long.MAX_VALUE);
							}
						}
					}
					for(SampledValue sv: svs) {
						energyDailyRealAgg.setValue(sv.getValue().getFloatValue());
					}
					util.logger.info("OnValueChanged Summary for "+energyDailyRealAgg.getLocation()+":\r\n"+
							(lastSv!=null?"Found existing last SampledValue in SlotsDB at "+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastSv.getTimestamp()):"")+
							",\r\n Calculated values for DP"+mapData.evalDp.getLocation()+" from "+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastVal+1)+" to "+StringFormatHelper.getFullTimeDateInLocalTimeZone(now+1)+
							",\r\n Found "+svs.size()+" new values. Wrote into "+mapData.recStor.getPath()); //+
							//".\r\n Set lastVal to "+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastVal));
				}
			};
			energyDailySource.addValueListener(mapData.aggListener, true);
			util.logger.info("   Starting Accumlated full Recstor size(3):"+mapData.recStor.size());
		}
	}*/
	protected static void addConnDatapoints(List<Datapoint> result, ElectricityConnection conn,
			String ph, DatapointService dpService) {
		addDatapoint(conn.powerSensor().reading(), result, ph, dpService);
		addDatapoint(conn.energySensor().reading(), result, ph, dpService);
		addDatapoint(conn.currentSensor().reading(), result, ph, dpService);
		addDatapoint(conn.voltageSensor().reading(), result, ph, dpService);
		addDatapoint(conn.frequencySensor().reading(), result, ph, dpService);		
		addDatapoint(conn.reactivePowerSensor().reading(), result, ph, dpService);
		addDatapoint(conn.reactiveAngleSensor().reading(), result, ph, dpService);			
	}
	
	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		ElectricityConnectionBox device = (ElectricityConnectionBox) appDevice.device();
		AlarmingUtiH.setTemplateValues(appDevice, device.connection().powerSensor().reading(),
				0.0f, 9999999.0f, 10, 20);
		//AlarmingUtiH.addAlarmingMQTT(device, appDevice);
	}

}
