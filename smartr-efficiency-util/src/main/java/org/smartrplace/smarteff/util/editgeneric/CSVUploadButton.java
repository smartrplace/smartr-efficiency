package org.smartrplace.smarteff.util.editgeneric;

import java.util.HashMap;
import java.util.Map;

import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.extensionservice.gui.WidgetProvider.FileUploaderProtected;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.button.Button;

public class CSVUploadButton extends Button {
	protected Integer getSize(OgemaHttpRequest req) {return null;}
	//protected SmartEffTimeSeries parent;
	protected final FileUploaderProtected uploader;
	protected final Alert alert;
	
	public CSVUploadButton(WidgetPage<?> page, String id, FileUploaderProtected uploader, Alert alert) {
		super(page, id);
		this.uploader = uploader;
		this.alert = alert;
    	uploader.registerTrigger(this); //triggerAction(upload, TriggeringAction.POST_REQUEST, TriggeredAction.POST_REQUEST);
		if(alert != null) triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	private static final long serialVersionUID = 1L;
	
	public static final Map<OgemaLocale, String> BUTTON_TEXTS = new HashMap<>();
	static {
		BUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Upload CSV");
		BUTTON_TEXTS.put(OgemaLocale.GERMAN, "CSV Hochladen");
		BUTTON_TEXTS.put(OgemaLocale.FRENCH, "CSV");
	}

	@Override
	public void onGET(OgemaHttpRequest req) {
		String text;
		text = BUTTON_TEXTS.get(req.getLocale());
		if(text == null) text = BUTTON_TEXTS.get(OgemaLocale.ENGLISH);
		//Resource parent;
		Integer size = getSize(req);
		//int size = getSize(parent);
		if(size != null)
			setText(text+"("+size+")", req);			
		else setText(text, req);			
	}
	
	@Override
	public void onPrePOST(String data, OgemaHttpRequest req) {
		uploader.onPOST(req);
		if(alert != null) alert.showAlert("Started Upload!", true, req);		
	}
	
	public static int getSize(SmartEffTimeSeries myResource) {
		//Getting real information from the provider on data points may be too costly 
		return myResource.isActive()?1:0;
	}

}
