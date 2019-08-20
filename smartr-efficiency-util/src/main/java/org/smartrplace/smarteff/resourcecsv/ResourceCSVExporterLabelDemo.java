package org.smartrplace.smarteff.resourcecsv;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
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
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.extensionservice.gui.PageImplementationContext;
import org.smartrplace.smarteff.resourcecsv.util.ResourceCSVUtil;
import org.smartrplace.smarteff.resourcecsv.util.ResourceListCSVRows;
import org.smartrplace.smarteff.resourcecsv.util.ScheduleCSVRows;
import org.smartrplace.smarteff.resourcecsv.util.SingleValueResourceCSVRow;
import org.smartrplace.smarteff.resourcecsv.util.SmartEff2DMapCSVRows;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingUnit;


/**
 * Allows the export of resources to CSV. First, export any resources
 * that are to be represented in a two-dimensional table of their own
 * (e.g. BuildingUnit), then dump all remaining SingleValueResources.
 * @author jruckel
 *
 */
public class ResourceCSVExporterLabelDemo {
	
	protected int resourceCount = 0;

	protected final Locale locale;
	protected final Map<String, Map<OgemaLocale, String>> labels;
	protected final CSVConfiguration conf;
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * 
	 * @param targetResource
	 * @param locale if null use English
	 * @param labels may be null
	 */
	public ResourceCSVExporterLabelDemo(Resource targetResource, Locale locale, 
			ApplicationManagerSPExt appManExt) {
		
		Class<? extends Resource> sampleType = BuildingUnit.class;
		NavigationPublicPageData editPage = appManExt.getMaximumPriorityPageStatic(sampleType, PageType.EDIT_PAGE);
		PageImplementationContext ctx = editPage.getPageContextData();
		if(ctx == null) {
			throw new IllegalStateException("Edit page without context!");
		}
		this.labels = ctx.getLabels();
		
		this.locale = locale;
		if (targetResource == null) 
			throw new RuntimeException("Target resource may not be null.");
		this.conf = new CSVConfiguration();
		conf.initDefaults(targetResource, targetResource.getParent());
	}
	
}
