package extensionmodel.smarteff.defaultproposal;

import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import extensionmodel.smarteff.api.base.SmartEffPriceData;

public class PriceDataParamsPage extends EditPageGenericParams<SmartEffPriceData> {
	@Override
	public void setData(SmartEffPriceData sr) {
		setLabel(sr.electrictiyPriceBase(), EN, "Base price electricity connection in EUR/year",
				DE, "Grundpreis Strom in EUR/Jahr");
		setLabel(sr.electrictiyPricePerkWh(), EN, "Additional price electricity consumption in EUR/kWh",
				DE, "Zusätzlicher variabler Preis Strom in EUR/kWh");
		setLabel(sr.gasPriceBase(), EN, "Base price gas connection in EUR/year",
				DE, "Grundpreis Gas in EUR/Jahr");
		setLabel(sr.gasPricePerkWh(), EN, "Additional price gas consumption in EUR/kWh",
				DE, "Zusätzlicher variabler Preis Gas in EUR/kWh");
		setLabel(sr.oilPriceBase(), EN, "Base price oil supply/service in EUR/year",
				DE, "Grundpreis Heizöl Lieferung/Wartung in EUR/Jahr");
		setLabel(sr.oilPricePerkWh(), EN, "Additional price oil consumption in EUR/kWh",
				DE, "Zusätzlicher variabler Preis Heizöl in EUR/kWh");
		setLabel(sr.yearlyInterestRate(), EN, "Yearly interest rate (simple share, not percent)",
				DE, "Zinssatz pro Jahr (Wert Anteil, nicht in Prozent)");
		setLabel(sr.standardRoomNum(), EN, "Default number of rooms assumed for a building without room data",
				DE, "Standard Zahl von Räumen für Gebäude ohne Raumdaten");
	}
	@Override
	public Class<SmartEffPriceData> primaryEntryTypeClass() {
		return SmartEffPriceData.class;
	}
}
