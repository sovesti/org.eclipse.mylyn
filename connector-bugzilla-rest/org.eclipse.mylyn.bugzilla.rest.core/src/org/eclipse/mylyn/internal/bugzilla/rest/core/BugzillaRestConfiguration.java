/*******************************************************************************
 * Copyright (c) 2013 Frank Becker and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Frank Becker - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.bugzilla.rest.core;

import java.io.Serializable;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.bugzilla.rest.core.response.data.Component;
import org.eclipse.mylyn.internal.bugzilla.rest.core.response.data.Field;
import org.eclipse.mylyn.internal.bugzilla.rest.core.response.data.FieldValues;
import org.eclipse.mylyn.internal.bugzilla.rest.core.response.data.Parameters;
import org.eclipse.mylyn.internal.bugzilla.rest.core.response.data.Product;
import org.eclipse.mylyn.internal.bugzilla.rest.core.response.data.SortableActiveEntry;
import org.eclipse.mylyn.internal.bugzilla.rest.core.response.data.StatusTransition;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskOperation;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;

public class BugzillaRestConfiguration implements Serializable {

	private static final BugzillaRestCreateTaskSchema SCHEMA = BugzillaRestCreateTaskSchema.getDefault();

	private static final long serialVersionUID = 4173223872076958202L;

	private final String repositoryId;

	private Map<String, Field> fields;

	private Map<String, Product> products;

	private Parameters parameters;

	public BugzillaRestConfiguration(String repositoryId) {
		this.repositoryId = repositoryId;
	}

	public String getRepositoryId() {
		return repositoryId;
	}

	void setFields(Map<String, Field> fields) {
		Function<Field, String> getName = new Function<Field, String>() {
			public String apply(Field item) {
				return item.getName();
			}
		};
		Function<String, String> comparatorFunction = Functions.compose(getName, Functions.forMap(fields));
		Ordering<String> comparator = Ordering.natural().onResultOf(comparatorFunction);
		this.fields = ImmutableSortedMap.copyOf(fields, comparator);
	}

	public Map<String, Field> getFields() {
		return fields;
	}

	public Field getFieldWithName(String fieldName) {
		return fields.get(fieldName);
	}

	void setProducts(Map<String, Product> products) {
		Function<Product, String> getName = new Function<Product, String>() {
			public String apply(Product item) {
				return item.getName();
			}
		};
		Function<String, String> comparatorFunction = Functions.compose(getName, Functions.forMap(products));
		Ordering<String> comparator = Ordering.natural().onResultOf(comparatorFunction);
		this.products = ImmutableSortedMap.copyOf(products, comparator);
	}

	public Map<String, Product> getProducts() {
		return products;
	}

	public Product getProductWithName(String productName) {
		return products.get(productName);
	}

	void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}

	public Parameters getParameters() {
		return parameters;
	}

	private Component getProductComponentWithName(Product product, String name) {
		for (Component componentEntry : product.getComponents()) {
			if (componentEntry.getName().equals(name)) {
				return componentEntry;
			}
		}
		return null;
	}

	public void updateInitialTaskData(TaskData data) throws CoreException {
		setProductOptions(data);
		updateProductOptions(data);
		for (String key : data.getRoot().getAttributes().keySet()) {
			if (key.equals(BugzillaRestTaskSchema.getDefault().ADD_SELF_CC.getKey())
					|| key.equals(BugzillaRestTaskSchema.getDefault().NEW_COMMENT.getKey())
					|| key.equals(TaskAttribute.OPERATION)) {
				continue;
			}
			TaskAttribute attribute = data.getRoot().getAttribute(key);
			if (!key.equals(SCHEMA.PRODUCT.getKey())) {
				String configName = mapTaskAttributeKey2ConfigurationFields(key);
				Field configField = getFieldWithName(configName);
				if (configField == null) {
					continue;
				}
				if (configName.equals("component") || configName.equals("version")
						|| configName.equals("target_milestone")) {
					if (attribute.getOptions().size() == 1 && attribute.getValue().isEmpty()) {
						attribute.setValue((String) attribute.getOptions().values().toArray()[0]);
					}
				} else {
					FieldValues[] val = configField.getValues();
					if (val != null && val.length > 0) {
						for (FieldValues fieldValues : val) {
							if (configName.equals("bug_status")) {
								if (fieldValues.getName() == null) {
									for (StatusTransition bugzillaRestBugStatusTransition : fieldValues
											.getCanChangeTo()) {
										attribute.putOption(bugzillaRestBugStatusTransition.getName(),
												bugzillaRestBugStatusTransition.getName());
									}
								}
							} else {
								attribute.putOption(fieldValues.getName(), fieldValues.getName());
							}

						}
					}
				}
			}
			if (attribute.getValue().isEmpty()) {
				attribute.setValue(getValueFromParameter(key));
			}

		}
		for (Field Field : fields.values()) {
			if (Field.isCustom() && Field.isOnBugEntry()) {
				TaskAttribute attribute = data.getRoot().createAttribute(Field.getName());
				if (attribute != null) {
					attribute.getMetaData().defaults().setLabel(Field.getDisplayName());
					attribute.getMetaData().setKind(TaskAttribute.KIND_DEFAULT);
					String type = getAttributeTypFromFieldTyp(Field.getType());
					attribute.getMetaData().setType(type);
					if (type.equals(TaskAttribute.TYPE_SINGLE_SELECT)) {
						attribute.getMetaData().setRequired(true);
					}

					FieldValues[] values1 = Field.getValues();
					if (values1 != null) {
						for (FieldValues FieldValues : values1) {
							attribute.putOption(FieldValues.getName(), FieldValues.getName());
						}
					}
					attribute.getMetaData().setReadOnly(false);
				}
			}
		}
	}

	private String getValueFromParameter(String attributeId) {
		if (attributeId.equals(TaskAttribute.PRIORITY)) {
			return getParameters().getDefaultpriority();
		} else if (attributeId.equals(TaskAttribute.SEVERITY)) {
			return getParameters().getDefaultseverity();
		} else if (attributeId.equals("platform")) {
			if (getParameters().getDefaultplatform() == null || getParameters().getDefaultplatform().isEmpty()) {
				return "All";
			} else {
				return getParameters().getDefaultplatform();
			}
		} else if (attributeId.equals("os")) {
			if (getParameters().getDefaultopsys() == null || getParameters().getDefaultopsys().isEmpty()) {
				return "All";
			} else {
				return getParameters().getDefaultopsys();
			}
		}
		return "";
	}

	private String getAttributeTypFromFieldTyp(int fieldTyp) throws CoreException {
		switch (fieldTyp) {
		case 1://Free Text
			return TaskAttribute.TYPE_SHORT_TEXT;
		case 2: //DropDown
			return TaskAttribute.TYPE_SINGLE_SELECT;
		case 3: //Multiple-Selection Box
			return TaskAttribute.TYPE_MULTI_SELECT;
		case 4: //Large Text Box
			return TaskAttribute.TYPE_LONG_TEXT;
		case 5: //Date/Time
			return TaskAttribute.TYPE_DATETIME;
		case 6: //Bug Id
			return TaskAttribute.TYPE_INTEGER;
		case 7: //Bug URLs
			return TaskAttribute.TYPE_URL;
		default:
			Status status = new Status(IStatus.INFO, BugzillaRestCore.ID_PLUGIN,
					"unknown custom field type " + fieldTyp);
			StatusHandler.log(status);
			throw new CoreException(status);
		}
	}

	private String mapTaskAttributeKey2ConfigurationFields(String taskAttributeKey) {
		String resultString;
		if (taskAttributeKey.equals("task.common.summary")) {
			resultString = "short_desc";
		} else if (taskAttributeKey.equals(TaskAttribute.PRODUCT) //
				|| taskAttributeKey.equals(TaskAttribute.RESOLUTION) //
				|| taskAttributeKey.equals(TaskAttribute.PRIORITY) //
				|| taskAttributeKey.equals(TaskAttribute.COMPONENT) //
				|| taskAttributeKey.equals(TaskAttribute.VERSION)) {
			resultString = taskAttributeKey.substring(12);
		} else if (taskAttributeKey.equals(TaskAttribute.STATUS)) {
			resultString = "bug_status";
		} else if (taskAttributeKey.equals(TaskAttribute.USER_ASSIGNED)) {
			resultString = "assigned_to";
		} else if (taskAttributeKey.equals(TaskAttribute.USER_CC)) {
			resultString = "cc";
		} else if (taskAttributeKey.equals(TaskAttribute.DESCRIPTION)) {
			resultString = "longdesc";
		} else if (taskAttributeKey.equals("description_is_private")) {
			resultString = "longdescs.isprivate";
		} else if (taskAttributeKey.equals("os")) {
			resultString = "op_sys";
		} else if (taskAttributeKey.equals("platform")) {
			resultString = "rep_platform";
		} else if (taskAttributeKey.equals(TaskAttribute.SEVERITY)) {
			resultString = "bug_severity";
		} else if (taskAttributeKey.equals("comment")) {
			resultString = "longdesc";
		} else {
			resultString = taskAttributeKey;
		}
		return resultString;
	}

	private void setAttributeOptionsForProduct(TaskAttribute taskAttribute, Product actualProduct) {
		taskAttribute.clearOptions();
		if (taskAttribute.getId().equals(SCHEMA.TARGET_MILESTONE.getKey())) {
			internalSetAttributeOptions(taskAttribute, actualProduct.getMilestones());
		} else if (taskAttribute.getId().equals(SCHEMA.VERSION.getKey())) {
			internalSetAttributeOptions(taskAttribute, actualProduct.getVersions());
		} else if (taskAttribute.getId().equals(SCHEMA.COMPONENT.getKey())) {
			internalSetAttributeOptions(taskAttribute, actualProduct.getComponents());
		}
	}

	private void internalSetAttributeOptions(TaskAttribute taskAttribute, SortableActiveEntry[] actualProductEntry) {
		boolean found = false;
		String actualValue = taskAttribute.getValue();
		for (SortableActiveEntry SortableActiveEntry : actualProductEntry) {
			if (SortableActiveEntry.isActive()) {
				taskAttribute.putOption(SortableActiveEntry.getName(), SortableActiveEntry.getName());
				if (!found) {
					found = actualValue.equals(SortableActiveEntry.getName());
				}
			}
		}
		if (!found) {
			taskAttribute.setValue(""); //$NON-NLS-1$
		}
	}

	public boolean updateAfterComponentChange(TaskAttribute taskAttribute) {
		if (taskAttribute != null) {
			if (taskAttribute.getId().equals(SCHEMA.COMPONENT.getKey())) {
				TaskAttribute rootAttribute = taskAttribute.getTaskData().getRoot();
				TaskAttribute productAttribute = taskAttribute.getTaskData()
						.getRoot()
						.getMappedAttribute(SCHEMA.PRODUCT.getKey());
				Product actualProduct = getProductWithName(productAttribute.getValue());
				Component actualComponent = getProductComponentWithName(actualProduct, taskAttribute.getValue());
				if (actualComponent != null) {
					taskAttribute.getMetaData().putValue(TaskAttribute.META_DESCRIPTION,
							actualComponent.getDescription());
					TaskAttribute attributeQaContact = rootAttribute.getMappedAttribute(SCHEMA.QA_CONTACT.getKey());
					if (attributeQaContact != null) {
						attributeQaContact.setValue(actualComponent.getDefaultQaContact());
					}
					TaskAttribute attributeAssignedTo = rootAttribute.getMappedAttribute(SCHEMA.ASSIGNED_TO.getKey());
					if (attributeAssignedTo != null) {
						attributeAssignedTo.setValue(actualComponent.getDefaultAssignedTo());
					}
				}
			}
			return true;
		}
		return false;
	}

	public boolean setProductOptions(@NonNull TaskData taskData) {
		TaskAttribute attributeProduct = taskData.getRoot().getMappedAttribute(SCHEMA.PRODUCT.getKey());
		if (attributeProduct != null) {
			SortedSet<String> products = new TreeSet<String>();
			Field configFieldComponent = getFieldWithName("component");
			FieldValues[] val = configFieldComponent.getValues();
			if (val != null && val.length > 0) {
				for (FieldValues fieldValues : val) {
					for (String visibilityValue : fieldValues.getVisibilityValues()) {
						if (!products.contains(visibilityValue)) {
							products.add(visibilityValue);
						}
					}
				}
				attributeProduct.clearOptions();
				for (String productName : products) {
					attributeProduct.putOption(productName, productName);
				}
			}

			return true;
		}
		return false;
	}

	public boolean updateProductOptions(@NonNull TaskData taskData) {
		TaskAttribute attributeProduct = taskData.getRoot().getMappedAttribute(SCHEMA.PRODUCT.getKey());
		if (!attributeProduct.getValue().isEmpty()) {
			Product actualProduct = getProductWithName(attributeProduct.getValue());

			TaskAttribute attributeComponent = taskData.getRoot().getMappedAttribute(SCHEMA.COMPONENT.getKey());
			if (attributeComponent != null) {
				setAttributeOptionsForProduct(attributeComponent, actualProduct);
			}
			TaskAttribute attributeVersion = taskData.getRoot().getMappedAttribute(SCHEMA.VERSION.getKey());
			if (attributeVersion != null) {
				setAttributeOptionsForProduct(attributeVersion, actualProduct);
			}
			TaskAttribute attributeTargetMilestone = taskData.getRoot()
					.getMappedAttribute(SCHEMA.TARGET_MILESTONE.getKey());
			if (attributeTargetMilestone != null) {
				setAttributeOptionsForProduct(attributeTargetMilestone, actualProduct);
			}
		} else {
			TaskAttribute attributeComponent = taskData.getRoot().getMappedAttribute(SCHEMA.COMPONENT.getKey());
			if (attributeComponent != null) {
				setAllAttributeOptions(attributeComponent, getFieldWithName("component")); //$NON-NLS-1$
			}
			TaskAttribute attributeVersion = taskData.getRoot().getMappedAttribute(SCHEMA.VERSION.getKey());
			if (attributeVersion != null) {
				setAllAttributeOptions(attributeVersion, getFieldWithName("version")); //$NON-NLS-1$
			}
			TaskAttribute attributeTargetMilestone = taskData.getRoot()
					.getMappedAttribute(SCHEMA.TARGET_MILESTONE.getKey());
			if (attributeTargetMilestone != null) {
				setAllAttributeOptions(attributeTargetMilestone, getFieldWithName("target_milestone")); //$NON-NLS-1$
			}
		}
		return true;
	}

	private void setAllAttributeOptions(TaskAttribute updateAttribute, Field configField) {
		FieldValues[] val = configField.getValues();
		if (val != null && val.length > 0) {
			for (FieldValues fieldValues : val) {
				updateAttribute.putOption(fieldValues.getName(), fieldValues.getName());
			}
		}
	}

	public void addValidOperations(TaskData bugReport) {
		TaskAttribute attributeStatus = bugReport.getRoot().getMappedAttribute(TaskAttribute.STATUS);
		String attributeStatusValue = attributeStatus.getValue();
		TaskAttribute operationAttribute = bugReport.getRoot().getAttribute(TaskAttribute.OPERATION);
		if (operationAttribute == null) {
			operationAttribute = bugReport.getRoot().createAttribute(TaskAttribute.OPERATION);
		}
		TaskAttribute attribute = bugReport.getRoot()
				.createAttribute(TaskAttribute.PREFIX_OPERATION + attributeStatusValue);
		TaskOperation.applyTo(attribute, attributeStatusValue, attributeStatusValue);
		// set as default
		TaskOperation.applyTo(operationAttribute, attributeStatusValue, attributeStatusValue);
		Field status = getFieldWithName("bug_status");
		for (FieldValues fieldValues : status.getValues()) {
			if (((attributeStatusValue == null || attributeStatusValue.isEmpty()) && fieldValues.getName() == null)
					|| (attributeStatusValue != null && attributeStatusValue.equals(fieldValues.getName()))) {
				for (StatusTransition statusTransition : fieldValues.getCanChangeTo()) {
					attribute = bugReport.getRoot()
							.createAttribute(TaskAttribute.PREFIX_OPERATION + statusTransition.name);
					TaskOperation.applyTo(attribute, statusTransition.name, statusTransition.name);
					if (statusTransition.name != null && statusTransition.name.equals("RESOLVED")) {
						TaskAttribute attrResolvedInput = attribute.getTaskData()
								.getRoot()
								.createAttribute("resolutionInput");
						attrResolvedInput.getMetaData().setType(TaskAttribute.TYPE_SINGLE_SELECT);
						attribute.getMetaData().putValue(TaskAttribute.META_ASSOCIATED_ATTRIBUTE_ID, "resolutionInput");
						Field resolution = getFieldWithName("resolution");
						for (FieldValues resolutionValues : resolution.getValues()) {
							if (resolutionValues.getName().compareTo("DUPLICATE") != 0) {
								attrResolvedInput.putOption(resolutionValues.getName(), resolutionValues.getName());
							}
						}
					}
				}
			}
		}
		attribute = bugReport.getRoot().createAttribute(TaskAttribute.PREFIX_OPERATION + "duplicate");
		TaskOperation.applyTo(attribute, "duplicate", "Mark as Duplicate");
		TaskAttribute attrResolvedInput = attribute.getTaskData().getRoot().createAttribute("dup_id");
		attrResolvedInput.getMetaData().setType(TaskAttribute.TYPE_TASK_DEPENDENCY);
		attribute.getMetaData().putValue(TaskAttribute.META_ASSOCIATED_ATTRIBUTE_ID, "dup_id");
	}
}