package org.smartrplace.smarteff.admin.protect;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.ogema.model.jsonresult.JSONResultFileData;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.ogema.util.evalcontrol.EvalScheduler;
import org.ogema.util.evalcontrol.EvalScheduler.OverwriteMode;
import org.smartrplace.critical.crossuser.ExtensionPageSystemAccessForCrossuserAccess;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionUserData;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.extensionservice.driver.DriverProvider;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForCreate;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForEvaluation;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForPageOpening;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForTimeseries;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.admin.timeseries.GenericDriverProvider;
import org.smartrplace.smarteff.util.CapabilityHelper;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoSuperEvalResult;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.util.resource.ResourceHelper;
import extensionmodel.smarteff.api.base.SmartEffUserData;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;
import extensionmodel.smarteff.api.common.AccessControl;
import extensionmodel.smarteff.api.common.BuildingData;

public class ExtensionResourceAccessInitDataImpl implements ExtensionResourceAccessInitData {
	private final int entryTypeIdx;
	private final List<Resource> entryResources;
	//private final List<GenericDataTypeDeclaration> entryData;
	
	private final ConfigInfo configInfo;
	private final ExtensionUserData userData;
	private final ExtensionUserDataNonEdit userDataNonEdit;
	private final ExtensionPageSystemAccessForPageOpening systemAccess;
	
	//TODO: This should be no risk for attacks on user data, but to be discussed
	private final SpEffAdminController controller;
	private final GenericDriverProvider tsDriver;
	
	public ExtensionResourceAccessInitDataImpl(int entryTypeIdx,
			List<Resource> entryResources,
			//List<GenericDataTypeDeclaration> entryData,
			ConfigInfo configInfo,
			ExtensionUserData userData, ExtensionUserDataNonEdit userDataNonEdit,
			ExtensionPageSystemAccessForPageOpening systemAccess,
			SpEffAdminController controller) {
		this.entryTypeIdx = entryTypeIdx;
		if(entryResources == null) {
			this.entryResources = entryResources;
			//this.entryData = entryData;			
		} else {
			this.entryResources = entryResources;
			//this.entryData = entryData;			
		}
		this.userData = userData;
		this.userDataNonEdit = userDataNonEdit;
		this.systemAccess = systemAccess;
		this.configInfo = configInfo;
		this.controller = controller;
		this.tsDriver = controller.tsDriver;
	}

	@Override
	public int entryTypeIdx() {
		return entryTypeIdx;
	}

	@Override
	public List<Resource> entryResources() {
		return entryResources;
	}
	//@Override
	//public List<GenericDataTypeDeclaration> entryData() {
	//	return entryData;
	//}

	@Override
	public ExtensionUserData userData() {
		return userData;
	}

	@Override
	public ExtensionUserDataNonEdit userDataNonEdit() {
		return userDataNonEdit;
	}

	@Override
	public ExtensionPageSystemAccessForCreate systemAccess() {
		if(!(systemAccess instanceof ExtensionPageSystemAccessForCreate))
			throw new IllegalStateException("Page without valid configID only supports systeAccessForPageOpening!");
		return (ExtensionPageSystemAccessForCreate) systemAccess;
	}
	@Override
	public ExtensionPageSystemAccessForPageOpening systemAccessForPageOpening() {
		return systemAccess;
	}

	@Override
	public ConfigInfo getConfigInfo() {
		return configInfo;
	}

	@Override
	public ExtensionPageSystemAccessForEvaluation getEvaluationManagement() {
		return new ExtensionPageSystemAccessForEvaluation() {
			
			@Override
			public long[] calculateKPIs(GaRoSingleEvalProvider eval, Resource entryResource,
					Resource configurationResource, List<DriverProvider> drivers, boolean saveJsonResult,
					int defaultIntervalsToCalculate, Integer stepInterval) {
				return calculateKPIs(eval, entryResource, configurationResource, drivers, saveJsonResult,
						null, null, defaultIntervalsToCalculate, stepInterval);
				
			}
			@Override
			public long[] calculateKPIs(GaRoSingleEvalProvider eval, Resource entryResource,
					Resource configurationResource, List<DriverProvider> drivers, boolean saveJsonResult,
					long startTime, long endTime, Integer stepInterval) {
				return calculateKPIs(eval, entryResource, configurationResource, drivers, saveJsonResult,
						startTime, endTime, null, stepInterval);
			}
			private long[] calculateKPIs(GaRoSingleEvalProvider eval, Resource entryResource,
					Resource configurationResource, List<DriverProvider> drivers, boolean saveJsonResult,
					Long startTime, Long endTime, Integer defaultIntervalsToCalculate, Integer stepInterval) {
			return AccessController.doPrivileged(new PrivilegedAction<long[]>() {
				@Override
				public long[] run()  {
					EvalScheduler scheduler = controller.serviceAccess.evalResultMan().getEvalScheduler();
					if(scheduler == null) throw new IllegalStateException("We need an implementation with scheduler here!");
					
					int[] intarray = new int[SpEffAdminController.INTERVALS_OFFERED.length];
					for(int i=0; i<SpEffAdminController.INTERVALS_OFFERED.length; i++) {
						intarray[i] = SpEffAdminController.INTERVALS_OFFERED[i];
					}
					
					List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse = new ArrayList<>();
					List<DriverProvider> driversToCheck;
					if(drivers == null) driversToCheck = controller.guiPageAdmin.drivers;
					else driversToCheck = drivers;
					for(DriverProvider driver: driversToCheck) {
						int idx = 0;
						for(EntryType et: driver.getEntryTypes()) {
							if(et.getType().representingResourceType().equals(entryResource.getResourceType())) {
								DataProvider<?> dp = driver.getDataProvider(idx, Arrays.asList(new Resource[]{entryResource}),
										userDataNonEdit.editableData(), userDataNonEdit);
								if(dp != null) dataProvidersToUse.add((GaRoMultiEvalDataProvider<?>) dp);
								break;
							}
							idx++;
						}
					}
					if(dataProvidersToUse.isEmpty()) return null;
					
					String subConfigId = entryResource.getLocation();
					MultiKPIEvalConfiguration startConfig;
					startConfig = scheduler.getOrCreateConfig(eval.id(),
							subConfigId, stepInterval, true);

					BuildingData bd;
					if(entryResource instanceof BuildingData) bd = (BuildingData) entryResource;
					else bd = ResourceHelper.getFirstParentOfType(entryResource, BuildingData.class);
					if(bd != null) {
						startConfig.gwIds().<StringArrayResource>create().setValues(new String[] {bd.getLocation()});
						//TODO: We cannot specify any rooms here yet
					}
					startConfig.configurationResource().setAsReference(configurationResource);
					CapabilityHelper.addMultiTypeToList(entryResource, eval.id(), startConfig);
					
					long[] result;
					if(defaultIntervalsToCalculate != null) {
						if(defaultIntervalsToCalculate <= 0) {
							//TODO
							//ts = dataProvidersToUse.get(0).getData(items)
						}
						result = scheduler.getStandardStartEndTime(startConfig, defaultIntervalsToCalculate, true);
					} else {
						result = new long[] {startTime, endTime};
					}
					result = scheduler.queueEvalConfig(startConfig, saveJsonResult, null,
							result[0], result[1], dataProvidersToUse, true, OverwriteMode.NO_OVERWRITE, true);
					return result;
				}
			});
			}
			
			/** Get all file descriptor resources that are indicated to be generated by certain provider in a certain interval
			 * @param startTime start of interval
			 * @param endTime end of interval
			 * @param includeOverlap if true all files that contribute to the interval at least partially are returned, 
			 * 		otherwise only files that are completely inside the interval*/
			public List<JSONResultFileData> getDataOfProvider(String providerId, long startTime, long endTime,
					boolean includeOverlap) {
				List<JSONResultFileData> result = new ArrayList<>();
				List<JSONResultFileData> allData = controller.serviceAccess.evalResultMan().getDataOfProvider(providerId, startTime, endTime,
						includeOverlap);
				for(JSONResultFileData item: allData) {
					if(CapabilityHelper.getSubPathBelowUser(item, userDataNonEdit.getName()) != null)
						result.add(item);
				}
				return result;
			}

			/** Get a super result containing all evaluation intervals of a certain interval even from different files.
			 * See {@link #getDataOfProvider(String, long, long, boolean)} for details. If the result intervals overlap
			 * always the newst result shall be used.
			 */
			public GaRoSuperEvalResult<?> getAggregatedResult(String providerId, long startTime, long endTime, boolean includeOverlap) {
				List<JSONResultFileData> result = getDataOfProvider(providerId, startTime, endTime, includeOverlap);
				return controller.serviceAccess.evalResultMan().getAggregatedResult(result, startTime, endTime, includeOverlap);
			}
			
			/**Configure instances of KPIStatisticsManagement
			 * @param providerId all ResultType-KPIs configured in all MultiKPIEvalConfigurations for the provider will
			 * be configured
			 * @return management objects for each ResultType-KPI*/
			@Override
			public List<KPIStatisticsManagementI> getKPIManagement(Resource entryResource, String providerId) {
				MultiKPIEvalConfiguration startConfig = null;
				@SuppressWarnings("unchecked")
				ResourceList<MultiKPIEvalConfiguration> resList =
						entryResource.getSubResource("multiKPIEvalConfiguration", ResourceList.class);
				for(MultiKPIEvalConfiguration m: resList.getAllElements()) {
					if(m.evaluationProviderId().getValue().equals(providerId)) startConfig = m;
					break;
				}
				if(startConfig == null) return null;
				GaRoSingleEvalProvider eval = controller.serviceAccess.evalResultMan().getEvalScheduler().getProvider(providerId);
				return controller.serviceAccess.evalResultMan().getEvalScheduler().configureKPIManagement(
						startConfig, eval );
			}
			
			private List<KPIStatisticsManagementI> getKPIManagement(MultiKPIEvalConfiguration startConfig) {
				String providerId = startConfig.evaluationProviderId().getValue();
				GaRoSingleEvalProvider eval = controller.serviceAccess.evalResultMan().getEvalScheduler().getProvider(providerId);
				return controller.serviceAccess.evalResultMan().getEvalScheduler().configureKPIManagement(
						startConfig, eval );
			}
			
			@Override
			public List<KPIStatisticsManagementI> getKPIManagement(Resource entryResource) {
				List<KPIStatisticsManagementI> result = new ArrayList<>();
				@SuppressWarnings("unchecked")
				ResourceList<MultiKPIEvalConfiguration> resList =
						entryResource.getSubResource("multiKPIEvalConfiguration", ResourceList.class);
				for(MultiKPIEvalConfiguration m: resList.getAllElements()) {
					result.addAll(getKPIManagement(m));
				}
				return result;
			}
			
			public List<JSONResultFileData> getDataOfResource(Resource entryResource) {
				List<JSONResultFileData> result = new ArrayList<>();
				@SuppressWarnings("unchecked")
				ResourceList<MultiKPIEvalConfiguration> resList =
						entryResource.getSubResource("multiKPIEvalConfiguration", ResourceList.class);
				for(MultiKPIEvalConfiguration m: resList.getAllElements()) {
					List<JSONResultFileData> single = controller.serviceAccess.evalResultMan().
							getDataOfConfig(m, 0, Long.MAX_VALUE, false);
					if(single != null) result.addAll(single);
				}
				return result;
			}
		};
	}

	@Override
	public PublicUserInfo getUserInfo() {
		return new PublicUserInfo() {

			@Override
			public String userName() {
				return userDataNonEdit().ogemaUserName().getValue();
			}

			@Override
			public boolean isAnonymousUser() {
				return controller.getUserAdmin().isAnonymousUser(userName());
			}
			
		};
	}

	@Override
	public ExtensionPageSystemAccessForTimeseries getTimeseriesManagement() {
		return new ExtensionPageSystemAccessForTimeseries() {
			
			@Override
			public void registerSingleColumnCSVFile(SmartEffTimeSeries timeSeries, GenericDataTypeDeclaration dataType,
					String sourceId, String filePath, String format) {
				tsDriver.addSingleColumnCSVFile(timeSeries, dataType, sourceId, filePath, format);
				
			}
			
			@Override
			public void registerSchedule(SmartEffTimeSeries timeSeries, GenericDataTypeDeclaration dataType, String sourceId,
					Schedule sched) {
				tsDriver.addSchedule(timeSeries, dataType, sourceId, sched);
				
			}
			
			@Override
			public void registerRecordedData(SmartEffTimeSeries timeSeries, GenericDataTypeDeclaration dataType, String sourceId,
					SingleValueResource recordedDataParent) {
				tsDriver.addRecordedData(timeSeries, dataType, sourceId, recordedDataParent);
				
			}
			
			@Override
			public String getGenericDriverProviderId() {
				return tsDriver.id();
			}

			@Override
			public List<ReadOnlyTimeSeries> getTimeSeries(Resource entryResource, GenericDataTypeDeclaration dataType,
					String sourceId) {
				if(sourceId != null) {
					ReadOnlyTimeSeries ts = tsDriver.getTimeSeries(entryResource, dataType, sourceId, userData, userDataNonEdit);					
					return Arrays.asList(new ReadOnlyTimeSeries[] {ts});
				}
				return tsDriver.getTimeSeries(entryResource, dataType, userData, userDataNonEdit);
			}
			
			@Override
			public ReadOnlyTimeSeries getTimeSeries(SmartEffTimeSeries smartTs) {
				return tsDriver.getTimeSeries(smartTs);
			}

			@Override
			public ReadOnlyTimeSeries readTimeSeriesFromFiles(String fileType, String[] paths) {
				return tsDriver.readTimeSeries(fileType, paths);
			}

			@Override
			public int getFileNum(SmartEffTimeSeries timeSeries, String sourceId) {
				return tsDriver.getFileNum(timeSeries, sourceId);
			}
		};
	}

	@Override
	public ExtensionPageSystemAccessForCrossuserAccess getCrossuserAccess() {
		return new ExtensionPageSystemAccessForCrossuserAccess() {

			@Override
			public <T extends Resource> T getAccess(String subUserPath, String userSource, Class<T> type, Object module4authentication) {
				SmartEffUserData destNonEdit = null;
				for(SmartEffUserDataNonEdit user: controller.getUserAdmin().getAllUserData()) {
					if(user.ogemaUserName().getValue().equals(userSource)) {
						destNonEdit = user.editableData();
						break;
					}
				}
				if(destNonEdit == null) return null;
				T result = ResourceHelper.getSubResource(destNonEdit, subUserPath, type);
				if(result == null) return null;
				Resource par = getFirstParentWithSubResourceOfType(result, "accessControl", AccessControl.class);
				if(par == null) return null;
				AccessControl sub = par.getSubResource("accessControl", AccessControl.class);
				if(sub == null) return null;
				if(!sub.modules().isActive()) return null;
				String[] modules = sub.modules().getValues();
				String className = module4authentication.getClass().getName();
				for(String m: modules) {
					if(m.equals(className)) return result;
				}
				return null;
			}

			@SuppressWarnings("unchecked")
			@Override
			public <T extends Resource> List<T> getAccess(Class<T> type) {
				List<AccessControl> accs = controller.appMan.getResourceAccess().getResources(AccessControl.class);
				String myUser = userDataNonEdit.getLocation();
				List<T> result = new ArrayList<>();
				for(AccessControl acc: accs) {
					if(ValueResourceUtils.contains(acc.users(), myUser)) {
						if(type == null) result.add(acc.getParent());
						else {
							Resource par = acc.getParent();
							if(type.isAssignableFrom(par.getClass())) result.add((T) par);
						}
					}
				}
				return result;
			}
		};
	}
	
	/** Get a resource that has an active sub resource with the name and the type requested. This may
	 * be the initial parent provided or any parent or super-parent of the initial parent.
	 * @param type
	 * @return
	 */
	public static Resource getFirstParentWithSubResourceOfType(Resource initialParent, String name, Class<? extends Resource> type) {
		Resource curParent = initialParent;
		Resource sub = curParent.getSubResource(name, type);
		while(sub == null || (!sub.isActive())) {
			curParent = curParent.getParent();
			if(curParent == null) return null;
			sub = curParent.getSubResource(name, type);
		}
		return curParent;
	}
}
