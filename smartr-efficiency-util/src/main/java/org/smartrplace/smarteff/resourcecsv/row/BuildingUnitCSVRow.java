package org.smartrplace.smarteff.resourcecsv.row;

import org.ogema.core.model.Resource;

import extensionmodel.smarteff.api.common.BuildingUnit;

public class BuildingUnitCSVRow extends ResourceCSVRow<BuildingUnit> {
	
	protected String groundArea;
	protected String totalOutsideWallArea;
	protected String roomHeight;
	
	public BuildingUnitCSVRow(BuildingUnit res) {
		super(res);
		this.groundArea = Float.toString(res.groundArea().getValue());
		this.totalOutsideWallArea = Float.toString(res.totalOutsideWallArea().getValue());
		this.roomHeight = Float.toString(res.roomHeight().getValue());
	}

	public BuildingUnitCSVRow() {
		super();
		this.groundArea = "Ground Area (m²)";
		this.totalOutsideWallArea = "Total area of outside walls (m²)";
		this.roomHeight = "Room height (m²)";
	}

	@Override
	public Class<? extends Resource> getResourceType() {
		return BuildingUnit.class;
	}
	
	@Override
	public String[] getCols() {
		return new String[] {
				this.path, this.name, this.groundArea, this.roomHeight, this.totalOutsideWallArea
		};
	}

}
