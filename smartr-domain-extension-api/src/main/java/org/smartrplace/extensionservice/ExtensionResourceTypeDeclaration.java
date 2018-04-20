package org.smartrplace.extensionservice;

import org.ogema.core.model.Resource;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Each type on the system that has an ExtensionResourceTypeDeclaration should only be used
 * as child of the declared parent type. Usually not two types that have a child-parent
 * inheritance relation should both have an ExtionsionResourceTypeDeclaration as this
 * would lead to confusion when searching for the right ExtensionResourceTypeDeclaration.
 */
public interface ExtensionResourceTypeDeclaration<T extends Resource> {
	/**Resource type required by app module to store its persistent data*/
	Class<? extends T> dataType();
	/** Human readable label of the type.
	 */
	String label(OgemaLocale locale);
	
	/**Super type to which new resource or resource list shall be applied. The element shall be 
	 * created as decorator if not (yet) defined as regular element. If the extension element is
	 * accepted as standard extension the respective element should be added in the parent tpye
	 * definition.
	 */
	Class<? extends T> parentType();
	
	public enum Cardinality {
	
	/**Maximal one subresource, do not create if not exists*/
	SINGLE_VALUE_OPTIONAL,
	/**Exactly one item required, should be created if not existing yet*/
	SINGLE_VALUE_REQUIRED,
	/** Create resource list*/
	MULTIPLE_OPTIONAL,
	MULTIPLE_REQUIRED
	
	}

	Cardinality cardinality();
}
