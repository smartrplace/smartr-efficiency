package org.smartrplace.app.monbase.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.util.evalcontrol.EvalScheduler.OverwriteMode;
import org.ogema.util.extended.eval.widget.MultiSelectByButtons;
import org.ogema.util.jsonresult.management.JsonOGEMAFileManagementImpl;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;
import org.smartrplace.app.monbase.MonitoringController;

import com.iee.app.evaluationofflinecontrol.gui.GaRoMultiResultDeser;
import com.iee.app.evaluationofflinecontrol.gui.GaRoSuperEvalResultDeser;
import com.iee.app.evaluationofflinecontrol.gui.OfflineEvaluationControl;
import com.iee.app.evaluationofflinecontrol.gui.PreEvalSelectDropdown;

import de.iwes.timeseries.eval.api.EvaluationProvider;
import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.semaextension.variant.GatewayDataExportUtil;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoPreEvaluationProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoStdPreEvaluationProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoSuperEvalResult;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper.CSVArchiveExporter;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProviderPreEvalRequesting;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.GaRoTestStarter;
import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
import de.iwes.widgets.html.form.button.WindowCloseButton;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.resource.widget.textfield.BooleanResourceCheckbox;
import de.iwes.widgets.template.DefaultDisplayTemplate;


public class OfflineControlGUI {
    protected final Map<String, List<String>> complexOptions; // = new LinkedHashMap<>();

	private final EvalResultManagement evalResultMan;
    
	private final MonitoringController controller;
	//private final TemplateMultiselect<String> multiSelectRooms;
	//private final Checkbox2 multiSelectRooms;
	private final MultiSelectByButtons multiSelectRooms2;
	//private List<ResultType> resultMultiSelection = new ArrayList<ResultType>();
	//private final TemplateMultiselect<ResultType> resultsSelection;
	private final PreEvalSelectDropdown selectPreEval1;
	private final PreEvalSelectDropdown selectPreEval2;
	private final TemplateDropdown<OverwriteMode> overWriteDrop;
	private final WidgetPage<?> page;
	private final TemplateDropdown<GaRoSingleEvalProvider> selectProvider;
	private final Button openScheduleViewer;
	//private final Button addKPIPageButton;
	//private final TextField evalName;
	//private final MultiSelectExtended<String> roomSelection;
	private final TemplateDropdown<String> selectConfig;
	private final TemplateDropdown<String> selectSingleValueIntervals;
	
	private final TemplateDropdown<String> selectDataType;
	
	public final long UPDATE_RATE = 5*1000;
	public EvaluationProvider selectEval;

	private GaRoTestStarter<GaRoMultiResult> lastEvalStarted = null;
	private final Button stopLastEvalButton;
	
    public List<String> getConfigOptionsMinimal() {
		List<String> configOptions = Arrays.asList(controller.getIntervalOptions());
    	return configOptions;
    }
    public Collection<String> getDataTypes() {
		return complexOptions.keySet();
     }
	    
	public WidgetPage<?> getPage() {
		return page;
	}
	
	public OfflineControlGUI(final WidgetPage<?> page, final MonitoringController app) {
		
		this.page = page;
		this.controller = app; 
		this.complexOptions = app.getComplexOptions();
		this.evalResultMan = controller.serviceAccess.evalResultMan(); //EvalResultManagementStd.getInstance(app.appMan);
		addHeader();
		
		//List<String> jsonList1 = new ArrayList<>();
		//List<String> jsonList2 = new ArrayList<>();
		List<String> configOptions = getConfigOptionsMinimal();
		List<String> singleValueOption = new ArrayList<>();
		
		singleValueOption.add("Days Value");
		singleValueOption.add("Weeks Value");
		singleValueOption.add("Months Value");
		//options that do not contribute to longer KPIs, just for manual check
		singleValueOption.add("Hours Value");
		singleValueOption.add("Minutes Value");
		singleValueOption.add("Current Hour Single From-To Minutes");
		
		//init GaRo
		final TemplateInitSingleEmpty<GaRoSingleEvalProvider> init = new TemplateInitSingleEmpty<GaRoSingleEvalProvider>(page, "init", false) {
			private static final long serialVersionUID = 1L;

			@Override
			protected GaRoSingleEvalProvider getItemById(String configId) {
				for(GaRoSingleEvalProvider p: getProviders()) {
					if(p.id().equals(configId)) return p;
				}
				if(getProviders().isEmpty())
					return null;
				else {
					return controller.getDefaultProvider();
				}
			}

			@Override
			public void init(OgemaHttpRequest req) {
				super.init(req);
				//switchToEvalConfig(req);
				//List<String> toUse = GatewayConfigPage.getGwsToUse(app);
				//multiSelectGWs.selectItems(toUse, req);
				
				selectProvider.update(getProviders(), req);
				GaRoSingleEvalProvider sel = getSelectedItem(req);
				if(sel == null) sel = controller.getDefaultProvider();
				selectProvider.selectItem(sel, req);
				
				initResultsRooms(req, false);
			}
		};
		
		page.append(init);
		
		//number of available json files
		final Label selectPreEval1Count = new Label(page, "selectPreEvalCount1");
		final Label selectPreEval2Count = new Label(page, "selectPreEvalCount2");

		WindowCloseButton closeTabButton = new WindowCloseButton(page, "closeTabButtonBuilding", "Fertig");
		closeTabButton.addDefaultStyle(ButtonData.BOOTSTRAP_RED);
		RedirectButton messageButton = new RedirectButton(page, "messageButton", "Alarme", "/de/iwes/ogema/apps/message/reader/index.html");
		messageButton.setDefaultOpenInNewTab(false);
		//RedirectButton groupMgmtButton = new RedirectButton(page, "groupMgmtButton", "Gruppen", GroupModifyPage.PATH);
		//groupMgmtButton.setDefaultOpenInNewTab(false);
		
		//provider drop-down
		selectProvider 
			= new TemplateDropdown<GaRoSingleEvalProvider>(page, "selectProvider") {

				private static final long serialVersionUID = 1L;

			@Override
			public void updateDependentWidgets(OgemaHttpRequest req) {
				//EvaluationProvider eval =  getSelectedItem(req);
				//When the provider changes we have to adapt the results, but we do not change the gateway selection
				initResultsRooms(req, true);
				//if(eval != null ) {
				//	resultMultiSelection = eval.resultTypes();
				//	resultsSelection.update(resultMultiSelection, req);
				//	resultsSelection.selectItems(resultMultiSelection, req);
				//}
				boolean autoPreEval = controller.appConfigData.autoPreEval().getValue();
				selectPreEval1.onGETRemote(autoPreEval, req);
				selectPreEval2.onGETRemote(autoPreEval, req);
				//evalName.setValue(getSelectedLabel(req)+"Result.json", req);
			}
			
		};
		
		selectProvider.setTemplate(new DefaultDisplayTemplate<GaRoSingleEvalProvider>() {
			@Override
			public String getLabel(GaRoSingleEvalProvider object, OgemaLocale locale) {
				return object.getClass().getSimpleName();
			}
		});
		selectProvider.setDefaultVisibility(false);

		selectConfig = 
				new TemplateDropdown<String>(page, "selectConfig") {

			private static final long serialVersionUID = 1L;
			
			public void onGET(OgemaHttpRequest req) {
				enable(req);
			}
			
		};
		selectConfig.setDefaultItems(configOptions);
		selectConfig.selectDefaultItem("Eine Woche");
		
		selectDataType =  new TemplateDropdown<String>(page, "selectDataType");
		selectDataType.setDefaultItems(getDataTypes());
		selectDataType.selectDefaultItem("Wasser- und Luftemperatur");
		
		//gateway multi-selection
		//multiSelectRooms = new Checkbox2(page, "roomSelectionMS");
		List<String> items = new ArrayList<>();
		List<String> ids = new ArrayList<>(controller.getAllRooms(null));
		//ids.add("Luft");
		//ids.add("Strom");
		ids.add(controller.getAllRoomLabel(null));
		for(String id: ids) {
			items.add(id);
		}
		multiSelectRooms2 = new MultiSelectByButtons(items, "msroom", page,
				ButtonData.BOOTSTRAP_GREEN, ButtonData.BOOTSTRAP_LIGHTGREY);
		/*multiSelectRooms = new TemplateMultiselect<String>(page, "roomSelectionMS") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
			}
		};*/
		//multiSelectRooms.setDefaultWidth("100%");
		//multiSelectRooms.setDefaultCheckboxList(entries);
		//multiSelectRooms.selectDefaultItems(controller.getGatewayIds());
		//multiSelectGWs.selectDefaultItems(controller.serviceAccess.gatewayParser.getGatewayIds());
		
		//roomSelection = 
		//		new MultiSelectExtended<String>(page, "roomSelection", multiSelectRooms, true, "", true, false);

		//single value intervals drop-down
		selectSingleValueIntervals = 
				new TemplateDropdown<String>(page, "singleValueIntervals") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				disable(req);
			}
		};
		selectSingleValueIntervals.setDefaultItems(singleValueOption);
		selectSingleValueIntervals.selectDefaultItem("ThreeFullDaysBeforeNow");
		
		//set a new json file name
		//evalName = new TextField(page, "evalName");

		//first preEvaluation drop-down
		selectPreEval1 = new PreEvalSelectDropdown(page, "selectPreEval1",
				selectProvider, selectPreEval1Count, 0);
		
		//second preEvaluation drop-down
		selectPreEval2 = new PreEvalSelectDropdown(page, "selectPreEval2",
				selectProvider, selectPreEval2Count, 1);
		
		overWriteDrop = new TemplateDropdown<OverwriteMode>(page, "overwriteDrop") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(controller.appConfigData.autoPreEval().getValue()) {
					setWidgetVisibility(true, req);
					selectPreEval1Count.setText("Shall existing results for the requested time span be re-calculated and overwritten?", req);
				} else
					setWidgetVisibility(false, req);
				disable(req);
			}
	};
		overWriteDrop.setDefaultItems(Arrays.asList(new OverwriteMode[] {OverwriteMode.NO_OVERWRITE,
				OverwriteMode.ONLY_PROVIDER_REQUESTED, OverwriteMode.ALL_INCLUDING_PRE_EVALUATIONS
		}));
		overWriteDrop.selectDefaultItem(OverwriteMode.ONLY_PROVIDER_REQUESTED);
		

		openScheduleViewer = OfflineEvaluationControl.getScheduleViewerOpenButton(page, "openScheduleViewer",
				null, //multiSelectRooms,
				selectProvider, app, selectConfig,
			new ScheduleViewerOpenButtonDataProviderImpl(controller) {
				
				@Override
				protected List<String> getRoomIDs(OgemaHttpRequest req) {
					final List<String> roomIDs = multiSelectRooms2.getSelectedItems(req);
					/*List<CheckboxEntry> entries = multiSelectRooms.getCheckboxList(req);
					roomIDs = new ArrayList<>();
					for(CheckboxEntry e: entries) {
						if(e.isChecked())
							roomIDs.add(e.label(null));
					}*/
					return roomIDs;
				}
				
				@Override
				protected GaRoSingleEvalProvider getEvalProvider(OgemaHttpRequest req) {
					return selectProvider.getSelectedItem(req);
				}
				
				@Override
				protected String getDataType(OgemaHttpRequest req) {
					return selectDataType.getSelectedItem(req);
				}
			});
		openScheduleViewer.addDefaultStyle(ButtonData.BOOTSTRAP_GREEN);

		stopLastEvalButton = new Button(page, "stopLastEvalButton", "Stop Last Eval") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(lastEvalStarted != null) enable(req);
				else disable(req);
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				System.out.println("Last Eval Started: "+(lastEvalStarted != null));
				if(lastEvalStarted != null)
					System.out.println("Last Eval Started: "+(lastEvalStarted != null)+" eval:"+(lastEvalStarted.eval!=null));
				if(lastEvalStarted.eval == null)
					System.out.println("Last Eval Started: "+(lastEvalStarted != null)+" getEval():"+(lastEvalStarted.getEval()!=null));
				lastEvalStarted.getEval().stopExecution();
			}
		};
		
		BooleanResourceCheckbox autoEvalActiveCheck = new BooleanResourceCheckbox(page, "autoEvalActive", "Auto-Eval active") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				final GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
				MultiKPIEvalConfiguration config = controller.getAutoEvalConfig(eval.id());
				if(config == null) {
				} else {
					selectItem(config.performAutoQueuing(), req);
				}
				disable(req);
			}
		};
		
		/*FileDownload download = new FileDownload(page, "download", app.appMan.getWebAccessManager(), true);
		download.triggerAction(download, TriggeringAction.GET_REQUEST, FileDownloadData.STARTDOWNLOAD);
		page.append(download);
		Button buttonDownload = new Button(page, "buttonDownloadProgram", "Download CSV Project 1") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				download.setDeleteFileAfterDownload(false, req);
				download.setFile(new File("../evaluationresults/csvBulk4997226878533545817.zip"), "temperature_humidity_project1.zip", req);
			}
		};
		buttonDownload.triggerAction(download, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);  // GET then triggers download start
		*/
		int i = 0;
		StaticTable table1 = new StaticTable(8, 3);
		page.append(table1);
		//table1.setContent(i, 0, "Name/ID"		);
		//table1.setContent(i, 1, selectProvider	);
		//table1.setContent(i, 2, " 		"		);
		//i++;
		table1.setContent(i, 0, "Interval"	);
		table1.setContent(i, 1, selectConfig	);
		StaticTable buttonTable = controller.provideButtonTable(this, closeTabButton, messageButton);
		table1.setContent(i, 2, buttonTable);
		i++;
		table1.setContent(i, 0, "Datentypen"	);
		table1.setContent(i, 1, selectDataType	);
		table1.setContent(i, 2, "              ");
		i++;
		/*table1.setContent(i, 0, "Result Selection"		);
		table1.setContent(i, 1, "  ALL   " ); //resultSelectionExtended	);
		table1.setContent(i, 2, autoEvalActiveCheck				);
		i++;*/
		table1.setContent(i, 0, "Auswahl Becken"	);
		table1.setContent(i, 1, multiSelectRooms2.getStaticTable()		);
		//table1.setContent(i, 1, roomSelection		);
		table1.setContent(i, 2, "		"				);
		i++;
		//table1.setContent(i, 0, "Single Value Intervals"	);
		//table1.setContent(i, 1, selectSingleValueIntervals	);
		//table1.setContent(i, 2, overWriteDrop				);
		//i++;
		table1.setContent(i, 0, selectPreEval2Count );
		table1.setContent(i, 1, selectPreEval2  	);
		table1.setContent(i, 2, ""					);
		i++;
		//table1.setContent(i, 0, buttonDownload 	);
		table1.setContent(i, 1, openScheduleViewer  );
		table1.setContent(i, 2, stopLastEvalButton	);
		table1.setContent(i, 2, ""	);
		
		//selectProvider.triggerAction(evalName, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		//selectProvider.triggerAction(resultsSelection, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectProvider.triggerAction(openScheduleViewer, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectProvider.triggerAction(selectPreEval1Count, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectProvider.triggerAction(selectPreEval1, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectProvider.triggerAction(selectPreEval2Count, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectProvider.triggerAction(selectPreEval2, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		//selectProvider.triggerAction(evalName, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectProvider.triggerAction(autoEvalActiveCheck, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		
		//resultsSelection.triggerAction(startOfflineEval, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		//TODO multiSelectRooms
		//multiSelectRooms2.triggerAction(openScheduleViewer, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		//resultSelectionExtended.selectAllButton.triggerAction(startOfflineEval, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		//resultSelectionExtended.deselectAllButton.triggerAction(startOfflineEval, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}

	protected void initResultsRooms(OgemaHttpRequest req, boolean resultOnly) {
		GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
		if(eval != null ) {
			if(!resultOnly) {
				//Set<String> ids = controller.getGatewayIds();
				List<String> ids = new ArrayList<>(controller.getAllRooms(req.getLocale()));
				//ids.add("Luft");
				//ids.add("Strom");
				ids.add(controller.getAllRoomLabel(req.getLocale()));
				//multiSelectRooms.update(ids, req);
				/*List<CheckboxEntry> entries = new ArrayList<>();
				for(String id: ids) {
					entries.add(new CheckboxEntry(ResourceUtils.getValidResourceName(id)) {
						
						@Override
						public String label(OgemaLocale locale) {
							return id;
						}
					});
				}
				multiSelectRooms.setCheckboxList(entries, req);
				Collection<String> toUse = ids;
				//multiSelectRooms.selectItems(toUse, req);
				multiSelectRooms.deselectAll(req);*/
			}
		}
	}
		
	/** This method could be offered as an OSGi service in the future
	 * For this task the method should be moved to the controller.
	 * Compared to {@link #startEvaluation(GaRoSingleEvalProvider, List, ChronoUnit, long, long, List, CSVArchiveExporter, String, OverwriteMode, Path)}
	 * this method mainly supports multiFileSuffix.*/
	public void startEvalutionLikeStartPage(String jsonFileName,
			GaRoSingleEvalProvider eval,
			IntervalConfiguration itv,
			ChronoUnit chronoUnit, List<ResultType> resultsRequested,
			List<String> gwIDs, OverwriteMode om, Path preEvalFile) {
		String newPath = "";
		int j = 1;
		String selectedEvalProvider = eval.getClass().getSimpleName();
		if (jsonFileName == null || jsonFileName.trim().isEmpty())
			jsonFileName = selectedEvalProvider+"Result";
		if(jsonFileName.endsWith(".json")) jsonFileName = jsonFileName.substring(0, jsonFileName.length()-5);
		String FILE_PATH = evalResultMan.getFilePath(null, 10, "");
		Path providerPath = Paths.get(FILE_PATH+"/"+jsonFileName+".json");
		if(!Files.isRegularFile(providerPath)) {
			newPath = jsonFileName;
		} else {
			while(j < JsonOGEMAFileManagementImpl.MAX_RESULT_WITH_SAME_BASE_NAME) {
				if(itv.multiFileSuffix != null) {
					boolean ok = true;
					for(String s: itv.multiFileSuffix) {
						providerPath = Paths.get(FILE_PATH+"/"+getFileNameByIndex(jsonFileName, j)+s+".json");
						if(Files.isRegularFile(providerPath)) {
							ok = false;
							break;
						}
					}
					if(ok) {
						newPath = getFileNameByIndex(jsonFileName, j);
						break;
					}
				} else {
					providerPath = Paths.get(FILE_PATH+"/"+getFileNameByIndex(jsonFileName, j)+".json");
					if(!Files.isRegularFile(providerPath)) {
						newPath = getFileNameByIndex(jsonFileName, j);
						break;
					}
				}
				j++;
			}
		}
		
		CSVArchiveExporter csvWriter = null;
		if(controller.appConfigData.writeCsv().getValue()) {
			csvWriter = new GatewayDataExportUtil.CSVArchiveExporterGDE(controller.gatewayDataExport);
		}
		
		if(itv.multiStart == null) {
			startEvaluation(eval, resultsRequested, chronoUnit, itv.start, itv.end, gwIDs, csvWriter,
					newPath+".json", om, preEvalFile);
		} else for(int i=0; i<itv.multiStart.length; i++) {
			itv.start = itv.multiStart[i];
			itv.end = itv.multiEnd[i];
			startEvaluation(eval, resultsRequested, chronoUnit, itv.start, itv.end, gwIDs, csvWriter,
					newPath+itv.multiFileSuffix[i]+".json", om, preEvalFile);
		}
		
	}
	
	private String getFileNameByIndex(String jsonFileName, int j) {
		String newPath = j < 10?jsonFileName+"_0"+j:jsonFileName+"_"+j;
		return newPath;
	}
	
	/** startEvaluation: Supports different starting modes. The standard start mode is now
	 * using the queue, but for generation of CSV imports still using
	 * GaRoEvalHelperJAXB.performGenericMultiEvalOverAllData is required. Also manual choice of
	 * pre-evaluation is still supported.<br>
	 * If standard mode is used then {@link MonitoringController#startEvaluationViaQueue(GaRoSingleEvalProvider, List, ChronoUnit, long, long, List, OverwriteMode, String)}
	 * is almost equivalent to this method.
	 * 
	 * @param eval
	 * @param resultsRequested
	 * @param chronoUnit
	 * @param start
	 * @param end
	 * @param gwIDs
	 * @param csvWriter
	 * @param newPath
	 * @param om
	 * @param preEvalFile may be null if manual selected pre-evaluation is not required
	 */
	private void startEvaluation(GaRoSingleEvalProvider eval,
			List<ResultType> resultsRequested,
			ChronoUnit chronoUnit, long start, long end, List<String> gwIDs,
			CSVArchiveExporter csvWriter, String newPath,
			OverwriteMode om, Path preEvalFile) {

		Class<? extends GaRoSuperEvalResult<?>> typeToUse = null;
		if(eval.getSuperResultClassForDeserialization() != null)
			typeToUse = eval.getSuperResultClassForDeserialization();
		
		boolean usedPreEval = false;
		if(eval instanceof GaRoSingleEvalProviderPreEvalRequesting && (!controller.appConfigData.autoPreEval().getValue())) {
			GaRoSingleEvalProviderPreEvalRequesting peval = (GaRoSingleEvalProviderPreEvalRequesting)eval;
			if(peval.preEvaluationsRequested() != null) switch(peval.preEvaluationsRequested().size()) {
			case 0:
				break;
			case 1:
				/**TODO: Use SuperEval-class provided by the respective provider via
				 * {@link GaRoMultiResultExtended#getSuperResultClassForDeserialization()}
				 */
				GaRoPreEvaluationProvider preProvider = 
						new GaRoStdPreEvaluationProvider<GaRoMultiResultDeser, GaRoSuperEvalResult<GaRoMultiResultDeser>>
				(GaRoSuperEvalResultDeser.class, preEvalFile.toString());
			
				//CHECK
				lastEvalStarted = evalResultMan.performGenericMultiEvalOverAllData(eval.getClass(),
						start, end, // TODO: SK
						chronoUnit,
						new GaRoPreEvaluationProvider[] {preProvider}, resultsRequested, gwIDs, newPath,
						eval.id(), typeToUse, true, controller.getDataProvidersToUse());
				usedPreEval = true;
				break;
			case 2:
				//TODO
				throw new UnsupportedOperationException("Two pre evaluations not implemented yet!");
			case 3:
				//TODO
				break;
			default:
				throw new IllegalStateException("maximum 3 PreEvaluation supported!");
			}
		}
		
		if(!usedPreEval) {
			if(controller.appConfigData.autoPreEval().getValue()) {
				controller.startEvaluationViaQueue(eval, resultsRequested, chronoUnit, start, end,
						gwIDs, om, newPath);
			} else //if(!controller.appConfigData.writeCsv().getValue())
				lastEvalStarted = evalResultMan.performGenericMultiEvalOverAllData(eval.getClass(),
					start, end,
					chronoUnit,
					null, resultsRequested, gwIDs, newPath,
					eval.id(), typeToUse, true,
					controller.getDataProvidersToUse());
			/*else {
				//without queue. This is just supported for csvWriting
				lastEvalStarted = GaRoEvalHelperJAXB_V2.performGenericMultiEvalOverAllData(eval.getClass(),
						((OfflineEvalServiceAccess)controller.serviceAccess).gatewayParser(),
					start, end,
					chronoUnit,
					csvWriter, true, null, resultsRequested, gwIDs, newPath, null);
			}*/
		} 
	}

	private void addHeader() {
		Header header = new Header(page, "header", "Chart-Konfiguration");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
	}

	public static EvaluationProvider getEvalationProviderById(String providerID, Collection<EvaluationProvider> providers) {
		for(EvaluationProvider eval : providers) {
			if(eval != null && providerID.equals(eval.id())) {
				return eval;
			}
		}
		return null;
	}

	private List<GaRoSingleEvalProvider> getProviders() {
		//return controller.serviceAccess.getEvaluations().values();
		return 	controller.serviceAccess.getEvaluations().values().stream()
				.filter(provider -> provider instanceof GaRoSingleEvalProvider)
				.map(provider -> (GaRoSingleEvalProvider) provider)
				.collect(Collectors.toList());

	}
}
