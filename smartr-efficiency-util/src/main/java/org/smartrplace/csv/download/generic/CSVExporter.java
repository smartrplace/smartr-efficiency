package org.smartrplace.csv.download.generic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.ogema.core.application.ApplicationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;


/**
 * Allows the export of device info from hardware installation to CSV.
 *
 */
public abstract class CSVExporter<T> {
	
	protected abstract List<T> getObjectsToExport(OgemaHttpRequest req);
	
	/** See example in comment at the end of the class*/
	protected abstract boolean printRow(T res, CSVPrinter p) throws IOException;
	protected abstract void printMainHeaderRow(CSVPrinter p) throws IOException;
	protected void printFinal(CSVPrinter p) throws IOException {};
	
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
	
	public int maxResourceCount = 9999;

	protected int resourceCount = 0;

	//protected final CSVConfigurationBase conf;
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final ApplicationManager appManExt;
	
	/** 
	public CSVExporter() {
		this(null, null);
	}*/
	
	/**
	 * 
	 * @param targetResource
	 * @param locale if null use English
	 * @param labels may be null
	 */
	public CSVExporter(Locale locale, ApplicationManager appManExt) {
		this.appManExt = appManExt;
		if(locale != null)
			this.locale = locale;
		//this.conf = new CSVConfigurationBase();
		//conf.initDefaults(targetResource);
		//this.conf.locale = locale;
	}
	
	/**
	 * Export a resource and its subresource to a CSV File.
	 * @return auto-generated file path
	 */
	public String exportToFile(OgemaHttpRequest req) {
		String filePath = genFilePath();
		try {
			File file = File.createTempFile(filePath, ".csv");
			log.info("Exporting to {}…", file.getAbsolutePath());
			exportToFile(file, req);
			log.info("Exported to {}", file.getAbsolutePath());
			return file.getAbsolutePath();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static String genFilePath() {
		return "devices.csv";
	}

	protected void exportToFile(File file, OgemaHttpRequest req) throws IOException {
		FileWriter out = new FileWriter(file);
		CSVPrinter p = new CSVPrinter(out, CSV_FORMAT);
		
		out.write(BOM);

		try {
			getAndExport(p, req);
		} finally {
			p.close();
			out.close();
		}
	}
	
	
	protected void getAndExport(CSVPrinter p, OgemaHttpRequest req) throws IOException {
		List<T> resources = getObjectsToExport(req);
		printMainHeaderRow(p);
		p.println();
		
		exportResources(p, resources);
		printFinal(p);
	}

	/**
	 * Export all resource of a type.
	 */
	private void exportResources(CSVPrinter p, List<T> resources) throws IOException {

		resourceCount += resources.size();
		if (resourceCount >= maxResourceCount) {
			throw new RuntimeException("Export failed: Maximum number of resources reached or exceeded.");
		}
		
		Iterator<T> iter = resources.iterator();
		while (iter.hasNext()) {
			T res = iter.next();
			printRow(res, p);
		}
	}


	/** Print one ore more rows that represent the respective resource type
	 * Get an appropriate row type for a resource.
	 * @param res
	 * @return true if resource and its value/children has been printed.
	 * @throws IOException 
	 */
	/*public boolean printRow(InstallAppDevice res, CSVPrinter p) throws IOException {
		List<String> toPrint = new ArrayList<>();
		toPrint.add(res.deviceId().getValue());
		Resource device = res.device().getLocationResource();
		Room room = null;
		if(device instanceof PhysicalElement) {
			room = ((PhysicalElement)device).location().room();
		}
		if(room != null && room.exists()) {
			toPrint.add(room.getLocation());
			toPrint.add(ResourceUtils.getHumanReadableShortName(room));
		} else {
			toPrint.add("");
			toPrint.add("");
		}
		toPrint.add(res.installationLocation().getValue());
		toPrint.add(""+res.installationStatus().getValue());
		toPrint.add(res.installationComment().getValue());
		toPrint.add(res.getName());
		toPrint.add(device.getLocation());
		toPrint.add(""+res.isTrash().getValue());
		
		p.printRecord(toPrint);
		return true;
	}*/
	
	/*public static void printMainHeaderRow(CSVPrinter p) throws IOException {
		List<String> toPrint = new ArrayList<>();
		toPrint.add("ID");
		toPrint.add("RoomLocation");
		toPrint.add("RoomName");
		toPrint.add("Location");
		toPrint.add("Status");
		toPrint.add("Comment");
		toPrint.add("IAD");
		toPrint.add("ResLoc");
		toPrint.add("isTrash");
		
		p.printRecord(toPrint);
	}*/
}
