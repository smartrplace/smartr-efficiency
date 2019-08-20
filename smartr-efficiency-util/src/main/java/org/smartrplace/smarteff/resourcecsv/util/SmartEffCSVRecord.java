package org.smartrplace.smarteff.resourcecsv.util;

import org.apache.commons.csv.CSVRecord;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.persistence.DBConstants;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.smarteff.resourcecsv.CSVConfiguration;

public class SmartEffCSVRecord {

	protected final CSVRecord record;
	protected final CSVConfiguration conf;
	
	public SmartEffCSVRecord(CSVRecord record, CSVConfiguration conf) {
		this.record = record;
		this.conf = conf;
	}
	
	public boolean isEmpty() {
		return record.size() == 1 && record.get(0).isEmpty();
	}

	public boolean isSet(String name) {
		return record.isSet(name);
	}

	/**
	 * @return null if not mapped or unset
	 */
	public String get(String name) {
		if (isSet(name))
			return record.get(name);
		else
			return null;
	}
	
	public String get(int i) {
		return record.get(i);
	}

	public int size() {
		return record.size();
	}

	public String getComment() {
		return record.getComment();
	}

	public long getRecordNumber() {
		return record.getRecordNumber();
	}
	
	/**
	 * @return name of the type of this record.  Null if resource has an invalid path.
	 * 'SingleValueResource' for SVR.  Use {@link #getSvrType()} to get the actual type.
	 */
	public String getTypeName() {
		if (!isSet(CSVConfiguration.HEADERS.TYPE) || !hasValidPath())
			return null;
		String type = getType();
		if (isSingleValueResource(type))
			return CSVConfiguration.HEADERS.SINGLE_VALUE_RESOURCE;
		else
			return type;
	}
	
	
	public Class<? extends SingleValueResource> getSvrType() {
		if (!isSet(CSVConfiguration.HEADERS.TYPE) || !hasValidPath())
			return null;
		String type = getType();
		try {
			Class<?> clazz = Class.forName(type);
			if (SingleValueResource.class.isAssignableFrom(clazz)) {
				return (Class<? extends SingleValueResource>) clazz;
			}
		} catch (ClassNotFoundException e) {
			//
		}
		return null;
	}
	
	public boolean isSingleValueResource(String type) {
		Class<?> clazz;
		try {
			clazz = Class.forName(type);
		} catch (ClassNotFoundException e) {
			return false;
		}
		return SingleValueResource.class.isAssignableFrom(clazz);
		//return ArrayUtils.contains(CSVConfiguration.SUPPORTED_SVR, type);
	}
	
	public boolean hasValidPath() {
		return ResourceUtils.isValidResourcePath(getPath());
	}
	
	/**
	 * Get the absolute resource path.
	 * @return
	 */
	public String getPath() {
		return conf.origRootPath + DBConstants.PATH_SEPARATOR + getRelPathRoot();
	}
	
	/**
	 * Get path relative to conf.origParentPath
	 * @return
	 */
	public String getRelPath() {
		return getPath().replaceFirst("^" + conf.origParentPath + DBConstants.PATH_SEPARATOR, "");
	}
	
	/**
	 * Get the path relative to the root resource, i.e. the value of the 'Path' column.
	 * @return
	 */
	public String getRelPathRoot() {
		if(!isSet(CSVConfiguration.HEADERS.PATH))
			return null;
		return get(CSVConfiguration.HEADERS.PATH);
	}
	
	public String[] getValues() {
		String[] values = new String[record.size()];
		for (int i = 0; i < values.length; i++)
			values[i] = record.get(i);
		return values;
	}

	/** Check if all required fields are present */
	public boolean isValid() {
		for (String header : CSVConfiguration.HEADERS.REQUIRED) {
			if (!isSet(header))
				return false;
		}
		return true;
	}

	public String getParentPath() {
		String path = getPath();
		String name = getResName();
		return path.replaceFirst(DBConstants.PATH_SEPARATOR + name + "$", "");
	}
	
	/** Get the human-readable name from the 'Name' column. */
	public String getName() {
		return get(CSVConfiguration.HEADERS.NAME);
	}

	/** Get the value from the 'Value' column. */
	public String getValue() {
		return get(CSVConfiguration.HEADERS.VALUE);
	}

	/** Get the unit from the 'Unit' column. */
	public String getUnit() {
		return get(CSVConfiguration.HEADERS.UNIT);
	}	

	/** Get the resource name from the 'Resource' column */
	public String getResName() {
		return get(CSVConfiguration.HEADERS.RESOURCE);
	}

	/** Get the link from the 'link' column. */
	public String getLink() {
		return get(CSVConfiguration.HEADERS.LINK);
	}

	/** Get the element type from the 'ElementType' column. */
	public String getElementType() {
		return get(CSVConfiguration.HEADERS.ELEMENTTYPE);
	}

	/** Get the active status from the 'isActive' column. */
	public boolean getActive() {
		return Boolean.parseBoolean(get(CSVConfiguration.HEADERS.ISACTIVE));
	}
	
	/** Get the type directly the 'type' column. */
	public String getType() {
		return get(CSVConfiguration.HEADERS.TYPE);
	}

}
