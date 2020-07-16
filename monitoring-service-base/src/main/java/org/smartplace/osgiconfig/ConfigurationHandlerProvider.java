package org.smartplace.osgiconfig;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.accordion.Accordion;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.html.html5.SimpleGrid;
import de.iwes.widgets.html.html5.flexbox.FlexDirection;
import de.iwes.widgets.template.DefaultLabelledItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Stream;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DriverHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

/**
 *
 * @author jlapp
 */
public class ConfigurationHandlerProvider implements DriverHandlerProvider {

    final static String RESOURCEBUNDLE = "OSGI-INF/l10n/uitext";
    final ConfigurationAdmin ca;
    final MetaTypeService mts;
    Logger logger = LoggerFactory.getLogger(getClass());
    final String ocdPid;
    MetaTypeInformation mti;
    final boolean isFactoryPid;
    final BundleContext bundleContext;

    List<ServiceRegistration<DriverHandlerProvider>> services = new ArrayList<>();

    public ConfigurationHandlerProvider(ConfigurationAdmin ca, MetaTypeService mts, BundleContext bundle, String ocdPid, boolean isFactoryPid) {
        this.ca = ca;
        this.mts = mts;
        this.ocdPid = ocdPid;
        this.isFactoryPid = isFactoryPid;
        this.bundleContext = bundle;
        mti = mts.getMetaTypeInformation(bundleContext.getBundle());
        if (mti == null) {
            throw new IllegalArgumentException("Bundle "
                    + bundleContext.getBundle().getSymbolicName()
                    + " has no MetaTypeInformation");
        }
    }

    @Override
    public List<DriverDeviceConfig> getDeviceConfigs() {
        System.out.println("getDeviceConfigs");
        return Collections.emptyList();
    }

    @Override
    public List<DeviceHandlerProvider<?>> getDeviceHandlerProviders(boolean registerByFramework) {
        return Collections.emptyList();
    }
    
    @Override
    public DeviceTableRaw<DriverHandlerProvider, Resource> getDriverInitTable(WidgetPage<?> page, Alert alert) {
        DeviceTableRaw<DriverHandlerProvider, Resource> dtr = new DeviceTableRaw<DriverHandlerProvider, Resource>(page, null, alert, null) {
            
        	/** Note that the table id must be unique, but fixed for a certain instance of DriverHandlerProvider*/
            @Override
            protected String id() {
                return pid();
            }
            
            @Override
            protected String pid() {
            	return ocdPid;
            }

            @Override
            protected String getTableTitle() { //XXX why no locale???
                ObjectClassDefinition ocd = mti.getObjectClassDefinition(ocdPid, null);
                return ocd.getName();
            }

            @Override
            public Collection<DriverHandlerProvider> getObjectsInTable(OgemaHttpRequest req) {
                return Collections.singletonList(ConfigurationHandlerProvider.this);
            }

            @Override
            public void addWidgets(
                    DriverHandlerProvider object,
                    ObjectResourceGUIHelper<DriverHandlerProvider, Resource> vh,
                    String id, OgemaHttpRequest req, RowTemplate.Row row, ApplicationManager appMan) {
                if (req == null) { //why?
                    return;
                }
                ResourceBundle rb = ResourceBundle.getBundle(RESOURCEBUNDLE,
                        req.getLocale().getLocale(),
                        ConfigurationHandlerProvider.class.getClassLoader());
                try {
                    ObjectClassDefinition ocd = mti.getObjectClassDefinition(ocdPid, req.getLocaleString());
                    String filter = isFactoryPid
                            ? String.format("(service.factoryPid=%s)", ocdPid)
                            : String.format("(service.pid=%s)", ocdPid);
                    Configuration[] configs = ca.listConfigurations(filter);
                    Flexbox flex = new Flexbox(mainTable, "_" + UUID.randomUUID().toString(), req);
                    flex.setFlexDirection(FlexDirection.COLUMN, req);
                    flex.addItem(new Label(mainTable, "_" + UUID.randomUUID().toString(), ocd.getDescription(), req), req);
                    if (configs == null) {
                        //System.out.println("no configurations");
                    } else {
                        //System.out.printf("have %d configurations%n", configs.length);
                        Stream.of(configs).forEach(cfg -> flex.addItem(createConfigWidget(cfg, ocd, page, req, rb), req));
                    }
                    flex.addItem(createNewConfigWidget(ocd, page, req, rb), req);
                    row.addCell(flex);
                } catch (IOException | InvalidSyntaxException ex) {
                    logger.warn("could not load config", ex);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    //XXX the using page does not clean up deregistered widgets!
                    System.out.println(e);
                    //e.printStackTrace();
                }
            }

        };
        return dtr;
    }
    
    OgemaWidget createNewConfigWidget(ObjectClassDefinition ocd, WidgetPage<?> page, OgemaHttpRequest req, ResourceBundle rb) {
        SimpleGrid grid = new SimpleGrid(page, "_" + UUID.randomUUID().toString(), false);
        AttributeDefinition[] attrDef = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
        final Map<String, Object> props = new HashMap<>();
        int rowNum = 0;
        for (; rowNum < attrDef.length; rowNum++) {
            AttributeDefinition ad = attrDef[rowNum];
            final String attrId = ad.getID();
            String label = String.format("%s (%s)", ad.getName(), ad.getID());
            grid.addItem(new Label(page, "_" + UUID.randomUUID().toString(), label), true, req);
            String[] defVal = ad.getDefaultValue();
            String def = ad.getDefaultValue() == null
                    ? ""
                    : Arrays.toString(defVal);
            if (defVal != null && defVal.length == 1 && defVal[0] != null && ad.getCardinality() == 0) {
                def = defVal[0];
            }
            TextField tf = new TextField(page, "_" + UUID.randomUUID().toString(), def) {
                static final long serialVersionUID = 1;
                
                @Override
                public void onPOSTComplete(String data, OgemaHttpRequest req) {
                    String val = getValue(req);
                    if (val == null) {
                    } else {
                        props.put(attrId, val);
                        System.out.println(props);
                    }
                }
            };
            grid.addItem(rowNum, 1, tf, req);
            grid.addItem(rowNum, 1, new Label(page, "_" + UUID.randomUUID().toString(), ad.getDescription()), req);
        }
        String[] cfgName = {""};
        TextField tfConfigName = new TextField(page, "_" + UUID.randomUUID().toString()) {
            static final long serialVersionUID = 1;
            
            @Override
            public void onPOSTComplete(String data, OgemaHttpRequest req) {
                String val = getValue(req);
                if (val == null) {
                } else {
                    cfgName[0] = val;
                    //TODO: validate name
                    System.out.println(cfgName[0]);
                }
            }
        };
        Button btCreate = new Button(page, "_" + UUID.randomUUID().toString(), rb.getString("new_config_button")) {
            static final long serialVersionUID = 1;

            @Override
            public void onPOSTComplete(String data, OgemaHttpRequest req) {
                try {
                    Configuration conf = isFactoryPid
                            ? ca.getFactoryConfiguration(ocdPid, cfgName[0])
                            : ca.getConfiguration(ocdPid);
                    Dictionary<String, Object> cfgProps = conf.getProperties();
                    if (cfgProps == null) {
                        cfgProps = new Hashtable<>();
                    }
                    //TODO: type checks & conversion
                    for (Entry<String, Object> e : props.entrySet()) {
                        cfgProps.put(e.getKey(), e.getValue());
                    }
                    conf.updateIfDifferent(cfgProps);
                } catch (IOException ex) {
                    //TODO
                    System.out.println(ex);
                }
            }
        };
        Label lbNewName = new Label(page, "_" + UUID.randomUUID().toString(), rb.getString("new_config_name"));
        Accordion acc = new Accordion(page, "_" + UUID.randomUUID().toString());
        if (isFactoryPid) {
            grid.addItem(lbNewName, true, req);
            grid.addItem(rowNum, 1, tfConfigName, req);
            grid.addItem(rowNum++, 1, btCreate, req);
        } else {
            grid.addItem(btCreate, true, req);
        }
        acc.addItem(new DefaultLabelledItem(
                "_" + UUID.randomUUID().toString(), rb.getString("new_config_label"),
                rb.getString("new_config_desc")), grid, false, req);
        return acc;
    }

    OgemaWidget createConfigWidget(Configuration cfg, ObjectClassDefinition ocd,
            WidgetPage<?> page, OgemaHttpRequest req, ResourceBundle rb) {
        Accordion acc = new Accordion(page, "_" + UUID.randomUUID().toString());
        SimpleGrid grid = new SimpleGrid(page, "_" + UUID.randomUUID().toString(), false);
        AttributeDefinition[] attrDef = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
        int rowNum = 0;
        final Map<String, Object> props = new HashMap<>();
        for (; rowNum < attrDef.length; rowNum++) {
            AttributeDefinition ad = attrDef[rowNum];
            grid.addItem(new Label(page, "_" + UUID.randomUUID().toString(), ad.getName()), true, req);
            Object value = cfg.getProperties().get(ad.getID());
            TextField tf = new TextField(page, "_" + UUID.randomUUID().toString(), String.valueOf(value)) {
                static final long serialVersionUID = 1;
                
                @Override
                public void onPOSTComplete(String data, OgemaHttpRequest req) {
                    String val = getValue(req);
                    if (val == null) {
                    } else {
                        props.put(ad.getID(), val);
                        System.out.println(props);
                    }
                }
            };
            grid.addItem(rowNum, 1, tf, req);
            grid.addItem(rowNum, 1, new Label(page, "_" + UUID.randomUUID().toString(), ad.getDescription()), req);
        }
        // show configuration properties that are not defined in MetaTypeService
        Enumeration<String> cfgKeys = cfg.getProperties().keys();
        while (cfgKeys.hasMoreElements()) {
            String key = cfgKeys.nextElement();
            if ("service.pid".equals(key) || "service.factoryPid".equals(key)) {
                continue;
            }
            if (!Stream.of(attrDef).filter(ad -> ad.getID().equals(key)).findAny().isPresent()) {
                grid.addItem(new Label(page, "_" + UUID.randomUUID().toString(), key), true, req);
                TextField tf = new TextField(page, "_" + UUID.randomUUID().toString(), String.valueOf(cfg.getProperties().get(key))) {
                    static final long serialVersionUID = 1;
                    
                    @Override
                    public void onPOSTComplete(String data, OgemaHttpRequest req) {
                        String val = getValue(req);
                        if (val == null) {
                        } else {
                            props.put(key, val);
                            System.out.println(props);
                        }
                    }
                };
                grid.addItem(rowNum, 1, tf, req);
                grid.addItem(rowNum++, 1, new Label(page, "_" + UUID.randomUUID().toString(), "(additional configuration property)"), req); //TODO: L10N
            }
        }

        String name = cfg.getPid();
        if (name.contains("~")) {
            name = name.substring(name.indexOf("~") + 1);
        }
        acc.addItem(new DefaultLabelledItem(
                "_" + UUID.randomUUID().toString(),
                name, "existing configuration " + cfg.getPid()), grid, false, req);//TODO: L10N
        //TODO: form functionality: save, delete
        Button btUpdate = new Button(page, "_" + UUID.randomUUID().toString(), rb.getString("update_config_button")) {
            static final long serialVersionUID = 1;
            
            @Override
            public void onPOSTComplete(String data, OgemaHttpRequest req) {
                try {
                    Dictionary<String, Object> cfgProps = cfg.getProperties();
                    if (cfgProps == null) {
                        cfgProps = new Hashtable<>();
                    }
                    //TODO: type checks & conversion
                    for (Entry<String, Object> e : props.entrySet()) {
                        cfgProps.put(e.getKey(), e.getValue());
                    }
                    cfg.updateIfDifferent(cfgProps);
                } catch (IOException ex) {
                    //TODO
                    System.out.println(ex);
                }
            }
        };
        Button btDelete = new Button(page, "_" + UUID.randomUUID().toString(), rb.getString("delete_config_button")) {
            static final long serialVersionUID = 1;
            
            @Override
            public void onPOSTComplete(String data, OgemaHttpRequest req) {
                try {
                    cfg.delete();
                } catch (IOException ex) {
                    //TODO
                    System.out.println(ex);
                }
            }
        };
        grid.addItem(btUpdate, true, req);
        grid.addItem(rowNum++, 1, btDelete, req);
        return acc;
    }

    @Override
    public DeviceTableRaw<DriverDeviceConfig, InstallAppDevice>
            getDriverPerDeviceConfigurationTable(
                    WidgetPage<?> page,
                    Alert alert,
                    InstalledAppsSelector selector,
                    boolean addUnfoundDevices) {
        return null;
    	/*System.out.println("getDriverPerDeviceConfigurationTable");
        DeviceTableRaw<DriverDeviceConfig, InstallAppDevice> dtr = new DeviceTableRaw<DriverDeviceConfig, InstallAppDevice>(page, null, alert, null) {
            @Override
            protected String id() {
                return pid(); //"_" + UUID.randomUUID().toString();
            }

            @Override
            protected String pid() {
            	return ocdPid+"X";
            }

            @Override
            protected String getTableTitle() {
                return "Superflous Table (cannot return null)";
            }

            @Override
            public Collection<DriverDeviceConfig> getObjectsInTable(OgemaHttpRequest req) {
                return Collections.emptyList();
            }

            @Override
            public void addWidgets(DriverDeviceConfig object, ObjectResourceGUIHelper<DriverDeviceConfig, InstallAppDevice> vh, String id, OgemaHttpRequest req, RowTemplate.Row row, ApplicationManager appMan) {
                System.out.println("getDriverPerDeviceConfigurationTable().addWidgets");
            }

        };
        return dtr;*/
    }

    @Override
    public String id() {
       	/** Note that the table id must be unique, but fixed for a certain instance of DriverHandlerProvider*/
        return ocdPid.replace('.', '_');
    }

    @Override
    public String label(OgemaLocale ol) {
        return mti.getObjectClassDefinition(ocdPid, ol.getLanguage()).getName();
    }

    @Override
    public String getDriverDocumentationPageURL(boolean publicVersion) {
        return "";
    }

}
