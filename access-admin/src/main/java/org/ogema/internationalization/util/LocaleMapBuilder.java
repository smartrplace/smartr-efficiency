package org.ogema.internationalization.util;

import java.util.HashMap;
import java.util.Map;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class LocaleMapBuilder {
	protected final Map<OgemaLocale, String> labels;
	
	public LocaleMapBuilder() {
		this.labels = new HashMap<>();
	}
	public LocaleMapBuilder(Map<OgemaLocale, String> mapToFill) {
		this.labels = mapToFill;
	}
	
	public LocaleMapBuilder addEnglish(String label) {
		labels.put(OgemaLocale.ENGLISH, label);
		return this;
	}
	public LocaleMapBuilder addGerman(String label) {
		labels.put(OgemaLocale.GERMAN, label);
		return this;
	}
	public LocaleMapBuilder addFrench(String label) {
		labels.put(OgemaLocale.FRENCH, label);
		return this;
	}
	public LocaleMapBuilder addCN(String label) {
		labels.put(OgemaLocale.CHINESE, label);
		return this;
	}
	
	public LocaleMapBuilder setDefault(String englishLabel) {
		labels.put(OgemaLocale.ENGLISH, englishLabel);
		return this;
	}
	public LocaleMapBuilder setDefault(String englishLabel, String germanLabel) {
		setDefault(englishLabel);
		labels.put(OgemaLocale.GERMAN, germanLabel);
		return this;
	}
	public LocaleMapBuilder setDefault(String englishLabel, String germanLabel, String frenchLabel) {
		setDefault(englishLabel, germanLabel);
		labels.put(OgemaLocale.FRENCH, frenchLabel);
		return this;
	}
	public LocaleMapBuilder setDefault(String englishLabel, String germanLabel, String frenchLabel,
			String chineseLabel) {
		setDefault(englishLabel, germanLabel, frenchLabel);
		labels.put(OgemaLocale.CHINESE, chineseLabel);
		return this;
	}
	
	public Map<OgemaLocale, String> getMap() {
		return labels;
	}
}
