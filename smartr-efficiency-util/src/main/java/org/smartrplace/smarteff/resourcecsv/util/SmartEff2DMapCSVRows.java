package org.smartrplace.smarteff.resourcecsv.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.smartrplace.extensionservice.SmartEff2DMap;
import org.smartrplace.extensionservice.SmartEffMapHelper;
import org.smartrplace.extensionservice.SmartEffMapHelper.Keys;
import org.smartrplace.smarteff.resourcecsv.CSVConfiguration;

public class SmartEff2DMapCSVRows extends SingleValueResourceCSVRow {

	protected List<String> resourceRow = new ArrayList<>();;
	protected List<String> nameRow = new ArrayList<>();;
	protected List<String> versionRow = new ArrayList<>();;
	protected List<String> unitRow = new ArrayList<>();;
	protected final SmartEff2DMap map;
	protected final String label;
	
	public SmartEff2DMapCSVRows(SmartEff2DMap map, CSVConfiguration conf, String label) {
		this.map = map;
		this.label = label;
		this.conf = conf;
	}

	public List<List<String>> getRows() {
		ArrayList<List<String>> rows = new ArrayList<>();

		SingleValueResourceCSVRow header = new SingleValueResourceCSVRow(SingleValueResourceCSVRow.init.EMPTY);
		header.name = label;
		header.value = CSVConfiguration.HEADERS.SMARTEFF2DMAP;
		header.resource = map.getName();
		header.path = getPath(map);
		header.type = CSVConfiguration.HEADERS.SMARTEFF2DMAP;
		//header.elementType = resList.getElementType().getSimpleName();
		rows.add(header.values());
		
		Keys keys = SmartEffMapHelper.getKeys(map);
		
		rows.add(Arrays.asList(new String[] {keys.yLabel, keys.xLabel}));
		
		List<String> primKeyRow = new ArrayList<>();
		primKeyRow.add(CSVConfiguration.HEADERS.DATA);
		for(float primKey : keys.x) {
			primKeyRow.add(ResourceCSVUtil.format(conf.locale, primKey));
		}
		rows.add(primKeyRow);

		for(float secKey : keys.y) {
			List<String> row = new ArrayList<>();
			row.add(ResourceCSVUtil.format(conf.locale, secKey));
			for(float primKey : keys.x) {
				float val = SmartEffMapHelper.getValue(primKey, secKey, map);
				row.add(ResourceCSVUtil.format(conf.locale, val));
			}
			rows.add(row);
		}
		
		return rows;
	}
}
