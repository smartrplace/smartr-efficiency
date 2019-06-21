package org.sp.example.smartrheating;

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
import org.smartrplace.smarteff.util.NaviPageBase;
import org.sp.calculator.hpadapt.HPAdaptEditPage;
import org.sp.calculator.hpadapt.HPAdaptEval;
import org.sp.calculator.hpadapt.HPAdaptParamsPage;
import org.sp.calculator.hpadapt.HPAdaptResultPage;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.hpadapt.HPAdaptData;
import extensionmodel.smarteff.smartrheating.SmartrHeatingData;

@Service(SmartEffExtensionService.class)
@Component
public class SmartrHeatingExtension implements SmartEffExtensionService {
	//private ApplicationManagerSPExt appManExt;
	
	@Override
	public void start(ApplicationManagerSPExt appManExt) {
		//this.appManExt = appManExt;
		PROPOSAL_PROV = new SmartrHeatingEval(appManExt);
		PROPOSAL_PROV_HPADAPT = new HPAdaptEval(appManExt);
	}

	@Override
	public void stop(AppStopReason reason) {
	}

	private final static NaviPageBase<SmartrHeatingData>.Provider EDIT_PROVIDER = new SmartrHeatingEditPage().provider;
	private final static NaviPageBase<HPAdaptData>.Provider EDIT_PROVIDER_HPA = new HPAdaptEditPage().provider;
	//private final static EditPage RADIATOR_PAGE = new RadiatorTypeRegistration.EditPage();
	//private final static GenericResourceByTypeTablePageBase<SHeatRadiatorType> RADIATOR_TABLE = RADIATOR_PAGE.getTablePage();
	private static SmartrHeatingEval PROPOSAL_PROV;
	private static HPAdaptEval PROPOSAL_PROV_HPADAPT;
	private final static SmartrHeatingParamsPage PARAM_PAGE = new SmartrHeatingParamsPage();
	private final static SmartrHeatingInternalParamsPage PARAMINTERNAL_PAGE = new SmartrHeatingInternalParamsPage();
	private final static HPAdaptParamsPage PARAM_PAGE_HPA = new HPAdaptParamsPage();
	private final static HPAdaptResultPage RESULT_PAGE_HPADAPT = new HPAdaptResultPage();
	@Override
	public Collection<ExtensionCapability> getCapabilities() {
		return Arrays.asList(new ExtensionCapability[] {EDIT_PROVIDER,
				PROPOSAL_PROV, PARAM_PAGE.provider,
				//RADIATOR_TABLE.provider,
				PARAMINTERNAL_PAGE.provider,
				EDIT_PROVIDER_HPA,
				PROPOSAL_PROV_HPADAPT, PARAM_PAGE_HPA.provider, RESULT_PAGE_HPADAPT.provider
				});
	}
	//RADIATOR_PAGE.provider
	
	@Override
	public Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> resourcesDefined() {
		Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> result = 
				new ArrayList<>();
		result.add(new ExtensionResourceTypeDeclaration<SmartEffResource>() {

			@Override
			public Class<? extends SmartEffResource> dataType() {
				return SmartrHeatingData.class;
			}

			@Override
			public String label(OgemaLocale locale) {
				return "smartrHeatingData";
			}

			@Override
			public Class<? extends SmartEffResource> parentType() {
				return BuildingData.class;
			}

			@Override
			public Cardinality cardinality() {
				return Cardinality.SINGLE_VALUE_REQUIRED;
			}
			
		});
		//result.add(new RadiatorTypeRegistration.TypeDeclaration());
		result.add(PROPOSAL_PROV.getResultTypeDeclaration());
		if(PROPOSAL_PROV.getParamTypeDeclaration() != null) result.add(PROPOSAL_PROV.getParamTypeDeclaration());
		if(PROPOSAL_PROV.getInternalParamTypeDeclaration() != null) result.add(PROPOSAL_PROV.getInternalParamTypeDeclaration());
		
		result.add(new ExtensionResourceTypeDeclaration<SmartEffResource>() {

			@Override
			public Class<? extends SmartEffResource> dataType() {
				return HPAdaptData.class;
			}

			@Override
			public String label(OgemaLocale locale) {
				return "hpAdaptData";
			}

			@Override
			public Class<? extends SmartEffResource> parentType() {
				return BuildingData.class;
			}

			@Override
			public Cardinality cardinality() {
				return Cardinality.SINGLE_VALUE_REQUIRED;
			}
			
		});
		result.add(PROPOSAL_PROV_HPADAPT.getResultTypeDeclaration());
		if(PROPOSAL_PROV_HPADAPT.getParamTypeDeclaration() != null) result.add(PROPOSAL_PROV_HPADAPT.getParamTypeDeclaration());
		return result ;
	}
}
