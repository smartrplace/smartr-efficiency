package org.smartrplace.commontypes;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericWithTable;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.WindowType;

public class WindowTypeRegistration {

	public static final Class<? extends SmartEffResource> TYPE_CLASS = WindowType.class;
	
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

			setLabel(type.name(), EN, "Name of window type", DE, "Name des Fenstertyps");
			setTableHeader(type.name(), EN, "Name");
			
			setLabel(type.count(), EN, "Number of windows of this type in building that are not modeled in rooms",
					DE, "Zahl der Fenster des Typs im Gebäude, die nicht in Räumen modelliert sind");
			setLabel(type.sensorInstallationShare(),
					EN, "Share of windows of the type that require a window sensor for optimized operation",
					DE, "Anteil Ziel Ausstattung Fensersensoren");
			
			setTableHeader(type.count(), EN, "# in building", DE, "# im Gebäude");
			
			setLabelWithUnit(type.uValue(), EN, "U-Value of windows of this type", DE, "U-Wert der Fenster des Typs");
			
			setLabelWithUnit(type.height(), EN, "Height of windows of this type", DE, "Höhe der Fenster des Typs");

			//setLabel(type.id(), EN, "Window Type", DE, "Art des Fensters");
			//setDisplayOptions(type.id(), EN, WINDOW_TYPEMAP_EN); // TODO add TYPEMAP_DE
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Class<WindowType> primaryEntryTypeClass() {
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
		
		@Override
		public boolean checkResource(WindowType res) {
			ValueResourceHelper.setIfNew(res.sensorInstallationShare(), 1.0f);
			return super.checkResource(res);
		}
	}
}