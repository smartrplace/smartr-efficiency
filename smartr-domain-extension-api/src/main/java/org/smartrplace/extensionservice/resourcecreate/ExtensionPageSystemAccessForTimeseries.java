package org.smartrplace.extensionservice.resourcecreate;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.smartrplace.extensionservice.SmartEffTimeSeries;

public interface ExtensionPageSystemAccessForTimeseries {
	
	/** After this registration the respective time series will be available via the GenericDriverProvider.
	 * This does not create a resource of type SmartEffTimeSeries.
	 * 
	 * @param entryResource
	 * @param dataType
	 * @param sourceId may be null
	 * @param sched
	 */
	public void registerSchedule(SmartEffTimeSeries timeSeries, GenericDataTypeDeclaration dataType, 
			String sourceId, Schedule sched);
	
	public void registerRecordedData(SmartEffTimeSeries timeSeries, GenericDataTypeDeclaration dataType, 
			String sourceId, SingleValueResource recordedDataParent);
	
	//TODO: This is a very specific method, should be more general in the future
	public void registerSingleColumnCSVFile(SmartEffTimeSeries timeSeries, GenericDataTypeDeclaration dataType, 
			String sourceId, String filePath, String format);
	
	public List<ReadOnlyTimeSeries> getTimeSeries(Resource entryResource, GenericDataTypeDeclaration dataType, 
			String sourceId);
	public ReadOnlyTimeSeries getTimeSeries(SmartEffTimeSeries smartTs);
	
	/** Read time series from files on disk.
	 * @param fileType see {@link SmartEffTimeSeries#fileType()}
	 * @param paths paths of files to be read*/
	public ReadOnlyTimeSeries readTimeSeriesFromFiles(String fileType, String[] paths);
	
	/** Number of data imports or similar larger data packages that were used to provide a certain
	 * time series. This is mainly intended for data provided via file imports and should be much more
	 * efficient than getting the actual time series to calculate the number of data points, which can
	 * also be used to provide size information.
	 */
	public int getFileNum(SmartEffTimeSeries timeSeries, String sourceId);
	
	public String getGenericDriverProviderId();
}
