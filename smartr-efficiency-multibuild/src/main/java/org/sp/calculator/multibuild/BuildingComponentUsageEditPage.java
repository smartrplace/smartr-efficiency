package org.sp.calculator.multibuild;


import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericWithTable;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.multibuild.BuildingComponentUsage;
import extensionmodel.smarteff.multibuild.MultiBuildData;

public class BuildingComponentUsageEditPage extends EditPageGenericWithTable<BuildingComponentUsage> {
	@Override
	public String label(OgemaLocale locale) {
		return "Edit components used in each building";
	}

	@Override
	public Class<BuildingComponentUsage> primaryEntryTypeClass() {
		return BuildingComponentUsage.class;
	}

	@Override
	public Class<? extends SmartEffResource> getPrimarySuperType() {
		return MultiBuildData.class;
	}

	@Override
	protected void addWidgets() {
		super.addWidgets();
	}
	
	@Override
	public void setData(BuildingComponentUsage data) {
		setLabel(data.paramType(), EN, "Building component type");
		setLabel(data.number(), EN, "Number of the selected items to be used per building in the project");
		setLabel(data.additionalCostPerItem(),
				EN, "Cost for configuration, installation per item, project-specific (EUR)");
		setLabel(data.additionalYearlyCost(), EN, "Additional yearly cost for maintenance (EUR)");
		setLabel(data.alternativeOfferText(), EN, "Text to be used in offer");
		setLabel(data.comment(), EN, "Comment (optional)");
		setTableHeader(data.paramType().name(), EN, "Name");
		setTableHeader(data.number(), EN, "#Num");
		setTableHeader(data.paramType().cost(), EN, "EUR/item");
		/* Documentation Links */
		setHeaderLink(EN, MultiBuildEval.WIKI_LINK + "#hardware-components-for-buildings-selected");
	}

	@Override
	protected void defaultValues(BuildingComponentUsage data, DefaultSetModes mode) {
		setDefault(data.number(), 1, mode);
	}
}