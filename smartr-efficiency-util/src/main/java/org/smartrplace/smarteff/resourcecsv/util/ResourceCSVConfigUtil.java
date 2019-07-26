package org.smartrplace.smarteff.resourcecsv.util;

import org.smartrplace.smarteff.resourcecsv.CSVConfiguration;
import org.smartrplace.smarteff.resourcecsv.CSVConfiguration.ActiveStatus;
import org.smartrplace.smarteff.resourcecsv.CSVConfiguration.ExportReferences;

import de.iwes.util.resource.ValueResourceHelper;
import extensionmodel.smarteff.api.csvconfig.ResourceCSVConfig;

/**
 * Utilities for dealing with ResourceCSV configuration.
 * @author jruckel
 *
 */
public class ResourceCSVConfigUtil {
	
	public static CSVConfiguration resToObj(ResourceCSVConfig res) {
		CSVConfiguration conf = new CSVConfiguration();
		conf.activeStatus = ActiveStatus.values()[res.activeStatus().getValue()]; // TODO catch oob
		conf.exportReferences = ExportReferences.values()[res.exportReferences().getValue()]; // TODO catch oob
		conf.parent = res.parent();
		conf.root = res.root();
		// TODO print warning if formats differ
		return conf;
	}
	
	public static ResourceCSVConfig objToRes(CSVConfiguration conf, ResourceCSVConfig res) {
		if (!res.exists()) res.create();
		ValueResourceHelper.setCreate(res.activeStatus(), conf.activeStatus.toInt());
		ValueResourceHelper.setCreate(res.exportReferences(), conf.exportReferences.toInt());
		ValueResourceHelper.setCreate(res.format(), CSVConfiguration.CSV_FORMAT.toString());
		res.parent().create();
		res.parent().setAsReference(conf.parent);
		res.root().create();
		res.root().setAsReference(conf.root);
		res.activate(true);
		return res;
	}

}
