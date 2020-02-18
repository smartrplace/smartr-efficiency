package org.sp.smarteff.monbase.capability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application.AppStopReason;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.defaultservice.BaseDataService;
import org.smartrplace.smarteff.util.editgeneric.GenericResourceByTypeTablePageBase;
import org.sp.smarteff.monitoring.alarming.AlarmingEditPage;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.monitoring.AlarmConfigBase;

@Service(SmartEffExtensionService.class)
@Component
public class SPEvalDataService1 implements SmartEffExtensionService {
	public static final AlarmingEditPage ALARM_PAGE = new AlarmingEditPage();
	static final GenericResourceByTypeTablePageBase ALARM_TABLE = ALARM_PAGE.getTablePage();

	@Override
	public void start(ApplicationManagerSPExt appManExt) {
		//this.appManExt = appManExt;
	}

	@Override
	public void stop(AppStopReason reason) {
	}

	@Override
	public Collection<ExtensionCapability> getCapabilities() {
		return Arrays.asList(new ExtensionCapability[] {
				ALARM_PAGE.provider, ALARM_TABLE.provider});
	}

	public static class TypeDeclarationAlarm implements ExtensionResourceTypeDeclaration<SmartEffResource> {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return AlarmConfigBase.class;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "Alarm Config Base";
		}

		@Override
		public Class<? extends SmartEffResource> parentType() {
			return BuildingUnit.class;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.MULTIPLE_OPTIONAL;
		}
	}

	
	@Override
	public Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> resourcesDefined() {
		Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> result = 
				new ArrayList<>();
		result.add(new TypeDeclarationAlarm());
		//result.add(QUALITY_FB_TYPE);
		//dependencies
		result.add(BaseDataService.BUILDING_DATA);
		result.add(BaseDataService.ROOM_TYPE);
		return result ;
	}
}
