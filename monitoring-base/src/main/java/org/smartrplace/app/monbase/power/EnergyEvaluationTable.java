package org.smartrplace.app.monbase.power;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.app.monbase.config.EnergyEvalInterval;
import org.smartrplace.app.monbase.power.EnergyEvaluationTableLine.EnergyEvalObj;
import org.smartrplace.app.monbase.power.EnergyEvaluationTableLine.EnergyEvalSessionDataProvider;
import org.smartrplace.app.monbase.power.EnergyEvaluationTableLine.SumType;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.calendar.datepicker.Datepicker;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
import de.iwes.widgets.html.form.button.WindowCloseButton;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;

public class EnergyEvaluationTable extends ObjectGUITablePage<EnergyEvaluationTableLine, ElectricityConnection> implements EnergyEvalSessionDataProvider {

	private static final long POLL_RATE = 10000;
	private static final String COST_HEADER ="Kosten (EUR)";
	TemplateInitSingleEmpty<EnergyEvalInterval> initResType;
	protected final Datepicker startPicker;
	protected final Datepicker endPicker;
	protected final Button updateButton;
	protected final MonitoringController controller;
	protected final FloatResource elPrice;
	protected final FloatResource gasPrice;
	protected final FloatResource gasEff;
	
	public EnergyEvaluationTable(WidgetPage<?> page, MonitoringController controller) {
		super(page, controller.appMan, new EnergyEvaluationTableLine((ElectricityConnection)null, "init", true, null, null, null, 0,
				null), false);
		this.controller = controller;
		elPrice = ResourceHelper.getSubResource(controller.appMan.getResourceAccess().getResource("master"),
				"editableData/buildingData/E_0/electricityPrice", FloatResource.class);
		elPrice.create();
		if(!elPrice.isActive())
			elPrice.activate(false);
		gasPrice = ResourceHelper.getSubResource(controller.appMan.getResourceAccess().getResource("master"),
				"editableData/buildingData/E_0/gasPrice", FloatResource.class);
		gasPrice.create();
		gasEff = ResourceHelper.getSubResource(controller.appMan.getResourceAccess().getResource("master"),
				"editableData/buildingData/E_0/heatingEfficiency", FloatResource.class);
		gasEff.create();
		
		startPicker = new Datepicker(page, "startPicker") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				EnergyEvalInterval intv = initResType.getSelectedItem(req);
				boolean lineShowsPower = !(intv.start().isActive());
				if(lineShowsPower) {
					setWidgetVisibility(false, req);
					return;
				}
				long ts = intv.start().getValue();
				setDate(ts, req);
				setWidgetVisibility(true, req);
			}
		};
		endPicker = new Datepicker(page, "endPicker") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				EnergyEvalInterval intv = initResType.getSelectedItem(req);
				boolean lineShowsPower = !(intv.start().isActive());
				if(lineShowsPower) {
					setPollingInterval(POLL_RATE, req);
					setWidgetVisibility(false, req);
					return;
				}
				long ts;
				if(intv.end().isActive()) {
					ts = intv.start().getValue();
					setDate(ts, req);
					setPollingInterval(-1, req);
				} else {
					ts = getDateLong(req);
					if(ts <= 0) {
						ts = appMan.getFrameworkTime();
					}
					setDate(ts, req);
					setPollingInterval(POLL_RATE, req);
				}
				setWidgetVisibility(true, req);
				//updateContent(req, ts);
			}
		};
		updateButton = new Button(page, "updateButton", "Aktualisieren") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				EnergyEvalInterval intv = initResType.getSelectedItem(req);
				boolean lineShowsPower = !(intv.start().isActive());
				if(lineShowsPower) {
					setWidgetVisibility(false, req);
					return;
				}
				setWidgetVisibility(true, req);
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				//long endTime = endPicker.getDateLong(req);
				//updateContent(req, endTime);
			}
		};
		updateButton.addWidget(startPicker);
		updateButton.addWidget(endPicker);
		triggerPageBuild();
	}

	@Override
	public void addWidgets(EnergyEvaluationTableLine object,
			ObjectResourceGUIHelper<EnergyEvaluationTableLine, ElectricityConnection> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		if(req == null) {
			vh.registerHeaderEntry("Messung");
			vh.registerHeaderEntry("Gesamt");
			vh.registerHeaderEntry("L1");
			vh.registerHeaderEntry("L2");
			vh.registerHeaderEntry("L3");
			vh.registerHeaderEntry(COST_HEADER);
			return;
		}
		Label lab = vh.stringLabel("Messung", id, object.getLabel(), row);
		if(object.type == SumType.SUM_LINE)
			lab.addDefaultCssStyle("font-weight", "bold");
		
		Label costLabel  = new Label(mainTable, "costLabel_"+id, "--", req);
		//costLabel.setDefaultPollingInterval(POLL_RATE);
		configureLabelForPolling(costLabel, req);
		if(object.type == SumType.SUM_LINE)
			costLabel.addCssStyle("font-weight", "bold", req);
		row.addCell(ResourceUtils.getValidResourceName(COST_HEADER), costLabel);

		lab = addLabel("Gesamt", object, id, 0, row, req, costLabel);
		//Label lab = vh.floatLabel("Gesamt", id, object.getPhaseValue(0), row, "%.1f");
		if(object.type == SumType.SUM_LINE)
			lab.addDefaultStyle(LabelData.BOOTSTRAP_GREEN);
		addLabel("L1", object, id, 1, row, req, null);
		addLabel("L2", object, id, 2, row, req, null);
		addLabel("L3", object, id, 3, row, req, null);
		//vh.floatLabel("L1", id, object.getPhaseValue(1), row, "%.1f");
		//vh.floatLabel("L2", id, object.getPhaseValue(2), row, "%.1f");
		//vh.floatLabel("L3", id, object.getPhaseValue(3), row, "%.1f");
	}

	protected Label addLabel(String columnId, EnergyEvaluationTableLine object, String id, int index, 
			Row row, OgemaHttpRequest req,
			Label costLabel) {
		Label phaseLabel = new Label(mainTable, "phaseLabel"+index+"_"+id, req) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				long startTime = startPicker.getDateLong(req);
				long endTime = endPicker.getDateLong(req);
				List<EnergyEvaluationTableLine> lines = mainTable.getItems(req);
				float val = object.getPhaseValue(index, startTime, endTime, appMan.getFrameworkTime(), lines);
				//float val = object.getPhaseValue(index, startTime, endTime);
				boolean setCostLabel = false;
				if(Float.isNaN(val))
					setText("--", req);
				else {
					if(object.lineShowsPower)
						setText(String.format("%.1f", val), req);
					else
						if(object.label.contains("Pumpe"))
							setText("--", req); //homematic counters are reset each time voltage is lost, so a special handling would have to be implemented
							//setText(String.format("%.1f", val*0.001f), req);
						else if(object.label.contains("Wärme")) {
							setText(String.format("%.1f", val), req);
							if((index == 0) && (costLabel != null) && (gasPrice != null) && (gasEff != null)) {
								costLabel.setText(String.format("%.2f", val*gasPrice.getValue()*gasEff.getValue()/100), req);
								setCostLabel = true;
							}
						} else {
							setText(String.format("%.1f", val), req);
							if((index == 0) && (costLabel != null) && (elPrice != null)) {
								costLabel.setText(String.format("%.2f", val*elPrice.getValue()), req);
								setCostLabel = true;
							}
						}
				}
				if((!setCostLabel) && (costLabel != null))
					costLabel.setText("--", req);
					
			}
		};
		//phaseLabel.setDefaultPollingInterval(POLL_RATE);
		configureLabelForPolling(phaseLabel, req);
		row.addCell(columnId, phaseLabel);
		return phaseLabel;
	}
	
	protected void configureLabelForPolling(Label label, OgemaHttpRequest req) {
		endPicker.triggerAction(label, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST, req);
		updateButton.registerDependentWidget(label, req);
	}
	
	@Override
	public ElectricityConnection getResource(EnergyEvaluationTableLine object, OgemaHttpRequest req) {
		return object.conn.conn;
	}

	@Override
	public void addWidgetsAboveTable() {
		initResType =
				new TemplateInitSingleEmpty<EnergyEvalInterval>(page, "initResType", false) {
			private static final long serialVersionUID = 1L;

			@Override
			protected EnergyEvalInterval getItemById(String configId) {
				Resource result = controller.appMan.getResourceAccess().getResource(configId);
				if(result != null && result instanceof EnergyEvalInterval) return (EnergyEvalInterval) result;
				return null;
				/*@SuppressWarnings("unchecked")
				ResourceList<EnergyEvaluationInterval> rlist = controller.appConfigData.getSubResource(
						"energyEvaluationInterval", ResourceList.class);
				return rlist.getSubResource(configId);*/
			}
			@Override
			public void init(OgemaHttpRequest req) {
				super.init(req);
			}
		};
		page.append(initResType);
		initResType.registerDependentWidget(mainTable);
		initResType.registerDependentWidget(startPicker);
		initResType.registerDependentWidget(endPicker);
		
		Header header = new Header(page, "header") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				EnergyEvalInterval intv = initResType.getSelectedItem(req);
				setText("Übersicht "+intv.name().getValue(), req);
			}
		};
		page.append(header);
		
		WindowCloseButton closeTabButton = new WindowCloseButton(page, "closeTabButtonBuilding", "Fertig");
		closeTabButton.addDefaultStyle(ButtonData.BOOTSTRAP_RED);
		RedirectButton messageButton = new RedirectButton(page, "messageButton", "Alarme", "/de/iwes/ogema/apps/message/reader/index.html");
		messageButton.setDefaultOpenInNewTab(false);
		
		StaticTable topTable = new StaticTable(1, 5);
		topTable.setContent(0, 0, startPicker);
		topTable.setContent(0, 1, endPicker);
		topTable.setContent(0, 2, updateButton);
		topTable.setContent(0, 3, closeTabButton);
		topTable.setContent(0, 4, messageButton);
		page.append(topTable);
	}

	/*protected void updateContent(OgemaHttpRequest req, long endTime) {
		List<EnergyEvaluationTableLine> lines = mainTable.getItems(req);
		long startTime = startPicker.getDateLong(req);
		for(EnergyEvaluationTableLine line: lines) {
			for(int index=0; index<=3; index++) {
				line.getPhaseValue(index, startTime, endTime, appMan.getFrameworkTime(), lines);
			}
		}
	}*/
	
	@Override
	public Collection<EnergyEvaluationTableLine> getObjectsInTable(OgemaHttpRequest req) {
		EnergyEvalInterval intv = initResType.getSelectedItem(req);
		boolean lineShowsPower = !(intv.start().isActive());
		return getObjectsInTable(intv, lineShowsPower, req);
	}
	public Collection<EnergyEvaluationTableLine> getObjectsInTable(EnergyEvalInterval intv, boolean lineShowsPower, OgemaHttpRequest req) {
		List<EnergyEvaluationTableLine> result = new ArrayList<>();
		int lineCounter = 0;
		
		List<EnergyEvaluationTableLine> rexoLines = new ArrayList<>();
		List<EnergyEvaluationTableLine> pumpLines = new ArrayList<>();
		List<EnergyEvaluationTableLine> heatLines = new ArrayList<>();
		//EnergyEvaluationTableLine rexoSum = new EnergyEvaluationTableLine(null, "Verteilung gesamt",
		//		lineShowsPower, null, SumType.SUM_LINE, null, intv, (lineCounter++));					
		//EnergyEvaluationTableLine pumpSum = new EnergyEvaluationTableLine(null, "Pumpen gesamt",
		//				lineShowsPower, null, SumType.SUM_LINE, null, intv, (lineCounter++));					
		EnergyEvaluationTableLine rexoSum = new EnergyEvaluationTableLine((ElectricityConnection)null, "Strom Summe Abgänge",
				lineShowsPower, SumType.SUM_LINE, rexoLines , intv, (lineCounter++), this);					
		EnergyEvaluationTableLine pumpSum = new EnergyEvaluationTableLine((ElectricityConnection)null, "Pumpen gesamt",
						lineShowsPower, SumType.SUM_LINE, pumpLines, intv, (lineCounter++), this);					
		EnergyEvaluationTableLine heatSum = new EnergyEvaluationTableLine((ElectricityConnection)null, "Wärme gesamt",
				lineShowsPower, SumType.SUM_LINE, heatLines, intv, (lineCounter++), this);					

		//List<EnergyEvaluationTableLine> clearList = Arrays.asList(new EnergyEvaluationTableLine[] {rexoSum, pumpSum});
		//In first line we have to add clearList
		addRexoLine("electricityConnectionBox_A/connection",
				lineShowsPower, result, rexoLines, intv, lineCounter++, req);
		addRexoLine("electricityConnectionBox_C/connection",
				lineShowsPower, result, rexoLines, intv, lineCounter++, req);
		addRexoLine("electricityConnectionBox_B/connection",
				lineShowsPower, result, rexoLines, intv, lineCounter++, req);
		addRexoLine("electricityConnectionBox_D/connection",
				lineShowsPower, result, null, intv, lineCounter++, req);
		if(!lineShowsPower) {
			addMainMeterLine("master", "editableData/buildingData/E_0/electricityMeterCountValue/recordedDataParent/program",
					result, intv, lineCounter++);			
		}
		
		//add heat meters
		addHeatLine("_10120253",
				lineShowsPower, result, heatLines, intv, lineCounter++, req);
		addHeatLine("_39009357",
				lineShowsPower, result, heatLines, intv, lineCounter++, req);
		addHeatLine("_39009358",
				lineShowsPower, result, heatLines, intv, lineCounter++, req);
		
		Resource hm =  controller.appMan.getResourceAccess().getResource("HomeMaticIP");
		List<ElectricityConnection> hmconns = hm.getSubResources(ElectricityConnection.class, true);
		Map<String, ElectricityConnection> conns = new HashMap<String, ElectricityConnection>();
		for(ElectricityConnection conn: hmconns) {
			if(!useConnection(conn)) continue;
			String label = controller.getRoomLabel(conn.getLocation(), req.getLocale())+"-Pumpe";
			conns.put(label, conn);
		}
		List<String> sorted = new ArrayList<>(conns.keySet());
		sorted.sort(null);
		for(String label: sorted) {
			ElectricityConnection conn = conns.get(label);
			EnergyEvaluationTableLine newLine = new EnergyEvaluationTableLine(
					conn, label, lineShowsPower, SumType.STD, null, intv, (lineCounter++), this);
			//result.add(new EnergyEvaluationTableLine(conn, label, lineShowsPower, pumpSum, SumType.STD, null, intv, (lineCounter++)));
			result.add(newLine);
			pumpLines.add(newLine);
		}
		result.add(rexoSum);					
		result.add(pumpSum);					
		result.add(heatSum);					
		/*addEvalLine("HomeMaticIP", "HomeMaticIP/devices/HM_HMIP_PSM_0001D3C99C4830/HM_SingleSwitchBox_0001D3C99C4830/electricityConnection",
				lineShowsPower, result);
		addEvalLine("HomeMaticIP", "HomeMaticIP/devices/HM_HMIP_PSM_0001D3C99C4847/HM_SingleSwitchBox_0001D3C99C4847/electricityConnection",
				lineShowsPower, result);*/
		return result;
	}

	protected static boolean useConnection(ElectricityConnection conn) {
		if(conn.isReference(false)) return false;
		if(conn.powerSensor().exists() || conn.energySensor().exists()) return true;
		return false;
	}
	
	protected EnergyEvaluationTableLine addRexoLine(String subPath, boolean lineShowsPower,
			List<EnergyEvaluationTableLine> result, List<EnergyEvaluationTableLine> rexoLines,
			EnergyEvalInterval intv, int lineIdx, OgemaHttpRequest req) {
		//Resource rexo = controller.appMan.getResourceAccess().getResource(MAIN_ELECTRICITY_METER_RES);
		ElectricityConnection conn =controller.getElectrictiyMeterDevice(subPath); // ResourceHelper.getSubResource(rexo,
		//		subPath, ElectricityConnection.class);

		String label = controller.getRoomLabel(conn.getLocation(), req.getLocale());
		//EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(conn, label, lineShowsPower, rexoSum, SumType.STD, clearList, intv, lineIdx);
		EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(conn, label, lineShowsPower, SumType.STD, null, intv, lineIdx, this);
		if(rexoLines != null)
			rexoLines.add(retVal);
		result.add(retVal);
		return retVal;
	}

	protected EnergyEvaluationTableLine addHeatLine(String subPath, boolean lineShowsPower,
			List<EnergyEvaluationTableLine> result, List<EnergyEvaluationTableLine> heatLines,
			EnergyEvalInterval intv, int lineIdx, OgemaHttpRequest req) {
		//Resource rexo = controller.appMan.getResourceAccess().getResource(MAIN_HEAT_METER_RES);
		SensorDevice conn = controller.getHeatMeterDevice(subPath); //ResourceHelper.getSubResource(rexo,
		//		subPath, SensorDevice.class);

		String label = "Wärme "+controller.getRoomLabel(conn.getLocation(), req.getLocale());
		//EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(conn, label, lineShowsPower, rexoSum, SumType.STD, clearList, intv, lineIdx);
		EnergyEvalObj connObj = new EnergyEvalHeatObj(conn);
		EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(connObj, label, lineShowsPower, SumType.STD, null, intv, lineIdx, this);
		if(heatLines != null)
			heatLines.add(retVal);
		result.add(retVal);
		return retVal;
	}
	
	protected EnergyEvaluationTableLine addMainMeterLine(String topResName, String subPath,
			List<EnergyEvaluationTableLine> result,
			EnergyEvalInterval intv, int lineIdx) {
		Resource rexo = controller.appMan.getResourceAccess().getResource(topResName);
		Schedule sched = ResourceHelper.getSubResource(rexo,
				subPath, Schedule.class);
		
		String label = "Ablesung Hauptzähler Strom";
		EnergyEvalObj connObj = new EnergyEvalObj(null) {
			@Override
			float getPowerValue() {
				return Float.NaN;
			}

			@Override
			float getEnergyValue() {
				SampledValue val = sched.getPreviousValue(Long.MAX_VALUE);
				if(val == null) return Float.NaN;
				return val.getValue().getFloatValue();
			}
			
			@Override
			float getEnergyValue(long startTime, long endTime, String label) {
				return getEnergyValue(sched, startTime, endTime, label);
			}
			
			@Override
			boolean hasSubPhases() {
				return false;
			}
			
			@Override
			boolean hasEnergySensor() {
				return true;
			}
			
			@Override
			float getPowerValueSubPhase(int index) {
				return Float.NaN;
			}

			@Override
			float getEnergyValueSubPhase(int index) {
				return Float.NaN;
			}
			
			@Override
			float getEnergyValueSubPhase(int index, long startTime, long endTime) {
				return Float.NaN;
			}
			
			@Override
			Resource getMeterReadingResource() {
				return sched;			
			}
			
		};
		//EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(conn, label, lineShowsPower, rexoSum, SumType.STD, clearList, intv, lineIdx);
		EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(connObj, label, false, SumType.STD, null, intv, lineIdx, this);
		result.add(retVal);
		return retVal;
	}

	@Override
	public String getLineId(EnergyEvaluationTableLine object) {
		return object.index+"_"+super.getLineId(object);
	}
}
