package org.sp.example.util.intern;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.smarteff.defaultservice.SmartEffExtServiceImpl;
import org.smartrplace.smarteff.util.LogicProviderBase;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;
import org.sp.calculator.hpadapt.intern.HPAdaptEvalInternal;
import org.sp.calculator.hpadapt.intern.HPAdaptParamsInternalPage;
import org.sp.calculator.smartrheating.intern.SmartrHeatingEvalInternal;
import org.sp.calculator.smartrheating.intern.SmartrHeatingInternalParamsPage;

@Service(SmartEffExtensionService.class)
@Component
public class ExampleCalcExtServiceIntern extends SmartEffExtServiceImpl {
	//private ApplicationManagerSPExt appManExt;
	
	/* Usually only PROPOSAL_PROV_HPADAPTI would be registered, but to also show the complete open source example
	 * here we register both versions.
	*/
	@Override
	protected List<LogicProviderBase<?>> getProjectProviders() {
		return Arrays.asList(new LogicProviderBase[] { 
				PROPOSAL_PROV_SMARTRHEAT, PROPOSAL_PROV_HPADAPTI});
	}
	@Override
	protected List<EditPageGeneric<?>> getEditPages() {
		return Arrays.asList(new EditPageGeneric[] {
				PARAMINTERNAL_PAGE,
				PARAM_PAGE_HPAI,
				
		});
	}
	
	@Override
	public void start(ApplicationManagerSPExt appManExt) {
		//this.appManExt = appManExt;
		PROPOSAL_PROV_HPADAPTI = new HPAdaptEvalInternal(appManExt);
		PROPOSAL_PROV_SMARTRHEAT = new SmartrHeatingEvalInternal(appManExt);
	}

	private static HPAdaptEvalInternal PROPOSAL_PROV_HPADAPTI;
	private static SmartrHeatingEvalInternal PROPOSAL_PROV_SMARTRHEAT;
	private final static SmartrHeatingInternalParamsPage PARAMINTERNAL_PAGE = new SmartrHeatingInternalParamsPage();
	private final static HPAdaptParamsInternalPage PARAM_PAGE_HPAI = new HPAdaptParamsInternalPage();
}
