/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util;

import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.modelarmor.v1.ModelArmorClient;
import com.google.cloud.modelarmor.v1.ModelArmorSettings;

import com.liferay.ai.hub.internal.configuration.ModelArmorConfiguration;
import com.liferay.portal.configuration.module.configuration.ConfigurationProviderUtil;
import com.liferay.portal.kernel.module.configuration.ConfigurationException;

import java.io.IOException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.threeten.bp.Duration;

/**
 * @author João Victor Alves
 */
public class ModelArmorClientUtil {

	public static ModelArmorClient getModelArmorClient(long companyId)
		throws ConfigurationException, IOException {

		ModelArmorClient modelArmorClient = _modelArmorClients.get(companyId);

		if (modelArmorClient != null) {
			return modelArmorClient;
		}

		synchronized (_modelArmorClients) {
			modelArmorClient = _modelArmorClients.get(companyId);

			if (modelArmorClient != null) {
				return modelArmorClient;
			}

			ModelArmorConfiguration modelArmorConfiguration =
				ConfigurationProviderUtil.getCompanyConfiguration(
					ModelArmorConfiguration.class, companyId);

			ModelArmorSettings.Builder modelArmorSettingsBuilder =
				ModelArmorSettings.newBuilder(
				).setEndpoint(
					"modelarmor." + modelArmorConfiguration.location() +
						".rep.googleapis.com:443"
				);

			Duration sanitizeRPCTimeout = Duration.ofMillis(
				modelArmorConfiguration.sanitizeRPCTimeout());

			modelArmorSettingsBuilder.sanitizeUserPromptSettings(
			).setRetrySettings(
				_buildSanitizeRetrySettings(
					modelArmorSettingsBuilder.sanitizeUserPromptSettings(
					).getRetrySettings(),
					sanitizeRPCTimeout)
			);

			modelArmorSettingsBuilder.sanitizeModelResponseSettings(
			).setRetrySettings(
				_buildSanitizeRetrySettings(
					modelArmorSettingsBuilder.sanitizeModelResponseSettings(
					).getRetrySettings(),
					sanitizeRPCTimeout)
			);

			modelArmorClient = ModelArmorClient.create(
				modelArmorSettingsBuilder.build());

			_modelArmorClients.put(companyId, modelArmorClient);

			return modelArmorClient;
		}
	}

	public static void invalidate(long companyId) {
		ModelArmorClient modelArmorClient = _modelArmorClients.remove(
			companyId);

		if (modelArmorClient != null) {
			modelArmorClient.close();
		}
	}

	private static RetrySettings _buildSanitizeRetrySettings(
		RetrySettings retrySettings, Duration timeout) {

		return retrySettings.toBuilder(
		).setInitialRpcTimeout(
			timeout
		).setMaxRpcTimeout(
			timeout
		).setTotalTimeout(
			timeout
		).build();
	}

	private static final Map<Long, ModelArmorClient> _modelArmorClients =
		new ConcurrentHashMap<>();

}