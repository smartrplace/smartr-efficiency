package org.smartrplace.apps.hw.install.gui.alarm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.user.NaturalPerson;
import org.ogema.tools.app.useradmin.config.MessagingAddress;
import org.ogema.tools.app.useradmin.config.UserAdminData;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationLevel;
import org.smartrplace.alarming.escalation.model.AlarmingMessagingApp;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.gui.tablepages.PerMultiselectConfigPage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Header;

@SuppressWarnings("serial")
public class MessagingAppConfigPage extends PerMultiselectConfigPage<AlarmingMessagingApp, String, AlarmingMessagingApp> {
	protected final AlarmingConfigAppController controller;
	protected final UserAdminData userAdminData;

	public MessagingAppConfigPage(WidgetPage<?> page, AlarmingConfigAppController controller) {
		super(page, controller.appMan, ResourceHelper.getSampleResource(AlarmingMessagingApp.class), false);
		this.controller = controller;
		this.userAdminData = controller.appMan.getResourceAccess().getResource("userAdminData");
		triggerPageBuild();
	}
	
	@Override
	protected void addWidgetsBeforeMultiSelect(AlarmingMessagingApp object,
			ObjectResourceGUIHelper<AlarmingMessagingApp, AlarmingMessagingApp> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		if(req == null) {
			vh.registerHeaderEntry("App Name");
			vh.registerHeaderEntry("Register");
			return;
		}
		vh.stringEdit("App Name", id, object.name(), row, alert);
		/*ResourceTextField<StringResource> appName = new ResourceTextField<StringResource>(mainTable, "appName"+id, req) {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				//TODO
			}
		};
		appName.selectDefaultItem(object.name());
		row.addCell(WidgetHelper.getValidWidgetId("App Name"), appName);*/
		
		ButtonConfirm registerApp = new ButtonConfirm(mainTable, "registerApp"+id, req) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(isRegistered(object.lastNameRegistered())) {
					setText("Update", req);
				} else
					setText("Register initially", req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				if(isRegistered(object.lastNameRegistered())) {
					controller.unregisterMessagingApp(object.lastNameRegistered().getValue(), object.name().getValue());
					controller.registerMessagingApp(object.name().getValue(), object.getName());
					setConfirmMsg("Update from "+object.lastNameRegistered().getValue()+" to "+object.name().getValue()+" ?", req);
				} else {
					//Use resource name as suffix, which is not shown in the RemoteConfig
					controller.registerMessagingApp(object.name().getValue(), object.getName());
					//for now we do not put this into a special receiver page, so this should not be necessary
					//controller.setupMessageReceiverConfiguration(mr, appList, pageRes3, showSuperAdmin);					
					setConfirmMsg("Register messaging app "+object.name().getValue()+" ?", req);
				}
				ValueResourceHelper.setCreate(object.lastNameRegistered(), object.name().getValue());
			}
		};
		row.addCell(WidgetHelper.getValidWidgetId("Register"), registerApp);
	}

	private boolean isRegistered(StringResource lastRegistered) {
		return (lastRegistered != null) && (lastRegistered.getValue() != null) && (!lastRegistered.getValue().isEmpty());
	}
	
	@Override
	public AlarmingMessagingApp getResource(AlarmingMessagingApp object, OgemaHttpRequest req) {
		return object;
	}

	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "header", "6. Alarming Messaging App Configuration (Message destination groups)");
		page.append(header);
		StaticTable topTable = new StaticTable(1, 4);
		Button addItemButton = new Button(page, "addItemButon", "Add M-App") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				ResourceListHelper.createAndSetUniqueHumanReadableNameForNewElement(
						controller.hwTableData.appConfigData.escalation().messagigApps(), "New App");
			}
		};
		topTable.setContent(0, 0, addItemButton);
		addItemButton.registerDependentWidget(mainTable);
		page.append(topTable);
	}

	@Override
	public Collection<AlarmingMessagingApp> getObjectsInTable(OgemaHttpRequest req) {
		return controller.hwTableData.appConfigData.escalation().messagigApps().getAllElements();
	}

	@Override
	protected String getGroupColumnLabel() {
		return "Users for mobile push notification";
	}

	@Override
	protected Collection<String> getAllGroups(AlarmingMessagingApp object, OgemaHttpRequest req) {
		List<String> result = new ArrayList<>();
		for(NaturalPerson userData: userAdminData.userData().getAllElements()) {
			@SuppressWarnings("unchecked")
			ResourceList<MessagingAddress> messagingAdds = userData.getSubResource("messagingAddresses", ResourceList.class);
			if(messagingAdds.size() > 0)
				result.add(ResourceListHelper.getNameForElement(userData));
		}
		return result;
	}

	@Override
	protected List<String> getGroups(AlarmingMessagingApp object, OgemaHttpRequest req) {
		return Arrays.asList(object.usersForPushMessage().getValues());
	}

	@Override
	protected void setGroups(AlarmingMessagingApp object, List<String> groups, OgemaHttpRequest req) {
		ValueResourceHelper.setCreate(object.usersForPushMessage(), groups.toArray(new String[0]));
	}

	@Override
	protected String getGroupLabel(String object, OgemaLocale locale) {
		return object;
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "M-App Id";
	}

	@Override
	protected String getLabel(AlarmingMessagingApp obj, OgemaHttpRequest req) {
		return ResourceUtils.getHumanReadableShortName(obj);
	}
}
