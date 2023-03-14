package org.smartrplace.device.export.csv;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.csv.CSVPrinter;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;


/**
 * Allows the export of device info from hardware installation to CSV.
 *
 */
public class DeviceTableCSVExporter {
	
	protected int resourceCount = 0;

	public final DeviceCSVConfiguration conf;
	protected List<InstallAppDevice> shown = null;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final ApplicationManager appManExt;
	
	public DeviceTableCSVExporter(ResourceList<InstallAppDevice> targetResource) {
		this(targetResource, null, null);
	}
	
	/**
	 * 
	 * @param targetResource
	 * @param locale if null use English
	 * @param labels may be null
	 */
	public DeviceTableCSVExporter(ResourceList<InstallAppDevice> targetResource, Locale locale, ApplicationManager appManExt) {
		if (targetResource == null) 
			throw new RuntimeException("Target resource may not be null.");
		this.appManExt = appManExt;
		this.conf = new DeviceCSVConfiguration();
		conf.initDefaults(targetResource);
		this.conf.locale = locale;
	}
	
	/**
	 * Export a resource and its subresource to a CSV File.
	 * @return auto-generated file path
	 */
	public String exportToFile() {
		String filePath = genFilePath();
		try {
			File file = File.createTempFile(filePath, ".csv");
			log.info("Exporting to {}…", file.getAbsolutePath());
			exportToFile(file);
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

	protected void exportToFile(File file) throws IOException {
		FileWriter out = new FileWriter(file);
		CSVPrinter p = new CSVPrinter(out, DeviceCSVConfiguration.CSV_FORMAT);
		
		out.write(DeviceCSVConfiguration.BOM);

		try {
			getAndExport(p);
		} finally {
			p.close();
			out.close();
		}
	}
	
	
	protected <T extends Resource> void getAndExport(CSVPrinter p) throws IOException {
		List<InstallAppDevice> resources;
		if(shown == null)
			resources = conf.parent.getAllElements();
		else
			resources = shown;
		printMainHeaderRow(p);
		p.println();
		
		resources.sort(new Comparator<InstallAppDevice>() {

			@Override
			public int compare(InstallAppDevice o1, InstallAppDevice o2) {
				return o1.deviceId().getValue().compareTo(o2.deviceId().getValue());
			}
		});
		
		exportResources(p, resources);
	}

	/**
	 * Export all resource of a type.
	 */
	private void exportResources(CSVPrinter p, List<InstallAppDevice> resources) throws IOException {

		resourceCount += resources.size();
		if (resourceCount >= conf.maxResourceCount) {
			throw new RuntimeException("Export failed: Maximum number of resources reached or exceeded.");
		}
		
		Iterator<InstallAppDevice> iter = resources.iterator();
		while (iter.hasNext()) {
			InstallAppDevice res = iter.next();
			printRow(res, p);
		}
	}


	/** Print one ore more rows that represent the respective resource type
	 * Get an appropriate row type for a resource.
	 * @param res
	 * @return true if resource and its value/children has been printed.
	 * @throws IOException 
	 */
	public boolean printRow(InstallAppDevice res, CSVPrinter p) throws IOException {
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
	}
	
	public static void printMainHeaderRow(CSVPrinter p) throws IOException {
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
	}

	public void setResources(List<InstallAppDevice> shown) {
		this.shown = shown;
		
	}
}
