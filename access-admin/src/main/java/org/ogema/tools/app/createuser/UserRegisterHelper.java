package org.ogema.tools.app.createuser;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accessadmin.api.util.UserPermissionUtil;
import org.ogema.core.administration.UserAccount;
import org.ogema.core.administration.UserConstants;
import org.ogema.model.locations.Room;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;

import de.iwes.widgets.api.extended.util.UserLocaleUtil;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.checkbox.Checkbox2;
import de.iwes.widgets.html.form.checkbox.DefaultCheckboxEntry;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.object.widget.popup.ClosingPopup;
import de.iwes.widgets.object.widget.popup.ClosingPopup.ClosingMode;
import de.iwes.widgets.template.DefaultDisplayTemplate;

@SuppressWarnings("serial")
public class UserRegisterHelper {
	public static interface NewUserHandler {
		void newUserCreated(UserAccount user, String fullName, OgemaHttpRequest req);
	}
	
	/** Provide popup to create new user
	 * 
	 * @param page
	 * @param userBuilder
	 * @param alert
	 * @param newUserHandler
	 * @param requestUserNameAsEmail if true only for an email address is asked. If not a valid email is detected a password is
	 * 		requested, then also non-email-address-user-names are accepted. Otherwise a login name is requested, but it is treated
	 * 		as the email address
	 * @return
	 */
	public static ClosingPopup<Object> registerUserAddPopup(WidgetPage<?> page, UserBuilder userBuilder,
			Alert alert, NewUserHandler newUserHandler, boolean requestUserNameAsEmail) {
		return registerUserAddPopup(page, userBuilder, alert, newUserHandler, requestUserNameAsEmail, false, null);
	}
	public static ClosingPopup<Object> registerUserAddPopup(WidgetPage<?> page, UserBuilder userBuilder,
			Alert alert, NewUserHandler newUserHandler, boolean requestUserNameAsEmail,
			boolean hasPrimaryRoomOptions, ApplicationManagerPlus appManPlus) {
		final TextField textLoginName = new TextField(page, "textLoginName");
		final TextField textFullUserName = new TextField(page, "textFullUserName");
		final TemplateDropdown<Room> dropPrimaryRoom;
		if(hasPrimaryRoomOptions) {
			dropPrimaryRoom = new TemplateDropdown<Room>(page, "dropPrimaryRoom") {
	        	@Override
	        	public void onGET(OgemaHttpRequest req) {
	        		Collection<? extends Room> primaryRoomOptions = KPIResourceAccess.getRealRooms(appManPlus.getResourceAccess());
					update(primaryRoomOptions , req);
	        		setAddEmptyOption(true, "No primary room", req);
	        	}
	        };
		} else {
			dropPrimaryRoom = null;
		}
		final TextField textPw = new TextField(page, "textPw") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String userName = textLoginName.getValue(req);
				if(isValidEmail(userName))
					disable(req);
				else
					enable(req);
			}
		};
		textLoginName.registerDependentWidget(textPw);
        //final TextField textEmail = new TextField(page, "textEmail");
        final Checkbox2 cbSendInvite = new Checkbox2(page, "cbInvite") {
        	@Override
        	public void onGET(OgemaHttpRequest req) {
        		if(userBuilder.enableEmailSending())
        			enable(req);
        		else
        			disable(req);
        	}
        };
        DefaultCheckboxEntry cbe = new DefaultCheckboxEntry("sendInvite", "", true);
        cbSendInvite.addDefaultEntry(cbe);
        Label inviteLabel = new Label(page, "inviteLabel") {
        	@Override
        	public void onGET(OgemaHttpRequest req) {
        		if(userBuilder.enableEmailSending())
        			setText("Send Invitation:", req);
        		else
        			setText("Send Invitation (disabled due to ongoing installation mode):", req);
        	}
        };
		
        String loginNameTitle = requestUserNameAsEmail?"Email:":"Login name:";
        
        final ClosingPopup<Object> addUserPop = new ClosingPopup<Object>(page, "addUserPop", true, ClosingMode.OK_CANCEL) {
			private static final long serialVersionUID = 1L;
			@Override
			public List<OgemaWidget> providePopupWidgets(OgemaHttpRequest req) {
				StaticTable dualTable = new StaticTable(5, 2);
				int c = 0;
				dualTable.setContent(c, 0, loginNameTitle).setContent(c, 1, textLoginName);
				c++;
				dualTable.setContent(c, 0, "Full User name:").setContent(c, 1, textFullUserName);
				c++; //3
		        if(hasPrimaryRoomOptions) {
			        dropPrimaryRoom.setTemplate(new DefaultDisplayTemplate<Room>() {
			        	@Override
			        	public String getLabel(Room object, OgemaLocale locale) {
			        		return ResourceUtils.getHumanReadableShortName(object);
			        	}
			        });
					dualTable.setContent(c, 0, "Select primary room:").setContent(c, 1, dropPrimaryRoom);
					c++; //3
		        }
				dualTable.setContent(c, 0, "Password:").setContent(c, 1, textPw);
                c++;
                dualTable.setContent(c, 0, inviteLabel).setContent(c, 1, cbSendInvite);
                //c++;
                //dualTable.setContent(c, 0, "Create mobile account:").setContent(c, 1, cbRest);
				getPopupSnippet().append(dualTable, null);
				return null;
			}
			@Override
			public void onOK(Object selected, OgemaHttpRequest req) {
				try {
					String userName = textLoginName.getValue(req);
					String name = textFullUserName.getValue(req);
					String password = textPw.getValue(req);
					UserAccount data = createAccount(userName, loginNameTitle, password, userBuilder,
							cbSendInvite.isChecked("sendInvite", req), false);
							//cbRest.isChecked("createRest", req));
					if(data == null) {
						alert.showAlert("User name "+name+" could not be created, maybe already exists!", false, req);
						return;
					}

					if(newUserHandler != null) newUserHandler.newUserCreated(data, name, req);
					
					if(hasPrimaryRoomOptions) {
		        		Room sel = dropPrimaryRoom.getSelectedItem(req);
		        		String nameAcc = AccessAdminConfig.class.getSimpleName().substring(0, 1).toLowerCase()+AccessAdminConfig.class.getSimpleName().substring(1);
		        		AccessAdminConfig appConfigData = appManPlus.getResourceAccess().getResource(nameAcc);
		        		AccessConfigUser accessConfig = UserPermissionUtil.getOrCreateUserPermissions(
		        				appConfigData.userPermissions(), userName);
		        		UserPermissionUtil.addPermission(sel.getLocation(),
		        				UserPermissionService.USER_PRIORITY_PERM, accessConfig.roompermissionData());
					}
					System.out.println("User account primary Room Setting finished :"+userName+" (in onOK)");
					
					//finally we should log out here ?
				} catch(Exception e) {
					e.printStackTrace();
					alert.showAlert("Creation of user failed", false, req);
				}
			}
		};
		addUserPop.okButton.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		addUserPop.okButton.setDefaultText("Create (be patient)");
		page.append(addUserPop);
		return addUserPop;
	}

	/** Create user account
	 * 
	 * @param userName
	 * @param realName
	 * @param password
	 * @param userBuilder
	 * @param sendEmailInvitation if userInvitiation is installed writing to SEND_INVITATION_BY_EMAIL triggers
	 * 		sending out an email invitation
	 * @param createRestAccount 
	 * @return
	 */
	public static UserAccount createAccount(String userName, String realName, String password, UserBuilder userBuilder,
			boolean sendEmailInvitation, boolean createRestAccount) {
		boolean hasEmail = isValidEmail(userName);
		System.out.println("User to be created :"+userName+" (in onOK)");
		UserAccount data = userBuilder.addUser(userName, password, false);
		System.out.println("User account adUser finished :"+userName+" (in onOK)");
		if(data == null) {
			return null;
		}
		if(hasEmail)
			data.getProperties().put(UserConstants.EMAIL, userName);
		UserLocaleUtil.setLocaleString(userName, UserLocaleUtil.getSystemDefaultLocaleString(), userBuilder.appMan);
        if(realName != null)
        	data.getProperties().put(UserConstants.FORMATTED_NAME, realName);
        System.out.println("User account propertySetting finished :"+userName+" (in onOK)");
        if (sendEmailInvitation && hasEmail) {
            data.getProperties().put(UserConstants.SEND_INVITATION_BY_EMAIL, "true");
			userBuilder.notifyEmailInvitationSentOut(userName);
       }
        if(createRestAccount) {
        	try {
        		String restUser = userName+"_rest";
        		createRESTAccount(restUser, restUser, userBuilder);
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
        }
		return data;
	}

	public static UserAccount createRESTAccount(String userName, String password, UserBuilder userBuilder) {
		UserAccount data = userBuilder.addUserAndInit(userName, password, false, false);
		return data;
	}
	
	public static boolean isValidEmail(String email) 
    { 
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\."+ 
                            "[a-zA-Z0-9_+&*-]+)*@" + 
                            "(?:[a-zA-Z0-9-]+\\.)+[a-z" + 
                            "A-Z]{2,7}$"; 
                              
        Pattern pat = Pattern.compile(emailRegex); 
        if (email == null) 
            return false; 
        return pat.matcher(email).matches(); 
    } 
}
