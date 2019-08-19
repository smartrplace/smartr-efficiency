package org.smartrplace.smarteff.resourcecsv.util;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.smartrplace.smarteff.resourcecsv.CSVConfiguration;

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
	
	public enum init { EMPTY, HEADER }

	public SingleValueResourceCSVRow(init initMode) {
		this.conf = null;
		if (initMode == init.EMPTY) return;

		this.name = CSVConfiguration.HEADERS.NAME;
		this.path = CSVConfiguration.HEADERS.PATH;
		this.type = CSVConfiguration.HEADERS.TYPE;
		this.resource = CSVConfiguration.HEADERS.RESOURCE;
		this.value = CSVConfiguration.HEADERS.VALUE;
		this.unit = CSVConfiguration.HEADERS.UNIT;
		this.elementType = CSVConfiguration.HEADERS.ELEMENTTYPE;
		this.link = CSVConfiguration.HEADERS.LINK;
		this.versionSpread = CSVConfiguration.HEADERS.VERSIONSPREAD;
		this.versionDone = CSVConfiguration.HEADERS.VERSIONDONE;
		this.activeStatus = CSVConfiguration.HEADERS.ISACTIVE;
	}
	
	public SingleValueResourceCSVRow() {
		this(init.HEADER);
	}
	
	public SingleValueResourceCSVRow(Resource res, CSVConfiguration conf, Locale locale, String label) {
		this(res, conf, locale, label, 1);
	}
	public SingleValueResourceCSVRow(Resource res, CSVConfiguration conf, Locale locale, String label,
			int versionSpread) {
		this.conf = conf;
		this.path = getPath(res);
		this.type = res.getResourceType().getName();

		this.name = label;
		this.resource = res.getName();
		if (res instanceof SingleValueResource)
			this.value = ResourceCSVUtil.getValueAsString((SingleValueResource) res, locale);
		this.unit = ResourceCSVUtil.getUnit(res);
		this.versionSpread = ""+versionSpread;
		this.versionDone = "";
		this.activeStatus = "true"; // TODO
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
		return ResourceCSVUtil.getRelativePath(res, conf);
	}
	
	/** Get location relative to root. */
	public String getLocation(Resource res) {
		return ResourceCSVUtil.getRelativeLocation(res, conf);
	}

}