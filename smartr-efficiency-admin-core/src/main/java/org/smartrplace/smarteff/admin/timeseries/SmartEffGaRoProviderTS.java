package org.smartrplace.smarteff.admin.timeseries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataType.Level;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.html.selectiontree.SelectionItem;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnitData;

public class SmartEffGaRoProviderTS extends GaRoMultiEvalDataProvider<SmartEffGaRoSelectionItemTS> {

	private final BuildingData[] buildingEntryResources;
	private final BuildingUnitData[] buildingUnitEntryResources;
	private final GenericDriverProvider provider;
	private final Map<String, GaRoDataType> knownGaRoTypes;
	
	/** The provider can either be setup on overall-level with a set of buildings or building-internal
	 * with a set of rooms that should all belong to a common building
	 */
	public SmartEffGaRoProviderTS(BuildingData[] buildingEntryResources, GenericDriverProvider provider,
			Map<String, GaRoDataType> knownGaRoTypes) {
		super();
		this.buildingEntryResources = buildingEntryResources;
		this.buildingUnitEntryResources = null;
		this.provider = provider;
		this.knownGaRoTypes = knownGaRoTypes;
	}
	public SmartEffGaRoProviderTS(BuildingUnitData[] buildingUnitEntryResources, GenericDriverProvider provider,
			Map<String, GaRoDataType> knownGaRoTypes) {
		super();
		BuildingData building = ResourceHelper.getFirstParentOfType(
				buildingUnitEntryResources[0], BuildingData.class);
		this.buildingEntryResources = new BuildingData[] {building};
		this.buildingUnitEntryResources = buildingUnitEntryResources;
		this.provider = provider;
		this.knownGaRoTypes = knownGaRoTypes;
	}

	@Override
	protected List<SelectionItem> getOptions(int level, SmartEffGaRoSelectionItemTS superItem) {
		switch(level) {
		case GaRoMultiEvalDataProvider.GW_LEVEL:
			if(gwSelectionItems == null) {
				gwSelectionItems = new ArrayList<>();
				for(BuildingData gw: buildingEntryResources)
					gwSelectionItems.add(new SmartEffGaRoSelectionItemTS(gw, provider));
			}
			return gwSelectionItems;
		case GaRoMultiEvalDataProvider.ROOM_LEVEL:
			List<SelectionItem> result = new ArrayList<>();
			if(buildingUnitEntryResources != null) for(BuildingUnitData room: buildingUnitEntryResources)
				result.add(new SmartEffGaRoSelectionItemTS(room, superItem));
			else if(buildingEntryResources[0].subUnits().isActive()) for(BuildingUnitData room: buildingEntryResources[0].subUnits().getAllElements())
				result.add(new SmartEffGaRoSelectionItemTS(room, superItem));
			result.add(new SmartEffGaRoSelectionItemTS(GaRoMultiEvalDataProvider.BUILDING_OVERALL_ROOM_ID, null, superItem));
			return result;
		case GaRoMultiEvalDataProvider.TS_LEVEL:
			//First get all time series declarators relevant to entry resources
			List<GenericTSDPTimeSeries> recIds = new ArrayList<>();
			if(buildingUnitEntryResources == null) {
				for(BuildingData bd: buildingEntryResources)
					recIds.addAll(provider.getTSConfigs(bd, null));
			} else {
				for(BuildingUnitData bd: buildingUnitEntryResources)
					recIds.addAll(provider.getTSConfigs(bd, null));
			}

			result = new ArrayList<>();
			if(superItem.resource == null) {
				for(GenericTSDPTimeSeries ts: recIds) {
					result.add(new SmartEffGaRoSelectionItemTS(ts.dataTypeId().getValue(),
							ts, superItem));
				}				
			} else {
				//here only use ids that belong to the room
				List<String> devicePaths = superItem.getDevicePaths();
				for(GenericTSDPTimeSeries ts: recIds) {
					String tsId = ts.dataTypeId().getValue();
					GaRoDataType gtype = knownGaRoTypes.get(tsId);
					//Gateway-specific types shall be evaluated for every room
					if(gtype != null &&  gtype.getLevel() == Level.GATEWAY) {
						result.add(new SmartEffGaRoSelectionItemTS(tsId, ts, superItem));
						continue;
					}
					for(String devE: devicePaths) {
						if(tsId.startsWith(devE)) {
							result.add(new SmartEffGaRoSelectionItemTS(tsId, ts, superItem));
							break;
						}
					}
				}
			}
			return result;
		default:
			throw new IllegalArgumentException("unknown level");
		}
	}

	@Override
	public boolean providesMultipleGateways() {
		return true;
	}

	@Override
	public void setGatewaysOffered(List<SelectionItem> gwSelectionItemsToOffer) {
		throw new IllegalStateException("setGatewaysOffered not supported here!");
	}

}
