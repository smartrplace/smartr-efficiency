package org.smartrplace.app.monbase.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.smartrplace.app.monbase.MonitoringController;

import com.iee.app.evaluationofflinecontrol.gui.OfflineEvaluationControl.ScheduleViewerOpenButtonDataProvider;
import com.iee.app.evaluationofflinecontrol.util.ExportBulkData;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
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
		String dataType = getDataType(req);
		if(dataType.equals(controller.getAllDataLabel()))
			return input;
		Set<String> inputsToUse = new HashSet<>(); //ArrayList<>();
		
		// For reverse conversion see InitUtil(?)
		List<String> baselabels = controller.getComplexOptions().get(dataType);
		if(baselabels == null)
			throw new IllegalStateException("unknown data type label:"+dataType);
		final List<TimeSeriesData> manualTsInput = new ArrayList<>();
		final List<String> roomIDsForManual;
		if(roomIDs.contains(controller.getAllRoomLabel(req!=null?req.getLocale():null))) {
			roomIDsForManual = controller.getAllRooms(req!=null?req.getLocale():null);
		} else
			roomIDsForManual = roomIDs;
		
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
						Schedule mansched = controller.getManualDataEntrySchedule(room, locpart.substring(1));
						if(mansched != null)
							manualTsInput.add(new TimeSeriesDataExtendedImpl(mansched,
									room+"-"+baselabel, room+"-"+baselabel, InterpolationMode.STEPS));
					}
				}
			}
		}
		ExportBulkData.cleanList(input, inputsToUse);
		input.addAll(manualTsInput);
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
