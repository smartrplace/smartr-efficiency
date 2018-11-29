package org.smartrplace.smarteff.admin.timeseries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.smartrplace.extensionservice.SmartEffTimeSeries;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.garo.api.base.EvaluationInputImplGaRo;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI.Level;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.resource.GaRoSelectionItemResource;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ResourceHelper.DeviceInfo;
import de.iwes.widgets.html.selectiontree.SelectionItem;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnit;

public class SmartEffGaRoProviderTS extends GaRoMultiEvalDataProvider<SmartEffGaRoSelectionItemTS> {

	private final BuildingData[] buildingEntryResources;
	private final BuildingUnit[] buildingUnitEntryResources;
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
	public SmartEffGaRoProviderTS(BuildingUnit[] buildingUnitEntryResources, GenericDriverProvider provider,
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
			if(buildingUnitEntryResources != null) for(BuildingUnit room: buildingUnitEntryResources)
				result.add(new SmartEffGaRoSelectionItemTS(room, superItem));
			else if(buildingEntryResources[0].buildingUnit().isActive()) for(BuildingUnit room: buildingEntryResources[0].buildingUnit().getAllElements())
				result.add(new SmartEffGaRoSelectionItemTS(room, superItem));
			result.add(new SmartEffGaRoSelectionItemTS(GaRoMultiEvalDataProvider.BUILDING_OVERALL_ROOM_ID, null, superItem));
			return result;
		case GaRoMultiEvalDataProvider.TS_LEVEL:
			//First get all time series declarators relevant to entry resources
			List<SmartEffTimeSeries> recIds = new ArrayList<>();
			if(buildingUnitEntryResources == null) {
				for(BuildingData bd: buildingEntryResources)
					recIds.addAll(provider.getTSConfigs(bd, null));
			} else {
				for(BuildingUnit bd: buildingUnitEntryResources)
					recIds.addAll(provider.getTSConfigs(bd, null));
			}

			result = new ArrayList<>();
			if(superItem.resource == null) {
				for(SmartEffTimeSeries ts: recIds) {
					result.add(new SmartEffGaRoSelectionItemTS(ts.dataTypeId().getValue(),
							ts, superItem));
				}				
			} else {
				//here only use ids that belong to the room
				List<String> devicePaths = superItem.getDevicePaths();
				for(SmartEffTimeSeries ts: recIds) {
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
	@Override
	public List<String> getGatewayIds() {
		List<String> result = new ArrayList<>();
		for(BuildingData gw: buildingEntryResources)
			result.add(gw.getLocation());
		return result;
	}
	@Override
	public EvaluationInputImplGaRo getData(List<SelectionItem> items) {
		//from GaRoMultiEvalProviderResource
		List<TimeSeriesData> tsList = new ArrayList<>();
		List<DeviceInfo> devList = new ArrayList<>();
		for(SelectionItem item: items) {
			tsList.add(terminalOption.getElement(item));
			if(item instanceof GaRoSelectionItemResource) {
				DeviceInfo dev = ResourceHelper.getDeviceInformation(((GaRoSelectionItemResource)item).resource);
				devList.add(dev);				
			} else devList.add(null);
		}
		return new EvaluationInputImplGaRo(tsList, devList);
	}

}
