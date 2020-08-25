package org.smartrplace.apps.hw.install.gui.expert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButton;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.tools.resource.util.LoggingUtils;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.apps.hw.install.gui.expert.ScheduleViwerOpenTemp.SchedOpenDataProvider;
import org.smartrplace.tissue.util.logconfig.LogTransferUtil;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;

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
	private static final List<String> ACTIONS = Arrays.asList(new String[] {LOG_ALL, LOG_NONE, DELETE, RESET, TRASH, MAKE_TEMPLATE});
	private static final List<String> ACTIONS_TEMPLATE = Arrays.asList(new String[] {LOG_ALL, LOG_NONE, DELETE, RESET, TRASH, APPLY_TEMPLATE});

	@Override
	protected String getHeader() {return "Smartrplace Hardware InstallationApp Expert";}

	public MainPageExpert(WidgetPage<?> page, final HardwareInstallController controller) {
		super(page, controller);
		
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

		final DeviceHandlerProvider<?> devHand = controller.handlerByDevice.get(object.getLocation());
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
						TimeSeriesDataExtendedImpl tsdExt = new TimeSeriesDataExtendedImpl(tsd, tsd.label(null), tsd.description(null));
						tsdExt.type = dp.getGaroDataType();
						result.add(tsdExt);
					}
					return result;
				}
			};
			ScheduleViewerOpenButton plotButton = ScheduleViwerOpenTemp.getScheduleViewerOpenButton(vh.getParent(), "plotButton"+id,
					"Plot", provider, req);
			row.addCell("Plot", plotButton);
			
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
					}
					setText(sel, req);
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					String sel = getText(req);
					switch(sel) {
					case LOG_ALL:
						controller.activateLogging(devHand, object, false, false);
						break;
					case LOG_NONE:
						controller.activateLogging(devHand, object, false, true);
						break;
					case DELETE:
						object.device().getLocationResource().delete();
						object.delete();
						break;
					case RESET:
						object.delete();
						break;
					case TRASH:
						performTrashOperation(object, devHand);
						break;
					case MAKE_TEMPLATE:
						InstallAppDevice currentTemplate = controller.getTemplateDevice(devHand);
						if(currentTemplate != null)
							currentTemplate.isTemplate().deactivate(false);
						ValueResourceHelper.setCreate(object.isTemplate(), devHand.id());
						if(!object.isTemplate().isActive())
							object.isTemplate().activate(false);
						break;
					case APPLY_TEMPLATE:
						for(InstallAppDevice dev: controller.getDevices(devHand)) {
							if(dev.equalsLocation(object))
								continue;
							controller.copySettings(object, dev);
						}
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

	protected void performTrashOperation(InstallAppDevice object, final DeviceHandlerProvider<?> devHand) {
		//deactivate logging
		controller.activateLogging(devHand, object, false, true);
		//remove all alarming
		for(AlarmConfiguration alarm: object.alarms().getAllElements()) {
			alarm.delete();
		}
		object.device().getLocationResource().deactivate(true);
		ValueResourceHelper.setCreate(object.isTrash(), true);		
	}
}
