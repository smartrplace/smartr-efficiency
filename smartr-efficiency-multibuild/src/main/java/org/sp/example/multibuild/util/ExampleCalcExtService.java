package org.sp.example.multibuild.util;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.smarteff.defaultservice.SmartEffExtServiceImpl;
import org.smartrplace.smarteff.util.LogicProviderBase;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;
import org.sp.calculator.multibuild.BuildingComponentParamsPage;
import org.sp.calculator.multibuild.BuildingComponentUsageEditPage;
import org.sp.calculator.multibuild.CommunicationBusTypetParamsPage;
import org.sp.calculator.multibuild.MultiBuildEditPage;
import org.sp.calculator.multibuild.MultiBuildEval;
import org.sp.calculator.multibuild.MultiBuildParamsPage;
import org.sp.calculator.multibuild.MultiBuildResultPage;

import extensionmodel.smarteff.multibuild.MultiBuildData;

@Service(SmartEffExtensionService.class)
@Component
public class ExampleCalcExtService extends SmartEffExtServiceImpl {
	
	@Override
	protected List<LogicProviderBase<?>> getProjectProviders() {
		return Arrays.asList(new LogicProviderBase[] { 
				PROPOSAL_PROV_MULTIBUILD});
	}
	@Override
	protected List<EditPageGeneric<?>> getEditPages() {
		return Arrays.asList(new EditPageGeneric[] {
				EDIT_PROVIDER_MULTIBUILD, PARAM_PAGE_MULTIBUILD, RESULT_PAGE_MULTIBUILD, PAGE_MULTIBUILD_1, PAGE_MULTIBUILD_2, PAGE_MULTIBUILD_3
		});
	}
	
	@Override
	public void start(ApplicationManagerSPExt appManExt) {
		//this.appManExt = appManExt;
		PROPOSAL_PROV_MULTIBUILD = new MultiBuildEval(appManExt);
	}

	private final static EditPageGeneric<MultiBuildData> EDIT_PROVIDER_MULTIBUILD = new MultiBuildEditPage();
	private static MultiBuildEval PROPOSAL_PROV_MULTIBUILD;
	private final static MultiBuildParamsPage PARAM_PAGE_MULTIBUILD = new MultiBuildParamsPage();
	private final static MultiBuildResultPage RESULT_PAGE_MULTIBUILD = new MultiBuildResultPage();
	private final static BuildingComponentParamsPage PAGE_MULTIBUILD_1 = new BuildingComponentParamsPage();
	private final static CommunicationBusTypetParamsPage PAGE_MULTIBUILD_2 = new CommunicationBusTypetParamsPage();
	private final static BuildingComponentUsageEditPage PAGE_MULTIBUILD_3 = new BuildingComponentUsageEditPage();
}
