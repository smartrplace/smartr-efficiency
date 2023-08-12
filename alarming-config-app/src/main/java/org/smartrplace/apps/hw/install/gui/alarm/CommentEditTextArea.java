package org.smartrplace.apps.hw.install.gui.alarm;

import org.ogema.core.model.simple.StringResource;
import org.ogema.model.extended.alarming.AlarmGroupData;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.textarea.TextArea;

@SuppressWarnings("serial")
public class CommentEditTextArea extends TextArea {

	private final AlarmGroupData alarm;
	
	public CommentEditTextArea(OgemaWidget parent, String id, OgemaHttpRequest req, AlarmGroupData alarm) {
		super(parent, id + "_comment", req);
		this.alarm = alarm;
		setDefaultRows(1);
		setDefaultCols(alarm.comment().isActive() && alarm.comment().getValue().length() >= 15 ? 15 : 10);
		this.triggerAction(this, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	
	@Override
	public void onGET(OgemaHttpRequest req) {
		if (alarm.comment().isActive()) {
			setText(alarm.comment().getValue(), req);
			setToolTip(alarm.comment().getValue(), req);
		} else {
			setText("", req);
			setToolTip("", req);
		}
	}
	
	@Override
	public void onPOSTComplete(String data, OgemaHttpRequest req) {
		data = getText(req).trim();
		if (data.isEmpty())
			alarm.comment().delete();
		else {
			alarm.comment().<StringResource> create().setValue(data);
			alarm.comment().activate(false);
		}
	}
	
}
