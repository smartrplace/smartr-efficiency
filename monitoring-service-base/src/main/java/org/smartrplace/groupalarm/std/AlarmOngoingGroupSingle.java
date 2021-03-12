package org.smartrplace.groupalarm.std;

import java.util.Arrays;
import java.util.Collection;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.AlarmOngoingGroup;
import org.ogema.devicefinder.api.AlarmingExtension;
import org.ogema.devicefinder.api.AlarmingGroupType;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmingData;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** For single alarms messaging is still done via the base message, so we do not take care of this here*/
@Deprecated
public class AlarmOngoingGroupSingle implements AlarmOngoingGroup {
	protected final AlarmConfiguration ac;
	protected final String subType;
	protected final long startTime;
	protected final AlarmingGroupType groupType;
	
	protected final DatapointService dpService;
	protected final ApplicationManager appMan;
	protected AlarmGroupData resource = null;
	
	protected boolean isFinished = false;
	
	public void setFinished() {
		isFinished = true;
	}
	
	/** Overwrite if appclicable*/
	@Override
	public AlarmingExtension sourceOfGroup() {
		return null;
	}
	
	public int getSubGroupsFound() {
		return isFinished?1:0;
	}
	
	public AlarmOngoingGroupSingle(AlarmConfiguration ac, String subType, long startTime,
			AlarmingGroupType groupType,
			ApplicationManagerPlus appManPlus) {
		this.ac = ac;
		this.subType = subType;
		this.startTime = startTime;
		this.groupType = groupType;
		this.dpService = appManPlus.dpService();
		this.appMan = appManPlus.appMan();
	}

	/** The id shall be the same for all occurences of the same type so that the resource for the group can be
	 * easily found
	 */
	@Override
	public String id() {
		if(subType != null)
			return ac.getLocation()+"_"+subType;
		return ac.getLocation(); //+"_"+StringFormatHelper.getFullTimeDateInLocalTimeZone(startTime);
	}

	@Override
	public String label(OgemaLocale locale) {
		return AlarmingConfigUtil.getDatapointLabel(ac, dpService)+"_"+StringFormatHelper.getFullTimeDateInLocalTimeZone(startTime);
	}

	@Override
	public Collection<AlarmConfiguration> baseAlarms() {
		return Arrays.asList(new AlarmConfiguration[] {ac});
	}

	@Override
	public AlarmGroupData getResource() {
		if(resource == null) {
			AlarmingData ad = ResourceHelper.getOrCreateTopLevelResource(AlarmingData.class, appMan);
			resource = ResourceListHelper.getOrCreateNamedElementFlex(id(), ad.ongoingGroups());
		}
		return resource;
	}

	@Override
	public AlarmingGroupType getType() {
		return groupType;
	}

	@Override
	public boolean isFinished() {
		return isFinished;
	}
}
