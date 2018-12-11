package org.sp.example.buildingwizard;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.button.AddEditButton;
import org.smartrplace.smarteff.util.button.ButtonControlProvider;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;
import extensionmodel.smarteff.api.common.BuildingUnit;

public abstract class AddEditButtonWizardList<T extends Resource> extends AddEditButton {
	private static final long serialVersionUID = 1L;

	protected abstract T getEntryResource(OgemaHttpRequest req);
	protected Boolean forceEnableState(OgemaHttpRequest req) {return null;}
	
	public static class WizBexRoomContext {
		List<BuildingUnit> allResource;
		int currentIndex;
	}
	
	public AddEditButtonWizardList(WidgetPage<?> page, String id, String pid,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider,
			boolean isBackButton) {
		super(page, id, pid, exPage, controlProvider);
		this.isBackButton = isBackButton;
	}

	protected final boolean isBackButton;
	
	private WizBexRoomContext getMyContext(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		Object ct = appData.getConfigInfo().context;
		if((ct != null) && (ct instanceof WizBexRoomContext)) {
			WizBexRoomContext wct = (WizBexRoomContext) ct;
			return wct;
		}
		
		T entryResource = getEntryResource(req);
		@SuppressWarnings("unchecked")
		ResourceList<BuildingUnit> sub = entryResource.getSubResource(CapabilityHelper.getSingleResourceName(
				BuildingUnit.class), ResourceList.class);
		WizBexRoomContext wct = new WizBexRoomContext();
		wct.allResource = sub.getAllElements();
		wct.currentIndex = 0;
		return wct;					
	};
	@Override
	public void onGET(OgemaHttpRequest req) {
		super.onGET(req);
		Boolean forceE = forceEnableState(req);
		if(forceE != null) {
			if(forceE) enable(req);
			else disable(req);
			return;
		}
		WizBexRoomContext ct = getMyContext(req);
		List<BuildingUnit> resList = ct.allResource;
		if((ct.currentIndex < 0) || (ct.currentIndex >= resList.size())) disable(req);
		else enable(req);
	}
	
	@Override
	protected Resource getResource(ExtensionResourceAccessInitData appData,
			OgemaHttpRequest req) {
		WizBexRoomContext ct = getMyContext(req);
		List<BuildingUnit> resList = ct.allResource;
		if((ct.currentIndex < 0) || (ct.currentIndex >= resList.size())) return null;
		else return ct.allResource.get(ct.currentIndex);
	}
	@Override
	protected Object getContext(ExtensionResourceAccessInitData appData, Resource object,
			OgemaHttpRequest req) {
		WizBexRoomContext ct = getMyContext(req);
		WizBexRoomContext newContext = new WizBexRoomContext();
		newContext.allResource = ct.allResource;
		if(isBackButton)
			newContext.currentIndex = ct.currentIndex - 1;
		else
			newContext.currentIndex = ct.currentIndex + 1;
		return newContext;
	}
}
