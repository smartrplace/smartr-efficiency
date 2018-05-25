package org.smartrplace.smarteff.util.editgeneric;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.FileItem;
import org.ogema.core.application.ApplicationManager;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.fileupload.FileUpload;
import de.iwes.widgets.html.fileupload.FileUploadListener;
import de.iwes.widgets.html.form.button.Button;

/** Provide all elements for a basic file upload including widgets
 * 
 * @author dnestle
 *
 */
public abstract class FileUploader {
	protected abstract void fileUploadFinished(FileItem fileItem, Float context, OgemaHttpRequest req)
				throws IOException;
	public final FileUpload upload;
	public final FileUploadListener<Float> listener;
	public final Button buttonUpload;
	
	/** Do not forget to add upload and buttonUpload to the page, this is not done by the constructor.
	 * 
	 * @param page
	 * @param appMan
	 * @param alert if null no alert notification on started uploaded is implemented
	 */
	public FileUploader(WidgetPage<?> page, final ApplicationManager appMan, final Alert alert) {
		this(page, new FileUpload(page, "upload", appMan, true), alert);
		
	}
	public FileUploader(WidgetPage<?> page, final FileUpload upload, final Alert alert) {
		this.upload = upload;
		listener = new FileUploadListener<Float>() {

			@Override
			public void fileUploaded(FileItem fileItem, Float context, OgemaHttpRequest req) {
				try {
					fileUploadFinished(fileItem, context, req);
				} catch (IOException e) {
					System.out.println("Error copying file");
					e.printStackTrace();
					return;
				}
			}
			
		};
		buttonUpload = new Button(page, "buttonUploadReplay", "Upload&Update Replay-on-clean") {
		//final Button buttonUploadReplay = new Button(page, "buttonUploadReplay", "Upload&Reboot") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				upload.registerListener(listener, 1.5f, req);
				if(alert != null) alert.showAlert("Started Upload!", true, req);
			}
		};
    	buttonUpload.triggerAction(upload, TriggeringAction.POST_REQUEST, TriggeredAction.POST_REQUEST);
		if(alert != null) buttonUpload.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	
	public static String destinationFile = "uploadDest/destinationFile";
	public static void fileUploadFinishedTemplate(FileItem fileItem, Float context, OgemaHttpRequest req)
			throws IOException {
		File destFile = new File(destinationFile, fileItem.getName());
		writeFile(fileItem.getInputStream(), destFile);						
	}

	
	public static void writeFile(InputStream is, File fileOut) throws IOException {
		FileOutputStream outputStream = null;
		try {
			outputStream =  new FileOutputStream(fileOut);
			int read = 0;
			byte[] bytes = new byte[1024];
	
			while ((read = is.read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
			}
		} finally {
			try {
				if (outputStream != null)
					outputStream.close();
				if (is != null)
					is.close();
			} catch (Exception e) {}
		}
	}
}
