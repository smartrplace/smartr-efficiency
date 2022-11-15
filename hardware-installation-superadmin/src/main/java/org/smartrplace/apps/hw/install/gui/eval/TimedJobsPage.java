package org.smartrplace.apps.hw.install.gui.eval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.ogema.internationalization.util.LocaleHelper;
import org.ogema.util.timedjob.TimedJobMemoryDataImpl;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;
import org.smartrplace.gui.filtering.GenericFilterBase;
import org.smartrplace.gui.filtering.GenericFilterOption;
import org.smartrplace.gui.filtering.SingleFiltering;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.gui.filtering.SingleFilteringDirect;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;

@SuppressWarnings("serial")
public class TimedJobsPage extends ObjectGUITablePageNamed<TimedJobMemoryData, TimedJobConfig> {
	protected final ApplicationManagerPlus appManPlus;
	protected final DatapointService dpService;
	protected final boolean isEvalPage;
	protected final TimedJobPageType pageType;
	
	protected SingleFiltering<TimedJobMemoryData, TimedJobMemoryData> filterDrop;
	
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
	
	public enum TimedJobPageType {
		STANDARD,
		SPECIAL
	}
	
	public TimedJobsPage(WidgetPage<?> page, ApplicationManagerPlus appManPlus, boolean isEvalPage) {
		this(page, appManPlus, isEvalPage, null);
	}
	public TimedJobsPage(WidgetPage<?> page, ApplicationManagerPlus appManPlus, boolean isEvalPage,
			TimedJobPageType pageType) {
		super(page, appManPlus.appMan(), null);
		this.appManPlus = appManPlus;
		this.dpService = appManPlus.dpService();
		this.isEvalPage = isEvalPage;
		this.pageType = pageType;
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "Evaluation and Timed Jobs Overview";
	}
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		StaticTable topTable = new StaticTable(1, 4);
		
		/** This is not a really direct filter, but we just offer the types*/
		filterDrop = new SingleFilteringDirect<TimedJobMemoryData> (
				page, "subcDrop", OptionSavingMode.GENERAL, 10000, true) {

			@Override
			protected List<GenericFilterOption<TimedJobMemoryData>> getOptionsDynamic(OgemaHttpRequest req) {
				List<GenericFilterOption<TimedJobMemoryData>> result;
				result = new ArrayList<>();
				for(final String type: dpService.timedJobService().getAllTypes()) {
					GenericFilterOption<TimedJobMemoryData> other = new GenericFilterBase<TimedJobMemoryData>(LocaleHelper.getLabelMap(type)) {
			
						@Override
						public boolean isInSelection(TimedJobMemoryData object, OgemaHttpRequest req) {
							return object.prov().getType().equals(type);
						}
						
					};
					result.add(other);
				}
				return result;
			}
			
			@Override
			protected long getFrameworkTime() {
				return appMan.getFrameworkTime();
			}
		};
		filterDrop.registerDependentWidget(mainTable);
		
		topTable.setContent(0, 0, filterDrop);
		page.append(topTable);
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
			if(!TimedJobPageType.SPECIAL.equals(pageType)) {
				vh.registerHeaderEntry("Aligned interval");
				vh.registerHeaderEntry("Init run after");
			}
			vh.registerHeaderEntry("Type");
			vh.registerHeaderEntry("Start");
			if(!TimedJobPageType.SPECIAL.equals(pageType))
				vh.registerHeaderEntry("Once");
			addWidgetsPlus(object, vh, id, req, row, false);
			return;
		}
		if(!isEvalPage) {
			vh.stringLabel("ID", id, object.prov().id(), row);
			vh.intLabel("IDX", id, object.res().persistentIndex().getValue(), row, 0);
		}
		vh.timeLabel("Last start", id, object.lastRunStart(), row, 0);
		Label ldLab = vh.timeLabel("Last duration", id, object.lastRunDuration(), row, 4);
		if(object.lastRunDuration() > 1000)
			ldLab.addStyle(LabelData.BOOTSTRAP_RED, req);
		Label maxLab = vh.timeLabel("Max Dur", id, object.maxRunDuration(), row, 4);
		if(object.maxRunDuration() > 2000)
			maxLab.addStyle(LabelData.BOOTSTRAP_ORANGE, req);
		final boolean timerActive = object.isTimerActive();
		if(timerActive)
			vh.timeLabel("Scheduled", id, object.nextScheduledStart(), row, 0);
		vh.floatEdit("Interval (min)", id, object.res().interval(), row, alert, 0, Float.MAX_VALUE, "Negative values not allowed!", 0);
		if(!TimedJobPageType.SPECIAL.equals(pageType)) {
			vh.dropdown("Aligned interval", id, object.res().alignedInterval(), row, AbsoluteTiming.INTERVAL_NAME_MAP);
			vh.floatEdit("Init run after", id, object.res().performOperationOnStartUpWithDelay(), row, alert,
					-1, Float.MAX_VALUE, "Values below -1 not allowed!");
		}
		boolean isEval = object.prov().evalJobType()>0;
		//String type = (!isEval)?"Base":("Eval"+object.prov().evalJobType());
		String type = object.prov().getType()+"("+object.prov().evalJobType()+")";
		vh.stringLabel("Type", id, type, row);
		
		Button startButton = TimedJobMemoryDataImpl.getTimedJobStatusButton(object, mainTable, id, req, alert, timerActive);
		row.addCell("Start", startButton);
				
		if(!TimedJobPageType.SPECIAL.equals(pageType)) {
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
		Collection<TimedJobMemoryData> all = dpService.timedJobService().getAllProviders();
		List<TimedJobMemoryData> result = filterDrop.getFiltered(all, req);
		return result;
	}

	@Override
	public String getLineId(TimedJobMemoryData object) {
		return String.format("%05d", object.res().persistentIndex().getValue())+object.prov().id();
	}
	
}
