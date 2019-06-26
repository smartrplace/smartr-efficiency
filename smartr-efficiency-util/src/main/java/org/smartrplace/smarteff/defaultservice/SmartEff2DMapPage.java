package org.smartrplace.smarteff.defaultservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.core.model.array.FloatArrayResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.SmartEff2DMap;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.NaviPageBase;
import org.smartrplace.smarteff.util.button.BackButton;
import org.smartrplace.smarteff.util.button.TabButton;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.resource.widget.table.DefaultResourceRowTemplate;
import de.iwes.widgets.resource.widget.table.ResourceTable;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class SmartEff2DMapPage extends NaviPageBase<SmartEff2DMap> {
	/** 0: normal, 1: temperatures*/
	protected final int modeKey1;
	protected final int modeKey2;
	protected final int modeValues;
	
	public static final Map<OgemaLocale, String> RESALL_BUTTON_TEXTS = new HashMap<>();
	static {
		RESALL_BUTTON_TEXTS.put(OgemaLocale.ENGLISH, "All Sub-Resources");
		RESALL_BUTTON_TEXTS.put(OgemaLocale.GERMAN, "Alle Unterressourcen");
		RESALL_BUTTON_TEXTS.put(OgemaLocale.FRENCH, "Tous Sub-Ressources");
	}

	protected TabButton tabButton;
	protected Button addPrimaryKeyButton;
	protected Button addSecondaryKeyButton;
	
	protected boolean isInherited() { return false;}
	protected final boolean hasAddResourceColumn;
	
	protected Resource getFixedParentResource(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {return null;}

	
	
	protected void addWidgetsAboveTable() {
		tabButton = new TabButton(page, "tabButton", pid());
		BackButton backButton = new BackButton(page, "backButton", pid(), exPage, tabButton.control);
		
		addPrimaryKeyButton = new Button(page, "addPrimaryKeyButton", "Add Line") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				SmartEff2DMap entryRes = getReqData(req);
				ValueResourceUtils.appendValue(entryRes.primaryKeys(),
						entryRes.primaryKeys().getElementValue(entryRes.primaryKeys().size()-1)+1);
			}
		};
		addSecondaryKeyButton = new Button(page, "addScondaryKeyButton", "Add Column") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				SmartEff2DMap entryRes = getReqData(req);
				ValueResourceUtils.appendValue(entryRes.secondaryKeys(),
						entryRes.secondaryKeys().getElementValue(entryRes.secondaryKeys().size()-1)+1);
			}
		};
		
		StaticTable topTable = new StaticTable(1, 4);
		topTable.setContent(0, 0, backButton).
		setContent(0, 1, addPrimaryKeyButton).setContent(0, 2, addSecondaryKeyButton).
		setContent(0, 3, tabButton);
		page.append(topTable);		
	}
	
	protected TablePage tablePage;
	
	public SmartEff2DMapPage() {
		this(true);
	}
	public SmartEff2DMapPage(int modeKey1, int modeKey2, int modeValues) {
		this(true, modeKey1, modeKey2, modeValues);
	}

	public SmartEff2DMapPage(boolean hasAddResourceColumn) {
		this(hasAddResourceColumn, 0, 0, 0);
	}
	public SmartEff2DMapPage(boolean hasAddResourceColumn, int modeKey1, int modeKey2, int modeValues) {
		super();
		this.hasAddResourceColumn = hasAddResourceColumn;
		this.modeKey1 = modeKey1;
		this.modeKey2 = modeKey2;
		this.modeValues = modeValues;
	}

	public class TablePage {
		//private final ApplicationManagerMinimal appManMin;
		//private ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
		protected final ResourceTable<FloatArrayResource> resTable;
		
		public TablePage(ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage, ApplicationManagerMinimal appManMin) {
			//this.exPage = exPage;
			
			addWidgetsAboveTable();
			
			RowTemplate<FloatArrayResource> tableTemplate = new DefaultResourceRowTemplate<FloatArrayResource>() {
				@Override
				public String getLineId(FloatArrayResource arr) {
					LineInfo li = getLineInfo(arr);
					if(li.isHeaderLine)
						return "0000"+super.getLineId(arr);
					else
						return String.format("1"+"%03d", li.lineIndex)+super.getLineId(arr);
				}
				
				@Override
				public Row addRow(FloatArrayResource arr, OgemaHttpRequest req) {
					String id = getLineId(arr);
					Row row = new Row();
					LineInfo li = getLineInfo(arr);
					if(!li.isHeaderLine) {
						TextField editHead = new FloatArrayEdit(resTable, "editHead"+id, li.lineHeader, li.lineIndex, req);
						editHead.addCssStyle("font-weight", "bold", req);
						row.addCell("editHead", editHead);
					} else
						row.addCell("editHead", "-");
					for(int idx=0; idx<li.columnNum; idx++) {
						if(arr.size() <= idx) ValueResourceUtils.setValue(arr, arr.size(), Float.NaN);
						TextField editVal = new FloatArrayEdit(resTable, "editVal"+idx+"_"+id, arr, idx, req);
						row.addCell("editVal"+idx, editVal);
						if(li.isHeaderLine)
							editVal.addCssStyle("font-weight", "bold", req);
					}
					return row;
				}
			};
			resTable = new ResourceTable<FloatArrayResource>(page, "resTable"+pid(), tableTemplate) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					List<FloatArrayResource> lines = new ArrayList<>();
					SmartEff2DMap entryRes = getReqData(req);
					if(!entryRes.exists())
						initRes(entryRes);
					lines.add(entryRes.secondaryKeys());
					for(int idx=0; idx<entryRes.primaryKeys().size(); idx++) {
						if(entryRes.characteristics().size() <= idx) {
							lines.add(entryRes.characteristics().add());
						} else
							lines.add(entryRes.characteristics().getAllElements().get(idx));
					}
					updateRows(lines, req);
				}
			};
			page.append(resTable);
		}
	}
	
	protected class LineInfo {
		int columnNum;
		FloatArrayResource lineHeader = null;
		int lineIndex;
		boolean isHeaderLine;
	}
	protected LineInfo getLineInfo(FloatArrayResource arr) {
		final SmartEff2DMap map;
		if(arr.getName().equals("secondaryKeys"))
			map = arr.getParent();
		else
			map = arr.getParent().getParent();
		LineInfo res = new LineInfo();
		res.columnNum = map.secondaryKeys().getValues().length;
		res.isHeaderLine = arr.equalsLocation(map.secondaryKeys());
		if(res.isHeaderLine) return res;
		List<FloatArrayResource> lists = map.characteristics().getAllElements();
		for(int idx = 0; idx < map.primaryKeys().size(); idx++) {
			final FloatArrayResource primaryArr;
			if(lists.size() <= idx)
				primaryArr = map.characteristics().add();
			else
				primaryArr = lists.get(idx);
			if(primaryArr.equalsLocation(arr)) {
				res.lineHeader = map.primaryKeys();
				res.lineIndex = idx;
				break;
			}
		}
		if(res.lineHeader == null)
			throw new IllegalStateException("Line header not found!");
		return res; 
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
		return SmartEff2DMap.class;
	}
	
	public static String PID = SmartEff2DMapPage.class.getSimpleName();
	@Override //optional
	public String pid() {
		return PID;
	}

	@Override
	protected String label(OgemaLocale locale) {
		return "Characteristics 2D map edit page";
	}

	@Override
	protected void addWidgets() {
		tablePage = new TablePage(exPage, appManExt);		
	}

	@Override
	protected String getHeader(OgemaHttpRequest req) {
		return "Characteristics 2D map in "+ResourceUtils.getHumanReadableName(getReqData(req)); //super.getHeader(req);
	}
	
	@Override
	protected List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(primaryEntryTypeClass());
	}

	@Override
	protected PageType getPageType() {
		return PageType.EDIT_PAGE;
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
	
	public static class FloatArrayEdit extends TextField {
		private static final long serialVersionUID = 1L;
		final FloatArrayResource res;
		final int index;
		
		public FloatArrayEdit(OgemaWidget parent, String id, FloatArrayResource res, int index,
				OgemaHttpRequest req) {
			super(parent, id, req);
			this.res = res;
			this.index = index;
		}

		@Override
		public void onGET(OgemaHttpRequest req) {
			setValue(String.format("%.2f", res.getElementValue(index)), req);
		}
		
		@Override
		public void onPOSTComplete(String data, OgemaHttpRequest req) {
			String vals = getValue(req);
			try {
				float val = Float.parseFloat(vals);
				ValueResourceUtils.setValue(res, index, val);
			} catch(NumberFormatException e) {
				
			}
		}
	}
	
	/** TODO: This should be moved into the respective application, only for testing here*/
	public static void initRes(SmartEff2DMap entryRes) {
		entryRes.primaryKeys().create();
		entryRes.secondaryKeys().create();
		entryRes.characteristics().create();
		entryRes.primaryKeys().setValues(new float[]{0.0f, 1.0f});
		entryRes.secondaryKeys().setValues(new float[]{0.0f, 1.0f});
		FloatArrayResource arr = entryRes.characteristics().add();
		arr.setValues(new float[]{-1, 1});
		arr = entryRes.characteristics().add();
		arr.setValues(new float[]{2, 5});
		entryRes.activate(true);
	}

}
