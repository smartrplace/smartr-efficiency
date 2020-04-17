package org.smartrplace.smarteff.defaultservice;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.ogema.accesscontrol.Constants;
import org.ogema.accesscontrol.SessionAuth;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.units.EnergyResource;
import org.ogema.core.model.units.LengthResource;
import org.ogema.core.model.units.PercentageResource;
import org.ogema.core.model.units.PhysicalUnitResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.ogema.util.extended.eval.widget.ConstantsUtilExtendedEval;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.EditPageBase;
import org.smartrplace.smarteff.util.editgeneric.CSVUploadButton;
import org.smartrplace.smarteff.util.editgeneric.CSVUploadWidgets;
import org.smartrplace.smarteff.util.editgeneric.CSVUploadWidgets.TimeseriesUploadListener;
import org.smartrplace.smarteff.util.editgeneric.DefaultWidgetProvider;
import org.smartrplace.smarteff.util.editgeneric.EditLineProvider;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;
import org.smartrplace.smarteff.util.editgeneric.TemplateDropdownLoc;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.calendar.datepicker.Datepicker;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.button.WindowCloseButton;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.html.html5.flexbox.FlexWrap;
import de.iwes.widgets.html.html5.flexbox.JustifyContent;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationBuilder;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilter;
import de.iwes.widgets.template.DisplayTemplate;

public class TSManagementPage extends EditPageGeneric<SmartEffTimeSeries> {
	public static final SimpleDateFormat TS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static final Map<OgemaLocale, String> SUPERBUTTON_TEXTS = new HashMap<>();

	//Entire object and all elements are optional (can be null)
	public static class ContextType {
		public Class<? extends Resource> type;
		public Map<OgemaLocale, Map<String, String>> innerMap;
	}
	
	//private static final String OPEN_TS_TEXT = "Open Timestamp";
	private static final Map<OgemaLocale, String> OPEN_TS_MAP = new HashMap<>();
	private static final Map<OgemaLocale, String> CLOSE_TS_MAP = new HashMap<>();
	private static final Map<OgemaLocale, String> SUBMIT_NOW_MAP = new HashMap<>();
	private static final Map<OgemaLocale, String> SUBMIT_TS_MAP = new HashMap<>();
	private static final Map<OgemaLocale, String> SUBMIT_DISABLED_MAP = new HashMap<>();
	static {
		OPEN_TS_MAP.put(OgemaLocale.ENGLISH, "Open Timestamp");
		OPEN_TS_MAP.put(OgemaLocale.GERMAN, "Zeitstempel eingeben");
		CLOSE_TS_MAP.put(OgemaLocale.ENGLISH, "Use Current Time");
		CLOSE_TS_MAP.put(OgemaLocale.GERMAN, "Aktuelle Zeit verwenden");
		SUBMIT_NOW_MAP.put(OgemaLocale.ENGLISH, "Submit for Now");
		SUBMIT_NOW_MAP.put(OgemaLocale.GERMAN, "Speichern (aktuell)");
		SUBMIT_TS_MAP.put(OgemaLocale.ENGLISH, "Submit for Timestamp");
		SUBMIT_TS_MAP.put(OgemaLocale.GERMAN, "Speichern (Zeitstempel)");
		SUBMIT_DISABLED_MAP.put(OgemaLocale.ENGLISH, "Submit");
		SUBMIT_DISABLED_MAP.put(OgemaLocale.GERMAN, "Speichern");
	}
	
	static {
		SUPERBUTTON_TEXTS.put(OgemaLocale.ENGLISH, System.getProperty("org.smartrplace.smarteff.defaultservice.SUPERBUTTON_TEXTS_EN", "Timeseries Administration..."));
		SUPERBUTTON_TEXTS.put(OgemaLocale.GERMAN, System.getProperty("org.smartrplace.smarteff.defaultservice.SUPERBUTTON_TEXTS_DE", "Verwaltung Zeitreihe..."));
	}
	protected Button activateTimestampButton;
	protected OgemaWidget newTimestamp;
	protected CSVUploadButton csvButton;
	protected CSVUploadButton csvImportButton;
	//protected TextField newComment;
	
	@SuppressWarnings("deprecation")
	@Override
	public void setData(SmartEffTimeSeries sr) {
		//setLabel("#humanreadable", EN, "Internal Name");
		//setLabel("#location", EN, "Resource Location", DE, "Interner Speicherort");
		//setLabel(sr.dataProviderId(), EN, "DataProvider");
		//setLabel(sr.sourceId(), EN, "SourceId");		
		if(!Boolean.getBoolean("org.smartrplace.smarteff.defaultservice.tsmanagementpage.removecsvupload")) {
			setLabel(sr.allowNanValues(), EN, "Not-a-number values (NaN) allowed", DE, "Ungültige Werte (NaN) in Zeitreihe zulässig");
		} else {
			WindowCloseButton closeTabButton = new WindowCloseButton(page, "closeTabButton", "Fertig");
			closeTabButton.addDefaultStyle(ButtonData.BOOTSTRAP_RED);
			EditLineProvider tabProv = new EditLineProvider() {
				@Override
				public OgemaWidget valueColumn() {
					return closeTabButton;
				}
			};
			setLineProvider("#tabClose", tabProv);
			setLabel("#tabClose", EN, "Tab schließen");			
		}
		//Label separatorLabel = new Label(page, "separatorLabel", "||");
				
		activateTimestampButton = new Button(page, "activateTimestampButton", OPEN_TS_MAP.get(OgemaLocale.ENGLISH)) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				SmartEffTimeSeries res = getReqData(req);
				boolean typeDef = isTypeDefined(res);
				if(res.fileType().isActive() || (!typeDef)) {
			        HttpSession session = req.getReq().getSession();
			        SessionAuth sauth = (SessionAuth) session.getAttribute(Constants.AUTH_ATTRIBUTE_NAME);
			        final String user = sauth.getName();
			        if(csvButton != null) {
				        if(user.equals("supermaster"))
				        	csvButton.setWidgetVisibility(true, req);
				        else
				        	csvButton.setWidgetVisibility(false, req);
						//svButton.enable(req);
						csvImportButton.setWidgetVisibility(true, req);
						//csvImportButton.disable(req);
			        }
				} else if(csvButton != null) {
					csvButton.setWidgetVisibility(false, req);
					//csvButton.disable(req);
					csvImportButton.setWidgetVisibility(true, req);
					//csvImportButton.enable(req);					
				}
				if(res.schedule().isActive() || (!typeDef)) {
					enable(req);
				} else disable(req);
			}
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				String befText = getText(req);
				if(!isTimeStampActive(befText)) setText(DefaultWidgetProvider.getLocalString(req.getLocale(), CLOSE_TS_MAP), req);
				else setText(DefaultWidgetProvider.getLocalString(req.getLocale(), OPEN_TS_MAP), req);
			}
		};
		
		if(Boolean.getBoolean("org.smartrplace.smarteff.defaultservice.tsmanagement.usedatepicker")) {
			newTimestamp = new Datepicker(page, "newTimestampPicker"+pid()) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				super.onGET(req);
				setWidgetVisibility(true, req);
				if(activateTimestampButton.isDisabled(req)) {
					disable(req);
				} else enable(req);
			}
			};
		} else {
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
		}
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
		
		TemplateDropdown<Integer> newValueDrop = new AddValueDropdown(page, "newValueDrop"+pid(), null) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(getInnerMapByReq(req) != null) {
					super.onGET(req);
					setWidgetVisibility(true, req);
					if(activateTimestampButton.isDisabled(req)) {
						disable(req);
					} else enable(req);
				}
				else
					setWidgetVisibility(false, req);
			}
			
			@Override
			protected Map<OgemaLocale, Map<String, String>> getInnerMap(OgemaHttpRequest req) {
				return getInnerMapByReq(req);
			}
		};
		TextField newValue = new NewValueTSTextField(page, "newValue"+pid()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean isFreeText(OgemaHttpRequest req) {
				SmartEffTimeSeries res = getResource(req);
				return (res.getName().endsWith("Text"));			
			}
			
			@Override
			protected SmartEffTimeSeries getResource(OgemaHttpRequest req) {
				return getReqData(req);
			}
			
			@Override
			protected long getFrameworkTime() {
				return appManExt.getFrameworkTime();
			}

			@Override
			public void onGET(OgemaHttpRequest req) {
				if(getInnerMapByReq(req) == null) {
					super.onGET(req);
					setWidgetVisibility(true, req);
					if(activateTimestampButton.isDisabled(req)) {
						disable(req);
					} else enable(req);
				}
				else
					setWidgetVisibility(false, req);
			}
		};
		/*TextField newValue = new TextField(page, "newValue"+pid()) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(getInnerMapByReq(req) == null) {
					setWidgetVisibility(true, req);
					if(activateTimestampButton.isDisabled(req)) {
						disable(req);
					} else enable(req);
				}
				else
					setWidgetVisibility(false, req);
			}
		};*/
		Button newValueButton = new SubmitTSValueButton(page, "newValueButton",
				newValue, newValueDrop, newTimestamp, newComment, alert, appManExt) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean submitForNow(OgemaHttpRequest req) {
				//String buttonText = activateTimestampButton.getText(req);
				return newTimestamp.isDisabled(req); //(!isTimeStampActive(buttonText)) &&
			}
			
			@Override
			protected boolean checkValue(OgemaHttpRequest req) {
				return (getInnerMapByReq(req) == null);
			}
			
			@Override
			protected ResourceInfo getResource(OgemaHttpRequest req) {
				ResourceInfo result = new ResourceInfo();
				result.res = getReqData(req);
				return result;
			}
			@Override
			protected Class<? extends Resource> getType(OgemaHttpRequest req) {
				return getValueType(req);
				/*ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
				Object ct = appData.getConfigInfo().context;
				if(ct == null || (!(ct instanceof Class))) return super.getType(req);
				return (Class<? extends Resource>) ct;*/
			}
			
			@Override
			protected boolean disableForSure(OgemaHttpRequest req) {
				return activateTimestampButton.isDisabled(req);
			}
		};
		newValueButton.setDefaultPadding("1em", false, true, false, true);
		Flexbox flexLineAdd = getHorizontalFlexBox(page, "addFlex"+pid(),
				newValue, newValueDrop, newValueButton);
		EditLineProvider addValueProv = new EditLineProvider() {
			@Override
			public OgemaWidget valueColumn() {
				return flexLineAdd;
			}
			@Override
			public OgemaWidget labelColumn() {
				final Map<OgemaLocale, String> innerMap = labels.get("#addValue");
				return new Label(page, "addValueLable"+pid()) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onGET(OgemaHttpRequest req) {
						Class<? extends Resource> type = getValueType(req);
						String unit = getEUHumanUnitString(type);
						if(unit == null) {
							SmartEffTimeSeries res = getReqData(req);
							if(res.recordedDataParent() instanceof PhysicalUnitResource)
								((PhysicalUnitResource)res).getUnit().toString();
							else unit = "";
						}
						String text = innerMap.get(req.getLocale());
						if(text == null) text = innerMap.get(localeDefault);
						if(text != null) {
							if(!unit.isEmpty())
								setText(text+" ("+unit+")", req);
							else
								setText(text, req);
						}
						else setText("*"+"#addValue"+"*", req);
					}
				};
			}
		};
		setLineProvider("#addValue", addValueProv);
		setLabel("#addValue", EN, "Enter new value", DE, "Neuer Messwert");
		setLineProvider("#tsValue", tsProv);
		setLabel("#tsValue", EN, "Provide time stamp of value (default: current time)", DE, "Zeitstempel für den Wert angeben (Default: Aktuelle Zeit)");
		setLineProvider("#commentValue", commentProv);
		setLabel("#commentValue", EN, "Add comment for new value", DE, "Kommentar zu dem neuen Wert");
		
		if(!Boolean.getBoolean("org.smartrplace.smarteff.defaultservice.tsmanagementpage.removecsvupload")) {
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
		}
		if(!Boolean.getBoolean("org.smartrplace.smarteff.defaultservice.tsmanagementpage.removecsvimport")) {
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
		}
		ScheduleViewerOpenButtonEval schedOpenButton = new ScheduleViewerOpenButtonEval(page, "schedOpenButton"+pid(),
				"Data Viewer",
				ScheduleViewerConfigProvTSMan.PROVIDER_ID, new ScheduleViewerConfigProvTSMan()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected ScheduleViewerConfiguration getViewerConfiguration(long startTime, long endTime,
					List<Collection<TimeSeriesFilter>> programs) {
				final ScheduleViewerConfiguration viewerConfiguration =
						ScheduleViewerConfigurationBuilder.newBuilder().setPrograms(programs).
						setStartTime(startTime).setEndTime(endTime).setShowManipulator(true).
						setShowCsvDownload(false).
						setShowIndividualConfigBtn(false).
						setShowPlotTypeSelector(true).
						build();
					return viewerConfiguration;
			}
			
			@Override
			protected List<TimeSeriesData> getTimeseries(OgemaHttpRequest req) {
				SmartEffTimeSeries res = getReqData(req);
				ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
				ReadOnlyTimeSeries ts = appData.getTimeseriesManagement().getTimeSeries(res);
				String label;
				if(Boolean.getBoolean("org.smartrplace.smarteff.defaultservice.tsmanagement.usemanualdatatsquickhacklabel")) {
					if(specialTSLabelProvider != null) {
						label = specialTSLabelProvider.provideLabel(res);
					} else
						label = res.getLocation();					
						
				} else {
					label = res.getLocation();					
				}
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
		
		/*RedirectButton openEvalButton = new LogicProvTableOpenButton(page, "openTSManButton", pid(),
				exPage, null);
		EditLineProvider logicProv = new EditLineProvider() {
			@Override
			public OgemaWidget valueColumn() {
				return openEvalButton;
			}
		};
		setLineProvider("#logicView", logicProv);
		setLabel("#logicView", EN, "Overview LogicProviders for timeseries", DE, "Überblick Rechner für die Zeitreihe");
		 */
		
		activateTimestampButton.registerDependentWidget(newTimestamp);
		activateTimestampButton.registerDependentWidget(newValueButton);
		activateTimestampButton.registerDependentWidget(newValue);
		if(!Boolean.getBoolean("org.smartrplace.smarteff.defaultservice.tsmanagementpage.removecsvupload")) {
			activateTimestampButton.registerDependentWidget(csvButton);
		}
		if(!Boolean.getBoolean("org.smartrplace.smarteff.defaultservice.tsmanagementpage.removecsvimport")) {
			activateTimestampButton.registerDependentWidget(csvImportButton);
		}
		if(alert != null) newValueButton.registerDependentWidget(alert);
		newValue.registerDependentWidget(newValueButton);
		newValueButton.registerDependentWidget(newValue);
		newValueDrop.registerDependentWidget(newValueButton);
	}

	protected boolean isTimeStampActive(String buttonText) {
		if(OPEN_TS_MAP.values().contains(buttonText)) return false;
		return true;
	}
	
	@Override
	public Class<SmartEffTimeSeries> primaryEntryTypeClass() {
		return SmartEffTimeSeries.class;
	}
	
	protected Class<? extends Resource> getValueType(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		Object ct = appData.getConfigInfo().context;
		if(ct == null || (!(ct instanceof ContextType)) || ((ContextType)ct).type == null) return FloatResource.class;
		ContextType ctt = (ContextType) ct;
		return ctt.type;
	}
	
	protected Map<OgemaLocale, Map<String, String>> getInnerMapByReq(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		Object ct = appData.getConfigInfo().context;
		if(ct == null || (!(ct instanceof ContextType)) || ((ContextType)ct).innerMap == null) return null;
		ContextType ctt = (ContextType) ct;
		return ctt.innerMap;
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
	
	public static class ResourceInfo {
		public SmartEffTimeSeries res;
		public Float min = null;
		public Float max = null;
	}
	public static abstract class SubmitTSValueButton extends Button {
		private static final long serialVersionUID = 1L;

		/**If true the widget shall be disabled even if other criteria would enable it*/
		protected abstract boolean disableForSure(OgemaHttpRequest req);
		protected boolean checkValue(OgemaHttpRequest req) {return true;}
		protected abstract ResourceInfo getResource(OgemaHttpRequest req);
		protected Class<? extends Resource> getType(OgemaHttpRequest req) {
			return FloatResource.class;
		}
		/**If false an external time stamp shall be used for submission*/
		protected abstract boolean submitForNow(OgemaHttpRequest req);
		/** Overwrite this if default conversion shall be overwritten
		 * @return null if no valid value could be obtained*/
		protected Float getValue(String value, Class<? extends Resource> type, OgemaHttpRequest req) {
			return getOGEMAValue(value, type);
		}

		private final TextField newValue2;
		private final TemplateDropdown<Integer> newValueDrop;
		//private final Datepicker newValueDrop;
		/** if null submitForNow needs to be true always*/
		private final TextField newTimestampTF;
		private final Datepicker newTimestampDP;
		/** may be null*/
		private final TextField newComment;
		private final Alert alert;
		private final ApplicationManagerSPExt appManExt;
		
		/** see {@link ObjectResourceGUIHelper#floadEdit()}
		 */
		int mode = 0;

		@SuppressWarnings("unchecked")
		public SubmitTSValueButton(WidgetPage<?> page, String id,
				OgemaWidget newValue, TextField newTimestamp, TextField newComment,
				Alert alert, ApplicationManagerSPExt appManExt) {
			this(page, id,
				(newValue instanceof TextField)?(TextField)newValue:null,
				(newValue instanceof TemplateDropdown)?(TemplateDropdown<Integer>)newValue:null,
				//(newValue instanceof Datepicker)?(Datepicker)newValue:null,
				newTimestamp, newComment, alert, appManExt);
		}
	
		public SubmitTSValueButton(WidgetPage<?> page, String id,
				TextField newValue,
				TemplateDropdown<Integer> newValueDrop,
				//Datepicker newValueDrop,
				//TextField newTimestamp,
				OgemaWidget newTimestamp,
				TextField newComment,
				Alert alert, ApplicationManagerSPExt appManExt) {
			super(page, id, System.getProperty("org.smartrplace.smarteff.defaultservice.timeseries.submitbuttonlabel",
					"Submit"));
			/*if(newValue instanceof TextField) {
				this.newValue2 = (TextField) newValue;
				newValueDrop = null;
			} else if(newValue instanceof TemplateDropdown) {
				this.newValue2 = null;
				this.newValueDrop = (TemplateDropdown<Integer>) newValue;
			} else throw new IllegalArgumentException("Unsupported widget type for newValue:"+newValue.getClass());*/
			this.newValue2 = newValue;
			this.newValueDrop = newValueDrop;
			if(newTimestamp instanceof TextField) {
				this.newTimestampTF = (TextField) newTimestamp;
				this.newTimestampDP = null;
			} else {
				this.newTimestampDP = (Datepicker) newTimestamp;
				this.newTimestampTF = null;
			}
			this.newComment = newComment;
			this.alert = alert;
			this.appManExt = appManExt;
		}
		
		@Override
		public void onGET(OgemaHttpRequest req) {
			//when disabled then text has to be set to Submit (not for now). If enabled we perform the check.
			setText(DefaultWidgetProvider.getLocalString(req.getLocale(), SUBMIT_DISABLED_MAP), req);
			
			if(disableForSure(req)) {
				disable(req);
				return;
			}
			//Dropdown has no invalid values
			ResourceInfo res = null;
			if(newValue2 != null) {
				res = getResource(req);
				if(res.res.getName().endsWith("Text")) {
					//Comment time series
					String val = newValue2.getValue(req);
					if((!val.isEmpty()) && getValue(val, getType(req), req) == null &&
							(!val.startsWith("*"))) {
						performEnable(req);
					} else
						disable(req);
					newValue2.setInputmode("text", req);
					return;				
				} else {
				newValue2.setInputmode("decimal", req);
				}
			}
			if(newValue2 != null && checkValue(req)) { //newValue2.isVisible(req)) {
				String val = newValue2.getValue(req);
				if(val.startsWith("*")) {
					disable(req);
					return;
				}
				if(val.toLowerCase().equals("nan")) {
					if(res.res.allowNanValues().getValue()) {
						performEnable(req);
					} else disable(req);
					return;
				}
				Float valf = getValue(val, getType(req), req); 
				if((valf == null) || (!EditPageGeneric.checkLimits(res.min, res.max, valf))) {
					disable(req);
					return;				
				}
				
			}
			performEnable(req);
		}
		private void performEnable(OgemaHttpRequest req) {
			if(submitForNow(req)) {
				setText(DefaultWidgetProvider.getLocalString(req.getLocale(), SUBMIT_NOW_MAP), req);
			} else setText(DefaultWidgetProvider.getLocalString(req.getLocale(), SUBMIT_TS_MAP), req);
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
			String notAllowedMessageUsed = "Invalid value!";
			String notAllowedMessageTS = "Invalid time series value!";

			Float value;
			ResourceInfo resInfo = null;
			if(newValue2 != null) {
				resInfo = getResource(req);
				if(resInfo.res.getName().endsWith("Text")) {
					//Comment time series
					final Long ts = getTimeStamp(notAllowedMessageTS, req);
					if(ts == null) return;
					String comment = newValue2.getValue(req);
					comment = comment.trim();
					if(!comment.isEmpty()) addComment(resInfo.res, comment, ts);
					Schedule sched = getSchedule(resInfo.res, req);
					setValue(sched , ConstantsUtilExtendedEval.COMMENT_ONLY_VALUE_TS, ts);
					if(!sched.isActive()) {
						sched.activate(false);
					}
					newValue2.setValue("*"+comment, req);
					return;				
				}
			}
			if((newValue2 != null)  && checkValue(req)) {
				String val = newValue2.getValue(req);
				if(val.toLowerCase().equals("nan")) {
					//ResourceInfo res = getResource(req);
					if(resInfo.res.allowNanValues().getValue()) {
						value = Float.NaN;
					} else  {
						if(alert != null) alert.showAlert(notAllowedMessageUsed+" (NaN)", false, req);
						return;
					}
				} else {
					value  = getValue(val, getType(req), req);
					if(value == null) {
						if(alert != null) alert.showAlert(notAllowedMessageUsed+":"+val, false, req);
						return;					
					}
				}
				//ResourceInfo res = getResource(req);
				if (!EditPageGeneric.checkLimits(resInfo.min, resInfo.max, value)) { //value < minimumAllowed) {
					if(alert != null) alert.showAlert(notAllowedMessageUsed, false, req);
					return;
				}
				/*if (value > maximumAllowed) {
					if(alert != null) alert.showAlert(notAllowedMessageUsed, false, req);
					return;
				}*/
			} else {
				value = (float)newValueDrop.getSelectedItem(req);
				//value = (float)newValueDrop.getDateLong(req);
			}
			final Long ts = getTimeStamp(notAllowedMessageTS, req);
			if(ts == null) return;
			//String buttonText = activateTimestampButton.getText(req);
			/*if(!submitForNow(req)) {
				String val = newTimestamp.getValue(req);
				try {
					ts = TS_FORMAT.parse(val).getTime();
				} catch (ParseException e) {
					alert.showAlert(notAllowedMessageTS+":"+val, false, req);
					return;
				}
			} else
				ts = appManExt.getFrameworkTime();*/
			SmartEffTimeSeries res = getResource(req).res;
			Schedule sched = getSchedule(res, req); //res.schedule();
			/*if(!sched.exists()) {
				Class<? extends Resource> type = getType(req);
				if(!type.equals(FloatResource.class))
					res.getSubResource("recordedDataParent", type);
				else
					res.recordedDataParent().create();
				res.recordedDataParent().program().create();
				sched.setAsReference(res.recordedDataParent().program());					
			}*/
			setValue(sched , value, ts);
			if(newComment != null) {
				String comment = newComment.getValue(req);
				comment = comment.trim();
				if(!comment.isEmpty()) addComment(res, comment, ts);
			}
			if(!sched.isActive()) {
				sched.activate(false);
			}
			if(newValue2 != null) {
				newValue2.setValue("*"+value, req);
			}
			
			if(alert != null) alert.showAlert("New value: " + value + " for "+TimeUtils.getDateAndTimeString(ts), true, req);
		}
		private Long getTimeStamp(String notAllowedMessageTS, OgemaHttpRequest req) {
			long ts;
			//String buttonText = activateTimestampButton.getText(req);
			if(!submitForNow(req)) {
				if(newTimestampTF != null) {
					String val = newTimestampTF.getValue(req);
					try {
						ts = TS_FORMAT.parse(val).getTime();
					} catch (ParseException e) {
						alert.showAlert(notAllowedMessageTS+":"+val, false, req);
						return null;
					}
				}else
					ts = newTimestampDP.getDateLong(req);
			} else
				ts = appManExt.getFrameworkTime();
			return ts;
		}
		private Schedule getSchedule(SmartEffTimeSeries res, OgemaHttpRequest req) {
			Schedule sched = res.schedule();
			if(!sched.exists()) {
				Class<? extends Resource> type = getType(req);
				if(!type.equals(FloatResource.class))
					res.getSubResource("recordedDataParent", type);
				else
					res.recordedDataParent().create();
				res.recordedDataParent().program().create();
				sched.setAsReference(res.recordedDataParent().program());					
			}
			return sched;
		}
	}
	
	public static void addComment(SmartEffTimeSeries res, String comment, long timestamp) {
		res.comments().create();
		res.commmentTimeStamps().create();
		ValueResourceUtils.appendValue(res.comments(), comment);
		ValueResourceUtils.appendValue(res.commmentTimeStamps(), timestamp);
		if(!res.comments().isActive()) res.comments().activate(false);
		if(!res.commmentTimeStamps().isActive()) res.commmentTimeStamps().activate(false);
	}

	/** Get comment for a timeseries
	 * 
	 * @param res
	 * @param timestamp
	 * @param maxDist if null the timeStamp must fit exactly, otherwise the next comment is used if it is
	 * 		within maxDist
	 * @param startOfDay if not null then the first comment within the day will be returned
	 * @return
	 */
	public static String getComment(SmartEffTimeSeries res, long timestamp, Long maxDist, Long startOfDay) {
		if(res.commmentTimeStamps().isActive()) {
			int len = res.commmentTimeStamps().size();
			if(len > 0) {
				Long lastv = null;
				int lastvIdx = -1;
				long[] tss = res.commmentTimeStamps().getValues();
				int idx = 0;
				for(long ts: tss) {
					if(startOfDay != null) {
						if(ts >= startOfDay && ((lastv == null)||(lastv<ts))) {
							lastv = ts;
							lastvIdx = idx;
						}						
					} else if(maxDist == null) {
						if(ts == timestamp) {
							lastv = 0l;
							lastvIdx = idx;
							break;							
						}
					} else {
						long diff = Math.abs(ts - timestamp);
						if((diff < maxDist) && ((lastv == null)||(diff < lastv))) {
							lastv = ts;
							lastvIdx = idx;
						}
					}
					idx++;
				}
				if(lastv != null) {
					return res.comments().getElementValue(lastvIdx);
				}
			}
		}
		return null;
	}	

	/** Convert from a European human standard value into OGEMA value (e.g. °C to K)
	 * 
	 * @param euHumValue value as expected by most continental European humans
	 * @param type
	 * @param req
	 * @return
	 */
	public static Float getOGEMAValue(String euHumValue, Class<? extends Resource> type) {
		try {
			float val =  Float.parseFloat(euHumValue);
			if(type.equals(TemperatureResource.class))
				return val + 273.15f;
			if(type.equals(EnergyResource.class))
				return val*3600000f;
			if(type.equals(PercentageResource.class))
				return val * 0.01f;
			return val;
		} catch (NumberFormatException | NullPointerException e) {
			return null;
		}
	}
	
	public static float getEUHumanValue(float ogemaValue, Class<? extends Resource> type) {
		if(type.equals(TemperatureResource.class))
			return ogemaValue - 273.15f;
		if(type.equals(EnergyResource.class))
			return ogemaValue *(1.0f/3600000f);
		if(type.equals(PercentageResource.class))
			return ogemaValue * 100;
		return ogemaValue;		
	}
	
	public static String getEUHumanUnitString(Class<? extends Resource> type) {
		if(type.equals(TemperatureResource.class))
			return "°C";
		if(type.equals(EnergyResource.class))
			return "kWh";
		if(type.equals(PercentageResource.class))
			return "%";
		if(type.equals(LengthResource.class))
			return "m";
		return null;
	}
	
	public static class AddValueDropdown extends TemplateDropdownLoc<Integer> {
		private static final long serialVersionUID = 1L;
		protected final Map<OgemaLocale, Map<String, String>> innerMapIn;
		protected Map<OgemaLocale, Map<String, String>> getInnerMap(OgemaHttpRequest req) {return null;}
		
		public AddValueDropdown(WidgetPage<?> page, String id,
			Map<OgemaLocale, Map<String, String>> innerMapIn) {
			super(page, id, innerMapIn == null);
			this.innerMapIn = innerMapIn;
			setTemplate(new DisplayTemplate<Integer>() {
				@Override
				public String getLabel(Integer object, OgemaLocale locale) {
					if(innerMapIn == null)
						throw new IllegalStateException("Have to use getLabel(OgemaHttpRequest) for flexible Map!");
					Map<String, String> inMapLoc = innerMapIn.get(locale);
					if(inMapLoc == null) inMapLoc = innerMapIn.get(OgemaLocale.ENGLISH);
					return inMapLoc.get(""+object);
				}
				
				@Override
				public String getId(Integer object) {
					return ""+object;
				}
			});
		}
		
		@Override
		public void onGET(OgemaHttpRequest req) {
			super.onGET(req);
			Map<OgemaLocale, Map<String, String>> innerMap;
			if(innerMapIn == null) {
				innerMap = getInnerMap(req);
			} else innerMap = innerMapIn;
			Map<String, String> inMapLoc = innerMap.get(OgemaLocale.ENGLISH);
			List<Integer> items = new ArrayList<>();
			for(String s: inMapLoc.keySet()) {
				items.add(Integer.parseInt(s));
			}
			update(items, req);
		}
			
		@Override
		public String getFlexLabel(Integer object, OgemaHttpRequest req) {
			OgemaLocale locale = req.getLocale();
			Map<OgemaLocale, Map<String, String>> innerMap;
			if(innerMapIn == null) {
				innerMap = getInnerMap(req);
			} else innerMap = innerMapIn;
			Map<String, String> inMapLoc = innerMap.get(locale);
			if(inMapLoc == null) inMapLoc = innerMap.get(OgemaLocale.ENGLISH);
			return inMapLoc.get(""+object);
		}
	}
	
	public static abstract class NewValueTSTextField extends TextField {
		private static final long serialVersionUID = 1L;
		
		public NewValueTSTextField(WidgetPage<?> page, String id) {
			super(page, id);
		}
		
		protected abstract boolean isFreeText(OgemaHttpRequest req);
		protected abstract SmartEffTimeSeries getResource(OgemaHttpRequest req);
		protected abstract long getFrameworkTime();
		
		@Override
		public void onGET(OgemaHttpRequest req) {
			SmartEffTimeSeries res = getResource(req);
			long startOfDay = AbsoluteTimeHelper.getIntervalStart(
					getFrameworkTime(), AbsoluteTiming.DAY);

			String enteredValue = getValue(req);
				if(enteredValue == null || enteredValue.isEmpty()) {
				if(isFreeText(req)) {
					String comment = getComment(res, -1, null, startOfDay);
					if(comment != null)
						setValue("*"+comment, req);
					/*if(res.commmentTimeStamps().isActive()) {
						int len = res.commmentTimeStamps().size();
						if(len > 0) {
							Long lastv = null;
							int lastvIdx = -1;
							long[] tss = res.commmentTimeStamps().getValues();
							int idx = 0;
							for(long ts: tss) {
								if(ts >= startOfDay && ((lastv == null)||(lastv<ts))) {
									lastv = ts;
									lastvIdx = idx;
								}
								idx++;
							}
							if(lastv != null) {
								setValue("*"+res.comments().getElementValue(lastvIdx), req);
							}
						}
					}*/
				} else if(res.schedule().isActive()) {
					SampledValue lastv = res.schedule().getPreviousValue(Long.MAX_VALUE);
					if(lastv != null && (lastv.getTimestamp() >= startOfDay)) {
						setValue("*"+lastv.getValue().getStringValue(), req);
					}
				}
			}
		}
		
	}
	
	public static final Map<OgemaLocale, String> HEADER_MAP = new LinkedHashMap<>();
	static {
		//TODO
		HEADER_MAP.put(EN, "Zeitreihen Details: ");
		HEADER_MAP.put(DE, "Zeitreihen Details: ");
	}

	@Override
	protected String getHeader(OgemaHttpRequest req) {
		return HEADER_MAP.get(req.getLocale())+ super.getHeader(req);
	}
	
	@Override
	public void changeMenuConfig(MenuConfiguration mc) {
		if(Boolean.getBoolean("org.smartrplace.smarteff.defaultservice.reducetopnavi")) {
			mc.setLanguageSelectionVisible(false);
			mc.setNavigationVisible(false);
		}
	}
	
	public static interface TSLabelProvider {
		String provideLabel(SmartEffTimeSeries res);
	}
	public static TSLabelProvider specialTSLabelProvider = null;
}