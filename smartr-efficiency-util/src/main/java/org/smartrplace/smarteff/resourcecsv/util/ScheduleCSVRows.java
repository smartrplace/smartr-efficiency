package org.smartrplace.smarteff.resourcecsv.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.smarteff.resourcecsv.CSVConfiguration;

public class ScheduleCSVRows extends SingleValueResourceCSVRow {
	
	//public final static String[] DEFAULT_EXPORT_FORMAT = {"yyyy-MM-dd'T'HH:mm:ss.SSSXXX"};
	/**
	 * Note: Leading zeroes will not be preserved by Excel/Calc and should be
	 * avoided.  A month exported with 'MM' correctly exports as '02' to the
	 * csv, but when opened and re-saved it'll become '2' and incompatible with
	 * 'MM'.
	 */
	public final static String[] DEFAULT_EXPORT_FORMAT = {"yyyy", "M", "d", "H", "m", "s.SSS"};
	public final static String EXPORT_FORMAT_PREFIX = "TS:";

	protected List<String> resourceRow = new ArrayList<>();;
	protected List<String> nameRow = new ArrayList<>();;
	protected List<String> versionRow = new ArrayList<>();;
	protected List<String> unitRow = new ArrayList<>();;
	protected final Schedule sched;
	protected final String label;
	
	public ScheduleCSVRows(Schedule sched, CSVConfiguration conf, String label) {
		this.sched = sched;
		this.label = label;
		this.conf = conf;
	}

	public List<List<String>> getRows() {
		ArrayList<List<String>> rows = new ArrayList<>();

		SingleValueResourceCSVRow header = new SingleValueResourceCSVRow(SingleValueResourceCSVRow.init.EMPTY);
		header.name = ResourceUtils.getHumanReadableShortName(sched.getParent());
		header.value = CSVConfiguration.HEADERS.SMARTEFFTIMESERIES;
		header.resource = sched.getParent().getName();
		header.path = getPath(sched);
		header.type = CSVConfiguration.HEADERS.SMARTEFFTIMESERIES;
		rows.add(header.values());
		
		
		// Header containing time format and resource names
		List<String> tsHeader = new ArrayList<>();
		List<SimpleDateFormat> dateFmts = new ArrayList<>();
		// Number of format columns
		int nFmt = 0;
		
		for (String fmt : DEFAULT_EXPORT_FORMAT) {
			tsHeader.add(EXPORT_FORMAT_PREFIX + fmt);
			dateFmts.add(new SimpleDateFormat(fmt));
			nFmt += 1;
		}
		tsHeader.add(header.resource); // TODO support export of multiple time series!
		
		List<String> versionHeader = new ArrayList<>();
		versionHeader.add(CSVConfiguration.HEADERS.VERSION);
		for (int i = 1; i < nFmt; i++)
			versionHeader.add("");
		versionHeader.add("1|");

		List<String> nameHeader = new ArrayList<>();
		nameHeader.add(CSVConfiguration.HEADERS.NAME);
		for (int i = 1; i < nFmt; i++)
			nameHeader.add("");
		nameHeader.add(header.name);

		rows.add(versionHeader);
		rows.add(nameHeader);
		rows.add(tsHeader);
		
		try {
			Iterator<SampledValue> iter = sched.iterator();
			while(iter.hasNext() ) {
				SampledValue val = iter.next();
				long timestamp = val.getTimestamp();
				Date date = new Date(timestamp);
				List<String> row = new ArrayList<>();
				for (SimpleDateFormat fmt : dateFmts) {
					row.add(fmt.format(date));
				}
				row.add(ResourceCSVUtil.format(conf.locale, val.getValue().getFloatValue())); // TODO export of multiple ts!
				rows.add(row);
			}
		} catch (Exception e) {
			// XXX
			System.out.println(e.getMessage());
		}
	
		
		return rows;
	}
}
