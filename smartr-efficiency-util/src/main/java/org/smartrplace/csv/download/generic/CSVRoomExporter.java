package org.smartrplace.csv.download.generic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.csv.CSVPrinter;
import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.csv.download.generic.CSVExporter;

import de.iwes.util.linkingresource.RoomHelper;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class CSVRoomExporter extends CSVExporter<Room> {
	private final boolean isFullExport;
	private final ApplicationManagerPlus appMan;
	
	/** Overwrite to implement filtering */
	protected List<Room> getRooms(OgemaHttpRequest req) {
		List<Room> all = KPIResourceAccess.getRealRooms(appMan.getResourceAccess());
		return all;
	}
	
	public CSVRoomExporter(boolean isFullExport, ApplicationManagerPlus appMan) {
		super(null, appMan.appMan());
		this.isFullExport = isFullExport;
		this.appMan = appMan;
	}
	
	@Override
	protected boolean printRow(Room room, CSVPrinter p) throws IOException {
		
		if(isFullExport && (room != null) && room.exists()) {
			List<PhysicalElement> devices = RoomHelper.getAllDevicesInRoom(room, appMan.getResourceAccess());
			if(!devices.isEmpty()) for(PhysicalElement dev: devices) {
				InstallAppDevice iad = appMan.dpService().getMangedDeviceResource(dev);
				printRow(room, iad, dev, p);
			}
			return true;
		}

		List<String> toPrint = new ArrayList<>();
		toPrint.add("");
		toPrint.add("");
		if(room != null && room.exists()) {
			toPrint.add(ResourceUtils.getHumanReadableShortName(room));
		} else {
			toPrint.add("");
		}
		toPrint.add("");
		toPrint.add("");
		if(isFullExport) {
			toPrint.add("");
			toPrint.add("");
			toPrint.add("");
		}		
		p.printRecord(toPrint);
		return true;
	}

	protected void printRow(Room room, InstallAppDevice iad, PhysicalElement dev, CSVPrinter p) throws IOException {
		
		List<String> toPrint = new ArrayList<>();
		if(iad != null) {
			String devId = iad.deviceId().getValue();
			int idx = devId.indexOf('-');
			String type;
			String id;
			if(idx < 1) {
				type = "";
				id = devId;
			} else {
				type = devId.substring(0, idx);
				if(idx < (devId.length()-1))
					id = devId.substring(idx+1);
				else
					id = "";
			}
			toPrint.add(type);
			toPrint.add(id);
		} else {
			toPrint.add("");
			toPrint.add("");			
		}
		toPrint.add(ResourceUtils.getHumanReadableShortName(room));
		if(iad != null && iad.installationLocation().exists())
			toPrint.add(iad.installationLocation().getValue());
		else
			toPrint.add("");
		if(iad != null && iad.installationComment().exists())
			toPrint.add(iad.installationComment().getValue());
		else
			toPrint.add("");
		String endCode = DeviceHandlerBase.getDeviceEndCode(dev, appMan.dpService());
		toPrint.add(endCode);
		toPrint.add("");
		toPrint.add(dev.getLocation());
		
		p.printRecord(toPrint);
	}

	@Override
	protected void printMainHeaderRow(CSVPrinter p) throws IOException {
		List<String> toPrint = new ArrayList<>();
		toPrint.add("Type");
		toPrint.add("ID");
		toPrint.add("Room");
		toPrint.add("Location (if known)");
		toPrint.add("comment");
		if(isFullExport) {
			toPrint.add("serialEndCode");
			toPrint.add("action");
			toPrint.add("dbLocation");
		}
		
		p.printRecord(toPrint);
	}

	@Override
	protected List<Room> getObjectsToExport(OgemaHttpRequest req) {
		List<Room> result = getRooms(req);
		result.sort(new Comparator<Room>() {
			@Override
			public int compare(Room o1, Room o2) {
				return ResourceUtils.getHumanReadableShortName(o1).compareTo(ResourceUtils.getHumanReadableName(o2));
			}
		});
		return result;
	}
}
