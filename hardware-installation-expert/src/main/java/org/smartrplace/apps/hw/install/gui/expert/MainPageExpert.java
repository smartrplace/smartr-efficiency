package org.smartrplace.apps.hw.install.gui.expert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButton;
import org.ogema.externalviewer.extensions.ScheduleViwerOpenUtil;
import org.ogema.externalviewer.extensions.ScheduleViwerOpenUtil.SchedOpenDataProvider;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.tools.resource.util.LoggingUtils;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.tissue.util.logconfig.LogTransferUtil;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Label;

@SuppressWarnings("serial")
public class MainPageExpert extends MainPage {

	private static final String DATAPOINT_INFO_HEADER = "DP/Log/Transfer/Tmpl";
	private static final String LOG_ALL = "Log All";
	private static final String LOG_NONE = "Log None";
	private static final String DELETE = "Delete";
	private static final String RESET = "Reset";
	private static final String TRASH = "Mark as Trash";
	private static final String MAKE_TEMPLATE = "Make Template";
	private static final String APPLY_TEMPLATE = "Apply Template To Devices";
	private static final String APPLY_DEFAULT_ALARM = "Apply Default Alarm Settings";
	private static final List<String> ACTIONS = Arrays.asList(new String[] {LOG_ALL, LOG_NONE, DELETE, RESET, TRASH, MAKE_TEMPLATE, APPLY_DEFAULT_ALARM});
	private static final List<String> ACTIONS_TEMPLATE = Arrays.asList(new String[] {LOG_ALL, LOG_NONE, DELETE, RESET, TRASH, APPLY_TEMPLATE, APPLY_DEFAULT_ALARM});

	@Override
	protected String getHeader() {return "Device Setup and Configuration Expert";}

	public MainPageExpert(WidgetPage<?> page, final HardwareInstallController controller) {
		super(page, controller, true);
		
		Button updateDatapoints = new Button(page, "updateDatapoints", "Update Datapoints") {
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				for(InstallAppDevice iad: controller.appConfigData.knownDevices().getAllElements()) {
					DeviceHandlerProvider<?> tableProvider = controller.handlerByDevice.get(iad.getLocation());
					if(tableProvider != null)
						controller.updateDatapoints(tableProvider, iad);
				}
			}
		};
		topTable.setContent(0, 4, updateDatapoints);
	}
	
	@Override
	protected void finishConstructor() {
		StaticTable secondTable = new StaticTable(1, 4);
		RedirectButton stdCharts = new RedirectButton(page, "stdCharts", "Chart Config", "/org/sp/app/srcmon/chartconfig.html");
		RedirectButton alarming = new RedirectButton(page, "alarming", "Alarming Configuration", "/org/smartrplace/alarmingconfig/index.html");
		secondTable.setContent(0, 1, stdCharts).setContent(0,  2, alarming);
		page.append(secondTable);
		super.finishConstructor();
	}
	
	@Override
	public void addWidgetsExpert(final InstallAppDevice object,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		vh.stringLabel("IAD", id, object.getName(), row);
		vh.stringLabel("ResLoc", id, object.device().getLocation(), row);
		if(req == null) {
			vh.registerHeaderEntry(DATAPOINT_INFO_HEADER);
			vh.registerHeaderEntry("Plot");
			vh.registerHeaderEntry("Action");
			vh.registerHeaderEntry("Perform");
			/*vh.registerHeaderEntry("Log All");
			vh.registerHeaderEntry("Log None");
			vh.registerHeaderEntry("Delete");
			vh.registerHeaderEntry("Reset");*/
			return;
		}

		final GetPlotButtonResult logResult = getPlotButton(id, object, controller, true, vh, row, req);
		if(logResult.devHand != null) {
		/*final DeviceHandlerProvider<?> devHand = controller.handlerByDevice.get(object.getLocation());
		if(devHand != null) {
			final Collection<Datapoint> datapoints = devHand.getDatapoints(object, controller.dpService);
			int logged = 0;
			int transferred = 0;
			for(Datapoint dp: datapoints) {
				ReadOnlyTimeSeries ts = dp.getTimeSeries();
				if(ts == null || (!(ts instanceof RecordedData)))
					continue;
				RecordedData rec = (RecordedData)ts;
				if(LoggingUtils.isLoggingEnabled(rec))
					logged++;
				if(Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway")) {
					Resource res = appMan.getResourceAccess().getResource(rec.getPath());
					if(res != null && (res instanceof SingleValueResource) &&
							LogTransferUtil.isResourceTransferred((SingleValueResource) res, controller.datalogs)) {
						transferred++;
					}
				}
			}
			String text = ""+datapoints.size()+"/"+logged+"/"+transferred;
			final boolean isTemplate = DeviceTableRaw.isTemplate(object, devHand);
			if(isTemplate) {
				text += "/T";
			}
			vh.stringLabel(DATAPOINT_INFO_HEADER, id, text, row);
			
			SchedOpenDataProvider provider = new SchedOpenDataProvider() {
				
				@Override
				public IntervalConfiguration getITVConfiguration() {
					return IntervalConfiguration.getDefaultDuration(IntervalConfiguration.ONE_DAY, controller.appMan);
				}
				
				@Override
				public List<TimeSeriesData> getData(OgemaHttpRequest req) {
					List<TimeSeriesData> result = new ArrayList<>();
					OgemaLocale locale = req!=null?req.getLocale():null;
					for(Datapoint dp: datapoints) {
						TimeSeriesDataImpl tsd = dp.getTimeSeriesDataImpl(locale);
						if(tsd == null)
							continue;
						TimeSeriesDataExtendedImpl tsdExt = new TimeSeriesDataExtendedImpl(tsd, tsd.label(null), tsd.description(null));
						tsdExt.type = dp.getGaroDataType();
						result.add(tsdExt);
					}
					return result;
				}
			};
			ScheduleViewerOpenButton plotButton = ScheduleViwerOpenTemp.getScheduleViewerOpenButton(vh.getParent(), "plotButton"+id,
					"Plot", provider, req);*/
			//row.addCell("Plot", plotButton);
			row.addCell("Plot", logResult.plotButton);
			
			final boolean isTemplate = DeviceTableRaw.isTemplate(object, logResult.devHand);
			final TemplateDropdown<String> actionDrop = new TemplateDropdown<String>(vh.getParent(), "actionDrop"+id, req) {
				@Override
				public void onGET(OgemaHttpRequest req) {
					if(isTemplate) {
						update(ACTIONS_TEMPLATE, req);
					} else {
						update(ACTIONS, req);
					}
				}
			};
			actionDrop.setDefaultItems(ACTIONS);
			row.addCell("Action", actionDrop);
			ButtonConfirm performButton = new ButtonConfirm(vh.getParent(), WidgetHelper.getValidWidgetId("delBut"+id), req) {
				@Override
				public void onGET(OgemaHttpRequest req) {
					String sel = actionDrop.getSelectedItem(req);
					switch(sel) {
					case LOG_ALL:
						setConfirmMsg("Really log all datapoints for "+object.device().getLocation()+" ?", req);
						break;
					case LOG_NONE:
						setConfirmMsg("Really log no datapoints for "+object.device().getLocation()+" ?", req);
						break;
					case DELETE:
						setConfirmMsg("Really delete "+object.device().getLocation()+" ?", req);
						break;
					case RESET:
						setConfirmMsg("Really delete installation&setup configuration for "+object.device().getLocation()+
								" ? Search for new devices to recreate clean configuration.", req);
						break;
					case TRASH:
						setConfirmMsg(getTrashConfirmation(object), req);
						break;
					case MAKE_TEMPLATE:
						setConfirmMsg("Really move template status to "+object.device().getLocation()+" ?", req);
						break;
					case APPLY_TEMPLATE:
						setConfirmMsg("Really apply settings of "+object.device().getLocation()+
								" to all devices of the same type? Note that all settings will be overwritten without further confirmation!", req);
						break;
					case APPLY_DEFAULT_ALARM:
						setConfirmMsg("Really overwrite settings of " + object.device().getLocation() +
									"with default alarming settings?", req);
						break;
					}
					setText(sel, req);
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					String sel = getText(req);
					switch(sel) {
					case LOG_ALL:
						controller.activateLogging(logResult.devHand, object, false, false);
						break;
					case LOG_NONE:
						controller.activateLogging(logResult.devHand, object, false, true);
						break;
					case DELETE:
						object.device().getLocationResource().delete();
						object.delete();
						break;
					case RESET:
						object.delete();
						break;
					case TRASH:
						performTrashOperation(object, logResult.devHand);
						break;
					case MAKE_TEMPLATE:
						InstallAppDevice currentTemplate = controller.getTemplateDevice(logResult.devHand);
						if(currentTemplate != null)
							DeviceTableRaw.setTemplateStatus(currentTemplate, null, false);
							//currentTemplate.isTemplate().deactivate(false);
						DeviceTableRaw.setTemplateStatus(object, logResult.devHand, true);
						//ValueResourceHelper.setCreate(object.isTemplate(), logResult.devHand.id());
						//if(!object.isTemplate().isActive())
						//	object.isTemplate().activate(false);
						break;
					case APPLY_TEMPLATE:
						for(InstallAppDevice dev: controller.getDevices(logResult.devHand)) {
							if(dev.equalsLocation(object))
								continue;
							AlarmingConfigUtil.copySettings(object, dev, controller.appMan);
						}
						break;
					case APPLY_DEFAULT_ALARM:
						DeviceHandlerProvider<?> tableProvider = controller.handlerByDevice.get(object.getLocation());
						if(tableProvider != null)
							tableProvider.initAlarmingForDevice(object, controller.appConfigData);
						break;
					}
				}
			};
			row.addCell("Perform", performButton);
			actionDrop.registerDependentWidget(performButton, req);
			
			/*Button logAll = new Button(vh.getParent(), "logAll"+id, "Log All", req) {
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					controller.activateLogging(devHand, object, false, false);
				}
			};
			row.addCell(WidgetHelper.getValidWidgetId("Log All"), logAll);
			Button logNone = new Button(vh.getParent(), "logNone"+id, "Log None", req) {
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					controller.activateLogging(devHand, object, false, true);
				}
			};
			row.addCell(WidgetHelper.getValidWidgetId("Log None"), logNone);

			ButtonConfirm deleteButton = new ButtonConfirm(vh.getParent(), WidgetHelper.getValidWidgetId("delBut"+id), req) {
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					object.device().getLocationResource().delete();
					object.delete();
				}
			};
			deleteButton.setDefaultConfirmMsg("Really delete "+object.device().getLocation()+" ?");
			deleteButton.setDefaultText("Delete");
			row.addCell("Delete", deleteButton);
			ButtonConfirm resetButton = new ButtonConfirm(vh.getParent(), WidgetHelper.getValidWidgetId("resetBut"+id), req) {
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					object.delete();
				}
			};
			resetButton.setDefaultConfirmMsg("Really delete installation&setup configuration for "+object.device().getLocation()+" ? Search for new devices to recreate clean configuration.");
			resetButton.setDefaultText("Reset");
			row.addCell("Reset", resetButton);*/
		}
		
	}
	
	protected String getTrashConfirmation(InstallAppDevice object) {
		return "Really mark "+object.device().getLocation()+" as trash?";
	}

	public void performTrashOperation(InstallAppDevice object, final DeviceHandlerProvider<?> devHand) {
		//deactivate logging
		controller.activateLogging(devHand, object, false, true);
		//remove all alarming
		for(AlarmConfiguration alarm: object.alarms().getAllElements()) {
			IntegerResource status = AlarmingConfigUtil.getAlarmStatus(alarm.sensorVal());
			if(status.exists())
				status.delete();
			alarm.delete();
		}
		object.device().getLocationResource().deactivate(true);
		ValueResourceHelper.setCreate(object.isTrash(), true);		
	}
	
	public static class GetPlotButtonResult {
		DeviceHandlerProvider<?> devHand;
		Collection<Datapoint> datapoints;
		Label dataPointInfoLabel;
		ScheduleViewerOpenButton plotButton;
	}
	
	/** Create widgets. The dataPointInfoLabel is directly added to the row if requested,
	 * the plotButton needs to be added to the row by separate operation
	 * 
	 * @param id
	 * @param controller
	 * @param vh
	 * @param req
	 * @return
	 */
	public static GetPlotButtonResult getPlotButton(String id, InstallAppDevice object,
			final HardwareInstallController controller,
			boolean addDataPointInfoLabel,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, Row row, OgemaHttpRequest req) {
		final GetPlotButtonResult resultMain = new GetPlotButtonResult();
		
		resultMain.devHand = controller.handlerByDevice.get(object.getLocation());
		if(resultMain.devHand != null) {
			resultMain.datapoints = resultMain.devHand.getDatapoints(object, controller.dpService);
			int logged = 0;
			int transferred = 0;
			for(Datapoint dp: resultMain.datapoints) {
				ReadOnlyTimeSeries ts = dp.getTimeSeries();
				if(ts == null || (!(ts instanceof RecordedData)))
					continue;
				RecordedData rec = (RecordedData)ts;
				if(LoggingUtils.isLoggingEnabled(rec))
					logged++;
				if(Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway")) {
					Resource res = controller.appMan.getResourceAccess().getResource(rec.getPath());
					if(res != null && (res instanceof SingleValueResource) &&
							LogTransferUtil.isResourceTransferred((SingleValueResource) res, controller.datalogs)) {
						transferred++;
					}
				}
			}
			String text = ""+resultMain.datapoints.size()+"/"+logged+"/"+transferred;
			final boolean isTemplate = DeviceTableRaw.isTemplate(object, resultMain.devHand);
			if(isTemplate) {
				text += "/T";
			}
			if(addDataPointInfoLabel)
				resultMain.dataPointInfoLabel = vh.stringLabel(DATAPOINT_INFO_HEADER, id, text, row);
			
			SchedOpenDataProvider provider = new SchedOpenDataProvider() {
				
				@Override
				public IntervalConfiguration getITVConfiguration() {
					return IntervalConfiguration.getDefaultDuration(IntervalConfiguration.ONE_DAY, controller.appMan);
				}
				
				@Override
				public List<TimeSeriesData> getData(OgemaHttpRequest req) {
					List<TimeSeriesData> result = new ArrayList<>();
					OgemaLocale locale = req!=null?req.getLocale():null;
					for(Datapoint dp: resultMain.datapoints) {
						TimeSeriesDataImpl tsd = dp.getTimeSeriesDataImpl(locale);
						if(tsd == null)
							continue;
						TimeSeriesDataExtendedImpl tsdExt = new TimeSeriesDataExtendedImpl(tsd, tsd.label(null), tsd.description(null));
						tsdExt.type = dp.getGaroDataType();
						result.add(tsdExt);
					}
					return result;
				}
			};
			resultMain.plotButton = ScheduleViwerOpenUtil.getScheduleViewerOpenButton(vh.getParent(), "plotButton"+id,
					"Plot", provider, ScheduleViewerConfigProvHWI.getInstance(), req);
		}
		return resultMain;
	}
}
