/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.guardrail;

import com.google.cloud.modelarmor.v1.ModelArmorClient;

import com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util.ModelArmorClientUtil;
import com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util.ModelArmorTemplateConfig;
import com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util.ModelArmorTemplateUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

/**
 * @author João Victor Alves
 */
public class ModelArmorOutputGuardrail implements OutputGuardrail {

	public ModelArmorOutputGuardrail(
		long companyId, ModelArmorTemplateConfig modelArmorTemplateConfig) {

		_companyId = companyId;
		_modelArmorTemplateConfig = modelArmorTemplateConfig;
	}

	@Override
	public OutputGuardrailResult validate(AiMessage aiMessage) {
		try {
			ModelArmorClient modelArmorClient =
				ModelArmorClientUtil.getModelArmorClient(_companyId);

			if (ModelArmorTemplateUtil.isMatchFound(
					_companyId, modelArmorClient, _modelArmorTemplateConfig,
					aiMessage.text())) {

				return fatal("Response blocked: Contains restricted content.");
			}

			return success();
		}
		catch (Exception exception) {
			_log.error(exception);

			return fatal(
				"Response blocked: Unable to validate against security " +
					"policy.");
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ModelArmorOutputGuardrail.class);

	private final long _companyId;
	private final ModelArmorTemplateConfig _modelArmorTemplateConfig;

}