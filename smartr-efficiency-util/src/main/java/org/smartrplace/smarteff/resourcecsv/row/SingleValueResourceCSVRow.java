package org.smartrplace.smarteff.resourcecsv.row;

import java.util.Locale;

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
	protected String link;
	protected String versionSpread;
	protected String versionDone;

	public SingleValueResourceCSVRow() {
		super();
		this.value = "Value";
		this.unit = "Unit";
		this.unit = "Link";
		this.versionSpread = "versionSpread";
		this.versionDone = "versionDone";
	}
	
	public SingleValueResourceCSVRow(SingleValueResource res, Locale locale) {
		this(res, locale, 1);
	}
	public SingleValueResourceCSVRow(SingleValueResource res, Locale locale, int versionSpread) {
		super(res);
		this.value = ResourceCSVUtil.getValueAsString(res, locale);
		if (res instanceof PhysicalUnitResource)
			this.unit = ((PhysicalUnitResource) res).getUnit().toString();
		else
			this.unit = "";
		this.versionSpread = ""+versionSpread;
		this.versionDone = "";
	}
	
	@Override
	public Class<? extends Resource> getResourceType() {
		return SingleValueResource.class;
	}

	@Override
	public String[] getCols() {
		return new String[] {
				this.name, this.value, this.unit, this.resource, this.link, "", this.versionSpread,
				this.versionDone, this.type, this.path
		};
	}
	
}