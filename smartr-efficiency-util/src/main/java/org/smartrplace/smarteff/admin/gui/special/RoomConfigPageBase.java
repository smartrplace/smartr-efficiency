package org.smartrplace.smarteff.admin.gui.special;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.accessadmin.api.NamedIntegerType;
import org.ogema.accessadmin.api.SubcustomerUtil;
import org.ogema.accessadmin.api.SubcustomerUtil.SubCustomerType;
import org.ogema.accessadmin.api.util.RoomEditHelper;
import org.ogema.apps.roomlink.NewRoomPopupBuilder.RoomCreationListern;
import org.ogema.apps.roomlink.localisation.mainpage.RoomLinkDictionary;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.external.accessadmin.config.SubCustomerData;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.gui.filtering.util.RoomFilteringWithGroups;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.directresourcegui.GUIHelperExtension;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;

import de.iwes.util.linkingresource.RoomHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.template.DefaultDisplayTemplate;

@SuppressWarnings("serial")
public class RoomConfigPageBase extends ObjectGUITablePageNamed<Room, Room> {
	public final static Map<String, String> valuesToSetDefault = new HashMap<>();
	static {
		int[] ks = RoomHelper.getRoomTypeKeys();
		for(int type: ks) {
			String label = RoomHelper.getRoomTypeString(type, OgemaLocale.ENGLISH);
			valuesToSetDefault.put(""+type, label);
		}
	}
	
	protected final ApplicationManager appMan;
	protected final AccessAdminConfig appConfigData;
	protected final ResourceList<BuildingPropertyUnit> roomGroups;
	
	protected RoomFilteringWithGroups<Room> roomFilter;
	protected final boolean isExpert;
	protected final boolean showLocations;

	public RoomConfigPageBase(WidgetPage<?> page, ApplicationManager appMan) {
		this(page, appMan, false);
	}
	public RoomConfigPageBase(WidgetPage<?> page, ApplicationManager appMan, boolean isExpert) {
		this(page, appMan, isExpert, false);
	}
	public RoomConfigPageBase(WidgetPage<?> page, ApplicationManager appMan, boolean isExpert,
			boolean showLocations) {
		super(page, appMan, ResourceHelper.getSampleResource(Room.class));
		this.appMan = appMan;
		this.isExpert = isExpert;
		this.showLocations = showLocations;
		this.appConfigData = ResourceHelper.getTopLevelResource(AccessAdminConfig.class, appMan.getResourceAccess());
		this.roomGroups = appConfigData.roomGroups();
		triggerPageBuild();
	}

	@Override
	public Room getResource(Room object, OgemaHttpRequest req) {
		return object;
	}

	@Override
	public void addWidgets(Room object, ObjectResourceGUIHelper<Room, Room> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		
		addNameLabel(object, vh, id, row, req);
		
		String roomId = ServletPageProvider.getNumericalIdString(object.getLocation(), true);
		if(isExpert) {
			if(req == null) {
				if(Boolean.getBoolean("org.smartrplace.smarteff.admin.gui.special.subcustomer.configongatewayoption"))
					vh.registerHeaderEntry("Subcustomer");
				vh.registerHeaderEntry("Room Type");
				vh.registerHeaderEntry("ID");
			} else {
				List<SubCustomerData> subcsAll = SubcustomerUtil.getSubcustomers(appMan);
				List<SubCustomerData> subcs = new ArrayList<>();
				for(SubCustomerData sub: subcsAll) {
					if(sub.aggregationType().getValue() <= 0) {
						subcs.add(sub);
					}
				}
				SubCustomerData subcustomerRef = SubcustomerUtil.getDataForRoom(object, appMan, false);
				final SubCustomerData subcustomerRefFin = subcustomerRef;

				if(Boolean.getBoolean("org.smartrplace.smarteff.admin.gui.special.subcustomer.configongatewayoption")) {
					TemplateDropdown<SubCustomerData> subCustDrop = new TemplateDropdown<SubCustomerData>(mainTable, "subCustDrop"+id, req) {
						@Override
						public void onGET(OgemaHttpRequest req) {
							update(subcs, req);
							selectItem(subcustomerRefFin, req);
						}
						
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							SubCustomerData selected = getSelectedItem(req);
							for(SubCustomerData subc: subcs) {
								if(subc.aggregationType().getValue() > 0)
									continue;
								ResourceListHelper.removeReferenceOrObject(subc.roomGroup().rooms(), object);
							}
							SubcustomerUtil.addRoomToGroup(object, selected.roomGroup());
						}
						
					};
					subCustDrop.setTemplate(new DefaultDisplayTemplate<SubCustomerData>());
					subCustDrop.setAddEmptyOption(true, "not set", req);
					row.addCell("Subcustomer", subCustDrop);
				}
				
				if(subcustomerRef == null)
					subcustomerRef = SubcustomerUtil.getDataForRoom(object, appMan, true);
				if(subcustomerRef != null && subcustomerRef.exists()) {
					Map<String, String> valuesToSet = new LinkedHashMap<>();
					int sid = subcustomerRef.subCustomerType().getValue();
					SubCustomerType data = SubcustomerUtil.subCustomerTypes.get(sid);
					for(Entry<Integer, NamedIntegerType> roomEntry: data.roomTypes.entrySet()) {						
						String label = roomEntry.getValue().labelReq(req);
						valuesToSet.put(""+roomEntry.getKey(), label);
					}
					if(!valuesToSet.isEmpty())
						vh.dropdown("Room Type", id, object.type(), row, valuesToSet);
					else
						appMan.getLogger().warn("Empty valuesToSet for "+subcustomerRef.getLocation());
				}
				
				vh.stringLabel("ID", id, roomId, row);
				
			}
		}
		addWidgetsAfterMultiSelect(object, vh, roomId, req, row, appMan);
	}
	
	protected void addWidgetsAfterMultiSelect(Room object, ObjectResourceGUIHelper<Room, Room> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		GUIHelperExtension.addDeleteButton(null, object, mainTable, id, alert, row, vh, req);
		if(showLocations) {
			vh.stringLabel("Location", id, object.getLocation(), row);
			if(req == null)
				vh.registerHeaderEntry("CMS ID");
			else {
				IntegerResource cmsIdRes = object.getSubResource("cmsId", IntegerResource.class);
				if(cmsIdRes != null && cmsIdRes.isActive())
					vh.intLabel("CMS ID", id, cmsIdRes.getValue(), row, 0);
			}
		}
	}
	
	@Override
	protected String getHeader(OgemaLocale locale) {
		if(isExpert)
			return "Room Type Configuration";
		return "Room Type Configuration";
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		StaticTable topTable = new StaticTable(1, 5);
		roomFilter = new RoomFilteringWithGroups<Room>(page, "roomFilter",
				OptionSavingMode.PER_USER, 5000, appConfigData.roomGroups(), false, appMan) {
			private static final long serialVersionUID = 1L;

			@Override
			protected Room getAttribute(Room object) {
				return object;
			}
		};
		
		roomFilter.registerDependentWidget(mainTable);
		
		topTable.setContent(0, 1, "").setContent(0,  2, roomFilter);
		if(!Boolean.getBoolean("org.smartrplace.external.accessadmin.gui.suppresscreateroom")) {
			RoomEditHelper.addButtonsToStaticTable(topTable, (WidgetPage<RoomLinkDictionary>) page,
					alert, appMan, 0, 2, new RoomCreationListern() {
						
				@Override
				public void roomCreated(Room room) {
					initRoom(room);
				}
			});
			RedirectButton calendarConfigButton = new RedirectButton(page, "calendarConfigButton",
					"Calendar Configuration", "/org/smartrplace/apps/smartrplaceheatcontrolv2/extensionpage.html");
			topTable.setContent(0, 4, calendarConfigButton);
		}
		page.append(topTable);
		
	}
	
	@Override
	public Collection<Room> getObjectsInTable(OgemaHttpRequest req) {
		List<Room> all = KPIResourceAccess.getRealRooms(appMan.getResourceAccess()); //.getToplevelResources(Room.class);
		List<Room> result = roomFilter.getFiltered(all, req);
		return result;
	}

	/*@Override
	protected String getGroupColumnLabel() {
		return "Room Attributes";
	}*/

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Room name";
	}

	@Override
	protected String getLabel(Room obj, OgemaHttpRequest req) {
		return ResourceUtils.getHumanReadableShortName(obj);
	}

	/*@Override
	protected List<BuildingPropertyUnit> getAllGroups(Room object, OgemaHttpRequest req) {
		return controller.roomGroups.getAllElements();
	}

	@Override
	protected List<BuildingPropertyUnit> getGroups(Room object, OgemaHttpRequest req) {
		return controller.getGroups(object);
	}

	@Override
	protected String getGroupLabel(BuildingPropertyUnit object, OgemaLocale locale) {
		return ResourceUtils.getHumanReadableShortName(object);
	}

	@Override
	protected void setGroups(Room object, List<BuildingPropertyUnit> groups, OgemaHttpRequest req) {
		setGroups(object, groups, getAllGroups(null, null));
	}*/

	/** Make sure a room is part of exactly a certain set of groups
	 * 
	 * @param object
	 * @param groups
	 * @param allGroups check all room groups whether room is referenced. If not in groups then reference is removed.
	 */
	protected static void setGroups(Room object, List<BuildingPropertyUnit> groups, List<BuildingPropertyUnit> allGroups) {
		for(BuildingPropertyUnit bu: groups) {
			ResourceListHelper.addReferenceUnique(bu.rooms(), object);
		}
		for(BuildingPropertyUnit bu: allGroups) { //getAllGroups(null, null)
			if(ResourceHelper.containsLocation(groups, bu))
				continue;
			ResourceListHelper.removeReferenceOrObject(bu.rooms(), object);
		}
	}
	
	protected void initRoom(Room object) {
		SubcustomerUtil.initRoom(object, roomGroups, appConfigData);
	}
}
