package org.sp.example.smarteff.electricity.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.channelmanager.measurements.FloatValue;
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
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.timeseries.eval.online.utils.TimeSeriesOnlineBuilder;
import extensionmodel.smarteff.electricity.example.ElectricityProfileEvalConfig;

/**
 * Example GaRo EvaluationProvider
 */
@Service(EvaluationProvider.class)
@Component
public class ElectricityProfileEvalProvider extends GenericGaRoSingleEvalProvider {
	public static final float DEFAULT_OFFPEAK_PRICE = 0.04f;
	public static final float DEFAULT_PEAK_PRICE = 0.07f;
	public static final long HOUR_MILLIS = 60*60000;
	
	/** Adapt these values to your provider*/
    public final static String ID = "electricity_profile_eval_provider";
    public final static String LABEL = "Building electricity profile evaluation provider";
    public final static String DESCRIPTION = "Provides analysis of electrictiy metering profile";
    
    public ElectricityProfileEvalProvider() {
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
	        	GaRoDataType.PowerMeter,
		};
	}
	
	/** It is recommended to define the indices of your input here.*/
	public static final int POWER_IDX = 0; 
        
 	public class EvalCore extends GenericGaRoEvaluationCore {
    	final long totalTime;
    	
    	/** Application specific state variables, see also documentation of the util classes used*/
    	public final InputSeriesAggregator consumption;
    	public final TimeSeriesOnlineBuilder tsBuilder;

    	ElectricityProfileEvalConfig configuration = null;
    	float peakPrice = DEFAULT_PEAK_PRICE;
    	float offPeakPrice = DEFAULT_OFFPEAK_PRICE;
    	float addPower = 0;
    	
    	double peakSum = 0;
    	double offpeakSum = 0;
    	
    	@SuppressWarnings("unchecked")
		public EvalCore(List<EvaluationInput> input, List<ResultType> requestedResults,
    			Collection<ConfigurationInstance> configurations, EvaluationListener listener, long time,
    			int size, int[] nrInput, int[] idxSumOfPrevious, long[] startEnd) {
    		//example how to calculate total time assuming offline evaluation
    		totalTime = startEnd[1] - startEnd[0];
 
    		consumption = new InputSeriesAggregator(nrInput, idxSumOfPrevious, POWER_IDX, startEnd[1],
    				null, AggregationMode.INTEGRATING);
    		tsBuilder = new TimeSeriesOnlineBuilder();
    		
            for (ConfigurationInstance cfg : configurations) {
                if (cfg.getConfigurationType().equals(OBJECT_CONFIGURATION)) {
                	configuration = ((ConfigurationInstance.GenericObjectConfiguration<ElectricityProfileEvalConfig>) cfg).getValue();
                    peakPrice = configuration.peakPrice().getValue();
                    offPeakPrice = configuration.offpeakPrice().getValue();
                    addPower = configuration.addPower().getValue();
                	break;
                }
            }
    	}
    	
    	/** In processValue the core data processing takes place. This method is called for each input
    	 * value of any input time series.*/
    	@Override
    	protected void processValue(int idxOfRequestedInput, int idxOfEvaluationInput,
    			int totalInputIdx, long timeStamp,
    			SampledValue sv, SampledValueDataPoint dataPoint, long duration) {
    		ValueDuration val = consumption.getCurrentValueDuration(idxOfEvaluationInput, sv, dataPoint, true);
    		
    		float addVal;
    		if(configuration != null) addVal = val.value + addPower;
    		else addVal = val.value;
			tsBuilder.addValue(new SampledValue(new FloatValue(addVal), timeStamp, sv.getQuality()));
			
			long startOfDay = AbsoluteTimeHelper.getIntervalStart(timeStamp, AbsoluteTiming.DAY);
			long timeInDay = timeStamp - startOfDay;
			int hourOfDay = (int) (timeInDay / HOUR_MILLIS);
			if (hourOfDay == 24) {
				// switching day-light savings time
				hourOfDay = 23;
			}
			
			//TODO: We ignore here that weekends are totally offpeak
			if(hourOfDay < 8 || hourOfDay >= 20) {
				offpeakSum += addVal * duration;
			} else {
				peakSum += addVal * duration;
			}
    	}
 	}
    
 	/**
 	 * Define the results of the evaluation here including the final calculation
 	 */
    public final static GenericGaRoResultType CLEAN_PROFILE_TS = new GenericGaRoResultType(
    		"Clean_Profile_TS", ID) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			return new TimeSeriesResultImpl(rt, cec.tsBuilder.getTimeSeries(), inputData);
		}
    };
    public final static GenericGaRoResultType PEAK_ENERGY = new GenericGaRoResultType("Peak_Energy",
    		"Total energy during peak hours", ID) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			return new SingleValueResultImpl<Float>(rt, (float) (cec.peakSum/HOUR_MILLIS), inputData);
		}
    };
    public final static GenericGaRoResultType OFFPEAK_ENERGY = new GenericGaRoResultType("Offpeak_Energy",
    		"Total energy during offpeak hours", ID) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			return new SingleValueResultImpl<Float>(rt, (float) (cec.offpeakSum/HOUR_MILLIS), inputData);
		}
    };
    public final static GenericGaRoResultType PRICE_PER_KWH_AV = new GenericGaRoResultType("Price_AV",
    		"Average price per kWh based on peak and offpeak price", ID) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			float cost = (float) ((cec.peakSum * cec.peakPrice + cec.offpeakSum * cec.offPeakPrice)/HOUR_MILLIS);
			float energy = (float) ((cec.peakSum + cec.offpeakSum)/HOUR_MILLIS);
			float price = cost / energy;
			return new SingleValueResultImpl<Float>(rt, price, inputData);
		}
    };
    private static final List<GenericGaRoResultType> RESULTS = Arrays.asList(CLEAN_PROFILE_TS,
    		PEAK_ENERGY, OFFPEAK_ENERGY, PRICE_PER_KWH_AV);
    
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
	
	public static final Configuration<GenericObjectConfiguration<ElectricityProfileEvalConfig>> OBJECT_CONFIGURATION =
			getConfiguration(ID, PEAK_ENERGY, ElectricityProfileEvalConfig.class);
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
