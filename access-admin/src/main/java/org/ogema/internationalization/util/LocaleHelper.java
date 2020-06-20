package org.ogema.internationalization.util;

import java.util.Collections;
import java.util.Map;

import org.smartrplace.gui.filtering.GenericFilterOption;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class LocaleHelper {
	public static Map<OgemaLocale, String> getLabelMap(String englishLabel) {
		return new LocaleMapBuilder().setDefault(englishLabel).getMap();
	}
	public static Map<OgemaLocale, String> getLabelMap(String englishLabel, String germanLabel) {
		return new LocaleMapBuilder().setDefault(englishLabel, germanLabel).getMap();
	}
	public static Map<OgemaLocale, String> getLabelMap(String englishLabel, String germanLabel, String frenchLabel) {
		return new LocaleMapBuilder().setDefault(englishLabel, germanLabel, frenchLabel).getMap();
	}
	public static Map<OgemaLocale, String> getLabelMap(String englishLabel, String germanLabel, String frenchLabel,
			String chineseLabel) {
		return new LocaleMapBuilder().setDefault(englishLabel, germanLabel, frenchLabel, chineseLabel).getMap();
	}
	public static Map<OgemaLocale, String> getLabelMap(String[] defaultorderedlocales) {
		if(defaultorderedlocales.length == 1)
			return getLabelMap(defaultorderedlocales[0]);
		if(defaultorderedlocales.length == 2)
			return getLabelMap(defaultorderedlocales[0], defaultorderedlocales[1]);
		if(defaultorderedlocales.length == 3)
			return getLabelMap(defaultorderedlocales[0], defaultorderedlocales[1], defaultorderedlocales[2]);
		if(defaultorderedlocales.length >= 4)
			return getLabelMap(defaultorderedlocales[0], defaultorderedlocales[1], defaultorderedlocales[2], defaultorderedlocales[3]);
		return Collections.emptyMap();
	}
	
	public static String getLabel(Map<OgemaLocale, String> labelMap, OgemaLocale locale) {
		if(locale == null)
			locale = OgemaLocale.ENGLISH;
		String result = labelMap.get(locale);
		if(result != null) {
			return result;
		} else if(locale != null && locale != OgemaLocale.ENGLISH) {
			result = labelMap.get(OgemaLocale.ENGLISH);
		}
		return result;
	}
}
