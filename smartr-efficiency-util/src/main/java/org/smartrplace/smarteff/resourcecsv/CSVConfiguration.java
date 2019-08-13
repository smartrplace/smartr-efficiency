package org.smartrplace.smarteff.resourcecsv;

import org.apache.commons.csv.CSVFormat;
import org.ogema.core.model.Resource;

/**
 * Configuration for CSV Import / Export.
 * Will replace the need for configuration resources.
 * @author jruckel
 *
 */
public class CSVConfiguration {
	
	/** How the active status of resources is exported/imported/modified. */
	public enum ActiveStatus {
		/** Preserve the active status of all resources upon export/import. */
		PRESERVE(0),
		/** Don't export the active status / ignore active status upon import. */
		IGNORE(1),
		/** Set all exported/imported resources active after export/import */
		SET_ALL_ACTIVE(2),
		/** Set all exported/imported resources inactive after export/import */
		SET_ALL_INACTIVE(3),
		;
		private final int i;
		private ActiveStatus(int i) { this.i = i; }
		public int toInt() { return i; }
	}
	// public ActiveStatus activeStatus; TODO
	
	/** Settings for how references should be dealt with */
	public enum ExportReferences {
		/** Only resolve references within the exported sub-tree. */
		SUBTREE_ONLY(0),
		/** Resolve all references, including ones outside of the sub-tree. */
		RESOLVE_ALL(1),
		;
		private final int i;
		private ExportReferences(int i) { this.i = i; }
		public int toInt() { return i; }
	}
	// public ExportReferences exportReferences; TODO
	
	/** The resource to be exported, along with all of its sub-resources. */
	public Resource parent;
	
	/**
	 * This root node shall be a resource in the path above the parent resource.
	 * The paths exported are shortened by the path of the rootNode, e.g if the
	 * parent is userData1/buildings/MyHome and rootNode is userData1/buildings
	 * then all paths exported start with MyHome.  If this parameter is null,
	 * then the full paths shall be exported.
	 */
	public Resource root;
	
	/**
	 * Maximum number of resources to export or import before aborting the
	 * process.
	 */
	public int maxResourceCount;
	
	/*
	 * If set to false, (sub-)resources that are present in the resource tree
	 * but not in their parent resource's interface will not be exported.
	 */
	public boolean exportUnknown;
	
	/**
	 * CSV Format to use.
	 */
	public final static CSVFormat CSV_FORMAT = CSVFormat.RFC4180.withCommentMarker('#').withDelimiter(';');
	
	/**
	 * This UTF-8 byte order mark should be added to all files for Excel
	 * compatibility.
	 */
	public final static char BOM = '\uFEFF';
	
	public void initDefaults(Resource parent, Resource root) {
		this.parent = parent;
		this.root = root;
		
		//activeStatus = ActiveStatus.PRESERVE;
		//exportReferences = ExportReferences.SUBTREE_ONLY;
		maxResourceCount = 1000;
		exportUnknown = true;
	}

}
