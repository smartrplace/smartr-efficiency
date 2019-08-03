package org.smartrplace.smarteff.resourcecsv.util;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.PhysicalUnitResource;
import org.ogema.tools.resource.util.ResourceUtils;

/**
 * Contains attributes (e.g. path, value, type…) of a resource that is
 * to be added to a row.
 * @author jruckel
 *
 */
public class SingleValueResourceCSVRow {

	final protected String name;
	final protected String path;
	final protected String type;
	final protected String resource;
	final protected String value;
	final protected String unit;
	final protected String link;
	final protected String versionSpread;
	final protected String versionDone;
	final protected String activeStatus;

	public SingleValueResourceCSVRow() {
		this.name = "Name";
		this.path = "Path";
		this.type = "Type";
		this.resource = "Resource";
		this.value = "Value";
		this.unit = "Unit";
		this.link = "Link";
		this.versionSpread = "versionSpread";
		this.versionDone = "versionDone";
		this.activeStatus = "isActive";
	}
	
	public SingleValueResourceCSVRow(SingleValueResource res, Locale locale) {
		this(res, locale, 1);
	}
	public SingleValueResourceCSVRow(SingleValueResource res, Locale locale, int versionSpread) {
		this.path = res.getPath();
		this.type = res.getResourceType().getSimpleName();

		// If a "name" sub-resource exists, use it for the name.
		this.name = ResourceUtils.getHumanReadableShortName(res);
		this.resource = res.getName();
		this.value = ResourceCSVUtil.getValueAsString(res, locale);
		if (res instanceof PhysicalUnitResource)
			this.unit = ((PhysicalUnitResource) res).getUnit().toString();
		else
			this.unit = "";
		this.versionSpread = ""+versionSpread;
		this.versionDone = "";
		this.activeStatus = "true";
		this.link = "";
	}
	
	public String[] getCols() {
		return new String[] {
				this.name, this.value, this.unit, this.resource, this.link, "", this.versionSpread,
				this.versionDone, this.type, this.path, this.activeStatus
		};
	}

	/** Returns columns as List */
	public List<String> values() {
		return Arrays.asList(getCols());
	}

}