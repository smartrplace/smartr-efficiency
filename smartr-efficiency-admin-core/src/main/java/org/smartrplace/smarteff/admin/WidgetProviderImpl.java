package org.smartrplace.smarteff.admin;

import org.ogema.core.application.ApplicationManager;
import org.smartrplace.extensionservice.gui.WidgetProvider;
import org.smartrplace.smarteff.admin.gui.special.FileUploaderProtectedImpl;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.fileupload.FileUploadListener;

public abstract class WidgetProviderImpl implements WidgetProvider {
	public abstract String getUserName(OgemaHttpRequest req);

	private final ApplicationManager appMan;
	
	public WidgetProviderImpl(ApplicationManager appMan) {
		this.appMan = appMan;
	}

	@Override
	public <T> FileUploaderProtected getFileUpload(WidgetPage<?> page, String id,
			FileUploadListenerToFile listenerToFile, FileUploadListener<T> listener, Alert alert) {
		return new FileUploaderProtectedImpl(page, id, appMan, alert, listenerToFile, listener) {
			@Override
			public String getUserName(OgemaHttpRequest req) {
				return WidgetProviderImpl.this.getUserName(req);
			}
			
		};
	}

}
