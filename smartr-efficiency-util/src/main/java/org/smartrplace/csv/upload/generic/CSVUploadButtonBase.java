package org.smartrplace.csv.upload.generic;

import org.smartrplace.extensionservice.gui.WidgetProvider.FileUploaderProtected;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.button.Button;

public abstract class CSVUploadButtonBase extends Button {
	protected Integer getSize(OgemaHttpRequest req) {return null;}
	//protected SmartEffTimeSeries parent;
	protected final FileUploaderProtected uploader;
	protected final Alert alert;
	
	protected abstract String getButtonText(OgemaHttpRequest req);
	
	public CSVUploadButtonBase(WidgetPage<?> page, String id, FileUploaderProtected uploader, Alert alert) {
		super(page, id);
		this.uploader = uploader;
		this.alert = alert;
    	uploader.registerTrigger(this); //triggerAction(upload, TriggeringAction.POST_REQUEST, TriggeredAction.POST_REQUEST);
		if(alert != null) triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	private static final long serialVersionUID = 1L;
	
	@Override
	public void onGET(OgemaHttpRequest req) {
		String text = getButtonText(req);
		setText(text, req);			
	}
	
	@Override
	public void onPrePOST(String data, OgemaHttpRequest req) {
		uploader.onPOST(req);
		if(alert != null) alert.showAlert("Started Upload!", true, req);		
	}
}
