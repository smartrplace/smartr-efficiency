package org.smartrplace.commontypes;

import org.smartrplace.smarteff.util.EditPageBase;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;

public class BuildingEditPageBak extends EditPageBase<BuildingData> {
	@Override
	public String label(OgemaLocale locale) {
		return "Standard Building Edit Page";
	}
	
	public boolean checkResource(BuildingData data) {
		if(!checkResourceBase(data, true)) return false;
		if(data.heatedLivingSpace().getValue() <= 0) return false;
		return true;
	}

	@Override
	protected Class<BuildingData> primaryEntryTypeClass() {
		return BuildingData.class;
	}

	@Override
	protected void getEditTableLines(EditPageBase<BuildingData>.EditTableBuilder etb) {
		etb.addEditLine("Name", mh.stringEdit("name", alert));
		etb.addEditLine("Beheizte Fläche", mh.floatEdit("heatedLivingSpace", alert, 1, 999999, "Heated Living Space value outside range!"));
	}
}
