package org.smartrplace.smarteff.defaultservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.extensionservice.proposal.CalculatedData;
import org.smartrplace.extensionservice.proposal.ProjectProposalEfficiency;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.NaviPageBase;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.smarteff.util.button.AddEditButton;
import org.smartrplace.smarteff.util.button.AddEntryButton;
import org.smartrplace.smarteff.util.button.ButtonControlProvider;
import org.smartrplace.smarteff.util.button.LogicProvTableOpenButton;
import org.smartrplace.smarteff.util.button.ProposalResTableOpenButton;
import org.smartrplace.smarteff.util.button.ResourceOfTypeTableOpenButton;
import org.smartrplace.smarteff.util.button.ResourceTableOpenButton;
import org.smartrplace.smarteff.util.button.TabButton;
import org.smartrplace.smarteff.util.button.TableOpenButton;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;
import org.smartrplace.util.directresourcegui.ResourceGUITablePage;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class ResourceTablePage extends NaviPageBase<Resource> {
	public static final Map<OgemaLocale, String> RESALL_BUTTON_TEXTS = new HashMap<>();
	static {
		RESALL_BUTTON_TEXTS.put(OgemaLocale.ENGLISH, "All Sub-Resources");
		RESALL_BUTTON_TEXTS.put(OgemaLocale.GERMAN, "Alle Unterressourcen");
		RESALL_BUTTON_TEXTS.put(OgemaLocale.FRENCH, "Tous Sub-Ressources");
	}

	protected TabButton tabButton;

	protected boolean isInherited() { return false;}
	protected final boolean hasAddResourceColumn;
	
	protected Resource getFixedParentResource(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {return null;}

	protected void addWidgetsAboveTable(Class<? extends Resource> resourceType) {
		tabButton = new TabButton(page, "tabButton", pid());
		
		AddEditButton editResource = new AddEditButton(page, "editEntry", pid(), exPage,
				tabButton.control);
		TableOpenButton allResourceButton2;
		if(isInherited()) {
			allResourceButton2 = new ResourceTableOpenButton(page, "allResourceButton", pid(), exPage, tabButton.control);
		} else {
			allResourceButton2 = new TableOpenButton(page, "allResourceButton", pid(), "All Resources", exPage, tabButton.control) {
				private static final long serialVersionUID = 1L;
				@Override
				protected NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
						Class<? extends Resource> type, PageType typeRequested, OgemaHttpRequest req) {
					return appData.systemAccessForPageOpening().getPageByProvider(SPPageUtil.getProviderURL(BaseDataService.RESOURCEALL_NAVI_PROVIDER));//super.getPageData(appData, type, typeRequested);
				}
				@Override
				public void onGET(OgemaHttpRequest req) {
					super.onGET(req);
					ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
					List<Resource> resultAll = getResource(appData, req).getSubResources(false);
					List<Resource> result = new ArrayList<>();
					for(Resource r: resultAll) {
						if(!(r instanceof ValueResource)) result.add(r);
					}
					String text = RESALL_BUTTON_TEXTS.get(req.getLocale());
					if(text == null) text = RESALL_BUTTON_TEXTS.get(OgemaLocale.ENGLISH);
					setText(text+"("+result.size()+")", req);
				}
			};
			allResourceButton2.setDefaultOpenInNewTab(false);
		}
		addTopButtonsExceptFirstTwo(editResource, allResourceButton2, tabButton);
	}
	
	protected void addTopButtonsExceptFirstTwo(AddEditButton editResource, OgemaWidget allResourceButton2, TabButton tabButton) {
		TableOpenButton proposalTableOpenButton = new LogicProvTableOpenButton(page, "proposalTableOpenButton", pid(), exPage, tabButton.control);
		TableOpenButton resultTableOpenButton = new ProposalResTableOpenButton(page, "resultTableOpenButton", pid(), exPage, tabButton.control, ProjectProposalEfficiency.class);
		TableOpenButton upTableOpenButton = new ResourceTableOpenButton(page, "upTableOpenButton", pid(), exPage, tabButton.control,true);
		
		StaticTable topTable = new StaticTable(1, 6);
		topTable.setContent(0, 0, editResource).setContent(0, 1, allResourceButton2).
				setContent(0, 2, proposalTableOpenButton).setContent(0, 3, resultTableOpenButton).
				setContent(0,  4, upTableOpenButton).setContent(0, 5, tabButton);
		page.append(topTable);		
	}
	
	protected TablePage tablePage;
	
	public ResourceTablePage() {
		this(true);
	}
	public ResourceTablePage(boolean hasAddResourceColumn) {
		super();
		this.hasAddResourceColumn = hasAddResourceColumn;
	}

	public class TablePage extends ResourceGUITablePage<Resource> {
		//private final ApplicationManagerMinimal appManMin;
		private ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
		
		public TablePage(ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage, ApplicationManagerMinimal appManMin) {
			super(exPage.getPage(), null, appManMin, Resource.class, false);
			this.exPage = exPage;
			//this.appManMin = appManMin;
			finishInit();
			retardationOnGET = 1000;
		}
		protected void finishInit() {
			triggerPageBuild();			
		}

		@Override
		public void addWidgets(Resource object, ResourceGUIHelper<Resource> vh, String id,
				OgemaHttpRequest req, Row row, ApplicationManager appMan) {
			addWidgetsInternal(object, vh, id, req, row, appMan, pid(), appManExt, exPage,
					(tabButton!=null)?tabButton.control:null, hasAddResourceColumn);
		}

		@Override
		public void addWidgetsAboveTable() {
			ResourceTablePage.this.addWidgetsAboveTable(resourceType);
		}
		
		@Override
		public List<Resource> getResourcesInTable(OgemaHttpRequest req) {
			return provideResourcesInTable(req);
		}
	}

	protected List<Resource> provideResourcesInTable(OgemaHttpRequest req) {
		Class<? extends Resource> resourceType = getReqData(req).getResourceType();
		List<Class<? extends Resource>> types = appManExt.getSubTypes(resourceType);
		List<Resource> result = new ArrayList<>();
		Resource parent = getReqData(req);
		for(Class<? extends Resource> t: types) {
			List<? extends Resource> resOfType = parent.getSubResources(t, false);
			if(resOfType.isEmpty()) {
				result.add(parent.getSubResource("Virtual"+t.getSimpleName(), t));
			} else result.addAll(resOfType);
		}
		return result;
	}
	
	@Override
	protected Class<? extends Resource> primaryEntryTypeClass() {
		return Resource.class;
	}
	
	public static String PID = ResourceTablePage.class.getSimpleName();
	@Override //optional
	public String pid() {
		return PID;
	}

	@Override
	protected String label(OgemaLocale locale) {
		return "Generic Resource Overview Table";
	}

	@Override
	protected void addWidgets() {
		tablePage = new TablePage(exPage, appManExt);		
	}

	@Override
	protected String getHeader(OgemaHttpRequest req) {
		return "Resources by Type in "+ResourceUtils.getHumanReadableName(getReqData(req)); //super.getHeader(req);
	}
	
	@Override
	protected List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(primaryEntryTypeClass());
	}

	@Override
	protected PageType getPageType() {
		return PageType.TABLE_PAGE;
	}
	
	/*@Override
	protected PagePriority getPriority() {
		return PagePriority.SECONDARY;
	}*/
	
	public static String getSimpleName(Resource resource) {
		Resource name = resource.getSubResource("name");
		if ((name != null) && (name instanceof StringResource)) {
			String val = ((StringResource) (name)).getValue();
			if (name.isActive() && (!val.trim().isEmpty()))
				return val;
		}
		return resource.getName();
	}

	public void addWidgetsInternal(Resource object, ResourceGUIHelper<Resource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			String pid, ApplicationManagerSPExt appManExt,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider tabController,
			boolean hasAddResourceColumn) {
		ExtensionResourceAccessInitData appData = null;
		if(req != null) appData = exPage.getAccessData(req);
		vh.stringLabel("Type", id, object.getResourceType().getSimpleName(), row);
		if(object.exists()) {
			vh.stringLabel("Name", id, getSimpleName(object), row);
			if(object instanceof CalculatedData) {
				//TODO: Page that provides details of result without option to edit
				//vh.stringLabel("Edit", id, "Result Resource", row);
				SPPageUtil.addResEditOpenButton("Edit", object, vh, id, row, appData, tabController, req);
				SPPageUtil.addResTableOpenButton("Open", object, vh, id, row, appData, tabController, req);
				vh.stringLabel("Evaluations", id, "--", row);
				vh.stringLabel("Delete", id, "--", row);
				if(hasAddResourceColumn) vh.stringLabel("AddResource", id, "--", row);
			} else {
				SPPageUtil.addResEditOpenButton("Edit", object, vh, id, row, appData, tabController, req);
				SPPageUtil.addResTableOpenButton("Open", object, vh, id, row, appData, tabController, req);
				if(object.isActive()) {
					SPPageUtil.addProviderTableOpenButton("Evaluations", object, vh, id, row, appData, tabController, req);
					//TableOpenButton proposalTableOpenButton = new StandardProposalTableOpenButton(vh.getParent(), "proposalTableOpenButton", PID, "Proposal providers", object.getResourceType(), exPage, req);
					//row.addCell("Evaluations", proposalTableOpenButton);
				} else
					vh.stringLabel("Evaluations", id, "Inactive", row);
				vh.linkingButton("Delete", id, object, row, "Delete", "delete.html");
				ExtensionResourceTypeDeclaration<? extends Resource> typeDecl = appManExt.getTypeDeclaration(object.getResourceType());
				if(hasAddResourceColumn) {
					if(SPPageUtil.isMulti(typeDecl.cardinality())) {
						Resource res = getFixedParentResource(appData, req);
						AddEntryButton addButton = new AddEntryButton(vh.getParent(), id, pid, "Add Sub Resource",  object.getResourceType(), exPage, tabController,
								res, req);
						row.addCell("AddResource", addButton);					
					} else {
						vh.stringLabel("AddResource", id, "SingleResource", row);
					}
				}
			}
		} else {
			vh.registerHeaderEntry("Name");
			vh.registerHeaderEntry("Edit");
			ExtensionResourceTypeDeclaration<? extends Resource> decl = appManExt.getTypeDeclaration(object.getResourceType());
			if(req != null && decl != null && SPPageUtil.isMulti(decl.cardinality())) {
				ResourceOfTypeTableOpenButton openButton = new ResourceOfTypeTableOpenButton(vh.getParent(), "ResourceOfTypeTableButton"+id, pid, exPage, tabController, req) {
					private static final long serialVersionUID = 1L;

					@Override
					protected Class<? extends Resource> typeToOpen(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
						return object.getResourceType();
					}
				};
				row.addCell("Open", openButton);
			} else vh.registerHeaderEntry("Open");
			vh.registerHeaderEntry("Evaluations");
			vh.registerHeaderEntry("Delete");
			if(hasAddResourceColumn) {if(req != null) {
					Resource res = getFixedParentResource(appData, req);
					AddEntryButton addButton = new AddEntryButton(vh.getParent(), id, pid, "Add Sub Resource", object.getResourceType(), exPage, tabController,
							res, req);
					row.addCell("AddResource", addButton);
				} else vh.registerHeaderEntry("AddResource");
			}
		}
	}
}
