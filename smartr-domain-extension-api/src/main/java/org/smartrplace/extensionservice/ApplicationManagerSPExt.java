package org.smartrplace.extensionservice;

import java.util.List;

import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;

public interface ApplicationManagerSPExt extends ApplicationManagerMinimal {
	public ExtensionGeneralData globalData();
	
	/** Get type declaration from extension resource type*/
	public <T extends Resource> ExtensionResourceTypeDeclaration<T> getTypeDeclaration(Class<? extends T> resourceType);
	
	/** Get all types declaring this type as parent or types from which parent is inherited*/
	public List<Class<? extends Resource>> getSubTypes(Class<? extends Resource> parentType);
	
	public List<ExtensionResourceTypeDeclaration<?>> getAllTypeDeclararions();
	
	public OgemaLogger log();
}
