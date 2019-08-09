package org.smartrplace.smarteff.resourcecsv.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.tools.resource.util.ResourceUtils;

public class ResourceListCSVRows<T extends Resource> extends SingleValueResourceCSVRow {

	protected List<String> headerRow = new ArrayList<>();;
	protected List<String> resourceRow = new ArrayList<>();;
	protected List<String> nameRow = new ArrayList<>();;
	protected List<String> versionRow = new ArrayList<>();;
	protected List<String> unitRow = new ArrayList<>();;
	protected final ResourceList<T> resList;
	protected List<T> subResources;
	protected final String label;
	protected final boolean exportUnknown;
	protected boolean isAggregated;
	
	public ResourceListCSVRows(ResourceList<T> resList, boolean exportUnknown, String label) {
		this(resList, exportUnknown, label, null);
	}

	/**
	 * Direct use of this constructor is intended for aggregated ResourceLists.
	 * @param resList the <i>parent</i> resource list
	 * @param exportUnknown
	 * @param label
	 * @param subResources elements aggregated from multiple ResourceLists within the parent list
	 */
	public ResourceListCSVRows(ResourceList<T> resList, boolean exportUnknown, String label, List<T> subResources) {
		this.resList = resList;
		this.label = label;
		this.exportUnknown = exportUnknown;
		if (subResources != null) {
			this.subResources = subResources;
			isAggregated = true;
		} else {
			this.subResources = resList.getAllElements();
			isAggregated = false;
		}
		setHeaderRows();
	}

	private void setHeaderRows() {
		resourceRow.add("Resource");
		nameRow.add("Name");
		versionRow.add("Version");
		unitRow.add("Unit");
		
		List<T> subResourcesForHeaders;
		if(!exportUnknown)
			subResourcesForHeaders = Arrays.asList(subResources.get(0));
		else
			subResourcesForHeaders = subResources;

		for (T subRes : subResourcesForHeaders) {
			List<Resource> subSubResources = subRes.getSubResources(false);
			for (Resource subSubRes : subSubResources) {
				if (exportUnknown || !subSubRes.isDecorator()) {
					String name = subSubRes.getName();
					if (!resourceRow.contains(name)) {
						resourceRow.add(name);
						String humanName = ResourceUtils.getHumanReadableShortName(subSubRes);
						nameRow.add(humanName);
						if (subSubRes instanceof ResourceList)
							unitRow.add("(agg)");
						else
							unitRow.add(ResourceCSVUtil.getUnit(subSubRes));
						versionRow.add("1| "); // TODO
					}
				}
			}
		}
		// Ensure that names are in column B
		if(resourceRow.contains("name")) {
			int idx = resourceRow.indexOf("name");
			resourceRow.add(1, resourceRow.remove(idx));
			nameRow.add(1, nameRow.remove(idx));
			versionRow.add(1, versionRow.remove(idx));
			unitRow.add(1, unitRow.remove(idx));
		}
		
		SingleValueResourceCSVRow header = new SingleValueResourceCSVRow(SingleValueResourceCSVRow.init.EMPTY);
		if (isAggregated) {
			if (!subResources.isEmpty()) {
				String elemType = subResources.get(0).getResourceType().getSimpleName();
				header.name = elemType + " in " + label;
				header.resource = resList.getName() + "_agg_" + elemType;
				header.elementType = elemType;
			}
			header.value = "ResourceList (agg)";
		} else {
			header.name = label;
			header.value = "ResourceList";
			header.resource = resList.getName();
			header.elementType = resList.getElementType().getSimpleName();
		}
		header.path = resList.getPath();
		headerRow = header.values();

	}

	public List<List<String>> getRows(Locale locale) {
		return getRows(locale, subResources);
	}

	public <U extends Resource> List<List<String>> getRows(Locale locale, List<U> resources) {
		
		List<List<String>> rows = new ArrayList<>();
		if (resources.isEmpty())
			return rows;

		Map<Class<? extends Resource>, List<? extends Resource>> nestedLists = new HashMap<>();

		rows.add(headerRow);
		rows.add(versionRow);
		rows.add(nameRow);
		rows.add(unitRow);
		rows.add(resourceRow);
		
		for (U subRes : resources) {
			List<String> row = new ArrayList<>();
			for (String col: resourceRow) {
				if (col == "Resource") {
					if (!isAggregated) {
						row.add(subRes.getName());
					} else {
						// Just the name is not sufficient for aggregated resource lists.
						row.add(getRelativePath(subRes));
					}
					continue;
				}
				Resource colRes = subRes.getSubResource(col);
				if (colRes instanceof SingleValueResource) {
					row.add(ResourceCSVUtil.getValueAsString((SingleValueResource) colRes, locale));
				} else if (colRes instanceof ResourceList) {
					List<String> elemNames = new ArrayList<>();
					for (Object elem : ((ResourceList<?>) colRes).getAllElements()) {
						if (elem instanceof Resource)
							elemNames.add(getRelativePath((Resource) elem));
					}
					Class<? extends Resource> elemType = ((ResourceList<?>) colRes).getElementType();
					if (nestedLists.containsKey(elemType)) {
						List<? extends Resource> l = nestedLists.get(elemType);
						l.addAll(((ResourceList) colRes).getAllElements());
					} else {
						nestedLists.put(elemType, ((ResourceList) colRes).getAllElements());
					}
					nestedLists.put(colRes.getResourceType(), ((ResourceList<?>) colRes).getAllElements());
					row.add(String.join(",", elemNames));
				} else {
					row.add("");
				}
			}
			rows.add(row);
		}
		
		rows.add(Arrays.asList(new String[] {""}));
		for (Class<? extends Resource> clazz : nestedLists.keySet()) {
			@SuppressWarnings({ "unchecked", "rawtypes" }) // XXX
			ResourceListCSVRows<?> r = new ResourceListCSVRows(resList, exportUnknown, label, nestedLists.get(clazz));
			rows.addAll(r.getRows(locale));
		}

		return rows;
	}
	
	/** Get a path relative to {@link #resList}.  Returns absolute path if not a subresource of {@link #resList}. */
	protected String getRelativePath(Resource res) {
		String listPath = resList.getPath();
		String resPath = res.getPath();
		if (!resPath.startsWith(listPath))
			return resPath;
		else
			return resPath.substring(listPath.length() + 1);
	}
}
