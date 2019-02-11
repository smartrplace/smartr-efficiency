package org.sp.example.buildingwizard;

import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.smarteff.util.wizard.WizBexWidgetProvider;
import org.sp.example.smarteff.eval.capability.SPEvalDataService;

import extensionmodel.smarteff.api.common.BuildingUnit;

public class WizBexWidgetProviderSpec extends WizBexWidgetProvider<BuildingUnit, BuildingUnit> {
	@Override
	protected void checkResource(BuildingUnit res) {
		IntegerResource subPos = res.getSubResource("wizardPosition", IntegerResource.class);
		if(!subPos.isActive()) {
			subPos.create();
			subPos.activate(true);
		}
	}

	@Override
	protected Class<BuildingUnit> getType() {
		return BuildingUnit.class;
	}

	@Override
	protected String getEditPageURL() {
		return SPPageUtil.getProviderURL(SPEvalDataService.WIZBEX_ROOM.provider);
	}
}
