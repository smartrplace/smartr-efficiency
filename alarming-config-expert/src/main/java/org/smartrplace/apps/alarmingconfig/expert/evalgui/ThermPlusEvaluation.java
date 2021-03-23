package org.smartrplace.apps.alarmingconfig.expert.evalgui;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.api.AlarmingExtension;
import org.ogema.devicefinder.api.AlarmingExtensionListener;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries;
import org.smartrplace.apps.alarmingconfig.eval.TimeseriesProcAlarming;
import org.smartrplace.apps.alarmingconfig.eval.TimeseriesProcAlarming.SetpReactInput;
import org.smartrplace.apps.alarmingconfig.model.eval.EvaluationByAlarmingOption;
import org.smartrplace.apps.alarmingconfig.model.eval.ThermPlusConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public class ThermPlusEvaluation implements AlarmingExtension {
	protected final DatapointService dpService;
	protected final TimeseriesProcAlarming tsProc;
	
	public ThermPlusEvaluation(TimeseriesProcAlarming tsProc, DatapointService dpService, ResourceList<EvaluationByAlarmingOption> optionList) {
		this.dpService = dpService;
		this.tsProc = tsProc;
		List<ThermPlusConfig> myConfigs = optionList.getSubResources(ThermPlusConfig.class, false);
		if(myConfigs.size() < 1) {
			optionList.create();
			ThermPlusConfig config = ResourceListHelper.getOrCreateNamedElementFlex(optionList, ThermPlusConfig.class);
			ValueResourceHelper.setCreate(config.maxIntervalBetweenNewValues(), 60f);
			ValueResourceHelper.setCreate(config.maxSetpointReactionTimeSeconds(), 60f);
			ValueResourceHelper.setCreate(config.isSelected(), true);
			optionList.activate(true);
		}
	}
	
	@Override
	public String id() {
		return ThermPlusEvaluation.class.getName();
	}

	@Override
	public String label(OgemaLocale locale) {
		return "Thermostat and similar evaluation";
	}

	EvaluationConfigurationExtensionDevice<ThermPlusConfig> myExt = null;
	@Override
	public EvaluationConfigurationExtensionDevice<?> getEvalConfigExtension() {
		if(myExt == null) {
			myExt = new EvaluationConfigurationExtensionDevice<ThermPlusConfig>() {

				@Override
				public void addWidgetsConfigRow(ThermPlusConfig object,
						ObjectResourceGUIHelper<ThermPlusConfig, ThermPlusConfig> vh, String id, OgemaHttpRequest req,
						Row row, Alert alert) {
					vh.floatEdit("MaxNoValue(min)", id, object.maxIntervalBetweenNewValues(), row, alert, -1, Float.MAX_VALUE, "Minimum value is -1");

					vh.floatEdit("MaxSetpReact(sec)", id, object.maxSetpointReactionTimeSeconds(), row, alert, -1, Float.MAX_VALUE, "Minimum value is -1");
				}

				@Override
				public Class<ThermPlusConfig> getType() {
					return ThermPlusConfig.class;
				}

				protected List<Datapoint> result = new ArrayList<>();
				
				@Override
				public List<Datapoint> updateEvaluation(List<InstallAppDevice> devices, ThermPlusConfig config,
						Long start, Long end, boolean useDefaultDatapoints) {
					if(!useDefaultDatapoints)
						throw new UnsupportedOperationException("Variants of result datapoints not supported yet!");
					for(InstallAppDevice dev: devices) {
						if(!(dev.device() instanceof Thermostat))
							continue;
						Thermostat phdev = (Thermostat) dev.device();
						Datapoint dpMes = dpService.getDataPointStandard(phdev.temperatureSensor().reading());
						Datapoint dpSetpReq = dpService.getDataPointStandard(phdev.temperatureSensor().settings().setpoint());
						Datapoint dpSetpFb = dpService.getDataPointStandard(phdev.temperatureSensor().deviceFeedback().setpoint());
						result.add(dpMes);
						result.add(dpSetpFb);
						result.add(dpSetpReq);
						addGapDatapoint(dpMes, config.maxIntervalBetweenNewValues().getValue(), "dpMesGap", phdev, tsProc, result);
						addGapDatapoint(dpSetpFb, config.maxIntervalBetweenNewValues().getValue(), "dpFbGap", phdev, tsProc, result);
						
						SetpReactInput input = new SetpReactInput();
						input.config = config;
						input.setpFb = dpSetpFb.getTimeSeries();
						Datapoint dpSetpReact = tsProc.processSingle(TimeseriesProcAlarming.SETPREACT_EVAL, dpSetpReq, input);
						((ProcessedReadOnlyTimeSeries)dpSetpReact.getTimeSeries()).reset(null);
						dpSetpReact.addAlias(phdev.getLocation()+"/$$dpSetpReact");
						result.add(dpSetpReact);
					}
					
					return result ;
				}

				@Override
				public int resetInternalDatapoints() {
					return 0;
				}
			};
		}
		return myExt;
	}
	
	@Override
	public boolean offerInGeneralAlarmingConfiguration(AlarmConfiguration ac) {
		//for now only evaluation
		return false;
	}

	@Override
	public AlarmingExtensionListener getListener(SingleValueResource res, AlarmConfiguration ac) {
		//as we provide evaluation only in first step we do not need this
		return null;
	}
	
	public static Datapoint addGapDatapoint(Datapoint dpMes, Float maxInterval, String subName, Resource phdev,
			TimeseriesProcAlarming tsProc, List<Datapoint> result) {
		Datapoint dpMesGap = tsProc.processSingle(TimeseriesProcAlarming.GAP_EVAL, dpMes, maxInterval);
		((ProcessedReadOnlyTimeSeries)dpMesGap.getTimeSeries()).reset(null);
		dpMesGap.addAlias(phdev.getLocation()+"/$$"+subName);
		result.add(dpMesGap);
		return dpMesGap;
	}
}
