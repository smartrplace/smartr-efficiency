package extensionmodel.smarteff.api.common;

import org.ogema.core.model.array.StringArrayResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/** Provide modules and users that can access the resource and the sub tree
 * TODO: A more sophisticated access control may be provided in the future.*/
public interface AccessControl extends SmartEffResource {
	/** Modules defined here will be allowed to read the resource to which the AccessControl resource is
	 * applied. This applies to all users. Usually the module uses the information but does not make all
	 * the information explicitly available to the users. Typically this is used for internal parameters
	 * (see {@link LogicProviderBase#getInternalParamType()} )
	 */
	StringArrayResource modules();
	
	/** Users defined here may access the data by all modules. Usually the resource type to which this
	 * is applied requires a special table page that supports cross-user access. This is the case for
	 * {@link https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-efficiency-util/src/main/java/org/smartrplace/commontypes/BuildingTablePage.java},
	 * which uses CrossUserBuildingTablePage.
	 * @return
	 */
	StringArrayResource users();
}
