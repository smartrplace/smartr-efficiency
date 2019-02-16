package org.smartrplace.smarteff.util.editgeneric;

import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.ObjectResourceGUIHelperExtPublic;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric.TypeResult;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.label.Label;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public interface EditPageGenericTableWidgetProvider<T extends Resource> {
	public static class CapabilityDeclaration {
		/** See {@link TypeResult}*/
		public Class<? extends Resource> type;
		public String typeString;
		/** highest value is used*/
		public int priority;
		
		public CapabilityDeclaration(Class<? extends Resource> type) {
			this(type, 10);
		}
		public CapabilityDeclaration(String typeString) {
			this(typeString, 10);
		}
		public CapabilityDeclaration(Class<? extends Resource> type, int priority) {
			this.type = type;
			this.typeString = null;
			this.priority = priority;
		}
		public CapabilityDeclaration(String typeString, int priority) {
			this.type = null;
			this.typeString = typeString;
			this.priority = priority;
		}
	}
	
	List<CapabilityDeclaration> capabilities();
	
	/** See {@link DefaultWidgetProvider} as an example
	 * TODO: Document input => explain how widget generation is done for EditPageGeneric
	 * @param sub
	 * @param type2
	 * @param labelWidgetForValue
	 * @param mhLoc
	 * @param isEditable
	 * @param isEditableSpecific most likely not required at all, should not be used
	 * @param pid
	 * @return
	 */
	OgemaWidget createValueWidget(String sub, TypeResult type2,
			Label labelWidgetForValue, ObjectResourceGUIHelperExtPublic<T> mhLoc,
			boolean isEditable,
			boolean isEditableSpecific, String pid);
	
	void setGlobalData(ObjectResourceGUIHelperExtPublic<T> mh, Alert alert, Map<String, Float> lowerLimits, Map<String, Float> upperLimits,
			Map<String, Map<OgemaLocale, Map<String, String>>> displayOptions,
			ApplicationManagerSPExt appManExt,  ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			WidgetPage<?> page,
			Class<? extends Resource> pageResoureType,
			Map<String, Object> widgetContexts);
}
