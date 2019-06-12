package org.sp.calculator.hpadapt;

import java.util.HashMap;
import java.util.Map;

import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.hpadapt.HPAdaptData;
import extensionmodel.smarteff.smartrheating.SmartrHeatingData;

public class HPAdaptEditPage extends EditPageGeneric<HPAdaptData> {
	@Override
	public String label(OgemaLocale locale) {
		return "Bivalent heat pumpt calculator edit page";
	}

	@Override
	protected Class<HPAdaptData> primaryEntryTypeClass() {
		return HPAdaptData.class;
	}

	public static final Map<String, String> FUNGUSMAP_EN = new HashMap<>();
	public static final Map<String, String> FUNGUSMAP_DE = new HashMap<>();

	static {
		FUNGUSMAP_EN.put("1", "None");
		FUNGUSMAP_EN.put("2", "Relevant");
		FUNGUSMAP_EN.put("3", "Important Issue");
		FUNGUSMAP_DE.put("1", "Keine");
		FUNGUSMAP_DE.put("2", "Relevant");
		FUNGUSMAP_DE.put("3", "Ernsthaftes Problem");
	}
	
	@Override
	public void setData(HPAdaptData sr) {
		setLabel(sr.name(), EN, "name", DE, "Name");
		setLabel(sr.estimatedSavingsBuildingEnvelope(), EN, "Estimated savings via building envelope measures");
	}
}
