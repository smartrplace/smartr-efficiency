package org.smartrplace.csv.download.generic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVPrinter;
import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceHandlerBase.DeviceByEndcodeResult;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.linkingresource.RoomHelper;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class CSVRoomExporter extends CSVExporter<Room> {
	private final boolean isFullExport;
	private final boolean exportInactiveAndNoHandlerResources;
	private final ApplicationManagerPlus appMan;
	
	/** Overwrite to implement filtering */
	protected List<Room> getRooms(OgemaHttpRequest req) {
		List<Room> all = KPIResourceAccess.getRealRooms(appMan.getResourceAccess());
		return all;
	}
	
	public CSVRoomExporter(boolean isFullExport, ApplicationManagerPlus appMan) {
		this(isFullExport, false, appMan);
	}
	public CSVRoomExporter(boolean isFullExport, boolean exportInactiveAndNoHandlerResources,
			ApplicationManagerPlus appMan) {
		super(null, appMan.appMan());
		this.isFullExport = isFullExport;
		this.exportInactiveAndNoHandlerResources = exportInactiveAndNoHandlerResources;
		this.appMan = appMan;
	}
	
	@Override
	protected Collection<String> printRow(Room room, CSVPrinter p) throws IOException {
		
		if(isFullExport && (room != null) && room.exists()) {
			List<String> done = new ArrayList<String>();
			List<PhysicalElement> devices = RoomHelper.getAllDevicesInRoom(room, appMan.getResourceAccess());
			if(!devices.isEmpty()) for(PhysicalElement dev: devices) {
				InstallAppDevice iad = appMan.dpService().getMangedDeviceResource(dev);
				done.add(dev.getLocation());
				
				if(!exportInactiveAndNoHandlerResources) {
					//exclude devices not relevant for device handlers
					DeviceByEndcodeResult<? extends PhysicalElement> devHandResult = DeviceHandlerBase.getDeviceHandler(dev, appMan);
					if(devHandResult == null)
						continue;
					if(iad == null) {
						printRow(room, null, dev, devHandResult.devHand.getDeviceTypeShortId(appMan.dpService()), p);
						continue;
					}
				}

				printRow(room, iad, dev, null, p);
			}
			return done;
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
			toPrint.add("");
		}		
		p.printRecord(toPrint);
		return null;
	}

	@Override
	protected void printFinal(CSVPrinter p, Set<String> done) throws IOException {
		@SuppressWarnings("rawtypes")
		Set<Class> typesDone = new HashSet<Class>();
		for(DeviceHandlerProviderDP<?> devHand: appMan.dpService().getDeviceHandlerProviders()) {
			if(!devHand.relevantForUsers())
				continue;
			@SuppressWarnings("unchecked")
			Class<? extends PhysicalElement> type = (Class<? extends PhysicalElement>) devHand.getResourceType();
			if(typesDone.contains(type))
				continue;
			typesDone.add(type);
			List<? extends PhysicalElement> devs = appMan.getResourceAccess().getResources(type);
			for(PhysicalElement dev: devs) {
				if(dev.location().room().exists())
					continue;
				if(done.contains(dev.getLocation()))
					continue;
				done.add(dev.getLocation());
				if(exportInactiveAndNoHandlerResources) {
					InstallAppDevice iad = appMan.dpService().getMangedDeviceResource(dev);
					printRow(null, iad, dev, null, p);
					continue;
				}
				DeviceByEndcodeResult<? extends PhysicalElement> devHandResult = DeviceHandlerBase.getDeviceHandler(dev, appMan);
				if(devHandResult == null)
					continue;
				InstallAppDevice iad = appMan.dpService().getMangedDeviceResource(dev);
				if(iad == null)
					printRow(null, null, dev, devHandResult.devHand.getDeviceTypeShortId(appMan.dpService()), p);
				else
					printRow(null, iad, dev, null, p);
			}
		}
	}
	
	protected void printRow(Room room, InstallAppDevice iad, PhysicalElement dev,
			String typeId, CSVPrinter p) throws IOException {
		
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
			if(typeId != null)
				toPrint.add(typeId);
			else
				toPrint.add("");
			toPrint.add("");			
		}
		if(room != null)
			toPrint.add(ResourceUtils.getHumanReadableShortName(room));
		else
			toPrint.add("");
		if(iad != null && iad.installationLocation().exists())
			toPrint.add(iad.installationLocation().getValue());
		else
			toPrint.add("");
		if(iad != null && iad.installationComment().exists())
			toPrint.add(iad.installationComment().getValue());
		else
			toPrint.add("");
		String endCode = DeviceHandlerBase.getDeviceEndCode(dev, appMan.dpService());
		toPrint.add("'"+endCode); //		toPrint.add("\""+endCode+"\"");
		toPrint.add("");
		toPrint.add(""+dev.isActive());
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
			toPrint.add("active");
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
