package org.smartrplace.extenservice.proposal;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;

public interface ProjectOffer extends CalculatedData {
	/**An offer always needs to provide some specification what is included and what has to
	 * be provided by the customer.
	 */
	StringResource description();
	/** Price of the offer. The price is usually equal or lower than {@link ProjectProposal#costOfProject()}
	 * as the cost of the project may include additional material cost etc. that is planned to be
	 * acquired or provided by the client himself/herself.
	 */
	FloatResource price();
	/** As an additional information a price may be given if the project is realized
	 * as turn key solution with minimal possible customer interaction.
	 */
	FloatResource turnKeyPrice();
	/** If a turnKeyPrice is given also a description has to be provided what kind of customer
	 * contribution still is required.
	 */
	StringResource turnKeyDescription();
	
	/** If the projected contract partner is different from {@link ProjectProposal#plannerUserName()}
	 * provide here the projected contract partner. Usually this should also be a valid user name, 
	 * but here also any company name by be given.*/
	StringResource offeredBy();
}
