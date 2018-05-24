package org.smartrplace.smarteff.admin.protect;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.ogema.model.jsonresult.JSONResultFileData;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.util.directresourcegui.kpi.KPIStatisticsManagement;
import org.ogema.util.evalcontrol.EvalScheduler;
import org.ogema.util.evalcontrol.EvalScheduler.OverwriteMode;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionUserData;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;
import org.smartrplace.extensionservice.driver.DriverProvider;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForCreate;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForEvaluation;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForPageOpening;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.util.CapabilityHelper;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoSuperEvalResult;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;

public class ExtensionResourceAccessInitDataImpl implements ExtensionResourceAccessInitData {
	private final int entryTypeIdx;
	private final List<Resource> entryResources;
	private final List<GenericDataTypeDeclaration> entryData;
	
	private final ConfigInfo configInfo;
	private final ExtensionUserData userData;
	private final ExtensionUserDataNonEdit userDataNonEdit;
	private final ExtensionPageSystemAccessForPageOpening systemAccess;
	
	//TODO: This should be no risk for attacks on user data, but to be discussed
	private final SpEffAdminController controller;
	
	public ExtensionResourceAccessInitDataImpl(int entryTypeIdx,
			List<Resource> entryResources, List<GenericDataTypeDeclaration> entryData,
			ConfigInfo configInfo,
			ExtensionUserData userData, ExtensionUserDataNonEdit userDataNonEdit,
			ExtensionPageSystemAccessForPageOpening systemAccess,
			SpEffAdminController controller) {
		this.entryTypeIdx = entryTypeIdx;
		if(entryResources == null) {
			this.entryResources = entryResources;
			this.entryData = entryData;			
		} else {
			this.entryResources = entryResources;
			this.entryData = entryData;			
		}
		this.userData = userData;
		this.userDataNonEdit = userDataNonEdit;
		this.systemAccess = systemAccess;
		this.configInfo = configInfo;
		this.controller = controller;
	}

	@Override
	public int entryTypeIdx() {
		return entryTypeIdx;
	}

	@Override
	public List<Resource> entryResources() {
		return entryResources;
	}
	@Override
	public List<GenericDataTypeDeclaration> entryData() {
		return entryData;
	}

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
					int defaultIntervalsToCalculate) {
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
					MultiKPIEvalConfiguration startConfig = scheduler.getOrCreateConfig(eval.id(),
							subConfigId);
					startConfig.configurationResource().setAsReference(configurationResource);
					@SuppressWarnings("unchecked")
					ResourceList<MultiKPIEvalConfiguration> resList =
							entryResource.getSubResource("multiKPIEvalConfiguration", ResourceList.class);
					if(!resList.exists()) {
						resList.create();
						resList.setElementType(MultiKPIEvalConfiguration.class);
						resList.activate(false);
					}
					resList.addDecorator(ResourceUtils.getValidResourceName(eval.id()), startConfig);
					long[] result = scheduler.getStandardStartEndTime(startConfig, defaultIntervalsToCalculate);
					result = scheduler.queueEvalConfig(startConfig, saveJsonResult, null,
							result[0], result[1], dataProvidersToUse, true, OverwriteMode.NO_OVERWRITE);
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
			public List<KPIStatisticsManagement> getKPIManagement(Resource entryResource, String providerId) {
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
			
			private List<KPIStatisticsManagement> getKPIManagement(MultiKPIEvalConfiguration startConfig) {
				String providerId = startConfig.evaluationProviderId().getValue();
				GaRoSingleEvalProvider eval = controller.serviceAccess.evalResultMan().getEvalScheduler().getProvider(providerId);
				return controller.serviceAccess.evalResultMan().getEvalScheduler().configureKPIManagement(
						startConfig, eval );
			}
			
			public List<KPIStatisticsManagement> getKPIManagement(Resource entryResource) {
				List<KPIStatisticsManagement> result = new ArrayList<>();
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
}
