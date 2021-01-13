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
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.alarmingconfig.gui.DeviceTypePage;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gui.filtering.DualFiltering2StepsStd;
import org.smartrplace.gui.filtering.GenericFilterFixedSingle;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;

@SuppressWarnings("serial")
public class DeviceDetailPageExpert extends DeviceTypePage {
	protected DualFiltering2StepsStd<InstallAppDevice, String, AlarmConfiguration> deviceDropLoc;
	
	public DeviceDetailPageExpert(WidgetPage<?> page, ApplicationManagerPlus appManPlus,
			AlarmingConfigAppController controller) {
		super(page, appManPlus, false, controller);
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
			vh.registerHeaderEntry("Res.Location");
			return;
		}
		long now = appMan.getFrameworkTime();
		long evalBasicInterval = controller.hwTableData.appConfigData.basicEvalInterval().getValue();
		long start = now - evalBasicInterval;
		Datapoint dpBase = getDatapoint(object, dpService);
		Datapoint dpGap = dpService.getDataPointAsIs(object.getLocation()+TimeProcUtil.ALARM_GAP_SUFFIX);
		Datapoint dpOut = dpService.getDataPointAsIs(object.getLocation()+TimeProcUtil.ALARM_OUTVALUE_SUFFIX);
	
		String text;
		if(dpGap != null && dpOut != null) {
			int gapNum = dpGap.getTimeSeries().getValues(start, now).size();
			int outNum = dpOut.getTimeSeries().getValues(start, now).size();
			text = gapNum+" / "+outNum;
		} else
			text = "Create";
		
		Button updateEvalBut = new Button(mainTable, "updateEvalBut"+id, text, req) {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				// TODO Auto-generated method stub
				super.onPOSTComplete(data, req);
			}
		};
		row.addCell("Gap/Out", updateEvalBut);
		//vh.stringLabel("Gap/Out", id, text, row);
		
		if(dpGap != null && dpOut != null) {
			SchedOpenDataProvider provider = new SchedOpenDataProvider() {
				
				@Override
				public IntervalConfiguration getITVConfiguration() {
					return IntervalConfiguration.getDefaultDuration(IntervalConfiguration.ONE_WEEK, appMan);
				}
				
				@Override
				public List<TimeSeriesData> getData(OgemaHttpRequest req) {
					List<TimeSeriesData> result = new ArrayList<>();
					addDatapoint(dpGap, dpBase.label(null)+"_gap", result);
					addDatapoint(dpOut, dpBase.label(null)+"_out", result);
					return result;
				}
			};
			ScheduleViewerOpenButton plotButton = ScheduleViwerOpenUtil.getScheduleViewerOpenButton(vh.getParent(), "plotButton"+id,
					"Plot", provider, ScheduleViewerConfigProvAlarmExp.getInstance(), req);
			
			row.addCell("Chart", plotButton);
		}
		
		vh.stringLabel("Res.Location", id, object.getLocation(), row);
	}
	
	protected void addDatapoint(Datapoint dp, String label, List<TimeSeriesData> result) {
		TimeSeriesDataImpl tsd = dp.getTimeSeriesDataImpl(null);
		TimeSeriesDataExtendedImpl tsdExt = new TimeSeriesDataExtendedImpl(tsd, label, label);
		tsdExt.type = dp.getGaroDataType();
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
			
			/*@Override
			protected List<GenericFilterOption<InstallAppDevice>> getOptionsDynamic(String group,
					OgemaHttpRequest req) {
				DatapointGroup dtGrp = dpService.getGroup(group);
				if(dtGrp == null)
					throw new IllegalStateException("Unknown device type group id:"+group);
				List<GenericFilterOption<InstallAppDevice>> result = new ArrayList<>();
				for(DatapointGroup dpGrp: dtGrp.getSubGroups()) {
					InstallAppDevice dev = controller.getIAD(dpGrp.id());
					//if(dpGrp.getType() != null && dpGrp.getType().equals("DEVICE_TYPE")) {
						GenericFilterOption<InstallAppDevice> option = new GenericFilterFixedSingle<InstallAppDevice>(
								dev, LocaleHelper.getLabelMap(dev.deviceId().getValue()));
						result.add(option);
					//}
				}
				return result;
			}*/

			@Override
			protected String getGroupLabel(String grp) {
				final DatapointGroup dtGrp = dpService.getGroup(grp);
				if(dtGrp == null)
					throw new IllegalStateException("Unknown device type group id:"+grp);
				return dtGrp.label(null);
			}
			
			/*@Override
			protected GenericFilterFixedGroup<InstallAppDevice, String> getGroupOptionDynamic(String grp) {
				final DatapointGroup dtGrp = dpService.getGroup(grp);
				if(dtGrp == null)
					throw new IllegalStateException("Unknown device type group id:"+grp);
				GenericFilterFixedGroup<InstallAppDevice, String> newOption = new GenericFilterFixedGroup<InstallAppDevice, String>(
						grp, LocaleHelper.getLabelMap(dtGrp.label(null))) {

					@Override
					public boolean isInSelection(InstallAppDevice object, String group) {
						throw new UnsupportedOperationException("Should not be used in first dropdown!");
						//return (dtGrp.getSubGroup(object.device().getLocation()) != null);
					}
				};
				return newOption;
			}*/

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
					alert.showAlert("Default alarming settings applied to template for "+selectedFilter.getValue(), true, req);					
				} else
					alert.showAlert("Template for type "+selectedFilter.getValue()+" not found!", false, req);
				//alert.showAlert("Currently only available as Installation&Setup Expert Dropdown Action!", false, req);
				//AlarmingConfigUtil.applyDefaultValuesToTemplate(selected.getValue(), appManPlus);
				//AlarmingConfigUtil.applyTemplate(selected.getValue(), appManPlus);
			}
		};
		applyDefaultToTemplate.setDefaultConfirmMsg("Really overwrite settings of template"+
								" with default alarming settings?");
		applyDefaultToTemplate.setDefaultText("Apply default settings to template");
		applyDefaultToTemplate.registerDependentWidget(alert);

		StaticTable secondTable = new StaticTable(1, 4);
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
}
