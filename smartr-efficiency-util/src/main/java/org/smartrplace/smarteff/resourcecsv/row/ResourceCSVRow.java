package org.smartrplace.smarteff.resourcecsv.row;

import java.util.Arrays;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;

@Deprecated //moved into SingleValueResourceCSVRow
public abstract class ResourceCSVRow<T extends Resource> {

	public Class<? extends Resource> getResourceType() {
		if (res != null)
			return res.getClass();
		else
			return null;
	}

	protected T res = null;

	final protected String name;
	final protected String path;
	final protected String type;
	final protected String resource;
	protected String value;
	
	/**
	 * Empty constructor initializes with labels for fields.
	 * Used for generating headers.
	 */
	public ResourceCSVRow() {
		this.name = "Name";
		this.path = "Path";
		this.type = "Type";
		this.resource = "Resource";
		this.value = "Value";
	}
	
	public ResourceCSVRow(T res) {
		this.path = res.getPath();
		this.type = res.getResourceType().getSimpleName();

		// If a "name" sub-resource exists, use it for the name.
		this.name = ResourceUtils.getHumanReadableShortName(res);
		this.resource = res.getName();
		this.value = "";
	}
	
	/** Returns columns as List */
	public List<String> values() {
		return Arrays.asList(getCols());
	}
	/**
	 * Returns values for columns in order.  Define the order here.
	 * @return
	 */
	public abstract String[] getCols();
	/*{
		return new String[] {
			this.path, this.name, this.value, this.type
		};
	}*/

}
