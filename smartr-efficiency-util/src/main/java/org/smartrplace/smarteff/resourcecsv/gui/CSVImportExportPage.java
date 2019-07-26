package org.smartrplace.smarteff.resourcecsv.gui;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

/**
 * Page to control and initialize importing/exporting resources.
 * @author jruckel
 *
 */
public class CSVImportExportPage<T extends Resource> {

	/**
	 * Add a button to the page that allows the user to create/modify CSV
	 * configuration as well as initiate the import/export of resources from/to
	 * CSV.  Mimics the functions found in {@link #SPPageUtil}.
	 * @param label 
	 * @param parent
	 * @param vh 
	 * @param id 
	 * @param row 
	 * @param appData 
	 * @param req 
	 * @return the added button?
	 */
	public static <T extends Resource> OgemaWidget addOpenButton(String label, T parent,
			ResourceGUIHelper<T> vh, String id, Row row,
			ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {

		return null;
	}

}
