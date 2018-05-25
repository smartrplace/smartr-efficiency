package org.smartrplace.smarteff.admin;

import org.ogema.core.application.ApplicationManager;
import org.smartrplace.extensionservice.gui.WidgetProvider;
import org.smartrplace.smarteff.admin.gui.special.FileUploaderProtectedImpl;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.fileupload.FileUploadListener;

public class WidgetProviderImpl implements WidgetProvider {
	private final ApplicationManager appMan;
	
	public WidgetProviderImpl(ApplicationManager appMan) {
		this.appMan = appMan;
	}

	@Override
	public <T> FileUploaderProtected getFileUpload(WidgetPage<?> page, String id,
			FileUploadListenerToFile listenerToFile, FileUploadListener<T> listener, Alert alert) {
		return new FileUploaderProtectedImpl(page, id, appMan, alert, listenerToFile, listener);
	}

}
