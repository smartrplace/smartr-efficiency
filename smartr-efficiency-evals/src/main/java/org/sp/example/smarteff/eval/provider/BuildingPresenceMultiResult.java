package org.sp.example.smarteff.eval.provider;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.ogema.core.timeseries.ReadOnlyTimeSeries;

import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.SingleEvaluationResult;
import de.iwes.timeseries.eval.api.SingleEvaluationResult.TimeSeriesResult;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance;
import de.iwes.timeseries.eval.api.extended.MultiEvaluationInputGeneric;
import de.iwes.timeseries.eval.garo.api.base.GaRoSuperEvalResult;
import de.iwes.timeseries.eval.garo.multibase.GaRoMultiResultExtended;

@Deprecated //not used
public class BuildingPresenceMultiResult extends GaRoMultiResultExtended {
	//For internal data transfer
	List<ReadOnlyTimeSeries> roomTimeSeries;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public BuildingPresenceMultiResult(List<MultiEvaluationInputGeneric> inputData, long start,
			long end, Collection<ConfigurationInstance> configurations) {
		super((List)inputData, start, end, configurations);
	}
	/**Only to be used by JSON Deserialization!*/
	public BuildingPresenceMultiResult() {
		super(null, 0, 0, null);
	}

	@Override
	public void finishRoom(GaRoMultiResultExtended resultExtended, String roomId) {
		//Take result and put it into position for JSON processing
		BuildingPresenceMultiResult pres = (BuildingPresenceMultiResult)resultExtended;
		Map<ResultType, SingleEvaluationResult> hmap = pres.roomData.evalResultObjects();
		SingleEvaluationResult obj1 = hmap.get(BuildingPresenceEvalProvider.ROOM_PRESENCE_TS);
		ReadOnlyTimeSeries obj = ((TimeSeriesResult)obj1).getValue();
		roomTimeSeries.add(obj);
	}

	@Override
	public void finishGateway(GaRoMultiResultExtended result, String gw) {}

	@Override
	public void finishTimeStep(GaRoMultiResultExtended result) {}

	// @SuppressWarnings("unchecked")
	@Override
	public void finishTotal(GaRoSuperEvalResult<?> result) {}
	
}
