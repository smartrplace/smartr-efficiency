package org.smartrplace.apps.hw.install.gui.expert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButton;
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
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;

@SuppressWarnings("serial")
public class MainPageExpert extends MainPage {

	private static final String DATAPOINT_INFO_HEADER = "DP/Log/Transfer";

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
			vh.registerHeaderEntry("Log All");
			vh.registerHeaderEntry("Log None");
			vh.registerHeaderEntry("Delete");
			vh.registerHeaderEntry("Reset");
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
			vh.stringLabel(DATAPOINT_INFO_HEADER, id, text, row);
			Button logAll = new Button(vh.getParent(), "logAll"+id, "Log All", req) {
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
			row.addCell("Reset", resetButton);
		}
		
	}
}
