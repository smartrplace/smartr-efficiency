package org.smartrplace.extensionservice.resourcecreate;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.generictype.GenericDataTypeDeclaration;

public interface ExtensionPageSystemAccessForTimeseries {
	
	/** After this registration the respective time series will be available via the GenericDriverProvider.
	 * This does not create a resource of type SmartEffTimeSeries.
	 * 
	 * @param entryResource
	 * @param dataType
	 * @param sourceId may be null
	 * @param sched
	 */
	public void registerSchedule(Resource entryResource, GenericDataTypeDeclaration dataType, 
			String sourceId, Schedule sched);
	
	public void registerRecordedData(Resource entryResource, GenericDataTypeDeclaration dataType, 
			String sourceId, SingleValueResource recordedDataParent);
	
	public void registerSingleColumnCSVFile(Resource entryResource, GenericDataTypeDeclaration dataType, 
			String sourceId, String filePath, String format);
	
	public List<ReadOnlyTimeSeries> getTimeSeries(Resource entryResource, GenericDataTypeDeclaration dataType, 
			String sourceId);
	
	/** Number of data imports or similar larger data packages that were used to provide a certain
	 * time series. This is mainly intended for data provided via file imports and should be much more
	 * efficient than getting the actual time series to calculate the number of data points, which can
	 * also be used to provide size information.
	 */
	public int getFileNum(Resource entryResource, GenericDataTypeDeclaration dataType,
			String sourceId);
	
	public String getGenericDriverProviderId();
}
