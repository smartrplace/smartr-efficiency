package org.smartrplace.app.monbase.power;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.UtilityType;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSimpleProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.app.monbase.power.ConsumptionEvalAdmin.SumType;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableLineI.EnergyEvalObjI;
import org.smartrplace.monbase.alarming.AlarmingManagement;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.calendar.datepicker.Datepicker;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.button.WindowCloseButton;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;

public abstract class ConsumptionEvalTableBase<C extends ConsumptionEvalTableLineI>
		extends ObjectGUITablePage<C, ElectricityConnection> implements ConsumptionEvalTableI<C> {

	private static final long POLL_RATE = 10000;
	private static final String COST_HEADER ="Kosten (EUR)";
	protected Datepicker startPicker;
	protected Datepicker endPicker;
	protected Button updateButton;
	protected final MonitoringController controller;
	protected boolean hasCostColumn() {
		return true;
	}
	/** This method is only relevant for the phase num configured. Overwrite to provide your own
	 * phase column labels
	 */
	public String[] getPhaseLabels() {
		return new String[] {"L1", "L2", "L3"};
	}
	
	/** Price calculcation, all values in the utility default/currency unit, e.g kWh/EUR*/
	public FloatResource elPrice = null;
	public FloatResource gasPrice = null;
	public FloatResource gasEff = null;
	public FloatResource waterprice = null;
	public FloatResource foodprice = null;
	
	//protected final WidgetGroup wg;
	
	protected long getDefaultStartTime(OgemaHttpRequest req) {
		Long preset = Long.getLong("org.smartrplace.app.monbase.power.defaultkpistarttime");
		if(preset != null)
			return preset;
		long now = appMan.getFrameworkTime();
		return now - 30*AlarmingManagement.DAY_MILLIS;
	}
	
	/** Override default implementation if required*/
	protected abstract String getHeaderText(OgemaHttpRequest req);
	/** Short label for the generation of plot titles*/
	protected abstract String getShortLabel();

	/** Override default implementation if required*/
	protected void configurePricingInformation() {
		elPrice = KPIResourceAccess.getDefaultPriceResource(UtilityType.ELECTRICITY, appMan);
		/*FloatResource elPriceLoc;
		elPriceLoc = ResourceHelper.getSubResource(controller.appMan.getResourceAccess().getResource("master"),
				"editableData/buildingData/E_0/electricityPrice", FloatResource.class);
		if(elPriceLoc == null) {
			EvalCollection evalCollection = EvalHelper.getEvalCollection(appMan);
			elPriceLoc = evalCollection.getSubResource("elPrice", FloatResource.class);
		}
		elPrice = elPriceLoc;*/
		elPrice.create();
		if(!elPrice.isActive())
			elPrice.activate(false);
		
		gasPrice = KPIResourceAccess.getDefaultPriceResource(UtilityType.HEAT_ENERGY, appMan);
		/*FloatResource gasPriceLoc;
		gasPriceLoc = ResourceHelper.getSubResource(controller.appMan.getResourceAccess().getResource("master"),
				"editableData/buildingData/E_0/gasPrice", FloatResource.class);
		if(gasPriceLoc == null) {
			EvalCollection evalCollection = EvalHelper.getEvalCollection(appMan);
			gasPriceLoc = evalCollection.getSubResource("gasPrice", FloatResource.class);
		}
		gasPrice = gasPriceLoc;
		gasPrice.create();*/
		gasEff = KPIResourceAccess.getDefaultEfficiencyResource(UtilityType.HEAT_ENERGY, appMan);
		/*FloatResource gasEffLoc;
		gasEffLoc = ResourceHelper.getSubResource(controller.appMan.getResourceAccess().getResource("master"),
				"editableData/buildingData/E_0/heatingEfficiency", FloatResource.class);
		if(gasEffLoc == null) {
			EvalCollection evalCollection = EvalHelper.getEvalCollection(appMan);
			gasEffLoc = evalCollection.getSubResource("gasEff", FloatResource.class);
		}
		gasEff = gasEffLoc;*/
		gasEff.create();
		
		waterprice = KPIResourceAccess.getDefaultPriceResource(UtilityType.WATER, appMan);
		/*FloatResource waterPriceLoc;
		waterPriceLoc = ResourceHelper.getSubResource(controller.appMan.getResourceAccess().getResource("master"),
				"editableData/buildingData/E_0/waterPrice", FloatResource.class);
		if(waterPriceLoc == null) {
			EvalCollection evalCollection = EvalHelper.getEvalCollection(appMan);
			waterPriceLoc = evalCollection.getSubResource("waterPrice", FloatResource.class);
		}
		waterprice = waterPriceLoc;*/
		waterprice.create();

		foodprice = KPIResourceAccess.getDefaultPriceResource(UtilityType.FOOD, appMan);
		/*FloatResource foodPriceLoc;
		foodPriceLoc = ResourceHelper.getSubResource(controller.appMan.getResourceAccess().getResource("master"),
				"editableData/buildingData/E_0/foodPrice", FloatResource.class);
		if(foodPriceLoc == null) {
			EvalCollection evalCollection = EvalHelper.getEvalCollection(appMan);
			foodPriceLoc = evalCollection.getSubResource("foodPrice", FloatResource.class);
		}
		foodprice = foodPriceLoc;*/
		foodprice.create();
	}		
	
	public ConsumptionEvalTableBase(WidgetPage<?> page, MonitoringController controller,
			C initObject) {
		super(page, controller.appMan, initObject, false);
		this.controller = controller;
		configurePricingInformation();
		
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
			for(int i=0; i<hasSubPhaseNum(); i++)
				vh.registerHeaderEntry(getPhaseLabels()[i]);
			/*if(hasSubPhaseNum() > 0)
				vh.registerHeaderEntry("L1");
			if(hasSubPhaseNum() > 1)
				vh.registerHeaderEntry("L2");
			if(hasSubPhaseNum() > 2)
				vh.registerHeaderEntry("L3");*/
			if(hasCostColumn())
				vh.registerHeaderEntry(COST_HEADER);
			return;
		}
		Label lab = vh.stringLabel("Messung", id, object.getLabel(), row);
		if(object.getLineType() == SumType.SUM_LINE)
			lab.addDefaultCssStyle("font-weight", "bold");
		
		if(hasCostColumn()) {
			Label costLabel  = addLabel(ResourceUtils.getValidResourceName(COST_HEADER), object, id, 0, row, req, true);
			if(object.getLineType() == SumType.SUM_LINE)
				costLabel.addCssStyle("font-weight", "bold", req);
		}
		
		lab = addLabel("Gesamt", object, id, 0, row, req, false);
		//Label lab = vh.floatLabel("Gesamt", id, object.getPhaseValue(0), row, "%.1f");
		if(object.getLineType() == SumType.SUM_LINE)
			lab.addDefaultStyle(LabelData.BOOTSTRAP_GREEN);
		for(int i=0; i<hasSubPhaseNum(); i++)
			addLabel(getPhaseLabels()[i], object, id, i+1, row, req, false);
		/*if(hasSubPhaseNum() > 0)
			addLabel("L1", object, id, 1, row, req, false);
		if(hasSubPhaseNum() > 1)
			addLabel("L2", object, id, 2, row, req, false);
		if(hasSubPhaseNum() > 2)
			addLabel("L3", object, id, 3, row, req, false);*/
		//vh.floatLabel("L1", id, object.getPhaseValue(1), row, "%.1f");
		//vh.floatLabel("L2", id, object.getPhaseValue(2), row, "%.1f");
		//vh.floatLabel("L3", id, object.getPhaseValue(3), row, "%.1f");
	}

	protected Label addLabel(String columnId, ConsumptionEvalTableLineI object, String id, int index, 
			Row row, OgemaHttpRequest req,
			boolean isCostLabel) {
		Label phaseLabel = new Label(mainTable, (isCostLabel?"costLabel":"phaseLabel")+index+"_"+id, req) {
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
					else {
						if(!isCostLabel)
							setText(String.format("%.1f", val), req);
						else {
							ColumnDataProvider cprov = object.getCostProvider();
							if(cprov == null)
								setText("--", req); //homematic counters are reset each time voltage is lost, so a special handling would have to be implemented
								//setText(String.format("%.1f", val*0.001f), req);
							else {
								setCostLabel = true;
								setText(cprov.getString(val, startTime, endTime), req);
							}
						}
					}
				}
				if((!setCostLabel) && (isCostLabel))
					setText("--", req);
					
			}
		};
		//phaseLabel.setDefaultPollingInterval(POLL_RATE);
		configureLabelForPolling(phaseLabel, object.lineShowsPower(), req);
		row.addCell(WidgetHelper.getValidWidgetId(columnId), phaseLabel);
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
		
		Header header = new Header(page, "header") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				setText(getHeaderText(req), req);
			}
		};
		page.append(header);
		
		startPicker = new Datepicker(page, "startPicker") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				boolean lineShowsPower = isPowerTable();
				if(lineShowsPower) {
					setWidgetVisibility(false, req);
					return;
				}
				long ts = getDefaultStartTime(req); //refRes.getValue();
				setDate(ts, req);
				setWidgetVisibility(true, req);
			}
		};
		endPicker = new Datepicker(page, "endPicker") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				boolean lineShowsPower = isPowerTable();
				if(lineShowsPower) {
					setPollingInterval(POLL_RATE, req);
					setWidgetVisibility(false, req);
					return;
				}
				long ts;
				ts = getDateLong(req);
				if(ts <= 0) {
					ts = appMan.getFrameworkTime();
				}
				setDate(ts, req);
				setPollingInterval(POLL_RATE, req);
				setWidgetVisibility(true, req);			}
		};
		updateButton = new Button(page, "updateButton", "Aktualisieren") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				boolean lineShowsPower = isPowerTable();
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
		
		WindowCloseButton closeTabButton = new WindowCloseButton(page, "closeTabButtonBuilding",
				System.getProperty("org.ogema.app.navigation.closetabbuttontext", "Fertig"));
		closeTabButton.addDefaultStyle(ButtonData.BOOTSTRAP_RED);
		
		RedirectButton messageButton = getMessageButton();
		
		StaticTable topTable = new StaticTable(1, 5);
		topTable.setContent(0, 0, startPicker);
		topTable.setContent(0, 1, endPicker);
		topTable.setContent(0, 2, updateButton);
		topTable.setContent(0, 3, closeTabButton);
		if(messageButton != null)
			topTable.setContent(0, 4, messageButton);
		page.append(topTable);
		
		//wg.addWidget(mainTable);
	}

	protected RedirectButton getMessageButton() {
		return null;
	}
	@Override
	public String getLineId(C object) {
		return object.getLinePosition()+"_"+super.getLineId(object);
	}
	
	/******
	 * Default line adder implementations
	 ********/

	protected ConsumptionEvalTableLineBase addRexoLineBase(ElectricityConnection conn, boolean lineShowsPower,
			List<ConsumptionEvalTableLineBase> result, List<ConsumptionEvalTableLineI> rexoLines,
			int lineIdx, OgemaLocale locale,
			Datapoint dp) {
		//Resource rexo = controller.appMan.getResourceAccess().getResource(MAIN_ELECTRICITY_METER_RES);
		//ElectricityConnection conn =controller.getElectrictiyMeterDevice(subPath); // ResourceHelper.getSubResource(rexo,
		//		subPath, ElectricityConnection.class);

		String label = (dp != null)?dp.label(null):controller.getRoomLabel(conn.getLocation(), locale);
		//EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(conn, label, lineShowsPower, rexoSum, SumType.STD, clearList, intv, lineIdx);
		EnergyEvalObjI connObj = new EnergyEvalElConnObj(conn);
		ConsumptionEvalTableLineBase retVal = new ConsumptionEvalTableLineBase(connObj, label, lineShowsPower, SumType.STD, null, lineIdx, dp);
		if(rexoLines != null)
			rexoLines.add(retVal);
		result.add(retVal);
		return retVal;
	}

	protected ConsumptionEvalTableLineBase addHeatLineBase(SensorDevice conn, boolean lineShowsPower,
			List<ConsumptionEvalTableLineBase> result, List<ConsumptionEvalTableLineI> heatLines,
			int lineIdx, OgemaLocale locale,
			Datapoint dp) {
		//Resource rexo = controller.appMan.getResourceAccess().getResource(MAIN_HEAT_METER_RES);
		//SensorDevice conn = controller.getHeatMeterDevice(subPath); //ResourceHelper.getSubResource(rexo,
		//		subPath, SensorDevice.class);

		String label = (dp != null)?dp.label(null):"Heat "+controller.getRoomLabel(conn.getLocation(), locale);
		//EnergyEvaluationTableLine retVal = new EnergyEvaluationTableLine(conn, label, lineShowsPower, rexoSum, SumType.STD, clearList, intv, lineIdx);
		EnergyEvalObjI connObj = new EnergyEvalHeatObj(conn);
		if(dp == null && controller.dpService != null) {
			dp = controller.dpService.getDataPointStandard(connObj.getMeterReadingResource().getLocation());
			//It is not possible to set the label directly, we have to check this in the future
			if(dp.getRoom() == null) {
				dp.setSubRoomLocation(null, null, label);
			}
		}
		ConsumptionEvalTableLineBase retVal = new ConsumptionEvalTableLineBase(connObj, label, lineShowsPower, SumType.STD, null, lineIdx, dp);
		if(heatLines != null)
			heatLines.add(retVal);
		result.add(retVal);
		return retVal;
	}

	/**
	 * 
	 * @param conn
	 * @param powerReading may be null
	 * @param lineShowsPower
	 * @param result
	 * @param linesForSum
	 * @param lineIdx
	 * @param locale
	 * @param label
	 * @return
	 */
	@Deprecated
	protected ConsumptionEvalTableLineBase addLineBase(FloatResource conn, FloatResource powerReading, boolean lineShowsPower,
			List<ConsumptionEvalTableLineBase> result, List<ConsumptionEvalTableLineI> linesForSum,
			int lineIdx, String label,
			Datapoint dp) {
		
		EnergyEvalObjI connObj = new EnergyEvalObjBase(conn, powerReading);
		ConsumptionEvalTableLineBase retVal = new ConsumptionEvalTableLineBase(connObj, label, lineShowsPower, SumType.STD, null, lineIdx, dp);
		if(linesForSum != null)
			linesForSum.add(retVal);
		result.add(retVal);
		return retVal;
	}
	protected ConsumptionEvalTableLineBase addLineBase(FloatResource conn, FloatResource powerReading, boolean lineShowsPower,
			List<ConsumptionEvalTableLineBase> result,
			int lineIdx, String label,
			Datapoint dp, ColumnDataProvider cprov, TimeseriesSimpleProcUtil tsUtil) {
		
		EnergyEvalObjI connObj = new EnergyEvalObjBase(conn, powerReading, dp, tsUtil);
		return addLineBase(connObj, powerReading, lineShowsPower, result, lineIdx, label, dp, cprov);
	}
	protected ConsumptionEvalTableLineBase addLineBase(EnergyEvalObjI connObj, FloatResource powerReading, boolean lineShowsPower,
			List<ConsumptionEvalTableLineBase> result,
			int lineIdx, String label,
			Datapoint dp, ColumnDataProvider cprov) {
		
		ConsumptionEvalTableLineBase retVal = new ConsumptionEvalTableLineBase(connObj, label, lineShowsPower,
				SumType.STD, null, lineIdx, dp) {
			@Override
			public ColumnDataProvider getCostProvider() {
				return cprov;
			}
			@Override
			public int hasSubPhaseNum() {
				return ConsumptionEvalTableBase.this.hasSubPhaseNum();
			}
		};
		result.add(retVal);
		return retVal;
	}
}
