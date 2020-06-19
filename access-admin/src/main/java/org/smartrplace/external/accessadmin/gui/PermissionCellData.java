package org.smartrplace.external.accessadmin.gui;

public interface PermissionCellData {
	/** Current specific setting for the permission field. If this is null this is NOT the
	 * effective setting as in this case the {@link #getDefaultStatus()} will determine the
	 * effective status.
	 * @return
	 */
	Boolean getOwnstatus();
	
	/** Set own status.
	 * 
	 * @param newStatus if null own status shall be removed leaving the permission determination
	 * to the group memberships or default settings represented by {@link #getDefaultStatus()}.
	 */
	void setOwnStatus(Boolean newStatus);
	
	/** Status of the permission field based on superior group membership if no explicit setting for
	 * the user and room combination is made.
	 */
	boolean getDefaultStatus();
	
	default boolean getEffectiveStatus() {
		Boolean ownStatus = getOwnstatus();
		if(ownStatus != null)
			return ownStatus;
		return getDefaultStatus();
	}
}
