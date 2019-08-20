package org.smartrplace.smarteff.resourcecsv;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.persistence.DBConstants;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.SmartEff2DMap;
import org.smartrplace.extensionservice.SmartEff2DMapPrimaryValue;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.smarteff.resourcecsv.util.ResourceCSVConfigUtil;
import org.smartrplace.smarteff.resourcecsv.util.ResourceCSVUtil;
import org.smartrplace.smarteff.resourcecsv.util.ScheduleCSVRows;
import org.smartrplace.smarteff.resourcecsv.util.SmartEffCSVRecord;
import org.smartrplace.smarteff.util.editgeneric.SubTypeHandler;

import com.google.common.collect.Iterables;

import de.iwes.util.resource.ValueResourceHelper;

/**
 * Allows the import of resources via CSV
 * @author jruckel
 *
 */
public class ResourceCSVImporter {
	//TODO: Make this an enum
	protected final boolean nonAllowedElementMode;
	private final ApplicationManagerSPExt appManExt;
	private final List<ExtensionResourceTypeDeclaration<?>> types;
	private final SubTypeHandler typeHandler;
	protected final CSVConfiguration conf;
	protected final Map<String, String> exportConf = new HashMap<>();
	private final Logger log = LoggerFactory.getLogger(ResourceCSVImporter.class);
	
	private final Map<Resource, String> references = new HashMap<>();
	/** Old path --> new path */
	private final Map<String, String> paths = new HashMap<>();

	private static final String TS_FORMAT_SEP = " ";
	
	protected enum Parsing {
		SINGLE, LIST_HEADERS, LIST_ELEMENTS, LIST_AGG_HEADERS,
		LIST_AGG_ELEMENTS, TS_HEADERS, TS_VALUES, MAP
	}
	protected Parsing parsing = Parsing.SINGLE;
	
	public ResourceCSVImporter(boolean nonAllowedElementMode, ApplicationManagerSPExt appManExt) {
		this.nonAllowedElementMode = nonAllowedElementMode;
		this.appManExt = appManExt;
		this.types = appManExt.getAllTypeDeclarations();
		this.typeHandler = new SubTypeHandler(null, appManExt);
		this.conf = new CSVConfiguration();
	}

	/**
	 * Extract data from a CSV file at filePaht and place it into targetParentResource
	 * @param filePath
	 * @param targetParentResource
	 * @return
	 */
	public Resource importFromFile(String filePath, Resource targetParentResource) {
		log.debug("-  CSV Import  ---------------------------------------------------------");
		conf.initDefaults(targetParentResource, targetParentResource.getParent());
		try {
			Reader r = newReader(new FileInputStream(filePath));
			importCSV(r);
			r.close();
			log.debug("------------------------------------------------------------------------");
			return targetParentResource;
		} catch (IOException e) {
			reportError("Export failed due to an IOException " + e);
			e.printStackTrace();
		}
		return null;
	}
	
	public Resource importCSV(Reader reader) throws IOException {
		
		if (conf.parent == null)
			return null;
		CSVParser parser = new CSVParser(reader, CSVConfiguration.CSV_FORMAT);
		
		if (!checkHeaders(parser))
			return null;
		
		/** True while we're parsing the config metadata. */
		boolean parsingConfig = false;
		
		/** Map of columns.  Used for storing list element subresources. */
		Map<String, Integer> colMap = new HashMap<>();
		ResourceList<?> lastResList = null;
		Schedule lastTS = null;
		SmartEff2DMap lastMap = null;
		List<Float> lastMapSecKeys = new ArrayList<>();
		int lastMapIdx = 0;
		Map<Integer, Map<Integer, Float>> lastMapValues = new HashMap<>();
		
		/** Column number --> unit */
		Map<Integer, String> units = new HashMap<>();
		Map<String, ResourceList<?>> aggLists = new HashMap<>();
		String tsFormat = "";
		int nTsCols = 0;
		
		for (CSVRecord r : parser) {
			SmartEffCSVRecord record = new SmartEffCSVRecord(r, conf);
			parsingConfig = parsingConfig || record.get(0).equals(CSVConfiguration.HEADERS.CONFIG);

			if (parsingConfig) {
				if (record.isEmpty()) {
					if (conf.origParentPath == null || conf.origParentPath.isEmpty()
							|| conf.origRootPath == null || conf.origRootPath.isEmpty()) {
						parser.close();
						throw new IllegalStateException("Could not read parent or root path.  Unable to import.");
					}
					parsingConfig = false;
					continue;
				}
				if (record.size() < 2) continue;
				String key = record.get(0);
				String val = record.get(1);
				if (!exportConf.containsKey(key)) {
					exportConf.put(key, val);
					if (key.equals("root") || key.equals("parent")) {
						String path = ResourceCSVConfigUtil.getPath(exportConf, key);
						if (key.equals("root") ) conf.origRootPath = path;
						else if (key.equals("parent")) conf.origParentPath = path;
					} else if (key.equals("locale")) {
						conf.locale = new Locale(val.split("_")[0]); // Codes like de_DE won't work
					}
				}
				continue;
			}
			
			if (record.isEmpty()) {
				if (parsing == Parsing.MAP) {
					writeMapValues(lastMap, lastMapValues);
				}
				parsing = Parsing.SINGLE; // Reset
			}

			if (parsing == Parsing.SINGLE) {

				if (!record.isValid()) {
					log.debug("Record #" + record.getRecordNumber() + " ('" + record.getName() + "') is invalid.");
					continue;
				}
				
				String type = record.getTypeName();
				log.trace("Record #" + r.getRecordNumber() + " is named '" + record.getName()
						+ "', of type '" + type + "' and has path '" + record.getRelPathRoot() + "'.");
				if (type == null)
					continue;
				
				Resource parent = getOrCreateParent(record);
				if (parent == null) {
					log.debug("Record #" + r.getRecordNumber() + " has no parent.");
					continue;
				}
				if (!parent.exists())
					parent.create();
				
				String resName = record.get(CSVConfiguration.HEADERS.RESOURCE);
				if (type.equals(CSVConfiguration.HEADERS.SINGLE_VALUE_RESOURCE)) {
					parsing = Parsing.SINGLE;
					createSingleValueResource(parent, resName,
							record.getValue(), record.getSvrType(), record.getRelPathRoot());
				} else if (type.equals(CSVConfiguration.HEADERS.RESOURCELIST)) {
					parsing = Parsing.LIST_HEADERS;
					lastResList = createResourceList(parent, resName, null, record.getElementType(),
							record.getRelPathRoot());
				} else if (type.equals(CSVConfiguration.HEADERS.RESOURCELIST_AGG)) {
					parsing = Parsing.LIST_AGG_HEADERS;
				} else if (type.equals(CSVConfiguration.HEADERS.SMARTEFFTIMESERIES)) {
					if (parent instanceof SmartEffTimeSeries) {
						parsing = Parsing.TS_HEADERS;
						lastTS = createSchedule((SmartEffTimeSeries) parent);
					} else {
						log.info("Schedule parent is '{}' but should be SmartEffTimeSeries.", parent.getResourceType());
					}
				} else if (type.equals(CSVConfiguration.HEADERS.SMARTEFF2DMAP)) {
					lastMap = createMap(parent, resName);
					if (lastMap != null) {
						parsing = Parsing.MAP;
						lastMapSecKeys.clear();
						lastMapIdx = 0;
						lastMapValues.clear();
					}
				} else {
					log.info("Nonstandard type '" + type + "'.");
					getOrCreate(record);
				}
			} else if (parsing == Parsing.LIST_HEADERS || parsing == Parsing.LIST_AGG_HEADERS) {
				String header = record.getName();
				if (header.equals(CSVConfiguration.HEADERS.RESOURCE)) {
					if (parsing == Parsing.LIST_HEADERS) {
						parsing = Parsing.LIST_ELEMENTS;
						aggLists.clear();
					} else if (parsing == Parsing.LIST_AGG_HEADERS) {
						parsing = Parsing.LIST_AGG_ELEMENTS;
					}
					colMap.clear();
					String[] subResources = record.getValues();
					for (int i = 1; i < subResources.length; i++) {
						String subRes = subResources[i];
						colMap.put(subRes, i);
					}
				} else if (header.equals(CSVConfiguration.HEADERS.UNIT)) {
					String[] u = record.getValues();
					for (int i = 1; i < u.length; i++) {
						units.put(i, u[i]);
					}
				}
			} else if (parsing == Parsing.TS_HEADERS) {
				if (record.get(0).startsWith(ScheduleCSVRows.EXPORT_FORMAT_PREFIX)) {
					parsing = Parsing.TS_VALUES;
					colMap.clear();
					tsFormat = "";
					nTsCols = 0;
					String[] cols = record.getValues();
					for (int i = 0; i < cols.length; i++) {
						colMap.put(cols[i], i);
						if (cols[i].startsWith(ScheduleCSVRows.EXPORT_FORMAT_PREFIX)) {
							String fmt = cols[i].replaceAll("^" + ScheduleCSVRows.EXPORT_FORMAT_PREFIX, "");
							tsFormat = tsFormat + TS_FORMAT_SEP + fmt;
							nTsCols++;
						}
					}
					
				}
			} else if (parsing == Parsing.MAP) {
				if (lastMap == null) continue;
				if (!lastMap.primaryKeyLabel().exists()) {
					ValueResourceHelper.setCreate(lastMap.primaryKeyLabel(), record.get(1));
					ValueResourceHelper.setCreate(lastMap.secondaryKeyLabel(), record.get(0));
				} else if (record.get(0).equals(CSVConfiguration.HEADERS.DATA)) {
					lastMap.primaryKeys().create();
					List<Float> primKeys = new ArrayList<>();
					String[] cols = record.getValues();
					for (int i = 1; i < cols.length; i++) {
						primKeys.add(ResourceCSVUtil.parseFloat(conf.locale, cols[i]));
						lastMapValues.put(i-1, new HashMap<>());
					}
					Float[] pK = Iterables.toArray(primKeys, Float.class);
					lastMap.primaryKeys().setValues(ArrayUtils.toPrimitive(pK));
				} else {
					// Write secondary key (TODO: Only write at the end of the map)
					lastMap.secondaryKeys().create();
					lastMapSecKeys.add(ResourceCSVUtil.parseFloat(conf.locale, record.get(0)));
					Float[] secKeys = Iterables.toArray(lastMapSecKeys, Float.class);
					lastMap.secondaryKeys().setValues(ArrayUtils.toPrimitive(secKeys));
					// Store values
					String[] cols = record.getValues();
					for (int i = 1; i < cols.length; i++) {
						float val = ResourceCSVUtil.parseFloat(conf.locale, cols[i]);
						lastMapValues.get(i-1).put(lastMapIdx, val);
					}
					lastMapIdx++;
				}
			} else if (parsing == Parsing.LIST_ELEMENTS) {
				if (lastResList == null) continue;
				String origListPath = null;
				String listPath = ResourceCSVUtil.getRelativePath(lastResList, conf);
				for (String origPath : paths.keySet()) {
					if (paths.get(origPath).equals(listPath)) {
						origListPath = origPath;
						break;
					}
				}

				String origListElemPath = origListPath + DBConstants.PATH_SEPARATOR + record.getName();
				Resource elem = createResourceListElement(lastResList, origListElemPath);
				for (String header : colMap.keySet()) {
					int col = colMap.get(header);
					String value = record.get(col);
					if (value.contains(CSVConfiguration.HEADERS.REFERENCE)) {
						createReference(elem, header, value, null);
					} else {
						
						createSingleValueResource(elem, header, value, null,
								origListElemPath + DBConstants.PATH_SEPARATOR + header);
					}
					String unit = units.get(col);
					if (unit != null && unit.startsWith(CSVConfiguration.HEADERS.AGG)) {
						ResourceList<?> l = createResourceList(elem, header, null,
								unit.split(CSVConfiguration.HEADERS.AGG)[1], null);
						for (String listElemName : value.split(CSVConfiguration.HEADERS.AGG_LIST_SEP))
							aggLists.put(listElemName, l);
					}
				}
			} else if (parsing == Parsing.LIST_AGG_ELEMENTS) {
				ResourceList<?> l = aggLists.get(record.getName());
				if (l == null) {
					reportError("No list element belonging to " + record.getName() + " found.");
					continue;
				}
				Resource elem = createResourceListElement(l, record.getRelPathRoot());
				for (String header : colMap.keySet()) {
					int col = colMap.get(header);
					String value = record.get(col);
					if (value.contains(CSVConfiguration.HEADERS.REFERENCE))
						createReference(elem, header, value, null);
					else
						createSingleValueResource(elem, header, value, null, record.getRelPathRoot());
				}
			} else if (parsing == Parsing.TS_VALUES) {
				String[] cols = record.getValues();
				if (nTsCols >= cols.length) continue; // Incomplete record
				String ts = "";
				for (int i = 0; i < cols.length; i++) {
					if (i < nTsCols) {
						ts = ts + TS_FORMAT_SEP + cols[i];
					} else {
						Date time = null;
						try {
							SimpleDateFormat d = new SimpleDateFormat(tsFormat);
							time = d.parse(ts);
						} catch (ParseException e) {
							continue;
						}
						addScheduleValue(lastTS, time.getTime(), cols[i]);
					}
				}
			}

			
		}
		parser.close();
		setReferences();
		conf.parent.activate(true);
		return conf.parent;
	}
	
	/** Note: {@link #setReferences()} must be called to actually set the references */
	private Resource createReference(Resource elem, String resName, String targetPath, String type) {
		Resource res = elem.getSubResource(resName);
		if (res == null) return null;
		res.create();
		references.put(res, targetPath.replaceFirst("^" + CSVConfiguration.HEADERS.REFERENCE, ""));
		return res;
	}
	
	private void setReferences() {
		for (Resource r : references.keySet()) {
			String targetPathOrig = references.get(r);
			String targetPath = paths.get(targetPathOrig);
			if (targetPath == null) continue;
			Resource target = getByPath(targetPath);
			if (target == null) continue;
			r.setAsReference(target);
		}
	}
	
	private Resource getByPath(String path) {
		Resource r = conf.root;
		for (String name : path.split(DBConstants.PATH_SEPARATOR)) {
			if (r != null)
				r = r.getSubResource(name);
		}
		return r;
	}

	private Resource getOrCreateParent(SmartEffCSVRecord record) {
		return getOrCreate(record, true);
	}
	
	private Resource getOrCreate(SmartEffCSVRecord record) {
		return getOrCreate(record, false);
	}
	
	private Resource getOrCreate(SmartEffCSVRecord record, boolean parent) {
		String relPath = record.getRelPath();
		String resName = record.getResName();
		if (parent && (relPath.equals(resName) || resName.startsWith(relPath + CSVConfiguration.HEADERS.AGG)))
			return conf.parent;
		if (parent && !relPath.contains(DBConstants.PATH_SEPARATOR))
			return null;
		if (parent)
			relPath = relPath.replaceAll("/" + resName + "$", ""); // Remove resource itself from path.  Does not apply to TimeSeries: `schedule` is also created.
		String[] requiredParents = relPath.split(DBConstants.PATH_SEPARATOR);
		Resource p = conf.parent;
		for (int i = 0; i < requiredParents.length; i++) {
			String subResName = requiredParents[i];
			Resource subRes = p.getSubResource(subResName);
			if (subRes == null) { // Dealing with a decorating resource
				log.trace("'{}' has no subresource named '{}'.  Attempting to create decorator.", p.getName(), subResName);
				if (!conf.exportImportUnknown) return null;
				String typeName = null;
				if (!parent && i == requiredParents.length - 1)
					typeName = record.getType();
				if (typeName == null || typeName.isEmpty()) {
					reportError("Creation of '" + subResName + "' failed.  "
							+ "No type found for record #" + record.getRecordNumber() + ".");
					return null;
				}
				try {
					Class<?> type = getType(typeName);
					if (Resource.class.isAssignableFrom(type)) {
						subRes = p.addDecorator(subResName, (Class<Resource>) type);
						log.trace("Added decorating resource '{}' to '{}'", subRes.getName(), p.getName());
					}
				} catch (Exception e) {
					reportError(e.getMessage());
				}
			}
			if (subRes != null) {
				if (!parent)
					storePath(record.getRelPathRoot(), ResourceCSVUtil.getRelativePath(subRes, conf));
				if (subRes instanceof SmartEffTimeSeries) {
					return subRes;
				} else {
					try {
						subRes.create();
						p = subRes;
					} catch (Exception e) {
						reportError(e.getMessage());
						return null;
					}
				}
			}
		}
		return p;
	}

	private boolean checkHeaders(CSVParser p) {
		Set<String> present = p.getHeaderMap().keySet();
		List<String> missing = new ArrayList<>();
		for (String header : CSVConfiguration.HEADERS.REQUIRED) {
			if (!present.contains(header))
				missing.add(header);
		}
		if(missing.size() == 0) {
			return true;
		} else {
			reportError("Missing the following headers: " + String.join(", ", missing));
			return false;
		}
	}

	protected boolean checkSubResourceCreation(Resource parent, String subResourceName) {
		if(!ResourceUtils.isValidResourceName(subResourceName)) {
			reportError("Invalid resource name '"+subResourceName+ "' requested for '"+parent.getLocation()+"'!");
			return false;
		}
		Resource subResource = parent.getSubResource(subResourceName);
		if(subResource != null && subResource.exists()) {
			//TODO: In some cases existing resources can be accepted, even merging would be nice
			//This is the simplest security measure, though
			reportError("Subresource '"+subResourceName+ "' requested for '"+parent.getLocation()+ "' already exists!");
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @param parent
	 * @param subResourceName
	 * @param value
	 * @param type may be null. In this case the type has to be identified via the name
	 */
	protected void createSingleValueResource(Resource parent, String subResourceName, String value,
			Class<? extends SingleValueResource> type, String oldPath) {
		if(!checkSubResourceCreation(parent, subResourceName)) return;
		SingleValueResource newRes;
		if(type != null) {
			newRes = parent.getSubResource(subResourceName, type);
		} else {
			Class<? extends Resource> subType = typeHandler.getSubTypes(parent.getResourceType()).get(subResourceName);
			if(subType == null || !(SingleValueResource.class.isAssignableFrom(subType))) {
				reportError(subResourceName+" at "+parent.getLocation()+" is not a SingleValueResource("+subType+")!");
				return;
			}
			newRes = (SingleValueResource) parent.getSubResource(subResourceName, subType);
		}
		newRes.create();
		//TODO: handler format exceptions
		setValue(newRes, value);
		storePath(oldPath, ResourceCSVUtil.getRelativePath(newRes, conf));
	}

	private void setValue(SingleValueResource newRes, String value) {
		if (value == null || value.isEmpty())
			return;

		if (newRes instanceof FloatResource)
			value = ResourceCSVUtil.unFormat(conf.locale, value);

		if (newRes instanceof TemperatureResource)
			((TemperatureResource) newRes).setCelsius(Float.parseFloat(value));
		else
			ValueResourceUtils.setValue(newRes, value);
	}

	/** TODO: Create resource list
	 * 
	 * @param parent
	 * @param subResourceName
	 * @param typeString
	 * @param elementTypeString
	 * @return ResourceList created elementType must be set
	 */
	protected ResourceList<?> createResourceList(Resource parent, String subResourceName,
			String typeString, String elementTypeString, String origPath) {
		ResourceList<?> result;
		Resource r = parent.getSubResource(subResourceName);
		if (r == null) {
			reportError("List is not part of " + parent.getName());
			return null;
		} else if (!(r instanceof ResourceList)) {
			reportError("Resource already exists, but is '" + r.getResourceType().getSimpleName() + "', not a list.");
			return null;
		} else {
			result = (ResourceList<?>) r;
		}
		result.create();
		if (!result.getElementType().getName().endsWith(elementTypeString)) {
			reportError("List types differ");
			return null;
		}
		storePath(origPath, ResourceCSVUtil.getRelativePath(result, conf));
		return result;
	}
	
	protected <T extends Resource> T createResourceListElement(ResourceList<T> parent, String origPath) {
		T result = parent.add();
		storePath(origPath, ResourceCSVUtil.getRelativePath(result, conf));
		if (origPath == null)
			return result;
		//result.getSubResource("name", StringResource.class).<StringResource>create().setValue(elementName);
		return result;
	}
	
	protected Schedule createSchedule(SmartEffTimeSeries ts) {
		// TODO perform checks?
		Schedule sched = ts.schedule();
		if (!sched.exists()) {
			ts.recordedDataParent().create();
			ts.recordedDataParent().program().create();
			sched.setAsReference(ts.recordedDataParent().program());
		}
		return sched;
	}
	
	protected SmartEff2DMap createMap(Resource parent, String resName) {
		SmartEff2DMap map = parent.getSubResource(resName);
		if (map == null)
			map = parent.addDecorator(resName, SmartEff2DMap.class);
		return map.create();
	}
	
	protected void writeMapValues(SmartEff2DMap map, Map<Integer, Map<Integer, Float>> lastMapValues) {
		log.trace("Writing to map {}", map);
		int nPrim = lastMapValues.size();
		int nSec = lastMapValues.get(0).size();
		map.characteristics().create();
		for (int p = 0; p < nPrim; p++) {
			SmartEff2DMapPrimaryValue val = map.characteristics().add();
			val.index().create();
			val.index().setValue(p);
			val.val().create();
			List<Float> l = new ArrayList<>();
			for (int s = 0; s < nSec; s++) {
				l.add(lastMapValues.get(p).get(s));
			}
			Float[] v = Iterables.toArray(l, Float.class);
			val.val().setValues(ArrayUtils.toPrimitive(v));
		}
	}
	
	protected void addScheduleValue(Schedule sched, long ts, String valueString) {
		float value = ResourceCSVUtil.parseFloat(conf.locale, valueString);
		sched.addValue(ts, new FloatValue(value));
	}
	
	protected Class<?> getType(String typeName) {
		for (ExtensionResourceTypeDeclaration<?> type : types) {
			String name = type.dataType().getName();
			if (name.equals(typeName))
				return type.dataType();
		}
		for (ExtensionResourceTypeDeclaration<?> type : types) {
			String name = type.dataType().getName();
			if (name.toLowerCase().endsWith(typeName.toLowerCase()))
				return type.dataType();
		}
		try {
			return Class.forName(typeName);
		} catch (ClassNotFoundException e) {
			//
		}
		reportError("Did not find '" + typeName + "'");
		return null;
	}
	
	private void storePath(String origPath, String newPath) {
		if (origPath != null && newPath != null)
			paths.put(origPath, newPath);
	}
	
	private void reportError(String message) {
		if(nonAllowedElementMode) {
			log.info("Error: {}", message);
		} else
			throw new IllegalStateException(message);
	}
	
	/**
	* Creates a reader capable of handling BOMs.
	* Taken from http://commons.apache.org/proper/commons-csv/user-guide.html#Handling_Byte_Order_Marks
	*/
	public InputStreamReader newReader(final InputStream inputStream) {
		return new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
	}
}
