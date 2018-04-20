package org.smartrplace.smarteff.admin.config;

import org.ogema.model.prototypes.Data;

import extensionmodel.smarteff.api.base.SmartEffGeneralData;

/** Data that is accessible for all users.*/
public interface SmartEffAdminData extends Data {
	/**Global data visible to apps*/
	SmartEffGeneralData globalData();
}
