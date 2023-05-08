package org.smartrplace.apps.hw.install.gui.alarm;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.ogema.core.resourcemanager.ResourceAccess;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.emptywidget.EmptyData;
import de.iwes.widgets.html.emptywidget.EmptyWidget;

@SuppressWarnings("serial")
public class KnownDevicesWidget extends EmptyWidget {
	
	private final ResourceAccess resAcc;

	public KnownDevicesWidget(WidgetPage<?> page, String id, ResourceAccess resAcc) {
		super(page, id);
		this.resAcc = resAcc;
	}
	
	@Override
	public KnownDevicesData createNewSession() {
		return new KnownDevicesData(this);
	}
	
	@Override
	public KnownDevicesData getData(OgemaHttpRequest req) {
		return (KnownDevicesData) super.getData(req);
	}
	
	@Override
	public void onGET(OgemaHttpRequest req) {
		final List<InstallAppDevice> devices = resAcc.getResources(HardwareInstallConfig.class).stream()
				.flatMap(cfg -> cfg.knownDevices().getAllElements().stream())
				.collect(Collectors.toUnmodifiableList());
		getData(req).knownDevices = devices;
	}
	
	public List<InstallAppDevice> getKnownDevices(OgemaHttpRequest req) {
		return getData(req).knownDevices; 
	}

	public static class KnownDevicesData extends EmptyData {
	
		List<InstallAppDevice> knownDevices = Collections.emptyList();
		
		public KnownDevicesData(KnownDevicesWidget empty) {
			super(empty);
		}
		
		
	}
	
	
}
