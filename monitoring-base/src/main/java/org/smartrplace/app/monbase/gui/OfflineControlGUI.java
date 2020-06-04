package org.smartrplace.app.monbase.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.util.extended.eval.widget.MultiSelectByButtons;
import org.smartrplace.app.monbase.MonitoringController;

import com.iee.app.evaluationofflinecontrol.gui.GatewayConfigPage;
import com.iee.app.evaluationofflinecontrol.gui.OfflineEvaluationControl;
import com.iee.app.evaluationofflinecontrol.util.ExportBulkData;
import com.iee.app.evaluationofflinecontrol.util.ExportBulkData.ComplexOptionDescription;

import de.iwes.timeseries.eval.api.EvaluationProvider;
import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiResult;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
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
import de.iwes.widgets.html.form.button.WindowCloseButton;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.multiselect.TemplateMultiselect;
import de.iwes.widgets.multiselect.extended.MultiSelectExtended;
import de.iwes.widgets.template.DefaultDisplayTemplate;


public class OfflineControlGUI {
    //protected final Map<String, List<String>> complexOptions; // = new LinkedHashMap<>();

	private final MonitoringController controller;
	private final OfflineControlGUIConfig guiConfig;
	private final MultiSelectByButtons multiSelectRooms2;
	private final WidgetPage<?> page;
	private final Button openScheduleViewer;
	private final TemplateDropdown<String> selectConfig;
	private final TemplateDropdown<String> selectSingleValueIntervals;
	
	private final TemplateDropdown<String> selectDataType;
	
	public final long UPDATE_RATE = 5*1000;
	public EvaluationProvider selectEval;

	private GaRoTestStarter<GaRoMultiResult> lastEvalStarted = null;
	private final Button stopLastEvalButton;
	
	private final TemplateMultiselect<String> multiSelectGWs;
	private final MultiSelectExtended<String> gateWaySelection;
	
    /** Overwrite these methods for alternative plot options*/
	public static class DefaultGUIConfig implements OfflineControlGUIConfig {
		public DefaultGUIConfig(MonitoringController controller) {
			this.controller = controller;
		}

		protected final MonitoringController controller;
		
		@Override
		public List<String> getIntervalOptions() {
			List<String> configOptions = Arrays.asList(controller.getIntervalOptions());
	    	return configOptions;
	    }
	    
		@Override
	    public Collection<String> getPlotNames() {
			return controller.getComplexOptions().keySet();
	    }
		
		@Override
	    public String getDefaultPlotName() {
			return controller.getDefaultComplexOptionKey();
		}
	
		@Override
		public List<String> baseLabels(String plotName, OgemaLocale locale) {
			return controller.getComplexOptions().get(plotName);
		}
		
		@Override
		public List<TimeSeriesData> getTimeseries(final List<String> gwIds, List<String> roomIDs,
				List<String> baselabels, OgemaHttpRequest req) {
			final GaRoSingleEvalProvider eval = controller.getDefaultProvider();
			List<GaRoMultiEvalDataProvider<?>> dps = controller.getDataProvidersToUse();
			GaRoMultiEvalDataProvider<?> dp = dps.get(0);
			final List<TimeSeriesData> input;

			//We perform room filtering in cleanListByRooms, so we get data for all rooms here
			input = GaRoEvalHelper.getFittingTSforEval(dp, eval, gwIds, null);
			if((!roomIDs.contains(controller.getAllRoomLabel(req!=null?req.getLocale():null)))) {
				ScheduleViewerOpenButtonDataProviderImpl.cleanListByRooms(input, roomIDs, controller);
			}
			
			Set<ComplexOptionDescription> inputsToUse = new HashSet<>(); //ArrayList<>();
			for(String baselabel: baselabels) {
				List<ComplexOptionDescription> newInp = controller.getDatatypesBaseExtended().get(baselabel);
				try {
					inputsToUse.addAll(newInp);
				} catch(NullPointerException e) {
					e.printStackTrace();
				}
			}
			ExportBulkData.cleanList(input, inputsToUse);

			return input;
		}
	}
	public WidgetPage<?> getPage() {
		return page;
	}
	
	public OfflineControlGUI(final WidgetPage<?> page, final MonitoringController app,
			OfflineControlGUIConfig guiConfig) {
		
		this.page = page;
		this.controller = app;
		if(guiConfig == null)
			this.guiConfig = new DefaultGUIConfig(app);
		else
			this.guiConfig = guiConfig;
		//this.complexOptions = app.getComplexOptions();
		addHeader();
		
		List<String> configOptions = this.guiConfig.getIntervalOptions();
		List<String> singleValueOption = new ArrayList<>();
		
		singleValueOption.add("Days Value");
		singleValueOption.add("Weeks Value");
		singleValueOption.add("Months Value");
		//options that do not contribute to longer KPIs, just for manual check
		singleValueOption.add("Hours Value");
		singleValueOption.add("Minutes Value");
		singleValueOption.add("Current Hour Single From-To Minutes");

		WindowCloseButton closeTabButton = new WindowCloseButton(page, "closeTabButtonBuilding", "Fertig");
		closeTabButton.addDefaultStyle(ButtonData.BOOTSTRAP_RED);
		RedirectButton messageButton = new RedirectButton(page, "messageButton", "Alarme", "/de/iwes/ogema/apps/message/reader/index.html");
		messageButton.setDefaultOpenInNewTab(false);

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
		selectDataType.setTemplate(new DefaultDisplayTemplate<String>() {
			@Override
			public String getLabel(String object, OgemaLocale locale) {
				String labelorg = super.getLabel(object, locale);
				if(labelorg.contains("##")) {
					String[] parts = labelorg.split("##");
					return parts[0];
				} else
					return labelorg;
			}
		});
		selectDataType.setDefaultItems(this.guiConfig.getPlotNames());
		selectDataType.selectDefaultItem(this.guiConfig.getDefaultPlotName());
		
		//gateway multi-selection
		if(!Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway")) {
			multiSelectGWs = new TemplateMultiselect<String>(page, "gateWaySelectionMS") {
	
				private static final long serialVersionUID = 1L;
				
				@Override
				public void onGET(OgemaHttpRequest req) {
				}
			};
			//multiSelectGWs.setDefaultWidth("100%");
			multiSelectGWs.setDefaultWidth("200px");
			multiSelectGWs.selectDefaultItems(controller.getGwIDs(null));
			multiSelectGWs.setDefaultSelectedItems(controller.getGwIDsDefaultSelected(null));
			//multiSelectGWs.selectDefaultItems(controller.serviceAccess.gatewayParser.getGatewayIds());
			
			gateWaySelection = 
					new MultiSelectExtended<String>(page, "gateWaySelection", multiSelectGWs, true, "", true, false) {
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean addMultiSelectAutomatically() {
					return false;
				}
			};
		} else {
			multiSelectGWs = null;
			gateWaySelection = null;
		}
		
		//multiSelectRooms = new Checkbox2(page, "roomSelectionMS");
		List<String> items = new ArrayList<>();
		List<String> ids = new ArrayList<>(controller.getAllRooms(null));
		ids.add(controller.getAllRoomLabel(null));
		for(String id: ids) {
			items.add(id);
		}
		multiSelectRooms2 = new MultiSelectByButtons(items, "msroom", page,
				ButtonData.BOOTSTRAP_GREEN, ButtonData.BOOTSTRAP_LIGHTGREY);

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

		GaRoSingleEvalProvider sel = controller.getDefaultProvider();
		openScheduleViewer = OfflineEvaluationControl.getScheduleViewerOpenButton(page, "openScheduleViewer",
				multiSelectGWs, //multiSelectRooms,
				null, sel.id(), //selectProvider,
				app, selectConfig,
			new ScheduleViewerOpenButtonDataProviderImpl(controller, this.guiConfig) {
				
				@Override
				protected List<String> getRoomIDs(OgemaHttpRequest req) {
					final List<String> roomIDs = multiSelectRooms2.getSelectedItems(req);
					return roomIDs;
				}
				
				@Override
				protected List<String> getGatewayIds(OgemaHttpRequest req) {
					if(gateWaySelection != null)
						return (List<String>) gateWaySelection.multiSelect.getSelectedLabels(req);
					else
						return controller.getGwIDs(req);
				}
				
				//@Override
				//protected GaRoSingleEvalProvider getEvalProvider(OgemaHttpRequest req) {
				//	return controller.getDefaultProvider();
				//}
				
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
		
		int i = 0;
		StaticTable table1 = new StaticTable(8, 3);
		page.append(table1);
		table1.setContent(i, 0, "Intervall"	);
		table1.setContent(i, 1, selectConfig	);
		StaticTable buttonTable = controller.provideButtonTable(this, closeTabButton, messageButton);
		table1.setContent(i, 2, buttonTable);
		i++;
		table1.setContent(i, 0, "Datentypen"	);
		table1.setContent(i, 1, selectDataType	);
		table1.setContent(i, 2, "              ");
		if(gateWaySelection != null) {
			i++;
			table1.setContent(i, 0, "Gateways"	);
			table1.setContent(i, 1, gateWaySelection		);
			table1.setContent(i, 2, "		"				);
			i++;
			table1.setContent(i, 0, "");
			table1.setContent(i, 1, multiSelectGWs		);
			table1.setContent(i, 2, "		"				);
		}
		i++;
		table1.setContent(i, 0, controller.getRoomOptionLineTitle());
		table1.setContent(i, 1, multiSelectRooms2.getStaticTable()		);
		//table1.setContent(i, 1, roomSelection		);
		table1.setContent(i, 2, "		"				);
		i++;
		//table1.setContent(i, 0, buttonDownload 	);
		table1.setContent(i, 1, openScheduleViewer  );
		table1.setContent(i, 2, stopLastEvalButton	);
		table1.setContent(i, 2, ""	);
		
		if(multiSelectGWs != null)
			multiSelectGWs.triggerAction(openScheduleViewer, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}

	protected void initResultsRooms(OgemaHttpRequest req, boolean resultOnly) {
		GaRoSingleEvalProvider eval = controller.getDefaultProvider(); //selectProvider.getSelectedItem(req);
		if(eval != null ) {
			if(!resultOnly) {
				//Set<String> ids = controller.getGatewayIds();
				List<String> ids = new ArrayList<>(controller.getAllRooms(req.getLocale()));
				ids.add(controller.getAllRoomLabel(req.getLocale()));
			}
		}
	}
		
	protected void initResultsGws(OgemaHttpRequest req, boolean resultOnly) {
		GaRoSingleEvalProvider eval = controller.getDefaultProvider(); //selectProvider.getSelectedItem(req);
		if(eval != null ) {
			if(!resultOnly) {
				Set<String> ids = controller.getGatewayIds();
				multiSelectGWs.update(ids, req);
				Collection<String> toUse = GatewayConfigPage.getGwsToUse(controller);
				multiSelectGWs.selectItems(toUse, req);
			}
		}
	}

	private void addHeader() {
		Header header = new Header(page, "header", "Chart-Konfiguration");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
	}
}
