package extensionmodel.smarteff.api.base;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.smartrplace.extensionservice.ExtensionUserData;

import extensionmodel.smarteff.api.common.MasterUserData;

/**Top-level data specification. For models defining SmartEffUserData or ExtensionUserData as
 * parent that are not specified here respective decorators with names defined in
 * CapabilityHelper.getSingleResourceName or getMulti*ResourceName shall be attached.
 * For resources with a multi-cardinality a default element can be specified (usually only a
 * default element for generalData). The name starts with "default".
 */
public interface SmartEffUserData extends SmartEffTopLevelData, ExtensionUserData {
	MasterUserData masterUserData();
	
	/** These resources shall be replaced my MemoryResources in the future. The list
	 * entries are removed on every system startup
	 */
	ResourceList<Resource> temporaryResources();
}
