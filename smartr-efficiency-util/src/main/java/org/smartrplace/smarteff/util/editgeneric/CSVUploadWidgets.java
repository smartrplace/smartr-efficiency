package org.smartrplace.smarteff.util.editgeneric;

import org.ogema.core.model.simple.StringResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.WidgetProvider.FileUploadListenerToFile;
import org.smartrplace.extensionservice.gui.WidgetProvider.FileUploaderProtected;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForTimeseries;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public abstract class CSVUploadWidgets {
	public final FileUploadListenerToFile listenerToFile;
	public final FileUploaderProtected uploader;
	public final CSVUploadButton csvButton;

	public static final String SINGLE_COLUMN_CSV_ID = "SINGLE_COLUMN_CSV:";
	public static final String DEFAULT_TIMESTAMP_FORMAT = "d.M.y H:m"; //"d:m:yy h:mm";
	
	public interface TimeseriesUploadListener {
		public void fileUploaded(ReadOnlyTimeSeries timeSeries, String filePath, OgemaHttpRequest req);
		default public String getFileType(OgemaHttpRequest req) {
			//Template method, so we keep structure of GenericDriverProvider#addSingleColumnCSVFile
			String format = null;
			return SINGLE_COLUMN_CSV_ID+((format !=null)?format:DEFAULT_TIMESTAMP_FORMAT);
		}
	}
	protected abstract SmartEffTimeSeries getTSResource(OgemaHttpRequest req);
	
	/** Bundled provision of upload Button and FileUpload
	 * @param tsListener if null the default upload procedure is done and the file is registered for access via a
	 * 		DataProvider or via a SmartEffTimeSeries resource. No data is actually imported into the OGEMA resource database.
	 * 		If the tsListener exists it is called when the file is imported and the time series content of the file
	 * 		is offered. If the content is imported it is recommended to delete the file afterwards, which is not done
	 * 		by the widgets generated here themselves.
	 */
	public CSVUploadWidgets(ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			WidgetPage<?> page, Alert alert,
			String pid,
			String uploadButtonText,
			TimeseriesUploadListener tsListener) {
		listenerToFile = new FileUploadListenerToFile() {
			
			@Override
			public void fileUploaded(String filePath, OgemaHttpRequest req) {
				if(tsListener != null) {
					ExtensionPageSystemAccessForTimeseries tsMan = exPage.getAccessData(req).getTimeseriesManagement();
					String fileType = tsListener.getFileType(req);
					ReadOnlyTimeSeries timeSeries = tsMan.readTimeSeriesFromFiles(fileType , new String[] {filePath});
					tsListener.fileUploaded(timeSeries , filePath, req);
					return;
				}
				System.out.println("File uploaded to "+filePath);
				ExtensionPageSystemAccessForTimeseries tsMan = exPage.getAccessData(req).getTimeseriesManagement();
				SmartEffTimeSeries tsResource = getTSResource(req);
				tsResource.driverId().<StringResource>create().setValue(tsMan.getGenericDriverProviderId());
				tsResource.dataTypeId().<StringResource>create().setValue(GaRoDataType.PowerMeter.label(null));
				if(!tsResource.isActive()) {
					tsResource.activate(true);
				}
				tsMan.registerSingleColumnCSVFile(
						tsResource, GaRoDataType.PowerMeter, null, filePath, null);
			}
		};
		uploader = exPage.getSpecialWidgetManagement().
				getFileUpload(page, "upload"+pid, listenerToFile, null, alert);
		csvButton = new CSVUploadButton(page, "csvUploadButton"+pid, uploader, alert) {
			private static final long serialVersionUID = 1L;
			@Override
			protected Integer getSize(OgemaHttpRequest req) {
				return null;
				//ExtensionPageSystemAccessForTimeseries tsMan = exPage.getAccessData(req).getTimeseriesManagement();
				//SmartEffTimeSeries tsResource = getTSResource(req);
				//return tsMan.getFileNum(tsResource, null);
			}
		};
		csvButton.setDefaultText(uploadButtonText);
		csvButton.triggerOnPOST(csvButton); //csvButton.registerDependentWidget(csvButton);*/
		
	}
}
