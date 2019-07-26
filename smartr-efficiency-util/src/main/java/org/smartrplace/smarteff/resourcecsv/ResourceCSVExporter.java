package org.smartrplace.smarteff.resourcecsv;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVPrinter;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.SingleValueResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.extensionservice.SmartEff2DMap;
import org.smartrplace.smarteff.resourcecsv.util.ResourceCSVUtil;
import org.smartrplace.smarteff.resourcecsv.row.ResourceCSVRow;

import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.api.csvconfig.ResourceCSVConfig;


/**
 * Allows the export of resources to CSV. First, export any resources
 * that are to be represented in a two-dimensional table of their own
 * (e.g. BuildingUnit), then dump all remaining SingleValueResources.
 * @author jruckel
 *
 */
public class ResourceCSVExporter extends CSVConfiguration {
	
	protected int resourceCount = 0;
	protected static final Class<?>[] SPECIAL_RESOURCE_TYPES = new Class[] {
			BuildingUnit.class,
			SmartEff2DMap.class
	};
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public ResourceCSVExporter(Resource targetResource) {
		if (targetResource == null) 
			throw new RuntimeException("Target resource may not be null.");
		initDefaults(targetResource, targetResource.getParent());
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
		CSVPrinter p = new CSVPrinter(out, CSV_FORMAT);
		
		out.write(BOM);

		try {
			List<? extends SingleValueResource> svr = parent.getSubResources(SingleValueResource.class, true);

			getAndExport(BuildingUnit.class, p, svr);
			getAndExport(SingleValueResource.class, p, null);

		} finally {
			p.close();
			out.close();
		}
	}
	
	
	protected <T extends Resource> void getAndExport(Class<T> clazz, CSVPrinter p,
			List<? extends SingleValueResource> svr) throws IOException {
		
		List<? extends Resource> resources = parent.getSubResources(clazz, true);
		exportResources(p, resources, svr);
	}

	/**
	 * Export all resource of a type.
	 */
	private <T extends Resource> void exportResources(CSVPrinter p, List<T> resources,
			List<? extends SingleValueResource> svr) throws IOException {

		resourceCount += resources.size();
		if (resourceCount >= maxResourceCount) {
			throw new RuntimeException("Export failed: Maximum number of resources reached or exceeded.");
		}
		
		p.printComment(resources.get(0).getClass().toString());
		
		@SuppressWarnings("unchecked")
		ResourceCSVRow<T> header = (ResourceCSVRow<T>) ResourceCSVUtil.getHeaderRow(resources.get(0).getClass());
		p.printRecord(header.values());
		
		Iterator<? extends Resource> iter = resources.iterator();
		while (iter.hasNext()) {
			Resource res = iter.next();
			if (exportUnknown || !res.isDecorator() || res.getParent() instanceof ResourceList) {
				@SuppressWarnings("unchecked")
				ResourceCSVRow<T> row = (ResourceCSVRow<T>) ResourceCSVUtil.getRow(res);
				p.printRecord(row.values());
			}
		}
		
	}


}
