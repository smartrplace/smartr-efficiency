package org.ogema.devicefinder.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.devicefinder.api.AlarmingService;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointInfo.UtilityType;
import org.ogema.devicefinder.api.DatapointInfoProvider;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.api.DpConnection;
import org.ogema.devicefinder.api.GatewayResource;
import org.ogema.devicefinder.api.InstallationProgressService;
import org.ogema.devicefinder.api.TimedJobMgmtService;
import org.ogema.devicefinder.api.VirtualScheduleService;
import org.ogema.devicefinder.util.AlarmingServiceImpl;
import org.ogema.devicefinder.util.DPRoomImpl;
import org.ogema.devicefinder.util.DatapointGroupImpl;
import org.ogema.devicefinder.util.DatapointImpl;
import org.ogema.devicefinder.util.DpConnectionImpl;
import org.ogema.messaging.api.MessageTransport;
import org.ogema.model.gateway.EvalCollection;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.GenericFloatSensor;
import org.ogema.tools.app.useradmin.api.UserDataAccess;
import org.ogema.tools.resource.util.ResourceUtils;
import org.osgi.service.cm.ConfigurationAdmin;
import org.smartrplace.alarming.escalation.util.EscalationManagerI;
import org.smartrplace.alarming.escalation.util.EscalationProvider;
import org.smartrplace.app.monservice.MonitoringServiceBaseApp;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.dpres.SensorDeviceDpRes;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatUtil;
import org.smartrplace.autoconfig.api.OSGiConfigAccessService;
import org.smartrplace.autoconfig.api.OSGiConfigAccessServiceImpl;
import org.smartrplace.tissue.util.resource.ResourceHelperSP;
import org.smartrplace.tissue.util.resource.ValueResourceHelperSP;
import org.smartrplace.util.frontend.servlet.UserServletUtil;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper.RecIdVal;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper.TypeChecker;
import de.iwes.util.logconfig.EvalHelper;
import de.iwes.util.logconfig.LogHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Implementation of central Data Point Service
 * TODO: Currently identification and generation of data row information for plots is done via an 
 * EvaluationProvider. So this information does not contain any Datapoint information yet.
 * Goals for Plotting:
 *   > Offer plots that have content (if no ElectricityConsumption, WaterConsumption info is there, the respective plot should not
 *   be offered at all)
 *   > On the other hand drivers and their extensions should be able to provide all information to offer a plot for the data and
 *   to include the data into an "all data plot" for a room etc.
 *   > Drivers or their extensions should also be able to provide {@link AggregationMode} information etc.
 *   
 *   It is not possible to add own Datapoint instances into the map of known datapoints, you have to get these
 *   object via {@link #getDataPointStandard(String)} and its variants. You can add your
 *   own DatapointpointInfoProvider via
 *   {@link Datapoint#registerInfoProvider(DatapointInfoProvider, int)}, though. This has not
 *   been tested, though. You can still use own instances of {@link DatapointImpl} or own implementations
 *   if it is not required to make them accessible via the {@link DatapointService}.
 */

//@Service(DatapointService.class)
//@Component
public abstract class DatapointServiceImpl implements DatapointService {
	protected final MonitoringServiceBaseApp baseApp;
	
	public DatapointServiceImpl(ApplicationManager appMan, ConfigurationAdmin configAdmin, TimedJobMgmtService timedJobApp,
			MonitoringServiceBaseApp baseApp) {
		this.configAdmin = configAdmin;
		this.appMan = appMan;
		this.timedJobMan = timedJobApp;
		this.baseApp = baseApp;
	}

	protected abstract Map<String, DeviceHandlerProvider<?>> getTableProviders();
	
	//private final MonitoringServiceBaseController controller;
	private final ApplicationManager appMan;
	private final ConfigurationAdmin configAdmin;
	private ResourceList<InstallAppDevice> installAppListInternal = null;
	@SuppressWarnings("unchecked")
	private ResourceList<InstallAppDevice> installAppList() {
		if(installAppListInternal == null) {
			installAppListInternal = ResourceHelperSP.getSubResource(null, "hardwareInstallConfig/knownDevices",
					ResourceList.class, appMan.getResourceAccess());			
		}
		return installAppListInternal;
	}
	
	/** GatewayId -> Resource-location -> Datapoint object*/
	protected final Map<String, Map<String, Datapoint>> knownDps = new HashMap<>();
	protected final Map<String, Map<String, Datapoint>> knownAliases = new HashMap<>();
	
	/** GatewayId -> GatewayResource-location -> GatewayResource object*/
	protected final Map<String, Map<String, GatewayResource>> knownGWRes = new HashMap<>();

	/** GatewayId -> Connection-location -> Connection object*/
	protected final Map<String, Map<String, DpConnection>> knownConnections = new HashMap<>();

	/** Group-Id -> Group object*/
	protected final Map<String, DatapointGroup> knownGroups = new HashMap<>();
	
	protected final AlarmingService alarming = new AlarmingServiceImpl();
	
	@Override
	public Datapoint getDataPointStandard(String resourceLocation) {
		return getDataPointStandard(resourceLocation, GaRoMultiEvalDataProvider.LOCAL_GATEWAY_ID);
	}

	@Override
	public Datapoint getDataPointAsIs(String resourceLocation) {
		return getDataPointAsIs(resourceLocation, GaRoMultiEvalDataProvider.LOCAL_GATEWAY_ID);
	}

	@Override
	public Datapoint getDataPointStandard(String resourceLocation, String gatewayId) {
		if(gatewayId == null)
			gatewayId = GaRoMultiEvalDataProvider.LOCAL_GATEWAY_ID;
		Datapoint result = getDataPointAsIs(resourceLocation, gatewayId);
		if(result == null) {
			result = new DatapointImpl(resourceLocation, gatewayId, null, null, this) {
				@Override
				public boolean setRoom(DPRoom room) {
					if(room != null)
						setStructure(room, room.id(), gatewayId);
					return super.setRoom(room);
				}
				@Override
				public GenericFloatSensor registerAsVirtualSensor() {
					return registerAsVirtualSensor(null);
				}
				@Override
				public GenericFloatSensor registerAsVirtualSensor(String sensorDeviceName) {
					return DatapointServiceImpl.this.registerAsVirtualSensor(this, sensorDeviceName);
				}
				@Override
				protected DpConnection getConnection(String connectionLocation, UtilityType type) {
					return DatapointServiceImpl.this.getConnectionForDp(connectionLocation, type, getGatewayId());
				}
				@Override
				public boolean addAlias(String alias) {
					registerAlias(gatewayId, this, alias);
					return super.addAlias(alias);
				}
			};
			Map<String, Datapoint> gwMap = getGwMap(gatewayId);
			gwMap.put(resourceLocation, result);
		}
		addStandardData(result);
		return result;
	}

	@Override
	public Datapoint getDataPointAsIs(String resourceLocation, String gatewayId) {
		if(gatewayId != null)
			gatewayId = ViaHeartbeatUtil.getBaseGwId(gatewayId);
		else 
			gatewayId = GaRoMultiEvalDataProvider.LOCAL_GATEWAY_ID;

		Map<String, Datapoint> subMap = knownDps.get(gatewayId);
		if(subMap == null) {
			return getDataPointAsIsFromAlias(resourceLocation, gatewayId);
		}
		Datapoint result = subMap.get(resourceLocation);
		if(result == null)
			return getDataPointAsIsFromAlias(resourceLocation, gatewayId);
		return result;
	}

	protected Datapoint getDataPointAsIsFromAlias(String resourceLocation, String gatewayId) {
		Map<String, Datapoint> subMap = knownAliases.get(gatewayId);
		if(subMap == null)
			return null;
		return subMap.get(resourceLocation);
		
	}
	
	protected Map<String, Datapoint> getGwMapLocal() {
		return getGwMap(GaRoMultiEvalDataProvider.LOCAL_GATEWAY_ID);
	}
	protected Map<String, Datapoint> getGwMap(String gatewayId) {
		if(gatewayId != null)
			gatewayId = ViaHeartbeatUtil.getBaseGwId(gatewayId);
		Map<String, Datapoint> subMap = knownDps.get(gatewayId);
		if(subMap == null) {
			subMap = new HashMap<String, Datapoint>();
			knownDps.put(gatewayId, subMap);
		}
		return subMap;
	}
	
	protected void registerAlias(String gatewayId, Datapoint dp, String alias) {
		if(gatewayId != null)
			gatewayId = ViaHeartbeatUtil.getBaseGwId(gatewayId);
		Map<String, Datapoint> subMap = knownAliases.get(gatewayId);
		if(subMap == null) {
			subMap = new HashMap<String, Datapoint>();
			knownAliases.put(gatewayId, subMap);
		}
		subMap.put(alias, dp);
	}
	
	public static void addStandardData(Datapoint result) {
		DatapointImpl.addStandardData(result);
		GaRoDataTypeI type = result.getGaroDataType();
		if(type != null) {
			typeIdsRegistered.add(type.label(null));
			typeIdsKnown.add(type.label(null));
			if(type instanceof GaRoDataType)
				GaRoTypeStringConfigProviderDP.typesWithoutDescription.put(type.label(null), (GaRoDataType) type);
		}
	}
	public static void addStandardData(Datapoint result, ValueResource valRes) {
		addStandardData(result);
		SingleValueResource svr;
		if(valRes instanceof SingleValueResource)
			svr = (SingleValueResource) valRes;
		else
			svr = null;
		if(result.getTimeSeriesID() == null && svr != null) {
			RecordedData recData = ValueResourceHelperSP.getRecordedData(svr);
			//if(LoggingUtils.isLoggingEnabled(svr)) {
			if(recData != null && (!recData.isEmpty())) {	
				String id = UserServletUtil.getOrAddTimeSeriesData(recData, recData.getPath());
				result.setTimeSeriesID(id);
			}
		}
	}
		
	@Override
	public Datapoint getDataPointStandard(ValueResource valResIn) {
		ValueResource valRes = valResIn.getLocationResource();
		if(valRes == null)
			valRes = valResIn;
		Datapoint result = getDataPointAsIs(valRes);
		if(result == null) {
			result = new DatapointImpl(valRes.getLocation(), null, valRes, null, this) {
				@Override
				public boolean setRoom(DPRoom room) {
					if(room != null)
						setStructure(room, room.id(), gatewayId);
					return super.setRoom(room);
				}
				
				@Override
				public GenericFloatSensor registerAsVirtualSensor() {
					return registerAsVirtualSensor(null);
				}
				@Override
				public GenericFloatSensor registerAsVirtualSensor(String sensorDeviceName) {
					return DatapointServiceImpl.this.registerAsVirtualSensor(this, sensorDeviceName);
				}
				@Override
				protected DpConnection getConnection(String connectionLocation, UtilityType type) {
					return DatapointServiceImpl.this.getConnectionForDp(connectionLocation, type, getGatewayId());
				}
				@Override
				public boolean addAlias(String alias) {
					registerAlias(gatewayId, this, alias);
					return super.addAlias(alias);
				}
			};
			Map<String, Datapoint> gwMap = getGwMap(GaRoMultiEvalDataProvider.LOCAL_GATEWAY_ID);
			gwMap.put(valRes.getLocation(), result);
		} else if(result.getResource() == null)
			((DatapointImpl)result).setResource(valRes);
		addStandardData(result, valRes);
		return result;
	}

	protected GenericFloatSensor registerAsVirtualSensor(DatapointImpl dp, String sensorDeviceName) {
		EvalCollection ec = EvalHelper.getEvalCollection(appMan);
		String devName;
		if(sensorDeviceName == null)
			devName = "virtualDatapointDeviceDefault";
		else
			devName = ResourceUtils.getValidResourceName(sensorDeviceName);
		SensorDeviceDpRes sensDev = ec.getSubResource(devName, (SensorDeviceDpRes.class));
		String loc =ResourceUtils.getValidResourceName(dp.getLocation());
		GenericFloatSensor sensRes = sensDev.sensors().getSubResource(loc, GenericFloatSensor.class);
		if(!sensRes.isActive()) {
			sensRes.reading().create();
			dp.setTimeSeries(sensRes.reading().getHistoricalData());
			if(!sensDev.isActive())
				sensDev.activate(true);
			else
				sensRes.activate(true);
		} else if(dp.getTimeSeries() == null)
			dp.setTimeSeries(sensRes.reading().getHistoricalData());
		return sensRes;
	}
	
	protected DpConnection getConnectionForDp(String connectionLocation, UtilityType type, String gatewayId) {
		if(gatewayId == null)
			gatewayId = GaRoMultiEvalDataProvider.LOCAL_GATEWAY_ID;
		Map<String, DpConnection> subData = knownConnections.get(gatewayId);
		if(subData == null) {
			subData = new HashMap<>();
			knownConnections.put(gatewayId, subData);
		}
		DpConnection result = subData.get(connectionLocation);
		if(result != null) {
			return result;
		}
		switch(type) {
		case ELECTRICITY:
			result = new DpConnectionImpl.DpElectricityConnectionImpl(connectionLocation);
			break;
		case HEAT_ENERGY:
			result = new DpConnectionImpl.DpThermalConnectionImpl(connectionLocation);
			break;
		case WATER:
			result = new DpConnectionImpl.DpFreshWaterConnectionImpl(connectionLocation);
			break;
		case FOOD:
			result = new DpConnectionImpl.DpFoodConnectionImpl(connectionLocation);
			break;
		default:
			throw new IllegalStateException("Unknown utility type:"+type);
		}
		
		subData.put(connectionLocation, result);
		return result;
	}


	@Override
	public Datapoint getDataPointAsIs(ValueResource valRes) {
		Datapoint result = getDataPointAsIs(valRes.getLocation());
		if(result != null && result.getResource() == null)
			((DatapointImpl)result).setResource(valRes.getLocationResource());
		return result;
	}

	@Override
	public List<Datapoint> getAllDatapoints() {
		List<Datapoint> result = new ArrayList<Datapoint>();
		for(Map<String, Datapoint> baselist: knownDps.values()) {
			result.addAll(baselist.values());
		}
		return result;
	}
	@Override
	public Collection<Datapoint> getAllDatapoints(String gwId) {
		String gwToUse = ViaHeartbeatUtil.getBaseGwId(gwId);
		Map<String, Datapoint> subMap = knownDps.get(gwToUse);
		if(subMap == null)
			return Collections.emptyList();
		return subMap.values();
	}

	static Map<DataTypeRegistrationStatus, Collection<String>> typeIds = new HashMap<>();
	static Set<String> typeIdsKnown = new LinkedHashSet<String>(GaRoEvalHelper.recIdSnippets.keySet());
	static Set<String> typeIdsEvalDefault = new LinkedHashSet<String>();
	static Set<String> typeIdsRegistered = new LinkedHashSet<String>();
	static {
		for(GaRoDataType t: GaRoDataType.standardEvalTypes) {
			typeIdsEvalDefault.add(t.label(null));
		}
		typeIdsKnown.addAll(typeIdsEvalDefault);
		typeIds.put(DataTypeRegistrationStatus.ALL, typeIdsKnown);
		typeIds.put(DataTypeRegistrationStatus.EVAL_DEFAULT, typeIdsEvalDefault);
		typeIds.put(DataTypeRegistrationStatus.REGISTERED, typeIdsRegistered);
		typeIds.put(DataTypeRegistrationStatus.FOR_EVAL, GaRoTypeStringConfigProviderDP.typeIdsForEval);
	}
	@Override
	public List<GaRoDataType> getRegisteredDataTypes(DataTypeRegistrationStatus filter) {
		Map<String, RecIdVal> full = getDataTypeDescriptions(filter, true);
		List<GaRoDataType> result = new ArrayList<>();
		for(RecIdVal desc: full.values()) {
			result.add(desc.type);
		}
		return result ;
	}

	@Override
	public Map<String, RecIdVal> getDataTypeDescriptions(DataTypeRegistrationStatus filter,
			boolean includeEmptyDescriptions) {
		Collection<String> relevantIds = typeIds.get(filter);
		Map<String, RecIdVal> result = new LinkedHashMap<>();
		for(String id: relevantIds) {
			RecIdVal rec = GaRoTypeStringConfigProviderDP.recIdSnippets.get(id);
			if(rec == null) {
				if(includeEmptyDescriptions) {
					GaRoDataType type = GaRoTypeStringConfigProviderDP.typesWithoutDescription.get(id);
					if(type == null) {
						//very ugly, should not occur
						type = new GaRoDataType(id, null);
					}
					Map<OgemaLocale, String> label = new HashMap<OgemaLocale, String>();
					label.put(OgemaLocale.ENGLISH, "unknown:"+type.label(null));
					result.put(id, new RecIdVal(type, (String[])null, label));
				}
				continue;
			}
			result.put(id, rec);
		}
		return result ;
	}

	@Override
	public RecIdVal addDataTypeDescription(GaRoDataType type, List<String> snippets, String labelEnglish,
			boolean registerForEvalution) {
		Map<OgemaLocale, String> labels = new HashMap<OgemaLocale, String>();
		labels.put(OgemaLocale.ENGLISH, labelEnglish);
		return addDataTypeDescription(type, snippets, labels);
	}

	@Override
	public RecIdVal addDataTypeDescription(GaRoDataType type, List<String> snippets, Map<OgemaLocale, String> labels) {
		RecIdVal rec = new RecIdVal(type, snippets, labels);
		return addDataTypeDescription(rec);
	}

	@Override
	public RecIdVal addDataTypeDescription(GaRoDataType type, TypeChecker typeChecker, String labelEnglish,
			boolean registerForEvalution) {
		Map<OgemaLocale, String> labels = new HashMap<OgemaLocale, String>();
		labels.put(OgemaLocale.ENGLISH, labelEnglish);
		return addDataTypeDescription(type, typeChecker, labels);
	}

	@Override
	public RecIdVal addDataTypeDescription(GaRoDataType type, TypeChecker typeChecker,
			Map<OgemaLocale, String> labels) {
		RecIdVal rec = new RecIdVal(type, typeChecker, labels);
		return addDataTypeDescription(rec);
	}

	@Override
	public RecIdVal addDataTypeDescription(RecIdVal recIdVal) {
		RecIdVal existing = GaRoTypeStringConfigProviderDP.recIdSnippets.get(recIdVal.type.label(null));
		RecIdVal result;
		if(existing == null) {
			result = recIdVal;
			GaRoTypeStringConfigProviderDP.recIdSnippets.put(recIdVal.type.label(null), recIdVal);
		} else {
			result = existing;
			if(recIdVal.snippets != null) {
				if(existing.snippets != null)
					existing.snippets.addAll(recIdVal.snippets);
				else
					existing.snippets = recIdVal.snippets;
			}			
			if(recIdVal.typeChecker != null) {
				//higher priority
				existing.typeChecker = recIdVal.typeChecker;
			}			
		}
		typeIdsKnown.add(recIdVal.type.label(null));
		return result;
	}

	@Override
	public RecIdVal registerTypeForEvaluation(GaRoDataType type) {
		typeIdsKnown.add(type.label(null));
		GaRoTypeStringConfigProviderDP.typeIdsForEval.add(type.label(null));
		RecIdVal existing = GaRoTypeStringConfigProviderDP.recIdSnippets.get(type.label(null));
		return existing;
	}

	@Override
	public Collection<Class<? extends Resource>> getManagedDeviceResourceTypes(boolean basedOnDeviceHandlers) {
		Set<Class<? extends Resource>> result = new HashSet<>();
		if(basedOnDeviceHandlers) {
			for(DeviceHandlerProvider<?> prov: getTableProviders().values()) {
				result.add(prov.getResourceType());
			}
			return result;
		}
		if(installAppList() != null) for(InstallAppDevice iad: installAppList().getAllElements()) {
			if(iad.isTrash().getValue())
				continue;
			Class<? extends Resource> resType = iad.device().getResourceType();
			result.add(resType);
		}
		return result;
	}

	@Override
	public Collection<InstallAppDevice> managedDeviceResoures(Class<? extends Resource> resourceType) {
		if(resourceType == null && installAppList() != null)
			return installAppList().getAllElements();
		List<InstallAppDevice> result = new ArrayList<>();
		if(installAppList() != null) for(InstallAppDevice iad: installAppList().getAllElements()) {
			if(iad.isTrash().getValue())
				continue;
			if(resourceType.isAssignableFrom(iad.device().getResourceType()))
				result.add(iad);
		}
		return result;
	}

	@Override
	public Collection<InstallAppDevice> managedDeviceResoures(String deviceHandlerId, boolean shortId) {
		return managedDeviceResoures(deviceHandlerId, shortId, false);
	}		
	@Override
	public Collection<InstallAppDevice> managedDeviceResoures(String deviceHandlerId, boolean shortId,
			boolean returnAlsoTrash) {
		if(returnAlsoTrash && (deviceHandlerId == null) && (installAppList() != null))
			return installAppList().getAllElements();
		List<InstallAppDevice> result = new ArrayList<>();
		if(installAppList() != null) for(InstallAppDevice iad: installAppList().getAllElements()) {
			if((!returnAlsoTrash) && iad.isTrash().getValue())
				continue;
			if(deviceHandlerId == null)
				result.add(iad);
			else if(shortId) {
				if(iad.devHandlerInfo().getValue().endsWith(deviceHandlerId))
					result.add(iad);
			} else {
				if(deviceHandlerId.equals(iad.devHandlerInfo().getValue()))
					result.add(iad);
			}
		}
		return result;
	}

	@Override
	public InstallAppDevice getMangedDeviceResource(PhysicalElement device) {
		if(installAppList() != null) for(InstallAppDevice iad: installAppList().getAllElements()) {
			if(device.equalsLocation(iad.device()))
				return iad;
		}
		return null;
	}
	
	@Override
	public InstallAppDevice getMangedDeviceResourceForSubresource(Resource subRes) {
		PhysicalElement device = LogHelper.getDeviceResource(subRes, true);
		if(device != null)
			return getMangedDeviceResource(device);
		device = LogHelper.getDeviceResource(subRes, false);
		if(device == null)
			return null;
		return getMangedDeviceResource(device);
	}
	
	@Override
	public InstallAppDevice getMangedDeviceResource(String deviceId) {
		if(installAppList() != null) for(InstallAppDevice iad: installAppList().getAllElements()) {
			if(deviceId.equals(iad.deviceId().getValue()))
				return iad;
		}
		return null;
	}

	@Override
	public <T extends Resource> DeviceHandlerProviderDP<T> getDeviceHandlerProvider(
			InstallAppDevice installAppDeviceRes) {
		String devHandId = installAppDeviceRes.devHandlerInfo().getValue();
		return getDeviceHandlerProvider(devHandId);
		/*for(DeviceHandlerProvider<?> prov: getTableProviders().values()) {
			if(prov.id().equals())
				return (DeviceHandlerProviderDP<T>) prov;
		}
		return null;*/
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Resource> DeviceHandlerProviderDP<T> getDeviceHandlerProvider(
			String devHandId) {
		for(DeviceHandlerProvider<?> prov: getTableProviders().values()) {
			if(prov.id().equals(devHandId))
				return (DeviceHandlerProviderDP<T>) prov;
		}
		return null;
		
	}

	@Override
	public List<DeviceHandlerProviderDP<?>> getDeviceHandlerProviders() {
		List<DeviceHandlerProviderDP<?>> result = new ArrayList<DeviceHandlerProviderDP<?>>();
		for(DeviceHandlerProvider<?> prov: getTableProviders().values()) {
			result.add(prov);
		}
		return result;
	}
	
	@Override
	public long getFrameworkTime() {
		return appMan.getFrameworkTime();
	}

	@Override
	public GatewayResource getStructure(String id, String gatewayId) {
		if(gatewayId != null)
			gatewayId = ViaHeartbeatUtil.getBaseGwId(gatewayId);
		Map<String, GatewayResource> subMap = knownGWRes.get(gatewayId);
		if(subMap == null)
			return null;
		return subMap.get(id);
	}
	
	@Override
	public List<GatewayResource> getAllStructures() {
		List<GatewayResource> result = new ArrayList<>();
		for(Map<String, GatewayResource> subMap: knownGWRes.values()) {
			result.addAll(subMap.values());
		}
		return result;
	}

	public void setStructure(GatewayResource gwRes, String id, String gatewayId) {
		if(gatewayId != null)
			gatewayId = ViaHeartbeatUtil.getBaseGwId(gatewayId);
		Map<String, GatewayResource> subMap = knownGWRes.get(gatewayId);
		if(subMap == null) {
			subMap = new HashMap<>();
			knownGWRes.put(gatewayId, subMap);
		}
		subMap.put(id, gwRes);
	}
	
	@Override
	public DPRoom getRoom(String id) {
		return getRoom(id, GaRoMultiEvalDataProvider.LOCAL_GATEWAY_ID);
	}
	
	@Override
	public DPRoom getRoom(String id, String gatewayId) {
		GatewayResource result = getStructure(id, gatewayId);
		if(result != null && !(result instanceof DPRoom))
			return null;
		if(result == null) {
			result = new DPRoomImpl(id);
			setStructure(result, id, gatewayId);
		}
		return (DPRoom) result;
	}
	
	@Override
	public List<DPRoom> getAllRooms() {
		List<DPRoom> result = new ArrayList<>();
		for(GatewayResource gwres: getAllStructures()) {
			if(gwres instanceof DPRoom)
				result.add((DPRoom) gwres);
		}
		return result;
	}
	
	@Override
	public List<DpConnection> getConnections(UtilityType type, String gatewayId) {
		if(gatewayId != null)
			gatewayId = ViaHeartbeatUtil.getBaseGwId(gatewayId);
		Map<String, DpConnection> subData = knownConnections.get(gatewayId);
		if(subData == null)
			return Collections.emptyList();
		List<DpConnection> result = new ArrayList<>();
		for(DpConnection conn: subData.values()) {
			if(type == null || conn.getUtilityType() == type)
				result.add(conn);
		}
		return result;
	}
	
	@Override
	public List<DpConnection> getConnections(UtilityType type) {
		return getConnections(type, GaRoMultiEvalDataProvider.LOCAL_GATEWAY_ID);
	}
	
	@Override
	public List<DatapointGroup> getAllGroups() {
		return Collections.unmodifiableList(new ArrayList<DatapointGroup>(knownGroups.values()));
	}
	
	@Override
	public DatapointGroup getGroup(String id) {
		DatapointGroup result = knownGroups.get(id);
		if(result == null) {
			result = new DatapointGroupImpl(id);
			knownGroups.put(id, result);
		}
		return result ;
	}
	
	@Override
	public boolean hasGroup(String id) {
		return knownGroups.containsKey(id);
	}
	
	@Override
	public AlarmingService alarming() {
		return alarming;
	}
	
	TimedJobMgmtService timedJobMan = null;
	@Override
	public TimedJobMgmtService timedJobService() {
		//if(timedJobMan == null) {
		//	timedJobMan = new TimedJobMgmtServiceImpl(appMan);
		//}
		return timedJobMan;
	}
	@Override
	public Collection<MessageTransport> messageTransportServices() {
		return baseApp.getMessageTransportProviders().values();
	}
	@Override
	public MessageTransport messageTransportService(String adressType) {
		return baseApp.getMessageTransportProviders().get(adressType);
	}
	
	@Override
	public UserDataAccess userAdminDataService() {
		return baseApp.getUserDataAccess();
	}
	
	OSGiConfigAccessService configService;
	@Override
	public OSGiConfigAccessService configService() {
		if(configService == null)
			configService = new OSGiConfigAccessServiceImpl(configAdmin);
		return configService;
	}
	
	VirtualScheduleService virtSchedService;
	@Override
	public VirtualScheduleService virtualScheduleService() {
		if(virtSchedService == null)
			virtSchedService = new VirtualScheduleService(this, appMan);
		return virtSchedService;
	}
	
	private Map<String, InstallationProgressService> installationServices  = new HashMap<>();
	public void addInstallationService(InstallationProgressService service) {
		installationServices.put(service.getComSystemId(), service);
	}
	@Override
	public InstallationProgressService installationService(String comSystemId) {
		return installationServices.get(comSystemId);
	}
	
	private volatile EscalationManagerI escService = null;
	private List<EscalationProvider> openInitialEscServices = new ArrayList<>();
	public void setEscService(EscalationManagerI escService) {
		this.escService = escService;
	}
	@Override
	public EscalationManagerI alarmingEscalationService() {
		return escService;
	}
	
	@Override
	public boolean addService(Object service) {
		if(service instanceof EscalationManagerI) {
			if(escService == null) {
				escService = (EscalationManagerI) service;
				for(EscalationProvider prov: openInitialEscServices) {
					escService.registerEscalationProvider(prov);
				}
				openInitialEscServices.clear();
				return true;
			}
		}
		return false;		
	}
	
	@Override
	public void registerEscalationProvider(EscalationProvider prov) {
		if(escService == null) {
			openInitialEscServices.add(prov);
			return;
		}
		escService.registerEscalationProvider(prov);		
	}
	
	@Override
	public EscalationProvider unregisterEscalationProvider(EscalationProvider prov) {
		if(escService == null) {
			if(openInitialEscServices.remove(prov))
				return prov;
			else
				return null;
		}
		return escService.unregisterEscalationProvider(prov);		
	}
}
