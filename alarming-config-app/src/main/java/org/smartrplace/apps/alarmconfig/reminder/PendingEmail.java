package org.smartrplace.apps.alarmconfig.reminder;

import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.Data;

/**
 * Resource type for internal use.
 */
public interface PendingEmail extends Data {
	
	StringResource senderEmail();
	StringResource senderName();
	StringResource subject();
	//StringResource recipient(); // will be determined from the current state of the issue
	StringResource message();

}
