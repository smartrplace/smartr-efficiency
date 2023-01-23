package org.smartrplace.csv.download.generic;

import org.apache.commons.csv.CSVRecord;
import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceHandlerBase.DeviceByEndcodeResult;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.autoconfig.api.InitialConfig;
import org.smartrplace.csv.upload.generic.CSVUploadWidgets.CSVUploadListener;

import de.iwes.util.format.StringFormatHelper;
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
			DeviceByEndcodeResult<? extends PhysicalElement> deviceRes, String proposedDeviceId) {
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
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void newLineAvailable(String filePath, CSVRecord record, OgemaHttpRequest req) {
		System.out.println("  CSV::Processing Line "+record.getRecordNumber()+":"+StringFormatHelper.getListToPrint(record.toMap().values()));
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
		InstallAppDevice iad = null;
		if(!(deviceId.isEmpty() || typeId.isEmpty()))
			iad = InitialConfig.getDeviceByNumericalIdString(deviceId, typeId, hwInstallConfig, 0);

		//Check device
		DeviceByEndcodeResult<? extends PhysicalElement> device = null;
		String endCode = readLine(record, "serialEndCode");
		if(endCode != null && endCode.startsWith("'"))
			endCode = endCode.substring(1);
		String dbLocation = readLine(record, "dbLocation");
		if(!(endCode == null && dbLocation == null)) {
			String devHandId = readLine(record, "devHandId");
			String action = readLine(record, "action");
			Resource deviceRes = appMan.getResourceAccess().getResource(dbLocation);
			if(action.equalsIgnoreCase("delete") && (deviceRes != null)) {
				if(iad == null && (deviceRes instanceof PhysicalElement))
					iad = appMan.dpService().getMangedDeviceResource((PhysicalElement) deviceRes);
				DeviceTableRaw.deleteDeviceBase(deviceRes);
				if(iad != null)
					iad.delete();
				System.out.println("Delete(1) finished for "+deviceRes.getLocation());
				return;
			}
			if(devHandId != null && dbLocation != null) {
				DeviceHandlerProviderDP<? extends PhysicalElement> devHand = appMan.dpService().getDeviceHandlerProvider(devHandId);
				if(devHand != null && (devHand instanceof DeviceHandlerBase) &&
						deviceRes != null && (deviceRes instanceof PhysicalElement)) {
					device = new DeviceByEndcodeResult((PhysicalElement)deviceRes, (DeviceHandlerBase) devHand);
				} else
					device = getDevice(endCode, typeId, dbLocation);
			} else
				device = getDevice(endCode, typeId, dbLocation);
			if(device != null) {
				if(iad == null)
					iad = appMan.dpService().getMangedDeviceResource(device.device);
				if(action.equalsIgnoreCase("delete")) {
					DeviceTableRaw.deleteDeviceBase(deviceRes);
					if(iad != null)
						iad.delete();
					System.out.println("Delete(2) finished for "+device.device.getLocation());
					return;
				}
			}
		}
		if (device == null) {
			try {
				String roomName = record.get("Room");
				Room room = KPIResourceAccess.getRealRoomAlsoByLocation(roomName, appMan.getResourceAccess());
				if(room != null) {
					String roomTypeStr = record.get("RoomType");
					if(roomTypeStr != null) {
						int roomType = Integer.parseInt(roomTypeStr);
						ValueResourceHelper.setCreate(room.type(), roomType);
					}
				}
			} catch(IllegalArgumentException | NullPointerException e) {
				//no room
			}
			return;
		}
		if(iad == null) {
			try {
				final int numericId = Integer.parseInt(deviceId);
				if (numericId >= 10_000 || numericId < 0)
					throw new NumberFormatException();
				deviceId = String.format("%s-%04d", typeId, numericId); // cf. hardware-installation, LocalDeviceId#generateDeviceId()
			} catch (Exception e) {
				System.out.println("No deviceId from file:" + deviceId + ", type " + typeId);
				deviceId = typeId+"-"+deviceId;
			}
			//try to create IAD
			iad = createInstallAppDevice(endCode, typeId, device, deviceId);
			if(iad == null)
				return;
		} else if((!typeId.isEmpty()) && (!deviceId.isEmpty())) {
			ValueResourceHelper.setCreate(iad.deviceId(), typeId+"-"+deviceId);
		}
		String installationLocation = readLine(record, "Location (if known)");
		if(installationLocation != null)
			ValueResourceHelper.setCreate(iad.installationLocation(), installationLocation);
		String comment = readLine(record, "comment");
		if(comment != null)
			ValueResourceHelper.setCreate(iad.installationComment(), comment);
		String trash = readLine(record, "trash");
		if(trash != null && Boolean.parseBoolean(trash))
			ValueResourceHelper.setCreate(iad.isTrash(), true);
		try {
			String roomName = record.get("Room");
			Room room = KPIResourceAccess.getRealRoomAlsoByLocation(roomName, appMan.getResourceAccess());
			if(room != null) {
				iad.device().location().room().setAsReference(room);
				String roomTypeStr = record.get("RoomType");
				if(roomTypeStr != null) {
					int roomType = Integer.parseInt(roomTypeStr);
					ValueResourceHelper.setCreate(room.type(), roomType);
				}
			}
		} catch(IllegalArgumentException | NullPointerException e) {
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
