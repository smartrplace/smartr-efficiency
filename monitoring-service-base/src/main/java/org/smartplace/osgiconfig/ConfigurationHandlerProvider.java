package org.smartplace.osgiconfig;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.accordion.Accordion;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.checkbox.Checkbox2;
import de.iwes.widgets.html.form.checkbox.CheckboxEntry;
import de.iwes.widgets.html.form.checkbox.DefaultCheckboxEntry;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.form.textfield.TextFieldType;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.html.html5.SimpleGrid;
import de.iwes.widgets.html.html5.flexbox.FlexDirection;
import de.iwes.widgets.template.DefaultLabelledItem;
import java.io.IOException;
import java.lang.reflect.Array;
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
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DriverHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.tools.resource.util.ResourceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
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

    Flexbox flex;
    AtomicBoolean rebuild = new AtomicBoolean(true);

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
                rebuild.set(true);
                ResourceBundle rb = ResourceBundle.getBundle(RESOURCEBUNDLE,
                        req.getLocale().getLocale(),
                        ConfigurationHandlerProvider.class.getClassLoader());
                try {

                    flex = new Flexbox(mainTable, "_" + UUID.randomUUID().toString(), req) {
                        @Override
                        public void onGET(OgemaHttpRequest req) {
                            if (!rebuild.compareAndSet(true, false) && req.isPolling()) {
                                return;
                            }
                            ObjectClassDefinition ocd = mti.getObjectClassDefinition(ocdPid, req.getLocaleString());
                            String filter = isFactoryPid
                                    ? String.format("(service.factoryPid=%s)", ocdPid)
                                    : String.format("(service.pid=%s)", ocdPid);
                            try {
                                Configuration[] configs = ca.listConfigurations(filter);
                                addItem(new Label(mainTable, "_" + UUID.randomUUID().toString(), ocd.getDescription(), req), req);
                                if (configs == null) {
                                    //System.out.println("no configurations");
                                } else {
                                    //System.out.printf("have %d configurations%n", configs.length);
                                    Stream.of(configs).forEach(cfg -> addItem(createEditConfigWidget(cfg, ocd, page, req, rb), req));
                                }
                                if (isFactoryPid || configs == null || configs.length == 0) {
                                    addItem(createNewConfigWidget(ocd, page, mainTable, req, rb), req);
                                }
                            } catch (IOException | InvalidSyntaxException ex) {
                                logger.warn("could not load configurations for {}", ocdPid, ex);
                            }
                        }
                    };
                    flex.setFlexDirection(FlexDirection.COLUMN, req);
                    flex.onGET(req);
                    row.addCell(flex);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    //XXX the using page does not clean up deregistered widgets!
                    System.out.println(e);
                    //e.printStackTrace();
                }
            }

        };
        return dtr;
    }
    
    private void rebuild(OgemaHttpRequest req) {
        flex.clear(req);
        rebuild.set(true);
    }
    
    TextField createParsedTextField(WidgetPage<?> page, String id, AttributeDefinition ad, Map<String, Object> props, Function<String, Object> parseFun, Object value) {
        final String attrId = ad.getID();
        String[] defVal = ad.getDefaultValue();
        String def = ad.getDefaultValue() == null
                ? ""
                : Arrays.toString(defVal);
        if (defVal != null && defVal.length == 1 && defVal[0] != null && ad.getCardinality() == 0) {
            def = defVal[0];
        }
        if (value != null) {
            def = value.toString();
        }
        return new TextField(page, id, def) {
            static final long serialVersionUID = 1;

            @Override
            public void onPOSTComplete(String data, OgemaHttpRequest req) {
                String val = getValue(req);
                logger.debug("{}: {}", ad.getID(), val);
                Object o = parseFun.apply(getValue(req));
                //TODO: parse errors
                logger.debug("{} parsed: {}", ad.getID(), o);
                props.put(attrId, o);
                logger.trace("props: {}", props);
            }
        };
    }
    
    OgemaWidget createEditWidgetForAttribute(Optional<Configuration> cfg, AttributeDefinition ad, Map<String, Object> props, WidgetPage<?> page, OgemaHttpRequest req, ResourceBundle rb) {
        if (ad.getCardinality() == 0) {
            Object val = cfg.map(c -> getConfigValue(c, ad)).orElse(null);
            return createEditWidgetForSingleAttribute(cfg, ad, props, val, page, req, rb);
        } else {
            return createEditWidgetForListAttribute(cfg, ad, props, page, req, rb);
        }
    }
    
    Object getConfigValue(Configuration cfg, AttributeDefinition ad) {
        if (cfg == null) {
            return null;
        }
        return cfg.getProperties() != null
                ? cfg.getProperties().get(ad.getID())
                : null;
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    OgemaWidget createEditWidgetForListAttribute(Optional<Configuration> cfg, AttributeDefinition ad, Map<String, Object> props, WidgetPage<?> page, OgemaHttpRequest req, ResourceBundle rb) {
        List values = new ArrayList<>();
        Object val = cfg.map(c -> getConfigValue(c, ad)).orElse(null);
        if (val != null) {
            if (val.getClass().isArray()) {
                for (int i = 0; i < Array.getLength(val); i++) {
                    values.add(Array.get(val, i));
                }
            } else if (val instanceof List) {
                values.addAll((List) val);
            } else {
                logger.error("unsupported value for {}: {}", ad.getID(), val);
            }
        }
        if (values.isEmpty()) {
            String[] defaults = ad.getDefaultValue();
            if (defaults != null) {
                values.addAll(Arrays.asList(defaults));
            }
        }

        SimpleGrid listGrid = new SimpleGrid(page, "_" + UUID.randomUUID().toString(), false) {
            
            AtomicBoolean rebuild = new AtomicBoolean(true);
            
            void rebuild(OgemaHttpRequest req) {
                clear(req);
                rebuild.set(true);
            }
            
            @Override
            public void onGET(OgemaHttpRequest req) {
                if (req.isPolling() && !rebuild.get()) {
                    return;
                }
                if (!rebuild.compareAndSet(true, false)) {
                    return;
                }
                int row = 0;
                String gridId = getId();
                for (Object o : values) {
                    final int idx = row;
                    Function<String, Object> parse = s -> {
                        values.set(idx, s);
                        return values;
                    };
                    TextField tf = createParsedTextField(page, "_" + UUID.randomUUID(), ad, props, parse, o == null ? "" : o);
                    addItem(tf, true, req);

                    Button addRowButton = new Button(page, "_" + UUID.randomUUID(), "+") {
                        @Override
                        public void onPOSTComplete(String data, OgemaHttpRequest req) {
                            values.add(idx, null);
                            props.put(ad.getID(), values);
                            rebuild(req);
                        }
                    };
                    addRowButton.triggerOnPOST(this);
                    addRowButton.setDefaultToolTip(rb.getString("list_insert_tt"));
                    addItem(row, 1, addRowButton, req);
                    
                    Button removeRowButton = new Button(page, "_" + UUID.randomUUID(), "-") {
                        @Override
                        public void onPOSTComplete(String data, OgemaHttpRequest req) {
                            values.remove(idx);
                            props.put(ad.getID(), values);
                            rebuild(req);
                        }
                    };
                    removeRowButton.setDefaultToolTip(rb.getString("list_remove_tt"));
                    removeRowButton.triggerOnPOST(this);
                    addItem(row, 2, removeRowButton, req);

                    row++;
                }
                
                Button newItem = new Button(page, "_" + UUID.randomUUID(), rb.getString("list_add")) {
                    @Override
                    public void onPOSTComplete(String data, OgemaHttpRequest req) {
                        values.add(null);
                        //do not insert into config here, only after actual edit
                        //props.put(ad.getID(), values);
                        rebuild(req);
                    }
                };
                newItem.triggerOnPOST(this);
                addItem(newItem, true, req);
            }
        };
        listGrid.setColumnGap("1px", req);
        listGrid.onGET(req); //init
        
        return listGrid;
    }
    
    // editor widget for single value attribute    
    OgemaWidget createEditWidgetForSingleAttribute(Optional<Configuration> cfg, AttributeDefinition ad, Map<String, Object> props, final Object value, WidgetPage<?> page, OgemaHttpRequest req, ResourceBundle rb) {
        final String attrId = ad.getID();
        String[] defVal = ad.getDefaultValue();
        String def = ad.getDefaultValue() == null
                ? ""
                : Arrays.toString(defVal);
        if (defVal != null && defVal.length == 1 && defVal[0] != null && ad.getCardinality() == 0) {
            def = defVal[0];
        }
        if (ad.getType() == AttributeDefinition.BOOLEAN) {
            String wId = "_" + UUID.randomUUID();
            String cbId = wId + "_Item";
            Boolean v = value == null
                    ? Boolean.valueOf(def)
                    : (Boolean) value;
            final CheckboxEntry e = new DefaultCheckboxEntry(cbId, "", v);
            Checkbox2 cb = new Checkbox2(page, "_" + UUID.randomUUID().toString()) {
                @Override
                public void onPOSTComplete(String data, OgemaHttpRequest req) {
                    boolean val = isChecked(cbId, req);
                    logger.debug("{} isChecked: {}", ad.getID(), val);
                    props.put(attrId, val);
                }
            };
            cb.setDefaultToolTip(getAttributeTooltip(ad));
            cb.addDefaultEntry(e);
            return cb;
        }
        if (ad.getType() == AttributeDefinition.BYTE) {
            TextField tf = createParsedTextField(page, "_" + UUID.randomUUID(), ad, props, Byte::valueOf, value);
            tf.setDefaultType(TextFieldType.NUMBER);
            tf.setDefaultToolTip(getAttributeTooltip(ad));
            return tf;
        }
        if (ad.getType() == AttributeDefinition.INTEGER) {
            TextField tf = createParsedTextField(page, "_" + UUID.randomUUID(), ad, props, Integer::valueOf, value);
            tf.setDefaultType(TextFieldType.NUMBER);
            tf.setDefaultToolTip(getAttributeTooltip(ad));
            return tf;
        }
        if (ad.getType() == AttributeDefinition.SHORT) {
            TextField tf = createParsedTextField(page, "_" + UUID.randomUUID(), ad, props, Short::valueOf, value);
            tf.setDefaultType(TextFieldType.NUMBER);
            tf.setDefaultToolTip(getAttributeTooltip(ad));
            return tf;
        }
        if (ad.getType() == AttributeDefinition.LONG) {
            TextField tf = createParsedTextField(page, "_" + UUID.randomUUID(), ad, props, Long::valueOf, value);
            tf.setDefaultType(TextFieldType.NUMBER);
            tf.setDefaultToolTip(getAttributeTooltip(ad));
            return tf;
        }
        if (ad.getType() == AttributeDefinition.FLOAT) {
            TextField tf = createParsedTextField(page, "_" + UUID.randomUUID(), ad, props, Float::valueOf, value);//TODO: locale dependent parsing
            tf.setDefaultType(TextFieldType.NUMBER);
            tf.setDefaultToolTip(getAttributeTooltip(ad));
            return tf;
        }
        if (ad.getType() == AttributeDefinition.DOUBLE) {
            TextField tf = createParsedTextField(page, "_" + UUID.randomUUID(), ad, props, Double::valueOf, value);//TODO: locale dependent parsing
            tf.setDefaultType(TextFieldType.NUMBER);
            tf.setDefaultToolTip(getAttributeTooltip(ad));
            return tf;
        }
        
        //handles AttributeDefinition.CHARACTER, STRING and PASSWORD
        TextField defaultText = createParsedTextField(page, "_" + UUID.randomUUID(), ad, props, s -> s, value);
        defaultText.setDefaultToolTip(getAttributeTooltip(ad));
        if (ad.getType() == AttributeDefinition.PASSWORD || ad.getID().toLowerCase().contains("password")) {
            logger.debug("password field: {}", ad.getID());
            defaultText.setDefaultType(TextFieldType.PASSWORD);
        }
        return defaultText;
    }
    
    String getAttributeTooltip(AttributeDefinition ad) {
        String[] a = ad.getDefaultValue();
        String def = a == null
                ? ""
                : ad.getCardinality() == 0
                    ? a[0]
                    : Arrays.toString(a);
        return String.format("%s:%s %s", ad.getID(), getAttributeTypeWithCardinality(ad), def);
    }
    
    @SuppressWarnings("deprecation")
    String getAttributeTypeString(AttributeDefinition ad) {
        switch (ad.getType()) {
            case AttributeDefinition.BOOLEAN: return "Boolean";
            case AttributeDefinition.BYTE: return "Byte";
            case AttributeDefinition.CHARACTER: return "Character";
            case AttributeDefinition.DOUBLE: return "Double";
            case AttributeDefinition.FLOAT: return "Float";
            case AttributeDefinition.INTEGER: return "Integer";
            case AttributeDefinition.LONG: return "Long";
            case AttributeDefinition.PASSWORD: return "Password";
            case AttributeDefinition.SHORT: return "Short";
            case AttributeDefinition.STRING: return "String";
            case AttributeDefinition.BIGDECIMAL: return "BigDecimal";
            case AttributeDefinition.BIGINTEGER: return "BigInteger";
            default: return "unknown";
        }
    }
    
    String getAttributeTypeWithCardinality(AttributeDefinition ad) {
        int c = ad.getCardinality();
        if (c < 0) {
            return String.format("List<%s>", getAttributeTypeString(ad)); //actually Vector
        } else if (c == 0) {
            return getAttributeTypeString(ad);
        } else {
            return getAttributeTypeString(ad) + "[]";
        }
    }
    
    OgemaWidget createNewConfigWidget(ObjectClassDefinition ocd, WidgetPage<?> page, OgemaWidget parent, OgemaHttpRequest req, ResourceBundle rb) {
        SimpleGrid grid = new SimpleGrid(page, "_" + UUID.randomUUID().toString(), false);
        AttributeDefinition[] attrDef = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
        final Map<String, Object> props = new HashMap<>();
        int rowNum = 0;
        for (; rowNum < attrDef.length; rowNum++) {
            AttributeDefinition ad = attrDef[rowNum];
            String label = String.format("%s (%s)", ad.getName(), ad.getID());
            grid.addItem(new Label(page, "_" + UUID.randomUUID().toString(), label), true, req);
            OgemaWidget edit = createEditWidgetForAttribute(Optional.empty(), ad, props, page, req, rb);
            grid.addItem(rowNum, 1, edit, req);
            String desc = String.format("%s (%s:%s)", ad.getDescription(), ad.getID(), getAttributeTypeWithCardinality(ad));
            grid.addItem(rowNum, 1, new Label(page, "_" + UUID.randomUUID().toString(), desc), req);
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
                    //TODO: validate name ?
                }
            }
        };
        Alert info = new Alert(page, "_"+UUID.randomUUID(), "");
        Button btCreate = new Button(page, "_" + UUID.randomUUID().toString(), rb.getString("new_config_button")) {
            static final long serialVersionUID = 1;

            @Override
            public void onPOSTComplete(String data, OgemaHttpRequest req) {
                try {
                    Configuration conf = isFactoryPid
                            ? ca.getFactoryConfiguration(ocdPid, cfgName[0])
                            : ca.getConfiguration(ocdPid);
                    conf.setBundleLocation(null); //set unbound, will be set to this bundle otherwise
                    updateConfiguration(ocd, conf, props, info, req, rb);
                    rebuild(req);
                } catch (IOException ex) {
                    //TODO
                    System.out.println(ex);
                }
            }
        };
        btCreate.triggerOnPOST(info);
        //TODO: display new configuration after create
        btCreate.triggerOnPOST(flex);
        Label lbNewName = new Label(page, "_" + UUID.randomUUID().toString(), rb.getString("new_config_name"));
        Accordion acc = new Accordion(page, "_" + UUID.randomUUID().toString());
        if (isFactoryPid) {
            grid.addItem(lbNewName, true, req);
            grid.addItem(rowNum, 1, tfConfigName, req);
            grid.addItem(rowNum++, 1, btCreate, req);
        } else {
            grid.addItem(btCreate, true, req);
        }
        grid.addItem(info, true, req);
        acc.addItem(new DefaultLabelledItem(
                "_" + UUID.randomUUID().toString(), rb.getString("new_config_label"),
                rb.getString("new_config_desc")), grid, false, req);
        return acc;
    }
    
    void updateConfiguration(ObjectClassDefinition ocd, Configuration conf, Map<String, Object> props, Alert info, OgemaHttpRequest req, ResourceBundle rb) throws IOException {
        Dictionary<String, Object> cfgProps = conf.getProperties();
        if (cfgProps == null) {
            cfgProps = new Hashtable<>();
        }
        AttributeDefinition[] attrDefs = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
        for (Entry<String, Object> e : props.entrySet()) {
            if (e.getValue() == null) {
                cfgProps.remove(e.getKey());
            } else {
                Optional<AttributeDefinition> a = Stream.of(attrDefs).filter(ad -> ad.getID().equals(e.getKey())).findAny();
                try {
                    Object val = a.map(ad -> parseValue(ad, e.getValue(), info, req, rb)).orElse(e.getValue());
                    cfgProps.put(e.getKey(), val);
                } catch (RuntimeException ex) {
                    logger.debug("parse failed", ex);
                    return;
                }
            }
        }
        conf.updateIfDifferent(cfgProps);
        info.showAlert(rb.getString("commit_success"), true, req);
    }
    
    //TODO: type checks & conversion
    Object parseValue(AttributeDefinition ad, Object val, Alert info, OgemaHttpRequest req, ResourceBundle rb) {
        if (ad.getCardinality() == 0) {
            return parseSingleValue(ad, val, info, req, rb);
        } else {
            return parseListValue(ad, val, info, req, rb);
        }
    }
    
    Object parseListValue(AttributeDefinition ad, Object val, Alert info, OgemaHttpRequest req, ResourceBundle rb) {
        boolean outputAsArray = ad.getCardinality() > 0;
        @SuppressWarnings("unchecked")
        List<String> input = (List<String>) val;
        List<Object> output = new ArrayList<>(input.size());
        for (String s: input) {
            output.add(parseSingleValue(ad, s, info, req, rb));
        }
        if (outputAsArray) {
            //TODO
        }
        return output;
    }
    
    Object parseSingleValue(AttributeDefinition ad, Object val, Alert info, OgemaHttpRequest req, ResourceBundle rb) {
        logger.trace("parsing {} as {}", val, ad);
        if (val == null) {
            return null;
        }
        switch (ad.getType()) {
            case AttributeDefinition.BOOLEAN:
                if (val instanceof Boolean) {
                    return val;
                } else {
                    String s = val.toString().toLowerCase();
                    switch (s) {
                        case "true" : return Boolean.TRUE;
                        case "false" : return Boolean.FALSE;
                        default:
                            info.showAlert(String.format(rb.getString("parse_failed"), ad.getID(), getAttributeTypeString(ad), s), false, req);
                            throw new IllegalArgumentException("Not a boolean value: " + s);
                    }
                }
            case AttributeDefinition.BYTE:
                if (val instanceof Number) {
                    return ((Number)val).byteValue();
                } else {
                    String s = val.toString();
                    try {
                        return Byte.parseByte(s);
                    } catch (RuntimeException ex) {
                        info.showAlert(String.format(rb.getString("parse_failed"), ad.getID(), getAttributeTypeString(ad), s), false, req);
                        throw ex;
                    }
                }
            case AttributeDefinition.CHARACTER:
                if (val instanceof Character) {
                    return val;
                } else {
                    String s = val.toString();
                    if (s.length() > 1) {
                        info.showAlert(String.format(rb.getString("parse_failed"), ad.getID(), getAttributeTypeString(ad), s), false, req);
                        throw new IllegalArgumentException("Not a Character value: " + s);
                    } else if (s.length() == 0) {
                            return null;
                    } else {
                        return s.charAt(0);
                    }
                }
            case AttributeDefinition.DOUBLE:
                if (val instanceof Number) {
                    return ((Number)val).doubleValue();
                } else {
                    String s = val.toString();
                    try {
                        return Double.parseDouble(s);
                    } catch (RuntimeException ex) {
                        info.showAlert(String.format(rb.getString("parse_failed"), ad.getID(), getAttributeTypeString(ad), s), false, req);
                        throw ex;
                    }
                }
            case AttributeDefinition.FLOAT:
                if (val instanceof Number) {
                    return ((Number)val).floatValue();
                } else {
                    String s = val.toString();
                    try {
                        return Float.parseFloat(s);
                    } catch (RuntimeException ex) {
                        info.showAlert(String.format(rb.getString("parse_failed"), ad.getID(), getAttributeTypeString(ad), s), false, req);
                        throw ex;
                    }
                }
            case AttributeDefinition.INTEGER:
                if (val instanceof Number) {
                    return ((Number)val).intValue();
                } else {
                    String s = val.toString();
                    try {
                        return Integer.parseInt(s);
                    } catch (RuntimeException ex) {
                        info.showAlert(String.format(rb.getString("parse_failed"), ad.getID(), getAttributeTypeString(ad), s), false, req);
                        throw ex;
                    }
                }
            case AttributeDefinition.LONG:
                if (val instanceof Number) {
                    return ((Number)val).longValue();
                } else {
                    String s = val.toString();
                    try {
                        return Long.parseLong(s);
                    } catch (RuntimeException ex) {
                        info.showAlert(String.format(rb.getString("parse_failed"), ad.getID(), getAttributeTypeString(ad), s), false, req);
                        throw ex;
                    }
                }
            case AttributeDefinition.PASSWORD:
            case AttributeDefinition.STRING:
                if (val instanceof String) {
                    return val;
                } else {
                    return val.toString();
                }
            case AttributeDefinition.SHORT:
                if (val instanceof Number) {
                    return ((Number)val).shortValue();
                } else {
                    String s = val.toString();
                    try {
                        return Short.parseShort(s);
                    } catch (RuntimeException ex) {
                        info.showAlert(String.format(rb.getString("parse_failed"), ad.getID(), getAttributeTypeString(ad), s), false, req);
                        throw ex;
                    }
                }
            default:
                // includes the deprecated BIGINTEGER and BIGDECIMAL attribute types
                logger.warn("unsupported attribute type in configuration: {}", ad);
        }
        return val;
    }
    
    static AtomicInteger count = new AtomicInteger();

    OgemaWidget createEditConfigWidget(Configuration cfg, ObjectClassDefinition ocd,
            WidgetPage<?> page, OgemaHttpRequest req, ResourceBundle rb) {
        Accordion acc = new Accordion(page, ResourceUtils.getValidResourceName("editConfig_" + cfg.getPid() + "_" + count.getAndIncrement()));
        SimpleGrid grid = new SimpleGrid(page, "_" + UUID.randomUUID().toString(), false);
        AttributeDefinition[] attrDef = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
        int rowNum = 0;
        final Map<String, Object> props = new HashMap<>();
        for (; rowNum < attrDef.length; rowNum++) {
            AttributeDefinition ad = attrDef[rowNum];
            grid.addItem(new Label(page, "_" + UUID.randomUUID().toString(), ad.getName()), true, req);
            OgemaWidget ed = createEditWidgetForAttribute(Optional.of(cfg), ad, props, page, req, rb);
            grid.addItem(rowNum, 1, ed, req);
            String desc = String.format("%s (%s:%s)", ad.getDescription(), ad.getID(), getAttributeTypeWithCardinality(ad));
            grid.addItem(rowNum, 1, new Label(page, "_" + UUID.randomUUID().toString(), desc), req);
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
        
        Alert info = new Alert(page, "_"+UUID.randomUUID(),"");
        
        Button btUpdate = new Button(page, "_" + UUID.randomUUID().toString(), rb.getString("update_config_button")) {
            static final long serialVersionUID = 1;
            
            @Override
            public void onPOSTComplete(String data, OgemaHttpRequest req) {
                try {
                    updateConfiguration(ocd, cfg, props, info, req, rb);
                } catch (IOException ex) {
                    //TODO
                    System.out.println(ex);
                }
            }
        };
        btUpdate.triggerOnPOST(info);
        Button btDelete = new Button(page, "_" + UUID.randomUUID().toString(), rb.getString("delete_config_button")) {
            static final long serialVersionUID = 1;
            
            @Override
            public void onPOSTComplete(String data, OgemaHttpRequest req) {
                try {
                    cfg.delete();
                    acc.setVisible(false, req);
                    rebuild(req);
                } catch (IOException ex) {
                    //TODO
                    System.out.println(ex);
                }
            }
        };
        btDelete.triggerOnPOST(flex);
        grid.addItem(btUpdate, true, req);
        grid.addItem(rowNum, 1, btDelete, req);
        grid.addItem(rowNum++, 2, info, req);
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
