package org.smartrplace.app.evaladm.gui;

import org.smartrplace.smarteff.access.api.EvalButtonConfigService;

public interface EvalButtonConfigServiceProvider {
	/** May return null if service is not yet available*/
	EvalButtonConfigService getService();
}
