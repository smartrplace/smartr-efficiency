package org.smartrplace.smarteff.admin.timeseries;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.ogema.tools.timeseries.implementations.FloatTreeTimeSeries;
import org.ogema.tools.timeseriesimport.api.ImportConfiguration;
import org.ogema.tools.timeseriesimport.api.ImportConfigurationBuilder;
import org.ogema.tools.timeseriesimport.api.TimeseriesImport;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.extensionservice.driver.DriverProvider;
import org.smartrplace.smarteff.model.syste.GenericTSDPTimeSeries;
import org.smartrplace.smarteff.util.CapabilityHelper;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnit;

/** Default and example implementation of the DriverProvider interface
 * 
 */
public class GenericDriverProvider implements DriverProvider {
	public static final String SINGLE_COLUMN_CSV_ID = "SINGLE_COLUMN_CSV:";
	public static final String DEFAULT_TIMESTAMP_FORMAT = "d.M.y H:m"; //"d:m:yy h:mm";
	
	//private GenericTSDPConfig appConfigData;
	//private final ApplicationManager appMan;
	private final TimeseriesImport csvImport;

	private final Map<String, GaRoDataType> knownGaRoTypes;
	
	public GenericDriverProvider(TimeseriesImport csvImport, ApplicationManager appMan) {
		//this.appMan = appMan;
		this.csvImport = csvImport;
		initConfigurationResource();
		this.knownGaRoTypes = knownGaRoTypesStandard();
	}

	@Override
	public String label(OgemaLocale locale) {
		return "Generic Timeseries Driver";
	}
	
	@Override
	public String id() {
		return GenericDriverProvider.class.getName();
	}

	@Override
	public List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(BuildingData.class, BuildingUnit.class);
	}

	@Override
	public Class<? extends DataProvider<?>> getDataProviderType() {
		return SmartEffGaRoProviderTS.class;
	}

	@Override
	public void init(ApplicationManagerSPExt appManExt) {
	}

	@Override
	public DataProvider<?> getDataProvider(int entryTypeIdx, List<Resource> entryResources, Resource userData,
			ExtensionUserDataNonEdit userDataNonEdit) {
		if(entryTypeIdx == 0) {
			BuildingData[] inp = entryResources.toArray(new BuildingData[0]);
			return new SmartEffGaRoProviderTS(inp, this, knownGaRoTypes);
		} else {
			BuildingUnit[] inp = entryResources.toArray(new BuildingUnit[0]);
			return new SmartEffGaRoProviderTS(inp, this, knownGaRoTypes);			
		}
	}

	@Override
	public List<ReadOnlyTimeSeries> getTimeSeries(Resource entryResource, GenericDataTypeDeclaration dataType,
			Resource userData, ExtensionUserDataNonEdit userDataNonEdit) {
		List<ReadOnlyTimeSeries> result = new ArrayList<>();
		for(SmartEffTimeSeries dpTS: getTSConfigs(entryResource, dataType)) {
			result.add(getTimeSeries(dpTS));
		}
		return result;
	}

	@Override
	public ReadOnlyTimeSeries getTimeSeries(Resource entryResource, GenericDataTypeDeclaration dataType,
			String sourceId,
			Resource userData, ExtensionUserDataNonEdit userDataNonEdit) {
		SmartEffTimeSeries cr = getTSConfig(entryResource, dataType, sourceId);
		return getTimeSeries(cr);
	}

	public ReadOnlyTimeSeries getTimeSeries(SmartEffTimeSeries dpTS) {
		if(dpTS.schedule().isActive()) return dpTS.schedule();
		if(dpTS.recordedDataParent().isActive()) return dpTS.recordedDataParent().getHistoricalData();
		// must be file
		return AccessController.doPrivileged(new PrivilegedAction<ReadOnlyTimeSeries>() { public ReadOnlyTimeSeries run() {
			if(dpTS.fileType().getValue().startsWith(SINGLE_COLUMN_CSV_ID)) {
				String format = dpTS.fileType().getValue().substring(SINGLE_COLUMN_CSV_ID.length());
				SimpleDateFormat dateTimeFormat = new SimpleDateFormat(format);
				ImportConfiguration csvConfig = ImportConfigurationBuilder.newInstance().
						setDateTimeFormat(dateTimeFormat).
						setInterpolationMode(InterpolationMode.STEPS).setDelimiter(';').setDecimalSeparator(',').build();
				try {
					String[] paths = dpTS.filePaths().filePaths().getValues();
					if(paths.length == 0) return null;
					if(paths.length == 1)
						return csvImport.parseCsv(Paths.get(paths[0]), csvConfig);
					FloatTreeTimeSeries result = new FloatTreeTimeSeries();
					for(int i=0; i<paths.length; i++) {
						ReadOnlyTimeSeries add = csvImport.parseCsv(Paths.get(paths[1]), csvConfig);
						result.addValues(add.getValues(0));
					}
					return result;
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
			throw new IllegalStateException("Unsupported file type: "+dpTS.fileType().getValue());
		}});
	}

	/**
	 * 
	 * @param entryResource
	 * @param dataType
	 * @param sourceId may be null
	 * @param sched
	 */
	public void addSchedule(SmartEffTimeSeries dpTS, GenericDataTypeDeclaration dataType, 
			String sourceId, Schedule sched) {
		//SmartEffTimeSeries dpTS = getOrCreateTSConfig(entryResource, dataType, sourceId);
		if(!dpTS.schedule().equalsLocation(sched)) dpTS.schedule().setAsReference(sched);
	}
	public void addRecordedData(SmartEffTimeSeries dpTS, GenericDataTypeDeclaration dataType, 
			String sourceId, SingleValueResource recordedDataParent) {
		//SmartEffTimeSeries dpTS = getOrCreateTSConfig(entryResource, dataType, sourceId);
		if(!dpTS.recordedDataParent().equalsLocation(recordedDataParent)) dpTS.recordedDataParent().setAsReference(recordedDataParent);
	}
	public void addSingleColumnCSVFile(SmartEffTimeSeries dpTS, GenericDataTypeDeclaration dataType, 
			String sourceId, String filePath, String format) {
		AccessController.doPrivileged(new PrivilegedAction<Void>() { public Void run() {
			//SmartEffTimeSeries dpTS = getOrCreateTSConfig(entryResource, dataType, sourceId);
			if(!dpTS.filePaths().isActive()) {
				SmartEffUserDataNonEdit nonEdit = CapabilityHelper.getNonEditUserData(dpTS);
				GenericTSDPTimeSeries gdpTS = CapabilityHelper.addMultiTypeToList(nonEdit.configurationSpace(),
						null, GenericTSDPTimeSeries.class);
				gdpTS.filePaths().<StringArrayResource>create().setValues(new String[] {filePath});
				gdpTS.activate(true);
				dpTS.filePaths().setAsReference(gdpTS);
			} else ValueResourceUtils.appendValue(dpTS.filePaths().filePaths(), filePath);
			ValueResourceHelper.setCreate(dpTS.fileType(),
					SINGLE_COLUMN_CSV_ID+((format!=null)?format:DEFAULT_TIMESTAMP_FORMAT));
			return null;
		}});
	}

	public int getFileNum(SmartEffTimeSeries dpTS, 
			String sourceId) {
		//SmartEffTimeSeries dpTS = getTSConfig(entryResource, dataType, sourceId);
		if(dpTS == null || (!dpTS.isActive())) return 0;
		if(dpTS.filePaths().isActive()) return dpTS.filePaths().filePaths().size();
		return -1;
	}
	
    protected void initConfigurationResource() {
    	/*appConfigData = ValueFormat.getStdTopLevelResource(GenericTSDPConfig.class,
				appMan.getResourceManagement());
		if (appConfigData.isActive()) { // resource already exists (appears in case of non-clean start)
			appMan.getLogger().debug("{} started with previously-existing config resource", getClass().getName());
		}
		else {
			appConfigData.activate(true);
			appMan.getLogger().debug("{} started with new config resource", getClass().getName());
		}*/
    }

    protected SmartEffTimeSeries getTSConfig(Resource entryResource, GenericDataTypeDeclaration dataType,
    		String sourceId) {
		ResourceList<SmartEffTimeSeries> rlist = CapabilityHelper.getMultiTypeList(entryResource,
				SmartEffTimeSeries.class);

    	for(SmartEffTimeSeries config: rlist.getAllElements()) { //appConfigData.timeseries().getAllElements()) {
    		if(config.dataTypeId().getValue().equals(dataType.id())) {
    			if((!config.sourceId().isActive()) || (config.sourceId().getValue().equals(sourceId)))
    				return config;
    		}
    	}
    	return null;
    }
    protected List<SmartEffTimeSeries> getTSConfigs(Resource entryResource, GenericDataTypeDeclaration dataType) {
       	List<SmartEffTimeSeries> result = new ArrayList<>();
		//ResourceList<SmartEffTimeSeries> rlist = CapabilityHelper.getMultiTypeList(entryResource,
		//		SmartEffTimeSeries.class);
       	for(SmartEffTimeSeries config: entryResource.getSubResources(SmartEffTimeSeries.class, true)) { //rlist.getAllElements()) { //appConfigData.timeseries().getAllElements()) {
    		//if(config.entryResource().equalsLocation(entryResource)) {
    			if((dataType == null) || config.dataTypeId().getValue().equals(dataType.id())) {
    				result.add(config);
    			}
    		//}
    	}
 		return result;
    }
    
    protected SmartEffTimeSeries getOrCreateTSConfig(Resource entryResource, GenericDataTypeDeclaration dataType,
    		String sourceId) {
    	SmartEffTimeSeries result = getTSConfig(entryResource, dataType, sourceId);
    	if(result != null) return result;
    	result = CapabilityHelper.addMultiTypeToList(entryResource, null, SmartEffTimeSeries.class); //appConfigData.timeseries().add();
    	//result.entryResource().setAsReference(entryResource);
    	result.dataTypeId().<StringResource>create().setValue(dataType.id());
    	if(sourceId != null) result.sourceId().<StringResource>create().setValue(sourceId);
    	result.activate(true);
    	return result;
    }
    
    public static Map<String, GaRoDataType> knownGaRoTypesStandard() {
    	Map<String, GaRoDataType> result = new HashMap<>();
    	for(GaRoDataType type: GaRoDataType.standardTypes) {
    		result.put(type.id(), type);
    	}
		return result ;
    }
}
