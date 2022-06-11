package org.smartrplace.csv.upload.generic;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.ogema.core.application.ApplicationManager;
import org.smartrplace.extensionservice.gui.WidgetProvider.FileUploadListenerToFile;
import org.smartrplace.extensionservice.gui.WidgetProvider.FileUploaderProtected;
import org.smartrplace.smarteff.admin.gui.special.FileUploaderProtectedImpl;
import org.smartrplace.smarteff.resourcecsv.CSVConfiguration;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.fileupload.FileUploadListener;

public class CSVUploadWidgets {
	public final FileUploadListenerToFile listenerToFile;
	public final FileUploaderProtected uploader;
	public final CSVUploadButtonBase csvButton;
	
	protected final ApplicationManager appMan;

	public interface CSVUploadListener {
		/** Notification that new file is available
		 * 
		 * @param filePath
		 * @param req
		 * @return if true then for each line of the file {@link #newLineAvailable(String, String[], OgemaHttpRequest)}
		 * 		is called, otherwise the lines shall be processed by the application directly
		 */
		public boolean fileUploaded(String filePath, OgemaHttpRequest req);
		default void newLineAvailable(String filePath, CSVRecord record, OgemaHttpRequest req) {};
		default String[] csvHeadersRequired(OgemaHttpRequest req) {return null;}
	}
	
	/** Bundled provision of upload Button and FileUpload
	 * @param tsListener if null the default upload procedure is done and the file is registered for access via a
	 * 		DataProvider or via a SmartEffTimeSeries resource. No data is actually imported into the OGEMA resource database.
	 * 		If the tsListener exists it is called when the file is imported and the time series content of the file
	 * 		is offered. If the content is imported it is recommended to delete the file afterwards, which is not done
	 * 		by the widgets generated here themselves.
	 */
	public CSVUploadWidgets(
			WidgetPage<?> page, Alert alert,
			String pid,
			String uploadButtonText,
			final CSVUploadListener tsListener,
			ApplicationManager appMan) {
		this.appMan = appMan;
		listenerToFile = new FileUploadListenerToFile() {
			
			@Override
			public void fileUploaded(String filePath, OgemaHttpRequest req) {
				if(tsListener != null) {
					if(tsListener.fileUploaded(filePath, req)) {
						try {
							Reader r = new InputStreamReader(new BOMInputStream(new FileInputStream(filePath)), StandardCharsets.UTF_8);
							importCSV(r, tsListener, filePath, req);
							r.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					return;
				}
				System.out.println("File uploaded to "+filePath);
			}
		};
		uploader = getFileUpload(page, "upload"+pid, listenerToFile, null, alert);
		csvButton = new CSVUploadButtonBase(page, "csvUploadButton"+pid, uploader, alert) {
			private static final long serialVersionUID = 1L;
			@Override
			protected String getButtonText(OgemaHttpRequest req) {
				return "Upload CSV";
			}
		};
		csvButton.setDefaultText(uploadButtonText);
		csvButton.triggerOnPOST(csvButton); //csvButton.registerDependentWidget(csvButton);*/
		
	}
	
	protected <T> FileUploaderProtected getFileUpload(WidgetPage<?> page, String id,
			FileUploadListenerToFile listenerToFile, FileUploadListener<T> listener, Alert alert) {
		return new FileUploaderProtectedImpl(page, id, appMan, alert, listenerToFile, listener) {
			@Override
			public String getUserName(OgemaHttpRequest req) {
				return "A";
			}			
		};
	}
	
	protected void importCSV(Reader reader, CSVUploadListener tsListener,
			String filePath, OgemaHttpRequest req) throws IOException {
		
		CSVParser parser = new CSVParser(reader, CSVConfiguration.CSV_FORMAT);
		
		String[] headersRequired = tsListener.csvHeadersRequired(req);
		if(headersRequired != null) if (!checkHeaders(parser, headersRequired))
			return;
		
		
		for (CSVRecord r : parser) {
			tsListener.newLineAvailable(filePath, r, req);
		}
		parser.close();
	}
	
	private boolean checkHeaders(CSVParser p, String[] headersRequired) {
		Set<String> present = p.getHeaderMap().keySet();
		List<String> missing = new ArrayList<>();
		for (String header : headersRequired) {
			if (!present.contains(header))
				missing.add(header);
		}
		if(missing.size() == 0) {
			return true;
		} else {
			return false;
		}
	}
}
