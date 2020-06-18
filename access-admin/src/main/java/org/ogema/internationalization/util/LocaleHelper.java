package org.ogema.internationalization.util;

import java.util.Map;

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
}
