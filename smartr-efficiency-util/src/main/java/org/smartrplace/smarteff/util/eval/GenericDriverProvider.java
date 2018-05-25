package org.smartrplace.smarteff.util.eval;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;
import org.smartrplace.extensionservice.driver.DriverProvider;
import org.smartrplace.smarteff.util.CapabilityHelper;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnitData;

public class GenericDriverProvider implements DriverProvider {

	@Override
	public String label(OgemaLocale locale) {
		return "Generic Timeseries Driver";
	}

	@Override
	public List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(BuildingData.class, BuildingUnitData.class);
	}

	@Override
	public Class<? extends DataProvider<?>> getDataProviderType() {
		return SmartEffGaRoProviderTS.class;
	}

	@Override
	public void init(ApplicationManagerSPExt appManExt) {
	}

	@Override
	public DataProvider<?> getDataProvider(int entryTypeIdx, List<Resource> entryResources, Resource userData,
			ExtensionUserDataNonEdit userDataNonEdit) {
		return new SmartEffGaRoProviderTS();
	}

	@Override
	public List<ReadOnlyTimeSeries> getTimeSeries(Resource entryResource, GenericDataTypeDeclaration dataType) {
		// TODO Auto-generated method stub
		return null;
	}

	public void addSchedule(Resource parent, Schedule sched) {
		
	}
}
