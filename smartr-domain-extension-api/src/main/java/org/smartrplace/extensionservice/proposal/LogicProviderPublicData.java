package org.smartrplace.extensionservice.proposal;

import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForEvaluation;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.resourcecreate.ProviderPublicDataForCreate;

/** Methods of a {@link LogicProvider} that are accessible by applications.
 *
 */
public interface LogicProviderPublicData extends ProviderPublicDataForCreate {

	/** Perform calculation of logic
	 * 
	 * @param data system access for the calculation
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
	
	public class EvaluationResultTypes {
		/** EvaluationProvider.id() -> Results*/
		private final Map<String, List<GenericDataTypeDeclaration>> results;

		/**Positive values are default KPI interval types; additional intervals may be configurable
		 * If positive values are defined KPI calculation is supported. a negative value indicates
		 * JSON file storage. Further types may be defined in the future.
		 */
		private final List<Integer> storageTypes;
		
		public EvaluationResultTypes(Map<String, List<GenericDataTypeDeclaration>> results,
				List<Integer> storageTypes) {
			this.results = results;
			this.storageTypes = storageTypes;
		}

		public Map<String, List<GenericDataTypeDeclaration>> getResults() {
			return results;
		}

		public List<Integer> getStorageTypes() {
			return storageTypes;
		}
	}
	
	/** If no evaluations are supported return null*/
	default List<EvaluationResultTypes> getEvaluationResultTypes() {return null;}
	
	/** If the logic provider calculates KPIs the provider id returned here can be used to obtain
	 * the KPI results via {@link ExtensionPageSystemAccessForEvaluation}.
	 */
	default String getProviderId() {return null;}
	
	/** The LogicProvider shall put all its primary specific parameters into a
	 * single type. The type may also be used by other LogicProviders. Parameters
	 * from other sources may be used, but value initialization should not be
	 * required from the provider.<br>
	 * If no own parameters need to be initialized this can be null.
	 */
	Class<? extends Resource> getParamType();
}
