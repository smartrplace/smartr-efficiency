package org.ogema.timeseries.eval.simple.mon3;

import org.ogema.timeseries.eval.simple.api.TimeseriesSetProcessor3;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSimpleProcUtilBase;

public abstract class TimeseriesSetProcessor3Base implements TimeseriesSetProcessor3 {
	protected final Integer absoluteTimingIntern;
	protected final long minIntervalForReCalcIntern;
	
	public TimeseriesSetProcessor3Base(Integer absoluteTiming, long minIntervalForReCalc) {
		this.absoluteTimingIntern = absoluteTiming;
		this.minIntervalForReCalcIntern = minIntervalForReCalc;
	}
	public TimeseriesSetProcessor3Base() {
		this(null, -1);
	}

	@Override
	public Integer getAbsoluteTiming() {
		return absoluteTimingIntern;
	}

	@Override
	public TimeseriesSimpleProcUtilBase getUtilProc() {
		return null;
	}

	@Override
	public long getMinIntervalForReCalc() {
		return minIntervalForReCalcIntern;
	}
}
