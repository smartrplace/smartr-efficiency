package org.smartrplace.commontypes;

import java.util.HashMap;
import java.util.Map;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericWithTable;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.HeatCostBillingInfo;

public class HeatBillRegistration {
	public static final Class<? extends SmartEffResource> TYPE_CLASS = HeatCostBillingInfo.class;
	
	public static final Map<String, String> UNITTYPEMAP_EN = new HashMap<>();
	public static final Map<String, String> UNITTYPEMAP_DE = new HashMap<>();
	
	public static final Map<String, String> HEATTYPEMAP_EN = new HashMap<>(BuildingEditPage.HEATTYPEMAP_EN);
	public static final Map<String, String> HEATTYPEMAP_DE = new HashMap<>(BuildingEditPage.HEATTYPEMAP_DE);

	static {
		UNITTYPEMAP_EN.put("1", "gas in m3");
		UNITTYPEMAP_EN.put("2", "oil in l");
		UNITTYPEMAP_EN.put("3", "solid fuel in kg");
		UNITTYPEMAP_EN.put("4", "kWh (input, for heat pump this would be electricity)");
		UNITTYPEMAP_EN.put("5", "kWh heat measured for heat pump");
		
		UNITTYPEMAP_DE.put("1", "Gas in m3");
		UNITTYPEMAP_DE.put("2", "Öl in l");
		UNITTYPEMAP_DE.put("3", "Festbrennstoff in kg");
		UNITTYPEMAP_DE.put("4", "kWh (System-Input, für Wärmepumpe/KWK: Strom)");
		UNITTYPEMAP_DE.put("5", "kWh wärmeseitig für Wärmepumpe/KWK");
		
		HEATTYPEMAP_EN.put("0", "Use Building Settings");
		HEATTYPEMAP_DE.put("0", "Wie Gebäude-Einstellung");
	}

	
	public static class TypeDeclaration implements ExtensionResourceTypeDeclaration<SmartEffResource> {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return TYPE_CLASS;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "Heat Cost Bill Data";
		}

		@Override
		public Class<? extends SmartEffResource> parentType() {
			return BuildingData.class;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.MULTIPLE_OPTIONAL;
		}
	}
	
	public static class EditPage extends EditPageGenericWithTable<HeatCostBillingInfo> {
		@Override
		public void setData(HeatCostBillingInfo sr) {
			setHeaderLabel(EN, "Heat Bill Data", DE, "Heizkostenabrechnung");
			//setHeaderLink(EN, "https://en.wikipedia.org/wiki/Master_data");
			setLabel(sr.unit(), EN, "Measurement unit",
					DE, "Einheit");
			setDisplayOptions(sr.unit(), EN, UNITTYPEMAP_EN);
			setDisplayOptions(sr.unit(), DE, UNITTYPEMAP_DE);
			setLabel(sr.heatSource(), EN, "Type of Heating", DE, "Art der Heizung");
			setDisplayOptions(sr.heatSource(), EN, HEATTYPEMAP_EN);
			setDisplayOptions(sr.heatSource(), DE, HEATTYPEMAP_DE);
			setLabel(sr.beginningOfBillingPeriodDay(), EN, "First day of billing period",
					DE, "Startdatum");
			setTableHeader(sr.beginningOfBillingPeriodDay(), EN, "Start");
			setLabel(sr.endOfBillingPeriodDay(), EN, "Last day of billing period",
					DE, "Enddatum");
			setTableHeader(sr.endOfBillingPeriodDay(), EN, "End", DE, "Ende");
			setLabel(sr.billedConsumption(), EN, "Energy consumption", DE, "Berechneter Verbrauch");
			setTableHeader(sr.billedConsumption(), EN, "Consumption", DE, "Verbrauch");
			setLabel(sr.cost(), EN, "Price of bill (EUR)", DE, "Rechnungsbetrag (EUR)");
			setTableHeader(sr.cost(), EN, "Price", DE, "Betrag");
		}
		
		@Override
		public boolean checkResource(HeatCostBillingInfo res) {
			ValueResourceHelper.setIfNew(res.unit(), 1);
			long endOfYear = AbsoluteTimeHelper.getIntervalStart(appManExt.getFrameworkTime(), AbsoluteTiming.YEAR) - 24*60*6000;
			long startOfYear = AbsoluteTimeHelper.getIntervalStart(endOfYear, AbsoluteTiming.YEAR);
			ValueResourceHelper.setIfNew(res.beginningOfBillingPeriodDay(), startOfYear);
			ValueResourceHelper.setIfNew(res.endOfBillingPeriodDay(), endOfYear);
			return super.checkResource(res);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Class<HeatCostBillingInfo> primaryEntryTypeClass() {
			return (Class<HeatCostBillingInfo>) TYPE_CLASS;
		}
		
		@Override
		protected String getMaintainer() {
			return "test1";
		}
		
		@Override
		protected String getHeader(OgemaHttpRequest req) {
			return getReqData(req).getParent().getParent().getName();
		}
		
		
	}
}
