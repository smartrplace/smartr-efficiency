package org.smartrplace.smarteff.defaultservice;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.tools.resource.util.TimeUtils;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.EditPageBase;
import org.smartrplace.smarteff.util.editgeneric.CSVUploadButton;
import org.smartrplace.smarteff.util.editgeneric.CSVUploadWidgets;
import org.smartrplace.smarteff.util.editgeneric.EditLineProvider;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
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
	
	@SuppressWarnings("deprecation")
	@Override
	public void setData(SmartEffTimeSeries sr) {
		setLabel("#humanreadable", EN, "Internal Name");
		setLabel("#location", EN, "Resource Location", DE, "Interner Speicherort");
		setLabel(sr.dataProviderId(), EN, "DataProvider");
		setLabel(sr.sourceId(), EN, "SourceId");		
		setLabel(sr.allowNanValues(), EN, "Not-a-number values (NaN) allowed", DE, "Ungültige Werte (NaN) in Zeitreihe zulässig");
		//Label separatorLabel = new Label(page, "separatorLabel", "||");
		
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
		Button newValueButton = new Button(page, "newValueButton", "Submit") {
			private static final long serialVersionUID = 1L;
			/** see {@link ObjectResourceGUIHelper#floadEdit()}
			 */
			int mode = 0;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(activateTimestampButton.isDisabled(req)) {
					disable(req);
					return;
				}
				String val = newValue.getValue(req);
				if(val.toLowerCase().equals("nan")) {
					SmartEffTimeSeries res = getReqData(req);
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
				String buttonText = getText(req);
				if(isTimeStampActive(buttonText) && newTimestamp.isDisabled(req)) {
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
					SmartEffTimeSeries res = getReqData(req);
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
				String buttonText = activateTimestampButton.getText(req);
				if(isTimeStampActive(buttonText) && (!newTimestamp.isDisabled(req))) {
					val = newTimestamp.getValue(req);
					try {
						ts = TS_FORMAT.parse(val).getTime();
					} catch (ParseException e) {
						alert.showAlert("Invalid time stamp:"+val, false, req);
						return;
					}
				} else
					ts = appManExt.getFrameworkTime();
				SmartEffTimeSeries res = getReqData(req);
				Schedule sched = res.schedule();
				if(!sched.exists()) {
					res.recordedDataParent().create();
					res.recordedDataParent().program().create();
					sched.setAsReference(res.recordedDataParent().program());					
				}
				setValue(sched , value, ts);
				if(!sched.isActive()) {
					sched.activate(false);
				}
				
				if(alert != null) alert.showAlert("New value: " + value + " for "+TimeUtils.getDateAndTimeString(ts), true, req);
			}
		};
		//newValueButton.addWidget(newValue);
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
		
		activateTimestampButton = new Button(page, "activateTimestampButton", OPEN_TS_TEXT) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				SmartEffTimeSeries res = getReqData(req);
				boolean typeDef = isTypeDefined(res);
				if(res.fileType().isActive() || (!typeDef)) {
					csvButton.enable(req);
				} else csvButton.disable(req);
				if(res.schedule().isActive() || (!typeDef)) {
					enable(req);
				} else disable(req);
			}
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				String befText = getText(req);
				if(isTimeStampActive(befText)) setText("Use Current Time", req);
				else setText(OPEN_TS_TEXT, req);
			}
		};
		
		newTimestamp = new TextField(page, "newTimestamp"+pid()) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				//SmartEffTimeSeries res = getReqData(req);
				//if(res.schedule().isActive() ) {
				String buttonText = activateTimestampButton.getText(req);
				if(isTimeStampActive(buttonText) && (!activateTimestampButton.isDisabled(req))) {
					enable(req);						
				} else {
					long now = appManExt.getFrameworkTime();
					String text = TS_FORMAT.format(new Date(now));
					setValue(text, req);
					disable(req);
				}
				//} else disable(req);
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
		setLineProvider("#tsValue", tsProv);
		setLabel("#tsValue", EN, "Provide time stamp of value (default: current time)", DE, "Zeitstempel für den Wert angeben (Default: Aktuelle Zeit)");
		
		CSVUploadWidgets csvData = new CSVUploadWidgets(exPage, page, alert, pid(),
				"Upload CSV file") {
			
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
		setLabel("#csv", EN,"CSV Upload", DE, "CSV-Datei hochladen");
		
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
		
		activateTimestampButton.registerDependentWidget(newTimestamp);
		activateTimestampButton.registerDependentWidget(newValueButton);
		activateTimestampButton.registerDependentWidget(newValue);
		activateTimestampButton.registerDependentWidget(csvButton);
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
}
