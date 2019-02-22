package org.smartrplace.app.evaladm.gui;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.ResourceNotFoundException;
import org.ogema.externalviewer.extensions.SmartEffEditOpenButton;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;
import org.smartrplace.app.evaladm.EvalAdmController;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;

import com.iee.app.evaluationofflinecontrol.util.SmartEffPageConfigProvEvalOff;

import de.iwes.timeseries.eval.api.configuration.Configuration;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance.GenericObjectConfiguration;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.resource.widget.textfield.BooleanResourceCheckbox;


/**
 * An HTML page, generated from the Java code.
 */
public class MainPage {
	
	public final long UPDATE_RATE = 5*1000;
	private final WidgetPage<?> page; 
	private final EvalAdmController controller;
	private final DynamicTable<GaRoSingleEvalProvider> table;
	public DynamicTable<GaRoSingleEvalProvider> popupTable; 
	public int buttonID = 0;
	public  int preevalsID = 0;
	private EvalResultManagement evalResultMan;

	public MainPage(final WidgetPage<?> page, final EvalAdmController app) {
		
		this.page = page;
		this.controller = app;
		this.evalResultMan = controller.serviceAccess.evalResultMan(); //EvalResultManagementStd.getInstance(app.appMan);
		getHeader();
		
		table = new DynamicTable<GaRoSingleEvalProvider>(page, "evalviewtable") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				Collection<GaRoSingleEvalProvider> providers = controller.serviceAccess.getEvaluations().values()
						.stream().filter(provider -> provider instanceof GaRoSingleEvalProvider)
						.map(provider -> (GaRoSingleEvalProvider) provider)
						.collect(Collectors.toList());
				updateRows(providers, req);
				
			}
		};
		
		table.setRowTemplate(new RowTemplate<GaRoSingleEvalProvider>() {

			@Override
			public Row addRow(GaRoSingleEvalProvider eval, OgemaHttpRequest req) {
				buttonID++;
				Row row = new Row();
				return addBasicEvalProvider(row, eval);
			}

			private Row addBasicEvalProvider(Row row, final GaRoSingleEvalProvider eval) {
				String name = eval.id();
				String description = eval.description(OgemaLocale.ENGLISH);
				String resType = "";
				if(eval.resultTypes() != null)
					resType = Integer.toString(eval.resultTypes().size());
				RedirectButton configpage;
				BooleanResourceCheckbox autoEvalCheck;
				
				row.addCell("name", name);
				row.addCell("description", description);
				row.addCell("resulttypes", resType);

				autoEvalCheck = new BooleanResourceCheckbox(page, "autoEvalActive"+buttonID, "") {
					private static final long serialVersionUID = 1L;

					@Override
					public void onGET(OgemaHttpRequest req) {
						MultiKPIEvalConfiguration config = controller.getAutoEvalConfig(eval.id());
						if(config == null) {
							disable(req);
						} else {
							enable(req);
							selectItem(config.performAutoQueuing(), req);
						}
					}
				};
				row.addCell("autoEval", autoEvalCheck);

				/*configpage = new RedirectButton(page, "idconfig"+buttonID, "Start") {

					private static final long serialVersionUID = 1L;
					
					@Override
					public void onPrePOST(String data, OgemaHttpRequest req) {
						
						setUrl("OfflineEvaluationControl.html?configId=" + eval.id(), req);	
					}
				};*/
				configpage = new SmartEffEditOpenButton(page,
						"openConfigResourceEdit"+buttonID, "Config Resource",
						"admin/org_sp_example_smarteff_eval_capability_BuildingEvalParamsPage.html",
						SmartEffPageConfigProvEvalOff.PROVIDER_ID,
						new SmartEffPageConfigProvEvalOff()) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onGET(OgemaHttpRequest req) {
						List<Configuration<?>> configs = eval.getConfigurations();
						if(configs.isEmpty()) {
							setWidgetVisibility(false, req);
							return;
						}
						Configuration<?> config = configs.get(0);
						Class<?> type = config.configurationType();
						ConfigurationInstance ci = config.defaultValues();
						if(!(ci instanceof GenericObjectConfiguration))
							throw new IllegalStateException("ConfigurationInstance does not match type!");
						Object vals = ((GenericObjectConfiguration<?>)ci).getValue();
						if(type.equals(GenericObjectConfiguration.class) &&
								(vals instanceof Class)) {
							enable(req);
						} else disable(req);
					}
					
					@Override
					public void onPrePOST(String data, OgemaHttpRequest req) {
						Configuration<?> configLoc2= eval.getConfigurations().get(0);
						ConfigurationInstance ci = configLoc2.defaultValues();
						if(ci instanceof GenericObjectConfiguration) {
							GenericObjectConfiguration<?> goc = (GenericObjectConfiguration<?>)ci;
							Object typeIn = goc.getValue();
							if(!(typeIn instanceof Class)) {
								throw new IllegalStateException("Expecting resource type!");
							}
							//@SuppressWarnings("unchecked")
							//Class<? extends Resource> type = (Class<? extends Resource>)typeIn;
							
							//ChronoUnit chronoUnit = getChronoUnit(req);	
							//List<String> multiSelectedResults = (List<String>) resultsSelection.getSelectedLabels(req);
							//List<ResultType> resultsRequested = getResultsSelected(multiSelectedResults, eval);
							//List<String> gwIDs = (List<String>) gateWaySelection.multiSelect.getSelectedLabels(req);
							MultiKPIEvalConfiguration evalConfig = controller.getEvalConfig(eval, null, null, null);
							Resource configRes = evalResultMan.getEvalScheduler().getConfigurationResource(evalConfig, eval);
							if(!configRes.exists()) {
								configRes.create();
								List<MultiKPIEvalConfiguration> anyForProvider = evalResultMan.getEvalScheduler().getConfigs(eval.id());
								for(MultiKPIEvalConfiguration any: anyForProvider) {
									try {
										Resource anyRes = evalResultMan.getEvalScheduler().getConfigurationResource(any, eval);
										//any.getSubResource("configurationResource", type);
										if(anyRes.exists() && (!anyRes.equalsLocation(configRes)))
											System.out.println("Warning: Configuration "+anyRes.getLocation()+" already exists!");
										else
											anyRes.setAsReference(configRes);
									} catch(ResourceNotFoundException e) {
										System.out.println("Warning: Configuration resource of wrong type at "+any.getLocation());
									}
								}
							}
							String ci2 = addConfig(evalConfig.configurationResource(), "Configuration for EvaluationProvider "+eval.label(req.getLocale()));
							
							NavigationPublicPageData pageData = controller.appManSpExt.getMaximumPriorityPageStatic(evalConfig.configurationResource().getResourceType(), PageType.EDIT_PAGE);
							String url = pageData.getUrl();
							setConfigId(ci2, url, req);
						}
					}
				};

								
				row.addCell("configpage", configpage);
				configpage.addDefaultStyle(ButtonData.BOOTSTRAP_GREEN);
				
				return row;
			}
			@Override
			public String getLineId(GaRoSingleEvalProvider object) {
				return object.id();
			}

			@Override
			public Map<String, Object> getHeader() {
				final Map<String, Object> header = new LinkedHashMap<>();
				header.put("name", "Name/ID");
				header.put("description", "Description");
				header.put("resulttypes", "Result Types");
				header.put("autoEval", "Auto-Eval");
				header.put("configpage", "Config Page");
				return header;
			}
			
		});

		page.append(table).linebreak();	
		
		StaticTable evalButtonRow = new StaticTable(1, 4);
		for(int col=0; col<4; col++) {
			EvalButtonConfigured openScheduleViewer = new EvalButtonConfigured(page, "openScheduleViewer"+col, controller.evalButtonConfigServiceProvider, col, controller.appMan);
			evalButtonRow.setContent(0, col, openScheduleViewer);
		}
		
		/*int col = 0;
		for(EvalButtonConfig bc: controller.evalButtonConfigService.configurations()) {
			ScheduleViewerOpenButton openScheduleViewer = new ScheduleViewerOpenButtonEval(page, "openScheduleViewer"+col,
					bc.buttonText(), ScheduleViewerConfigProvEvalOff.PROVIDER_ID,
					ScheduleViewerConfigProvEvalOff.getInstance()) {
				private static final long serialVersionUID = 1L;

				@Override
				protected List<TimeSeriesData> getTimeseries(OgemaHttpRequest req) {
					List<TimeSeriesData> tsdList = new ArrayList<>();
					//int index = 0;
					for(TimeSeriesData ts: bc.timeSeriesToOpen()) {
						if(ts.label(req.getLocale()) == null) {
							if(ts instanceof TimeSeriesDataImpl) {
								String label = ScheduleViewerUtil.getScheduleShortName(((TimeSeriesDataImpl) ts).getTimeSeries(), controller.appMan.getResourceAccess());
								ts = new TimeSeriesDataExtendedImpl((TimeSeriesDataImpl) ts, label, label);
							}
						}
						tsdList.add(ts);
						//TimeSeriesDataImpl tsd = new TimeSeriesDataImpl(ts, label, label, null);
						//TimeSeriesDataExtendedImpl tsExt = new TimeSeriesDataExtendedImpl(tsd, null, null);
						//tsExt.addProperty("deviceName", di.getDeviceName());
						//tsExt.addProperty("deviceResourceLocation", di.getDeviceResourceLocation());
						//tsdList.add(tsExt);
						//index++;
					}
					return tsdList;
				}

				@Override
				protected String getEvaluationProviderId(OgemaHttpRequest req) {
					return bc.buttonText();
				}

				@Override
				protected IntervalConfiguration getITVConfiguration(OgemaHttpRequest req) {
					IntervalConfiguration itv = bc.getDefaultInterval();
					if(itv != null) return itv;
					itv = new IntervalConfiguration();
					itv.end = controller.appMan.getFrameworkTime();
					itv.start = AbsoluteTimeHelper.getIntervalStart(itv.end, AbsoluteTiming.DAY);
					itv.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(itv.start, -1, AbsoluteTiming.DAY);
					return itv;
				}
			};
			evalButtonRow.setContent(0, col, openScheduleViewer);
			col++;
		}*/
		page.append(evalButtonRow);
	}
	
	//Header
	private void getHeader() {
		Header header = new Header(page, "header", "Evaluation Provider Overview");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
	}

	public WidgetPage<?> getPage() {
		return page;
	}
}
