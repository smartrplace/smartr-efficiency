package org.smartrplace.smarteff.resourcecsv.util;

import java.util.Locale;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.TemperatureResource;
import org.smartrplace.smarteff.resourcecsv.row.ResourceCSVRow;
import org.smartrplace.smarteff.resourcecsv.row.SingleValueResourceCSVRow;

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
	
	/**
	 * TODO: Remove quote chars?
	 * @param in
	 * @return
	 */
	public static String getStringFromString(String in) {
		return in;
	}

	/**
	 * Get an appropriate row type for a resource.
	 * @param res
	 * @return
	 */
	public static ResourceCSVRow<? extends Resource> getRow(Resource res, Locale locale) {
		//TODO: Process lists
		if (res instanceof SingleValueResource)
			return new SingleValueResourceCSVRow((SingleValueResource) res, locale);
		return null;
	}
	
	public static ResourceCSVRow<? extends Resource> getHeaderRow(Class<? extends Resource> clazz) {
		return new SingleValueResourceCSVRow();
		/*if (SingleValueResource.class.isAssignableFrom(clazz))
			return new SingleValueResourceCSVRow();
		else if (BuildingUnit.class.isAssignableFrom(clazz))
			return new BuildingUnitCSVRow();
		else
			return new ResourceCSVRow<Resource>();*/
	}

}
