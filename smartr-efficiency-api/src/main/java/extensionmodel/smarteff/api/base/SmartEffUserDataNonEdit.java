package extensionmodel.smarteff.api.base;

import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;

public interface SmartEffUserDataNonEdit extends ExtensionUserDataNonEdit {
	@Override
	SmartEffUserData editableData();
}
