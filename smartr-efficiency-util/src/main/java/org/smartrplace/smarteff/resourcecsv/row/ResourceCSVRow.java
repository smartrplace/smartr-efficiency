package org.smartrplace.smarteff.resourcecsv.row;

import java.util.Arrays;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.StringResource;

public class ResourceCSVRow<T extends Resource> {

	public Class<? extends Resource> getResourceType() {
		if (res != null)
			return res.getClass();
		else
			return null;
	}

	protected T res = null;

	protected String name;
	protected String path;
	protected String type;
	protected String value = "";
	
	/**
	 * Empty constructor initializes with labels for fields.
	 * Used for generating headers.
	 */
	public ResourceCSVRow() {
		this.name = "Name";
		this.path = "Path";
		this.type = "Type";
	}
	
	public ResourceCSVRow(T res) {
		this.path = res.getPath();
		this.type = res.getResourceType().getSimpleName();

		// If a "name" sub-resource exists, use it for the name.
		StringResource name = res.getSubResource("name", StringResource.class);
		String humanFriendlyName = null;
		if (name.exists()) {
			humanFriendlyName = name.getValue();
			if (! humanFriendlyName.isEmpty())
				this.name = humanFriendlyName;
		} 
		this.name = (humanFriendlyName == null || humanFriendlyName.isEmpty()) ? res.getName() : humanFriendlyName;
	}
	
	/** Returns columns as List */
	public List<String> values() {
		return Arrays.asList(getCols());
	}
	/**
	 * Returns values for columns in order.  Define the order here.
	 * @return
	 */
	public String[] getCols() {
		return new String[] {
			this.path, this.name, this.value, this.type
		};
	}

}
