/**
 * 
 */
package org.smartrplace.smarteff.resourcecsv.row;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.SingleValueResource;

/**
 * TODO DOCME
 * @author jruckel
 * @param <T>
 *
 */
public class ResourceListCSVRow extends ResourceCSVRow<ResourceList<? extends SingleValueResource>> {
	
	public ResourceListCSVRow() {
		super();
		value = "Value";
	}
	
	@Override
	public Class<? extends Resource> getResourceType() {
		return ResourceList.class;
	}
	
	@Override
	public String[] getCols() {
		String[] cols = new String[] {
				this.path, this.name, this.value
		};
		return cols;
	}
}
