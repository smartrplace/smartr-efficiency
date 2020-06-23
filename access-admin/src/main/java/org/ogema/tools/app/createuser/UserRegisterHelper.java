package org.ogema.tools.app.createuser;

import java.util.List;

import org.ogema.core.administration.UserAccount;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.object.widget.popup.ClosingPopup;
import de.iwes.widgets.object.widget.popup.ClosingPopup.ClosingMode;

public class UserRegisterHelper {
	public static interface NewUserHandler {
		void newUserCreated(UserAccount user, String fullName, OgemaHttpRequest req);
	}
	public static ClosingPopup<Object> registerUserAddPopup(WidgetPage<?> page, UserBuilder userBuilder,
			Alert alert, NewUserHandler newUserHandler) {
		final TextField textLoginName = new TextField(page, "textLoginName");
		final TextField textFullUserName = new TextField(page, "textFullUserName");
		final TextField textPw = new TextField(page, "textPw");
		final ClosingPopup<Object> addUserPop = new ClosingPopup<Object>(page, "addUserPop", true, ClosingMode.OK_CANCEL) {
			private static final long serialVersionUID = 1L;
			@Override
			public List<OgemaWidget> providePopupWidgets(OgemaHttpRequest req) {
				StaticTable dualTable = new StaticTable(3, 2);
				int c = 0;
				dualTable.setContent(c, 0, "Login name:").setContent(c, 1, textLoginName);
				c++;
				dualTable.setContent(c, 0, "Full User name:").setContent(c, 1, textFullUserName);
				c++; //3
				dualTable.setContent(c, 0, "Password:").setContent(c, 1, textPw);
				getPopupSnippet().append(dualTable, null);
				return null;
			}
			@Override
			public void onOK(Object selected, OgemaHttpRequest req) {
				try {
					String userName = textLoginName.getValue(req);
					String name = textFullUserName.getValue(req);
					UserAccount data = userBuilder.addUser(userName, textPw.getValue(req), false);
					if(data == null) {
						alert.showAlert("User name "+name+" could not be created, maybe already exists!", false, req);
						return;
					}
					
					if(newUserHandler != null) newUserHandler.newUserCreated(data, name, req);
					
					alert.showAlert("New user "+userName+" created. Please log out and login as the new user. "
							+ "This will be done automatically in the future (if not already available).",
							true, 60*60000, req);

					// XXX: Problem: If we were to just invalidate the session
					// right here, the GET to the alert would fail as it is
					// executed after the session invalidation.  With this
					// workaround, chances are the user will see the alert and
					// then get their session invalidated.  This is still far
					// from ideal though.
					req.getReq().getSession().setMaxInactiveInterval(1);
					//req.getReq().getSession().invalidate();

					//finally we should log out here ?
				} catch(Exception e) {
					e.printStackTrace();
					alert.showAlert("Creation of user failed", false, req);
				}
			}
		};
		addUserPop.okButton.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		page.append(addUserPop);
		return addUserPop;
	}
}
