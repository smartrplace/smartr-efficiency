package extensionmodel.smarteff.defaultproposal;

import org.ogema.core.model.array.TimeArrayResource;
import org.ogema.core.model.simple.StringResource;
import org.smartrplace.extensionservice.proposal.CalculatedData;

public interface CalculatedEvalResult extends CalculatedData {
	/** Start times used or just indicating initial start time and start time after
	 * gaps calculated
	 */
	TimeArrayResource startTimes();
	TimeArrayResource endTimes();
	StringResource evalProviderId();
}
