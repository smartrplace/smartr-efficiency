package org.smartrplace.apps.hw.install.gui.eval;

import java.util.Collection;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.TimedJobMemoryData;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;

@SuppressWarnings("serial")
public class TimedJobsPage extends ObjectGUITablePageNamed<TimedJobMemoryData, TimedJobConfig> {
	protected final ApplicationManagerPlus appManPlus;
	protected final DatapointService dpService;
	
	public TimedJobsPage(WidgetPage<?> page, ApplicationManagerPlus appManPlus) {
		super(page, appManPlus.appMan(), null);
		this.appManPlus = appManPlus;
		this.dpService = appManPlus.dpService();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "Timed Jobs on the system";
	}
	
	@Override
	public void addWidgets(final TimedJobMemoryData object, ObjectResourceGUIHelper<TimedJobMemoryData, TimedJobConfig> vh, String id,
			OgemaHttpRequest req, Row row, final ApplicationManager appMan) {
		if(req == null) {
			vh.registerHeaderEntry("ID");
			vh.registerHeaderEntry("Last start");
			vh.registerHeaderEntry("Last duration");
			vh.registerHeaderEntry("Interval (min)");
			vh.registerHeaderEntry("Aligned interval");
			vh.registerHeaderEntry("Init run after");
			vh.registerHeaderEntry("Disable");
			vh.registerHeaderEntry("Type");
			vh.registerHeaderEntry("Start");
			vh.registerHeaderEntry("Reset");
			return;
		}
		vh.stringLabel("ID", id, object.prov.id(), row);
		vh.timeLabel("Last start", id, object.lastRunStart, row, 0);
		vh.timeLabel("Last duration", id, object.lastRunDuration, row, 3);
		vh.floatEdit("Interval (min)", id, object.res.interval(), row, alert, 0, Float.MAX_VALUE, "Negative values not allowed!", 0);
		vh.dropdown("Aligned interval", id, object.res.alignedInterval(), row, AbsoluteTiming.INTERVAL_NAME_MAP);
		vh.floatEdit("Init run after", id, object.res.performOperationOnStartUpWithDelay(), row, alert,
				-1, Float.MAX_VALUE, "Values below -1 not allowed!");
		vh.booleanEdit("Disable", id, object.res.disable(), row);
		String type = object.prov.evalJobType()<=0?"Base":("Eval"+object.prov.evalJobType());
		vh.stringLabel("Type", id, type, row);
		if(object.prov.isRunning()) {
			vh.stringLabel("Start", id, "Running", row);
		} else {
			Button startButton = new Button(mainTable, "startBut"+id, "Start", req) {
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					object.executeNonBlocking(appMan);
				}
			};
			row.addCell("Start", startButton);
			
			ButtonConfirm resetButton = new ButtonConfirm(mainTable, "resetBut"+id, req) {
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					dpService.timedJobService().unregisterTimedJobProvider(object.prov);
					object.res.delete();
				}
			};
			resetButton.setDefaultText("Reset");
			resetButton.setDefaultConfirmMsg("Really delete config resource and remove service "+id+" until restart ?");
			row.addCell("Reset", resetButton);
		}
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Provider Name";
	}

	@Override
	protected String getLabel(TimedJobMemoryData obj, OgemaHttpRequest req) {
		return obj.prov.label(req.getLocale());
	}

	@Override
	public Collection<TimedJobMemoryData> getObjectsInTable(OgemaHttpRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

}
