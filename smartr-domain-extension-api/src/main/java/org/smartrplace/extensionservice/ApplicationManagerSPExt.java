package org.smartrplace.extensionservice;

import java.util.List;

import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.driver.DriverProvider;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForEvaluation;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;

import de.iwes.timeseries.eval.api.configuration.Configuration;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;

public interface ApplicationManagerSPExt extends ApplicationManagerMinimal {
	public ExtensionGeneralData globalData();
	
	/** Get type declaration from extension resource type*/
	public <T extends Resource> ExtensionResourceTypeDeclaration<T> getTypeDeclaration(Class<? extends T> resourceType);
	
	/** Get all types declaring this type as parent or types from which parent is inherited*/
	public List<Class<? extends Resource>> getSubTypes(Class<? extends Resource> parentType);
	
	public List<ExtensionResourceTypeDeclaration<?>> getAllTypeDeclarations();
	/** System types that do not have an ExtensionResourceTypeDeclaration*/
	public List<Class<? extends Resource>> getSystemTypes();
	
	public OgemaLogger log();
	
	/**
	 * 
	 * @param eval
	 * @param entryResource
	 * @param userData
	 * @param userDataNonEdit
	 * @param drivers may be null, then all available drivers will be tested if they can provide
	 * 		data for the entryResource
	 * @return
	 * @deprecated moved to {@link ExtensionPageSystemAccessForEvaluation}
	 */
	/*@Deprecated
	public long[] calculateKPIs(GaRoSingleEvalProvider eval, Resource entryResource, List<Configuration<?>> configurations,
			Resource userData, ExtensionUserDataNonEdit userDataNonEdit,
			List<DriverProvider> drivers, boolean saveJsonResult,
			int defaultIntervalsToCalculate);
			*/
}
