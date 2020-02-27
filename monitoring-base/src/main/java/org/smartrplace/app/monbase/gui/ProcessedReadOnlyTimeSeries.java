package org.smartrplace.app.monbase.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;

public abstract class ProcessedReadOnlyTimeSeries implements ReadOnlyTimeSeries {
	protected abstract List<SampledValue> updateValues(long start, long end);

	protected List<SampledValue> values = null;
	protected long knownStart = -1;
	protected long knownEnd;
	protected boolean isOwnList = false;
	
	protected final InterpolationMode interpolationMode;
	
	public ProcessedReadOnlyTimeSeries(InterpolationMode interpolationMode) {
		this.interpolationMode = interpolationMode;
	}

	@Override
	public List<SampledValue> getValues(long startTime, long endTime) {
		if(knownStart < 0) {
			values = updateValues(startTime, endTime);
			knownStart = startTime;
			knownEnd = endTime;
		} else if(startTime < knownStart && endTime > knownEnd) {
			values = updateValues(startTime, endTime);
			knownStart = startTime;
			knownEnd = endTime;			
		} else if(startTime < knownStart) {
			List<SampledValue> newVals = updateValues(startTime, knownStart);
			List<SampledValue> concat = new ArrayList<SampledValue>(newVals);
			concat.addAll(values);
			values = concat;
			isOwnList = true;
			knownStart = startTime;			
		} else if(endTime > knownEnd) {
			List<SampledValue> newVals = updateValues(knownEnd, endTime);
			if(isOwnList)
				values.addAll(newVals);
			else {
				List<SampledValue> concat = new ArrayList<SampledValue>(values);
				concat.addAll(newVals);
				values = concat;
				isOwnList = true;
			}
			knownEnd = endTime;			
		}
		return values;
	}

	@Override
	public List<SampledValue> getValues(long startTime) {
		return getValues(startTime, Long.MAX_VALUE);
	}

	@Override
	public SampledValue getValue(long time) {
		if(interpolationMode != InterpolationMode.NONE)
			throw new UnsupportedOperationException("Interpolation only for NONE supported yet!");
		List<SampledValue> asList = getValues(time, time);
		if(asList.isEmpty())
			return null;
		else
			return asList.get(0);
	}

	@Override
	public SampledValue getNextValue(long time) {
		List<SampledValue> asList = getValues(time, Long.MAX_VALUE);
		if(asList.isEmpty())
			return null;
		else
			return asList.get(0);
	}

	@Override
	public SampledValue getPreviousValue(long time) {
		List<SampledValue> asList = getValues(0, time);
		if(asList.isEmpty())
			return null;
		else
			return asList.get(asList.size()-1);
	}

	@Override
	public InterpolationMode getInterpolationMode() {
		return interpolationMode;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean isEmpty(long startTime, long endTime) {
		return getValues(startTime, endTime).isEmpty();
	}

	@Override
	public int size() {
		return getValues(0, Long.MAX_VALUE).size();
	}

	@Override
	public int size(long startTime, long endTime) {
		return getValues(startTime, endTime).size();
	}

	@Override
	public Iterator<SampledValue> iterator() {
		return getValues(0, Long.MAX_VALUE).listIterator();
	}

	@Override
	public Iterator<SampledValue> iterator(long startTime, long endTime) {
		return getValues(startTime, endTime).listIterator();
	}

	@Override
	public Long getTimeOfLatestEntry() {
		return getPreviousValue(Long.MAX_VALUE).getTimestamp();
	}
	
}
