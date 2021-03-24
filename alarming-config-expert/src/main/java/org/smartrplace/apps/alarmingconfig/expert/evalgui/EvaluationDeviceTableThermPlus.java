package org.smartrplace.apps.alarmingconfig.expert.evalgui;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.AlarmingExtension;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButton;
import org.ogema.externalviewer.extensions.ScheduleViwerOpenUtil;
import org.ogema.externalviewer.extensions.ScheduleViwerOpenUtil.SchedOpenDataProvider;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.alarmingconfig.expert.gui.DeviceDetailPageEval;
import org.smartrplace.apps.alarmingconfig.expert.gui.ScheduleViewerConfigProvAlarmExp;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.autoconfig.api.InitialConfig;
import org.smartrplace.hwinstall.basetable.HardwareTableData;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Label;

@SuppressWarnings("serial")
public abstract class EvaluationDeviceTableThermPlus extends DeviceTableBase {
	protected final String pageTitle;
	protected final HardwareTableData resData;
	protected final Button recalcAllButton;
	protected final AlarmingConfigAppController controller;
	protected final DeviceEvaluationPage devEvalPage;
	
	abstract protected AlarmingExtension getAlarmingEvalSelected(OgemaHttpRequest req);
	abstract protected long getStart(OgemaHttpRequest req);
	abstract protected long getEnd(OgemaHttpRequest req);
	
	public EvaluationDeviceTableThermPlus(WidgetPage<?> page, AlarmingConfigAppController controller, Alert alert,
			final String pageTitle,	final HardwareTableData resData, Button recalcAllButton,
			InstalledAppsSelector appSelector, DeviceHandlerProvider<?> devHand,
			DeviceEvaluationPage devEvalPage) {
		super(page, controller.appManPlus, alert, appSelector, devHand);
		this.pageTitle = pageTitle;
		this.resData = resData;
		this.recalcAllButton = recalcAllButton;
		this.controller = controller;
		this.devEvalPage = devEvalPage;
	}
	@Override
	protected String pid() {
		return WidgetHelper.getValidWidgetId(devHand.id());
	}
	
	@Override
	public void addWidgets(InstallAppDevice object,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		id = id + pid();  // avoid duplicates for now
		addWidgetsInternal(object, vh, id, req, row, appMan);
		//appSelector.addWidgetsExpert(object, vh, id, req, row, appMan);
	}

	@Override
	protected Class<? extends Resource> getResourceType() {
		return devHand.getResourceType();
	}

	@Override
	protected String id() {
		return devHand.id();
	}

	@Override
	protected String getTableTitle() {
		return pageTitle;
	}

	protected class ResultLabel {
		Label label;
		Datapoint dp;
	}
	protected ResultLabel addResultLabel(PhysicalElement device, String resName, String colName,
			long start, long end,
			ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, List<Datapoint> forPlot) {
		ResultLabel reslab = new ResultLabel();
		reslab.dp = controller.dpService.getDataPointAsIs(resName);
		if(reslab.dp == null) {
			reslab.label = vh.stringLabel(colName, id, "noDp", row);
			return reslab;
		}
		ReadOnlyTimeSeries ts = reslab.dp.getTimeSeries();
		if(ts == null) {
			reslab.label = vh.stringLabel(colName, id, "noTs", row);
			return reslab;
		}
		reslab.label = vh.intLabel(colName, id, ts.size(start, end), row, 0);
		if(forPlot != null) {
			forPlot.add(reslab.dp);
		}
		reslab.dp.addToSubRoomLocationAtomic(null, null, colName, false);
		return reslab;
	}
	
	/** TODO: We should provide the result table by the AlarmingExension as also the number of columns needs to vary*/
	public PhysicalElement addWidgetsInternal(final InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, final ApplicationManager appMan) {

		addNameWidget(object, vh, id, req, row, appMan);
		
		AlarmingExtension eval = getAlarmingEvalSelected(req);
		
		PhysicalElement device = object.device();

		if(req == null) {
			vh.registerHeaderEntry("MesNum");
			vh.registerHeaderEntry("FbNum");
			vh.registerHeaderEntry("ReqNum");
			
			vh.registerHeaderEntry("NoValue");
			vh.registerHeaderEntry("NoSetpFb");
			
			vh.registerHeaderEntry("ReqRealNum");
			vh.registerHeaderEntry("NoSetpReact");

			vh.registerHeaderEntry("NoValueMax");

			vh.registerHeaderEntry("Value Chart");
			vh.registerHeaderEntry("Source Chart");
		} else {
			int[] alNum = AlarmingConfigUtil.getActiveAlarms(object);
			vh.stringLabel("Active Alarms", id, String.format("%d / %d", alNum[0], alNum[1]), row);

			if(!(device instanceof Thermostat))
				return device;
			Thermostat therm = (Thermostat)device;
			long start = getStart(req);
			long end = getEnd(req);
			
			List<Datapoint> forPlot = new ArrayList<>();
			List<Datapoint> sourcePlot = new ArrayList<>();
			
			addResultLabel(device, therm.temperatureSensor().reading().getLocation(), "MesNum", start, end, vh, id, req, row, sourcePlot);
			addResultLabel(device, therm.temperatureSensor().deviceFeedback().setpoint().getLocation(), "FbNum", start, end, vh, id, req, row, sourcePlot);
			addResultLabel(device, therm.temperatureSensor().settings().setpoint().getLocation(), "ReqNum", start, end, vh, id, req, row, sourcePlot);
			
			ResultLabel mesGap = addResultLabel(device, therm.getLocation()+"/$$dpMesGap", "NoValue", start, end, vh, id, req, row, forPlot);
			ResultLabel fbGap = addResultLabel(device, therm.getLocation()+"/$$dpFbGap", "NoSetpFb", start, end, vh, id, req, row, forPlot);
			
			ResultLabel reqReal = addResultLabel(device, therm.getLocation()+"/$$dpSetpReqRealChange", "ReqRealNum", start, end, vh, id, req, row, forPlot);
			ResultLabel reactGap = addResultLabel(device, therm.getLocation()+"/$$dpSetpReact", "NoSetpReact", start, end, vh, id, req, row, forPlot);

			Float maxGapMes = TimeProcUtil.getMaxValue(mesGap.dp, start, end);
			Float maxGapFb = TimeProcUtil.getMaxValue(fbGap.dp, start, end);
			Float maxGapReact = TimeProcUtil.getMaxValue(reactGap.dp, start, end);
			String text = (maxGapMes!=null?String.format("%.0f", maxGapMes):"--")+" / "+(maxGapFb!=null?String.format("%.0f", maxGapFb):"--")
					+" / "+(maxGapReact!=null?String.format("%.0f", maxGapReact):"--");
			
			Button recalc = new Button(mainTable, "recalc"+id, req) {
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					List<InstallAppDevice> devices = new ArrayList<>();
					devices.add(object);
					devEvalPage.updateEval(devices , req);
				}
			};
			recalc.setText(text, req);
			row.addCell("NoValueMax", recalc);
			//vh.stringLabel("NoValueMax", id, text, row);

			SchedOpenDataProvider provider = new SchedOpenDataProvider() {
				
				@Override
				public IntervalConfiguration getITVConfiguration() {
					IntervalConfiguration itv = new IntervalConfiguration();
					itv.start = start;
					itv.end = end;
					return itv ;
				}
				
				@Override
				public List<TimeSeriesData> getData(OgemaHttpRequest req) {
					List<TimeSeriesData> result = new ArrayList<>();
					for(Datapoint dpBase: forPlot) {
						DeviceDetailPageEval.addDatapoint(dpBase, dpBase.label(null), result);						
					}
					return result;
				}
			};
			ScheduleViewerOpenButton plotButton = ScheduleViwerOpenUtil.getScheduleViewerOpenButton(vh.getParent(), "plotValuesButton"+id,
					"Plot Eval", provider, ScheduleViewerConfigProvAlarmExp.getInstance(), req);
			
			row.addCell(WidgetHelper.getValidWidgetId("Value Chart"), plotButton);

			SchedOpenDataProvider providerSource = new SchedOpenDataProvider() {
				
				@Override
				public IntervalConfiguration getITVConfiguration() {
					IntervalConfiguration itv = new IntervalConfiguration();
					itv.start = start;
					itv.end = end;
					return itv ;
				}
				
				@Override
				public List<TimeSeriesData> getData(OgemaHttpRequest req) {
					List<TimeSeriesData> result = new ArrayList<>();
					for(Datapoint dpBase: sourcePlot) {
						DeviceDetailPageEval.addDatapoint(dpBase, dpBase.label(null), result);						
					}
					return result;
				}
			};
			ScheduleViewerOpenButton plotSourceButton = ScheduleViwerOpenUtil.getScheduleViewerOpenButton(vh.getParent(), "ploSourcesButton"+id,
					"Plot Source", providerSource, ScheduleViewerConfigProvAlarmExp.getInstance(), req);
			
			row.addCell(WidgetHelper.getValidWidgetId("Source Chart"), plotSourceButton);
		}					
		Room deviceRoom = device.location().room();
		if(device == null || (!device.exists()))
			vh.stringLabel("Room", id, "--", row);
		else
			vh.stringLabel("Room", id, ResourceUtils.getHumanReadableShortName(deviceRoom), row);
		addSubLocation(object, vh, id, req, row);
		
		return device;
	}
	
	public PhysicalElement addNameWidget(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		final PhysicalElement device;
		if(req == null)
			device = ResourceHelper.getSampleResource(PhysicalElement.class);
		else
			device = object.device().getLocationResource();
		DatapointGroup devGrp = DpGroupUtil.getDeviceGroup(device.getLocation(), appManPlus.dpService(), false);
		String name;
		if(devGrp != null)
			name = devGrp.label(null);
		else
			name = ResourceUtils.getHumanReadableShortName(device);
		if(!InitialConfig.isInitDone(object.deviceId().getValue()+devHand.getInitVersion(), resData.appConfigData.initDoneStatus()))
			name += "*";
		vh.stringLabel("Name", id, name, row);
		vh.stringLabel("ID", id, object.deviceId().getValue(), row);
		return device;
	}	

}
