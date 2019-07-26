package org.smartrplace.smarteff.resourcecsv;

import org.ogema.core.model.Resource;

/**
 * Allows the import of resources via CSV
 * @author jruckel
 *
 */
public class ResourceCSVImporter {
	/**
	 * Extract data from a CSV file at filePaht and place it into targetParentResource
	 * @param filePath
	 * @param targetParentResource
	 * @return
	 */
	public Resource importFromFile(String filePath, Resource targetParentResource) {
		targetParentResource.activate(true);
		return targetParentResource;
	}
}
