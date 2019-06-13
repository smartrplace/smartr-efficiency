package org.smartrplace.commontypes;

import java.util.HashMap;
import java.util.Map;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericWithTable;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.WindowType;

public class WindowTypeRegistration {

	public static final Class<? extends SmartEffResource> TYPE_CLASS = WindowType.class;
	
	public static final Map<String, String> WINDOW_TYPEMAP_EN = new HashMap<>();
	public static final Map<String, String> WINDOW_TYPEMAP_DE = new HashMap<>();
	static {
		WINDOW_TYPEMAP_EN.put(Integer.toString(WindowType.WINDOW_NORMAL), "Normal window");
		WINDOW_TYPEMAP_DE.put(Integer.toString(WindowType.WINDOW_NORMAL), "");
		WINDOW_TYPEMAP_EN.put(Integer.toString(WindowType.WINDOW_DOOR), "Door");
		WINDOW_TYPEMAP_DE.put(Integer.toString(WindowType.WINDOW_DOOR), "");
		WINDOW_TYPEMAP_EN.put(Integer.toString(WindowType.WINDOW_DEFECTIVE), "Old or defective window");
		WINDOW_TYPEMAP_DE.put(Integer.toString(WindowType.WINDOW_DEFECTIVE), "");
	}
	
	public static class TypeDeclaration implements ExtensionResourceTypeDeclaration<SmartEffResource> {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return TYPE_CLASS;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "Window type data";
		}

		@Override
		public Class<? extends SmartEffResource> parentType() {
			return BuildingData.class;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.MULTIPLE_OPTIONAL;
		}
	}
	
	public static class EditPage extends EditPageGenericWithTable<WindowType> {
		@Override
		public void setData(WindowType type) {
			setHeaderLabel(EN, "Window Type Data", DE, "Daten Fenstertypen im Gebäude");

			setLabel(type.count(), EN, "Number of windows of this type in building",
					DE, "Zahl der Fenster des Typs im Gebäude");
			setTableHeader(type.count(), EN, "# in building", DE, "# im Gebäude");
			
			setLabelWithUnit(type.height(), EN, "Height of windows of the type", DE, "Höhe der Fenster des Typs");

			setLabel(type.id(), EN, "Window Type", DE, "Art des Fensters");
			setDisplayOptions(type.id(), EN, WINDOW_TYPEMAP_EN); // TODO add TYPEMAP_DE
		}
		
		@SuppressWarnings("unchecked")
		@Override
		protected Class<WindowType> primaryEntryTypeClass() {
			return (Class<WindowType>) TYPE_CLASS;
		}
		
		@Override
		protected String getMaintainer() {
			return "test1";
		}
		
		@Override
		protected String getHeader(OgemaHttpRequest req) {
			return getReqData(req).getParent().getParent().getName();
		}
	}
}