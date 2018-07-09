package extensionmodel.smarteff.api.common;

import org.ogema.core.model.array.StringArrayResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/** Provide modules and users that can access the resource and the sub tree
 * TODO: A more sophisticated access control may be provided in the future.*/
public interface AccessControl extends SmartEffResource {
	StringArrayResource modules();
	StringArrayResource users();
}
