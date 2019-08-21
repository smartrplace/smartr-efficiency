package extensionmodel.smarteff.multibuild;

import org.ogema.core.model.simple.IntegerResource;

import de.iwes.util.timer.AbsoluteTiming;

public interface OfferLineRecurrent extends OfferLineInit {
	/** See {@link AbsoluteTiming} for options*/
	IntegerResource billingInterval();
}
