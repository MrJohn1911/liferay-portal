/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.content.site.generator.internal.workflow.node.delegate;

import com.liferay.ai.hub.util.AgentUtil;
import com.liferay.ai.hub.workflow.node.ServiceNodeDelegate;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Validator;

import java.io.Serializable;

import java.util.Map;

import org.osgi.service.component.annotations.Component;

/**
 * @author Iliyan Peychev
 */
@Component(service = ServiceNodeDelegate.class)
public class AcknowledgeAgentServiceNodeDelegate
	implements ServiceNodeDelegate {

	@Override
	public String execute(
		Map<String, String> inputVariables,
		Map<String, Serializable> workflowContext) {

		String acknowledgment = inputVariables.get("acknowledgment");

		if (Validator.isNull(acknowledgment)) {
			acknowledgment =
				"I am generating the content now. The preview will refresh " +
					"as soon as it is ready.";
		}

		Message message = new Message();

		message.put(
			"workflowContext",
			HashMapBuilder.<String, Serializable>put(
				"output", acknowledgment
			).build());
		message.put(
			"workflowInstanceId",
			GetterUtil.getLong(workflowContext.get("workflowInstanceId")));

		AgentUtil.complete(message);

		return acknowledgment;
	}

	@Override
	public String getKey() {
		return "javaDelegate#acknowledgeAgent";
	}

}