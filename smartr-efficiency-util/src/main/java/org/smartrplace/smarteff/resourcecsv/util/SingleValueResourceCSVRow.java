package org.smartrplace.smarteff.resourcecsv.util;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.PhysicalUnitResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.smarteff.resourcecsv.CSVConfiguration;

import de.iwes.util.resource.ResourceHelper;

/**
 * Contains attributes (e.g. path, value, type…) of a resource that is
 * to be added to a row.
 * @author jruckel
 *
 */
public class SingleValueResourceCSVRow {

	protected String name;
	protected String path;
	protected String type;
	protected String resource;
	protected String value;
	protected String unit;
	protected String elementType;
	protected String link;
	protected String versionSpread;
	protected String versionDone;
	protected String activeStatus;
	
	protected CSVConfiguration conf;
	
	enum init { EMPTY, HEADER }

	public SingleValueResourceCSVRow(init initMode) {
		this.conf = null;
		if (initMode == init.EMPTY) return;

		this.name = "Name";
		this.path = "Path";
		this.type = "Type";
		this.resource = "Resource";
		this.value = "Value";
		this.unit = "Unit";
		this.elementType = "ElementType";
		this.link = "Link";
		this.versionSpread = "versionSpread";
		this.versionDone = "versionDone";
		this.activeStatus = "isActive";
	}
	
	public SingleValueResourceCSVRow() {
		this(init.HEADER);
	}
	
	public SingleValueResourceCSVRow(SingleValueResource res, CSVConfiguration conf, Locale locale, String label) {
		this(res, conf, locale, label, 1);
	}
	public SingleValueResourceCSVRow(SingleValueResource res, CSVConfiguration conf, Locale locale, String label,
			int versionSpread) {
		this.conf = conf;
		this.path = getPath(res);
		this.type = res.getResourceType().getSimpleName();

		this.name = label;
		this.resource = res.getName();
		this.value = ResourceCSVUtil.getValueAsString(res, locale);
		this.unit = ResourceCSVUtil.getUnit(res);
		this.versionSpread = ""+versionSpread;
		this.versionDone = "";
		this.activeStatus = "true";
		this.elementType = "";
		this.link = "";
	}
	
	public String[] getCols() {
		return new String[] {
				this.name, this.value, this.unit, this.resource, this.link, this.elementType, this.versionSpread,
				this.versionDone, this.type, this.path, this.activeStatus
		};
	}

	/** Returns columns as List */
	public List<String> values() {
		return Arrays.asList(getCols());
	}
	
	/** Get path relative to root. */
	public String getPath(Resource res) {
		String resPath = res.getPath();
		String rootPath = conf.root.getPath();
		return resPath.replaceFirst("^" + rootPath + "/", "");
	}

}