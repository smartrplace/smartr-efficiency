package org.sp.example.smarteff.eval.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.tools.timeseries.iterator.api.SampledValueDataPoint;

import de.iwes.timeseries.eval.api.EvaluationInput;
import de.iwes.timeseries.eval.api.EvaluationInstance.EvaluationListener;
import de.iwes.timeseries.eval.api.EvaluationProvider;
import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.SingleEvaluationResult;
import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.configuration.Configuration;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance.GenericObjectConfiguration;
import de.iwes.timeseries.eval.base.provider.utils.ConfigurationBuilder;
import de.iwes.timeseries.eval.base.provider.utils.SingleValueResultImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesResultImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoEvaluationCore;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoResultType;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoSingleEvalProvider;
import de.iwes.timeseries.eval.online.utils.InputSeriesAggregator;
import de.iwes.timeseries.eval.online.utils.InputSeriesAggregator.AggregationMode;
import de.iwes.timeseries.eval.online.utils.InputSeriesAggregator.ValueDuration;
import de.iwes.timeseries.eval.online.utils.TimeSeriesOnlineBuilder;
import extensionmodel.smarteff.basic.evals.BuildingEvalData;

/**
 * Example GaRo EvaluationProvider
 */
@Service(EvaluationProvider.class)
@Component
public class BuildingPresenceEvalProvider extends GenericGaRoSingleEvalProvider {
	public static final long DEFAULT_MINIMUM_ABSENCE = 60*60000;
	
	/** Adapt these values to your provider*/
    public final static String ID = "building_presence_eval_provider";
    public final static String LABEL = "Building Presence evaluation provider";
    public final static String DESCRIPTION = "Provides presence signal for entire building based"
    		+ " on the presence signals of single rooms.";
    
    public BuildingPresenceEvalProvider() {
        super(ID, LABEL, DESCRIPTION);
    }
    
    @Override
    public int[] getRoomTypes() {
    	return new int[]{-1};
    }

	@Override
	/** Provide your data types here*/
	public GaRoDataType[] getGaRoInputTypes() {
		return new GaRoDataType[] {
	        	GaRoDataType.MotionDetection,
		};
	}
	/** It is recommended to define the indices of your input here.*/
	public static final int MOTION_IDX = 0; 
        
 	public class EvalCore extends GenericGaRoEvaluationCore {
    	final long totalTime;
    	
    	/** Application specific state variables, see also documentation of the util classes used*/
    	public final InputSeriesAggregator motion;
    	public final TimeSeriesOnlineBuilder tsBuilder;

    	//Before we get the first motion signal we assume no presence
    	private boolean isPresent = false;
    	private long memorizePotentialAbsence;
    	private long absenceSignalStarted = -1;
  	
    	private final long minimumAbsence;
    	private long countPresenceTime = 0;
    	private int countPresenceEvents = 0;
    	
    	@SuppressWarnings("unchecked")
		public EvalCore(List<EvaluationInput> input, List<ResultType> requestedResults,
    			Collection<ConfigurationInstance> configurations, EvaluationListener listener, long time,
    			int size, int[] nrInput, int[] idxSumOfPrevious, long[] startEnd) {
    		//example how to calculate total time assuming offline evaluation
    		totalTime = startEnd[1] - startEnd[0];
    		/**The InputSeriesAggregator aggregates the input from all motion sensors in the room.
    		 *	AggregationMode.MAX makes sure that the aggregated value will be one if any motion
    		 *  sensor in the room has signal one.
    		*/
    		motion = new InputSeriesAggregator(nrInput, idxSumOfPrevious,
    				MOTION_IDX, startEnd[1], null, AggregationMode.MAX);
    		tsBuilder = new TimeSeriesOnlineBuilder();
            long val = DEFAULT_MINIMUM_ABSENCE;
    		BuildingEvalData configuration;
            for (ConfigurationInstance cfg : configurations) {
                if (cfg.getConfigurationType().equals(OBJECT_CONFIGURATION)) {
                	configuration = ((ConfigurationInstance.GenericObjectConfiguration<BuildingEvalData>) cfg).getValue();
                    val = configuration.minimumAbsenceTime().getValue();
                    break;
                }
            }
    		minimumAbsence = val;
    	}
    	
    	/** In processValue the core data processing takes place. This method is called for each input
    	 * value of any input time series.*/
    	@Override
    	protected void processValue(int idxOfRequestedInput, int idxOfEvaluationInput,
    			int totalInputIdx, long timeStamp,
    			SampledValue sv, SampledValueDataPoint dataPoint, long duration) {
    		if(isPresent && (memorizePotentialAbsence > 0) && (memorizePotentialAbsence < timeStamp) &&
    				((timeStamp - absenceSignalStarted) > minimumAbsence)) {
    			//In the mean time the datapoint is in the past, so we have to indicate absence
   				tsBuilder.addValue(new SampledValue(new FloatValue(0), timeStamp, Quality.GOOD));
   				isPresent = false;
     		}
    		
    		switch(idxOfRequestedInput) {
    		case MOTION_IDX:
    			final ValueDuration val = motion.getCurrentValueDuration(idxOfEvaluationInput, sv, dataPoint, true);
   				boolean newPresence = (val.value > 0.5f);
   				if(isPresent && (!newPresence)) {
   					absenceSignalStarted = timeStamp;
					memorizePotentialAbsence = timeStamp;
					break;
   				} else if((absenceSignalStarted > 0) && newPresence) {
   					countPresenceTime += (timeStamp - absenceSignalStarted);
   					absenceSignalStarted = -1;
   					memorizePotentialAbsence = -1;
     			} else if(!isPresent && newPresence) {
   					absenceSignalStarted = -1;
   					memorizePotentialAbsence = -1;
   					isPresent = true;
   					countPresenceEvents++;
   				}
   				tsBuilder.addValue(sv);
   				break;
     		}
    		
    		if(isPresent && (memorizePotentialAbsence < 0))
    			countPresenceTime += duration;
    	}
    }
    
 	/**
 	 * Define the results of the evaluation here including the final calculation
 	 */
    public final static GenericGaRoResultType ROOM_PRESENCE_TS = new GenericGaRoResultType("Cleaned_Presence_TS") {
			@Override
			public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
					List<TimeSeriesData> inputData) {
				EvalCore cec = ((EvalCore)ec);
				return new TimeSeriesResultImpl(rt, cec.tsBuilder.getTimeSeries(), inputData);
			}
    };
    public final static GenericGaRoResultType PRESENCE_SHARE = new GenericGaRoResultType("Presence_Share", "Time with presence compared to total evaluation time") {
			@Override
			public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
					List<TimeSeriesData> inputData) {
				EvalCore cec = ((EvalCore)ec);
				return new SingleValueResultImpl<Float>(rt, (float) ((double)cec.countPresenceTime/cec.totalTime), inputData);
			}
    };
    public final static GenericGaRoResultType PRESENCE_NUM = new GenericGaRoResultType("Presence_Num", "Number of Presence Times separated by Absence") {
			@Override
			public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
					List<TimeSeriesData> inputData) {
				EvalCore cec = ((EvalCore)ec);
				return new SingleValueResultImpl<Integer>(rt, cec.countPresenceEvents, inputData);
			}
    };
    private static final List<GenericGaRoResultType> RESULTS = Arrays.asList(ROOM_PRESENCE_TS,
    		PRESENCE_SHARE, PRESENCE_NUM);
    
	@Override
	protected List<GenericGaRoResultType> resultTypesGaRo() {
		return RESULTS;
	}

	@Override
	protected GenericGaRoEvaluationCore initEval(List<EvaluationInput> input, List<ResultType> requestedResults,
			Collection<ConfigurationInstance> configurations, EvaluationListener listener, long time, int size,
			int[] nrInput, int[] idxSumOfPrevious, long[] startEnd) {
		return new EvalCore(input, requestedResults, configurations, listener, time, size, nrInput, idxSumOfPrevious, startEnd);
	}
	
	public static final Configuration<GenericObjectConfiguration<BuildingEvalData>> OBJECT_CONFIGURATION =
			getConfiguration(ID, ROOM_PRESENCE_TS, BuildingEvalData.class);
		/*ConfigurationBuilder.newBuilder(ConfigurationInstance.GenericDurationConfiguration.class)
			.withId("minimum_absence_cfg")
			.withLabel("Minimum Absence")
			.withDescription("Absence times below this will be covered by presence.")
			.withDefaultFloat(DEFAULT_MINIMUM_ABSENCE)
			.withResultTypes(ROOM_PRESENCE_TS)
			.isOptional(true)
			.build();*/
	@Override
	public List<Configuration<?>> getConfigurations() {
		List<Configuration<?>> result = new ArrayList<>();
		result.addAll(super.getConfigurations());
		result.add(OBJECT_CONFIGURATION);
		return result ;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected static <T extends Resource> Configuration<GenericObjectConfiguration<T>> getConfiguration(String id, ResultType anyResult,
			Class<? extends Resource> configType) {
		Configuration result2 =
			ConfigurationBuilder.newBuilder(ConfigurationInstance.GenericObjectConfiguration.class)
				.withId(id+"_cfg")
				.withLabel(configType.getSimpleName()+" Configuration Object")
				.withDescription(configType.getName()+" Configuration Object for provider "+id)
				.withResultTypes(anyResult)
				.isOptional(true)
				.build();
		Configuration<GenericObjectConfiguration<T>> result = result2;
		return result ;
	}
}
