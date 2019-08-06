package org.smartrplace.smarteff.resourcecsv;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.csv.CSVPrinter;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.extensionservice.SmartEff2DMap;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.smarteff.resourcecsv.util.ResourceCSVUtil;
import org.smartrplace.smarteff.resourcecsv.util.ResourceListCSVRows;
import org.smartrplace.smarteff.resourcecsv.util.SingleValueResourceCSVRow;


/**
 * Allows the export of resources to CSV. First, export any resources
 * that are to be represented in a two-dimensional table of their own
 * (e.g. BuildingUnit), then dump all remaining SingleValueResources.
 * @author jruckel
 *
 */
public class ResourceCSVExporter {
	
	protected int resourceCount = 0;

	protected final Locale locale;
	protected final CSVConfiguration conf;
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public ResourceCSVExporter(Resource targetResource) {
		this(targetResource, null);
	}
	public ResourceCSVExporter(Resource targetResource, Locale locale) {
		this.locale = locale;
		if (targetResource == null) 
			throw new RuntimeException("Target resource may not be null.");
		this.conf = new CSVConfiguration();
		conf.initDefaults(targetResource, targetResource.getParent());
	}
	
	/**
	 * Export a resource and its subresource to a CSV File.
	 * @return auto-generated file path
	 */
	public String exportToFile() {
		String filePath = ResourceCSVUtil.genFilePath();
		try {
			File file = File.createTempFile(filePath, ".csv");
			log.info("Exporting to {}…", file.getAbsolutePath());
			exportToFile(file);
			log.info("Exported to {}", file.getAbsolutePath());
			return file.getAbsolutePath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	protected void exportToFile(File file) throws IOException {
		FileWriter out = new FileWriter(file);
		CSVPrinter p = new CSVPrinter(out, CSVConfiguration.CSV_FORMAT);
		
		out.write(CSVConfiguration.BOM);

		try {
			getAndExport(p);
		} finally {
			p.close();
			out.close();
		}
	}
	
	
	protected <T extends Resource> void getAndExport(CSVPrinter p) throws IOException {
		List<? extends Resource> resources = conf.parent.getSubResources(false);
		ResourceCSVUtil.printMainHeaderRow(p);
		p.printComment(ResourceUtils.getHumanReadableShortName(conf.parent));
		exportResources(p, resources);
		p.printComment("TODO: Metadata, e.g. configuration"); // TODO
	}

	/**
	 * Export all resource of a type.
	 */
	private <T extends Resource> void exportResources(CSVPrinter p, List<T> resources) throws IOException {

		resourceCount += resources.size();
		if (resourceCount >= conf.maxResourceCount) {
			throw new RuntimeException("Export failed: Maximum number of resources reached or exceeded.");
		}
		
		Iterator<? extends Resource> iter = resources.iterator();
		while (iter.hasNext()) {
			Resource res = iter.next();
			if (conf.exportUnknown || !res.isDecorator() /*|| res.getParent() instanceof ResourceList*/) {
				boolean printed = printRows(res, p);
				if (!printed) {
					List<Resource> subResources = res.getSubResources(false);
					p.println();
					p.printComment(ResourceUtils.getHumanReadableShortName(res));
					exportResources(p, subResources);
				}
			}
		}
		p.println();
	}


	/** Print one ore more rows that represent the respective resource type
	 * Get an appropriate row type for a resource.
	 * @param res
	 * @return
	 * @throws IOException 
	 */
	public boolean printRows(Resource res, CSVPrinter p) throws IOException {
		//TODO: Process lists and special data
		String name = ResourceUtils.getHumanReadableName(res);
		if (res instanceof SingleValueResource) {
			SingleValueResourceCSVRow row = new SingleValueResourceCSVRow((SingleValueResource) res, locale);
			p.printRecord(row.values());
			return true;
		} else if(res instanceof ResourceList) {
			p.println();
			p.printComment("List: " + name);
			@SuppressWarnings({ "rawtypes", "unchecked" }) // XXX
			ResourceListCSVRows<?> rows = new ResourceListCSVRows((ResourceList<?>) res, conf.exportUnknown);
			List<List<String>> r = rows.getRows(locale);
			for (List<String> row : r) {
				p.printRecord(row);
			}
			return true;
		} else if(res instanceof SmartEff2DMap) {
			p.println();
			p.printComment("2DMap: " + name);
			//TODO
			p.println();
			return true;
		} else if(res instanceof SmartEffTimeSeries) {
			p.println();
			p.printComment("TS: " + name);
			//TODO
			p.println();
			return true;
		}
		return false;
	}
}
