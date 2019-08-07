package org.sp.example.smartrheating.util;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.smarteff.defaultservice.SmartEffExtServiceImpl;
import org.smartrplace.smarteff.util.LogicProviderBase;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;
import org.sp.calculator.hpadapt.HPAdaptEditPage;
import org.sp.calculator.hpadapt.HPAdaptEval;
import org.sp.calculator.hpadapt.HPAdaptParamsPage;
import org.sp.calculator.hpadapt.HPAdaptResultPage;
import org.sp.calculator.multibuild.BuildingComponentParamsPage;
import org.sp.calculator.multibuild.BuildingComponentUsageEditPage;
import org.sp.calculator.multibuild.CommunicationBusTypetParamsPage;
import org.sp.calculator.multibuild.MultiBuildEditPage;
import org.sp.calculator.multibuild.MultiBuildEval;
import org.sp.calculator.multibuild.MultiBuildParamsPage;
import org.sp.calculator.multibuild.MultiBuildResultPage;
import org.sp.calculator.smartrheating.intern.HPAdaptEvalInternal;
import org.sp.calculator.smartrheating.intern.HPAdaptParamsInternalPage;
import org.sp.example.smartrheating.SmartrHeatingEditPage;
import org.sp.example.smartrheating.SmartrHeatingEval;
import org.sp.example.smartrheating.SmartrHeatingInternalParamsPage;
import org.sp.example.smartrheating.SmartrHeatingParamsPage;

import extensionmodel.smarteff.hpadapt.HPAdaptData;
import extensionmodel.smarteff.multibuild.MultiBuildData;
import extensionmodel.smarteff.smartrheating.SmartrHeatingData;

@Service(SmartEffExtensionService.class)
@Component
public class ExampleCalcExtService extends SmartEffExtServiceImpl {
	//private ApplicationManagerSPExt appManExt;
	
	/* Usually only PROPOSAL_PROV_HPADAPTI would be registered, but to also show the complete open source example
	 * here we register both versions.
	*/
	@Override
	protected List<LogicProviderBase<?>> getProjectProviders() {
		return Arrays.asList(new LogicProviderBase[] {PROPOSAL_PROV, PROPOSAL_PROV_HPADAPT, 
				PROPOSAL_PROV_HPADAPTI, PROPOSAL_PROV_MULTIBUILD});
	}
	@Override
	protected List<EditPageGeneric<?>> getEditPages() {
		return Arrays.asList(new EditPageGeneric[] {
				EDIT_PROVIDER, PARAM_PAGE, PARAMINTERNAL_PAGE,
				EDIT_PROVIDER_HPA, PARAM_PAGE_HPA, PARAM_PAGE_HPAI, RESULT_PAGE_HPADAPT,
				EDIT_PROVIDER_MULTIBUILD, PARAM_PAGE_MULTIBUILD, RESULT_PAGE_MULTIBUILD, PAGE_MULTIBUILD_1, PAGE_MULTIBUILD_2, PAGE_MULTIBUILD_3
		});
	}
	
	@Override
	public void start(ApplicationManagerSPExt appManExt) {
		//this.appManExt = appManExt;
		PROPOSAL_PROV = new SmartrHeatingEval(appManExt);
		PROPOSAL_PROV_HPADAPT = new HPAdaptEval(appManExt);
		PROPOSAL_PROV_HPADAPTI = new HPAdaptEvalInternal(appManExt);
		PROPOSAL_PROV_MULTIBUILD = new MultiBuildEval(appManExt);
	}

	private final static EditPageGeneric<SmartrHeatingData> EDIT_PROVIDER = new SmartrHeatingEditPage();
	private final static EditPageGeneric<HPAdaptData> EDIT_PROVIDER_HPA = new HPAdaptEditPage();
	private final static EditPageGeneric<MultiBuildData> EDIT_PROVIDER_MULTIBUILD = new MultiBuildEditPage();
	//private final static EditPage RADIATOR_PAGE = new RadiatorTypeRegistration.EditPage();
	//private final static GenericResourceByTypeTablePageBase<SHeatRadiatorType> RADIATOR_TABLE = RADIATOR_PAGE.getTablePage();
	private static SmartrHeatingEval PROPOSAL_PROV;
	private static HPAdaptEval PROPOSAL_PROV_HPADAPT;
	private static HPAdaptEvalInternal PROPOSAL_PROV_HPADAPTI;
	private static MultiBuildEval PROPOSAL_PROV_MULTIBUILD;
	private final static SmartrHeatingParamsPage PARAM_PAGE = new SmartrHeatingParamsPage();
	private final static SmartrHeatingInternalParamsPage PARAMINTERNAL_PAGE = new SmartrHeatingInternalParamsPage();
	private final static HPAdaptParamsPage PARAM_PAGE_HPA = new HPAdaptParamsPage();
	private final static HPAdaptParamsInternalPage PARAM_PAGE_HPAI = new HPAdaptParamsInternalPage();
	private final static HPAdaptResultPage RESULT_PAGE_HPADAPT = new HPAdaptResultPage();
	private final static MultiBuildParamsPage PARAM_PAGE_MULTIBUILD = new MultiBuildParamsPage();
	private final static MultiBuildResultPage RESULT_PAGE_MULTIBUILD = new MultiBuildResultPage();
	private final static BuildingComponentParamsPage PAGE_MULTIBUILD_1 = new BuildingComponentParamsPage();
	private final static CommunicationBusTypetParamsPage PAGE_MULTIBUILD_2 = new CommunicationBusTypetParamsPage();
	private final static BuildingComponentUsageEditPage PAGE_MULTIBUILD_3 = new BuildingComponentUsageEditPage();
	
	/*@Override
	public Collection<ExtensionCapability> getCapabilities() {
		return Arrays.asList(new ExtensionCapability[] {EDIT_PROVIDER.provider,
				PROPOSAL_PROV, PARAM_PAGE.provider,
				//RADIATOR_TABLE.provider,
				PARAMINTERNAL_PAGE.provider,
				EDIT_PROVIDER_HPA.provider,
				EDIT_PROVIDER_MULTIBUILD.provider,
				PROPOSAL_PROV_HPADAPT, PARAM_PAGE_HPA.provider, RESULT_PAGE_HPADAPT.provider,
				PROPOSAL_PROV_MULTIBUILD, PARAM_PAGE_MULTIBUILD.provider, RESULT_PAGE_MULTIBUILD.provider,
				PAGE_MULTIBUILD_1.provider, PAGE_MULTIBUILD_2.provider, PAGE_MULTIBUILD_3.provider
				});
	}*/
	
	/*@Override
	public Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> resourcesDefined() {
		Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> result = 
				new ArrayList<>();
		addDataType(result, SmartrHeatingData.class);
		//result.add(new RadiatorTypeRegistration.TypeDeclaration());
		result.add(PROPOSAL_PROV.getResultTypeDeclaration());
		if(PROPOSAL_PROV.getParamTypeDeclaration() != null) result.add(PROPOSAL_PROV.getParamTypeDeclaration());
		if(PROPOSAL_PROV.getInternalParamTypeDeclaration() != null) result.add(PROPOSAL_PROV.getInternalParamTypeDeclaration());
		
		addDataType(result, HPAdaptData.class);
		addDataType(result, MultiBuildData.class);
		addDataType(result, BuildingComponent.class, Cardinality.MULTIPLE_OPTIONAL);
		addDataType(result, BuildingComponentUsage.class, Cardinality.MULTIPLE_OPTIONAL);
		addDataType(result, CommunicationBusType.class, Cardinality.MULTIPLE_OPTIONAL);
		result.add(PROPOSAL_PROV_HPADAPT.getResultTypeDeclaration());
		if(PROPOSAL_PROV_HPADAPT.getParamTypeDeclaration() != null) result.add(PROPOSAL_PROV_HPADAPT.getParamTypeDeclaration());
		result.add(PROPOSAL_PROV_MULTIBUILD.getResultTypeDeclaration());
		if(PROPOSAL_PROV_MULTIBUILD.getParamTypeDeclaration() != null) result.add(PROPOSAL_PROV_MULTIBUILD.getParamTypeDeclaration());
		return result ;
	}*/
}
