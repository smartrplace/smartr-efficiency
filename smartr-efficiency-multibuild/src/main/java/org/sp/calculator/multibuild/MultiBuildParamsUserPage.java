package org.sp.calculator.multibuild;

import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import extensionmodel.smarteff.multibuild.MultiBuildParamsUser;

public class MultiBuildParamsUserPage extends EditPageGenericParams<MultiBuildParamsUser> {
	@Override
	public void setData(MultiBuildParamsUser params) {
		setLabel(params.buildingComponent(), EN, "Selected components per building specifically for user");
		setLabel(params.communicationBusType(), EN, "Communication adapters available specifically for user");
			
		/* Documentation Links */
		setHeaderLink(EN, MultiBuildEval.WIKI_LINK + "#parameters");
	}
	@Override
	public Class<MultiBuildParamsUser> primaryEntryTypeClass() {
		return MultiBuildParamsUser.class;
	}
}
