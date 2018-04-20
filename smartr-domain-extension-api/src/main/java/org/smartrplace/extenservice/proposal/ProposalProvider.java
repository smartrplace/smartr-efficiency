package org.smartrplace.extenservice.proposal;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ExtensionCapabilityForCreate;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;

/** A ProposalProvider calculates some results based on an entry point-based input resource and futher user and
 * general data it finds itself based on the entry point-based resource(s). So a ProposalProvider acts very
 * similar to a GUI Page provider that generates resources, but the ProposalProvider generates the content
 * automatically whereas the GUI provider lets the user enter the content. So a ProposalProvider is
 * suitable to be operated in batch processes etc.<br>
 * ProposalProviders are not designed to operate as a background service, listen to new resources etc. This
 * has to be done by a separate module if required that would start relevant ProposalProviders when
 * new data is available etc.
 */
public interface ProposalProvider extends ExtensionCapabilityForCreate {
	void init(ApplicationManagerSPExt appManExt);
	
	/** For each new session the relevant user data is provided with this method
	 * 
	 * @param entryTypeIdx index within {@link #getEntryTypes()} used to open the page
	 * @param entryResources resources of the entry type specified by entryTypeIdx. If the cardinality of
	 * 		the EntryType does not allow multiple entries the list will only contain a single element. If
	 * 		the cardinality allows zero the list may be empty.
	 * @param userData domain-specific reference to user data. May also be obtainable just as parent of the resource.
	 * @param listener when the user presses a "Save" button or finishes editing otherwise, finishing of editing
	 *		 shall be notified to the main domain app so that it can activate resources etc.
	 * @return resources created and modified. The first element should contain the most important result and the
	 * 		further order of the list should reflect the relevance of the changes (if possible) 
	 */
	List<Resource> calculate(ExtensionResourceAccessInitData data);
	
	public class CalculationResultType {
		private final Class<? extends CalculatedData> resourceType;
		private final int entryTypeParentIdx;
		public CalculationResultType(Class<? extends CalculatedData> resourceType) {
			this.resourceType = resourceType;
			this.entryTypeParentIdx = 0;
		}
		public CalculationResultType(Class<? extends CalculatedData> resourceType, int entryTypeParentIdx) {
			this.resourceType = resourceType;
			this.entryTypeParentIdx = entryTypeParentIdx;
		}
		public Class<? extends CalculatedData> resourceType() {
			return resourceType;
		}
		public int entryTypeParentIdx() {
			return entryTypeParentIdx;
		}
	}
	/**Usually this list contains only a single type, but in some cases the result may be distributed
	 * over more than one result type. The {@link CalculationResultType#entryTypeParentIdx} specifies
	 * where the respective result is placed. Usually the {@link ExtensionResourceTypeDeclaration} for
	 * the result type shall specifiy the type of respective EntryType given from {@link #getEntryTypes()}
	 * as parent and the results is attached to the parent.<br>
	 * If the parent type does not match the type must match any resource type given in the direct
	 * parent - child - hierarchy of the type specified by the EntryType. The result is then attached to
	 * the fitting type element if it exists. Otherwise the result cannot be provided.
	 */
	List<CalculationResultType> resultTypes();
}
