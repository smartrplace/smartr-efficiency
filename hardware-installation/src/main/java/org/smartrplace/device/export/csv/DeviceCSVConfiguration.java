package org.smartrplace.device.export.csv;

import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.ogema.core.model.ResourceList;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

/**
 * Configuration for CSV Import / Export.
 * Will replace the need for configuration resources.
 * ! Note: Fields are most likely not relevant!
 *
 */
public class DeviceCSVConfiguration {
	
	public String ID;
	public String RoomLocation;
	public String RoomName;
	public String Location;
	public String Status;
	public String Comment;
	public String IAD;
	public String ResLoc;
	public String isTrash;

	/**
	 * CSV Format to use.
	 */
	public final static CSVFormat CSV_FORMAT = CSVFormat.RFC4180
			.withCommentMarker('#')
			.withDelimiter(';')
			.withIgnoreEmptyLines(false)
			.withFirstRecordAsHeader()
			.withAllowMissingColumnNames()
			;
	
	/**
	 * This UTF-8 byte order mark should be added to all files for Excel
	 * compatibility.
	 */
	public final static char BOM = '\uFEFF';
	
	/**
	 * Locale.
	 * TODO: Let exporter use this {@link #locale}.
	 */
	public Locale locale = Locale.GERMAN;
	public int maxResourceCount;
	public ResourceList<InstallAppDevice> parent;
	
	public static final class HEADERS {
		/* First row headers */
		public static final String ID = "ID";
		public static final String RoomLocation = "RoomLocation";
		public static final String RoomName = "RoomName";
		public static final String Location = "Location";
		public static final String Status = "Status";
		public static final String Comment = "Comment";
		public static final String IAD = "IAD";
		public static final String ResLoc = "ResLoc";
		public static final String isTrash = "isTrash";
		public static final String CONFIG = "CSV Configuration";
		
		public static final String[] REQUIRED = {
				ID, RoomLocation, RoomName, Location,
				Status, Comment, IAD, ResLoc, isTrash
		};
		public static final String AGG_LIST_SEP = ",";
	}

	public void initDefaults(ResourceList<InstallAppDevice> parent) {
		this.parent = parent;
		
		maxResourceCount = 30000;
	}

}
