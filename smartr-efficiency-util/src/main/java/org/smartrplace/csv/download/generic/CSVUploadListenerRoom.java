package org.smartrplace.csv.download.generic;

import org.apache.commons.csv.CSVRecord;
import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.devicefinder.util.DeviceHandlerBase.DeviceByEndcodeResult;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.autoconfig.api.InitialConfig;
import org.smartrplace.csv.upload.generic.CSVUploadWidgets.CSVUploadListener;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class CSVUploadListenerRoom implements CSVUploadListener {
	//Probably not thread-safe (if more than one upload at the same time)
	String previousType = null;
	private final ApplicationManagerPlus appMan;
	private final HardwareInstallConfig hwInstallConfig;
	
	/** Overwrite to provide InstallAppDevice via serial end code
	 * @param typeId 
	 * @param deviceLocation may be null if only serialEndCode and typeId is available*/
	protected DeviceByEndcodeResult<? extends PhysicalElement> getDevice(String serialEndCode, String typeId,
			String deviceLocation) {
		return null;
	};
	protected InstallAppDevice createInstallAppDevice(String serialEndCode, String typeId,
			DeviceByEndcodeResult<? extends PhysicalElement> deviceRes) {
		return null;
	}
	
	public CSVUploadListenerRoom(HardwareInstallConfig hwInstallConfig, ApplicationManagerPlus appMan) {
		this.appMan = appMan;
		this.hwInstallConfig = hwInstallConfig;
	}
	
	@Override
	public boolean fileUploaded(String filePath, OgemaHttpRequest req) {
		return true;
	}
	
	@Override
	public void newLineAvailable(String filePath, CSVRecord record, OgemaHttpRequest req) {
		String typeId =  readLine(record, "Type");
		if(typeId == null)
			typeId = previousType;
		else
			previousType = typeId;
		if(typeId == null)
			return;
		String deviceId = readLine(record, "DeviceId number");
		if(deviceId == null)
			deviceId = readLine(record, "ID");
		if(deviceId == null)
			return;
		InstallAppDevice iad = InitialConfig.getDeviceByNumericalIdString(deviceId, typeId, hwInstallConfig, 0);

		//Check device
		DeviceByEndcodeResult<? extends PhysicalElement> device = null;
		String endCode = readLine(record, "serialEndCode");
		String dbLocation = readLine(record, "dbLocation");
		if(!(endCode == null && dbLocation == null)) {
			device = getDevice(endCode, typeId, dbLocation);
			if(device != null) {
				String action = readLine(record, "action");
				if(action.equalsIgnoreCase("delete")) {
					device.device.delete();
					if(iad != null)
						iad.delete();
					return;
				}
			}
		}
		if (device == null)
			return;
		if(iad == null) {
			//try to create IAD
			iad = createInstallAppDevice(endCode, typeId, device);
			if(iad == null)
				return;
		}
		
		
		String installationLocation = readLine(record, "Location (if known)");
		if(installationLocation != null)
			ValueResourceHelper.setCreate(iad.installationLocation(), installationLocation);
		String comment = readLine(record, "comment");
		if(comment != null)
			ValueResourceHelper.setCreate(iad.installationComment(), comment);
		try {
			String roomName = record.get("Room");
			Room room = KPIResourceAccess.getRealRoomAlsoByLocation(roomName, appMan.getResourceAccess());
			if(room != null)
				iad.device().location().room().setAsReference(room);
		} catch(IllegalArgumentException e) {
			//no room
		}
	}
	
	protected String readLine(CSVRecord record, String col) {
		try {
			return record.get(col);
		} catch(IllegalArgumentException e) {
			return null;
		}
	}

}
