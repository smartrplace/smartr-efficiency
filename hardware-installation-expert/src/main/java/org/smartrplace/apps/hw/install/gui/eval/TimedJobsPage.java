package org.smartrplace.apps.hw.install.gui.eval;

import java.util.Collection;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ValueResourceHelper;
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
	protected final boolean isEvalPage;
	
	protected void addWidgetsPlus(final TimedJobMemoryData object, ObjectResourceGUIHelper<TimedJobMemoryData, TimedJobConfig> vh, String id,
			OgemaHttpRequest req, Row row,
			boolean isEval) {
		if(req == null ) {
			vh.registerHeaderEntry("Disable");
			vh.registerHeaderEntry("Reset");
			return;
		}
		
		if(!(object.isRunning() || object.triggeredForExecutionOnceOutsideTime())) {
			vh.booleanLabel("Disable", id, object.res().disable(), row, 0);

			ButtonConfirm resetButton = new ButtonConfirm(mainTable, "resetBut"+id, req) {
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					dpService.timedJobService().unregisterTimedJobProvider(object.prov());
					object.res().delete();
				}
			};
			resetButton.setDefaultText("Reset");
			resetButton.setDefaultConfirmMsg("Really delete config resource and remove service "+id+" until restart ?");
			row.addCell("Reset", resetButton);
		}
	}
	
	public TimedJobsPage(WidgetPage<?> page, ApplicationManagerPlus appManPlus, boolean isEvalPage) {
		super(page, appManPlus.appMan(), null);
		this.appManPlus = appManPlus;
		this.dpService = appManPlus.dpService();
		this.isEvalPage = isEvalPage;
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "Evaluation and Timed Jobs Overview";
	}
	
	@Override
	public void addWidgets(final TimedJobMemoryData object, ObjectResourceGUIHelper<TimedJobMemoryData, TimedJobConfig> vh, String id,
			OgemaHttpRequest req, Row row, final ApplicationManager appMan) {
		addNameLabel(object, vh, id, row, req);
		if(req == null) {
			if(!isEvalPage) {
				vh.registerHeaderEntry("ID");
				vh.registerHeaderEntry("IDX");
			}
			vh.registerHeaderEntry("Last start");
			vh.registerHeaderEntry("Last duration");
			vh.registerHeaderEntry("Max Dur");
			vh.registerHeaderEntry("Scheduled");
			vh.registerHeaderEntry("Interval (min)");
			vh.registerHeaderEntry("Aligned interval");
			vh.registerHeaderEntry("Init run after");
			vh.registerHeaderEntry("Type");
			vh.registerHeaderEntry("Start");
			vh.registerHeaderEntry("Once");
			addWidgetsPlus(object, vh, id, req, row, false);
			return;
		}
		if(!isEvalPage) {
			vh.stringLabel("ID", id, object.prov().id(), row);
			vh.intLabel("IDX", id, object.res().persistentIndex().getValue(), row, 0);
		}
		vh.timeLabel("Last start", id, object.lastRunStart(), row, 0);
		vh.timeLabel("Last duration", id, object.lastRunDuration(), row, 4);
		vh.timeLabel("Max Dur", id, object.maxRunDuration(), row, 4);
		final boolean timerActive = object.isTimerActive();
		if(timerActive)
			vh.timeLabel("Scheduled", id, object.nextScheduledStart(), row, 0);
		vh.floatEdit("Interval (min)", id, object.res().interval(), row, alert, 0, Float.MAX_VALUE, "Negative values not allowed!", 0);
		vh.dropdown("Aligned interval", id, object.res().alignedInterval(), row, AbsoluteTiming.INTERVAL_NAME_MAP);
		vh.floatEdit("Init run after", id, object.res().performOperationOnStartUpWithDelay(), row, alert,
				-1, Float.MAX_VALUE, "Values below -1 not allowed!");
		boolean isEval = object.prov().evalJobType()>0;
		String type = (!isEval)?"Base":("Eval"+object.prov().evalJobType());
		vh.stringLabel("Type", id, type, row);
		
		Button startButton;
		if((!timerActive) && (!object.canTimerBeActivated())) {
			startButton = new Button(mainTable, "startBut"+id, "Itv short", req);
			startButton.disable(req);
		} else {
			startButton = new Button(mainTable, "startBut"+id, timerActive?"Stop Timer":"Start timer", req) {
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					if(timerActive) {
						ValueResourceHelper.setCreate(object.res().disable(), true);
						object.stopTimerIfRunning();
						alert.showAlert("Stopped timer for service "+object.prov().label(req.getLocale()), true, req);
					}
					else {
						object.res().disable().setValue(false);
						int result = object.startTimerIfNotStarted();
						if(result == 0)
							alert.showAlert("Started timer for service "+object.prov().label(req.getLocale()), true, req);
						else
							alert.showAlert("Could not start timer for service "+object.prov().label(req.getLocale())+" ("+result+")", true, req);
					}
				}
			};
		}
		startButton.registerDependentWidget(alert);
		row.addCell("Start", startButton);
				
		if(object.isRunning()) {
			vh.stringLabel("Start", id, "Running", row);
		} else if(object.triggeredForExecutionOnceOutsideTime()) {
			vh.stringLabel("Start", id, "Once-trigger waiting", row);				
		} else {
			Button onceButton = new Button(mainTable, "onceBut"+id, "Trigger once", req) {
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					object.executeNonBlockingOnce();
				}
			};
			row.addCell("Once", onceButton);
		}
		
		addWidgetsPlus(object, vh, id, req, row, isEval);
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Provider Name";
	}

	@Override
	protected String getLabel(TimedJobMemoryData obj, OgemaHttpRequest req) {
		return obj.prov().label(req.getLocale());
	}

	@Override
	public Collection<TimedJobMemoryData> getObjectsInTable(OgemaHttpRequest req) {
		return dpService.timedJobService().getAllProviders();
	}

	@Override
	public String getLineId(TimedJobMemoryData object) {
		return object.prov().id();
	}
}
