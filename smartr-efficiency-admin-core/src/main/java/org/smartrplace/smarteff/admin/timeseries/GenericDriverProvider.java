package org.smartrplace.smarteff.admin.timeseries;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
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
import org.smartrplace.extensionservice.driver.DriverProvider;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.util.format.ValueFormat;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnitData;

public class GenericDriverProvider implements DriverProvider {
	public static final String SINGLE_COLUMN_CSV_ID = "SINGLE_COLUMN_CSV:";
	public static final String DEFAULT_TIMESTAMP_FORMAT = "d:m:yy h:mm";
	
	private GenericTSDPConfig appConfigData;
	private final ApplicationManager appMan;
	private final TimeseriesImport csvImport;

	private final Map<String, GaRoDataType> knownGaRoTypes;
	
	public GenericDriverProvider(TimeseriesImport csvImport, ApplicationManager appMan) {
		this.appMan = appMan;
		this.csvImport = csvImport;
		initConfigurationResource();
		this.knownGaRoTypes = knownGaRoTypesStandard();
	}

	@Override
	public String label(OgemaLocale locale) {
		return "Generic Timeseries Driver";
	}

	@Override
	public List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(BuildingData.class, BuildingUnitData.class);
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
			BuildingUnitData[] inp = entryResources.toArray(new BuildingUnitData[0]);
			return new SmartEffGaRoProviderTS(inp, this, knownGaRoTypes);			
		}
	}

	@Override
	public List<ReadOnlyTimeSeries> getTimeSeries(Resource entryResource, GenericDataTypeDeclaration dataType,
			Resource userData, ExtensionUserDataNonEdit userDataNonEdit) {
		List<ReadOnlyTimeSeries> result = new ArrayList<>();
		for(GenericTSDPTimeSeries dpTS: getTSConfigs(entryResource, dataType)) {
			result.add(getTimeSeries(dpTS));
		}
		return result;
	}
	
	protected ReadOnlyTimeSeries getTimeSeries(GenericTSDPTimeSeries dpTS) {
		if(dpTS.schedule().isActive()) return dpTS.schedule();
		if(dpTS.recordedDataParent().isActive()) return dpTS.recordedDataParent().getHistoricalData();
		// must be file
		if(dpTS.fileType().getValue().startsWith(SINGLE_COLUMN_CSV_ID)) {
			String format = dpTS.fileType().getValue().substring(SINGLE_COLUMN_CSV_ID.length());
			SimpleDateFormat dateTimeFormat = new SimpleDateFormat(format);
			ImportConfiguration csvConfig = ImportConfigurationBuilder.newInstance().
					setDateTimeFormat(dateTimeFormat).
					setInterpolationMode(InterpolationMode.STEPS).build();
			try {
				String[] paths = dpTS.filePaths().getValues();
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
	}

	/**
	 * 
	 * @param entryResource
	 * @param dataType
	 * @param sourceId may be null
	 * @param sched
	 */
	public void addSchedule(Resource entryResource, GenericDataTypeDeclaration dataType, 
			String sourceId, Schedule sched) {
		GenericTSDPTimeSeries dpTS = getOrCreateTSConfig(entryResource, dataType, sourceId);
		if(!dpTS.schedule().equalsLocation(sched)) dpTS.schedule().setAsReference(sched);
	}
	public void addRecordedData(Resource entryResource, GenericDataTypeDeclaration dataType, 
			String sourceId, SingleValueResource recordedDataParent) {
		GenericTSDPTimeSeries dpTS = getOrCreateTSConfig(entryResource, dataType, sourceId);
		if(!dpTS.recordedDataParent().equalsLocation(recordedDataParent)) dpTS.recordedDataParent().setAsReference(recordedDataParent);
	}
	public void addSingleColumnCSVFile(Resource entryResource, GenericDataTypeDeclaration dataType, 
			String sourceId, String filePath, String format) {
		GenericTSDPTimeSeries dpTS = getOrCreateTSConfig(entryResource, dataType, sourceId);
		if(!dpTS.filePaths().isActive()) {
			dpTS.filePaths().create();
			dpTS.filePaths().setValues(new String[] {filePath});
			dpTS.filePaths().activate(false);
		} else ValueResourceUtils.appendValue(dpTS.filePaths(), filePath);
		ValueResourceHelper.setCreate(dpTS.fileType(),
				SINGLE_COLUMN_CSV_ID+((format!=null)?format:DEFAULT_TIMESTAMP_FORMAT));
	}

	
    protected void initConfigurationResource() {
    	appConfigData = ValueFormat.getStdTopLevelResource(GenericTSDPConfig.class,
				appMan.getResourceManagement());
		if (appConfigData.isActive()) { // resource already exists (appears in case of non-clean start)
			appMan.getLogger().debug("{} started with previously-existing config resource", getClass().getName());
		}
		else {
			appConfigData.activate(true);
			appMan.getLogger().debug("{} started with new config resource", getClass().getName());
		}
    }

    protected GenericTSDPTimeSeries getTSConfig(Resource entryResource, GenericDataTypeDeclaration dataType,
    		String sourceId) {
    	for(GenericTSDPTimeSeries config: appConfigData.timeseries().getAllElements()) {
    		if(config.entryResource().equalsLocation(entryResource) &&
    				config.dataTypeId().getValue().equals(dataType.id())) {
    			if((!config.sourceId().isActive()) || (config.sourceId().getValue().equals(sourceId)))
    				return config;
    		}
    	}
    	return null;
    }
    protected List<GenericTSDPTimeSeries> getTSConfigs(Resource entryResource, GenericDataTypeDeclaration dataType) {
       	List<GenericTSDPTimeSeries> result = new ArrayList<>();
       	for(GenericTSDPTimeSeries config: appConfigData.timeseries().getAllElements()) {
    		if(config.entryResource().equalsLocation(entryResource)) {
    			if((dataType == null) || config.dataTypeId().getValue().equals(dataType.id())) {
    				result.add(config);
    			}
    		}
    	}
 		return result;
    }
    
    protected GenericTSDPTimeSeries getOrCreateTSConfig(Resource entryResource, GenericDataTypeDeclaration dataType,
    		String sourceId) {
    	GenericTSDPTimeSeries result = getTSConfig(entryResource, dataType, sourceId);
    	if(result != null) return result;
    	result = appConfigData.timeseries().add();
    	result.entryResource().setAsReference(entryResource);
    	result.dataTypeId().<StringResource>create().setValue(dataType.id());
    	result.sourceId().<StringResource>create().setValue(sourceId);
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
