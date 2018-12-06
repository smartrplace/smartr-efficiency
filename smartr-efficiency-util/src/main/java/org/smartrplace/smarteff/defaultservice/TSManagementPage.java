package org.smartrplace.smarteff.defaultservice;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.EditPageBase;
import org.smartrplace.smarteff.util.button.LogicProvTableOpenButton;
import org.smartrplace.smarteff.util.editgeneric.CSVUploadButton;
import org.smartrplace.smarteff.util.editgeneric.CSVUploadWidgets;
import org.smartrplace.smarteff.util.editgeneric.CSVUploadWidgets.TimeseriesUploadListener;
import org.smartrplace.smarteff.util.editgeneric.EditLineProvider;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.html.html5.flexbox.FlexWrap;
import de.iwes.widgets.html.html5.flexbox.JustifyContent;

public class TSManagementPage extends EditPageGeneric<SmartEffTimeSeries> {
	public static final SimpleDateFormat TS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static final Map<OgemaLocale, String> SUPERBUTTON_TEXTS = new HashMap<>();

	private static final String OPEN_TS_TEXT = "Open Timestamp";
	static {
		SUPERBUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Timeseries Administration...");
		SUPERBUTTON_TEXTS.put(OgemaLocale.GERMAN, "Verwaltung Zeitreihe...");
	}
	protected Button activateTimestampButton;
	protected TextField newTimestamp;
	protected CSVUploadButton csvButton;
	protected CSVUploadButton csvImportButton;
	//protected TextField newComment;
	
	@SuppressWarnings("deprecation")
	@Override
	public void setData(SmartEffTimeSeries sr) {
		setLabel("#humanreadable", EN, "Internal Name");
		setLabel("#location", EN, "Resource Location", DE, "Interner Speicherort");
		setLabel(sr.dataProviderId(), EN, "DataProvider");
		setLabel(sr.sourceId(), EN, "SourceId");		
		setLabel(sr.allowNanValues(), EN, "Not-a-number values (NaN) allowed", DE, "Ungültige Werte (NaN) in Zeitreihe zulässig");
		//Label separatorLabel = new Label(page, "separatorLabel", "||");
				
		activateTimestampButton = new Button(page, "activateTimestampButton", OPEN_TS_TEXT) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				SmartEffTimeSeries res = getReqData(req);
				boolean typeDef = isTypeDefined(res);
				if(res.fileType().isActive() || (!typeDef)) {
					csvButton.enable(req);
					csvImportButton.disable(req);					
				} else {
					csvButton.disable(req);
					csvImportButton.enable(req);					
				}
				if(res.schedule().isActive() || (!typeDef)) {
					enable(req);
				} else disable(req);
			}
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				String befText = getText(req);
				if(!isTimeStampActive(befText)) setText("Use Current Time", req);
				else setText(OPEN_TS_TEXT, req);
			}
		};
		
		newTimestamp = new TextField(page, "newTimestamp"+pid()) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				String buttonText = activateTimestampButton.getText(req);
				if(isTimeStampActive(buttonText) && (!activateTimestampButton.isDisabled(req))) {
					enable(req);						
				} else {
					long now = appManExt.getFrameworkTime();
					String text = TS_FORMAT.format(new Date(now));
					setValue(text, req);
					disable(req);
				}
			}
		};
		activateTimestampButton.setDefaultPadding("1em", false, true, false, true);
		
		Flexbox flexLineTS = getHorizontalFlexBox(page, "flexLineTS"+pid(),
				newTimestamp, activateTimestampButton);
		EditLineProvider tsProv = new EditLineProvider() {
			@Override
			public OgemaWidget valueColumn() {
				return flexLineTS;
			}
		};
		
		TextField newComment = new TextField(page, "newComment"+pid()) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				//String buttonText = activateTimestampButton.getText(req);
				if(!activateTimestampButton.isDisabled(req)) {
					enable(req);						
				} else {
					disable(req);
				}
			}
		};
		EditLineProvider commentProv = new EditLineProvider() {
			@Override
			public OgemaWidget valueColumn() {
				return newComment;
			}
		};
		
		TextField newValue = new TextField(page, "newValue"+pid()) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(activateTimestampButton.isDisabled(req)) disable(req);
				else enable(req);
				/*SmartEffTimeSeries res = getReqData(req);
				if(res.schedule().isActive()) {
					enable(req);
				} else disable(req);*/
			}
		};
		Button newValueButton = new SubmitTSValueButton(page, "newValueButton",
				newValue, newTimestamp, newComment, alert, appManExt) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean submitForNow(OgemaHttpRequest req) {
				//String buttonText = activateTimestampButton.getText(req);
				return newTimestamp.isDisabled(req); //(!isTimeStampActive(buttonText)) &&
			}
			
			@Override
			protected SmartEffTimeSeries getResource(OgemaHttpRequest req) {
				return getReqData(req);
			}
			
			@Override
			protected boolean disableForSure(OgemaHttpRequest req) {
				return activateTimestampButton.isDisabled(req);
			}
		};
		newValueButton.setDefaultPadding("1em", false, true, false, true);
		Flexbox flexLineAdd = getHorizontalFlexBox(page, "addFlex"+pid(),
				newValue, newValueButton);
		EditLineProvider addValueProv = new EditLineProvider() {
			@Override
			public OgemaWidget valueColumn() {
				return flexLineAdd;
			}
		};
		setLineProvider("#addValue", addValueProv);
		setLabel("#addValue", EN, "Enter new value", DE, "Neuer Messwert");
		setLineProvider("#tsValue", tsProv);
		setLabel("#tsValue", EN, "Provide time stamp of value (default: current time)", DE, "Zeitstempel für den Wert angeben (Default: Aktuelle Zeit)");
		setLineProvider("#commentValue", commentProv);
		setLabel("#commentValue", EN, "Add comment for new value", DE, "Kommentar zu dem neuen Wert");
		
		CSVUploadWidgets csvData = new CSVUploadWidgets(exPage, page, alert, pid(),
				"Upload CSV file", null) {
			
			@Override
			protected SmartEffTimeSeries getTSResource(OgemaHttpRequest req) {
				SmartEffTimeSeries res = getReqData(req);
				return res;
			}
		};
		csvData.uploader.getFileUpload().setDefaultPadding("1em", false, true, false, true);
		csvButton = csvData.csvButton;
		Flexbox flexLineCSV = getHorizontalFlexBox(page, "csvFlex"+pid(),
				csvData.csvButton, csvData.uploader.getFileUpload());
		EditLineProvider csvProv = new EditLineProvider() {
			@Override
			public OgemaWidget valueColumn() {
				return flexLineCSV;
			}
		};
		setLineProvider("#csv", csvProv);
		setLabel("#csv", EN,"CSV Upload (Standard mode)", DE, "CSV-Datei hochladen (Standardmodus)");
		
		TimeseriesUploadListener tsListener = new TimeseriesUploadListener() {
			@Override
			public void fileUploaded(ReadOnlyTimeSeries timeSeries, String filePath, OgemaHttpRequest req) {
				SmartEffTimeSeries res = getReqData(req);
				List<SampledValue> values = timeSeries.getValues(0);
				res.schedule().addValues(values);
			}
		};
		CSVUploadWidgets csvImportData = new CSVUploadWidgets(exPage, page, alert, pid()+"Imp",
				"Upload CSV file", tsListener ) {
			
			@Override
			protected SmartEffTimeSeries getTSResource(OgemaHttpRequest req) {
				SmartEffTimeSeries res = getReqData(req);
				return res;
			}
		};
		csvImportData.uploader.getFileUpload().setDefaultPadding("1em", false, true, false, true);
		csvImportButton = csvImportData.csvButton;
		Flexbox flexLineCSVImport = getHorizontalFlexBox(page, "csvImportFlex"+pid(),
				csvImportData.csvButton, csvImportData.uploader.getFileUpload());
		EditLineProvider csvImportProv = new EditLineProvider() {
			@Override
			public OgemaWidget valueColumn() {
				return flexLineCSVImport;
			}
		};
		setLineProvider("#csvImport", csvImportProv);
		setLabel("#csvImport", EN,"CSV Upload (Import mode - not for very large files)", DE, "CSV-Datei hochladen (Importmodus - nicht für sehr große Dateien)");

		ScheduleViewerOpenButtonEval schedOpenButton = new ScheduleViewerOpenButtonEval(page, "schedOpenButton"+pid(),
				"Schedule Viewer",
				ScheduleViewerConfigProvTSMan.PROVIDER_ID, new ScheduleViewerConfigProvTSMan()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected List<TimeSeriesData> getTimeseries(OgemaHttpRequest req) {
				SmartEffTimeSeries res = getReqData(req);
				ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
				ReadOnlyTimeSeries ts = appData.getTimeseriesManagement().getTimeSeries(res);
				String label = res.getLocation();
				return Arrays.asList(new TimeSeriesData[] {new TimeSeriesDataImpl(ts, label, label, null)});
			}

			@Override
			protected String getEvaluationProviderId(OgemaHttpRequest req) {
				return "Single time series from TSMan";
			}

			@Override
			protected IntervalConfiguration getITVConfiguration(OgemaHttpRequest req) {
				IntervalConfiguration result = new IntervalConfiguration();
				//TODO: Make start/end configurable with default options or other
				result.start = 0;
				result.end = appManExt.getFrameworkTime();
				return result ;
			}
	
		};
		EditLineProvider schedViewProv = new EditLineProvider() {
			@Override
			public OgemaWidget valueColumn() {
				return schedOpenButton;
			}
		};
		setLineProvider("#schedView", schedViewProv);
		setLabel("#schedView", EN,"Open Schedule Viewer", DE, "Plot/Bearbeiten");
		
		RedirectButton openEvalButton = new LogicProvTableOpenButton(page, "openTSManButton", pid(),
				exPage, null);
		EditLineProvider logicProv = new EditLineProvider() {
			@Override
			public OgemaWidget valueColumn() {
				return openEvalButton;
			}
		};
		setLineProvider("#logicView", logicProv);
		setLabel("#logicView", EN, "Overview LogicProviders for timeseries", DE, "Überblick Rechner für die Zeitreihe");

		
		activateTimestampButton.registerDependentWidget(newTimestamp);
		activateTimestampButton.registerDependentWidget(newValueButton);
		activateTimestampButton.registerDependentWidget(newValue);
		activateTimestampButton.registerDependentWidget(csvButton);
		activateTimestampButton.registerDependentWidget(csvImportButton);
		if(alert != null) newValueButton.registerDependentWidget(alert);
		newValue.registerDependentWidget(newValueButton);
	}

	protected boolean isTimeStampActive(String buttonText) {
		if(buttonText.equals(OPEN_TS_TEXT)) return false;
		return true;
	}
	
	@Override
	protected Class<SmartEffTimeSeries> primaryEntryTypeClass() {
		return SmartEffTimeSeries.class;
	}
	
	public static Flexbox getHorizontalFlexBox(WidgetPage<?> page, String id, OgemaWidget... w1) {
		Flexbox flex = EditPageBase.getHorizontalFlexBox(page, id, w1);
		flex.setDefaultJustifyContent(JustifyContent.FLEX_LEFT);
		flex.setDefaultFlexWrap(FlexWrap.NOWRAP);
		return flex;
	}

	public static boolean isTypeDefined(SmartEffTimeSeries res) {
		return res.schedule().isActive() || res.recordedDataParent().isActive() || res.fileType().isActive();
	}
	
	public static abstract class SubmitTSValueButton extends Button {
		private static final long serialVersionUID = 1L;

		/**If true the widget shall be disabled even if other criteria would enable it*/
		protected abstract boolean disableForSure(OgemaHttpRequest req);
		protected abstract SmartEffTimeSeries getResource(OgemaHttpRequest req);
		/**If false an external time stamp shall be used for submission*/
		protected abstract boolean submitForNow(OgemaHttpRequest req);
		
		private final TextField newValue;
		/** if null submitForNow needs to be true always*/
		private final TextField newTimestamp;
		/** may be null*/
		private final TextField newComment;
		private final Alert alert;
		private final ApplicationManagerSPExt appManExt;
		
		/** see {@link ObjectResourceGUIHelper#floadEdit()}
		 */
		int mode = 0;

		public SubmitTSValueButton(WidgetPage<?> page, String id,
				TextField newValue, TextField newTimestamp, TextField newComment,
				Alert alert, ApplicationManagerSPExt appManExt) {
			super(page, "newValueButton", "Submit");
			this.newValue = newValue;
			this.newTimestamp = newTimestamp;
			this.newComment = newComment;
			this.alert = alert;
			this.appManExt = appManExt;
		}

		@Override
		public void onGET(OgemaHttpRequest req) {
			if(disableForSure(req)) {
				disable(req);
				return;
			}
			String val = newValue.getValue(req);
			if(val.toLowerCase().equals("nan")) {
				SmartEffTimeSeries res = getResource(req);
				if(res.allowNanValues().getValue()) {
					enable(req);
				} else disable(req);
				return;
			}
			try {
				Float.parseFloat(val);
			} catch (NumberFormatException | NullPointerException e) {
				disable(req);
				return;
			}
			if(submitForNow(req)) {
				setText("Submit for Now", req);
			} else setText("Submit for Timestamp", req);
			enable(req);
		}
		private void setValue(Schedule sched, float value, long timestamp) {
			if((sched.getParent() != null)&&(sched.getParent() instanceof TemperatureResource)
					&&(mode == 0))
				value += 273.15f;
			sched.addValue(timestamp, new FloatValue(value));
		}
		@Override
		public void onPOSTComplete(String data, OgemaHttpRequest req) {
			String notAllowedMessageUsed = "Invalid time series value!";
			float maximumAllowed = 999999;
			float minimumAllowed = 0;

			String val = newValue.getValue(req);
			float value;
			if(val.toLowerCase().equals("nan")) {
				SmartEffTimeSeries res = getResource(req);
				if(res.allowNanValues().getValue()) {
					value = Float.NaN;
				} else  {
					if(alert != null) alert.showAlert(notAllowedMessageUsed+" (NaN)", false, req);
					return;
				}
			} else 	try {
				value  = Float.parseFloat(val);
			} catch (NumberFormatException | NullPointerException e) {
				if(alert != null) alert.showAlert(notAllowedMessageUsed, false, req);
				return;
			}
			if (value < minimumAllowed) {
				if(alert != null) alert.showAlert(notAllowedMessageUsed, false, req);
				return;
			}
			if (value > maximumAllowed) {
				if(alert != null) alert.showAlert(notAllowedMessageUsed, false, req);
				return;
			}
			
			long ts;
			//String buttonText = activateTimestampButton.getText(req);
			if(!submitForNow(req)) {
				val = newTimestamp.getValue(req);
				try {
					ts = TS_FORMAT.parse(val).getTime();
				} catch (ParseException e) {
					alert.showAlert("Invalid time stamp:"+val, false, req);
					return;
				}
			} else
				ts = appManExt.getFrameworkTime();
			SmartEffTimeSeries res = getResource(req);
			Schedule sched = res.schedule();
			if(!sched.exists()) {
				res.recordedDataParent().create();
				res.recordedDataParent().program().create();
				sched.setAsReference(res.recordedDataParent().program());					
			}
			setValue(sched , value, ts);
			if(newComment != null) {
				String comment = newComment.getValue(req);
				comment = comment.trim();
				if(!comment.isEmpty()) addComment(res, comment, ts);
			}
			if(!sched.isActive()) {
				sched.activate(false);
			}
			
			if(alert != null) alert.showAlert("New value: " + value + " for "+TimeUtils.getDateAndTimeString(ts), true, req);
		}
		private void addComment(SmartEffTimeSeries res, String comment, long timestamp) {
			res.comments().create();
			res.commmentTimeStamps().create();
			ValueResourceUtils.appendValue(res.comments(), comment);
			ValueResourceUtils.appendValue(res.commmentTimeStamps(), timestamp);
			if(!res.comments().isActive()) res.comments().activate(false);
			if(!res.commmentTimeStamps().isActive()) res.commmentTimeStamps().activate(false);
		}
	}
}
