package org.smartrplace.smarteff.resourcecsv.row;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.PhysicalUnitResource;
import org.smartrplace.smarteff.resourcecsv.util.ResourceCSVUtil;

/**
 * Contains attributes (e.g. path, value, type…) of a resource that is
 * to be added to a row.
 * @author jruckel
 *
 */
public class SingleValueResourceCSVRow extends ResourceCSVRow<SingleValueResource> {

	protected String value;
	protected String unit;

	public SingleValueResourceCSVRow() {
		super();
		this.value = "Value";
		this.unit = "Unit";
	}
	
	public SingleValueResourceCSVRow(SingleValueResource res) {
		super(res);
		this.value = ResourceCSVUtil.getValueAsString(res);
		if (res instanceof PhysicalUnitResource)
			this.unit = ((PhysicalUnitResource) res).getUnit().toString();
		else
			this.unit = "";
	}
	
	@Override
	public Class<? extends Resource> getResourceType() {
		return SingleValueResource.class;
	}

	@Override
	public String[] getCols() {
		return new String[] {
				this.path, this.name, this.value, this.unit, this.type
		};
	}
	
}