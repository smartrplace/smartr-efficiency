package org.smartrplace.smarteff.defaultservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.core.application.Application.AppStopReason;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration.Cardinality;
import org.smartrplace.extensionservice.proposal.LogicProvider;
import org.smartrplace.smarteff.util.LogicProviderBase;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericWithTable;
import org.smartrplace.util.format.ValueFormat;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;

public abstract class SmartEffExtServiceImpl implements SmartEffExtensionService {
	//private ApplicationManagerSPExt appManExt;
	
	/** ProjectProviders have to be constructed and put into static variables in start method*/
	protected abstract List<LogicProviderBase<?>> getProjectProviders();
	protected abstract List<EditPageGeneric<?>> getEditPages();
	
	@Override
	public void stop(AppStopReason reason) {
	}

	//protected final static List<EditPageGeneric<?>> editPages;
	protected List<ExtensionCapability> capList = null;
	protected List<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> resourcesDefined = null;
	
	@Override
	public Collection<ExtensionCapability> getCapabilities() {
		if(capList == null) {
			capList = new ArrayList<>();
			for(LogicProvider projProv: getProjectProviders()) {
				capList.add(projProv);
			}
			for(EditPageGeneric<?> editP: getEditPages()) {
				capList.add(editP.provider);
				if(editP instanceof EditPageGenericWithTable) {
					capList.add(((EditPageGenericWithTable<?>) editP).getTablePage().provider);
				}
			}
		}
		return capList;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> resourcesDefined() {
		if(resourcesDefined == null) {
			resourcesDefined = new ArrayList<>();
			Set<Class<? extends SmartEffResource>> done = new HashSet<>();
			for(LogicProviderBase<?> projProv: getProjectProviders()) {
				resourcesDefined.add(projProv.getResultTypeDeclaration());
				if(projProv.getParamTypeDeclaration() != null) {
					done.add(projProv.getParamTypeDeclaration().dataType());
					resourcesDefined.add(projProv.getParamTypeDeclaration());
				}
				if(projProv.getInternalParamTypeDeclaration() != null) {
					done.add(projProv.getInternalParamTypeDeclaration().dataType());
					resourcesDefined.add(projProv.getInternalParamTypeDeclaration());
				}
			}
			for(EditPageGeneric<?> editP: getEditPages()) {
				if(SmartEffResource.class.isAssignableFrom(editP.primaryEntryTypeClass())) {
					if(done.contains(editP.primaryEntryTypeClass())) {
						continue;
					}
					if(editP.getPrimarySuperType() != null) {
						addDataType(resourcesDefined, (Class<? extends SmartEffResource>) editP.primaryEntryTypeClass(),
								Cardinality.MULTIPLE_OPTIONAL, editP.getPrimarySuperType());
						if(!(editP instanceof EditPageGenericWithTable))
							System.out.println("  !!!!   WARNING: "+editP.primaryEntryTypeClass()+" is declared as sub type, but no table page declared automatically!");
					} else
						addDataType(resourcesDefined, (Class<? extends SmartEffResource>) editP.primaryEntryTypeClass());
				}
			}
		}
		return resourcesDefined;
		
	}
	
	protected void addDataType(Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> result,
			Class<? extends SmartEffResource> dataType) {
		addDataType(result, dataType, Cardinality.SINGLE_VALUE_REQUIRED, BuildingData.class);
	}
	protected void addDataType(Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> result,
			Class<? extends SmartEffResource> dataType, Cardinality cardinality,
			Class<? extends SmartEffResource> parentType) {
		result.add(new ExtensionResourceTypeDeclaration<SmartEffResource>() {

			@Override
			public Class<? extends SmartEffResource> dataType() {
				return dataType;
			}

			@Override
			public String label(OgemaLocale locale) {
				return ValueFormat.firstLowerCase(dataType.getSimpleName());
			}

			@Override
			public Class<? extends SmartEffResource> parentType() {
				return parentType;
			}

			@Override
			public Cardinality cardinality() {
				return cardinality;
			}
			
		});
		
	}
}
