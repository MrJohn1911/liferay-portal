/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.content.site.generator.internal.workflow.node.delegate;

import com.liferay.ai.hub.workflow.node.ServiceNodeDelegate;
import com.liferay.content.site.generator.internal.workflow.node.delegate.util.PageSpecToIRConverter;
import com.liferay.portal.kernel.util.GetterUtil;

import java.io.Serializable;

import java.util.Map;

import org.osgi.service.component.annotations.Component;

/**
 * @author Mario Gomes
 * @author Iliyan Peychev
 */
@Component(service = ServiceNodeDelegate.class)
public class PageSpecToIRServiceNodeDelegate implements ServiceNodeDelegate {

	@Override
	public String execute(
			Map<String, String> inputVariables,
			Map<String, Serializable> workflowContext)
		throws Exception {

		PageSpecToIRConverter pageSpecToIRConverter =
			new PageSpecToIRConverter();

		return pageSpecToIRConverter.convert(
			inputVariables.get("currentPageSpecification"),
			inputVariables.get("pageTitle"),
			GetterUtil.getString(inputVariables.get("locale"), "en-US"));
	}

	@Override
	public String getKey() {
		return "javaDelegate#pageSpecToIR";
	}

}