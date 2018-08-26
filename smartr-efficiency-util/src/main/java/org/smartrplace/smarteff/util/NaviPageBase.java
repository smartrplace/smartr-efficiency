package org.smartrplace.smarteff.util;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.resourcecreate.ProviderPublicDataForCreate.PagePriority;
import org.smartrplace.smarteff.util.button.ResourceOfTypeTableOpenButton;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.label.Header;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public abstract class NaviPageBase<T extends Resource>  {
	/** For Edit-pages this is the resource type that can be edited by the class. For
	 * table pages this is the super type below which resources are shown. As the method
	 * {@link #getEntryTypes()} supports more than one type more resource types may be added for a
	 * table page. In most cases only a single type is sufficient, though.<br>
	 * If the table shall only contain resources of a certain type and the type shall be declared
	 * use {@link #typesListedInTable()}. See also {@link ResourceOfTypeTableOpenButton}.typeToOpen()
	 * regarding this. 
	 */
	protected abstract Class<T> primaryEntryTypeClass();
	protected abstract String label(OgemaLocale locale);
	protected abstract void addWidgets();
	protected abstract List<EntryType> getEntryTypes();
	protected List<GenericDataTypeDeclaration> getElementTypes() {
		return null;
	}

	protected abstract PageType getPageType();
	//Overwrite if necessary
	protected String getHeader(OgemaHttpRequest req) {
		return getReqData(req).getLocation();
	}
	protected String id() {
		return this.getClass().getName();
	}
	protected String pid() {
		return primaryEntryTypeClass().getSimpleName();
	}
	protected PagePriority getPriority() {
		return PagePriority.STANDARD;
	}
	protected String getMaintainer() { return null;}
	protected List<GenericDataTypeDeclaration> typesListedInTable() {return null;}

	protected EditPage editOrTablePage;
	public final Provider provider;	

	protected ApplicationManagerSPExt appManExt;
	protected ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
	protected WidgetPage<?> page;
	
	public boolean providerInitDone = false;

	public NaviPageBase() {
		this.provider = new Provider();
	}

	public class EditPage {

		public EditPage(ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage, ApplicationManagerSPExt appManExt) {
			NaviPageBase.this.exPage = exPage;
			//this.appManExt = appManExt;
			//BuildingData bd = null; bd.heatedLivingSpace()
			NaviPageBase.this.page = exPage.getPage();
			
			Header header = new Header(page, "header"+pid()) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onGET(OgemaHttpRequest req) {
					setText(getHeader(req), req);
				}
			};
			header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
			page.append(header);
			
			addWidgets();
			exPage.registerDependentWidgetOnInit(header);
			finishPage();
		}
	}
	
	protected void finishPage() {
		SPPageUtil.STANDARD_PAGE_FORMATTER.formatPage(page);
	}
	
	public class Provider implements NavigationGUIProvider {
		//private Resource generalData;
	
		@Override
		public String label(OgemaLocale locale) {
			return NaviPageBase.this.label(locale);
		}
		
		@Override
		public String id() {
			return NaviPageBase.this.id();
		}
	
		@Override
		public void initPage(ExtensionNavigationPageI<?, ?> pageIn, ApplicationManagerSPExt appManExt) {
			NaviPageBase.this.appManExt = appManExt;
			//this.generalData = generalData;
			@SuppressWarnings("unchecked")
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> page =
				(ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData>) pageIn;
			//Label test = new Label(page.page, "test", "Hello World!");
			//page.page.append(test);
			editOrTablePage = new EditPage(page, appManExt);
			providerInitDone = true;
		}
	
		@Override
		public List<EntryType> getEntryTypes() {
			return NaviPageBase.this.getEntryTypes();
		}
		
		@Override
		public PageType getPageType() {
			return NaviPageBase.this.getPageType();
		}
		
		@Override
		public PagePriority getPriority() {
			return NaviPageBase.this.getPriority();
		}
		
		@Override
		public String userName() {
			return NaviPageBase.this.getMaintainer();
		}
		
		@Override
		public List<GenericDataTypeDeclaration> typesListedInTable() {
			return NaviPageBase.this.typesListedInTable();
		}
	}
	
	@SuppressWarnings("unchecked")
	protected T getReqData(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		return (T) appData.entryResources().get(0);
	}
	
	protected String getUserName(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		return appData.userDataNonEdit().ogemaUserName().getValue();
	}
}
