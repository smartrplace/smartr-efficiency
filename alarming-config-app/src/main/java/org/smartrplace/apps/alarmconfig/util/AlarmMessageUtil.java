package org.smartrplace.apps.alarmconfig.util;

public class AlarmMessageUtil {
	public static final boolean addAlarmDocLink = !Boolean.getBoolean("org.smartrplace.apps.alarmconfig.util.suppressAlarmDocLink");
	
	public static String getAlarmGuideLink(String alarmMessage) {
		for(AlarmType type: AlarmType.getKnownTypes()) {
			if(type.isMessageRelevant(alarmMessage))
				return type.getLink();
		}
		return null;
	}
}
