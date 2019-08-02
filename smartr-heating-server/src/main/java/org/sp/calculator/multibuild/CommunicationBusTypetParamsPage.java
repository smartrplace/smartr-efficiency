package org.sp.calculator.multibuild;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import extensionmodel.smarteff.multibuild.CommunicationBusType;
import extensionmodel.smarteff.multibuild.MultiBuildParams;

public class CommunicationBusTypetParamsPage extends EditPageGenericParams<CommunicationBusType> {
	@Override
	public void setData(CommunicationBusType params) {
		
		setLabel(params.name(),
				EN, "Identifier");
		setLabelWithUnit(params.cost(),
				EN, "Initial cost per item incl. hardware and configuration (EUR)");
		setLabelWithUnit(params.yearlyCost(), EN,
				"Operational cost per item per year (EUR)");
		setLabelWithUnit(params.development(), EN, "Development effort for driver (EUR)");
		setLabel(params.link(), EN, "Documentation link");
			
		/* Documentation Links */
		setHeaderLink(EN, MultiBuildEval.WIKI_LINK + "#communication-bus-or-hardware-interface");
	}
	@Override
	public Class<CommunicationBusType> primaryEntryTypeClass() {
		return CommunicationBusType.class;
	}
	
	@Override
	public Class<? extends SmartEffResource> getPrimarySuperType() {
		return MultiBuildParams.class;
	}
}
