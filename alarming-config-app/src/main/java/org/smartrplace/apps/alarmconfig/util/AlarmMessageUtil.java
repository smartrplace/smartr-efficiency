package org.smartrplace.apps.alarmconfig.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.label.Label;

public class AlarmMessageUtil {
	public static final boolean addAlarmDocLink = !Boolean.getBoolean("org.smartrplace.apps.alarmconfig.util.suppressAlarmDocLink");
	
	public static String getAlarmGuideLink(String alarmMessage) {
		for(AlarmType type: AlarmType.getKnownTypes()) {
			if(type.isMessageRelevant(alarmMessage))
				return type.getLink();
		}
		return null;
	}
	
	public static Label configureAlarmValueLabel(InstallAppDevice object, ApplicationManager appMan, Label valueField, OgemaHttpRequest req, Locale locale) {
		final ValueData valueData = getValueData(object, appMan, locale);
		valueField.setText(valueData.message, req);
		if (valueData.responsibleResource != null)
			valueField.setToolTip("Value resource: " + valueData.responsibleResource.getLocationResource(), req);
		return valueField;
	}
	
	public static SingleValueResource findResponsibleResource(InstallAppDevice knownDevice, ApplicationManager appMan, Locale locale) {
		final SingleValueResource mainValue = getMainSensorValue(knownDevice, appMan.getAppID().getBundle().getBundleContext());
		final VoltageResource batteryVoltage = DeviceHandlerBase.getBatteryVoltage(knownDevice.device());
		final IntegerResource rssiDevice = DeviceHandlerBase.getSubResourceOfSibblingOrDirectChildMaintenance(knownDevice.device(),
				"rssiDevice", IntegerResource.class);
		final FloatResource valveState = knownDevice.device() instanceof Thermostat ? 
				((Thermostat) knownDevice.device()).valve().getSubResource("eq3state") : null;
		final Map<SingleValueResource, String> priorityResources = new LinkedHashMap<>();
		// if we find an alarm for any of the below resources, its value or last update is shown in the value field;
		// otherwise we select an arbitrary alarm from the list
		priorityResources.put(batteryVoltage, locale == Locale.GERMAN ? "Batteriestand niedrig" : "low battery voltage");
		priorityResources.put(mainValue, locale == Locale.GERMAN ? "Sensorwert außerhalb des erlaubten Bereichs" : "sensor value range violation");
		priorityResources.put(rssiDevice, locale == Locale.GERMAN ? "RSSI niedrig" : "rssi value low");
		priorityResources.put(valveState, locale == Locale.GERMAN ? "Thermostatventil lässt sich nicht steuern" : "thermostat valve state problematic");
		SingleValueResource responsibleResource = null;
		AlarmStatus status = null;
		for (Map.Entry<SingleValueResource, String> prioEntry: priorityResources.entrySet()) {
			final SingleValueResource prio = prioEntry.getKey();
			final AlarmStatus status0 = findAlarmForSensorValue(prio, knownDevice, appMan);
			if (status0 == null || (!status0.valueViolation && !status0.contactViolation))
				continue;
			status = status0;
			responsibleResource = prio;
			break;
		}
		if (responsibleResource == null) {
			// find any alarm in alarm state
			final Optional<AlarmStatus> statusOpt = knownDevice.alarms().getAllElements().stream()
				.map(alarm -> statusForAlarm(alarm, appMan))
				.filter(alarm -> alarm.valueViolation || alarm.contactViolation)
				.findAny();
			if (statusOpt.isPresent()) {
				status = statusOpt.get();
				responsibleResource = status.config.sensorVal();
			}
		}
		if (responsibleResource == null)  {
			responsibleResource = mainValue;
		}
		return responsibleResource;
	}
	
	private static ValueData getValueData(InstallAppDevice knownDevice, ApplicationManager appMan, Locale locale) {
		final SingleValueResource mainValue = getMainSensorValue(knownDevice, appMan.getAppID().getBundle().getBundleContext());
		final VoltageResource batteryVoltage = DeviceHandlerBase.getBatteryVoltage(knownDevice.device());
		final IntegerResource rssiDevice = DeviceHandlerBase.getSubResourceOfSibblingOrDirectChildMaintenance(knownDevice.device(),
				"rssiDevice", IntegerResource.class);
		final FloatResource valveState = knownDevice.device() instanceof Thermostat ? 
				((Thermostat) knownDevice.device()).valve().getSubResource("eq3state") : null;
		final Map<SingleValueResource, String> priorityResources = new LinkedHashMap<>();
		// if we find an alarm for any of the below resources, its value or last update is shown in the value field;
		// otherwise we select an arbitrary alarm from the list
		priorityResources.put(batteryVoltage, locale == Locale.GERMAN ? "Batteriestand niedrig" : "low battery voltage");
		priorityResources.put(mainValue, locale == Locale.GERMAN ? "Sensorwert außerhalb des erlaubten Bereichs" : "sensor value range violation");
		priorityResources.put(rssiDevice, locale == Locale.GERMAN ? "RSSI niedrig" : "rssi value low");
		priorityResources.put(valveState, locale == Locale.GERMAN ? "Thermostatventil lässt sich nicht steuern" : "thermostat valve state problematic");
		String valueFieldText = "";
		SingleValueResource responsibleResource = null;
		AlarmStatus status = null;
		String explanation = null;
		for (Map.Entry<SingleValueResource, String> prioEntry: priorityResources.entrySet()) {
			final SingleValueResource prio = prioEntry.getKey();
			final AlarmStatus status0 = findAlarmForSensorValue(prio, knownDevice, appMan);
			if (status0 == null || (!status0.valueViolation && !status0.contactViolation))
				continue;
			status = status0;
			responsibleResource = prio;
			explanation = prioEntry.getValue();
			break;
		}
		if (responsibleResource == null) {
			// find any alarm in alarm state
			final Optional<AlarmStatus> statusOpt = knownDevice.alarms().getAllElements().stream()
				.map(alarm -> statusForAlarm(alarm, appMan))
				.filter(alarm -> alarm.valueViolation || alarm.contactViolation)
				.findAny();
			if (statusOpt.isPresent()) {
				status = statusOpt.get();
				responsibleResource = status.config.sensorVal();
			}
		}
		if (responsibleResource == null)  {
			responsibleResource = mainValue;
			explanation = locale == Locale.GERMAN ? "Kein Problem ekannt" : "no problem detected";
		}
		if (responsibleResource != null) {
			if (status == null || status.valueViolation) {
				valueFieldText = (locale == Locale.GERMAN ? "Wert: " : "Value: ") + (responsibleResource instanceof FloatResource ? ValueResourceUtils.getValue((FloatResource) responsibleResource, 1) 
									: ValueResourceUtils.getValue(responsibleResource));
				if (explanation != null)
					valueFieldText += " (" + explanation + ")";
			}
			if (status != null && status.contactViolation) {
				final boolean hasValue = status.valueViolation;
				if (hasValue)
					valueFieldText += " (";
				final long lastContact = responsibleResource.getLastUpdateTime();
				valueFieldText += (locale == Locale.GERMAN ? "Letzter Kontakt: " : "Last contact: ")+ DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ", Locale.ENGLISH).format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastContact), ZoneId.of("Z")));
				if (hasValue)
					valueFieldText += ")";
			}
		}
		return new ValueData(responsibleResource, valueFieldText);
	}
	
	private static AlarmStatus findAlarmForSensorValue(SingleValueResource prio, InstallAppDevice object, ApplicationManager appMan) {
		if (prio == null || !prio.isActive())
			return null;
		final Optional<AlarmConfiguration> alarmConfigOpt = object.alarms().getAllElements().stream()
				.filter(cfg -> prio.equalsLocation(cfg.sensorVal()))
				.findAny();
		if (!alarmConfigOpt.isPresent())
			return null;
		return statusForAlarm(alarmConfigOpt.get(), appMan);
	}
	
	private static AlarmStatus statusForAlarm(AlarmConfiguration cfg, ApplicationManager appMan) {
		final SingleValueResource prio = cfg.sensorVal();
		boolean valueViolation = false;
		boolean contactViolation = false;
		if (prio instanceof IntegerResource || prio instanceof FloatResource || prio instanceof TimeResource) {
			final float value = ValueResourceUtils.getFloatValue(prio);
			valueViolation = (cfg.lowerLimit().isActive() && value < cfg.lowerLimit().getValue()) ||
					(cfg.upperLimit().isActive() && value > cfg.upperLimit().getValue());
		}
		final FloatResource maxInterval = cfg.maxIntervalBetweenNewValues();
		if (maxInterval.isActive() && maxInterval.getValue() > 0) {
			final long interval = appMan.getFrameworkTime() - prio.getLastUpdateTime();
			contactViolation = interval > maxInterval.getValue() * 60 * 1000;
		}
		return new AlarmStatus(cfg, valueViolation, contactViolation);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static SingleValueResource getMainSensorValue(InstallAppDevice config, BundleContext ctx) {
		final ServiceReference<DatapointService> dpServiceRef = ctx.getServiceReference(DatapointService.class);
		if (dpServiceRef != null) {
			try {
				final DatapointService dpService = ctx.getService(dpServiceRef);
				final DeviceHandlerProviderDP provider = dpService.getDeviceHandlerProvider(config);
				final SingleValueResource res = provider.getMainSensorValue(config);
				if (res != null)
					return res;
			} catch (Exception ignore) {
			} finally {
				ctx.ungetService(dpServiceRef);
			}
		}
		final PhysicalElement device = config.device();
		final Collection<ServiceReference<DeviceHandlerProviderDP>> references;
		try {
			references = ctx.getServiceReferences(DeviceHandlerProviderDP.class, null);
		} catch (InvalidSyntaxException e) {
			throw new RuntimeException(e);
		}
		List<SingleValueResource> candidates = null;
		for (ServiceReference<DeviceHandlerProviderDP> ref: references) {
			try {
				final DeviceHandlerProviderDP service = ctx.getService(ref);
				try {
					if (!service.getResourceType().isAssignableFrom(device.getResourceType()))
						continue;
					final SingleValueResource res = service.getMainSensorValue(device, config);
					if (res != null) {
						final Resource alarmStatus = res.getSubResource("alarmStatus");
						if (alarmStatus != null && alarmStatus instanceof IntegerResource && ((IntegerResource) alarmStatus).getValue() > 0)
							return res;
						if (candidates == null)
							candidates = new ArrayList<>(4);
						candidates.add(res);
					}
				} finally {
					ctx.ungetService(ref);
				}
			} catch (Exception ignore) {}
			
		}
		if (candidates != null)
			return candidates.get(0);
		final String typeName = device.getResourceType().getSimpleName();
		switch (typeName) {
		case "GatewayDevice":  
			return device.getSubResource("systemRestart");  // ?
		}
		return null;
	}
	
	private static class AlarmStatus {
		
		AlarmStatus(AlarmConfiguration config, boolean valueViolation, boolean contactViolation) {
			this.config = config;
			this.valueViolation = valueViolation;
			this.contactViolation = contactViolation;
		}
		
		final AlarmConfiguration config;
		final boolean valueViolation;
		final boolean contactViolation;
		
	}
	
	private static class ValueData {
		
		public ValueData(SingleValueResource responsibleResource, String message) {
			this.responsibleResource = responsibleResource;
			this.message = message;
		}
		final SingleValueResource responsibleResource; // may be null
		final String message;
		
	}
	
}
