package org.smartrplace.hwinstall.basetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.resourcemanager.ResourceNotFoundException;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DeviceHandlerProvider.DeviceTableConfig;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.model.gateway.EvalCollection;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Location;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.util.extended.eval.widget.BooleanResourceButton;
import org.smartrplace.apps.hw.install.LocalDeviceId;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.DeviceTypeFilterDropdown;
import org.smartrplace.gui.filtering.DualFiltering;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.gui.filtering.util.RoomFiltering2Steps;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.alert.AlertData;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;
import de.iwes.widgets.html.form.label.Label;

@SuppressWarnings("serial")
/** Note that only the method getDevicesSelected is really implemented here*/
public class HardwareTablePage implements InstalledAppsSelector { //extends DeviceTablePageFragment
	public static final int IDRANGE_MAX_NOLIMIT_DEFAULT = 30;
	public static final int MAX_DEVICE_PER_ALL = Integer.getInteger("org.smartrplace.hwinstall.basetable.maxdeviceforall", 400);
	private static final long IDRANGE_RESET_INTERVAL = Long.getLong("org.smartrplace.hwinstall.basetable.idrange.resetinterval", 2*TimeProcUtil.HOUR_MILLIS);

	/** Option: Definition of a range of IDs to which view shall be limited*/
	public static Integer idRangeStart = null;
	public static Integer idRangeEnd = null;
	/** Maximum number of devices in category so that no filtering by idRange is performed*/
	public static Integer idRangeMaxNoLimit = IDRANGE_MAX_NOLIMIT_DEFAULT;
	public static long idRangeLastEdit = -1;

	//Overwrite to reduce columns
	protected boolean showOnlyBaseColsHWT() {return false;}
	protected boolean hideDeviceHandler(DeviceHandlerProvider<?> devHand) {return false;}

	//protected final HardwareInstallController controller;
	protected final WidgetPage<?> page;
	protected final Alert alert;
	protected final ApplicationManager appMan;
	protected final ApplicationManagerPlus appManPlus;
	protected final DeviceHandlerAccess devHandAcc;
	protected final HardwareTableData resData;

	public enum FilterMode {
		STANDARD,
		KNOWN_FAULTS
	}
	protected final FilterMode filterMode;
	
	private Header header;
	//protected RoomSelectorDropdown roomsDrop;
	protected RoomFiltering2Steps<InstallAppDevice> roomsDrop;
	//protected InstallationStatusFilterDropdown2 installFilterDrop;
	protected DeviceTypeFilterDropdown typeFilterDrop;
	protected DualFiltering<Room, InstallAppDevice, InstallAppDevice> finalFilter;
	//protected final InstalledAppsSelector instAppsSelector;
	protected final StaticTable topTable;

	protected int getTopTableLines() {
		return 1;
	}
	protected boolean offerAddRoomButton() {
		return true;
	}
	
	protected boolean suppressSearchForNewDevices() {
		return false;
	}
	
	protected class SubTableData {
		DeviceHandlerProvider<?> pe;
		DeviceTableBase table;
		public SubTableData(DeviceHandlerProvider<?> pe, DeviceTableBase table) {
			this.pe = pe;
			this.table = table;
		}
	}
	protected final List<SubTableData> subTables = new ArrayList<>();
	
	protected String pid() {
		return WidgetHelper.getValidWidgetId(this.getClass().getName());
	}
	protected String getHeader() {return "Device Setup and Configuration";}
	
	protected boolean isAllOptionAllowedSuper(OgemaHttpRequest req) {
		int size = HardwareTablePage.this.resData.appConfigData.knownDevices().size();
		return size < MAX_DEVICE_PER_ALL;		
	}

	public HardwareTablePage(WidgetPage<?> page, final ApplicationManagerPlus appManPlus,
			final DeviceHandlerAccess devHandAcc, HardwareTableData resData) {
		this(page, appManPlus, devHandAcc, resData, true);
	}
	public HardwareTablePage(WidgetPage<?> page, final ApplicationManagerPlus appManPlus,
			final DeviceHandlerAccess devHandAcc, HardwareTableData resData, boolean triggerFinishConstructorAutomatically) {
		this(page, appManPlus, devHandAcc, resData, triggerFinishConstructorAutomatically, FilterMode.STANDARD);
	}
	public HardwareTablePage(WidgetPage<?> page, final ApplicationManagerPlus appManPlus,
			final DeviceHandlerAccess devHandAcc, HardwareTableData resData, boolean triggerFinishConstructorAutomatically,
			FilterMode filterMode) {
		this.page = page;
		this.appMan = appManPlus.appMan();
		this.appManPlus = appManPlus;
		this.devHandAcc = devHandAcc;
		this.resData = resData;
		this.filterMode = filterMode;
		
		//this.controller = controller;
		//init all widgets
		this.alert = new Alert(page, WidgetHelper.getValidWidgetId("alert"+pid()), "") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(idRangeStart != null) {
					checkIdRangeReset();
				}
				if(idRangeStart != null) {
					autoDismiss(TimeProcUtil.DAY_MILLIS, req);
					setWidgetVisibility(true, req);
					String text;
					if(idRangeEnd != null)
						text = "Limited to ID range: "+idRangeStart+" to "+idRangeEnd;
					else
						text = "Limited to ID: "+idRangeStart;
					if(idRangeMaxNoLimit != null)
						text += " (from "+idRangeMaxNoLimit+" devices per type)";
					alert.setText(text, req);
					alert.setStyle(AlertData.BOOTSTRAP_DANGER, req);
				}			
			}
		};
		page.append(alert).linebreak();
		
		//this.instAppsSelector = this;

		header = new Header(page, "header") {
			public void onGET(OgemaHttpRequest req) {
				setText(getHeader(), req);				
			}
		};
		header.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_LEFT);
		page.append(header).linebreak();
		
		final OgemaWidget installMode;
		if(suppressSearchForNewDevices()) {
			//leave out
			installMode = null;
		} else if(devHandAcc == null) {
			installMode = new Label(page, "installModeLab", "Search for new devices: "+
					(resData.appConfigData.isInstallationActive().getValue()?"active":"inactive"));
		} else {
			installMode = new BooleanResourceButton(page, "installMode", "Search for new devices",
					resData.appConfigData.isInstallationActive(), ButtonData.BOOTSTRAP_GREEN,
					ButtonData.BOOTSTRAP_RED) {
				private static final long serialVersionUID = 1L;
	
				@Override
				public void updateDependentWidgets(OgemaHttpRequest req) {
					for(SubTableData tabData: subTables) {
						if(isObjectsInTableEmpty(tabData.pe, req)) {
							tabData.table.getMainTable().setWidgetVisibility(false, req);
							tabData.table.getHeaderWidget().setWidgetVisibility(false, req);
							tabData.table.setEmpty(true);
						} else {
							tabData.table.getMainTable().setWidgetVisibility(true, req);						
							tabData.table.getHeaderWidget().setWidgetVisibility(true, req);
							tabData.table.setEmpty(false);
						}
					}
				}
	
				@Override
				public void onPrePOST(String data, OgemaHttpRequest req) {
					super.onPrePOST(data, req);
					//controller.checkDemands();
				}
			};
		}
		//roomsDrop = new RoomSelectorDropdown(page, "roomsDrop", controller);
		ResourceList<BuildingPropertyUnit> roomGroups = resData.accessAdminConfigRes.roomGroups();
		final IntegerResource allAllowedMode = HardwareTablePage.this.resData.appConfigData.allowAllDevicesInTablePagesMode();
		roomsDrop = new RoomFiltering2Steps<InstallAppDevice>(page, "roomsDrop",
				OptionSavingMode.GENERAL, 10000, roomGroups, appManPlus, true) {

			@Override
			protected Room getAttribute(InstallAppDevice object) {
				try {
					return object.device().location().room().getLocationResource();
				} catch(ResourceNotFoundException e) {
					e.printStackTrace();
					PhysicalElement dev = object.device();
					Resource subres = dev.getSubResource("location");
					if(subres != null) {
						System.out.println("Location type:"+subres.getResourceType().getName());
						subres.delete();
					} else
						System.out.println("Location is not available");
					Location loc = dev.getSubResource("location", Location.class);
					Room room = loc.room();
					return room.getLocationResource();
				}
			}
			
			@Override
			protected boolean isAllOptionAllowed(OgemaHttpRequest req) {
				if(allAllowedMode.isActive())
					return allAllowedMode.getValue()>=2?false:true;
				return isAllOptionAllowedSuper(req);
			}
		};
		roomsDrop.suppressEmptyOptionsInFirstDropdown = false;
		//installFilterDrop = new InstallationStatusFilterDropdown(page, "installFilterDrop", controller);
		//installFilterDrop = new InstallationStatusFilterDropdown2(page, "installFilterDrop",
		//		OptionSavingMode.PER_USER, appMan,
		//		filterMode==FilterMode.KNOWN_FAULTS?true:false);
		typeFilterDrop = new DeviceTypeFilterDropdown(page, "devTypeFilterDrop", OptionSavingMode.PER_USER, appMan, appManPlus.dpService()) {
			/*@Override
			protected boolean isAllOptionAllowed(OgemaHttpRequest req) {
				//if(roomsDrop.getSelectedItem(req) != roomsDrop.getAllOption(req))
				//	return true;
				int size = HardwareTablePage.this.resData.appConfigData.knownDevices().size();
				return size < MAX_DEVICE_PER_ALL;
			}*/			
		};
		finalFilter = new DualFiltering<Room, InstallAppDevice, InstallAppDevice>(roomsDrop, typeFilterDrop);
		
		
		//RedirectButton roomLinkButton = new RedirectButton(page, "roomLinkButton", "Room Administration", "/de/iwes/apps/roomlink/gui/index.html");
		
		//RedirectButton calendarConfigButton = new RedirectButton(page, "calendarConfigButton",
		//		"Calendar Configuration", "/org/smartrplace/apps/smartrplaceheatcontrolv2/extensionpage.html");
		
		final OgemaWidget commitBtn;
		if(suppressSearchForNewDevices())
			commitBtn = null;
		else {
			commitBtn = getCommitButton();
		}
		/*Button commitBtn = new Button(page, "commitBtn", "Commit changes") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				EvalCollection ec = ResourceHelper.getEvalCollection(appMan);
				if(!ec.roomDeviceUpdateCounter().isActive()) {
					ec.roomDeviceUpdateCounter().create().activate(false);
				}
				ec.roomDeviceUpdateCounter().getAndAdd(1);
			}
		};*/
		
		topTable = new StaticTable(getTopTableLines(), 7, new int[] {2, 2, 1, 2, 1, 2, 2});
		int installFilterCol=3;
		if(installMode != null) {
			topTable.setContent(0, 0, roomsDrop.getFirstDropdown())
					.setContent(0, 1, roomsDrop)
					.setContent(0, installFilterCol, typeFilterDrop)
					.setContent(0, installFilterCol+2, installMode);//setContent(0, 2, roomLinkButton).
		}
		if(commitBtn != null) {
			topTable.setContent(0, 2, commitBtn);
		}
		if(offerAddRoomButton()) {
			RedirectButton addRoomLink = new RedirectButton(page, "addRoomLink", "Add room", "/org/smartrplace/external/accessadmin/roomconfig.html");
			topTable.setContent(0, installFilterCol+3, addRoomLink);
		}
		//RoomEditHelper.addButtonsToStaticTable(topTable, (WidgetPage<RoomLinkDictionary>) page,
		//		alert, appMan, 0, 3);
		//topTable.setContent(0, 5, calendarConfigButton);
		page.append(topTable);
		
		if(triggerFinishConstructorAutomatically)
			finishConstructor();
	}
	
	protected OgemaWidget getCommitButton() {
		Button commitBtn = new Button(page, "commitBtn", "Commit changes") {
			/*@Override
			public void onGET(OgemaHttpRequest req) {
				if(idRangeStart != null) {
					addStyle(ButtonData.BOOTSTRAP_RED, req);
				}
			}*/
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				EvalCollection ec = ResourceHelper.getEvalCollection(appMan);
				if(!ec.roomDeviceUpdateCounter().isActive()) {
					ec.roomDeviceUpdateCounter().create().activate(false);
				}
				ec.roomDeviceUpdateCounter().getAndAdd(1);
			}
		};
		return commitBtn;
	}
	
	protected void finishConstructor() {
		updateTables();
	}
	
	protected Set<String> tableProvidersDone = new HashSet<>();
	public void updateTables() {
		synchronized(tableProvidersDone) {
		//wait until standard sensors are there
		if(tableProvidersDone.isEmpty() && (!devHandAcc.leadHandlerFound()))
			return;
		List<DeviceHandlerProvider<?>> allProv = new ArrayList<DeviceHandlerProvider<?>>(devHandAcc.getTableProviders().values());
		allProv.sort(new Comparator<DeviceHandlerProvider<?>>() {

			@Override
			public int compare(DeviceHandlerProvider<?> o1, DeviceHandlerProvider<?> o2) {
				return o1.getTableTitle().compareToIgnoreCase(o2.getTableTitle());
			}
		});
		if(devHandAcc != null) for(DeviceHandlerProvider<?> pe: allProv) {
			//if(isObjectsInTableEmpty(pe))
			//	continue;
			String id = pe.id();
			if(hideDeviceHandler(pe))
				continue;
			if(tableProvidersDone.contains(id)) {
				continue;
			}
			tableProvidersDone.add(id);
			DeviceTableBase tableLoc = pe.getDeviceTable(page, alert, this, new DeviceTableConfig() {

				@Override
				public boolean showOnlyBaseCols() {
					return HardwareTablePage.this.showOnlyBaseColsHWT();
				}
				
				@Override
				public boolean emptyStateControlledExternally() {
					return true;
				}
				
			});
			tableLoc.triggerPageBuild();
			tableLoc.getMainTable().postponeLoading(); // these potentially heavy-weight tables can block the loading of the page otherwise
			tableLoc.getMainTable().setComposite(15_000 + (long) (Math.random() * 10_000));
			typeFilterDrop.registerDependentWidget(tableLoc.getMainTable());
			roomsDrop.registerDependentWidget(tableLoc.getMainTable());
			roomsDrop.getFirstDropdown().registerDependentWidget(tableLoc.getMainTable());
			subTables.add(new SubTableData(pe, tableLoc));
			
		}
		}
	}

	protected boolean isObjectsInTableEmpty(DeviceHandlerProvider<?> pe, OgemaHttpRequest req) {
		List<InstallAppDevice> all = getDevicesSelected(pe, req);
		return all.isEmpty();
	}

	private void checkIdRangeReset() {
		long now = appMan.getFrameworkTime();
		if(now - idRangeLastEdit > IDRANGE_RESET_INTERVAL) {
			idRangeStart = null;
			idRangeEnd = null;
			idRangeMaxNoLimit = IDRANGE_MAX_NOLIMIT_DEFAULT;
		}		
	}
	
	@Override
	public List<InstallAppDevice> getDevicesSelected(DeviceHandlerProvider<?> devHand, OgemaHttpRequest req) {
//if(Boolean.getBoolean("org.smartrplace.hwinstall.basetable.debugfiltering"))
//	System.out.println("Searching all devices for "+devHand.label(null));
		Collection<InstallAppDevice> all = getDevices(devHand);
//System.out.println("For "+pe.label(null)+" before filter:"+all.size());
//if(Boolean.getBoolean("org.smartrplace.hwinstall.basetable.debugfiltering"))
//	System.out.println("Filtering "+all.size()+" for "+devHand.label(null));
		List<InstallAppDevice> result = finalFilter.getFiltered(all, req);
		if(idRangeStart != null) {
			checkIdRangeReset();
		}
		return filterByIdRange(all, result);
		/*if(idRangeStart != null &&
				(idRangeMaxNoLimit == null || all.size() > idRangeMaxNoLimit)) {
			List<InstallAppDevice> resultIdRange = new ArrayList<>();
			for(InstallAppDevice iad: result) {
				int numId = LocalDeviceId.getDeviceIdNumericalPart(iad);
				if(numId < 0) {
					resultIdRange.add(iad);
					continue;
				}
				if(numId < idRangeStart)
					continue;
				if(numId == idRangeStart) {
					resultIdRange.add(iad);
					continue;
				}
				if(idRangeEnd != null && numId <=idRangeEnd)
					resultIdRange.add(iad);
			}
			return resultIdRange;
		}
//if(Boolean.getBoolean("org.smartrplace.hwinstall.basetable.debugfiltering"))
//	System.out.println("After Filtering has "+result.size()+" for "+devHand.label(null));
		return result;*/
	}
	
	public List<InstallAppDevice> filterByIdRange(Collection<InstallAppDevice> all, List<InstallAppDevice> filteredResult) {
		if(idRangeStart != null) {
			checkIdRangeReset();
		}
		if(idRangeStart != null &&
				(idRangeMaxNoLimit == null || all.size() > idRangeMaxNoLimit)) {
			List<InstallAppDevice> resultIdRange = new ArrayList<>();
			for(InstallAppDevice iad: filteredResult) {
				int numId = LocalDeviceId.getDeviceIdNumericalPart(iad);
				if(numId < 0) {
					resultIdRange.add(iad);
					continue;
				}
				if(numId < idRangeStart)
					continue;
				if(numId == idRangeStart) {
					resultIdRange.add(iad);
					continue;
				}
				if(idRangeEnd != null && numId <=idRangeEnd)
					resultIdRange.add(iad);
			}
			return resultIdRange;
		}
		return filteredResult;
	}
	
	public <T extends Resource> Collection<InstallAppDevice> getDevices(DeviceHandlerProviderDP<?> devHand) {
		//boolean includeInactiveDevices = resData.appConfigData.includeInactiveDevices().getValue();
		//return getDevices(tableProvider, includeInactiveDevices, false);
		return getDevices(devHand, false);
	}
	
	//private Map<String, List<InstallAppDevice>> devPerHandler = new HashMap<>();
	//private long lastMapUpd = -1;
	public <T extends Resource> Collection<InstallAppDevice> getDevices(DeviceHandlerProviderDP<?> devHand,
			boolean includeTrash) {
		return appManPlus.dpService().managedDeviceResoures(devHand==null?null:devHand.id(), false, includeTrash);
	}

	
	@Override
	public InstallAppDevice getInstallResource(Resource device) {
		throw new UnsupportedOperationException("Not implemented in HardwareTableBase!");
	}
	@Override
	public <T extends PhysicalElement> InstallAppDevice addDeviceIfNew(T model, DeviceHandlerProvider<T> tableProvider) {
		throw new UnsupportedOperationException("Not implemented in HardwareTableBase!");
	}
	@Override
	public <T extends Resource> InstallAppDevice removeDevice(T model) {
		throw new UnsupportedOperationException("Not implemented in HardwareTableBase!");
	}
	@Override
	public <T extends PhysicalElement> void startSimulation(DeviceHandlerProvider<T> tableProvider, T device) {
		throw new UnsupportedOperationException("Not implemented in HardwareTableBase!");		
	}
	@Override
	public void addWidgetsExpert(DeviceHandlerProvider<?> tableProvider, InstallAppDevice object,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		throw new UnsupportedOperationException("Not implemented in HardwareTableBase!");
	}
}


