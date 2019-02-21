package org.sp.example.buildingwizard;

import org.ogema.core.model.Resource;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.smarteff.util.wizard.WizBexWidgetProvider;
import org.sp.example.smarteff.eval.capability.SPEvalDataService;

import extensionmodel.smarteff.api.common.BuildingUnit;

public abstract class WizBexWidgetProviderSpec<T extends Resource> extends WizBexWidgetProvider<T, BuildingUnit> {
	public WizBexWidgetProviderSpec() {
		super();
	}

	@Override
	protected Class<BuildingUnit> typeS() {
		return BuildingUnit.class;
	}

	@Override
	protected String editPageURL() {
		return SPPageUtil.getProviderURL(SPEvalDataService.WIZBEX_ROOM.provider);
	}

	@Override
	protected String entryPageUrl() {
		return SPPageUtil.getProviderURL(SPEvalDataService.WIZBEX_ENTRY.provider);
	}

	@Override
	protected String tablePageUrl() {
		return SPPageUtil.getProviderURL(SPEvalDataService.WIZBEX_ROOM_TABLE.provider);
	}
	
	@Override
	protected String editParentPageURL() {
		return SPPageUtil.getProviderURL(SPEvalDataService.WIZBEX_BUILDING.provider);
	}
}

