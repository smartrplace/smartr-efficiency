package org.smartrplace.app.monbase.power;

import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.tools.resource.util.TimeUtils;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.app.monbase.config.EnergyEvalInterval;
import org.smartrplace.app.monbase.config.EnergyEvalIntervalMeterData;
import org.smartrplace.util.directresourcegui.DeleteButton;
import org.smartrplace.util.directresourcegui.GUIHelperExtension;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;
import org.smartrplace.util.directresourcegui.ResourceGUITablePage;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.resource.widget.textfield.ResourceTextField;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;

public class EnergyEvaluationIntervalTable extends ResourceGUITablePage<EnergyEvalInterval> {
	protected final MonitoringController controller;
	protected final EnergyEvaluationTable energyTablePage;
	
	public EnergyEvaluationIntervalTable(WidgetPage<?> page, MonitoringController controller,
			EnergyEvaluationTable energyTablePage) {
		super(page, controller.appMan, null, EnergyEvalInterval.class, false);
		this.controller = controller;
		this.energyTablePage = energyTablePage;
		triggerPageBuild();
	}

	@Override
	public void addWidgets(EnergyEvalInterval object, ResourceGUIHelper<EnergyEvalInterval> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		vh.stringEdit("Name", id, object.name(), row, null);
		vh.timeLabel("Start", id, object.start(), row, 0);
		vh.timeLabel("Ende", id, object.end(), row, 0);
		//vh.floatLabel("Energie final", id, object.energyConsumedInInterval(), row, "%.1f");
		Button but = vh.linkingButton("Öffnen", id, object, row, "Daten", "energyTable.html");
		if(req == null) {
			vh.registerHeaderEntry("Löschen");
			vh.registerHeaderEntry("copy");
			vh.registerHeaderEntry("Beschreibung");
			return;			
		}
		but.addDefaultStyle(ButtonData.BOOTSTRAP_GREEN);
		@SuppressWarnings("unchecked")
		ResourceList<EnergyEvalInterval> rlist = controller.appConfigData.getSubResource(
				"energyEvaluationInterval", ResourceList.class);
		DeleteButton<EnergyEvalInterval> del = new DeleteButton<EnergyEvalInterval>(
				rlist, object, mainTable, id, alert, row, vh, req);
		del.addDefaultStyle(ButtonData.BOOTSTRAP_RED);
		row.addCell("Löschen", del);
		but = GUIHelperExtension.addCopyButton(rlist, object, mainTable, id, alert, row, vh, req, controller.appMan);
		but.addDefaultStyle(ButtonData.BOOTSTRAP_RED);
		vh.stringEdit("Beschreibung", id, object.description(), row, null);
	}

	@Override
	public void addWidgetsAboveTable() {
		Button addIntvBut = new Button(page, "addIntvBut", "Neues Auswertungsinterval jetzt starten") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				addEnergyIntervalForNow(req);
			}
		};
		page.append(addIntvBut).linebreak();
	}

	protected void addEnergyIntervalForNow(OgemaHttpRequest req) {
		@SuppressWarnings("unchecked")
		ResourceList<EnergyEvalInterval> rlist = controller.appConfigData.getSubResource(
				"energyEvaluationInterval", ResourceList.class);
		EnergyEvalInterval powerInterval = rlist.add();
		long now = controller.appMan.getFrameworkTime();
		powerInterval.name().<StringResource>create().setValue("Seit "+TimeUtils.getDateAndTimeString(now)); //TimeUtils.getDateString(now));
		powerInterval.description().<StringResource>create().setValue("");
		powerInterval.start().<TimeResource>create().setValue(now);
		powerInterval.activate(true);
		Collection<EnergyEvaluationTableLine> conns = energyTablePage.getObjectsInTable(powerInterval, false, req);
		setEnergyMeterValues(powerInterval, conns, false);		
	}
	
	/** Currently we save meter count information NOT for sub phases*/
	public static void setEnergyMeterValues(EnergyEvalInterval powerInterval, Collection<EnergyEvaluationTableLine> conns,
			boolean writeZero) {
		powerInterval.meterData().create();
		for(EnergyEvaluationTableLine conn: conns) {
			if(conn.conn == null) continue;
			if(!conn.conn.hasEnergySensor()) continue;
			
			EnergyEvalIntervalMeterData md = powerInterval.meterData().add();
			md.conn().setAsReference(conn.conn.getMeterReadingResource());
			
			final float value;
			if(writeZero) {
				value = 0;
			} else {
				value = conn.conn.getEnergyValue();
			}
			md.startCounterValue().create();
			md.startCounterValue().setValue(value);
		}
		powerInterval.meterData().activate(true);
	}
	
	@Override
	public List<EnergyEvalInterval> getResourcesInTable(OgemaHttpRequest req) {
		@SuppressWarnings("unchecked")
		ResourceList<EnergyEvalInterval> rlist = controller.appConfigData.getSubResource(
				"energyEvaluationInterval", ResourceList.class);
		if(!rlist.isActive()) {
			rlist.create();
			rlist.setElementType(EnergyEvalInterval.class);
			EnergyEvalInterval powerInterval = rlist.addDecorator("power", EnergyEvalInterval.class);
			powerInterval.name().<StringResource>create().setValue("Aktuelle Leistung");
			powerInterval.description().<StringResource>create().setValue("Zeigt an Stelle eines Intervalls die aktuelle Leistung");
			powerInterval = rlist.addDecorator("counters", EnergyEvalInterval.class);
			powerInterval.name().<StringResource>create().setValue("Zählerstände");
			powerInterval.description().<StringResource>create().setValue("Aktuelle Zählerstände");
			powerInterval.start().<TimeResource>create().setValue(0);
			Collection<EnergyEvaluationTableLine> conns = energyTablePage.getObjectsInTable(powerInterval, false, req);
			setEnergyMeterValues(powerInterval, conns, true);
			powerInterval = rlist.addDecorator("initialTest", EnergyEvalInterval.class);
			powerInterval.name().<StringResource>create().setValue("Seit 18.09.2019 13:47");
			powerInterval.description().<StringResource>create().setValue("!!TODO:Reale Werte müssen noch manuell eingegeben werden!");
			powerInterval.start().<TimeResource>create().setValue(1568807220000l); //1568814420000l-2*3600000);
			//524 Gesamt: 18/09/2019 13:47:00,                11.38
			//529 Beleuchtung, Halle: 18/09/2019 13:47:00,     0.83
			//531 Schaltschrank: 18/09/2019 13:47:00,          1.38
			//530 Quarantäne: 18/09/2019 13:47:00,             0.61
			//Hauptzähler: 78854,3
			//HM5634:        344
			//HM4847:		 359
			//HM5638: regular resets (ca. 0)
			//HM4830:       4100
			//HM4885:		 321
			//HM5635:        605
			//HM563E: n/a
			//HM563B: n/a
			//HM561E: n/a
			//Wärme 1: 354 * 3600000
			//Wärme 2: 347 * 3600000
			rlist.activate(true);
			setEnergyMeterValues(powerInterval, conns, false);
			addEnergyIntervalForNow(req);
		} else if(rlist.getElementType() == null)
			rlist.setElementType(EnergyEvalInterval.class);
		return rlist.getAllElements();
	}
	
	@Override
	protected void addWidgetsBelowTable() {
		StaticTable priceTable = new StaticTable(3, 2);
		ResourceTextField<FloatResource> elpriceEdit = new ValueResourceTextField<FloatResource>(page,
				"elpriceEdit", energyTablePage.elPrice);
		ResourceTextField<FloatResource> gaspriceEdit = new ValueResourceTextField<FloatResource>(page,
				"gaspriceEdit", energyTablePage.gasPrice);
		ResourceTextField<FloatResource> gaseffEdit = new ValueResourceTextField<FloatResource>(page,
				"gaseffEdit", energyTablePage.gasEff);
		priceTable.setContent(0, 0, "Strompreis in EUR/kWh").setContent(0, 1, elpriceEdit);
		priceTable.setContent(0, 0, "Gaspreis in EUR/kWh").setContent(0, 1, gaspriceEdit);
		priceTable.setContent(0, 0, "Wirkungsgrad Heizung (%)").setContent(0, 1, gaseffEdit);
		page.append(priceTable);
	}
}
