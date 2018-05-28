package org.smartrplace.extensionservice.gui;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.fileupload.FileUploadListener;

public interface WidgetProvider {
	public interface FileUploaderProtected {
		/**Call this in button that initiates the upload*/
		void onPOST(OgemaHttpRequest req);
		
		/**Register a button that opens the widget*/
		void registerTrigger(OgemaWidget governor);
		
		OgemaWidget getFileUpload();
	}
	
	public interface FileUploadListenerToFile {
		void fileUploaded(String filePath, OgemaHttpRequest req);
	}
	
	/** Upload file
	 * 
	 * @param page
	 * @param id
	 * @param listenerToFile exactly one of listenerToFile and listener should be non-null. If
	 * 		listenerToFile is given then the file is stored in a standard location and the path
	 * 		is provided. Otherwise the FileItem is provided which can be processed without saving
	 * 		to disk
	 * @param listener
	 * @param alert
	 * @return
	 */
	<T> FileUploaderProtected getFileUpload(WidgetPage<?> page, String id,
			FileUploadListenerToFile listenerToFile, FileUploadListener<T> listener, Alert alert);
}
