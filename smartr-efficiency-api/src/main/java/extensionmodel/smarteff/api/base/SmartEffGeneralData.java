package extensionmodel.smarteff.api.base;

import org.smartrplace.extensionservice.ExtensionGeneralData;

/** Data that is accessible for all users. The data entries are default values that can
 * be override by user specific resources at the same location relative to this
 * model inside ExtensionUserData.*/
public interface SmartEffGeneralData extends SmartEffTopLevelData, ExtensionGeneralData {
}
