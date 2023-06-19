package org.smartrplace.apps.hw.install.gui.alarm;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmGroupDataMajor;
import org.ogema.model.user.NaturalPerson;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmconfig.util.AlarmResourceUtil;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gateway.device.GatewaySuperiorData;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.alert.AlertData;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirmData;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.DynamicTableData;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownOption;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.form.textfield.TextFieldType;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.html.html5.flexbox.JustifyContent;
import de.iwes.widgets.html.popup.Popup;
import de.iwes.widgets.resource.widget.label.ValueResourceLabel;

public class ResponsibilityContactsPage {
	
	private static final List<DropdownOption> AGGREGATION_OPTIONS = Arrays.asList(
		new DropdownOption("none", "None", true),
		new DropdownOption("daily", "Daily", false)
	);
	
	private final WidgetPage<?> page;
	private final ApplicationManager appMan;
	private final Alert info;
	private final Alert alert;
	private final DynamicTable<NaturalPerson> contactsTable;
	private final ContactCreationPopup creationPopup;
	private final Button creationTriggerBtn;
	
	@SuppressWarnings("serial")
	public ResponsibilityContactsPage(WidgetPage<?> page, ApplicationManager appMan) {
		this.page = page;
		this.appMan = appMan;
		this.info = new Alert(page, "info", "This page displays a list of contacts that can be selected as responsibles for a device issue. "
				+ "In the OGEMA resource tree they are subresources of \"gatewaySuperiorDataRes/responsibilityContacts\".");
		info.setDefaultVisibility(true);
		info.setDefaultStyles(Collections.singleton(AlertData.BOOTSTRAP_INFO));
		this.alert = new Alert(page, "alert", false, "");
		this.contactsTable = new DynamicTable<NaturalPerson>(page, "contactsTable") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final GatewaySuperiorData supData = appMan.getResourceAccess().getResource("gatewaySuperiorDataRes");
				final ResourceList<NaturalPerson> contacts = supData != null && supData.exists() ? supData.responsibilityContacts() : null;
				this.updateRows(contacts != null ? contacts.getAllElements() : Collections.emptyList(), req);
			}
			
		};
		final RowTemplate<NaturalPerson> template = new RowTemplate<NaturalPerson>() {

			@Override
			public String getLineId(NaturalPerson person) {
				return ResourceUtils.getValidResourceName(person.getLocation());
			}
			
			@Override
			public Map<String, Object> getHeader() {
				final LinkedHashMap<String, Object> header = new LinkedHashMap<>();
				header.put("username", "User name");
				header.put("name", "Name");
				header.put("role", "User role");
				header.put("email", "Email");
				header.put("assigned", "Issues assigned");
				header.put("aggregation", "Email aggregation");
				header.put("delete", "Delete");
				return header;
			}
			
			
			@Override
			public Row addRow(NaturalPerson person, OgemaHttpRequest req) {
				final Row row = new Row();
				final String line = getLineId(person);
				final ValueResourceLabel<StringResource> userName = new ValueResourceLabel<>(contactsTable, line + "_username", req);
				userName.selectDefaultItem(person.userName());
				row.addCell("username", userName);
				row.addCell("name", new Label(contactsTable, line + "_name", req) {
					@Override
					public void onGET(OgemaHttpRequest req) {
						setText(name(person), req);
					}
				});
				final ValueResourceLabel<StringResource> userRole = new ValueResourceLabel<>(contactsTable, line + "_role", req);
				userRole.selectDefaultItem(person.userRole());
				row.addCell("role", userRole);
				
				final ValueResourceLabel<StringResource> email = new ValueResourceLabel<>(contactsTable, line + "_email", req);
				email.selectDefaultItem(person.getSubResource("emailAddress", StringResource.class));
				row.addCell("email", email);
				
				final Dropdown aggregation = new Dropdown(contactsTable, line + "_aggregation", req) {
					
					@Override
					public void onGET(OgemaHttpRequest req) {
						final StringResource aggregation = person.getSubResource(AlarmResourceUtil.EMAIL_AGGREGATION_SUBRESOURCE);
						if (aggregation != null && aggregation.isActive() && "daily".equals(aggregation.getValue()))
							selectSingleOption("daily", req);
						else
							selectSingleOption("none", req);
					}
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						
						final String selected = getSelectedValue(req);
						if ("daily".equals(selected)) {
							final StringResource aggregation = person.getSubResource(AlarmResourceUtil.EMAIL_AGGREGATION_SUBRESOURCE, StringResource.class).create();
							aggregation.setValue(selected);
							aggregation.activate(false);
						} else {
							final StringResource aggregation = person.getSubResource(AlarmResourceUtil.EMAIL_AGGREGATION_SUBRESOURCE);
							if (aggregation != null && aggregation.exists())
								aggregation.delete();
						}
					}
					
					
				};
				aggregation.setDefaultOptions(AGGREGATION_OPTIONS);
				aggregation.setDefaultWidth("8em");
				aggregation.setDefaultToolTip("Select the aggregation mode for emails related to device issues. Either: \"none\", "
						+ "in which case emails are sent immediately when an issue occurs, or \"daily\", in wich case an email with new issues is sent once a day.");
				row.addCell("aggregation", aggregation);
				
				final Label assignedLabel = new Label(contactsTable, line + "_assignedcnt", req) {
					
					@Override
					public void onGET(OgemaHttpRequest req) {
						final String email = person.getSubResource("emailAddress", StringResource.class).getValue();
						if (email == null || "".equals(email)) {
							setText("0", req);
							return;
						}
						final ResourceList<InstallAppDevice> knownDevices = appMan.getResourceAccess().getResource("hardwareInstallConfig/knownDevices");
						final Stream<AlarmGroupData> stream1 = knownDevices == null ? Stream.empty() : knownDevices.getAllElements().stream().map(InstallAppDevice::knownFault).filter(Resource::isActive);
						/*
						final ResourceList<AlarmGroupDataMajor> majorIssues = appMan.getResourceAccess().getResource("gatewaySuperiorDataRes/majorKnownIssues");
						@SuppressWarnings({ "unchecked", "rawtypes" })
						final Stream<AlarmGroupData> stream2 = majorIssues == null ? Stream.empty() : (Stream) majorIssues.getAllElements().stream();
						final Stream<AlarmGroupData> stream = Stream.concat(stream1, stream2);
						*/
						final Stream<AlarmGroupData> stream = stream1; // sufficient, since we have a link from the knownFault() subresource to the major issue
						final long cnt = stream.map(AlarmGroupData::responsibility)
							.filter(res -> res.isActive() && email.equalsIgnoreCase(res.getValue()))
							.count();
						setText(String.valueOf(cnt), req);
					}
					
				};
				row.addCell("assigned", assignedLabel);
				
				final ButtonConfirm delete = new ButtonConfirm(contactsTable, line + "_delete", req) {
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						final String nameOrId = nameOrId(person);
						final String location = person.getLocation();
						person.delete();
						alert.showAlert("Responsible contact " + nameOrId + " (resource " + location + ") has been deleted.", true, req);
					}
					
				};
				delete.setDefaultConfirmBtnMsg("Delete");
				delete.setDefaultConfirmMsg("Do you really want to delete the contact " + nameOrId(person) + "?");
				delete.setDefaultConfirmPopupTitle("Delete contact?");
				delete.setDefaultCancelBtnMsg("Cancel");
				delete.addDefaultStyle(ButtonConfirmData.CANCEL_LIGHT_BLUE);
				delete.addDefaultStyle(ButtonConfirmData.CONFIRM_RED);
				delete.setDefaultText("Delete contact");
				delete.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
				//delete.triggerAction(contactsTable, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
				
				row.addCell("delete", delete);
				
				
				return row;
			}
			
		};
		this.contactsTable.setRowTemplate(template);
		contactsTable.addDefaultStyle(DynamicTableData.BOLD_HEADER);
		contactsTable.addDefaultCssItem(">div>div>table>tbody>tr:first-child", Collections.singletonMap("color", "darkblue"));
		contactsTable.addDefaultCssItem(">div>div>table>tbody>tr:first-child", Collections.singletonMap("background-color", "lightgray"));
		this.creationPopup = new ContactCreationPopup(appMan, page, alert, contactsTable);
		this.creationTriggerBtn = new Button(page, "creationtrigger", "Create contact");
		creationTriggerBtn.setDefaultToolTip("Open a popup for creating a new contact");
		creationTriggerBtn.setDefaultStyles(Collections.singleton(ButtonData.BOOTSTRAP_BLUE));
		creationTriggerBtn.triggerAction(creationPopup.popup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET);
		this.buildPage();
	}
	
	private void buildPage() {
		final Header header = new Header(page, "title", "10. Responsibility contacts");
		header.setDefaultHeaderType(1);
		header.setDefaultColor("darkblue");
		final Header existingContactsHeader = new Header(page, "exContactsHeader", "Existing contacts");
		existingContactsHeader.setDefaultHeaderType(2);
		existingContactsHeader.setDefaultColor("darkblue");
		final Header newContactsHeader = new Header(page, "newContactsHeader", "New contacts");
		newContactsHeader.setDefaultHeaderType(2);
		newContactsHeader.setDefaultColor("darkblue");
		page
			.append(header).linebreak()
			.append(info)
			.append(alert).linebreak()
			.append(existingContactsHeader).linebreak()
			.append(contactsTable)
			.append(newContactsHeader)
			.append(creationTriggerBtn);
		
		page.append(this.creationPopup.popup);
	}
	
	private static String nameOrId(final NaturalPerson person) {
		final String name = name(person);
		if ("".equals(name) && person.userName().isActive())
			return person.userName().getValue();
		if ("".equals(name) && person.userRole().isActive())
			return person.userRole().getValue();
		return name;
	}
	
	private static String name(final NaturalPerson person) {
		final boolean firstActive = person.firstName().isActive();
		final boolean lastActive = person.lastName().isActive();
		if (!firstActive && !lastActive)
			return "";
		final StringBuilder sb = new StringBuilder();
		if (firstActive)
			sb.append(person.firstName().getValue()).append(' ');
		if (person.middleName().isActive())
			sb.append(person.middleName().getValue()).append(' ');
		if (lastActive)
			sb.append(person.lastName().getValue()).append(' ');
		return sb.toString().trim();
	}
	
	private static class ContactCreationPopup {
		
		final Popup popup;
		
		public ContactCreationPopup(final ApplicationManager appMan, final WidgetPage<?> page, final Alert alert, final DynamicTable<?> table) {
			this.popup = new Popup(page, "creationpopup", true);
			final TextField firstName = new TextField(page, "creationfirstname");
			final TextField lastName = new TextField(page, "creationlastname");
			final TextField userName = new TextField(page, "creationusername");
			final TextField userRole = new TextField(page, "creationuserrole");
			final TextField email = new TextField(page, "creationemail");
			email.setDefaultType(TextFieldType.EMAIL);
			email.setDefaultPlaceholder("someone@somemail.com");
			
			final Button cancelButton = new Button(page, "createpopupcancel", "Cancel");
			final Button submitButton = new Button(page, "createpopupsubmit", "Create user") {
				
				private static final String MISSING_DATA_TOOLTIP = "The fields \"email\" and at least one of \"firstName\", \"lastName\", \"userName\" or \"userRole\""
						+ " are required";
				
				@Override
				public void onGET(OgemaHttpRequest req) {
					final String eml = email.getValue(req).trim();
					if ("".equals(eml) || eml.indexOf('@') < 0) {
						disable(req);
						removeStyle(ButtonData.BOOTSTRAP_BLUE, req);
						setToolTip(MISSING_DATA_TOOLTIP, req);
						return;
					}
					final boolean active = !"".equals(firstName.getValue(req).trim()) || !"".equals(lastName.getValue(req).trim()) ||
							!"".equals(userName.getValue(req).trim()) || !"".equals(userRole.getValue(req).trim());
					if (active) {
						setStyle(ButtonData.BOOTSTRAP_BLUE, req);
						setToolTip("Create new user", req);
						enable(req);
					} else {
						removeStyle(ButtonData.BOOTSTRAP_BLUE, req);
						setToolTip(MISSING_DATA_TOOLTIP, req);
						disable(req);
					}
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					final String eml = email.getValue(req).trim();
					if ("".equals(eml) || eml.indexOf('@') < 0) {
						alert.showAlert("Email empty/invalid", false, req);
						return;
					}
					final GatewaySuperiorData supData = appMan.getResourceAccess().getResource("gatewaySuperiorDataRes");
					final ResourceList<NaturalPerson> contacts = supData != null && supData.exists() ? supData.responsibilityContacts() : null;
					if (contacts == null) {
						alert.showAlert("Superior data does not exist on your gateway. If you think this is an error, please contact your gateway administrator", false, req);
						return;
					}
					final Optional<StringResource> existingEmail = contacts.getAllElements().stream()
						.map(ct -> ct.getSubResource("emailAddress", StringResource.class))
						.filter(email -> email.isActive() && eml.equalsIgnoreCase(email.getValue()))
						.findAny();
					if (existingEmail.isPresent()) {
						alert.showAlert("User with email " + eml + " already exists: " + existingEmail.get().getLocation(), false, req);
						return;
					}
					final String first = firstName.getValue(req).trim();
					final String last = lastName.getValue(req).trim();
					final String user = userName.getValue(req).trim();
					final String role = userRole.getValue(req).trim();
					final boolean firstSet = !"".equals(first);
					final boolean lastSet = !"".equals(last);
					final boolean userSet = !"".equals(user);
					final boolean roleSet = !"".equals(role);
					if (!firstSet && !lastSet && !userSet && !roleSet) {
						alert.showAlert("No name, user name or role set, could not create user", false, req);
						return;
					}
					if (userSet) {
						final Optional<StringResource> existingUserRes = contacts.getAllElements().stream()
							.map(ct -> ct.userName())
							.filter(existingUser -> existingUser.isActive() && user.equalsIgnoreCase(existingUser.getValue()))
							.findAny();
						if (existingUserRes.isPresent()) {
							alert.showAlert("User " + user + " already exists: " + existingUserRes.get().getParent().getLocation(), false, req);
							return;
						}
					}
					if (roleSet) {
						final Optional<StringResource> existingRoleRes = contacts.getAllElements().stream()
							.map(ct -> ct.userRole())
							.filter(existingRole -> existingRole.isActive() && role.equalsIgnoreCase(existingRole.getValue()))
							.findAny();
						if (existingRoleRes.isPresent()) {
							alert.showAlert("User role " + role + " already exists: " + existingRoleRes.get().getParent().getLocation(), false, req);
							return;
						}
					}
					final String name = (first + " " + last).trim();
					if (firstSet || lastSet) {
						final Optional<NaturalPerson> existingContact = contacts.getAllElements().stream()
								.filter(contact -> contact.isActive() && name.equalsIgnoreCase(name(contact)))
								.findAny();
							if (existingContact.isPresent()) {
								alert.showAlert("User " + name + " already exists: " + existingContact.get().getLocation(), false, req);
								return;
							}
					}
					final String newUser = !"".equals(name) ? name : userSet ? user : role;
					try {
						String id = ResourceUtils.getValidResourceName(newUser);
						int cnt = 0;
						while (contacts.getSubResource(id) != null) {
							if (!contacts.getSubResource(id).isActive() && contacts.getSubResource(id) instanceof NaturalPerson)  // ok, reuse
								break;
							if (cnt > 0) {
								final int lastIdx = id.lastIndexOf('_');
								id = id.substring(0, lastIdx+1) + String.valueOf(cnt); 
							} else 
								id = id + "_" + cnt;
							cnt += 1;
						}
						if (!contacts.exists())
							contacts.create();
						final NaturalPerson newPerson = contacts.getSubResource(id, NaturalPerson.class).create();
						if (firstSet)
							newPerson.firstName().<StringResource> create().setValue(first);
						if (lastSet)
							newPerson.lastName().<StringResource> create().setValue(last);
						if (userSet)
							newPerson.userName().<StringResource> create().setValue(user);
						if (roleSet)
							newPerson.userRole().<StringResource> create().setValue(role);
						newPerson.getSubResource("emailAddress", StringResource.class).<StringResource> create().setValue(eml);
						newPerson.activate(true);
						if (!contacts.isActive())
							contacts.activate(false);
						alert.showAlert("New contact " + newUser + " created: " + newPerson.getLocation(), true, req);
					} catch (Exception e) {
						alert.showAlert("New contact creation failed: " + e, false, req);
					}
				}
				
			};
			
			cancelButton.triggerAction(popup, TriggeringAction.POST_REQUEST, TriggeredAction.HIDE_WIDGET);
			submitButton.triggerAction(popup, TriggeringAction.POST_REQUEST, TriggeredAction.HIDE_WIDGET);
			submitButton.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
			submitButton.triggerAction(table, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
			
			firstName.triggerAction(submitButton, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
			lastName.triggerAction(submitButton, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
			userName.triggerAction(submitButton, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
			userRole.triggerAction(submitButton, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
			email.triggerAction(submitButton, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
			
			final StaticTable bodyTable = new StaticTable(5, 2, new int[] {3, 9})
					.setContent(0, 0, "First name").setContent(0, 1, firstName)
					.setContent(1, 0, "Last name").setContent(1, 1, lastName)
					.setContent(2, 0, "User name").setContent(2, 1, userName)
					.setContent(3, 0, "Role").setContent(3, 1, userRole)
					.setContent(4, 0, "Email").setContent(4, 1, email);
			final PageSnippet bodySnippet = new PageSnippet(page, "creationpopupbody", true);
			bodySnippet.append(bodyTable, null);
			
			final Flexbox footerFlex = new Flexbox(page, "creationpopupfooter", true);
			footerFlex.addItem(submitButton, null).addItem(cancelButton, null);
			footerFlex.setJustifyContent(JustifyContent.FLEX_RIGHT, null);
			footerFlex.addCssItem(">div", Map.of("column-gap", "1em"), null);
			
			
			popup.setTitle("Create contact", null);
			popup.setBody(bodySnippet, null);
			popup.setFooter(footerFlex, null);
			popup.addCssItem(">div>div>div>div.modal-body", Collections.singletonMap("min-height", "10em"), null);
			
			
			
		}
		
		
		
	}
	

}
