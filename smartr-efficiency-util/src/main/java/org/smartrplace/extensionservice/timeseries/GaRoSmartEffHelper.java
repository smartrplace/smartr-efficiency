package org.smartrplace.extensionservice.timeseries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.ogema.generictype.GenericDataTypeDeclaration.TypeCardinality;
import org.ogema.model.jsonresult.MultiEvalStartConfiguration;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.model.locations.Room;
import org.ogema.util.evalcontrol.EvalScheduler;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionUserData;
import org.smartrplace.extensionservice.driver.DriverProvider;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.MyParam;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoSelectionItem;
import de.iwes.util.resource.ResourceHelper;
import extensionmodel.smarteff.api.common.BuildingData;

/** The concept implemented here implies that
 * Evaluations using time series input need to find that via GaRo-DataProviders. That implies
 * that all time series can be accessed via the GaRo scheme. This again means that we need a
 * DataProvider that offers all KPIs calculated. And we need a DataProvider that offers all
 * JSON file Pre-evaluation results. Such a DataProvider has to create a {@link GaRoDataType}
 * from the {@link GenericDataTypeDeclaration} provided by the Result declaration so that it
 * can be used as input to others again.
 * TODO: By default this done via a combination of provider and result id if the respective
 * result can only be obtained from this provider (default). It should also be possible to
 * declare an existing GaRoDataType for a result an automatically use the data from this for the
 * result GenericaDataTypeDeclaration values also. Then it should be possible to declare this
 * as input time series directly instead of injection of PreEvaluation (which should still be
 * supported as legacy, but maybe not in SmartrEff).
 * 
 * Evaluations shall be started by queueing in the {@link EvalScheduler}. There must be a method
 * in the {@link ApplicationManagerSPExt} interface for this. This also organizes the result
 * storage. If the evaluation calculates KPIs a resource of type {@link MultiKPIEvalConfiguration}
 * is required, which shall be placed below the main entry resource or just a link to it.
 * TODO: The   question where to put the configuration resources also for {@link DriverProvider}s is still to
 * be clarified in detail also. If only JSON results are calculated then a
 * {@link MultiEvalStartConfiguration} may be sufficient. Some elements are not relevant here
 * such as the gatewayIds.
 * 
 * This works fine for Multi-evaluations, but not for single evaluations and visualization.
 * Visualizations can trigger Multi-Evaluations available or use existing results. Existing
 * results are found via 
 * TODO: Methods accessible via ApplicationManagerSPExt (preferred). You can ask which
 * GenericDataTypes/GaRoDataTypes are available, then get access to time series via
 * masked methods of AbsoluteTimeHelper (StatisticalAggregation) and JSONFileManagement. 
 * 
 * TODO: General mechanisms for auto-queuing, management of repeated operation.
 */
public class GaRoSmartEffHelper {

	protected List<GaRoMultiEvalDataProvider<?>> dataProviders = new ArrayList<>();

	public void addDataProvider(GaRoMultiEvalDataProvider<?> dp) {
		dataProviders.add(dp);
	}
	public boolean removeDataProvider(GaRoMultiEvalDataProvider<?> dp) {
		return dataProviders.remove(dp);
	}
	
	/** The evaluation output definition must determine wheter stored as JSON, KPI, both,
	 * others,...
	 */
	public static class MultiEvalInput {
		List<String> gwIds;
		List<String> roomIds;
		List<GaRoMultiEvalDataProvider<?>> relevantDataProviders;
		long startTime;
		long endTime; //could also be determined by evaluation itself, but can also be set by
						//initial calling, then has to be transferred here
	}
	public static class EvaluationInputFlex {
		/** Only one of the two input types shall be non-null*/
		MyParam<? extends Resource> paramHelper;
		MultiEvalInput timeSeries;
	}
	EvaluationInputFlex getInput(Resource parent, GenericDataTypeDeclaration requiredInput,
			ExtensionUserData userData, ApplicationManagerSPExt appManExt,
			long startTime, long endTime) {
		if(requiredInput.typeCardinality() == TypeCardinality.OBJECT ||
				requiredInput.typeCardinality() == TypeCardinality.SINGLE_VALUE) {
			MyParam<? extends Resource> myParams = CapabilityHelper.getMyParams(requiredInput.representingResourceType(),
					userData, appManExt);
			EvaluationInputFlex result = new EvaluationInputFlex();
			result.paramHelper = myParams;
			return result;
		}
		//Timeseries
		//TODO: Support also different versions (public/user) here? => Next step
		EvaluationInputFlex result = new EvaluationInputFlex();
		result.timeSeries = new MultiEvalInput();
		BuildingData singleBuilding = getBuilding(parent);
		if(singleBuilding != null) result.timeSeries.gwIds = Arrays.asList(new String[] {getGwId(singleBuilding)});
		else {
			List<BuildingData> multiBuildings = getBuildings(parent);
			if(multiBuildings != null) {
				result.timeSeries.gwIds = new ArrayList<>();
				for(BuildingData bd: multiBuildings) result.timeSeries.gwIds.add(getGwId(bd));
			} else {
				result.timeSeries.gwIds = Arrays.asList(new String[] {GaRoMultiEvalDataProvider.LOCAL_GATEWAY_ID});
			}
		}
		if(result.timeSeries.gwIds.size() == 1) {
			Room singleRoom = getRoom(parent);
			if(singleRoom != null) result.timeSeries.roomIds = Arrays.asList(new String[] {getRoomId(singleRoom)});
			else {
				List<Room> multiRooms = getRooms(parent);
				if(multiRooms != null) {
					result.timeSeries.roomIds = new ArrayList<>();
					for(Room bd: multiRooms) result.timeSeries.gwIds.add(getRoomId(bd));
				} else {
					result.timeSeries.roomIds = Collections.emptyList();
				}
			}
		}
		result.timeSeries.relevantDataProviders = new ArrayList<>();
		for(GaRoMultiEvalDataProvider<?> dp: dataProviders) {
			if(isDataProviderRelevant(dp, result.timeSeries.gwIds, result.timeSeries.roomIds))
				result.timeSeries.relevantDataProviders.add(dp);
		}
		return result;
	}
	
	private boolean isDataProviderRelevant(GaRoMultiEvalDataProvider<?> dataProvider,
			List<String> gwIds, List<String> roomIds) {
		if(gwIds.size() > 1) {
			if(!dataProvider.getSelectionItemsForGws(gwIds).isEmpty()) return true;
		} else {
			List<GaRoSelectionItem> gwItems = dataProvider.getSelectionItemsForGws(gwIds);
			if(gwItems.isEmpty()) throw new IllegalStateException("There should always be a gateway!");
			if(!dataProvider.getSelectionItemsForRooms(gwItems.get(0), roomIds).isEmpty()) return true;			
		}
		return false;
	}
	
	public String getGwId(BuildingData building) {
		final String gwId;
		StringResource gwIdRes = building.getSubResource("gwId", StringResource.class);
		if(gwIdRes.isActive()) gwId = gwIdRes.getValue();
		else gwId = building.name().getValue();
		return gwId;
	}
	
	public BuildingData getBuilding(Resource res) {
		if(res instanceof BuildingData) return (BuildingData) res;
		return ResourceHelper.getFirstParentOfType(res, BuildingData.class);
	}
	
	public List<BuildingData> getBuildings(Resource res) {
		return res.getSubResources(BuildingData.class, true);
	}
	
	@SuppressWarnings("unchecked")
	public List<BuildingData> getBuildingsFromList(ResourceList<?> resList) {
		if(BuildingData.class.isAssignableFrom(resList.getElementType())) return (List<BuildingData>) resList.getAllElements();
		return null;
	}
	
	public String getRoomId(Room building) {
		final String gwId;
		StringResource gwIdRes = building.getSubResource("roomId", StringResource.class);
		if(gwIdRes.isActive()) gwId = gwIdRes.getValue();
		else gwId = building.name().getValue();
		return gwId;
	}
	
	public Room getRoom(Resource res) {
		if(res instanceof Room) return (Room) res;
		return ResourceHelper.getFirstParentOfType(res, Room.class);
	}
	
	public List<Room> getRooms(Resource res) {
		return res.getSubResources(Room.class, true);
	}
	
	@SuppressWarnings("unchecked")
	public List<Room> getRoomsFromList(ResourceList<?> resList) {
		if(Room.class.isAssignableFrom(resList.getElementType())) return (List<Room>) resList.getAllElements();
		return null;
	}

}
