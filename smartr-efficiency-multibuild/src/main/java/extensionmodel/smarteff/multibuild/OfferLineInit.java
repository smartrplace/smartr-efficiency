package extensionmodel.smarteff.multibuild;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface OfferLineInit extends SmartEffResource {
	@Override
	StringResource name();
	
	/** price of single unit in EUR*/
	FloatResource singlePrice();
	
	/** number of items*/
	FloatResource number();
	
	/** Total price for all units of the type*/
	FloatResource totalPrice();
	
	/**0/not existing: standard line
	 * 1 : sum without VAT
	 * 2 : VAT line
	 * 3 : sum including VAT
	 */
	IntegerResource lineType();
}
