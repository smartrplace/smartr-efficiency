package org.smartrplace.smarteff.resourcecsv.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.tools.resource.util.ResourceUtils;

public class ResourceListCSVRows<T extends Resource> extends SingleValueResourceCSVRow {

	protected List<String> resourceRow = new ArrayList<>();;
	protected List<String> nameRow = new ArrayList<>();;
	protected List<String> versionRow = new ArrayList<>();;
	protected List<String> unitRow = new ArrayList<>();;
	protected final ResourceList<T> resList;
	
	public ResourceListCSVRows(ResourceList<T> resList, boolean exportUnknown) {
		this.resList = resList;
		setHeaderRows(exportUnknown);
	}

	private void setHeaderRows(boolean exportUnknown) {
		resourceRow.add("Resource");
		nameRow.add("Name");
		versionRow.add("Version");
		unitRow.add("Unit");
		
		List<T> subResources = resList.getAllElements();
		if(!exportUnknown) subResources = Arrays.asList(subResources.get(0));
		for (T subRes : subResources) {
			List<Resource> subSubResources = subRes.getSubResources(false);
			for (Resource subSubRes : subSubResources) {
				if (exportUnknown || !subSubRes.isDecorator()) {
					String name = subSubRes.getName();
					if (!resourceRow.contains(name)) {
						resourceRow.add(name);
						String humanName = ResourceUtils.getHumanReadableName(subSubRes);
						nameRow.add(humanName);
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
	}

	public List<List<String>> getRows(Locale locale) {
		ArrayList<List<String>> rows = new ArrayList<>();

		SingleValueResourceCSVRow header = new SingleValueResourceCSVRow(SingleValueResourceCSVRow.init.EMPTY);
		header.name = ResourceUtils.getHumanReadableShortName(resList);
		header.value = "ResourceList";
		header.resource = resList.getName();
		header.path = resList.getPath();
		header.elementType = resList.getElementType().getSimpleName();
		rows.add(header.values());

		rows.add(versionRow);
		rows.add(nameRow);
		rows.add(unitRow);
		rows.add(resourceRow);
		
		for (T subRes : resList.getAllElements()) {
			List<String> row = new ArrayList<>();
			for (String col: resourceRow) {
				if (col == "Resource") {
					row.add(subRes.getName());
					continue;
				}
				Resource colRes = subRes.getSubResource(col);
				if (colRes instanceof SingleValueResource) {
					row.add(ResourceCSVUtil.getValueAsString((SingleValueResource) colRes, locale));
				} else {
					row.add("");
				}
			}
			rows.add(row);
		}
		
		return rows;
	}
}
