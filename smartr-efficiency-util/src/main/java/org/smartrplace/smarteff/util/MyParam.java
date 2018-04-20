package org.smartrplace.smarteff.util;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.smartrplace.extensionservice.ExtensionUserData;

import de.iwes.util.resource.OGEMAResourceCopyHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import extensionmodel.smarteff.api.base.SmartEffUserData;

public class MyParam<T extends Resource> {
	private final T globalResource;
	//private final T myResource;
	//private final boolean useMyResource;
	private final Class<T> resourceType;
	
	private final ResourceList<Resource> tempData;
	//if myNewResource is not null the globalResource is to be used
	private final T tempResource;
	
	@SuppressWarnings("unchecked")
	public MyParam(T globalResource, T myResource, ExtensionUserData userData) {
		this.globalResource = globalResource;
		//this.myResource = myResource;
		if(!(userData instanceof SmartEffUserData)) throw new IllegalStateException("User Data must be from SmartEff!");
		this.tempData = ((SmartEffUserData) userData).temporaryResources();
		this.resourceType = (Class<T>) globalResource.getResourceType();
		
		if(myResource == null || (!myResource.isActive())) {
			tempResource = null;
			return;
		}
		boolean foundMy = false;
		for(Resource mySub: myResource.getSubResources(false)) {
			if(mySub.isActive()) {
				foundMy = true;				
			}
		}
		if(!foundMy) {
			tempResource = null;
			return;
		}
		String name = ResourceListHelper.createNewDecoratorName(globalResource.getName(), tempData);
		tempResource = (T) tempData.addDecorator(name, resourceType);
		//Now we have to merge globalResource and myResource
		OGEMAResourceCopyHelper.copySubResourceIntoDestination(tempResource, globalResource, null, true);
		OGEMAResourceCopyHelper.copySubResourceIntoDestination(tempResource, myResource, null, true);
	}
	
	public T get() {
		if(tempResource != null) return tempResource;
		else return globalResource;
	}

	public void close() {
		if((tempResource == null) || (!tempResource.exists())) return;
		if(tempResource != null) tempResource.delete();
	}

	@Override
	protected void finalize() throws Throwable {
		if((tempResource == null) || (!tempResource.exists())) return;
		System.out.println("MyParam object with tempResource "+tempResource.getLocation()+" was not closed correctly, now closed with object garbage collection!");
		close();
	}
}
