package org.sp.calculator.multibuild;


import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.multibuild.MultiBuildData;

public class MultiBuildEditPage extends EditPageGeneric<MultiBuildData> {
	@Override
	public String label(OgemaLocale locale) {
		return "Multi-building IoT project calculator edit page";
	}

	@Override
	public Class<MultiBuildData> primaryEntryTypeClass() {
		return MultiBuildData.class;
	}

	@Override
	public void setData(MultiBuildData data) {
		setLabel(data.buildingComponentUsage(), EN, "Selected building components for project");
		setLabel(data.name(), EN, "name", DE, "Name");
		setLabel(data.buildingNum(), EN, "Number of buildings in project");
		setLabelWithUnit(data.operationalCost(),
				EN, "Project-specific operational cost per year in EUR. These cost are added to the yearly cost determined via parameters.");
		
		/* Documentation Links */
		setHeaderLink(EN, MultiBuildEval.WIKI_LINK + "#data");
		setLink(data.operationalCost(), EN, MultiBuildEval.WIKI_LINK +  "#operational-cost-overall-year");
	}

	@Override
	protected void defaultValues(MultiBuildData data, DefaultSetModes mode) {
		setDefault(data.buildingNum(), 1, mode);
		setDefault(data.operationalCost(), 0, mode);
	}
	
	/** TODO: This may change to null in the future like BuildingData*/
	@Override
	public Class<? extends SmartEffResource> getPrimarySuperType() {
		return BuildingData.class;
	}
}