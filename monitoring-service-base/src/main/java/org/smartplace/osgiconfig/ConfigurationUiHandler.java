package org.smartplace.osgiconfig;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ogema.devicefinder.api.DriverHandlerProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlapp
 */
@Component(configurationPid = ConfigurationUiHandler.PID)
@Designate(ocd = ConfigurationUiHandler.Config.class)
public class ConfigurationUiHandler {

    public static final String PID = "org.smartrplace.widgets.ConfigAdminUi";

    @ObjectClassDefinition(localization = "OSGI-INF/l10n/uitext", name = "OSGi ConfigAdmin UI")
    public static @interface Config {

        @AttributeDefinition(description = "%uicfg_bsnlist")
        String[] bsnList() default {"org.smartrplace.internal.smart-school", "org.smartrplace.apps.monitoring-service-base"};

        @AttributeDefinition(description = "%uicfg_bsnlistdeny")
        boolean bsnListIsDeny() default false;

        //String[] ocdList() default {};

        //boolean ocdListIsDeny() default true;
    }

    @Reference
    ConfigurationAdmin ca;

    @Reference
    MetaTypeService mts;

    Map<Bundle, Collection<ServiceRegistration<DriverHandlerProvider>>> bundleConfigUis = new ConcurrentHashMap<>();
    Config cfg;
    BundleContext ctx;
    BundleListener l = this::bundleChanged;
    
    Logger logger = LoggerFactory.getLogger(getClass());

    @Activate
    void activate(BundleContext ctx, Config cfg) {
        this.ctx = ctx;
        this.cfg = cfg;
        Stream.of(ctx.getBundles()).forEach(this::addBundle);
        ctx.addBundleListener(l);
    }

    @Deactivate
    void deactivate() {
        ctx.removeBundleListener(l);
        bundleConfigUis.values().stream()
                .flatMap(Collection::stream)
                .forEach(ServiceRegistration::unregister);
    }

    synchronized void bundleChanged(BundleEvent event) {
        Bundle b = event.getBundle();
        if (b.getState() == Bundle.ACTIVE) {
            addBundle(b);
        } else {
            unregisterBundle(b);
        }
    }

    void unregisterBundle(Bundle b) {
        Collection<ServiceRegistration<DriverHandlerProvider>> sr
                = bundleConfigUis.remove(b);
        if (sr != null) {
            sr.forEach(r -> {
                try {
                    r.unregister();
                } catch (IllegalStateException ise) {
                    //nevermind
                }
            });
        }
    }

    void addBundle(Bundle b) {
        if (b.getState() != Bundle.ACTIVE || b.getBundleContext() == null) {
            return;
        }
        if (!acceptBundle(b)) {
            return;
        }
        MetaTypeInformation bundleMetaTypeInfo = mts.getMetaTypeInformation(b);
        if (bundleMetaTypeInfo == null) {
            return;
        }
        Stream.of(bundleMetaTypeInfo.getPids()).filter(this::acceptPid)
                .forEach(s -> {
                    logger.debug("adding configuration UI for PID {} from {}", s, b.getSymbolicName());
                    bundleConfigUis.put(b,
                            addConfigurationFrontend(b.getBundleContext(), b.getSymbolicName(), s, false));
                });
        Stream.of(bundleMetaTypeInfo.getFactoryPids()).filter(this::acceptPid)
                .forEach(s -> {
                    logger.debug("adding configuration UI for PID {} from {}", s, b.getSymbolicName());
                    bundleConfigUis.put(b,
                            addConfigurationFrontend(b.getBundleContext(), b.getSymbolicName(), s, true));
                });
    }

    boolean acceptBundle(Bundle b) {
        Optional<String> inList = Stream.of(cfg.bsnList()).filter(s -> b.getSymbolicName().equals(s)).findAny();
        if (cfg.bsnListIsDeny() && inList.isPresent()) {
            logger.debug("blocked: {} is in BSN deny list.", b.getSymbolicName());
            return false;
        }
        if (!cfg.bsnListIsDeny() && !inList.isPresent()) {
            logger.debug("blocked: {} is not in allowed BSN list.", b.getSymbolicName());
            return false;
        }
        return true;
    }

    boolean acceptPid(String pid) {
        return true;
    }

    Collection<ServiceRegistration<DriverHandlerProvider>> addConfigurationFrontend(
            BundleContext ctx, String bundleSymbolicName,
            String ocdPid, boolean isFactoryPid
    ) {
        return Stream.of(ctx.getBundles())
                .filter(b -> b.getSymbolicName().equals(bundleSymbolicName))
                .map(b -> {
                    ConfigurationHandlerProvider dp = new ConfigurationHandlerProvider(ca, mts, b.getBundleContext(), ocdPid, isFactoryPid);
                    return ctx.registerService(DriverHandlerProvider.class, dp, null);
                }).collect(Collectors.toList());
    }

}
