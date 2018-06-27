package org.smartrplace.extensionservice.proposal;

import org.ogema.core.model.array.TimeArrayResource;
import org.ogema.core.model.simple.StringResource;

/** Result of a {@link LogicProvider} that uses time series as input, e.g. implemented based on
 * {@link LogicEvalProviderBase}
 */
public interface CalculatedEvalResult extends CalculatedData {
	/** Start times used or just indicating initial start time and start time after
	 * gaps calculated
	 */
	TimeArrayResource startTimes();
	TimeArrayResource endTimes();
	StringResource evalProviderId();
}
