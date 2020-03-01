package org.smartrplace.app.monbase.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.app.monbase.gui.TimeSeriesServlet.AggregationMode;

import com.iee.app.evaluationofflinecontrol.gui.OfflineEvaluationControl.ScheduleViewerOpenButtonDataProvider;
import com.iee.app.evaluationofflinecontrol.util.ExportBulkData;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.resource.GaRoMultiEvalDataProviderResource;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public abstract class ScheduleViewerOpenButtonDataProviderImpl implements ScheduleViewerOpenButtonDataProvider{
	protected abstract GaRoSingleEvalProvider getEvalProvider(OgemaHttpRequest req);
	protected abstract List<String> getRoomIDs(OgemaHttpRequest req);
	protected abstract String getDataType(OgemaHttpRequest req);
	protected final MonitoringController controller;
	
	public ScheduleViewerOpenButtonDataProviderImpl(MonitoringController controller) {
		this.controller = controller;
	}

	@Override
	public IntervalConfiguration getITVConfiguration(String config, ApplicationManager appMan) {
		return controller.getConfigDuration(config, controller.appMan);
	}
	
	@Override
	public List<TimeSeriesData> getData(OgemaHttpRequest req) {
		final GaRoSingleEvalProvider eval = getEvalProvider(req);
		List<GaRoMultiEvalDataProvider<?>> dps = controller.getDataProvidersToUse();
		GaRoMultiEvalDataProvider<?> dp = dps.get(0);
		final List<String> roomIDs = getRoomIDs(req);
		final List<String> gwIds;
		final List<TimeSeriesData> input;
		//roomIDs = (List<String>) roomSelection.multiSelect.getSelectedLabels(req);
		/*List<CheckboxEntry> entries = multiSelectRooms.getCheckboxList(req);
		roomIDs = new ArrayList<>();
		for(CheckboxEntry e: entries) {
			if(e.isChecked())
				roomIDs.add(e.label(null));
		}*/
		//On local GW we use provider ending on Resource
		if(dp.id().endsWith("Resource")) {
			gwIds = Arrays.asList(new String[] {GaRoMultiEvalDataProviderResource.LOCAL_GATEWAY_ID});
		} else {
			gwIds = controller.getGwIDs(req);
		}
		//We perform room filtering in cleanListByRooms, so we get data for all rooms here
		input = GaRoEvalHelper.getFittingTSforEval(dp, eval, gwIds, null);
		//else
		//	input = GaRoEvalHelper.getFittingTSforEval(dp, eval, gwIds, roomIDs);
		if((!roomIDs.contains(controller.getAllRoomLabel(req!=null?req.getLocale():null)))) {
			cleanListByRooms(input, roomIDs);
		}
		final String dataTypeOrg = getDataType(req);
		//final String dataType;
		final String tsProcessRequest;
		if(dataTypeOrg.contains("##")) {
			String[] parts = dataTypeOrg.split("##");
			//dataType = parts[0];
			tsProcessRequest = parts[1];
		} else {
			//dataType = dataTypeOrg;
			tsProcessRequest = null;			
		}
		if(dataTypeOrg.equals(controller.getAllDataLabel()))
			return input;
		Set<String> inputsToUse = new HashSet<>(); //ArrayList<>();
		
		// For reverse conversion see InitUtil(?)
		List<String> baselabels = controller.getComplexOptions().get(dataTypeOrg);
		if(baselabels == null)
			throw new IllegalStateException("unknown data type label:"+dataTypeOrg);
		final List<TimeSeriesData> manualTsInput = new ArrayList<>();
		final List<String> roomIDsForManual;
		if(roomIDs.contains(controller.getAllRoomLabel(req!=null?req.getLocale():null))) {
			roomIDsForManual = controller.getAllRooms(req!=null?req.getLocale():null);
		} else
			roomIDsForManual = roomIDs;
		
		List<String> done = new ArrayList<String>();
		for(String baselabel: baselabels) {
			List<String> newInp = controller.getDatatypesBase().get(baselabel);
			try {
			inputsToUse.addAll(newInp);
			} catch(NullPointerException e) {
				e.printStackTrace();
			}
			for(String locpart: newInp) {
				if(locpart.startsWith("#")) {
					for(String room: roomIDsForManual) {
						TimeSeriesData mansched = controller.getManualDataEntrySchedule(room, locpart.substring(1),
								baselabel);
						if(mansched != null && (!done.contains(mansched.label(null)))) {
							manualTsInput.add(mansched);
							done.add(mansched.label(null));
						}
					}
				}
			}
		}
		ExportBulkData.cleanList(input, inputsToUse);
		input.addAll(manualTsInput);
		if(tsProcessRequest != null) {
			List<TimeSeriesData> result = new ArrayList<>();
			for(TimeSeriesData tsd: input) {
				if(!(tsd instanceof TimeSeriesDataImpl))
					continue;
				TimeSeriesDataImpl tsdi = (TimeSeriesDataImpl) tsd;
				if(tsProcessRequest.equals("DAY")) {
					final AggregationMode mode;
					final String cparam = controller.getConfigParam(tsd.label(null));
					if(cparam != null && cparam.contains(AggregationMode.Consumption2Meter.name()))
						mode = AggregationMode.Consumption2Meter;
					else
						mode = AggregationMode.Meter2Meter;
					ProcessedReadOnlyTimeSeries newTs = new ProcessedReadOnlyTimeSeries(InterpolationMode.STEPS) {
						private Long lastTimestampInSource = null;
						
						@Override
						protected List<SampledValue> updateValues(long start, long end) {
							ReadOnlyTimeSeries ts = tsdi.getTimeSeries();
							if(lastTimestampInSource == null) {
								SampledValue sv = ts.getPreviousValue(Long.MAX_VALUE);
								if(sv != null)
									lastTimestampInSource = sv.getTimestamp();
								else
									return Collections.emptyList();
							} if(end > lastTimestampInSource)
								end = lastTimestampInSource;
							return TimeSeriesServlet.getDayValues(ts, start, end, mode, 1.0f);
						}
					};
					String shortId = tsd.label(null);
					if(tsdi instanceof TimeSeriesDataExtendedImpl) {
						TimeSeriesDataExtendedImpl tse = (TimeSeriesDataExtendedImpl) tsdi;
						if(tse.type != null && tse.type instanceof GaRoDataTypeI) {
							GaRoDataTypeI dataType = (GaRoDataTypeI) tse.type;
							shortId = nameProvider().getShortNameForTypeI(dataType, tse);
						}
					}
					TimeSeriesDataExtendedImpl newtsdi = new TimeSeriesDataExtendedImpl(newTs,
							shortId+"_proTag", tsd.description(null)+"_proTag", InterpolationMode.STEPS);
					result.add(newtsdi);
				}
			}
			return result;
		}
		return input;
	}
	
	@Override
	public TimeSeriesNameProvider nameProvider() {
		return new TimeSeriesNameProviderImpl(controller);
	}

	/** Like {@link ExportBulkData#cleanList(List, List, List)} but removes all sensors not
	 * belonging to rooms selected
	 * 
	 * @param input
	 * @param rooms
	 */
	public void cleanListByRooms(List<TimeSeriesData> input, List<String> rooms) {
		List<TimeSeriesData> toRemove = new ArrayList<>();
		for (TimeSeriesData tsdBase : input) {
			boolean found = false;
			for(String room: rooms) {
				if(controller.isTimeSeriesInRoom(tsdBase, room)) {
					found = true;
					break;
				}				
			}
			if(!found) {
				toRemove.add(tsdBase);
			}				
		}
		input.removeAll(toRemove);
	}
}
