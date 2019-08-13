package org.sp.calculator.multibuild;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import extensionmodel.smarteff.multibuild.BuildingComponent;
import extensionmodel.smarteff.multibuild.MultiBuildParams;

public class BuildingComponentParamsPage extends EditPageGenericParams<BuildingComponent> {
	@Override
	public void setData(BuildingComponent params) {
		
		setLabel(params.name(),
				EN, "Name (Human readable identifier)");
		setLabelWithUnit(params.cost(),
				EN, "Initial cost per item incl. hardware and configuration (EUR)");
		setTableHeader(params.cost(), EN, "Cost");
		setLabelWithUnit(params.yearlyCost(), EN,
				"Operational cost per item per year (EUR)");
		setLabel(params.type(), EN, "Communication adapter required");
		setLabel(params.link(), EN, "Documentation link");
			
		/* Documentation Links */
		setHeaderLink(EN, MultiBuildEval.WIKI_LINK + "#hardware-components-for-buildings");
	}
	
	@Override
	public Class<BuildingComponent> primaryEntryTypeClass() {
		return BuildingComponent.class;
	}
	
	@Override
	public Class<? extends SmartEffResource> getPrimarySuperType() {
		return MultiBuildParams.class;
	}
}
