package org.smartrplace.smarteff.admin.timeseries;

import java.util.List;

import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.smartrplace.extensionservice.SmartEffTimeSeries;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoSelectionItem;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnitData;

public class SmartEffGaRoSelectionItemTS extends GaRoSelectionItem {
	//only relevant for level GW_LEVEL
	private final String gwId;
	private final SmartEffTimeSeries timeSeries;
	private final GenericDriverProvider provider;
	
	protected BuildingUnitData resource;
	
	//only relevant for level ROOM_LEVEL, TS_LEVEL
	public SmartEffGaRoSelectionItemTS getGwSelectionItem() {
		return (SmartEffGaRoSelectionItemTS) gwSelectionItem;
	}
	
	public SmartEffGaRoSelectionItemTS(BuildingData building, GenericDriverProvider provider) {
		this(building.getLocation(), provider);
	}
	public SmartEffGaRoSelectionItemTS(String gwId, GenericDriverProvider provider) {
		super(GaRoMultiEvalDataProvider.GW_LEVEL, gwId);
		this.gwId = gwId;
		this.provider = provider;
		this.timeSeries = null;
	}
	public SmartEffGaRoSelectionItemTS(BuildingUnitData buildingUnit,
			SmartEffGaRoSelectionItemTS superSelectionItem) {
		super(GaRoMultiEvalDataProvider.ROOM_LEVEL, buildingUnit.getLocation());
		//String unitId = buildingUnit.getLocation();
		this.gwId = superSelectionItem.gwId;
		this.gwSelectionItem = superSelectionItem;
		this.resource = buildingUnit;
		this.provider = superSelectionItem.provider;
		this.timeSeries = null;
	}
	public SmartEffGaRoSelectionItemTS(String tsId, SmartEffTimeSeries timeSeries,
			SmartEffGaRoSelectionItemTS superSelectionItem) {
		super(GaRoMultiEvalDataProvider.TS_LEVEL, tsId);
		this.gwId = superSelectionItem.gwId;
		this.gwSelectionItem = superSelectionItem.gwSelectionItem;
		this.roomSelectionItem = superSelectionItem;
		this.provider = superSelectionItem.provider;
		this.timeSeries = timeSeries;
	}
	
	@Override
	protected List<String> getDevicePaths(GaRoSelectionItem roomSelItem) {
		throw new IllegalStateException("getDevicePaths() should not be used with with DataProvider!");
	}
	
	@Override
	public TimeSeriesData getTimeSeriesData() {
		if(level == GaRoMultiEvalDataProvider.TS_LEVEL) {
			ReadOnlyTimeSeries recData = provider.getTimeSeries(timeSeries);
			return new TimeSeriesDataImpl(recData, id,
					id, InterpolationMode.STEPS);
		}
		return null;
	}
	
	protected BuildingUnitData getBuildingUnit() {
		switch(level) {
		case GaRoMultiEvalDataProvider.GW_LEVEL:
			throw new IllegalArgumentException("No gateway resource available");
		case GaRoMultiEvalDataProvider.ROOM_LEVEL:
			return resource;
		case GaRoMultiEvalDataProvider.TS_LEVEL:
			throw new UnsupportedOperationException("Access to resources of data row parents not implemented yet, but should be done!");
		default:
			throw new IllegalStateException("level "+level+" not supported!");
		}
	}

	@Override
	public Integer getRoomType() {
		if(resource == null) return null;
		return getRoomTypeStatic(getBuildingUnit());
	}
	
	public static Integer getRoomTypeStatic(BuildingUnitData room) {
		return room.roomData().type().getValue();
	}

	@Override
	public String getRoomName() {
		if(resource == null) return null;
		return getBuildingUnit().getName();
	}

	@Override
	public String getPath() {
		if(resource == null) return null;
		return getBuildingUnit().getPath();
	}
}
