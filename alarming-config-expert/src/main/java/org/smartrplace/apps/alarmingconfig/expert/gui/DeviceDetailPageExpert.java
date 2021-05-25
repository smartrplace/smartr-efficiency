package org.smartrplace.apps.alarmingconfig.expert.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButton;
import org.ogema.externalviewer.extensions.ScheduleViwerOpenUtil;
import org.ogema.externalviewer.extensions.ScheduleViwerOpenUtil.SchedOpenDataProvider;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.alarmingconfig.eval.TimeseriesProcAlarming;
import org.smartrplace.apps.alarmingconfig.gui.DeviceTypePage;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gui.filtering.DualFiltering2StepsStd;
import org.smartrplace.gui.filtering.GenericFilterFixedSingle;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;

@SuppressWarnings("serial")
public class DeviceDetailPageExpert extends DeviceTypePage {
	protected DualFiltering2StepsStd<InstallAppDevice, String, AlarmConfiguration> deviceDropLoc;
	
	protected void addFinalWidgets(AlarmConfiguration object,
			ObjectResourceGUIHelper<AlarmConfiguration, AlarmConfiguration> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		vh.stringLabel("Res.Location", id, object.getLocation(), row);		
	}
	
	public DeviceDetailPageExpert(WidgetPage<?> page, ApplicationManagerPlus appManPlus,
			AlarmingConfigAppController controller, boolean showReducedColumns, boolean showSuperAdmin) {
		super(page, appManPlus, false, controller, showReducedColumns, showSuperAdmin);
	}
	
	@Override
	protected String getHeader(OgemaLocale locale) {
		return "2. Alarming Details Per Device"+(showSuperAdmin?" Admin":"");
	}
	
	@Override
	public void addWidgets(AlarmConfiguration object,
			ObjectResourceGUIHelper<AlarmConfiguration, AlarmConfiguration> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		super.addWidgets(object, vh, id, req, row, appMan);
		if(req == null) {
			vh.registerHeaderEntry("Gap/Out");
			//vh.registerHeaderEntry("Gap/Out");
			vh.registerHeaderEntry("Chart");
			addFinalWidgets(object, vh, id, req, row, appMan);
			return;
		}
		long now = appMan.getFrameworkTime();
		long evalBasicInterval = controller.hwTableData.appConfigData.basicEvalInterval().getValue();
		long start = now - evalBasicInterval;
		Datapoint dpBase = getDatapoint(object, dpService);
		Datapoint dpGap = dpService.getDataPointAsIs(object.sensorVal().getLocation()+TimeProcUtil.ALARM_GAP_SUFFIX);
		Datapoint dpOut = dpService.getDataPointAsIs(object.sensorVal().getLocation()+TimeProcUtil.ALARM_OUTVALUE_SUFFIX);
	
		String text;
		if(dpGap != null && dpOut != null) {
			int gapNum = dpGap.getTimeSeries().getValues(start, now).size();
			int outNum = dpOut.getTimeSeries().getValues(start, now).size();
			text = gapNum+" / "+outNum;
		} else if(dpOut != null) {
			int outNum = dpOut.getTimeSeries().getValues(start, now).size();
			text = "-- / "+outNum;
		} else
			text = "Create";
		
		Button updateEvalBut = new Button(mainTable, "updateEvalBut"+id, text, req) {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				//Datapoint dpIn = dpService.getDataPointAsIs(object.sensorVal());
				//Datapoint gapDp = controller.tsProcAl.processSingle(TimeseriesProcAlarming.GAP_EVAL, dpIn, object);
				//Datapoint outDp = controller.tsProcAl.processSingle(TimeseriesProcAlarming.OUTVALUE_EVAL, dpIn, object);
				updateAlarmEval(object);
				String message = "Please update page: Created or updated alarming eval for "+object.sensorVal().getLocation();
				System.out.println(message);
				alert.showAlert(message, true, req);
			}
		};
		updateEvalBut.registerDependentWidget(alert);
		row.addCell(WidgetHelper.getValidWidgetId("Gap/Out"), updateEvalBut);
		//vh.stringLabel("Gap/Out", id, text, row);
		
		if(dpOut != null) {
			SchedOpenDataProvider provider = new SchedOpenDataProvider() {
				
				@Override
				public IntervalConfiguration getITVConfiguration() {
					return getITVConfigurationInternal();
					//return IntervalConfiguration.getDefaultDuration(IntervalConfiguration.ONE_WEEK, appMan);
				}
				
				@Override
				public List<TimeSeriesData> getData(OgemaHttpRequest req) {
					List<TimeSeriesData> result = new ArrayList<>();
					if(dpGap != null)
						addDatapoint(dpGap, dpBase.label(null)+"_gap", result, true);
					addDatapoint(dpOut, dpBase.label(null)+"_out", result, true);
					return result;
				}
			};
			ScheduleViewerOpenButton plotButton = ScheduleViwerOpenUtil.getScheduleViewerOpenButton(vh.getParent(), "plotButton"+id,
					"Plot", provider, ScheduleViewerConfigProvAlarmExp.getInstance(), req);
			
			row.addCell("Chart", plotButton);
		}
		addFinalWidgets(object, vh, id, req, row, appMan);
	}
	
	public static void addDatapoint(Datapoint dp, String label, List<TimeSeriesData> result, boolean plotRawValues) {
		TimeSeriesDataImpl tsd = dp.getTimeSeriesDataImpl(null);
		TimeSeriesDataExtendedImpl tsdExt = new TimeSeriesDataExtendedImpl(tsd, label, label);
		GaRoDataType garoType = dp.getGaroDataType();
		if(plotRawValues || garoType.id().equals(GaRoDataType.Unknown.id()))
			tsdExt.type = GaRoDataType.KPI_DURATION;
		else
			tsdExt.type = garoType;
		result.add(tsdExt);
		
	}
	
	@Override
	protected void addWidgetsAboveTableInternal() {
		deviceDropLoc = new DualFiltering2StepsStd<InstallAppDevice, String, AlarmConfiguration>(page, "deviceDropDual", OptionSavingMode.GENERAL,
				10000, false, true) {

			@Override
			protected Map<String, InstallAppDevice> getAttributesByGroup(String group) {
				DatapointGroup dtGrp = dpService.getGroup(group);
				if(dtGrp == null)
					throw new IllegalStateException("Unknown device type group id:"+group);
				Map<String, InstallAppDevice> result = new HashMap<>();
				for(DatapointGroup dpGrp: dtGrp.getSubGroups()) {
					InstallAppDevice dev = controller.getIAD(dpGrp.id());
					result.put(dev.deviceId().getValue(), dev);
				}
				return result ;
			}
			
			@Override
			protected String getGroupLabel(String grp) {
				final DatapointGroup dtGrp = dpService.getGroup(grp);
				if(dtGrp == null)
					throw new IllegalStateException("Unknown device type group id:"+grp);
				return dtGrp.label(null);
			}
			
			@Override
			protected List<String> getGroups(InstallAppDevice object) {
				DatapointGroup devTypeGrp = getDeviceTypeGroup(object);
				return Arrays.asList(new String[] {devTypeGrp.id()});
			}

			@Override
			protected boolean isGroupEqual(String group1, String group2) {
				return group1.equals(group2);
			}

			@Override
			protected long getFrameworkTime() {
				return appMan.getFrameworkTime();
			}

			@Override
			protected InstallAppDevice getAttribute(AlarmConfiguration attr) {
				InstallAppDevice iad = ResourceHelper.getFirstParentOfType(attr, InstallAppDevice.class);
				return iad;
			}

			@Override
			protected Collection<String> getAllGroups() {
				List<String> result = new ArrayList<>();
				for(DatapointGroup dpGrp: appManPlus.dpService().getAllGroups()) {
					if(dpGrp.getType() != null && dpGrp.getType().equals("DEVICE_TYPE")) {
						result.add(dpGrp.id());
					}
				}
				return result;
			}
		};
		//deviceDrop = deviceDropLoc;
		deviceDropLoc.registerDependentWidget(mainTable);
		
		ButtonConfirm applyDefaultToTemplate = new ButtonConfirm(page, "applyDefaultToTemplate") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				GenericFilterFixedSingle<InstallAppDevice> selectedFilter = (GenericFilterFixedSingle<InstallAppDevice>) deviceDropLoc.getSelectedItem(req);
				InstallAppDevice selected = selectedFilter.getValue();
				//InstallAppDevice template = AlarmingConfigUtil.getTemplate(selected.getValue(), appManPlus);
				if(selected != null) {
					DeviceHandlerProvider<?> devHand = controller.getDeviceHandler(selected);
					devHand.initAlarmingForDevice(selected, controller.getHardwareConfig());
					alert.showAlert("Default alarming settings applied to device for "+selectedFilter.getValue(), true, req);					
				} else
					alert.showAlert("Template for type "+selectedFilter.getValue()+" not found!", false, req);
			}
		};
		applyDefaultToTemplate.setDefaultConfirmMsg("Really overwrite settings of device"+
								" with default alarming settings?");
		applyDefaultToTemplate.setDefaultText("Apply default alarming settings to device");
		applyDefaultToTemplate.registerDependentWidget(alert);

		secondTable = new StaticTable(1, 4);
		secondTable.setContent(0, 0, deviceDropLoc.getFirstDropdown());
		secondTable.setContent(0, 1, deviceDropLoc);
		//secondTable.setContent(0, 2, "");
		secondTable.setContent(0, 3, applyDefaultToTemplate);
		
		page.append(secondTable);
	}

	@Override
	public Collection<AlarmConfiguration> getObjectsInTable(OgemaHttpRequest req) {
		Collection<AlarmConfiguration> all = appMan.getResourceAccess().getResources(AlarmConfiguration.class);
		return deviceDropLoc.getFiltered(all, req);
	}
	
	protected IntervalConfiguration getITVConfigurationInternal() {
		long evalBasicInterval = controller.hwTableData.appConfigData.basicEvalInterval().getValue();
		IntervalConfiguration r = new IntervalConfiguration();
		long now = appMan.getFrameworkTime();
		r.start = now - evalBasicInterval;
		r.end = now;
		return r;
	}

	protected Datapoint[] updateAlarmEval(AlarmConfiguration object) {
		Datapoint dpIn = dpService.getDataPointAsIs(object.sensorVal());
		Datapoint gapDp = null;
		if(object.maxIntervalBetweenNewValues().getValue() >= 0) {
			gapDp = controller.tsProcAl.processSingle(TimeseriesProcAlarming.GAP_EVAL, dpIn, object.maxIntervalBetweenNewValues().getValue());
			((ProcessedReadOnlyTimeSeries)gapDp.getTimeSeries()).reset(null);
		}
		Datapoint outDp = controller.tsProcAl.processSingle(TimeseriesProcAlarming.OUTVALUE_EVAL, dpIn, object);
		((ProcessedReadOnlyTimeSeries)outDp.getTimeSeries()).reset(null);
		return new Datapoint[] {gapDp, outDp};
	}
}
