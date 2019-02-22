package org.ogema.timeseries.access.api.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.smartrplace.app.evaladm.EvalAdmController;
import org.smartrplace.smarteff.access.api.EvalButtonConfig;
import org.smartrplace.smarteff.access.api.EvalButtonConfigService;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.timeseries.eval.garo.api.base.EvaluationInputImplGaRo;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoSelectionItem;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.html.selectiontree.SelectionItem;

@Service(EvalButtonConfigService.class)
@Component
public class EvalButtonConfigServiceExpl implements EvalButtonConfigService {
	public static final String PROPERTY_NAME = "org.ogema.timeseries.access.api.example.toshow.";
	//TODO inject via constructor and start service in app
	public static EvalAdmController eac = null;
	
	protected class TsData {
		String dataProviderId;
		String gwId;
		String tsId;
		String orgString;
		TimeSeriesDataImpl found = null;
		String label = null;
	}
	//Map<String, List<TsData>> tsDataMap = new HashMap<>();
	
	@Override
	public List<EvalButtonConfig> configurations() {
		List<EvalButtonConfig> configs = new ArrayList<>();
		Properties props = System.getProperties();
		for(Object prop: props.keySet()) {
			if(!(prop instanceof String)) continue;
			String name = (String)prop;
			if(!name.startsWith(PROPERTY_NAME)) continue;
			String buttonText = name.substring(PROPERTY_NAME.length());
			String tsDef = System.getProperty(name);
			addButtonSetup(buttonText, tsDef, configs);
		}
		return configs;
	}
	
	protected <T extends GaRoSelectionItem> void addButtonSetup(String buttonText, String tsDef, List<EvalButtonConfig> configs) {
		if(tsDef == null) return;
		String[] tsStrings = tsDef.split(",");
		List<TsData> tsList = new ArrayList<>();
		for(String ts: tsStrings) {
			String[] els = ts.split("#");
			if(!(els.length == 3)) continue;
			TsData tsdata = new TsData();
			tsdata.dataProviderId = els[0];
			tsdata.gwId = els[1];
			tsdata.tsId = els[2];
			tsdata.orgString = ts;
			tsList.add(tsdata);
		}
		EvalButtonConfig stdButton = new EvalButtonConfig() {
			
			@Override
			public List<TimeSeriesData> timeSeriesToOpen() {
				List<TimeSeriesData> result = new ArrayList<>();
				for(TsData tsdata: tsList) {
					TimeSeriesDataImpl tsd = tsdata.found;
					if(tsd != null) {
						result.add(tsd);
						continue;
					}
					DataProvider<?> dp = eac.serviceAccess.getDataProviders().get(tsdata.dataProviderId);
					if(dp == null || (!(dp instanceof GaRoMultiEvalDataProvider)))
						continue;
					@SuppressWarnings("unchecked")
					GaRoMultiEvalDataProvider<T> dpg = (GaRoMultiEvalDataProvider<T>) dp;
					List<GaRoSelectionItem> gwItems = dpg.getSelectionItemsForGws(Arrays.asList(new String[]{tsdata.gwId}));
					if(gwItems.size() > 1)
						throw new IllegalStateException("Found more than one gw selection item for "+tsdata.orgString);
					else if(gwItems.isEmpty())
						continue;
					@SuppressWarnings("unchecked")
					List<SelectionItem> roomItems = dpg.getOptions(GaRoMultiEvalDataProvider.ROOM_LEVEL, (T) gwItems.get(0));
					SelectionItem roomItem = null;
					for(SelectionItem rit: roomItems) {
						if(rit.id().equals(GaRoMultiEvalDataProvider.BUILDING_OVERALL_ROOM_ID)) {
							roomItem = rit;
							break;
						}
					}
					if(roomItem == null)
						throw new IllegalStateException("No BUILDING_OVERALL_ROOM_ID for "+tsdata.orgString);
					@SuppressWarnings("unchecked")
					List<SelectionItem> opts = dpg.getOptions(GaRoMultiEvalDataProvider.TS_LEVEL, (T) roomItem);

					List<SelectionItem> items = new ArrayList<>();
					for(SelectionItem tsSel: opts) {
						if(tsSel.id().equals(tsdata.tsId)) {
							items.add(tsSel);
							break;
						}
					}
					EvaluationInputImplGaRo evalInput = dpg.getData(items);
					if(evalInput.getInputData().size() > 1) {
						throw new IllegalStateException("Found more than one time series for "+tsdata.orgString);
					}
					if(evalInput.getInputData().isEmpty())
						continue;
					TimeSeriesDataImpl tsi = (TimeSeriesDataImpl)evalInput.getInputData().get(0);
					List<String> ids2 = new ArrayList<>();
					ids2.add(tsdata.gwId);
					ids2.add(tsdata.tsId);
					Object type = null;
					if(tsdata.tsId.contains("temperatureSensor")) type = TemperatureResource.class;
					tsdata.found = new TimeSeriesDataExtendedImpl(tsi, ids2 , type);
					//ReadOnlyTimeSeries rots = tsdata.found.getTimeSeries();
					result.add(tsdata.found);
				}
				return result;
			}
			
			@Override
			public IntervalConfiguration getDefaultInterval() {
				IntervalConfiguration itv = new IntervalConfiguration();
				itv.end = eac.appMan.getFrameworkTime();
				itv.start = AbsoluteTimeHelper.getIntervalStart(itv.end, AbsoluteTiming.DAY);
				itv.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(itv.start, -4, AbsoluteTiming.DAY);
				return itv ;
			}
			
			@Override
			public String buttonText() {
				return buttonText;
			}
		};
		configs.add(stdButton);
	}

}
