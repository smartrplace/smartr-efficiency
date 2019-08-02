package org.sp.calculator.multibuild;

import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import extensionmodel.smarteff.multibuild.MultiBuildParams;

public class MultiBuildParamsPage extends EditPageGenericParams<MultiBuildParams> {
	@Override
	public void setData(MultiBuildParams params) {
		
		setLabelWithUnit(params.costSPBox(),
				EN, "Cost of SmartrplaceBox per Building (EUR)");
		setLabelWithUnit(params.costProjectBase(),
				EN, "Base cost for each individual project (EUR)");
		setLabelWithUnit(params.operationalCost(), EN,
				"Operational cost for the base system per year (EUR)");
		setLabel(params.buildingComponent(), EN, "Selected components per building");
		setLabel(params.communicationBusType(), EN, "Communication adapters");
			
		/* Documentation Links */
		setHeaderLink(EN, MultiBuildEval.WIKI_LINK + "#parameters");
		setLink(params.costProjectBase(), EN, MultiBuildEval.WIKI_LINK +  "#project-base-cost");
		setLink(params.operationalCost(), EN, MultiBuildEval.WIKI_LINK +  "#operational-cost-per-building-year");

	}
	@Override
	public Class<MultiBuildParams> primaryEntryTypeClass() {
		return MultiBuildParams.class;
	}
}
