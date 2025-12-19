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

<div class="ml-8 mr-8 mt-5">
	<div class="mb-6">
		<h2>AI Tasks</h2>
	</div>

	<frontend-data-set:headless-display
		apiURL="<%= taskDefinitionDisplayContext.getAPIURL() %>"
		formName="fm"
		id="<%= AIHubSiteInitializerFDSNames.TASK_DEFINITION %>"
		itemsPerPage="<%= 10 %>"
		selectedItemsKey="id"
		selectionType="multiple"
		style="fluid"
	/>
</div>