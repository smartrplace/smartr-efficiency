package org.sp.example.smarteff.driver.basic.jaxb;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;
import org.smartrplace.extensionservice.driver.DriverProvider;
import org.smartrplace.smarteff.util.CapabilityHelper;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.jaxb.GaRoMultiEvalDataProviderJAXB;
import de.iwes.timeseries.eval.garo.api.jaxb.GaRoSelectionItemJAXB;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.html.selectiontree.SelectionItem;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.driver.basic.BasicGaRoDataProviderConfig;

public class DataDriverGaroJAXB implements DriverProvider {
	private final GaRoMultiEvalDataProviderJAXB jaxbProvider;

	public DataDriverGaroJAXB(GaRoMultiEvalDataProviderJAXB jaxbProvider) {
		this.jaxbProvider = jaxbProvider;
	}

	@Override
	public String label(OgemaLocale locale) {
		return jaxbProvider.label(locale);
	}

	@Override
	public List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(BuildingData.class);
	}

	@Override
	public Class<? extends DataProvider<?>> getDataProviderType() {
		return GaRoMultiEvalDataProviderJAXB.class;
	}

	@Override
	public void init(ApplicationManagerSPExt appManExt) {
	}

	@Override
	public DataProvider<?> getDataProvider(int entryTypeIdx, List<Resource> entryResources, Resource userData,
			ExtensionUserDataNonEdit userDataNonEdit) {
		final BuildingData building = (BuildingData) entryResources.get(0);
		final BasicGaRoDataProviderConfig gwsAllowedRes = CapabilityHelper.getSubResourceSingle(
				((SmartEffUserDataNonEdit)userDataNonEdit).configurationSpace(),
				BasicGaRoDataProviderConfig.class);
		if(gwsAllowedRes == null) return null;
		String loc = building.getLocation();
		String gwIdLoc = null;
		for(int i=0; i<gwsAllowedRes.buildingLocations().getValues().length; i++) {
			if(gwsAllowedRes.buildingLocations().getValues()[i].equals(loc)) {
				gwIdLoc = gwsAllowedRes.gwIdsAllowed().getValues()[i];
				break;
			}
		}
		if(gwIdLoc == null) return null;
		final String gwId = gwIdLoc;
		
		return new GaRoMultiEvalDataProvider<GaRoSelectionItemJAXB>() {

			@Override
			protected List<SelectionItem> getOptions(int level, GaRoSelectionItemJAXB superItem) {
				//List<String> gwsAllowed = Arrays.asList(gwsAllowedRes.gwIdsAllowed().getValues());
				if(level == GaRoMultiEvalDataProvider.GW_LEVEL) {
					List<SelectionItem> available = jaxbProvider.getOptions(level, superItem);
					List<SelectionItem> result = new ArrayList<>();
					for(SelectionItem si: available) {
						if(gwId.equals(si.id())) result.add(si);
					}
					return result ;
				}
				if(!gwId.equals(superItem.getGwSelectionItem().id())) throw new IllegalStateException("We have a lower level "
						+ "request with a gateway not allowed for the user!");
				return jaxbProvider.getOptions(level, superItem);
			}

			@Override
			public boolean providesMultipleGateways() {
				return jaxbProvider.providesMultipleGateways();
			}

			@Override
			public void setGatewaysOffered(List<SelectionItem> gwSelectionItemsToOffer) {
				throw new IllegalStateException("setGatewaysOffered not supported here!");
			}
		};
	}

	@Override
	public List<ReadOnlyTimeSeries> getTimeSeries(Resource entryResource, GenericDataTypeDeclaration dataType) {
		throw new UnsupportedOperationException("not implemented yet!");
	}

}
