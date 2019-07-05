package org.smartrplace.smarteff.accesscontrol;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.common.AccessControl;

public class AccessControlRegistration {
	public static final Class<? extends SmartEffResource> TYPE_CLASS = AccessControl.class;
	

	
	public static class TypeDeclaration implements ExtensionResourceTypeDeclaration<SmartEffResource> {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return TYPE_CLASS;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "Access Control Data";
		}

		@Override
		public Class<? extends SmartEffResource> parentType() {
			return SmartEffResource.class;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.SINGLE_VALUE_OPTIONAL;
		}
	}
	
	public static class EditPage extends EditPageGeneric<AccessControl> {
		@Override
		public void setData(AccessControl sr) {
			setHeaderLabel(EN, "Access Control", DE, "Zugriffsberechtigungen");
			//setHeaderLink(EN, "https://en.wikipedia.org/wiki/Master_data");
			setLabel(sr.modules(), EN, "Module Full Classnames",
					DE, "Module (Klassennamen)");
			setEditable(sr.modules(), false);
			setLabel(sr.users(), EN, "User names",
					DE, "Nutzernamen");
		}
		
		@Override
		public boolean checkResource(AccessControl res) {
			return super.checkResource(res);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Class<AccessControl> primaryEntryTypeClass() {
			return (Class<AccessControl>) TYPE_CLASS;
		}
		
		@Override
		protected String getMaintainer() {
			return "master";
		}
		
		@Override
		protected String getHeader(OgemaHttpRequest req) {
			return "Access Permissions for "+getReqData(req).getParent().getParent().getName();
		}
	}
}
