package org.smartrplace.app.monbase.servlet;

import javax.servlet.Servlet;

import org.ogema.accesscontrol.RestAccess;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.smartrplace.app.monbase.MonitoringApp;
import org.smartrplace.util.frontend.servlet.UserServlet;
import org.smartrplace.util.frontend.servlet.UserServletTest;

@Component(
		service=Servlet.class,
		property= { 
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "="+MonitoringApp.urlPathServlet+"/userdatatest", // prefix to be set in ServletContextHelper
				//HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=" + UserServletTest.CONTEXT_FILTER
		}
)
public class UserServletTestMon extends UserServletTest {
	private static final long serialVersionUID = 1L;
	
	public static class UserServletHolder {
		public UserServlet userServlet;		
	}
	public static UserServletHolder userServletHolder = new UserServletHolder();
	public static RestAccess restAcc;
	
	@Override
	protected UserServlet getUserServlet() {
		return userServletHolder.userServlet;
	}

	@Override
	protected RestAccess getRESTAccess() {
		return restAcc;
	}

}
