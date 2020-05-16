package org.smartrplace.app.monbase.power;

import java.util.List;

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
import org.smartrplace.app.monbase.power.ConsumptionEvalAdmin.SumType;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableLineI.EnergyEvalObjI;
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

/** The initial version of the ConsumptionTable is based on the Resource Type
 * EnergyEvalInterval. This type does not necessarily implement implements ConsumptionEvalTableI<C>
 */
public abstract class ConsumptionEvalTableBaseWithEvalInterval<C extends ConsumptionEvalTableLineI>
		extends ObjectGUITablePage<C, ElectricityConnection> {

	private static final long POLL_RATE = 10000;
	private static final String COST_HEADER ="Kosten (EUR)";
	protected TemplateInitSingleEmpty<EnergyEvalInterval> initResType;
	protected final Datepicker startPicker;
	protected final Datepicker endPicker;
	protected final Button updateButton;
	protected final MonitoringController controller;
	
	/** Price calculcation, all values in the utility default/currency unit, e.g kWh/EUR*/
	public FloatResource elPrice = null;
	public FloatResource gasPrice = null;
	public FloatResource gasEff = null;
	public FloatResource waterprice = null;
	public FloatResource foodprice = null;
	
	//protected final WidgetGroup wg;
	
	protected abstract void configurePricingInformation();
	
	public ConsumptionEvalTableBaseWithEvalInterval(WidgetPage<?> page, MonitoringController controller,
			C initObject) {
		super(page, controller.appMan, initObject, false);
		this.controller = controller;
		
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
					//setPollingInterval(POLL_RATE, req);
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
		
		//updateButton.governingWidget = true;
		//Just do this to make sure that updateButton.governingWidget = true;
		//Note that if session-specific widgets have a dependency to a non-session-specific widget then
		//the non-session-specific parent widget needs to register a non-session-specific dependency once
		updateButton.registerDependentWidget(alert);
		
		//wg = page.registerWidgetGroup("pollingGroup");
		//wg.setPollingInterval(POLL_RATE);
		triggerPageBuild();
	}

	@Override
	public void addWidgets(C object,
			ObjectResourceGUIHelper<C, ElectricityConnection> vh, String id, OgemaHttpRequest req, Row row,
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
		if(object.getLineType() == SumType.SUM_LINE)
			lab.addDefaultCssStyle("font-weight", "bold");
		
		Label costLabel  = new Label(mainTable, "costLabel_"+id, "--", req);
		//costLabel.setDefaultPollingInterval(POLL_RATE);
		configureLabelForPolling(costLabel, object.lineShowsPower(), req);
		if(object.getLineType() == SumType.SUM_LINE)
			costLabel.addCssStyle("font-weight", "bold", req);
		row.addCell(ResourceUtils.getValidResourceName(COST_HEADER), costLabel);

		lab = addLabel("Gesamt", object, id, 0, row, req, costLabel);
		//Label lab = vh.floatLabel("Gesamt", id, object.getPhaseValue(0), row, "%.1f");
		if(object.getLineType() == SumType.SUM_LINE)
			lab.addDefaultStyle(LabelData.BOOTSTRAP_GREEN);
		addLabel("L1", object, id, 1, row, req, null);
		addLabel("L2", object, id, 2, row, req, null);
		addLabel("L3", object, id, 3, row, req, null);
		//vh.floatLabel("L1", id, object.getPhaseValue(1), row, "%.1f");
		//vh.floatLabel("L2", id, object.getPhaseValue(2), row, "%.1f");
		//vh.floatLabel("L3", id, object.getPhaseValue(3), row, "%.1f");
	}

	protected Label addLabel(String columnId, ConsumptionEvalTableLineI object, String id, int index, 
			Row row, OgemaHttpRequest req,
			Label costLabel) {
		Label phaseLabel = new Label(mainTable, "phaseLabel"+index+"_"+id, req) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				long startTime = startPicker.getDateLong(req);
				long endTime = endPicker.getDateLong(req);
				List<C> lines = mainTable.getItems(req);
				@SuppressWarnings("unchecked")
				float val = object.getPhaseValue(index, startTime, endTime, appMan.getFrameworkTime(), (List<ConsumptionEvalTableLineI>) lines);
				//float val = object.getPhaseValue(index, startTime, endTime);
				boolean setCostLabel = false;
				if(Float.isNaN(val))
					setText("--", req);
				else {
					if(object.lineShowsPower())
						setText(String.format("%.1f", val), req);
					else
						if(object.getLabel().contains("Pumpe"))
							setText("--", req); //homematic counters are reset each time voltage is lost, so a special handling would have to be implemented
							//setText(String.format("%.1f", val*0.001f), req);
						else if(object.getLabel().contains("Wärme")) {
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
		configureLabelForPolling(phaseLabel, object.lineShowsPower(), req);
		row.addCell(columnId, phaseLabel);
		return phaseLabel;
	}
	
	protected void configureLabelForPolling(Label label, boolean lineShowsPower, OgemaHttpRequest req) {
		if(lineShowsPower)
			endPicker.triggerAction(label, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST, req);
		else {
			//updateButton.addWidget(label);
			updateButton.registerDependentWidget(label, req);
		}
		//wg.addWidget(label);
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
		initResType.registerDependentWidget(updateButton);
		
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
		
		//wg.addWidget(mainTable);
	}

	@Override
	public String getLineId(C object) {
		return object.getLinePosition()+"_"+super.getLineId(object);
	}
	
	/******
	 * Default line adder implementations
	 ********/

	protected ConsumptionEvalTableLineBase addRexoLineBase(String subPath, boolean lineShowsPower,
			List<ConsumptionEvalTableLineBase> result, List<ConsumptionEvalTableLineI> rexoLines,
			int lineIdx, OgemaHttpRequest req) {
		//Resource rexo = controller.appMan.getResourceAccess().getResource(MAIN_ELECTRICITY_METER_RES);
		ElectricityConnection conn =controller.getElectrictiyMeterDevice(subPath); // ResourceHelper.getSubResource(rexo,
		//		subPath, ElectricityConnection.class);

		String label = controller.getRoomLabel(conn.getLocation(), req.getLocale());
		//EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(conn, label, lineShowsPower, rexoSum, SumType.STD, clearList, intv, lineIdx);
		EnergyEvalObjI connObj = new EnergyEvalElConnObj(conn);
		ConsumptionEvalTableLineBase retVal = new ConsumptionEvalTableLineBase(connObj, label, lineShowsPower, SumType.STD, null, lineIdx);
		if(rexoLines != null)
			rexoLines.add(retVal);
		result.add(retVal);
		return retVal;
	}

	protected ConsumptionEvalTableLineBase addHeatLineBase(String subPath, boolean lineShowsPower,
			List<ConsumptionEvalTableLineBase> result, List<ConsumptionEvalTableLineI> heatLines,
			int lineIdx, OgemaHttpRequest req) {
		//Resource rexo = controller.appMan.getResourceAccess().getResource(MAIN_HEAT_METER_RES);
		SensorDevice conn = controller.getHeatMeterDevice(subPath); //ResourceHelper.getSubResource(rexo,
		//		subPath, SensorDevice.class);

		String label = "Heat "+controller.getRoomLabel(conn.getLocation(), req.getLocale());
		//EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(conn, label, lineShowsPower, rexoSum, SumType.STD, clearList, intv, lineIdx);
		EnergyEvalObjI connObj = new EnergyEvalHeatObj(conn);
		ConsumptionEvalTableLineBase retVal = new ConsumptionEvalTableLineBase(connObj, label, lineShowsPower, SumType.STD, null, lineIdx);
		if(heatLines != null)
			heatLines.add(retVal);
		result.add(retVal);
		return retVal;
	}
	
	protected ConsumptionEvalTableLineBase addMainMeterLineBase(String topResName, String subPath,
			List<ConsumptionEvalTableLineBase> result, int lineIdx) {
		Resource rexo = controller.appMan.getResourceAccess().getResource(topResName);
		Schedule sched = ResourceHelper.getSubResource(rexo,
				subPath, Schedule.class);
		
		String label = "Main Electricity Meter Reading";
		EnergyEvalElConnObj connObj = new EnergyEvalElConnObj(null) {
			@Override
			public
			float getPowerValue() {
				return Float.NaN;
			}

			@Override
			public
			float getEnergyValue() {
				SampledValue val = sched.getPreviousValue(Long.MAX_VALUE);
				if(val == null) return Float.NaN;
				return val.getValue().getFloatValue();
			}
			
			@Override
			public
			float getEnergyValue(long startTime, long endTime, String label) {
				return getEnergyValue(sched, startTime, endTime, label);
			}
			
			@Override
			public
			int hasSubPhaseNum() {
				return 0;
			}
			
			@Override
			public
			boolean hasEnergySensor() {
				return true;
			}
			
			@Override
			public
			float getPowerValueSubPhase(int index) {
				return Float.NaN;
			}

			@Override
			public
			float getEnergyValueSubPhase(int index) {
				return Float.NaN;
			}
			
			@Override
			public
			float getEnergyValueSubPhase(int index, long startTime, long endTime) {
				return Float.NaN;
			}
			
			@Override
			public
			Resource getMeterReadingResource() {
				return sched;			
			}
			
		};
		//EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(conn, label, lineShowsPower, rexoSum, SumType.STD, clearList, intv, lineIdx);
		ConsumptionEvalTableLineBase retVal = new ConsumptionEvalTableLineBase(connObj, label, false, SumType.STD, null, lineIdx);
		result.add(retVal);
		return retVal;
	}
	
}
