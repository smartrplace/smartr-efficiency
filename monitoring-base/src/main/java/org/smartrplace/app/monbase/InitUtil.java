package org.smartrplace.app.monbase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.ogema.accesscontrol.RestAccess;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.security.WebAccessManager;
import org.ogema.model.sensors.Sensor;
import org.ogema.timeseries.eval.simple.mon.TimeSeriesServlet;
import org.smartrplace.app.monbase.servlet.ConsumptionEvalServlet;
import org.smartrplace.app.monbase.servlet.DatapointServlet;
import org.smartrplace.app.monbase.servlet.SensorServlet;
import org.smartrplace.app.monbase.servlet.TimeseriesBaseServlet;
import org.smartrplace.app.monbase.servlet.UserServletTestMon;
import org.smartrplace.app.monbase.servlet.UserServletTestMonAPI;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.monbase.alarming.AlarmingManagement;
import org.smartrplace.os.util.DirUtils;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric.DefaultSetModes;
import org.smartrplace.util.frontend.servlet.UserServlet;
import org.smartrplace.util.frontend.servlet.UserServletTest;
import org.sp.smarteff.monitoring.alarming.AlarmingEditPage;
import org.sp.smarteff.monitoring.alarming.AlarmingUtil;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.filedownload.FileDownloadServlet;
import de.iwes.widgets.html.fileupload.FileUploadServlet;
import de.iwes.widgets.html.fileupload.UploadState;
import extensionmodel.smarteff.api.base.SmartEffUserData;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.monitoring.AlarmConfigBase;

public class InitUtil {
	public static final File GENERIC_UPLOAD_FOR_WEB_PATH = new File("uploads");

	public static void initAlarmingForManual(MonitoringController controller) {
		SmartEffUserDataNonEdit user = controller.appMan.getResourceAccess().getResource("master");
		if(user == null) return;
		if(user.editableData().buildingData().size() != 1) return;
		ResourceList<BuildingUnit> buildings = user.editableData().buildingData().getAllElements().get(0).buildingUnit();
		for(BuildingUnit bu: buildings.getAllElements()) {
			@SuppressWarnings("unchecked")
			ResourceList<AlarmConfigBase> alarms = bu.getSubResource("alarmConfig", ResourceList.class);
			if(!alarms.exists()) {
				alarms.create();
				alarms.setElementType(AlarmConfigBase.class);
			} 
			//FIXME: This is a workaround because import from JSON does not work properly
			else if(alarms.getElementType() == null) {
				alarms.setElementType(AlarmConfigBase.class);				
			}
			StringResource manualAlarmNames = buildings.getSubResource("manualAlarmNames", StringResource.class);
			if(!manualAlarmNames.isActive()) {
				manualAlarmNames.create();
				manualAlarmNames.activate(true);
			}
			List<String> alarmNames = StringFormatHelper.getListFromString(manualAlarmNames.getValue());
			List<SmartEffTimeSeries> tss = controller.getManualTimeSeries(bu);
			if(tss != null) {
				for(SmartEffTimeSeries ts: tss) {
					AlarmConfigBase ac = AlarmingUtil.getAlarmConfig(alarms, ts);
					//Usually no alarming needs to be configured for manual data entry
					if(alarmNames.contains(ts.getName())) {
						if(ac == null) {
							ac = alarms.add();
							AlarmingEditPage.setDefaultValuesStatic(ac, DefaultSetModes.OVERWRITE);
							ac.supervisedTS().setAsReference(ts);
							ts.addDecorator(AlarmingManagement.ALARMSTATUS_RES_NAME, IntegerResource.class);
						}
						ValueResourceHelper.setCreate(ac.name(), controller.getLabel(ac, false));
					} else {
						if((ac != null) && (!ac.sendAlarm().getValue())) {
							ac.delete();
						}
					}
				}
			}
			if(!alarms.isActive()) alarms.activate(true);
			AlarmingUtil.cleanUpAlarmConfigs(alarms);
		}
	}
	
	public static BuildingUnit getBuildingUnitByRoom(final String roomName, SmartEffUserData user) {
		for(BuildingData build: user.buildingData().getAllElements()) {
			for(BuildingUnit buildUnit: build.buildingUnit().getAllElements()) {
				if(buildUnit.name().getValue().equals(roomName))
					return buildUnit;				
			}
		}
		return null;
	}

	/*public static void initAlarmForSensor(Sensor dev, Room room, SmartEffUserDataNonEdit user,
			MonitoringController controller) {
		BuildingUnit bu = controller.getBuildingUnitByRoom(dev, room, user.editableData());
		if(bu == null) {
			controller.appMan.getLogger().warn("No room found for device "+dev);
			return;
		}
		initAlarmForSensor(dev, bu, user, controller);
	}*/
	public static void initAlarmForSensor(Sensor dev, BuildingUnit bu,
			RoomLabelProvider roomLabelProv) {
		@SuppressWarnings("unchecked")
		ResourceList<AlarmConfigBase> alarms = bu.getSubResource("alarmConfig", ResourceList.class);
		alarms.create();
		if(alarms.getElementType() == null)
			alarms.setElementType(AlarmConfigBase.class);

		AlarmConfigBase ac = AlarmingUtil.getAlarmConfig(alarms, dev);
		if(ac == null) {
			ac = alarms.add();
			AlarmingEditPage.setDefaultValuesStatic(ac , DefaultSetModes.OVERWRITE);
			ac.supervisedSensor().setAsReference(dev);
			ac.activate(true);
			dev.addDecorator(AlarmingManagement.ALARMSTATUS_RES_NAME, IntegerResource.class).activate(false);
		}
		//ValueResourceHelper.setCreate(ac.name(), controller.getLabel(ac, bu.name().getValue().equals("gesamt")));
		if(ValueResourceHelper.setIfNew(ac.name(), roomLabelProv.getLabel(ac, bu.name().getValue().equals("gesamt"))))
			ac.activate(true);
	}
	
 	/** Call this from all applications implementing this bundle*/
 	public static boolean registerGenericMonitoringServlet(MonitoringController controller,
 			boolean includeSpecialTimeseriesServlet, RestAccess restAcc) {
 		if(UserServletTestMon.userServlet != null)
 			return false;
 		if(Boolean.getBoolean("org.ogema.impl.security.mobileloginvianaturaluser")) {
 			UserServletTest.userData = controller.appMan.getResourceAccess().getResource("userAdminData/userData");
 		}
		//register own servlet
		String userServletPath = MonitoringApp.urlPathServlet+"/userdata";
		String userServletPathAPI = MonitoringApp.urlPathServletAPIWeb;
		String genericFileUploadServletPath = "/upload";
		String genericFileDownloadServletPath = "/download/";
		UserServlet userServlet = registerServlet(userServletPath, controller, includeSpecialTimeseriesServlet);
		UserServletTestMon.userServlet = userServlet;
		UserServletTestMon.restAcc = restAcc;
		UserServlet userServletAPI = registerServlet(userServletPathAPI, controller, includeSpecialTimeseriesServlet);
		UserServletTestMonAPI.userServlet = userServletAPI;
		UserServletTestMonAPI.restAcc = restAcc;
		/*SensorServlet sensServlet = new SensorServlet(controller);
		userServlet.addPage("sensorsByRoom", sensServlet);
		TimeseriesBaseServlet timeSeriesServlet = new TimeseriesBaseServlet(controller);
		userServlet.addPage("timeseries", timeSeriesServlet);
		DatapointServlet dpServlet = new DatapointServlet(controller);
		userServlet.addPage("datapoints", dpServlet);
		if(includeSpecialTimeseriesServlet) {
			TimeSeriesServlet timeSeriesServletExt = new TimeSeriesServlet(controller.appMan);
			userServlet.addPage("timeseriesExtended", timeSeriesServletExt);
		}
		ConsumptionEvalServlet conEvalServlet = new ConsumptionEvalServlet(controller);
		userServlet.addPage("consumptionData", conEvalServlet);
		//controller.appMan.getWebAccessManager().registerWebResource(userServletPath, userServlet);
		controller.appMan.getWebAccessManager().registerWebResource(userServletPathAPI, userServlet);*/
		
		UploadState uploadListener = new UploadState() {
			
			@Override
			public void finished(FileItem item, OgemaHttpRequest req) {
				File destFile = new File(GENERIC_UPLOAD_FOR_WEB_PATH, item.getName());
				try {
					Files.copy(item.getInputStream(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					registerDownload(destFile, genericFileDownloadServletPath, controller.appMan.getWebAccessManager());
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		};
		FileUploadServlet fileUploadServlet = new FileUploadServlet(uploadListener, includeSpecialTimeseriesServlet, GENERIC_UPLOAD_FOR_WEB_PATH, null);
		controller.appMan.getWebAccessManager().registerWebResource(genericFileUploadServletPath, fileUploadServlet);
		registerDownloads(GENERIC_UPLOAD_FOR_WEB_PATH, genericFileDownloadServletPath, controller.appMan.getWebAccessManager());
		//controller.appMan.getWebAccessManager().registerWebResource("/uploadlic", "/license");
		//controller.appMan.getWebAccessManager().registerWebResource("/uploados", "os-config");
		//File locallic = controller.appMan.getDataFile("license");
		//controller.appMan.getWebAccessManager().registerWebResource("/locallic", locallic.getAbsolutePath());
		//System.out.println("Registered locallic for "+locallic.getAbsolutePath());
		return true;
 	}
 	
 	public static UserServlet registerServlet(String servletPath, MonitoringController controller,
 			boolean includeSpecialTimeseriesServlet) {
 		UserServlet userServlet = new UserServlet(servletPath, controller.appManPlusMon); //.getInstance();
 		SensorServlet sensServlet = new SensorServlet(controller);
		userServlet.addPage("sensorsByRoom", sensServlet);
		TimeseriesBaseServlet timeSeriesServlet = new TimeseriesBaseServlet(controller);
		userServlet.addPage("timeseries", timeSeriesServlet);
		DatapointServlet dpServlet = new DatapointServlet(controller);
		userServlet.addPage("datapoints", dpServlet);
		if(includeSpecialTimeseriesServlet) {
			TimeSeriesServlet timeSeriesServletExt = new TimeSeriesServlet(controller.appMan);
			userServlet.addPage("timeseriesExtended", timeSeriesServletExt);
		}
		ConsumptionEvalServlet conEvalServlet = new ConsumptionEvalServlet(controller);
		userServlet.addPage("consumptionData", conEvalServlet);
		controller.appMan.getWebAccessManager().registerWebResource(servletPath, userServlet);
 		return userServlet;
 	}
 	
 	public static void registerDownloads(File sourceDir, String baseServletPath, WebAccessManager webMan) {
 		DirUtils.makeSureDirExists(sourceDir.toPath());
 		Collection<File> allFiles = FileUtils.listFiles(sourceDir, FileFileFilter.FILE, null);
 		for(File file: allFiles) {
 			registerDownload(file, baseServletPath, webMan);
 		}
 	}
 	
 	public static void registerDownload(File file, String baseServletPath, WebAccessManager webMan) {
	 		FileDownloadServlet fds = new FileDownloadServlet(file, false, false);
	 		String servpath = baseServletPath+file.getName();
	 		webMan.registerWebResource(servpath, fds);
	 		System.out.println("Registered "+servpath+" for "+file.getAbsolutePath()); 		
 	}
}
