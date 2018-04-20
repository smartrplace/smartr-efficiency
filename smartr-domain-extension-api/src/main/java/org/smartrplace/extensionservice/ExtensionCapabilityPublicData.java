package org.smartrplace.extensionservice;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.proposal.ProposalProvider;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration.Cardinality;

import de.iwes.widgets.template.LabelledItem;

public interface ExtensionCapabilityPublicData extends LabelledItem {
	public static interface EntryType {
		Class<? extends Resource> getType();
		/** The standard cardinality is SINGLE_VALUE_REQUIRED. If _OPTIONAL is specified the
		 * navigator must be able to search by itself for suitable data in the userData or generalData.
		 */
		Cardinality getCardinality();
	}

	/** Any resource of any entry type shall be sufficient to open the page. Only a single entry type is used to
	 * open a page, but if the cardinality of the type allows it more than one element may be submitted.
	 * If this is null the page is a start page.<br>
	 * Note that this is NOT necessarily a complete list of resources accessed by the page or by the
	 * proposal calculation.
	 */
	List<EntryType> getEntryTypes();

	/** For ProposalProviders this method checks if {@link ProposalProvider#calculate(ExtensionResourceAccessInitData)}
	 * can be called with the same input data. For NavigationProviders it checks whether a page can be
	 * opened for a certain input. If only a single entry resource is required this is usually true
	 * assuming that the edit page for the entry resource has a meaningful checkResource method before
	 * the resource is activated, which makes sure all really required values are set.
	 */
	default boolean isEntryPossible(ExtensionResourceAccessInitData data) {
		return true;
	};
}
