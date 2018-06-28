package org.smartrplace.smarteff.util;

import java.util.Arrays;
import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.proposal.CalculatedData;
import org.smartrplace.extensionservice.proposal.LogicProvider;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Standard template for LogicProvider implementations. Note that LogicProviders that
 * perform project planning usually use {@link ProjectProviderBase} as template.
 *
 * @param <T> Type of resource that is required as input. Note that this template in its standard
 * form is intended for LogicProviders that require only a single resource as input besides the
 * parameter resource that is not declared here. The parameter resource type is defined by
 * {@link #getParamType()}.<br>
 * Note that each calculation in method {@link #calculateProposal(SmartEffResource, CalculatedData, ExtensionResourceAccessInitData)}
 * usually takes 3 input: The input resource, the parameter resource that provides parameters that
 * may be a mix of public and private user parameters but that are not specific for a certain input
 * resource and the output resource that is filled by the calculation.
 */
public abstract class LogicProviderBase<T extends SmartEffResource>  implements LogicProvider {
	protected abstract void calculateProposal(T input, CalculatedData result, ExtensionResourceAccessInitData data);
	protected abstract Class<? extends CalculatedData> getResultType();
	protected abstract Class<T> typeClass();
	/** The ProjectLogicProvider shall put all its specific parameters into a
	 * single type. The type may also be used by other LogicProviders. Parameters
	 * from other sources may be used, but value initialization should not be
	 * required there.<br>
	 * If no own parameters need to be initialized this can be null. Otherwise a parameter resource is
	 * created in the {@link #init(ApplicationManagerSPExt)} method of this module and the implementation
	 * of {@link #initParams(SmartEffResource)} is called so that the resource can be filled with
	 * values. By default parameter resources are created in the global space, user specific
	 * adaptations that overwrite the global settings if present can be created via a page extending
	 * {@link EditPageGenericParams}.<br>
	 * If this type is provided and the parameter type is not registered as {@link ExtensionResourceTypeDeclaration}
	 * somewhere else then the respective ExtensionResourceTypeDeclaration is created automatically by this
	 * template class. The declaration can be obtained via {@link #getParamTypeDeclaration()} in order to be
	 * added to the {@link SmartEffExtensionService#resourcesDefined()} of the module. In most cases parameter
	 * resources do not have a parent and they are just appended to the editableData or globalData as parameters
	 * are not specific for a certain resources, but are values that are applied to all evaluations of a type. In
	 * some cases parameters still have a hierarchy, then the parent parameter type has to be given in
	 * {@link #getParentParamType()} in order to build the correct ExtensionResourceTypeDeclaration.
	 */
	public abstract Class<? extends SmartEffResource> getParamType();
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
		CalculatedData result = input.addDecorator(CapabilityHelper.getSingleResourceName(getResultType()), getResultType());
		calculateProposal(input, result, data);
		result.activate(true);
		return Arrays.asList(new CalculatedData[] {result});
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
		return LogicProviderBase.this.getClass().getName();
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
	
	public LogicProviderBase(ApplicationManagerSPExt appManExt) {
		this.appManExt = appManExt;
		resultTypeDeclaration = new ExtensionResourceTypeDeclaration<SmartEffResource>() {

			@SuppressWarnings("unchecked")
			@Override
			public Class<? extends SmartEffResource> dataType() {
				return (Class<? extends SmartEffResource>) getResultType();
			}

			@Override
			public String label(OgemaLocale locale) {
				return LogicProviderBase.this.label(locale)+" Result";
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
						return LogicProviderBase.this.label(locale)+" Parameters";
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
