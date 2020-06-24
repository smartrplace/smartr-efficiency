package org.smartrplace.external.accessadmin.gui;

import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;

public abstract class UserTaggedTbl {
	public static int indexCounter = 0;
	public UserTaggedTbl(String userName) {
		this.userName = userName;
		this.index = String.format("%011d", ++indexCounter);
	}

	public final String userName;
	public String index;
	
	public static class RoomTbl extends UserTaggedTbl {
		public final Room room;
		
		public RoomTbl(Room room, String userName) {
			super(userName);
			this.room = room;
		}
	}
	
	public static class RoomGroupTbl extends UserTaggedTbl {
		public final BuildingPropertyUnit roomGrp;
		
		public RoomGroupTbl(BuildingPropertyUnit roomGrp, String userName) {
			super(userName);
			String name = roomGrp.name().getValue();
			if(name.startsWith("New Room Group"))
				index = String.format("%011d", 0);
			this.roomGrp = roomGrp;
		}
	}

}
