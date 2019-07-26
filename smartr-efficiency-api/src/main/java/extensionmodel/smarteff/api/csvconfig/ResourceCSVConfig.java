package extensionmodel.smarteff.api.csvconfig;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.Configuration;

/**
 * CSVConfiguration, in resource form.
 *
 */
public interface ResourceCSVConfig extends Configuration {
	Resource parent();
	Resource root();
	IntegerResource activeStatus();
	IntegerResource exportReferences();
	/** Information about the CSV Format.  Cannot be used to change the format! */
	StringResource format();
}
