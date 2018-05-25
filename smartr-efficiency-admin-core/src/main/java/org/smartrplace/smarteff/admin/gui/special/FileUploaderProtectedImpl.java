package org.smartrplace.smarteff.admin.gui.special;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.fileupload.FileItem;
import org.ogema.core.application.ApplicationManager;
import org.smartrplace.extensionservice.gui.WidgetProvider.FileUploadListenerToFile;
import org.smartrplace.extensionservice.gui.WidgetProvider.FileUploaderProtected;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.fileupload.FileUpload;
import de.iwes.widgets.html.fileupload.FileUploadListener;

/** Provide all elements for a basic file upload including widgets
 * 
 * @author dnestle
 *
 */
public class FileUploaderProtectedImpl implements FileUploaderProtected {
	
	/** Call this on button POST*/
	@Override
	public void onPOST(OgemaHttpRequest req) {
		upload.registerListener(listener, 1.5f, req);
	}
	
	@Override
	public void registerTrigger(OgemaWidget governor) {
	   	governor.triggerAction(upload, TriggeringAction.POST_REQUEST, TriggeredAction.POST_REQUEST);		
	}
	
	//protected abstract void fileUploadFinished(FileItem fileItem, Float context, OgemaHttpRequest req)
	//			throws IOException;
	private final FileUpload upload;
	private final FileUploadListener<Float> listener;
	//private final Button buttonUpload;
	//private final FileUploadListener<?> userListener;
	
	/** Do not forget to add upload and buttonUpload to the page, this is not done by the constructor.
	 * 
	 * @param page
	 * @param appMan
	 * @param alert if null no alert notification on started uploaded is implemented
	 */
	public FileUploaderProtectedImpl(WidgetPage<?> page, String widgetId, final ApplicationManager appMan, final Alert alert,
			FileUploadListenerToFile listenerToFile, FileUploadListener<?> userListener) {
		this.upload =  new FileUpload(page, "upload"+widgetId, appMan, true);
		//this.userListener = userListener;
		listener = new FileUploadListener<Float>() {

			@Override
			public void fileUploaded(FileItem fileItem, Float context, OgemaHttpRequest req) {
				try {
					if(listenerToFile != null) {
						String destinationDir = System.getProperty("org.smartrplace.store.filestoragelocation",
								"./filestorage");
						String fileName = fileItem.getName();
						Path file = Paths.get(destinationDir, fileName);
						int i = fileName.lastIndexOf(".");
						String ext;
						if(fileName.length() > i && i > 0)
							ext = fileName.substring(i);
						else ext = "";
						String main;
						if(i > 0) main = fileName.substring(0, i);
						else main = fileName;
						int add = 1;
						while(Files.exists(file)) {
							fileName = main + "_" + add + "." + ext;
							add++;
							file = Paths.get(destinationDir, fileName);						
						}
						//File destFile = new File(destinationDir, fileItem.getName());
						writeFile(fileItem.getInputStream(), file.toFile());						
						listenerToFile.fileUploaded(file.toString(), req);
					} else {
						userListener.fileUploaded(fileItem, null, req);
					}
				} catch (IOException e) {
					System.out.println("Error copying file");
					e.printStackTrace();
					return;
				}
			}
			
		};
		page.append(upload);
		/*buttonUpload = new Button(page, "buttonUploadReplay"+widgetId, "Upload&Update Replay-on-clean") {
		//final Button buttonUploadReplay = new Button(page, "buttonUploadReplay", "Upload&Reboot") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				upload.registerListener(listener, 1.5f, req);
				if(alert != null) alert.showAlert("Started Upload!", true, req);
			}
		};*/
    	//buttonUpload.triggerAction(upload, TriggeringAction.POST_REQUEST, TriggeredAction.POST_REQUEST);
		//if(alert != null) buttonUpload.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	
	//public static String destinationFile = "uploadDest/destinationFile";
	/*public static void fileUploadFinishedTemplate(FileItem fileItem, Float context, OgemaHttpRequest req)
			throws IOException {
		File destFile = new File(destinationFile, fileItem.getName());
		writeFile(fileItem.getInputStream(), destFile);						
	}*/

	
	private void writeFile(InputStream is, File fileOut) throws IOException {
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
	/*@Override
	public Button getUploadButton() {
		return buttonUpload;
	}*/
}
