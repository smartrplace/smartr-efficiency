package org.smartrplace.app.monbase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.externalviewer.extensions.DefaultDedicatedTSSessionConfiguration;
import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesWithFilters;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.Sensor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.widgets.reswidget.scheduleviewer.api.expert.ext.ScheduleViewerConfigurationBuilderExpert;
import org.smartrplace.app.monbase.gui.OfflineControlGUI;
import org.smartrplace.app.monbase.gui.ScheduleViewerOpenButtonDataProviderImpl;
import org.smartrplace.app.monbase.gui.TimeSeriesNameProviderImpl;
import org.smartrplace.app.monbase.gui.TimeSeriesServlet;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.monbase.alarming.AlarmingManagement;
import org.smartrplace.smarteff.defaultservice.TSManagementPage;
import org.sp.smarteff.monitoring.alarming.AlarmingEditPage;
import org.sp.smarteff.monitoring.alarming.AlarmingEditPage.AlarmingUpdater;
import org.sp.smarteff.monitoring.alarming.AlarmingUtil;
import org.sp.smarteff.monitoring.kpireporting.EvalProviderMonitoringBase;

import com.iee.app.evaluationofflinecontrol.OfflineEvalServiceAccessBase;
import com.iee.app.evaluationofflinecontrol.OfflineEvaluationControlController;
import com.iee.app.evaluationofflinecontrol.gui.KPIPageGWOverviewMultiKPI.PageConfig;
import com.iee.app.evaluationofflinecontrol.util.ExportBulkData.ComplexOptionDescription;
import com.iee.app.evaluationofflinecontrol.util.ScheduleViewerConfigProvEvalOff;
import com.iee.app.evaluationofflinecontrol.util.StandardConfigurations;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider.SessionConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilter;
import extensionmodel.smarteff.api.base.SmartEffUserData;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.monitoring.AlarmConfigBase;

// here the controller logic is implemented
public abstract class MonitoringController extends OfflineEvaluationControlController implements AlarmingUpdater {
	protected AlarmingManagement alarmMan = null;
	
	/** Implementation must be able to deal with a null value for locale
	 * TODO: getRoomLabel should have default implementation finding the room based on the resource structure
	 * */
	public abstract String getRoomLabel(String resLocation, OgemaLocale locale);
	
	/** Label to be shown for the room selection option indicating that all rooms are to
	 * be included into the plot*/
	public abstract String getAllRoomLabel(OgemaLocale locale);
	public abstract List<String> getAllRooms(OgemaLocale locale);
	
	/** This method is used to obtain any time series that are not delivered via the EvaluationProvider
	 * used. This includes any schedules including used for manual data entry, but also other
	 * ReadOnlyTimeseries.
	 * @label the relevant element of a value of the map returned by {@link #getDatatypesBase()}. The
	 * 		value must start with a hash('#'), but the hash sign is removed in the parameter
	 * @baseLabel the relevant key of the map returned by {@link #getDatatypesBase()}. This should be
	 *      a human readable label, but the real plot option used is determined by the label of
	 *      the TimeSeriesData returned by the method so that the implementation can determine the
	 *      label.
	 * @param return TimeSeriesData indicating the ReadOnlyTimeSeries as well as the label to be used.*/
	public abstract TimeSeriesData getManualDataEntrySchedule(String room, String label, String baseLabel);
	
	Map<String, List<String>> cachedDt = null;
	long lastCacheDtE = -1;
	/** you have to overwrite either {@link #getDatatypesBase()} or {@link #getDatatypesBaseExtended()}*/
	public Map<String, List<String>> getDatatypesBase() {return null;}
	/*public Map<String, List<String>> getDatatypesBase() {
		long now = appMan.getFrameworkTime();
		if(cachedDt == null || (now - lastCacheDtE > 10000)) {
			lastCacheDtE = now;
			cachedDt = new HashMap<>();
			for(Entry<String, List<ComplexOptionDescription>> base: getDatatypesBaseExtended().entrySet()) {
				cachedDt.put(base.getKey(), base.getValue());
			}
		}
		return cachedDt;		
	};*/

	/** you have to overwrite either {@link #getDatatypesBase()} or {@link #getDatatypesBaseExtended()}*/
	Map<String, List<ComplexOptionDescription>> cachedDtE = null;
	public Map<String, List<ComplexOptionDescription>> getDatatypesBaseExtended() {
		long now = appMan.getFrameworkTime();
		if(cachedDtE == null || (now - lastCacheDtE > 10000)) {
			lastCacheDtE = now;
			cachedDtE = new HashMap<>();
			for(Entry<String, List<String>> base: getDatatypesBase().entrySet()) {
				cachedDtE.put(base.getKey(), getComplexOptionsByString(base.getValue().toArray(new String[0])));
			}
		}
		return cachedDtE;
	}
	protected List<ComplexOptionDescription> getComplexOptionsByString(String... values) {
		List<ComplexOptionDescription> result = new ArrayList<>();
		for(String val: values) {
			result.add(new ComplexOptionDescription(val));
		}
		return result;
	}
	/*protected List<String> getStringsByComplexOptions(List<ComplexOptionDescription> values) {
		List<String> result = new ArrayList<>();
		for(ComplexOptionDescription val: values) {
			if(pathElement != null)
				result.add(val.pathElement);
			else for(String snippet: val.type.)
				result.add(val.type);
		}
		return result;
	}*/
	
	/** Plot types to be offered by the charting app
	 * @return The keys of the map indicate the label of each plot type (e.g.
	 * 		"All air quality values for the rooms selected"). The values of the map indicate the
	 * 		labels of the time series, these must be found as keys in {@link #getDatatypesBaseExtended()}.
	 * 		Keys ending on "##DAY" indicate that the input timeseries shall be aggregated
	 * 		to one value per day. "##METER" indicates that all timeseries shall be shown as meters starting
	 * 		with a common meter value at the reference data. See
	 * 		{@link TimeSeriesServlet#getMeterFromConsumption(ReadOnlyTimeSeries, long, long, org.smartrplace.app.monbase.gui.TimeSeriesServlet.MeterReference)}
	 * 		for details.<br>
	 * 		Such processing statements are removed for the label displayed. In this
	 * 		case for the time series processed additional information for each timeseries provided via
	 * 		{@link #getConfigParam(String)} may be relevant, see examples for more information.
	 */
	public abstract Map<String, List<String>> getComplexOptions();
	
	public String getDefaultComplexOptionKey() {
		return new ArrayList<>(getComplexOptions().keySet()).get(0);
	}
	
	/** The real rooms may have some type of hierarchy, so a timeseries may be plotted not only with
	 * a single room, but also when a super-room is selected (e.g. an entire floor may be offered as
	 * a room).<br>
	 * A default implementation for just using the representation of devices in rooms via location().room()
	 * is provided in the SRC Monitoring app.<br>
	 * TODO: Check if the default implementation can be made part
	 * of this base implementation.*/
	public abstract boolean isTimeSeriesInRoom(TimeSeriesData tsdBase, String room);
	
	/** A label added to all alarm messages generated by the app. The label is printed at the
	 * beginning of each message.
	 */
	public abstract String getAlarmingDomain();
	
	/** TODO: We should have a default implementation for this*/
	public abstract String getTsName(AlarmConfigBase ac);
	
	/** TODO: Maybe this should not be required to be implemented*/
	public abstract BuildingUnit getBuildingUnitByRoom(Sensor device, Room room, SmartEffUserData user);
	
	public abstract SensorDevice getHeatMeterDevice(String subPath);
	public abstract ElectricityConnection getElectrictiyMeterDevice(String subPath);
	
	/** GatewayIds relevant for this application. Only relevant on server. The implementation must
	 * be able to handle a null argument for the request.*/
	public abstract List<String> getGwIDs(OgemaHttpRequest req);
	public List<String> getGwIDsDefaultSelected(OgemaHttpRequest req) {
		List<String> all = getGwIDs(req);
		if(all.size() > 5)
			return Collections.emptyList();
		else
			return all;
	};
	
	//For manual alarming: TODO: explicit implementation should not be required, use complexOptions, DatatypesBase
	public abstract List<SmartEffTimeSeries> getManualTimeSeries(BuildingUnit bu);
	public abstract String getLabel(AlarmConfigBase ac, boolean isOverall);
	
	protected abstract void initManualResources();
	protected abstract boolean activateAlarming();
	/** TODO: We should have a default or template implementation for this, probably a Util class*/
	protected abstract void registerStaticTimeSeriesViewerLinks();
	
	/** Overwrite to provide configurations for certain time series*/
	public String getConfigParam(String tsLabel) {return null;}
	
	/** Method for OfflineControlGUI page, overwrite if additional buttons shall be added*/
	public StaticTable provideButtonTable(OfflineControlGUI page, Button closeTabButton, Button messageButton) {
		StaticTable buttonTable = new StaticTable(1,2);
		buttonTable.setContent(0, 0, closeTabButton).setContent(0, 1, messageButton);
		return buttonTable;
	}
	
    public static final String ALL_DATA = "All Data";
    public static final String ONE_DAY = "One Day";
    public static final String ONE_WEEK = "One Week";
    public static final String THREE_WEEKS = "Three Weeks";
    public static final String[] OPTIONS = {ALL_DATA, ONE_DAY, ONE_WEEK, THREE_WEEKS};
	/** Overwrite if necesary*/
    public String getAllDataLabel() {
    	return ALL_DATA;
    }
	/** Overwrite if necesary*/
	public String[] getIntervalOptions( ) {
		return OPTIONS;
	}
	public String getRoomOptionLineTitle() {
		return "Auswahl Räume";
	}
	public IntervalConfiguration getConfigDuration(String config, ApplicationManager appMan) {
    	switch(config) {
    	case ALL_DATA:
        	IntervalConfiguration r = new IntervalConfiguration();
			r.start = 0;
			//r.end = startEnd[1];
			r.end = appMan.getFrameworkTime();
        	return r;
		case ONE_DAY:
			r = new IntervalConfiguration();
			long now = appMan.getFrameworkTime();
			long startOfDay = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.DAY);
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startOfDay, -1, AbsoluteTiming.DAY);
			r.end = now;
			return r;
		case ONE_WEEK:
			r = new IntervalConfiguration();
			now = appMan.getFrameworkTime();
			startOfDay = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.DAY);
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startOfDay, -7, AbsoluteTiming.DAY);
			r.end = now;
			return r;
		case THREE_WEEKS:
			r = new IntervalConfiguration();
			now = appMan.getFrameworkTime();
			startOfDay = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.DAY);
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startOfDay, -21, AbsoluteTiming.DAY);
			r.end = now;
			return r;
    	}
    	return StandardConfigurations.getConfigDuration(config, appMan);
    }
	
	public MonitoringController(ApplicationManager appMan, OfflineEvalServiceAccessBase evaluationOCApp) {
		super(appMan, evaluationOCApp, null, new PageConfig(false, false), true);
		EvalProviderMonitoringBase.controller = this;
		evaluationOCApp.messageService().registerMessagingApp(appMan.getAppID(), getAlarmingDomain()+"_Alarming");
		if(activateAlarming()) {
			initManualResources();
			initAlarmingResources();
			registerStaticTimeSeriesViewerLinks();
			updateAlarming();
			AlarmingEditPage.alarmingUpdater = this;
			
			TSManagementPage.specialTSLabelProvider = new TSManagementPage.TSLabelProvider() {
				
				@Override
				public String provideLabel(SmartEffTimeSeries res) {
					String room = AlarmingUtil.getRoomNameFromSub(res);
					return room+"-"+ getScheduleLabel(res.getName(), res);
				}
			};
		}
	}
    
	public String getScheduleLabel(String resName, Resource fullResource) {
		String name = null;
		//first test for manual resources
		String entryName = "#"+resName;
		for(Entry<String, List<ComplexOptionDescription>> e: getDatatypesBaseExtended().entrySet()) {
			for(ComplexOptionDescription locPart: e.getValue()) {
				if(entryName.equals(locPart.pathElement)) {
					name = e.getKey();
					return name;
				}
			}
		}
		
		name = ResourceUtils.getHumanReadableShortName(fullResource);
		if(!name.equals("supervisedTS")) return name;
		return fullResource.getLocation();
	}

	protected void initAlarmingResources() {
		initAlarmingForSensors();
		InitUtil.initAlarmingForManual(this);
	}
	
	public void initAlarmingForSensors() {
		SmartEffUserDataNonEdit user = appMan.getResourceAccess().getResource("master");
		if(user == null) return;
		
		List<String> done = new ArrayList<>();
		Map<String, List<String>> roomSensors = new HashMap<>();
		System.out.println("In initAlarmingSensors: Processing "+getAllRooms(null).size()+ "Rooms!");
		for(String roomLoop: getAllRooms(null)) {
			ScheduleViewerOpenButtonDataProviderImpl allProv = new ScheduleViewerOpenButtonDataProviderImpl(this) {
				
				@Override
				protected List<String> getRoomIDs(OgemaHttpRequest req) {
					return Arrays.asList(new String[] {roomLoop});
				}
				
				@Override
				protected GaRoSingleEvalProvider getEvalProvider(OgemaHttpRequest req) {
					return controller.getDefaultProvider();
				}
				
				@Override
				protected String getDataType(OgemaHttpRequest req) {
					return "Alle(Ext)";
				}
			};
			List<TimeSeriesData> input = allProv.getData(null);
			
			System.out.println("Processing "+input.size()+ " timeseries for alarming configuration!");
			for(TimeSeriesData tsd: input) {
				if(!(tsd instanceof TimeSeriesDataImpl)) {
					System.out.println("Skipping as not TimeSeriesDataImpl:"+tsd.label(null));
					continue;
				}
				TimeSeriesDataImpl tsdi = (TimeSeriesDataImpl) tsd;
				ReadOnlyTimeSeries ts = tsdi.getTimeSeries();
				if(!(ts instanceof RecordedData)) {
					System.out.println("Skipping as not RecordedData:"+tsd.label(null)+ "Class:"+ts.getClass().getName());
					continue;
				}
				RecordedData rec = (RecordedData) ts;
				SingleValueResource reading = appMan.getResourceAccess().getResource(rec.getPath());
				if(reading == null) {
					System.out.println("Resource for RecordedData not found"+rec.getPath());
					log.warn("Resource for RecordedData not found"+rec.getPath());
					continue;
				}
				if(done.contains(reading.getLocation())) {
					System.out.println("Already in done:"+reading.getLocation());
					continue;
				} else done.add(reading.getLocation());
				System.out.println("Added to done:"+reading.getLocation());
				Sensor sensor = ResourceHelper.getFirstParentOfType(reading, Sensor.class);
				String room = getRoomLabel(rec.getPath(), null);
				BuildingUnit bu = InitUtil.getBuildingUnitByRoom(room, user.editableData());
				if(bu == null) {
					if(room.toLowerCase().equals("gesamt"))
						continue;
					bu = InitUtil.getBuildingUnitByRoom("Gesamt", user.editableData());
					if(bu == null)
						continue;
				}
				List<String> buSensors = roomSensors.get(bu.getLocation());
				if(buSensors == null) {
					buSensors = new ArrayList<>();
					roomSensors.put(bu.getLocation(), buSensors);
				}
				buSensors.add(sensor.getLocation());
				InitUtil.initAlarmForSensor(sensor, bu, user, this);
			}
		}

		//clean up sensor entries
		for(BuildingData build: user.editableData().buildingData().getAllElements()) {
			for(BuildingUnit bu: build.buildingUnit().getAllElements()) {
				@SuppressWarnings("unchecked")
				ResourceList<AlarmConfigBase> alarms = bu.getSubResource("alarmConfig", ResourceList.class);
				for(AlarmConfigBase ac: alarms.getAllElements()) {
					if(ac.supervisedTS().exists()) continue;
					if(!ac.supervisedSensor().exists()) continue;
					List<String> buSensors = roomSensors.get(bu.getLocation());
					if(buSensors == null) {
						ac.delete();
						continue;
					}
					if(!buSensors.contains(ac.supervisedSensor().getLocation())) {
						ac.delete();
					}
				}
			}
		}
		
		List<Sensor> allSensors = appMan.getResourceAccess().getResources(Sensor.class);
		
		//TODO: This is a workaround or may be extended in the future
		for(Sensor sens: allSensors) {
			if(!sens.reading().isActive()) continue;
			if(sens.isReference(false)) continue;
			String loc = sens.reading().getLocation();
			if(done.contains(loc)) continue;
			System.out.println("** For "+loc+" adding alarmStatus without configuration!");
			sens.addDecorator(AlarmingManagement.ALARMSTATUS_RES_NAME, IntegerResource.class).activate(false);
			done.add(loc);
		}
	}

	
	private List<GaRoSingleEvalProvider> getProviders() {
		//return controller.serviceAccess.getEvaluations().values();
		return 	serviceAccess.getEvaluations().values().stream()
				.filter(provider -> provider instanceof GaRoSingleEvalProvider)
				.map(provider -> (GaRoSingleEvalProvider) provider)
				.collect(Collectors.toList());

	}
	public GaRoSingleEvalProvider getDefaultProvider() {
		for(GaRoSingleEvalProvider p: getProviders()) {
			if(p.getClass().getSimpleName().startsWith("ExtendedTSQualityEvalProvider"))
				return p;
		}
		return getProviders().get(0);			
	}
	
	public void registerStaticTimeSeriesViewerLink(//long startTime, long endTime,
			TimeSeriesNameProviderImpl sprov,
			TimeSeriesData tsd,
			DefaultScheduleViewerConfigurationProviderExtended schedConfigProv) {
		List<TimeSeriesData> input = new ArrayList<>();
		input.add(tsd);
		registerStaticTimeSeriesViewerLink(//startTime, endTime,
				sprov, input, schedConfigProv, true);
	}
	public String registerStaticTimeSeriesViewerLink(//long startTime, long endTime,
			TimeSeriesNameProviderImpl sprov,
			List<TimeSeriesData> input,
			DefaultScheduleViewerConfigurationProviderExtended schedConfigProv,
			boolean writeLink) {
		TimeSeriesWithFilters filteringResult = ScheduleViewerOpenButtonEval.getTimeSeriesWithFilters(input,
				"Filter for "+getDefaultProvider().id(), sprov);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<TimeSeriesFilter> programsInner = (List)filteringResult.filters;
		//List<ReadOnlyTimeSeries> result = new ArrayList<>();

		List<Collection<TimeSeriesFilter>> programs = new ArrayList<>();
		programs.add(programsInner);
		
		String ci = addConfig(new DefaultDedicatedTSSessionConfiguration(filteringResult.timeSeries,
				null) {
			@Override
			public ScheduleViewerConfiguration viewerConfiguration() {
				long endTime = appMan.getFrameworkTime();
				long startTime = AbsoluteTimeHelper.getIntervalStart(endTime, AbsoluteTiming.DAY);
				startTime = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startTime, -7, AbsoluteTiming.DAY);
				ScheduleViewerConfiguration viewerConfiguration =
						ScheduleViewerConfigurationBuilderExpert.newBuilder().
						setDoScale(false).
						setPrograms(programs).
						setStartTime(startTime).setEndTime(endTime).setShowManipulator(false).
						setShowPlotTypeSelector(true).build();
				return viewerConfiguration;
			}
		}, schedConfigProv);
		if(writeLink)
			System.out.println("      For "+input.get(0).id()+"(#"+filteringResult.timeSeries.size()+"/"+input.size()+") added ts expert ID:"+ci+" providerID:"+ScheduleViewerConfigProvEvalOff.PROVIDER_ID);		
		return ci;
	}

	protected static String addConfig(SessionConfiguration sc, DefaultScheduleViewerConfigurationProviderExtended schedConfigProv) {
		String result = schedConfigProv.addConfig(sc);
		return result;
	}
	
	public void close() {
		if(alarmMan != null) {
			alarmMan.close();
			alarmMan = null;
		}
	}

	@Override
	public void updateAlarming() {
		if(alarmMan != null) {
			alarmMan.close();
		}
		List<AlarmConfigBase> configs = appMan.getResourceAccess().getResources(AlarmConfigBase.class);
		alarmMan = new AlarmingManagement(configs, this, getAlarmingDomain());
	}

	
}
