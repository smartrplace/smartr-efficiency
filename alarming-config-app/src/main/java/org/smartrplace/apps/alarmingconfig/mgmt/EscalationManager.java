package org.smartrplace.apps.alarmingconfig.mgmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.AppID;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.ogema.devicefinder.api.TimedJobProvider;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationLevel;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationSettings;
import org.smartrplace.alarming.escalation.model.AlarmingMessagingApp;
import org.smartrplace.alarming.escalation.util.EscalationManagerI;
import org.smartrplace.alarming.escalation.util.EscalationProvider;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.alarmingconfig.escalationservices.ExternalEscalationProvider;
import org.smartrplace.apps.alarmingconfig.escalationservices.GatewayEscalationProvider;
import org.smartrplace.apps.alarmingconfig.escalationservices.NotAssignedEscalationProvider;
import org.smartrplace.apps.alarmingconfig.escalationservices.OnOffSwitchEscalationProvider;
import org.smartrplace.apps.alarmingconfig.escalationservices.ThermostatResetService;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class EscalationManager implements EscalationManagerI {
	protected final AlarmingConfigAppController controller;
	protected final AlarmingEscalationSettings escData;
	
	public final Map<String, EscalationProvider> knownEscProvs = new HashMap<>();
	public final Map<Integer, ExternalEscalationProvider> extEscProvs = new HashMap<>();
	
	@Override
	public void registerEscalationProvider(EscalationProvider prov) {
		knownEscProvs.put(prov.id(), prov);
		initTimedJob(prov);
	}

	@Override
	public EscalationProvider unregisterEscalationProvider(EscalationProvider prov) {
		//TODO: TimedJob is not unregistered or stopped yet
		return knownEscProvs.remove(prov.id());
	}

	public EscalationManager(AlarmingConfigAppController controller) {
		this.controller = controller;
		escData = controller.hwTableData.appConfigData.escalation();
		controller.dpService.addService(this);
		
		initStandardProviders();
		
		for(EscalationProvider prov: knownEscProvs.values()) {
			initTimedJob(prov);
		}
	}
	
	private void initTimedJob(EscalationProvider prov) {
		TimedJobProvider timedJobP = new TimedJobProvider() {
			AlarmingEscalationLevel persistData;
			
			@Override
			public String label(OgemaLocale locale) {
				return "TJ_"+prov.label(locale);
			}
			
			@Override
			public String id() {
				return prov.id();
			}
			
			@Override
			public boolean initConfigResource(TimedJobConfig config) {
				ValueResourceHelper.setIfNew(config.disable(), true);

				List<AlarmingEscalationLevel> lvs = config.getReferencingResources(AlarmingEscalationLevel.class);
				
				final AlarmingEscalationLevel levelRes;
				if(lvs.isEmpty() || (!((config.interval().getValue() > 0) && (config.alignedInterval().getValue() > 0)))) {
					levelRes = ResourceListHelper.getOrCreateNamedElement(prov.id(), escData.levelData());
					//ValueResourceHelper.setIfNew(levelRes.isProviderActive(), false);
					levelRes.timedJobData().setAsReference(config);
					prov.initConfig(levelRes);
				} else 	if(lvs.size() != 1)
					throw new IllegalStateException("For "+config.getLocation()+" found "+lvs.size()+" EscalationLevel references!");
				else
					levelRes = lvs.get(0);
				persistData = levelRes;
									
				List<InstallAppDevice> issueDevs = new ArrayList<>();
				for(InstallAppDevice iad: controller.hwTableData.appConfigData.knownDevices().getAllElements()) {
					if(iad.isTrash().getValue())
						continue;
					if(iad.knownFault().isActive())
						issueDevs.add(iad);
				}

				Boolean start = prov.initProvider(levelRes, escData, issueDevs);
				if(start != null)
					config.disable().setValue(!start);
				if(config.disable().getValue())
					return true;
				
				//levelRes.isProviderActive().setValue(start);
				return true;
			}
			
			@Override
			public String getInitVersion() {
				return "XXX";
			}
			
			@Override
			public void execute(long now, TimedJobMemoryData data) {
				List<AppID> appIDs = new ArrayList<>();
				for(AlarmingMessagingApp mapp: persistData.messagingApps().getAllElements()) {
					AppID appId = controller.getAppId(mapp);
					appIDs.add(appId);
				}
				prov.execute(now, data, appIDs);
			}
			
			@Override
			public int evalJobType() {
				return 0;
			}
		};
		controller.dpService.timedJobService().registerTimedJobProvider(timedJobP);		
	}
	
	public void knownIssueNotification(AlarmGroupData knownDeviceFault, String title, String message) {
		InstallAppDevice iad = knownDeviceFault.getParent();
		String text = StringFormatHelper.getTimeDateInLocalTimeZone(controller.appMan.getFrameworkTime())+" : "+title+
				"\r\n"+message;
		ValueResourceHelper.setCreate(knownDeviceFault.lastMessage(), text); //title+" :: "+message);
		for(EscalationProvider prov: knownEscProvs.values()) {
			prov.knownIssueNotification(iad);
		}
	}
	
	/** TODO: In the future these services shall be collected e.g. via OSGi services or DatapointService*/
	protected void initStandardProviders() {
		ThermostatResetService thermReset = new ThermostatResetService(controller.appManPlus, controller.getHardwareConfig());
		knownEscProvs.put(thermReset.id(), thermReset);
		
		NotAssignedEscalationProvider notAssigned = new NotAssignedEscalationProvider(controller.appManPlus, controller.getHardwareConfig());
		knownEscProvs.put(notAssigned.id(), notAssigned);

		OnOffSwitchEscalationProvider onOff = new OnOffSwitchEscalationProvider(controller.appManPlus);
		knownEscProvs.put(onOff.id(), onOff);

		GatewayEscalationProvider gateway = new GatewayEscalationProvider(controller.appManPlus);
		knownEscProvs.put(gateway.id(), gateway);

		ExternalEscalationProvider allFault = new ExternalEscalationProvider(controller.appManPlus, 
				"All Fault Signals", 0);
		knownEscProvs.put(allFault.id(), allFault);
		extEscProvs.put(allFault.index, allFault);
		
		ExternalEscalationProvider fireFault = new ExternalEscalationProvider(controller.appManPlus, 
				"Fire Fault Signals", 1);
		knownEscProvs.put(fireFault.id(), fireFault);
		extEscProvs.put(fireFault.index, fireFault);

		ExternalEscalationProvider heatingFault = new ExternalEscalationProvider(controller.appManPlus, 
				"Heating Fault Signals", 2);
		knownEscProvs.put(heatingFault.id(), heatingFault);
		extEscProvs.put(heatingFault.index, heatingFault);

		ExternalEscalationProvider waterFault = new ExternalEscalationProvider(controller.appManPlus, 
				"Water Fault Signals", 3);
		knownEscProvs.put(waterFault.id(), waterFault);
		extEscProvs.put(waterFault.index, waterFault);
		
		ExternalEscalationProvider ventilationFault = new ExternalEscalationProvider(controller.appManPlus, 
				"Ventilation/Cooling Fault Signals", 4);
		knownEscProvs.put(ventilationFault.id(), ventilationFault);
		extEscProvs.put(ventilationFault.index, ventilationFault);

		ExternalEscalationProvider centralControlFault = new ExternalEscalationProvider(controller.appManPlus, 
				"Central Control Fault Signals", 5);
		knownEscProvs.put(centralControlFault.id(), centralControlFault);
		extEscProvs.put(centralControlFault.index, centralControlFault);

		ExternalEscalationProvider otherFault = new ExternalEscalationProvider(controller.appManPlus, 
				"Backup Other Fault Signals", 6);
		knownEscProvs.put(otherFault.id(), otherFault);
		extEscProvs.put(otherFault.index, otherFault);
	}
}
