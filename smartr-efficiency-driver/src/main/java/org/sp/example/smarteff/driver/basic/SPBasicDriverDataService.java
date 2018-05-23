package org.sp.example.smarteff.driver.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.ogema.core.application.Application.AppStopReason;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.sp.example.smarteff.driver.basic.jaxb.DataDriverGaroJAXB;

import de.iwes.timeseries.eval.garo.api.jaxb.GaRoMultiEvalDataProviderJAXB;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.base.SmartEffConfigurationSpace;
import extensionmodel.smarteff.driver.basic.BasicGaRoDataProviderConfig;

public class SPBasicDriverDataService implements SmartEffExtensionService {
	//private ApplicationManagerSPExt appManExt;
	public SPBasicDriverDataService(GaRoMultiEvalDataProviderJAXB jaxbProvider) {
		DRIVER_PROVIDER = new DataDriverGaroJAXB(jaxbProvider);
	}
	
	public final static ExtensionResourceTypeDeclaration<SmartEffResource> CONFIG_DATA = new ExtensionResourceTypeDeclaration<SmartEffResource>() {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return BasicGaRoDataProviderConfig.class;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "GaRo Driver Configuration Data";
		}

		@Override
		public Class<? extends SmartEffResource> parentType() {
			return SmartEffConfigurationSpace.class;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.SINGLE_VALUE_OPTIONAL;
		}
	};
	/*public final static org.smartrplace.smarteff.defaultservice.BuildingTablePage.Provider BUILDING_NAVI_PROVIDER = new BuildingTablePage().provider;
	public final static org.smartrplace.smarteff.defaultservice.BuildingEditPage.Provider BUILDING_EDIT_PROVIDER = new BuildingEditPage().provider;
	*/
	public DataDriverGaroJAXB DRIVER_PROVIDER;

	@Override
	public void start(ApplicationManagerSPExt appManExt) {
		//this.appManExt = appManExt;
	}

	@Override
	public void stop(AppStopReason reason) {
	}

	@Override
	public Collection<ExtensionCapability> getCapabilities() {
		return Arrays.asList(new ExtensionCapability[] {DRIVER_PROVIDER});
	}

	@Override
	public Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> resourcesDefined() {
		Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> result = 
				new ArrayList<>();
		result.add(CONFIG_DATA);
		return result ;
	}
}
