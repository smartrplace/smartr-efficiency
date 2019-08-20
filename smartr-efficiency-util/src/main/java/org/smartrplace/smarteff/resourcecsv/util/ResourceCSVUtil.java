package org.smartrplace.smarteff.resourcecsv.util;

import java.io.IOException;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.apache.commons.csv.CSVPrinter;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.PhysicalUnitResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.persistence.DBConstants;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.smarteff.resourcecsv.CSVConfiguration;

/**
 * General utility for ResourceCSV
 * @author jruckel
 *
 */
public class ResourceCSVUtil {
	public static String genFilePath() {
		return "resources.csv";
	}

	/**
	 * Returns the value of a SingleValueResource as a String.
	 * For a simpler version see {@link ValueResourceUtils#getValue(SingleValueResource)}
	 * @return Value as string.  Null if value could not be retrieved.
	 */
	public static String getValueAsString(SingleValueResource res, Locale locale) {
		if (res instanceof StringResource) {
			return ((StringResource) res).getValue();
		} else if (res instanceof TemperatureResource) {
			if(locale != null) return String.format(locale, "%.2f", ((TemperatureResource) res).getCelsius());
			else return String.format("%.2f", ((TemperatureResource) res).getCelsius());
		} else if (res instanceof FloatResource) {
			if(locale != null) return String.format(locale, "%.3f", ((FloatResource) res).getValue());
			else return String.format("%.3f", ((FloatResource) res).getValue());
			//return Float.toString(((FloatResource) res).getValue());
		} else if (res instanceof IntegerResource) {
			return Integer.toString(((IntegerResource) res).getValue());
		} else if (res instanceof BooleanResource) {
			return Boolean.toString(((BooleanResource) res).getValue());
		} else {
			return null;
		}
	}

	
	public static void printMainHeaderRow(CSVPrinter p) throws IOException {
		SingleValueResourceCSVRow header = new SingleValueResourceCSVRow();
		p.printRecord(header.values());
		/*if (SingleValueResource.class.isAssignableFrom(clazz))
			return new SingleValueResourceCSVRow();
		else if (BuildingUnit.class.isAssignableFrom(clazz))
			return new BuildingUnitCSVRow();
		else
			return new ResourceCSVRow<Resource>();*/
	}

	public static String getUnit(Resource res) {
		if (res instanceof TemperatureResource)
			return "°C";
		else if (res instanceof PhysicalUnitResource)
			return ((PhysicalUnitResource) res).getUnit().toString();
		return "";
	}

	public static String format(Locale locale, float f) {
		return String.format(locale, "%.3f", f);
	}
	
	public static String unFormat(Locale locale, String s) {
		if (s == null) return null;
		if (s.isEmpty()) return "";
		DecimalFormatSymbols d = new DecimalFormatSymbols(locale);
		String grouping = Character.toString(d.getGroupingSeparator());
		char decimal = d.getDecimalSeparator();
		return s.replace(grouping, "").replace(decimal, '.');
	}

	public static String getRelativePath(Resource res, CSVConfiguration conf) {
		return getRelativePath(res, conf.root);
	}
	
	public static String getRelativePath(Resource res, Resource relativeTo) {
		return getRelativePath(res.getPath(), relativeTo.getPath());
	}
	
	public static String getRelativePath(String resPath, String relativeToPath) {
		return resPath.replaceFirst("^" + relativeToPath + DBConstants.PATH_SEPARATOR, "");
	}
	
	public static String getRelativeLocation(Resource res, CSVConfiguration conf) {
		return getRelativePath(res.getLocation(), conf.root.getLocation());
	}

	public static Float parseFloat(Locale locale, String val) {
		if (val == null) return null;
		try {
			return Float.parseFloat(unFormat(locale, val));
		} catch (NumberFormatException e) {
			return Float.NaN;
		}
	}

}
