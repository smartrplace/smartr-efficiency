package org.sp.example.smartrheating;

import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.EditPageBase;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.smartrheating.SmartrHeatingData;

public class SmartrHeatingEditPage extends EditPageBase<SmartrHeatingData> {
	@Override
	public String label(OgemaLocale locale) {
		return "SmartrHeating Edit Page";
	}
	
	public boolean checkResource(SmartrHeatingData data) {
		if(!checkResourceBase(data, false)) return false;
		String newName = CapabilityHelper.getnewDecoratorName("SmartrHeatingProject", data.getParent());
		ValueResourceHelper.setIfNew(data.name(), newName);
		if(data.numberOfRadiators().getValue() <= 0) return false;
		if(data.numberOfRooms().getValue() <= 0) return false;
		return true;
	}

	@Override
	protected Class<SmartrHeatingData> primaryEntryTypeClass() {
		return SmartrHeatingData.class;
	}

	@Override
	protected void getEditTableLines(EditPageBase<SmartrHeatingData>.EditTableBuilder etb) {
		etb.addEditLine("Name", mh.stringEdit("name", alert));
		etb.addEditLine("Zahl Termostate", mh.integerEdit("numberOfRadiators", alert, 1, 999999, "Value outside range!"));
		etb.addEditLine("Zahl Räume", mh.integerEdit("numberOfRooms", alert, 1, 999999, "Value outside range!"));
	}
}
