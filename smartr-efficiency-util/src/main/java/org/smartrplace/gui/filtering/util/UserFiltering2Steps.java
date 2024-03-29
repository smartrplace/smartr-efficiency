package org.smartrplace.gui.filtering.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.SubcustomerUtil;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.core.administration.UserAccount;
import org.ogema.internationalization.util.LocaleHelper;
import org.ogema.tools.app.createuser.UserAdminBaseUtil;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.gui.filtering.DualFiltering2Steps;
import org.smartrplace.gui.filtering.GenericFilterFixedGroup;
import org.smartrplace.gui.filtering.GenericFilterFixedSingle;
import org.smartrplace.gui.filtering.GenericFilterOption;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public abstract class UserFiltering2Steps<T> extends DualFiltering2Steps<String, AccessConfigUser, T> {
	private static final long serialVersionUID = 1L;
	//protected final AccessAdminController controller;
	protected final ApplicationManagerPlus appManPlus;
	protected final AccessAdminConfig appConfigData;

	public static class SingleUserOption extends GenericFilterFixedSingle<String> {
		public SingleUserOption(String myUserName, Map<OgemaLocale, String> optionLab) {
			super(myUserName, optionLab);
		}
	}

	public UserFiltering2Steps(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode, long optionSetUpdateRate,
			AccessAdminConfig appConfigData, ApplicationManagerPlus appManPlus) {
		super(page, id, saveOptionMode, optionSetUpdateRate, false);
		//this.controller = controller;
		this.appManPlus = appManPlus;
		this.appConfigData = appConfigData;
	}

	@Override
	protected List<GenericFilterOption<String>> getOptionsDynamic(AccessConfigUser group, OgemaHttpRequest req) {
		List<GenericFilterOption<String>> result = new ArrayList<>();
		for(UserAccount ac: UserAdminBaseUtil.getNaturalUsers(appManPlus, req)) { //controller.appMan.getAdministrationManager().getAllUsers()) {
			if(group != null && (!firstDropDown.isInSelection(ac.getName(), group)))
				continue;
			GenericFilterOption<String> newOption = new SingleUserOption(ac.getName(), LocaleHelper.getLabelMap(ac.getName()));
			result.add(newOption);
		}
		return result;
	}

	@Override
	protected List<GenericFilterFixedGroup<String, AccessConfigUser>> getGroupOptionsDynamic() {
		List<GenericFilterFixedGroup<String, AccessConfigUser>> result = new ArrayList<>();
		for(AccessConfigUser grp: SubcustomerUtil.getUserGroups(false, true, appConfigData)) {
			GenericFilterFixedGroup<String, AccessConfigUser> newOption = getGroupOptionDynamic(grp);
			if(newOption == null)
				continue;
			result.add(newOption);			
		}
		return result;
	}
	
	//@Override
	protected GenericFilterFixedGroup<String, AccessConfigUser> getGroupOptionDynamic(AccessConfigUser grp) {
		String name = grp.name().getValue();
		if(grp.isGroup().getValue() >= 2) {
			GenericFilterFixedGroup<String, AccessConfigUser> filter = appManPlus.userPermService().getUserGroupFiler(name);
			return filter;
		}
		GenericFilterFixedGroup<String, AccessConfigUser> newOption = new GenericFilterFixedGroup<String, AccessConfigUser>(
				grp, LocaleHelper.getLabelMap(name)) {

			@Override
			public boolean isInSelection(String object, AccessConfigUser group) {
				AccessConfigUser userConfig = getUserConfig(object, appConfigData);
				return ResourceHelper.containsLocation(userConfig.superGroups().getAllElements(), grp);
			}
		};
		return newOption;
	}
	/*public static List<GenericFilterFixedGroup<String, AccessConfigUser>> getOptionsDynamic2S(AccessAdminController controller, OgemaHttpRequest req) {
		List<GenericFilterFixedGroup<String, AccessConfigUser>> result = new ArrayList<>();
		for(AccessConfigUser grp: controller.getUserGroups(false)) {
			String name = grp.name().getValue();
			GenericFilterFixedGroup<String, AccessConfigUser> newOption = getGroupOptionDynamic(grp, req);
			if(grp.isGroup().getValue() >= 2) {
				GenericFilterFixedGroup<String, AccessConfigUser> filter = controller.userPermService.getUserGroupFiler(name);
				if(filter == null)
					continue;
				result.add(filter);
				continue;
			}
			GenericFilterFixedGroup<String, AccessConfigUser> newOption = new GenericFilterFixedGroup<String, AccessConfigUser>(
					grp, LocaleHelper.getLabelMap(name)) {

					@Override
				public boolean isInSelection(String object, AccessConfigUser group) {
					AccessConfigUser userConfig = controller.getUserConfig(object);
					return ResourceHelper.containsLocation(userConfig.superGroups().getAllElements(), grp);
				}
			};
			result.add(newOption);			
		}
		return result;
	}*/
	

	@Override
	protected List<AccessConfigUser> getGroups(String object) {
		AccessConfigUser userConfig = getUserConfig(object, appConfigData);
		List<AccessConfigUser> result = new ArrayList<>();
		result.addAll(getAllGroupsForUser(userConfig, appConfigData, appManPlus.userPermService())); //.superGroups().getAllElements());
		return result ;
	}
	
	@Override
	protected boolean isAttributeSinglePerDestinationObject() {
		return true;
	}
	
	public String getSelectedUser(OgemaHttpRequest req) {
		GenericFilterOption<String> selected = getSelectedItem(req);
		if(selected == NONE_OPTION)
			return null;
		if(selected == null) {
			onGET(req);
			return getSelectedUser(req);
			//return ((SingleUserOption)getItems(req).get(0)).getValue();
		}
		return ((SingleUserOption)selected).getValue();
	}
	
	@Override
	protected long getFrameworkTime() {
		return appManPlus.appMan().getFrameworkTime();
	}

	@Override
	protected boolean isGroupEqual(AccessConfigUser group1, AccessConfigUser group2) {
		return group1.equalsLocation(group2);
	}
	
	public static AccessConfigUser getUserConfig(String userName, AccessAdminConfig appConfigData) {
		AccessConfigUser result = ResourceListHelper.getOrCreateNamedElement(userName, appConfigData.userPermissions());
		return result;
		/*for(AccessConfigUser user: appConfigData.userPermissions().getAllElements()) {
			if(user.name().getValue().equals(userName))
				return user;
		}
		return null;*/
	}

	public static List<AccessConfigUser> getAllGroupsForUser(AccessConfigUser naturalUser,
			AccessAdminConfig appConfigData, UserPermissionService userPermService) {
		List<AccessConfigUser> result = getLevel2GroupsForUser(naturalUser, appConfigData, userPermService);
		result.addAll(naturalUser.superGroups().getAllElements());
		return result;
	}
	public static List<AccessConfigUser> getLevel2GroupsForUser(AccessConfigUser naturalUser,
			AccessAdminConfig appConfigData, UserPermissionService userPermService) {
		List<AccessConfigUser> result = new ArrayList<>();
		if(naturalUser.isGroup().getValue() > 0)
			return result;
		String userName = naturalUser.name().getValue();
		for(AccessConfigUser grp: appConfigData.userPermissions().getAllElements()) {
			if(grp.isGroup().getValue() < 2)
				continue;
			GenericFilterFixedGroup<String, AccessConfigUser> filter = userPermService.getUserGroupFiler(grp.name().getValue());
			if(filter == null)
				continue;
			if(filter.isInSelection(userName, grp))
				result.add(grp);
		}
		return result ;
	}
}
