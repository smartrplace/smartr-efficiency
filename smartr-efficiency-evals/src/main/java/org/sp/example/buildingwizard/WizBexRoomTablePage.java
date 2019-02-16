package org.sp.example.buildingwizard;

import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.resourcecreate.ProviderPublicDataForCreate.PagePriority;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericWithTable;
import org.smartrplace.smarteff.util.editgeneric.GenericResourceByTypeTablePage;
import org.smartrplace.smarteff.util.wizard.AddEditButtonWizardList;
import org.smartrplace.smarteff.util.wizard.AddEditButtonWizardList.BACKTYPE;
import org.smartrplace.smarteff.util.wizard.WizBexWidgetProvider;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;
import org.sp.example.smarteff.eval.capability.SPEvalDataService;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;
import extensionmodel.smarteff.api.common.BuildingUnit;

public class WizBexRoomTablePage extends GenericResourceByTypeTablePage<BuildingUnit> {
	
	public WizBexRoomTablePage(EditPageGenericWithTable<BuildingUnit> creatingPage, String id) {
		super(creatingPage, id);
	}
	
	class TablePageWizBexRoom extends TablePageEPM {
		public TablePageWizBexRoom(
				ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
				ApplicationManagerMinimal appManMin) {
			super(exPage, appManMin);
		}
		
		@Override
		protected String getHeaderEntry(Resource object, String sub) {
			switch(sub) {
			case WizBexWidgetProvider.ROOM_ENTRY_LABEL_ID:
				return "Entry";
			default:
				return null;
			}
		}
		
		@Override
		protected boolean processWidgetByInheritedPage(BuildingUnit object, String sub,
				Map<OgemaLocale, String> labels, String id, OgemaHttpRequest req, Row row) {
			if(sub.equals(WizBexWidgetProvider.ROOM_ENTRY_LABEL_ID)) {
				AddEditButtonWizardList<BuildingUnit> openButton = new AddEditButtonWizardList<BuildingUnit>(
						mainTable, id, pid(), exPage, null, BACKTYPE.START, appManExt, req) {
					private static final long serialVersionUID = 1L;
	
					@Override
					protected Resource getEntryResource(OgemaHttpRequest req) {
						return object.getParent().getParent();
					}
	
					@Override
					protected Resource getElementResource(OgemaHttpRequest req) {
						return object;
					}
					
					@Override
					protected Class<BuildingUnit> getType() {
						return BuildingUnit.class;
					}
					
					@Override
					protected Map<OgemaLocale, String> getButtonTexts(OgemaHttpRequest req) {
						return WizBexWidgetProvider.ROOMENTRYBUTTON_TEXTS;
					}

					@Override
					protected String getDestinationURL(ExtensionResourceAccessInitData appData, Resource object,
							OgemaHttpRequest req) {
						return SPPageUtil.getProviderURL(SPEvalDataService.WIZBEX_ROOM.provider);
					}
				};
				row.addCell(getHeaderEntry(object, sub), openButton);
				return true;
			}
			return false;
		}
		
		@Override
		protected void finishRow(Resource object, ResourceGUIHelper<Resource> vh, String id, OgemaHttpRequest req,
				Row row, ExtensionResourceAccessInitData appData) {
			//do nothing
		}
	}
		
	@Override
	protected GenericResourceByTypeTablePage<BuildingUnit>.TablePageEPM createTablePage(
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ApplicationManagerMinimal appManMin) {
		return new TablePageWizBexRoom(exPage, appManExt);
	}
	
	@Override
	protected PagePriority getPriorityImpl() {
		return PagePriority.HIDDEN;
	}
}
