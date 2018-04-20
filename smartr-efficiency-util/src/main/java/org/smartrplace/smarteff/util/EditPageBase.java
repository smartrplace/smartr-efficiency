package org.smartrplace.smarteff.util;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.model.prototypes.Data;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI.InitListener;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.smarteff.util.button.BackButton;
import org.smartrplace.smarteff.util.button.ProposalProvTableOpenButton;
import org.smartrplace.smarteff.util.button.TableOpenButton;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;

public abstract class EditPageBase<T extends Resource> extends NaviPageBase<T> {
	protected abstract void getEditTableLines(EditTableBuilder etb);
	public abstract boolean checkResource(T data);
	protected void registerWidgetsAboveTable() {};

	@Override
	protected List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(primaryEntryTypeClass());
	}

	protected ObjectResourceGUIHelperExt mh;
	protected Alert alert;
	
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
		public OgemaWidget decriptionLink = null;
		
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
			decriptionLink = descriptionLink;
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
	}
	
	protected class ObjectResourceGUIHelperExt extends ObjectResourceGUIHelper<T, T> {

		public ObjectResourceGUIHelperExt(WidgetPage<?> page, TemplateInitSingleEmpty<T> init,
				ApplicationManager appMan, boolean acceptMissingResources) {
			super(page, init, appMan, acceptMissingResources);
			// TODO Auto-generated constructor stub
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
		alert = new Alert(page, "alert"+pid(), "");
		page.append(alert);
		registerWidgetsAboveTable();
		
		buildMainTable();
		
		exPage.registerInitExtension(new InitListener() {
			@Override
			public void onInitComplete(OgemaHttpRequest req) {
				T data = getReqData(req);
				checkResource(data);
			}
		});
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

		EditTableBuilder etb = new EditTableBuilder();
		getEditTableLines(etb);

		StaticTable table = new StaticTable(etb.editElements.size()+1, 4, new int[]{1,5,5,1});
		int c = 0;
		for(EditPageBase<T>.EditElement etl: etb.editElements) {
			if((etl.title != null)&&(etl.widget != null)) {
				table.setContent(c, 1, etl.title).setContent(c,2, etl.widget);
				etl.widget.registerDependentWidget(activateButton);
			} else if((etl.title != null)&&(etl.stringForWidget != null))
				table.setContent(c, 1, etl.title).setContent(c,2, etl.stringForWidget);
			else if((etl.widgetForTitle != null)&&(etl.widget != null)) {
				table.setContent(c, 1, etl.widgetForTitle).setContent(c,2, etl.widget);
				if(etl.decriptionLink != null) table.setContent(c, 3, etl.decriptionLink);
				etl.widget.registerDependentWidget(activateButton);
			}
			else
				throw new IllegalStateException("Something went wrong with building the edit line "+c+" Obj:"+etl);
			c++;
		}
		TableOpenButton tableButton = new BackButton(page, "back", pid(), exPage, null);
		table.setContent(c, 0, activateButton).setContent(c, 1, tableButton);
		TableOpenButton proposalTableOpenButton = new ProposalProvTableOpenButton(page, "proposalTableOpenButton", pid(), exPage, null);
		table.setContent(c, 2, proposalTableOpenButton);

		page.append(table);
		exPage.registerAppTableWidgetsDependentOnInit(table);
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

}
