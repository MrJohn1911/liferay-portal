/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.workflow.web.internal.util.comparator;

import com.liferay.portal.kernel.util.DateUtil;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.workflow.WorkflowDefinition;

import java.util.Date;

/**
 * @author Adam Brandizzi
 */
public class WorkflowDefinitionModifiedDateComparator
	extends OrderByComparator<WorkflowDefinition> {

	public WorkflowDefinitionModifiedDateComparator(boolean ascending) {
		_ascending = ascending;
	}

	@Override
	public int compare(
		WorkflowDefinition workflowDefinition1,
		WorkflowDefinition workflowDefinition2) {

		int value = Boolean.compare(
			workflowDefinition1.isActive(), workflowDefinition2.isActive());

		if (value == 0) {
			Date modifiedDate1 = workflowDefinition1.getModifiedDate();
			Date modifiedDate2 = workflowDefinition2.getModifiedDate();

			value = DateUtil.compareTo(modifiedDate1, modifiedDate2);

			if (_ascending) {
				return value;
			}
		}

		return -value;
	}

	@Override
	public String getOrderBy() {
		if (_ascending) {
			return _ORDER_BY_ASC;
		}

		return _ORDER_BY_DESC;
	}

	@Override
	public String[] getOrderByFields() {
		return _ORDER_BY_FIELDS;
	}

	@Override
	public boolean isAscending() {
		return _ascending;
	}

	private static final String _ORDER_BY_ASC = "modifiedDate ASC";

	private static final String _ORDER_BY_DESC = "modifiedDate DESC";

	private static final String[] _ORDER_BY_FIELDS = {"modifiedDate"};

	private final boolean _ascending;

}