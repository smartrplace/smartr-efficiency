package org.sp.calculator.multibuild;


import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.multibuild.MultiBuildResult;


public class MultiBuildResultPage extends EditPageGeneric<MultiBuildResult> {
	@Override
	public String label(OgemaLocale locale) {
		return "Multi-building IoT project calculator result page";
	}

	@Override
	public Class<MultiBuildResult> primaryEntryTypeClass() {
		return MultiBuildResult.class;
	}

	@Override
	public void setData(MultiBuildResult result) {
		setLabel(result.name(), EN, "name", DE, "Name");

		setLabel(result.costPerBuilding(), EN, "Variable cost per building (EUR)");

		/* Documentation Links */
		setHeaderLink(EN, MultiBuildEval.WIKI_LINK + "#results");
		setLink(result.costPerBuilding(), EN, MultiBuildEval.WIKI_LINK +  "#cost-per-building");
	}
}
