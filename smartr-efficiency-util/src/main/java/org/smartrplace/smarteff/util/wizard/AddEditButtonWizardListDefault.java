package org.smartrplace.smarteff.util.wizard;

import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.button.ButtonControlProvider;
import org.smartrplace.smarteff.util.editgeneric.DefaultWidgetProvider.SmartEffTimeSeriesWidgetContext;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

@Deprecated
public class AddEditButtonWizardListDefault<T extends Resource> extends AddEditButtonWizardList<T>{
	protected final String sub;
	
	public AddEditButtonWizardListDefault(WidgetPage<?> page, String id, String pid,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider, BACKTYPE isBackButton, ApplicationManagerMinimal appManExt,
			String sub) {
		super(page, id, pid, exPage, controlProvider, isBackButton, appManExt);
		this.sub = sub;
	}

	public AddEditButtonWizardListDefault(OgemaWidget widget, String id, String pid,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider, BACKTYPE isBackButton, ApplicationManagerMinimal appManExt,
			OgemaHttpRequest req, String sub) {
		super(widget, id, pid, exPage, controlProvider, isBackButton, appManExt, req);
		this.sub = sub;
	}

	private static final long serialVersionUID = 1L;

	@Override
	protected Resource getEntryResource(OgemaHttpRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Class<T> getType() {
		// TODO Auto-generated method stub
		return null;
	}

/*
	@Override
	protected Map<OgemaLocale, String> getButtonTexts(OgemaHttpRequest req) {
		switch(sub) {
		case WizBexWidgetProvider.ROOM_FIRST_LABEL_ID:
			return WizBexWidgetProvider.ROOMFIRSTBUTTON_TEXTS;
		case WizBexWidgetProvider.ROOM_SECOND_LABEL_ID:
			return WizBexWidgetProvider.ROOMFIRSTBUTTON_TEXTS;
		case WizBexWidgetProvider.ROOM_THIRD_LABEL_ID:
			return WizBexWidgetProvider.ROOMFIRSTBUTTON_TEXTS;
		case WizBexWidgetProvider.ROOM_NEXT_LABEL_ID:
			ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
			Resource res = getResource(appData, req);
			if(res == null || (!typeS().isAssignableFrom(res.getResourceType())))
				return WizBexWidgetProvider.ROOMFINALBUTTON_TEXTS;
			else
				return WizBexWidgetProvider.ROOMSTEPBUTTON_TEXTS;
		case WizBexWidgetProvider.ROOM_BACK_LABEL_ID:
			return WizBexWidgetProvider.ROOMBACKBUTTON_TEXTS;
		case WizBexWidgetProvider.ROOM_ENTRY_LABEL_ID:
			return WizBexWidgetProvider.ROOMENTRYBUTTON_TEXTS;
		//case ROOM_TABLE_LABEL_ID:
		//	return ROOMTABLEOPEN_BUTTON_TEXTS;
		default: throw new IllegalStateException("Unknown page sub ID:"+sub);
		}
	}
	@Override
	protected Boolean forceEnableState(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		if(getResource(appData, req) == null) return true;
		return null;
	}
	
	@Override
	protected Object getContext(ExtensionResourceAccessInitData appData, Resource object,
			OgemaHttpRequest req) {
		if(!typeS().isAssignableFrom(object.getResourceType())) return null;
		return super.getContext(appData, object, req);
	}
	
	@Override
	protected T getEntryResource(OgemaHttpRequest req) {
		T res = mhLoc.getGatewayInfo(req);
		//checkResource(res);
		return res;
	}
	@Override
	protected String getDestinationURL(ExtensionResourceAccessInitData appData, Resource object,
			OgemaHttpRequest req) {
		Resource res = getResource(appData, req);
		if(res == null)
			return entryPageUrl();
		else if(!typeS().isAssignableFrom(res.getResourceType()))
			 return editParentPageURL();
		else
			return editPageURL();
		//return editPageURL();
		//return SPPageUtil.getProviderURL(SPEvalDataService.WIZBEX_ROOM.provider);
	}
	
	/*@Override
	protected Class<S> getType() {
		return typeS();
	}
	
	@Override
	protected List<SmartEffTimeSeriesWidgetContext> tsCounters() {
		return tsCountersImpl();
	}*/
}
