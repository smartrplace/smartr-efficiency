package org.smartrplace.extensionservice;

import java.util.List;

import org.ogema.generictype.GenericDataTypeDeclaration;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration.Cardinality;
import org.smartrplace.extensionservice.driver.DriverProvider;
import org.smartrplace.extensionservice.proposal.LogicProvider;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;

import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.widgets.template.LabelledItem;

/** Capability information that is made public to users and applications. The main information
 * provided via this interface is the definition of the input types of NavigationGUIProviders and
 * LogicProviders whereas LogicProviders can also be Synchronizers (see {@link DriverProvider},
 * HouseKeeping modules and probably also manual import/export modules (to be tested via development).
 * <br>
 * There are currently 3 interfaces used for evaluation input type definition:
 * - {@link ResultType}: Standard evaluation input. Besides id and label that cannot be interpreted
 *    automatically this only contains the SingleValueResource-type that is expected as timeseries
 *    input. Scalar/Constant input is only supported via configurations. Furthermore this is usually
 *    limited to a certain EvaluationProvider.
 * - {@link GenericDataTypeDeclaration}: Here timeseries, constant value and structured input can
 *    be requested in extension to the definition of the input resource type. Also additional
 *    attributes can be given. Note that it does NOT extend ResultType as such instances are usually
 *    not limited to be used only by a single EvaluationProvider.
 * - GaRoDataType: This extends GenericDataTypeDeclaration. It provides additional documentation to
 *    really define a very specific input type in the GaRo context as two data sources that have
 *    the same GenericDataTypeDeclaration may still have different content. As it extends
 *    GenericDataTypeDeclaration also a GaRoDataType can be given in {@link EntryType#getType()}.
 */
public interface ExtensionCapabilityPublicData extends LabelledItem {
	public static interface EntryType {
		//Class<? extends Resource> getType();
		GenericDataTypeDeclaration getType();
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

	/** For LogicProviders this method checks if {@link LogicProvider#calculate(ExtensionResourceAccessInitData)}
	 * can be called with the same input data. For NavigationProviders it checks whether a page can be
	 * opened for a certain input. If only a single entry resource is required this is usually true
	 * assuming that the edit page for the entry resource has a meaningful checkResource method before
	 * the resource is activated, which makes sure all really required values are set.
	 */
	default boolean isEntryPossible(ExtensionResourceAccessInitData data) {
		return true;
	};
}
