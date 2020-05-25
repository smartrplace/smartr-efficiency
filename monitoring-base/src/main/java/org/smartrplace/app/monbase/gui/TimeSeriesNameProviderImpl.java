package org.smartrplace.app.monbase.gui;

import java.util.List;
import java.util.Map.Entry;

import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.util.format.StringListFormatUtils;

import com.iee.app.evaluationofflinecontrol.util.ExportBulkData.ComplexOptionDescription;

import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;

public class TimeSeriesNameProviderImpl implements TimeSeriesNameProvider {
	protected final MonitoringController controller;

	public TimeSeriesNameProviderImpl(MonitoringController controller) {
		this.controller = controller;
	}
	
	@Override
	public String getShortNameForTypeI(GaRoDataTypeI dataType, TimeSeriesDataExtendedImpl tse) {
		String location = tse.label(null);
		return getShortNameForTypeI(dataType, location);
	}

	public String getShortNameForTypeI(GaRoDataTypeI dataType, String location) {
		String sensorType = getSensorType(location); //getSensorType(inputLabel, location);
		String room = controller.getRoomLabel(location, null);
		if(room.contains("?")) {
			room = location;
		}
		if(controller.appConfigData.expertMode().getValue())
			return StringListFormatUtils.getStringFromList(null, room, sensorType, ScheduleViewerOpenButtonEval.getDeviceShortId(location));
		else
			return StringListFormatUtils.getStringFromList(null, room, sensorType);
	}
	
	@Override
	public int compareInput(String shortName1, String shortName2) {
		if(shortName2.contains(GaRoMultiEvalDataProvider.BUILDING_OVERALL_ROOM_ID))
			return 1;
		else return -1;
	}

	public String getSensorType(String location) {
		GaRoDataTypeI locationType = null;
		for(Entry<String, List<ComplexOptionDescription>> e: controller.getDatatypesBaseExtended().entrySet()) {
			for(ComplexOptionDescription s: e.getValue()) {
				if(s.pathElement != null) {
					if(location.contains(s.pathElement))
						return e.getKey();
				} else {
					if(locationType == null)
						locationType = GaRoEvalHelper.getDataType(location);
					if(s.type.equals(locationType))
						return e.getKey();
				}
			}
		}
		return location;
	}
}
