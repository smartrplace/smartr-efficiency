package org.smartrplace.app.monbase.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.ogema.timeseries.eval.simple.api.TimeseriesSimpleProcUtil;
import org.smartrplace.app.monbase.MonitoringController;

import com.iee.app.evaluationofflinecontrol.gui.OfflineEvaluationControl.ScheduleViewerOpenButtonDataProvider;
import com.iee.app.evaluationofflinecontrol.util.ExportBulkData;
import com.iee.app.evaluationofflinecontrol.util.ExportBulkData.ComplexOptionDescription;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.resource.GaRoMultiEvalDataProviderResource;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public abstract class ScheduleViewerOpenButtonDataProviderImpl implements ScheduleViewerOpenButtonDataProvider{
	protected abstract GaRoSingleEvalProvider getEvalProvider(OgemaHttpRequest req);
	protected abstract List<String> getRoomIDs(OgemaHttpRequest req);
	/** Only relevant on server*/
	protected List<String> getGatewayIds(OgemaHttpRequest req) {return null;}
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
			gwIds = getGatewayIds(req);
			//gwIds = controller.getGwIDs(req);
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
		Set<ComplexOptionDescription> inputsToUse = new HashSet<>(); //ArrayList<>();
		
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
			List<ComplexOptionDescription> newInp = controller.getDatatypesBaseExtended().get(baselabel);
			try {
				inputsToUse.addAll(newInp);
			} catch(NullPointerException e) {
				e.printStackTrace();
			}
			for(ComplexOptionDescription locc: newInp) {
				if(locc.pathElement == null)
					continue;
				String locpart = locc.pathElement;
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
			TimeseriesSimpleProcUtil util = new TimeseriesSimpleProcUtil(controller.appMan, controller.dpService);
			List<TimeSeriesData> result = util.processTSD(tsProcessRequest, input, nameProvider(), controller);
			/*List<TimeSeriesData> result = new ArrayList<>();
			for(TimeSeriesData tsd: input) {
				if(!(tsd instanceof TimeSeriesDataImpl))
					continue;
				TimeSeriesDataImpl tsdi = (TimeSeriesDataImpl) tsd;
				if(tsProcessRequest.equals("METER")) {
					ProcessedReadOnlyTimeSeries2 newTs2 = new ProcessedReadOnlyTimeSeries2(tsdi, nameProvider(), controller) {
						@Override
						protected List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start,
								long end, AggregationMode mode) {
							MeterReference ref = new MeterReference();
							ref.referenceMeterValue = 0;
							TimeResource refRes = ResourceHelperSP.getSubResource(null,
									"offlineEvaluationControlConfig/energyEvaluationInterval/initialTest/start",
									TimeResource.class, controller.appMan.getResourceAccess());
							ref.referenceTime = refRes.getValue();
							return TimeSeriesServlet.getMeterFromConsumption(timeSeries, start, end, ref, mode);						
						}
						@Override
						protected String getLabelPostfix() {
							return "_vm";
						}
					}; 
					TimeSeriesDataExtendedImpl newtsdi = newTs2.getResultSeries();
					result.add(newtsdi);
				} else if(tsProcessRequest.equals("DAY")) {
					ProcessedReadOnlyTimeSeries2 newTs2 = new ProcessedReadOnlyTimeSeries2(tsdi, nameProvider(), controller) {
						@Override
						protected List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start,
								long end, AggregationMode mode) {
							return TimeSeriesServlet.getDayValues(timeSeries, start, end, mode, 1.0f);						
						}
						@Override
						protected String getLabelPostfix() {
							return "_proTag";
						}
					}; 
					TimeSeriesDataExtendedImpl newtsdi = newTs2.getResultSeries();
					result.add(newtsdi);				
				}
			}*/
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
