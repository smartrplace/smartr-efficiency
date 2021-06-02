package org.smartrplace.apps.hw.install.expert.plottest;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButton;
import org.ogema.externalviewer.extensions.ScheduleViwerOpenUtil;
import org.ogema.externalviewer.extensions.ScheduleViwerOpenUtil.SchedOpenDataProvider;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class ScheduleViewerTest {
	public static ScheduleViewerOpenButton getPlotButton(WidgetPage<?> page, String id,
			DatapointService dpService, final ApplicationManager appMan,//final HardwareInstallController controller2,
			final List<Datapoint> dpList,
			DefaultScheduleViewerConfigurationProviderExtended schedViewProv) {
		
		SchedOpenDataProvider provider = new SchedOpenDataProvider() {
				
			@Override
			public IntervalConfiguration getITVConfiguration() {
				return IntervalConfiguration.getDefaultDuration(IntervalConfiguration.ONE_DAY, appMan);
			}
			
			@Override
			public List<TimeSeriesData> getData(OgemaHttpRequest req) {
				List<TimeSeriesData> result = new ArrayList<>();
				OgemaLocale locale = req!=null?req.getLocale():null;
				for(Datapoint dp: dpList) {
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
		ScheduleViewerOpenButton result = ScheduleViwerOpenUtil.getScheduleViewerOpenButton(page, "plotButton"+id,
				"Plot", provider, schedViewProv, true);
		return result;
	}
}
