package org.smartrplace.smarteff.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.model.prototypes.Data;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI.InitListener;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.button.BackButton;
import org.smartrplace.smarteff.util.button.LogicProvTableOpenButton;
import org.smartrplace.smarteff.util.button.TableOpenButton;
import org.smartrplace.smarteff.util.editgeneric.EditLineProvider;
import org.smartrplace.smarteff.util.editgeneric.EditLineProvider.Visibility;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.html.html5.flexbox.FlexWrap;
import de.iwes.widgets.html.html5.flexbox.JustifyContent;

/** The most general template page offers a table for editing the resource with 3 columns
 * and an arbitrary number of lines. Usually one line for each sub resource to be edited is
 * foreseen, but the lines can be defined as required via {@link EditTableBuilder#addEditLine(String, String)}
 * and its derivatives.<br>
 * The first columnn is usually foreseen for a description. The description can either be the title
 * set via addEditLine or you can give alternative titles for languages via {@link EditTableBuilder#setLabel(String, OgemaLocale, String)}
 * and create a dynamic label with {@link #getLabel(String, Visibility)} and a dynmic link button (if required) with
 * {@link #getLinkButton(String, Visibility)} .<br>
 * In the second column, the widget column, several widgets can be placed in a row using
 * {@link #getHorizontalFlexBox(WidgetPage, String, OgemaWidget...)}.<br>
 * Note that {@link EditPageGeneric} also can be used in a very flexible way. In this page you can add any widget to the second
 * column overwriting {@link EditLineProvider#valueColumn()}. You can also add lines that do not directly corrspond with
 * subresources by just using a unique resourceName as id in {@link EditPageGeneric#setLabel} etc.
 *
 * @param <T> resource type to be edited
 */
public abstract class EditPageBase<T extends Resource> extends NaviPageBase<T> {
	protected abstract void getEditTableLines(EditTableBuilder etb);
	/**  During this method resource elements may be initialized and checked
	 * @param data
	 * @return if true the resource is ok for usage and can be activated. If false activation
	 * 		is blocked.
	 */
	public abstract boolean checkResource(T data);
	protected void registerWidgetsAboveTable() {};

	@Override
	protected List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(primaryEntryTypeClass());
	}

	protected ObjectResourceGUIHelperExt mh;
	protected Alert alert;
	
	protected Map<String, Map<OgemaLocale, String>> labels = new LinkedHashMap<>();
	protected Map<String, Map<OgemaLocale, String>> links = new HashMap<>();
	protected OgemaLocale localeDefault = OgemaLocale.ENGLISH;
	public static OgemaLocale EN = OgemaLocale.ENGLISH;
	public static OgemaLocale DE = OgemaLocale.GERMAN;
	public static OgemaLocale FR = OgemaLocale.FRENCH;
	public static OgemaLocale CN = OgemaLocale.CHINESE;
	public static final Map<OgemaLocale, String> LINK_BUTTON_TEXTS = new HashMap<>();
	static {
		LINK_BUTTON_TEXTS.put(EN, "Info in Wiki");
		LINK_BUTTON_TEXTS.put(DE, "Info im Wiki");
		LINK_BUTTON_TEXTS.put(FR, "Info en Wiki");
	}
	
	public EditPageBase() {
		super();
	}
	
	public class EditElement {
		//Exactly one of the following should be non-null
		public String title;
		public OgemaWidget widgetForTitle;
		
		//Exactly one of the following should be non-null
		public OgemaWidget widget;
		public String stringForWidget;
		public OgemaWidget descriptionLink = null;
		
		public EditElement(String title, OgemaWidget widget) {
			this.title = title;
			this.widget = widget;
		}
		public EditElement(String title, String stringForWidget) {
			this.title = title;
			this.stringForWidget = stringForWidget;
		}
		public EditElement(OgemaWidget ogemaWidgetForTitle, OgemaWidget widget) {
			this.widgetForTitle = ogemaWidgetForTitle;
			this.widget = widget;
		}
		public void setDescriptionUrl(OgemaWidget descriptionLink) {
			this.descriptionLink = descriptionLink;
		}
	}
	public class EditTableBuilder {
		public List<EditElement> editElements = new ArrayList<>();
		public void addEditLine(String title, OgemaWidget widget) {
			editElements.add(new EditElement(title, widget));
		}
		public void addEditLine(OgemaWidget widgetForTitle, OgemaWidget widget) {
			editElements.add(new EditElement(widgetForTitle, widget));
		}
		public void addEditLine(String title, String stringForWidget) {
			editElements.add(new EditElement(title, stringForWidget));
		}
		public void addEditLine(OgemaWidget widgetForTitle, OgemaWidget widget, OgemaWidget descriptionLink) {
			EditPageBase<T>.EditElement el = new EditElement(widgetForTitle, widget);
			el.setDescriptionUrl(descriptionLink);
			editElements.add(el);
		}
		
		public void setLabel(String title, OgemaLocale locale, String text) {
			Map<OgemaLocale, String> innerMap = labels.get(title);
			if(innerMap == null) {
				innerMap = new HashMap<>();
				labels.put(title, innerMap);
			}
			innerMap.put(locale, text);
		}
		public void setLabel(String title, OgemaLocale locale, String text, OgemaLocale locale2, String text2) {
			setLabel(title, locale, text);
			setLabel(title, locale2, text2);
		}

	}
	
	protected class ObjectResourceGUIHelperExt extends ObjectResourceGUIHelperExtPublic<T> {

		public ObjectResourceGUIHelperExt(WidgetPage<?> page, TemplateInitSingleEmpty<T> init,
				ApplicationManager appMan, boolean acceptMissingResources) {
			super(page, init, appMan, acceptMissingResources);
		}
		
		@Override
		protected T getResource(T object, OgemaHttpRequest req) {
			return object;
		}
		
		@Override
		public T getGatewayInfo(OgemaHttpRequest req) {
			return getReqData(req);
		}
	}
	
	@Override
	protected void addWidgets() {
		mh = new ObjectResourceGUIHelperExt(page, (TemplateInitSingleEmpty<T>)null , null, false);
		mh.setDoRegisterDependentWidgets(true);
		if(alert == null) alert = new Alert(page, "alert"+pid(), "");
		page.append(alert);
		registerWidgetsAboveTable();
		
		buildMainTable();
		
		if(getEntryTypes() != null) exPage.registerInitExtension(new InitListener() {
			@Override
			public void onInitComplete(OgemaHttpRequest req) {
				T data = getReqData(req);
				checkResource(data);
			}
		});
	}
	
	protected class BuildMainTableCoreResult {
		public StaticTable table;
		public int c;
	}
	protected BuildMainTableCoreResult buildMainTableCore(Button activateButton) {
		EditTableBuilder etb = new EditTableBuilder();
		getEditTableLines(etb);

		StaticTable table = new StaticTable(etb.editElements.size()+1, 4, new int[]{1,5,5,1});
		int c = 0;
		for(EditPageBase<T>.EditElement etl: etb.editElements) {
			if((etl.title != null)&&(etl.widget != null)) {
				table.setContent(c, 1, etl.title).setContent(c,2, etl.widget);
				if(activateButton != null) etl.widget.registerDependentWidget(activateButton);
			} else if((etl.title != null)&&(etl.stringForWidget != null))
				table.setContent(c, 1, etl.title).setContent(c,2, etl.stringForWidget);
			else if((etl.widgetForTitle != null)&&(etl.widget != null)) {
				table.setContent(c, 1, etl.widgetForTitle).setContent(c,2, etl.widget);
				if(etl.descriptionLink != null) table.setContent(c, 3, etl.descriptionLink);
				if(activateButton != null) etl.widget.registerDependentWidget(activateButton);
			}
			else
				throw new IllegalStateException("Something went wrong with building the edit line "+c+" Obj:"+etl);
			c++;
		}
		BuildMainTableCoreResult result = new BuildMainTableCoreResult();
		result.table = table;
		result.c = c;
		return result;
	}
	
	protected void buildMainTable() {
		Button activateButton = new Button(page, "activateButton", "activate") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				T res = getReqData(req);
				if(res.isActive()) {
					setWidgetVisibility(false, req);
				} else {
					setWidgetVisibility(true, req);
					if(checkResource(res)) enable(req);
					else disable(req);
				}
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
				T res = getReqData(req);
				appData.systemAccess().activateResource(res);
			}
		};
		activateButton.registerDependentWidget(activateButton);

		BuildMainTableCoreResult tableData = buildMainTableCore(activateButton);
		TableOpenButton tableButton = new BackButton(page, "back", pid(), exPage, null);
		tableData.table.setContent(tableData.c, 0, activateButton).setContent(tableData.c, 1, tableButton);
		if(!Boolean.getBoolean("smartrefficiency.util.suppressCalculatorButton")) {
			TableOpenButton proposalTableOpenButton = new LogicProvTableOpenButton(page, "proposalTableOpenButton", pid(), exPage, null);
			tableData.table.setContent(tableData.c, 2, proposalTableOpenButton);
		}

		page.append(tableData.table);
		exPage.registerAppTableWidgetsDependentOnInit(tableData.table);
	}
	
	@Override
	protected PageType getPageType() {
		return PageType.EDIT_PAGE;
	}
	
	protected <R extends Resource> boolean checkResourceBase(R resource, boolean nameRelevant) {
		Data data;
		String name = null;
		if(nameRelevant) {
			data = (Data)resource;
			name = data.name().getValue();
			if(name.isEmpty()) return false;
		}
		@SuppressWarnings("unchecked")
		Class<R> type = (Class<R>) resource.getResourceType();
		List<R> otherOfType = resource.getParent().getSubResources(type, false);
		for(R ot: otherOfType) {
			if(ot.equalsLocation(resource)) continue;
			if(nameRelevant &&(((Data)ot).name().getValue().equals(name))) return false;
		}
		return true;
	}
	
	/** Get button to open link
	 * 
	 * @param title
	 * @param vis may be null
	 * @return null if no link is configured
	 */
	public RedirectButton getLinkButton(String title, Visibility vis) {
		Map<OgemaLocale, String> innerMap = labels.get(title);
		if(innerMap != null) return getLinkButton(title, innerMap, vis);
		else return null;
	}
	public RedirectButton getLinkButton(String title, Map<OgemaLocale, String> linkMap, Visibility vis) {
		return new RedirectButton(page, WidgetHelper.getValidWidgetId("linkButton"+title+pid()), "") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(vis != null) {
					if(vis == Visibility.HIDDEN) {
						setWidgetVisibility(false, req);
						return;
					} else {
						setWidgetVisibility(true, req);
						if(vis == Visibility.DISABLED) disable(req);
						else enable(req);
					}
				}
				String text = LINK_BUTTON_TEXTS.get(req.getLocale());
				if(text == null) text = LINK_BUTTON_TEXTS.get(localeDefault);
				if(text != null) setText(text, req);
				else setText("*"+title+"*", req);
			}
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				String text = linkMap.get(req.getLocale());
				if(text == null) text = linkMap.get(localeDefault);
				if(text != null) setUrl(text, req);
				else setUrl("*"+title+"*/error.html", req);
			}
		};		
	}
	/**
	 * 
	 * @param title
	 * @param innerMap
	 * @param vis may be null
	 * @return
	 */
	protected Label getLabel(String title, Visibility vis) {
		Map<OgemaLocale, String> innerMap = labels.get(title);
		if(innerMap != null) return getLabel(title, innerMap, vis);
		else return new Label(page,  WidgetHelper.getValidWidgetId("label"+title+pid()), title);
	}
	protected Label getLabel(String title, Map<OgemaLocale, String> innerMap, Visibility vis) {
		return new Label(page, WidgetHelper.getValidWidgetId("label"+title+pid())) {
			private static final long serialVersionUID = -2849170377959516221L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(vis != null) {
					if(vis == Visibility.HIDDEN) {
						setWidgetVisibility(false, req);
						return;
					} else {
						setWidgetVisibility(true, req);
						if(vis == Visibility.DISABLED) disable(req);
						else enable(req);
					}
				}
				String text = innerMap.get(req.getLocale());
				if(text == null) text = innerMap.get(localeDefault);
				if(text != null) setText(text, req);
				else setText("*"+title+"*", req);
			}
		};
	}
	
	public static Flexbox getHorizontalFlexBox(WidgetPage<?> page, String id, OgemaWidget... w1) {
		Flexbox flex = new Flexbox(page, id, true);
		for(OgemaWidget w: w1) {
			flex.addItem(w, null);			
		}
		flex.setDefaultJustifyContent(JustifyContent.SPACE_AROUND);
		flex.setDefaultFlexWrap(FlexWrap.NOWRAP);
		return flex;
	}
}
