package org.smartrplace.tsproc.persist;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.TimedJobProvider;
import org.ogema.devicefinder.util.TimedJobMemoryData;
import org.ogema.model.jsonresult.JsonOGEMAFileData;
import org.ogema.model.jsonresult.JsonOGEMAFileManagementData;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;
import org.ogema.timeseries.eval.simple.mon3.ProcTs3PersistentData;
import org.ogema.timeseries.eval.simple.mon3.TimeseriesSimpleProcUtil3;
import org.ogema.util.jsonresult.management.EvalResultManagementStd;
import org.ogema.util.jsonresult.management.JsonOGEMAFileManagementImpl;
import org.ogema.util.jsonresult.management.api.JsonOGEMAFileManagement;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;
import org.smartrplace.os.util.DirUtils;
import org.smartrplace.util.format.ValueFormat;
import org.smartrplace.util.frontend.servlet.UserServletUtil;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class TsProcPersistUtil {
	private static final String PROC3_WORKSPACE = "WSPRoc3";
	public static final int MAX_LOCATION_FILENAME_LENGTH = 128;
	protected static JsonOGEMAFileManagement<ProcTs3PersistentData, JsonOGEMAFileData> fileMgmt;
	public static JsonOGEMAFileData saveTsToFile(Datapoint dp, ApplicationManager appMan) {
		initFileMgmt(appMan);
		
		ReadOnlyTimeSeries ts1 = dp.getTimeSeries();
		if(ts1 == null || (!(ts1 instanceof ProcessedReadOnlyTimeSeries3)))
			return null;
		ProcessedReadOnlyTimeSeries3 ts = (ProcessedReadOnlyTimeSeries3) dp.getTimeSeries();
		String fileName = getFileName(dp, ts);
		ProcTs3PersistentData persData = ts.getPersistentData();
		if(persData == null)
			return null;
		JsonOGEMAFileData result = fileMgmt.saveResult(persData, ProcTs3PersistentData.class,
				10, fileName, true, "ProcTs3");
		return result;
	}
	
	public static String getFileName(Datapoint dp, ProcessedReadOnlyTimeSeries3 ts) {
		//String result1 = UserServletUtil.getHashWithPrefix("X2S_"+ts.resultLabel(), dp.getLocation());
		String result = DirUtils.getValidFilename(dp.getLocation(), MAX_LOCATION_FILENAME_LENGTH);
		return result;
	}
	
	public static void importTsFromFile(Datapoint dp, ApplicationManager appMan) {
		initFileMgmt(appMan);
		
		ReadOnlyTimeSeries ts1 = dp.getTimeSeries();
		if(ts1 == null || (!(ts1 instanceof ProcessedReadOnlyTimeSeries3)))
			return;
		ProcessedReadOnlyTimeSeries3 ts = (ProcessedReadOnlyTimeSeries3) dp.getTimeSeries();
		String fileName = getFileName(dp, ts);
		String fileNameWithPath = JsonOGEMAFileManagementImpl.getJSONFilePath(
				fileMgmt.getFilePath(null, 10, null), fileName, true);
		ProcTs3PersistentData fromFile = fileMgmt.importFromJSON(fileNameWithPath, ProcTs3PersistentData.class);
		if(fromFile != null)
			ts.initValuesFromFile(fromFile);
	}
	
	public static TimedJobMemoryData registerTimedSaving(TimeseriesSimpleProcUtil3 util,
			String label, String id, DatapointService dpService) {
		TimedJobProvider prov = new TimedJobProvider() {
			
			@Override
			public String label(OgemaLocale locale) {
				return label;
			}
			
			@Override
			public String id() {
				return id;
			}
			
			@Override
			public boolean initConfigResource(TimedJobConfig config) {
				ValueResourceHelper.setCreate(config.alignedInterval(), AbsoluteTiming.DAY);
				ValueResourceHelper.setCreate(config.performOperationOnStartUpWithDelay(), 15);
				ValueResourceHelper.setCreate(config.disable(), false);
				return true;
			}
			
			@Override
			public String getInitVersion() {
				return "A";
			}
			
			@Override
			public void execute(long now, TimedJobMemoryData data) {
				util.saveUpdatesForAllData();
			}
			
			@Override
			public int evalJobType() {
				return 0;
			}
		};
		return dpService.timedJobService().registerTimedJobProvider(prov);
	}
	
	protected static void initFileMgmt(ApplicationManager appMan) {
		if(fileMgmt == null) {
			JsonOGEMAFileManagementData mgmtRes = ValueFormat.getStdTopLevelResource(
					JsonOGEMAFileManagementData.class, appMan.getResourceManagement());

			fileMgmt = new JsonOGEMAFileManagementImpl<ProcTs3PersistentData, JsonOGEMAFileData>(
					mgmtRes, EvalResultManagementStd.FILE_PATH, appMan) {

				@Override
				protected Class<JsonOGEMAFileData> getDescriptorType() {
					return JsonOGEMAFileData.class;
				}
			};
		fileMgmt.setWorkspace(PROC3_WORKSPACE);	
		}		
	}
}
