package org.smartrplace.smarteff.util.editgeneric;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.smarteff.defaultservice.ResourceTablePage;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;
import org.smartrplace.util.directresourcegui.GUIHelperExtension;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;
import org.smartrplace.util.format.ValueFormat;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class GenericResourceByTypeTablePage<T extends Resource> extends GenericResourceByTypeTablePageBase<T> {
	private final EditPageGenericWithTable<T> creatingPage;
	
	public GenericResourceByTypeTablePage(EditPageGenericWithTable<T> creatingPage) {
		super();
		this.creatingPage = creatingPage;
	}

	class TablePageEPM extends ResourceTablePage.TablePage {
		public TablePageEPM(
				ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
				ApplicationManagerMinimal appManMin) {
			super(exPage, appManMin);
		}

		@Override
		public void addWidgets(Resource object, ResourceGUIHelper<Resource> vh, String id,
				OgemaHttpRequest req, Row row, ApplicationManager appMan) {
			ExtensionResourceAccessInitData appData = null;
			if(req != null) {
				appData = exPage.getAccessData(req); //creatingPage.getExPage().getAccessData(req);
			}
			Map<String, Map<OgemaLocale, String>> locMap = creatingPage.tableHeaders;
			for(String sub: locMap.keySet()) {
				Resource cellObject = ResourceHelper.getSubResource(object, sub);
				
				//TODO: Language-specific headers are not supported yet by GUIHelper
				String text = ValueFormat.getLocaleString(OgemaLocale.ENGLISH, locMap.get(sub));
				
				if(cellObject == null)
					vh.stringLabel(text, id, "n/a", row);
				else if(cellObject instanceof FloatResource)
					vh.floatLabel(text, id, (FloatResource)cellObject, row, "%.2f");
				else if(cellObject instanceof IntegerResource)
					vh.intLabel(sub, id, (IntegerResource)cellObject, row, 0);
				else if(cellObject instanceof BooleanResource)
					vh.stringLabel(text, id, ""+((BooleanResource)cellObject).getValue(), row);
				else if(cellObject instanceof TimeResource) {
					if(sub.contains("Day"))
						vh.timeLabel(text, id, (TimeResource)cellObject, row, 4);
					else
						vh.timeLabel(text, id, (TimeResource)cellObject, row, 0);
				} else if(cellObject instanceof SingleValueResource)
					vh.stringLabel(text, id, ValueResourceUtils.getValue((SingleValueResource)cellObject), row);
				else
					vh.stringLabel(text, id, "not a value", row);
			}
			if(req != null) {
				SPPageUtil.addResEditOpenButton("Edit", object, vh, id, row, appData, tabButton.control, req);
			} else {
				vh.registerHeaderEntry("Edit");
			}
			GUIHelperExtension.addDeleteButton(null, object, mainTable, id, alert, row, vh, req);
		}
		
		//We trigger the init ourselves
		@Override
		protected void finishInit() {}
	}
	
	/** The page can be used to show the subresources below any tree entry point*/
	@Override
	protected Class<Resource> primaryEntryTypeClass() {
		return Resource.class;
		//return super.primaryEntryTypeClass();
	}
	
	@Override
	protected List<Class<? extends Resource>> typesListedInTable() {
		List<Class<? extends Resource>> result = new ArrayList<>();
		result.add(creatingPage.getPrimaryEntryTypeClass());
		return result ;
	}
	
	//TODO
	@Override
	protected String getMaintainer() {
		return super.getMaintainer();
	}
	
	//@Override
	//protected String getHeader(OgemaHttpRequest req) {
	//	return getReqData(req).getParent().getParent().getName();
	//}
	
	//Note: Supertable is already HIDDEN
	
	@Override
	protected void addWidgets() {
		tablePage = new TablePageEPM(exPage, appManExt);
		if(creatingPage.providerInitDone && (!triggerForTablePageDone)) {
			triggerForTablePageDone = true;
			triggerPageBuild();
		}
	}

	@Override
	protected Class<? extends Resource> typeSelected(OgemaHttpRequest req) {
		return creatingPage.getPrimaryEntryTypeClass();
	}
}
