package org.smartrplace.smarteff.admin.protect;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.smartrplace.extensionservice.ExtensionUserData;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForCreate;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForPageOpening;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;

public class ExtensionResourceAccessInitDataImpl implements ExtensionResourceAccessInitData {
	private final int entryTypeIdx;
	private final List<Resource> entryResources;
	private final List<GenericDataTypeDeclaration> entryData;
	
	private final ConfigInfo configInfo;
	private final ExtensionUserData userData;
	private final ExtensionUserDataNonEdit userDataNonEdit;
	private final ExtensionPageSystemAccessForPageOpening systemAccess;
	
	public ExtensionResourceAccessInitDataImpl(int entryTypeIdx,
			List<Resource> entryResources, List<GenericDataTypeDeclaration> entryData,
			ConfigInfo configInfo,
			ExtensionUserData userData, ExtensionUserDataNonEdit userDataNonEdit,
			ExtensionPageSystemAccessForPageOpening systemAccess) {
		this.entryTypeIdx = entryTypeIdx;
		if(entryResources == null) {
			this.entryResources = entryResources;
			this.entryData = entryData;			
		} else {
			this.entryResources = entryResources;
			this.entryData = entryData;			
		}
		this.userData = userData;
		this.userDataNonEdit = userDataNonEdit;
		this.systemAccess = systemAccess;
		this.configInfo = configInfo;
	}

	@Override
	public int entryTypeIdx() {
		return entryTypeIdx;
	}

	@Override
	public List<Resource> entryResources() {
		return entryResources;
	}
	@Override
	public List<GenericDataTypeDeclaration> entryData() {
		return entryData;
	}

	@Override
	public ExtensionUserData userData() {
		return userData;
	}

	@Override
	public ExtensionUserDataNonEdit userDataNonEdit() {
		return userDataNonEdit;
	}

	@Override
	public ExtensionPageSystemAccessForCreate systemAccess() {
		if(!(systemAccess instanceof ExtensionPageSystemAccessForCreate))
			throw new IllegalStateException("Page without valid configID only supports systeAccessForPageOpening!");
		return (ExtensionPageSystemAccessForCreate) systemAccess;
	}
	@Override
	public ExtensionPageSystemAccessForPageOpening systemAccessForPageOpening() {
		return systemAccess;
	}

	@Override
	public ConfigInfo getConfigInfo() {
		return configInfo;
	}
}
