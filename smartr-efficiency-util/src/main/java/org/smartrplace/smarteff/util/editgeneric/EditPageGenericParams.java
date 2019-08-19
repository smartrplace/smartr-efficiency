package org.smartrplace.smarteff.util.editgeneric;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.EditPageBase;
import org.smartrplace.smarteff.util.button.BackButton;
import org.smartrplace.smarteff.util.button.LogicProvTableOpenButton;
import org.smartrplace.smarteff.util.button.TableOpenButton;

import de.iwes.util.resource.OGEMAResourceCopyHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
import de.iwes.widgets.html.form.label.Label;

/** Base template for paramter edit pages that allow to see global parameters and to create local variants for
 * each value. The input resource can be the global resource or the local user resource.
 *
 * @param <T> parameter resource type to be edited.
 */
public abstract class EditPageGenericParams<T extends Resource> extends EditPageGenericWithTable<T> {
	public EditPageGenericParams() {
		super(true, false);
	}
	public EditPageGenericParams(boolean isWithTable) {
		super(isWithTable, false);
	}
	public EditPageGenericParams(List<EditPageGenericTableWidgetProvider<T>> additionalWidgetProviders,
			boolean isWithTable) {
		super(additionalWidgetProviders, isWithTable, false);
	}
	
	@Override
	protected String pid() {
		return super.pid()+"pars";
	}
	
	protected class EditElementParams extends EditElement {
		public final OgemaWidget globalValueWidget;
		public final OgemaWidget controlWidget;
		public EditElementParams(OgemaWidget ogemaWidgetForTitle, OgemaWidget valueWidget,
				OgemaWidget globalValueWidget, OgemaWidget controlWidget) {
			super(ogemaWidgetForTitle, valueWidget);
			this.globalValueWidget = globalValueWidget;
			this.controlWidget = controlWidget;
		}
	}
	
	protected class EditTableBuilderParams extends EditTableBuilder {
		public void addEditLine(OgemaWidget widgetForTitle, OgemaWidget widget,
				OgemaWidget globalValueWidget, OgemaWidget controlWidget,
				OgemaWidget descriptionLink) {
			EditElementParams el = new EditElementParams(widgetForTitle, widget, globalValueWidget, controlWidget);
			el.setDescriptionUrl(descriptionLink);
			editElements.add(el);
		}
	}

	//TODO: This is quite costly
	@Override
	protected T getReqData(OgemaHttpRequest req) {
		T entryRes = super.getReqData(req);
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		return CapabilityHelper.getParamVariants(entryRes, appData.userData(), appManExt).userVariant;
	}
	protected T getReqDataGlobal(OgemaHttpRequest req) {
		T entryRes = super.getReqData(req);
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		return CapabilityHelper.getParamVariants(entryRes, appData.userData(), appManExt).globalVariant;
	}
	
	@Override
	protected void buildMainTable() {
		EditTableBuilderParams etb = new EditTableBuilderParams();
		getEditTableLines(etb);

		StaticTable table = new StaticTable(etb.editElements.size()+1, 6, new int[]{1,4,2,2,1,2});
		int c = 0;
		for(EditElement etl: etb.editElements) {
			if((etl.title != null)&&(etl.widget != null)) {
				table.setContent(c, 1, etl.title).setContent(c,2, etl.widget);
				//etl.widget.registerDependentWidget(activateButton);
			} else if((etl.title != null)&&(etl.stringForWidget != null))
				table.setContent(c, 1, etl.title).setContent(c,2, etl.stringForWidget);
			else if((etl.widgetForTitle != null)&&(etl.widget != null)) {
				table.setContent(c, 1, etl.widgetForTitle).setContent(c,2, etl.widget);
				if(etl.descriptionLink != null) table.setContent(c, 5, etl.descriptionLink);
				//etl.widget.registerDependentWidget(activateButton);
				if(etl instanceof EditPageGenericParams.EditElementParams) {
					@SuppressWarnings("unchecked")
					EditElementParams etlp = (EditElementParams)etl;
					if(etlp.globalValueWidget != null) table.setContent(c, 3, etlp.globalValueWidget);
					if(etlp.controlWidget != null) table.setContent(c, 4, etlp.controlWidget);
				}
			}
			else
				throw new IllegalStateException("Something went wrong with building the edit line "+c+" Obj:"+etl);
			c++;
		}
		RedirectButton allParamsButton = new RedirectButton(page, "allParamsButton", "My Params", "org_smartrplace_smarteff_defaultservice_TopConfigTablePage.html");
		allParamsButton.setDefaultOpenInNewTab(false);
		TableOpenButton backButton = new BackButton(page, "back", pid(), exPage, null);
		table.setContent(c, 0, "").setContent(c, 1, backButton);
		TableOpenButton proposalTableOpenButton = new LogicProvTableOpenButton(page, "proposalTableOpenButton", pid(), exPage, null);
		table.setContent(c, 2, proposalTableOpenButton);

		page.append(table);
		exPage.registerAppTableWidgetsDependentOnInit(table);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void performAddEditLine(OgemaWidget label, OgemaWidget valueWidget, OgemaWidget linkButton, String sub,
			TypeResult type, EditPageBase<T>.EditTableBuilder etb, EditLineProvider orgProvider) {
		
		//We have finished to create widgets, so we can replace the provider
		EditLineProviderDisabling paramProvider = new EditLineProviderDisabling() {
			@Override
			protected boolean enable(OgemaHttpRequest req) {
				if(sub.startsWith("#")) return true;
				Resource res = ResourceHelper.getSubResource(getReqData(req), sub);
				if((res == null) || (!res.isActive())) return false;
				return true;
			}
		};
		setLineProvider(sub, paramProvider);
		
		ObjectResourceGUIHelperExt mhGlob = new ObjectResourceGUIHelperExt(page, (TemplateInitSingleEmpty<T>)null , null, false) {
			@Override
			public T getGatewayInfo(OgemaHttpRequest req) {
				return getReqDataGlobal(req);
			}
		};

		OgemaWidget globalValueWidget = createValueWidget(sub, type, (Label)label, mhGlob, false, "glob");
		String buttonId = ResourceUtils.getValidResourceName("control_" + sub + pid()); //sub.replace("/",  "_")
		Button control = new Button(page, buttonId) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(sub.startsWith("#")) {
					valueWidget.disable(req);
					setText("--", req);
					return;
				}
				boolean enable = paramProvider.enable(req);
				if(enable) {
					valueWidget.enable(req);
					setText("Use Global", req);
				} else {
					valueWidget.disable(req);
					setText("Use Local", req);
				}
			}
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				Resource res = ResourceHelper.getSubResource(getReqData(req), sub);
				//boolean enable =
				paramProvider.enable(req);
				if(res == null) {
					alert.showAlert("Resource path null, user resource cannot be generated", false, req);
				} else if(!res.isActive()) {
					if(!res.exists()) {
						res.create();
						Resource global = ResourceHelper.getSubResource(getReqDataGlobal(req), sub);
						if(global != null)
							OGEMAResourceCopyHelper.copySubResourceIntoDestination(global, res, null, false);
					}
					res.activate(false);
				} else {
					res.deactivate(false);
				}
				onGET(req);
			}
		};
		control.registerDependentWidget(valueWidget);
		control.registerDependentWidget(alert);
		control.registerDependentWidget(control);
		
		if(etb instanceof EditPageGenericParams.EditTableBuilderParams) {
			((EditTableBuilderParams)etb).addEditLine(label, valueWidget, linkButton, globalValueWidget, control);
		} else etb.addEditLine(label, valueWidget, linkButton);
	}
}
