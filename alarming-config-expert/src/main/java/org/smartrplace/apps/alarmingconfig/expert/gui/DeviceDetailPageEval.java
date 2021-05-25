package org.smartrplace.apps.alarmingconfig.expert.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButton;
import org.ogema.externalviewer.extensions.ScheduleViwerOpenUtil;
import org.ogema.externalviewer.extensions.ScheduleViwerOpenUtil.SchedOpenDataProvider;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;

@SuppressWarnings("serial")
public class DeviceDetailPageEval extends DeviceDetailPageExpert {

	public static class EvalResults {
		int valueNum;
	}
	// AlarmConfiguration.location -> results
	protected Map<String, EvalResults> knownResults = new HashMap<>();
	protected EvalResults getEval(String location) {
		EvalResults result = knownResults.get(location);
		if(result == null) {
			result = new EvalResults();
			knownResults.put(location, result);
		}
		return result;
	}
	
	public DeviceDetailPageEval(WidgetPage<?> page, ApplicationManagerPlus appManPlus,
			AlarmingConfigAppController controller) {
		super(page, appManPlus, controller, true, true);
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "6. Alarming Evaluations Per Device";
	}
	
	@Override
	protected void addFinalWidgets(AlarmConfiguration object,
			ObjectResourceGUIHelper<AlarmConfiguration, AlarmConfiguration> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		if(req == null) {
			vh.registerHeaderEntry("values");
			vh.registerHeaderEntry("Value Chart");
			return;
		}
		String text;
		EvalResults eres = knownResults.get(object.getLocation());
		if(eres == null)
			text = "Calculate";
		else
			text = ""+eres.valueNum;
		Button updateEvalBut = new Button(mainTable, "baseEvalBut"+id, text, req) {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				EvalResults result = updateEval(object);
				String message = "Found "+result.valueNum+" values recorded. Please reload page.";
				System.out.println(message);
				alert.showAlert(message, true, req);
			}
		};
		updateEvalBut.registerDependentWidget(alert);
		row.addCell(WidgetHelper.getValidWidgetId("values"), updateEvalBut);
		
		SchedOpenDataProvider provider = new SchedOpenDataProvider() {
			
			@Override
			public IntervalConfiguration getITVConfiguration() {
				return getITVConfigurationInternal();
			}
			
			@Override
			public List<TimeSeriesData> getData(OgemaHttpRequest req) {
				List<TimeSeriesData> result = new ArrayList<>();
				Datapoint dpBase = getDatapoint(object, dpService);
				addDatapoint(dpBase, dpBase.label(null), result, true);
				return result;
			}
		};
		ScheduleViewerOpenButton plotButton = ScheduleViwerOpenUtil.getScheduleViewerOpenButton(vh.getParent(), "plotValuesButton"+id,
				"Plot", provider, ScheduleViewerConfigProvAlarmExp.getInstance(), req);
		
		row.addCell(WidgetHelper.getValidWidgetId("Value Chart"), plotButton);

	}
	
	protected EvalResults updateEval(AlarmConfiguration object) {
		int num = ValueResourceHelper.getRecordedData(object.sensorVal()).size();
		EvalResults eval = getEval(object.getLocation());
		eval.valueNum = num;
		return eval;
	}
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		
		Button allEvalBut = new Button(page, "allEvalBut", "Update all") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				Collection<AlarmConfiguration> all = getObjectsInTable(req);
				for(AlarmConfiguration ac: all) {
					if(!ac.sendAlarm().getValue())
						continue;
					updateEval(ac);
					updateAlarmEval(ac);
				}
				String message = "Updated evaluation for "+all.size()+" recorded data series.";
				System.out.println(message);
				alert.showAlert(message, true, req);
			}
		};
		allEvalBut.registerDependentWidget(alert);
		secondTable.setContent(0, 2, allEvalBut);
	}
}
