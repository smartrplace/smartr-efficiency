package org.smartrplace.smarteff.util.editgeneric;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.defaultservice.ResourceTablePage;
import org.smartrplace.smarteff.util.CapabilityHelper;
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

public class GenericResourceByTypeTablePage<T extends Resource> extends GenericResourceByTypeTablePageBase {
	private final EditPageGenericWithTable<T> creatingPage;
	private final String id;
	
	protected TablePageEPM createTablePage(ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
				ApplicationManagerMinimal appManMin) {
		return new TablePageEPM(exPage, appManExt);
	}
	
	public GenericResourceByTypeTablePage(EditPageGenericWithTable<T> creatingPage, String id) {
		super();
		this.creatingPage = creatingPage;
		this.id = id;
	}
	
	@Override
	public String id() {
		return id;
	}

	public class TablePageEPM extends ResourceTablePage.TablePage {
		/** Note that the underlying class {@link ResourceTablePage} is not typed and always uses
		 * Resource.class as type. For this reason we also get just objects of type Resource here. Especially
		 * during init we really have an object just of type Resource.
		 * TODO: Maybe we can cast the obejct for processWidgetByInheritedPage, but this has to be checked.
		 */
		protected String getHeaderEntry(Resource object, String sub) {return null;}
		protected boolean processWidgetByInheritedPage(T object, String sub,
				Map<OgemaLocale,String> labels, String id, OgemaHttpRequest req, Row row) {
			return false;
		}
		
		public TablePageEPM(
				ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
				ApplicationManagerMinimal appManMin) {
			super(exPage, appManMin);
		}

		protected void finishRow(Resource object, ResourceGUIHelper<Resource> vh, String id,
				OgemaHttpRequest req, Row row, ExtensionResourceAccessInitData appData) {
			if(req != null) {
				if(!(appData.getConfigInfo().context instanceof ResourceOfTypeContext)) throw new IllegalStateException("Type must be transmitted as ResourceOfTypeContext!");
				ResourceOfTypeContext param = (ResourceOfTypeContext)appData.getConfigInfo().context;
				SPPageUtil.addResEditOpenButton("Edit", object, vh, id, row, appData, tabButton.control, req,
						param.editPageURL);
			} else {
				vh.registerHeaderEntry("Edit");
			}
			GUIHelperExtension.addDeleteButton(null, object, mainTable, id, alert, row, vh, req);			
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void addWidgets(Resource object, ResourceGUIHelper<Resource> vh, String id,
				OgemaHttpRequest req, Row row, ApplicationManager appMan) {
			ExtensionResourceAccessInitData appData = null;
			if(req != null) {
				appData = exPage.getAccessData(req); //creatingPage.getExPage().getAccessData(req);
			}
			Map<String, Map<OgemaLocale, String>> locMap = creatingPage.tableHeaders;
			for(Entry<String,Map<OgemaLocale,String>> entry: locMap.entrySet()) {
				String sub = entry.getKey();
				if(req == null) {
					String headEntry = getHeaderEntry(object, sub);
					if(headEntry != null) {
						vh.registerHeaderEntry(headEntry);
						continue;
					}
				} else if(processWidgetByInheritedPage((T) object, sub, entry.getValue(), id, req, row))
					continue;
				/*if(sub.equals(WizBexWidgetProvider.ROOM_ENTRY_LABEL_ID)) {
					if(req != null) {
						AddEditButtonWizardList<T> openButton = new AddEditButtonWizardList<T>(
								mainTable, sub, sub, exPage, null, registerDependentWidgets, req) {
							private static final long serialVersionUID = 1L;

							@Override
							protected Resource getEntryResource(OgemaHttpRequest req) {
								return object;
							}

							@Override
							protected Class<T> getType() {
								return (Class<T>) creatingPage.primaryEntryTypeClassPublic();
							}
							
							@Override
							protected String getDestinationURL(ExtensionResourceAccessInitData appData,
									Resource object, OgemaHttpRequest req) {
								return SPPageUtil.getProviderURL();
							}
						};
						row.addCell("Entry", openButton);
						//SPPageUtil.addResEditOpenButton("Entry", object, vh, id, row, appData, tabButton.control, req);
					} else {
						vh.registerHeaderEntry("Entry");
					}
					continue;
				}*/
				
				Resource cellObject = ResourceHelper.getSubResource(object, sub);
				//Resource cellObject = CapabilityHelper.getOrcreateResource(object,
				//		sub, appData.systemAccess(), appManExt, type);
				
				//TODO: Language-specific headers are not supported yet by GUIHelper
				String text = ValueFormat.getLocaleString(OgemaLocale.ENGLISH, locMap.get(sub));
				
				//TODO: Provide different way of setting this
				String ed = entry.getValue().get(OgemaLocale.CHINESE);
				boolean doEdit = (ed != null)&&(ed.contains("edit"));
				
				if(cellObject == null)
					vh.stringLabel(text, id, "n/a", row);
				else if(cellObject instanceof FloatResource)
					vh.floatLabel(text, id, (FloatResource)cellObject, row, "%.2f");
				else if(cellObject instanceof IntegerResource) {
					//if(doEdit)
					//	vh.integerEdit(sub, id, (IntegerResource)cellObject, row, null, Integer.MIN_VALUE, Integer.MAX_VALUE, null);
					//else
						vh.intLabel(sub, id, (IntegerResource)cellObject, row, 0);
				}
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
			finishRow(object, vh, id, req, row, appData);
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
	protected List<GenericDataTypeDeclaration> typesListedInTable() {
		List<GenericDataTypeDeclaration> result = new ArrayList<>();
		result.add(CapabilityHelper.getGenericDataTypeDeclaration(creatingPage.primaryEntryTypeClassPublic()));
		return result ;
	}
	
	//TODO
	@Override
	protected String getMaintainer() {
		return super.getMaintainer();
	}
	
	//Note: Supertable is already HIDDEN
	
	@Override
	protected void addWidgets() {
		tablePage = createTablePage(exPage, appManExt);
		if(creatingPage.providerInitDone && (!triggerForTablePageDone)) {
			triggerForTablePageDone = true;
			triggerPageBuild();
		}
	}

	@Override
	protected Class<? extends Resource> typeSelected(OgemaHttpRequest req) {
		return creatingPage.primaryEntryTypeClassPublic();
	}
	
	@Override
	protected Map<OgemaLocale, String> getSuperEditButtonTexts() {
		return creatingPage.getSuperEditButtonTexts();
	}
}
