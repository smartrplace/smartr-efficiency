package org.smartrplace.smarteff.util;

import java.util.Arrays;
import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extenservice.proposal.ProjectProposal;
import org.smartrplace.extenservice.proposal.ProposalProvider;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public abstract class ProjectProviderBase<T extends SmartEffResource>  implements ProposalProvider {
	protected abstract void calculateProposal(T input, ProjectProposal result, ExtensionResourceAccessInitData data);
	protected abstract Class<? extends ProjectProposal> getResultType();
	protected abstract Class<T> typeClass();
	/** The ProjectProposalProvider shall put all its specific parameters into a
	 * single type. The type may also be used by other ProposalProviders. Parameters
	 * from other sources may be used, but value initialization should not be
	 * required there.<br>
	 * If no own parameters need to be initialized this can be null
	 */
	protected abstract Class<? extends SmartEffResource> getParamType();
	/** Return true if resource shall be activated*/
	protected boolean initParams(SmartEffResource params) {return false;};
	
	/**override this if the param type can be attached to another param type*/
	private Class<? extends SmartEffResource> getParentParamType() {
		return null;
	}
	
	/** Override these methods if more than one result shall be supported*/
	@Override
	public List<Resource> calculate(ExtensionResourceAccessInitData data) {
		T input = getReqData(data);
		ProjectProposal result = input.addDecorator(CapabilityHelper.getSingleResourceName(getResultType()), getResultType());
		calculateProposal(input, result, data);
		result.activate(true);
		return Arrays.asList(new ProjectProposal[] {result});
	}
	@Override
	public List<CalculationResultType> resultTypes() {
		return Arrays.asList(new CalculationResultType[] {
				new CalculationResultType(getResultType())});
	}

	
	//private Resource generalData;	
	protected ApplicationManagerSPExt appManExt;
	private ExtensionResourceTypeDeclaration<SmartEffResource> resultTypeDeclaration;
	private ExtensionResourceTypeDeclaration<SmartEffResource> paramTypeDeclaration;
	private boolean registerParamType = false;
	
	@Override
	public String id() {
		return ProjectProviderBase.this.getClass().getName();
	}

	@Override
	public void init(ApplicationManagerSPExt appManExt) {
		//ProjectProviderBase.this.appManExt = appManExt;
		//add param and init
		if(getParamType() != null) {
			SmartEffResource params = CapabilityHelper.getSubResourceSingle(appManExt.globalData(), getParamType(), appManExt);
			params.create();
			if(initParams(params)) params.activate(true);
		}
	}
	
	public ProjectProviderBase(ApplicationManagerSPExt appManExt) {
		this.appManExt = appManExt;
		resultTypeDeclaration = new ExtensionResourceTypeDeclaration<SmartEffResource>() {

			@SuppressWarnings("unchecked")
			@Override
			public Class<? extends SmartEffResource> dataType() {
				return (Class<? extends SmartEffResource>) getResultType();
			}

			@Override
			public String label(OgemaLocale locale) {
				return ProjectProviderBase.this.label(locale)+" Result";
			}

			@Override
			public Class<? extends SmartEffResource> parentType() {
				return typeClass();
			}

			@Override
			public Cardinality cardinality() {
				return Cardinality.SINGLE_VALUE_OPTIONAL;
			}
			
		};
		Class<? extends SmartEffResource> paramType = getParamType();
		if(paramType != null) {
			paramTypeDeclaration = appManExt.getTypeDeclaration(paramType);
			if(paramTypeDeclaration == null) {
				paramTypeDeclaration = new ExtensionResourceTypeDeclaration<SmartEffResource>() {

					@Override
					public Class<? extends SmartEffResource> dataType() {
						return paramType;
					}

					@Override
					public String label(OgemaLocale locale) {
						return ProjectProviderBase.this.label(locale)+" Parameters";
					}

					@Override
					public Class<? extends SmartEffResource> parentType() {
						return getParentParamType();
					}

					@Override
					public Cardinality cardinality() {
						return Cardinality.SINGLE_VALUE_OPTIONAL;
					}
					
				};
				registerParamType = true;
			}
		}
	}
	
	@Override
	public List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(typeClass());
	}

	@SuppressWarnings("unchecked")
	protected T getReqData(ExtensionResourceAccessInitData appData) {
		return (T) appData.entryResources().get(0);
	}
	
	protected String getUserName(ExtensionResourceAccessInitData appData) {
		return getReqData(appData).getParent().getParent().getName();
	}
	
	public ExtensionResourceTypeDeclaration<SmartEffResource> getTypeDeclaration() {
		return resultTypeDeclaration;
	}
	
	public ExtensionResourceTypeDeclaration<SmartEffResource> getParamTypeDeclaration() {
		if(registerParamType) return paramTypeDeclaration;
		return null;
	}
}
