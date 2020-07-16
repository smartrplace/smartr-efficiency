package org.ogema.accessadmin.api.util;

import org.ogema.apps.roomlink.NewRoomPopupBuilder;
import org.ogema.apps.roomlink.localisation.mainpage.RoomLinkDictionary;
import org.ogema.apps.roomlink.localisation.mainpage.RoomLinkDictionary_de;
import org.ogema.apps.roomlink.localisation.mainpage.RoomLinkDictionary_en;
import org.ogema.apps.roomlink.localisation.mainpage.RoomLinkDictionary_fr;
import org.ogema.core.application.ApplicationManager;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.popup.Popup;

public class RoomEditHelper {
	public static void addButtonsToStaticTable(StaticTable stable, final WidgetPage<RoomLinkDictionary> widgetPage, Alert alert,
			ApplicationManager appManager, int tableRow, int firstCol) {
        /*Popup editRoomPopup = new Popup(widgetPage, "editRoomPopup", true);
        final Dropdown roomSelector = EditRoomPopupBuilder.addWidgets(widgetPage, editRoomPopup, alert, null, appManager);
        Button editRoomPopupTrigger =new Button(widgetPage, "editRoomPopupTrigger") {

			private static final long serialVersionUID = 1L;

			public void onGET(OgemaHttpRequest req) {
        		setText(widgetPage.getDictionary(req.getLocaleString()).editRoomButton(), req);
        	}
			
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				roomSelector.selectSingleOption(EditRoomPopupBuilder.emptyValue, req);
			}
        	
        };
        editRoomPopupTrigger.triggerAction(editRoomPopup, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
        editRoomPopupTrigger.triggerAction(editRoomPopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET);
        */
        Popup newRoomPopup = new Popup(widgetPage, "newRoomPopup", true);
        Button createBtn = NewRoomPopupBuilder.addWidgets(widgetPage, newRoomPopup, alert, appManager, false);
        //createBtn.triggerAction(ddAssign, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
        createBtn.triggerAction(newRoomPopup, TriggeringAction.POST_REQUEST, TriggeredAction.HIDE_WIDGET);
        Button newRoomPopupTrigger =new Button(widgetPage, "newRoomPopupTrigger") {

			private static final long serialVersionUID = 1L;

			public void onGET(OgemaHttpRequest req) {
        		setText(widgetPage.getDictionary(req.getLocaleString()).createRoomButton(), req);
        	}
        	
        };
        newRoomPopupTrigger.triggerAction(newRoomPopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET);
        
        //stable.setContent(tableRow, firstCol, editRoomPopupTrigger);
        stable.setContent(tableRow, firstCol+1, newRoomPopupTrigger);
        widgetPage.append(newRoomPopup).linebreak();
        //widgetPage.append(editRoomPopup).linebreak();
        widgetPage.registerLocalisation(RoomLinkDictionary_de.class).registerLocalisation(RoomLinkDictionary_en.class).registerLocalisation(RoomLinkDictionary_fr.class);		
	}
}
