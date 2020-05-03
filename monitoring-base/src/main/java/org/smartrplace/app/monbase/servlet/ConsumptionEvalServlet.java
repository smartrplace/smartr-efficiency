package org.smartrplace.app.monbase.servlet;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.ConsumptionInfo.UtilityType;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;


public class ConsumptionEvalServlet implements ServletPageProvider<UtilityType> {
	protected final ApplicationManager appMan;
	
	public ConsumptionEvalServlet(ApplicationManager appMan) {
		this.appMan = appMan;
	}

	@Override
	public Map<String, ServletValueProvider> getProviders(UtilityType object, String user,
			Map<String, String[]> parameters) {
		Map<String, ServletValueProvider> result = new HashMap<>();
		return result;
	}

	@Override
	public Collection<UtilityType> getAllObjects(String user) {
		return Arrays.asList(UtilityType.values());
	}

	@Override
	public UtilityType getObject(String objectId) {
		return UtilityType.valueOf(objectId);
	}
}
