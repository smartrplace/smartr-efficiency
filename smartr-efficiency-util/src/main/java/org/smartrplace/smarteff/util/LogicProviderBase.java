package org.smartrplace.smarteff.util;

import java.util.Arrays;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;
import org.smartrplace.extensionservice.proposal.CalculatedData;
import org.smartrplace.extensionservice.proposal.LogicProvider;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.defaultservice.LogicProvTablePage;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.AccessControl;

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
 * <br>
 * TIMESERIES EVALUATION INPUT CONCEPT<br>
 * There are currently 3 interfaces used for time series evaluation input type definition:
 * - {@link ResultType}: Standard evaluation input. Besides id and label that cannot be interpreted
 *    automatically this only contains the SingleValueResource-type that is expected as timeseries
 *    input. Scalar/Constant input is only supported via configurations. Furthermore this is usually
 *    limited to a certain EvaluationProvider.
 * - {@link GenericDataTypeDeclaration}: Here timeseries, constant value and structured input can
 *    be requested in extension to the definition of the input resource type. Also additional
 *    attributes can be given. Note that it does NOT extend ResultType as such instances are usually
 *    not limited to be used only by a single EvaluationProvider.
 * - {@link GaRoDataType}: This extends GenericDataTypeDeclaration. It provides additional documentation to
 *    really define a very specific input type in the GaRo context as two data sources that have
 *    the same GenericDataTypeDeclaration may still have different content. As it extends
 *    GenericDataTypeDeclaration also a GaRoDataType can be given in {@link EntryType#getType()}.
 */
public abstract class LogicProviderBase<T extends SmartEffResource>  implements LogicProvider {
	protected abstract void calculateProposal(T input, CalculatedData result, ExtensionResourceAccessInitData data);
	protected abstract Class<? extends CalculatedData> getResultType();
	
	/** Resource type providing the anchor for the input data to the evaluation. This typically is
	 * a sub resource in the tree below a building. By navigating through the data of the building
	 * also other data of this building can be accessed.<br>
	 * Note that general data not specific to the building evaluated shall be part of the parameter
	 * resource (see {@link #getParamType()}).
	 */
	protected abstract Class<T> typeClass();
	
	/** Documentation of super method in interface: The LogicProvider shall put all its primary specific parameters into a
	 * single type. The type may also be used by other LogicProviders. Parameters
	 * from other sources may be used, but value initialization should not be
	 * required from the provider. As parameters are not linked to the input resource they can be
	 * accessed via {@link CapabilityHelper#getMyParams(Class, org.smartrplace.extensionservice.ExtensionUserData, ApplicationManagerSPExt)},
	 * which can be done for any parameter resource type required and for an arbitrary number of parameter
	 * resource types. In the {@link LogicProvTablePage} only the parameter type declared can be
	 * edited, though. So for each parameter type used somehow has to be made sure that a navigation to
	 * creating and editing the resource type is provided. Initializing can be done in the method
	 * {@link #initParams(SmartEffResource)} as it is shown in the example SmartrHeatingEval. Editing
	 * can be done via the "Top Level config resources for master" page, which is a start page that
	 * e.g. can be opened via the Navigation page overview. It should probably by default be
	 * added to the standard pages offered in the navigation bar.<br>
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
	 * {@link #getParentParamType()} in order to build the correct ExtensionResourceTypeDeclaration.<br>
	 * If vendor-internal parameters are required declare the model in {@link #getInternalParamType()}.
	 * So in total four resource types are required to describe input (3x) and output (1x) in general.
	 * This assumes that vendor-internal parameters are only required from a single vendor.
	 */
	@Override
	public abstract Class<? extends SmartEffResource> getParamType();
	
	/**Override this if an internal parameter type that is not visible publicly shall be declared.
	 * The elements of this resouce can be added via the page "Top Level Config Overview Table",
	 * which can be accessed as global page from the "Navigation Page Overview" page. This page is
	 * only relevant for the master user and for users owning internalParamType pages. In
	 * the {@link #init(ApplicationManagerSPExt, ExtensionUserDataNonEdit)} method an
	 * AccessControl sub resource will be added allowing the logic provider to access the
	 * resource for all users.*/
	public Class<? extends SmartEffResource> getInternalParamType() { return null;}
	
	/** Return true if resource shall be activated*/
	protected boolean initParams(SmartEffResource params) {return false;};

	protected boolean initInternalParams(SmartEffResource params) {return false;};
	
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
	private ExtensionResourceTypeDeclaration<SmartEffResource> paramTypeDeclarationInternal;
	private boolean registerParamType = false;
	private boolean registerInternalParamType = false;
	
	@Override
	public String id() {
		return LogicProviderBase.this.getClass().getName();
	}

	@Override
	public void init(ApplicationManagerSPExt appManExt, ExtensionUserDataNonEdit userData) {
		//ProjectProviderBase.this.appManExt = appManExt;
		//add param and init
		if(getParamType() != null) {
			SmartEffResource params = CapabilityHelper.getSubResourceSingle(appManExt.globalData(), getParamType(), appManExt);
			params.create();
			if(initParams(params)) params.activate(true);
		}
		
		if(getInternalParamType() != null && userData != null) {
			SmartEffResource params = CapabilityHelper.getSubResourceSingle(userData.editableData(), getInternalParamType(), appManExt);
			params.create();
			if(initInternalParams(params)) params.activate(true);
			String moduleCl = this.getClass().getName();
			addAccessControlResource(params, moduleCl);
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
		
		Class<? extends SmartEffResource> paramTypeInternal = getInternalParamType();
		if(paramTypeInternal != null) {
			paramTypeDeclarationInternal = appManExt.getTypeDeclaration(paramTypeInternal);
			if(paramTypeDeclarationInternal== null) {
				paramTypeDeclarationInternal = new ExtensionResourceTypeDeclaration<SmartEffResource>() {

					@Override
					public Class<? extends SmartEffResource> dataType() {
						return paramTypeInternal;
					}

					@Override
					public String label(OgemaLocale locale) {
						return LogicProviderBase.this.label(locale)+" Internal Parameters";
					}

					@Override
					public Class<? extends SmartEffResource> parentType() {
						return null;
					}

					@Override
					public Cardinality cardinality() {
						return Cardinality.SINGLE_VALUE_OPTIONAL;
					}
					
				};
				registerInternalParamType = true;
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
	
	public ExtensionResourceTypeDeclaration<SmartEffResource> getResultTypeDeclaration() {
		return resultTypeDeclaration;
	}
	
	public ExtensionResourceTypeDeclaration<SmartEffResource> getParamTypeDeclaration() {
		if(registerParamType) return paramTypeDeclaration;
		return null;
	}
	
	public ExtensionResourceTypeDeclaration<SmartEffResource> getInternalParamTypeDeclaration() {
		if(registerInternalParamType) return paramTypeDeclarationInternal;
		return null;
	}
	
	/** Add user access control to a parameter resource. This is still an experimental
	 * concept and will need more development in the future.*/
	public static void addAccessControlResource(Resource params, String moduleClassName) {
		AccessControl sub = params.getSubResource("accessControl", AccessControl.class);
		if(!sub.isActive()) {
			sub.modules().create();
			ValueResourceUtils.appendValue(sub.modules(), moduleClassName);
			sub.activate(true);
		} else if(!ValueResourceUtils.contains(sub.modules(), moduleClassName)) {
			ValueResourceUtils.appendValue(sub.modules(), moduleClassName);
		}
	}
}
