package org.sp.example.smarteff.eval.capability;

import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import extensionmodel.smarteff.electricity.example.ElectricityProfileEvalConfig;

public class ElectricityProfileParamsPage extends EditPageGenericParams<ElectricityProfileEvalConfig> {
	@Override
	public void setData(ElectricityProfileEvalConfig sr) {
		setLabel(sr.peakPrice(), EN, "Peak Price (8am - 8pm)",
				DE, "Peak-Preis (8:00 - 20:00)");
		setLabel(sr.offpeakPrice(), EN, "Virtual Price during Offpeak hours",
				DE, "Virtueller Preis während Offpeak-Stunden");
		setLabel(sr.addPower(), EN, "Base power to be added on entire load profile (W)",
				DE, "Basis-Leistung, die zu allen Werten des Profils hinzugefügt werden soll (W)");
	}
	@Override
	protected Class<ElectricityProfileEvalConfig> primaryEntryTypeClass() {
		return ElectricityProfileEvalConfig.class;
	}
}
