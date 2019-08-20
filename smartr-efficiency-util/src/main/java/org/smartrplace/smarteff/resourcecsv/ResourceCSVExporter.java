package org.smartrplace.smarteff.resourcecsv;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.csv.CSVPrinter;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.SmartEff2DMap;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.extensionservice.gui.PageImplementationContext;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.smarteff.resourcecsv.util.ResourceCSVUtil;
import org.smartrplace.smarteff.resourcecsv.util.ResourceListCSVRows;
import org.smartrplace.smarteff.resourcecsv.util.ScheduleCSVRows;
import org.smartrplace.smarteff.resourcecsv.util.SingleValueResourceCSVRow;
import org.smartrplace.smarteff.resourcecsv.util.SmartEff2DMapCSVRows;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;


/**
 * Allows the export of resources to CSV. First, export any resources
 * that are to be represented in a two-dimensional table of their own
 * (e.g. BuildingUnit), then dump all remaining SingleValueResources.
 * @author jruckel
 *
 */
public class ResourceCSVExporter {
	
	protected int resourceCount = 0;

	protected final Map<String, Map<OgemaLocale, String>> labels;
	protected final CSVConfiguration conf;
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final ApplicationManagerSPExt appManExt;
	
	public ResourceCSVExporter(Resource targetResource) {
		this(targetResource, null, null);
	}
	/**
	 * 
	 * @param targetResource
	 * @param locale if null use English
	 * @param labels may be null
	 */
	public ResourceCSVExporter(Resource targetResource, Locale locale, ApplicationManagerSPExt appManExt) {
		if (targetResource == null) 
			throw new RuntimeException("Target resource may not be null.");
		this.appManExt = appManExt;
		this.conf = new CSVConfiguration();
		conf.initDefaults(targetResource, targetResource.getParent());
		this.conf.locale = locale;
		this.labels = getLabels(targetResource.getResourceType());
	}
	
	protected Map<String, Map<OgemaLocale, String>> getLabels(Class<? extends Resource> type) {
		if(appManExt == null) {
			return null;
		}
		NavigationPublicPageData editPage = appManExt.getMaximumPriorityPageStatic(
				type, PageType.EDIT_PAGE);
		if(editPage == null) {
			return null;
		}
		PageImplementationContext ctx = editPage.getPageContextData();
		if(ctx == null) {
			throw new IllegalStateException("Edit page without context!");
		}
		return ctx.getLabels();
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
		printConfig(p);
		p.println();
		p.printComment(ResourceUtils.getHumanReadableShortName(conf.parent));
		exportResources(p, resources);

	}

	private void printConfig(CSVPrinter p) throws IOException {
		p.println();
		p.printRecords(CSVConfiguration.HEADERS.CONFIG);
		for (Field f : conf.getClass().getFields()) {
			try {
				Object val = f.get(conf);
				if (val != null) {
					String strVal = null;
					if (val instanceof String[])
						strVal = String.join(",", (String[]) val);
					else
						strVal = val.toString();
					if (strVal != null && !strVal.isEmpty())
						p.printRecord(f.getName(), strVal);
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		p.println();
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
			if (conf.exportImportUnknown || !res.isDecorator() /*|| res.getParent() instanceof ResourceList*/) {
				boolean fullyPrinted = printRows(res, p);
				if (!fullyPrinted) {
					List<Resource> subResources = res.getSubResources(false);
					p.println();
					p.printComment(getLabel(res));
					exportResources(p, subResources);
				}
			}
		}
		p.println();
	}


	/** Print one ore more rows that represent the respective resource type
	 * Get an appropriate row type for a resource.
	 * @param res
	 * @return true if resource and its value/children has been printed.
	 * @throws IOException 
	 */
	public boolean printRows(Resource res, CSVPrinter p) throws IOException {
		//TODO: Process lists and special data
		String name = ResourceUtils.getHumanReadableName(res);
		String label = getLabel(res);
		if (res instanceof SingleValueResource) {
			SingleValueResourceCSVRow row = new SingleValueResourceCSVRow((SingleValueResource) res, conf, label);
			p.printRecord(row.values());
			return true;
		} else if(res instanceof ResourceList) {
			p.println();
			p.printComment("List: " + name);
			@SuppressWarnings({ "rawtypes", "unchecked" }) // XXX
			ResourceListCSVRows<?> rows = new ResourceListCSVRows((ResourceList<?>) res, conf, label);
			List<List<String>> r = rows.getRows();
			for (List<String> row : r) {
				p.printRecord(row);
			}
			p.println();
			return true;
		} else if(res instanceof SmartEff2DMap) {
			p.println();
			p.printComment("2DMap: " + name);
			SmartEff2DMapCSVRows rows = new SmartEff2DMapCSVRows((SmartEff2DMap) res, conf, label);
			List<List<String>> r = rows.getRows();
			for (List<String> row : r) {
				p.printRecord(row);
			}
			p.println();
			return true;
		} else if(res instanceof SmartEffTimeSeries) {
			p.println();
			p.printComment("TS: " + name);
			SmartEffTimeSeries ts = (SmartEffTimeSeries) res;
			if (!ts.schedule().exists()) {
				p.printComment("Time series could not be exported because only schedule-based SmartEffTimeSeries are"
						+ " currently supported.");
			} else {
				ScheduleCSVRows rows = new ScheduleCSVRows(ts.schedule(), conf, label);
				List<List<String>> r = rows.getRows();
				for (List<String> row : r) {
					p.printRecord(row);
				}
			}
			p.println();
			return true;
		} else {
			SingleValueResourceCSVRow row = new SingleValueResourceCSVRow(res, conf, label);
			p.printRecord(row.values());
			return false;
		}
		//return false;
	}
	
	/**
	 * Get a label/description for a resource.
	 * @param res
	 * @return the label defined on the edit page (preferably in the current
	 * locale), the value of a <code>name</code> subresource or, if none of
	 * these are available, <code>getName()</code> of the resource.
	 */
	private String getLabel(Resource res) {
		String path = res.getName();
		if (labels != null && labels.containsKey(path)) {
			Map<OgemaLocale, String> resLabel = labels.get(path);
			OgemaLocale l = new OgemaLocale(conf.locale);
			if (resLabel.containsKey(l)) {
				return resLabel.get(l);
			} else if (resLabel.containsKey(OgemaLocale.ENGLISH)) {
				return resLabel.get(OgemaLocale.ENGLISH);
			}
		}
		return ResourceUtils.getHumanReadableShortName(res);
	}
}
