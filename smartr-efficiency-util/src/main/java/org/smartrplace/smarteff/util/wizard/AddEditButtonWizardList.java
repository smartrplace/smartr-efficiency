package org.smartrplace.smarteff.util.wizard;

import java.util.Comparator;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.button.AddEditButton;
import org.smartrplace.smarteff.util.button.ButtonControlProvider;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public abstract class AddEditButtonWizardList<T extends Resource> extends AddEditButton {
	private static final long serialVersionUID = 1L;

	protected abstract Resource getEntryResource(OgemaHttpRequest req);
	protected abstract Class<T> getType();
	protected Boolean forceEnableState(OgemaHttpRequest req) {return null;}
	
	public class WizBexRoomContext {
		List<T> allResource;
		int currentIndex;
	}
	
	public AddEditButtonWizardList(WidgetPage<?> page, String id, String pid,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider,
			boolean isBackButton) {
		super(page, id, pid, exPage, controlProvider);
		this.isBackButton = isBackButton;
	}
	public AddEditButtonWizardList(OgemaWidget widget, String id, String pid,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider,
			boolean isBackButton, OgemaHttpRequest req) {
		super(widget, id, pid, exPage, controlProvider, req);
		this.isBackButton = isBackButton;
	}

	protected final boolean isBackButton;
	
	private WizBexRoomContext getMyContext(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		Object ct = appData.getConfigInfo().context;
		if((ct != null) && (ct instanceof AddEditButtonWizardList.WizBexRoomContext)) {
			@SuppressWarnings("unchecked")
			WizBexRoomContext wct = (WizBexRoomContext) ct;
			return wct;
		}
		
		Resource entryResource = getEntryResource(req);
		@SuppressWarnings("unchecked")
		ResourceList<T> sub = entryResource.getSubResource(CapabilityHelper.getSingleResourceName(
				getType()), ResourceList.class);
		WizBexRoomContext wct = new WizBexRoomContext();
		wct.allResource = sub.getAllElements();
		wct.allResource.sort(new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				IntegerResource subPos1 = o1.getSubResource("wizardPosition", IntegerResource.class);
				IntegerResource subPos2 = o2.getSubResource("wizardPosition", IntegerResource.class);
				return Integer.compare(subPos1.getValue(), subPos2.getValue());
			}
		});
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
		List<T> resList = ct.allResource;
		if((ct.currentIndex < 0) || (ct.currentIndex >= resList.size())) disable(req);
		else enable(req);
	}
	
	@Override
	protected Resource getResource(ExtensionResourceAccessInitData appData,
			OgemaHttpRequest req) {
		WizBexRoomContext ct = getMyContext(req);
		List<T> resList = ct.allResource;
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
