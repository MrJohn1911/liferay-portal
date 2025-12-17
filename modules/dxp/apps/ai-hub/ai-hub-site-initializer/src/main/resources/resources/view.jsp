<%--
/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
TaskDefinitionDisplayContext taskDefinitionDisplayContext = new TaskDefinitionDisplayContext();
%>

<frontend-data-set:headless-display
	apiURL="<%= taskDefinitionDisplayContext.getAPIURL() %>"
	bulkActionDropdownItems="<%= taskDefinitionDisplayContext.getBulkActionDropdownItems() %>"
	fdsActionDropdownItems="<%= taskDefinitionDisplayContext.getFDSActionDropdownItems() %>"
	formName="fm"
	id="<%= AIHubSiteInitializerFDSNames.TASK_DEFINITION %>"
	itemsPerPage="<%= 10 %>"
	selectedItemsKey="id"
	selectionType="multiple"
	style="fluid"
/>