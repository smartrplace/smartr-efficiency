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

public class ScheduleCSVRows extends SingleValueResourceCSVRow {
	
	public final static String[] DEFAULT_EXPORT_FORMAT = {"yyyy-MM-dd'T'HH:mm:ss.SSSX"};
	public final static String EXPORT_FORMAT_PREFIX = "TS:";

	protected List<String> resourceRow = new ArrayList<>();;
	protected List<String> nameRow = new ArrayList<>();;
	protected List<String> versionRow = new ArrayList<>();;
	protected List<String> unitRow = new ArrayList<>();;
	protected final Schedule sched;
	
	public ScheduleCSVRows(Schedule sched, boolean exportUnknown) {
		this.sched = sched;
	}

	public List<List<String>> getRows(Locale locale) {
		ArrayList<List<String>> rows = new ArrayList<>();

		SingleValueResourceCSVRow header = new SingleValueResourceCSVRow(SingleValueResourceCSVRow.init.EMPTY);
		header.name = ResourceUtils.getHumanReadableShortName(sched.getParent());
		header.value = "SmartEffTimeSeries";
		header.resource = sched.getName();
		header.path = sched.getPath();
		//header.elementType = resList.getElementType().getSimpleName();
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
		versionHeader.add("Version:");
		for (int i = 1; i < nFmt; i++)
			versionHeader.add("");
		versionHeader.add("1|");

		List<String> nameHeader = new ArrayList<>();
		nameHeader.add("Name:");
		for (int i = 1; i < nFmt; i++)
			nameHeader.add("");
		nameHeader.add(header.name);

		rows.add(versionHeader);
		rows.add(nameHeader);
		rows.add(tsHeader);

		Iterator<SampledValue> iter = sched.iterator();
		while(iter.hasNext() ) {
			SampledValue val = iter.next();
			long timestamp = val.getTimestamp();
			Date date = new Date(timestamp);
			List<String> row = new ArrayList<>();
			for (SimpleDateFormat fmt : dateFmts) {
				row.add(fmt.format(date));
			}
			row.add(ResourceCSVUtil.format(locale, val.getValue().getFloatValue())); // TODO export of multiple ts!
			rows.add(row);
		}
		
		return rows;
	}
}
