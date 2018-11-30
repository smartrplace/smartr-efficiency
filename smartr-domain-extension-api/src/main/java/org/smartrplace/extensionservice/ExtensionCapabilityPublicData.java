package org.smartrplace.extensionservice;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration.Cardinality;
import org.smartrplace.extensionservice.driver.DriverProvider;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;
import org.smartrplace.extensionservice.proposal.LogicProvider;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;

import de.iwes.widgets.template.LabelledItem;

/** Capability information that is made public to users and applications. The main information
 * provided via this interface is the definition of the input types of NavigationGUIProviders and
 * LogicProviders whereas LogicProviders can also be Synchronizers (see {@link DriverProvider},
 * HouseKeeping modules and probably also manual import/export modules (to be tested via development).
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
	 * If this is null the page is a start page which means it can be opened without any entry resource.
	 * A typical example is a table of all buildings belonging to the current user.<br>
	 * If a page can be opened by any resource (but a resource needs to be selected) then the
	 * EntryType may specify {@link Resource} as entry type as all resource types are inherited from this class.<br<
	 * For Edit-Pages this usually is the resource type that can be edited by the page. For table pages
	 * this is usually the type of the resource expected which is the parent of all resources listed in
	 * the table. Some table pages list all resources below the parent, some only those of a selected type.
	 * The type(s) of resources in the table can be specified by
	 * Note that this is NOT necessarily a complete list of resources accessed by the page or by the
	 * proposal calculation.
	 */
	List<EntryType> getEntryTypes();
	
	/**Only relevant for GUI pages with {@link NavigationGUIProvider#getPageType()}==TABLE_PAGE or
	 * {@link LogicProvider}s. For table pages this is a list of resource types used for the table.
	 * If null all resources may be used or a selection that is not limited by resource type. A
	 * logic provider may provide sub types here that are used, but cannot be given as entry resources.
	 * A return value of null may also indicate that the information is just not provided and is
	 * not a guarantee that the types of resources used is not limited.
	 */
	default List<GenericDataTypeDeclaration> typesListedInTable() {return null;}
	/** Pages that provide a table specifically for one or several resource types indicate these resource
	 * types here
	 * @return usually a single element list. Specific tables for similar types not inherited from each other
	 * 		may be provided as a list
	 */
	//List<Class<? extends Resource>> typesListedInTable();
	//default List<Class<? extends Resource>> typesListedInTable() {return null;}


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
