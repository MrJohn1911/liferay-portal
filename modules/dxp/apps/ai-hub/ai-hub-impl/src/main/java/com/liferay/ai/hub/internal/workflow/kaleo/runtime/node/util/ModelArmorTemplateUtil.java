/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.modelarmor.v1.DataItem;
import com.google.cloud.modelarmor.v1.DetectionConfidenceLevel;
import com.google.cloud.modelarmor.v1.FilterConfig;
import com.google.cloud.modelarmor.v1.FilterMatchState;
import com.google.cloud.modelarmor.v1.LocationName;
import com.google.cloud.modelarmor.v1.MaliciousUriFilterSettings;
import com.google.cloud.modelarmor.v1.ModelArmorClient;
import com.google.cloud.modelarmor.v1.PiAndJailbreakFilterSettings;
import com.google.cloud.modelarmor.v1.RaiFilterSettings;
import com.google.cloud.modelarmor.v1.RaiFilterType;
import com.google.cloud.modelarmor.v1.SanitizationResult;
import com.google.cloud.modelarmor.v1.SanitizeModelResponseRequest;
import com.google.cloud.modelarmor.v1.SanitizeModelResponseResponse;
import com.google.cloud.modelarmor.v1.SanitizeUserPromptRequest;
import com.google.cloud.modelarmor.v1.SanitizeUserPromptResponse;
import com.google.cloud.modelarmor.v1.SdpBasicConfig;
import com.google.cloud.modelarmor.v1.SdpFilterSettings;
import com.google.cloud.modelarmor.v1.Template;
import com.google.protobuf.FieldMask;

import com.liferay.ai.hub.internal.configuration.ModelArmorConfiguration;
import com.liferay.ai.hub.internal.configuration.VertexAIConfiguration;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.configuration.module.configuration.ConfigurationProviderUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.module.configuration.ConfigurationException;
import com.liferay.portal.kernel.util.Validator;

import java.util.Map;

/**
 * @author João Victor Alves
 */
public class ModelArmorTemplateUtil {

	public static void createTemplate(
			long companyId, ModelArmorClient modelArmorClient,
			ModelArmorTemplateConfig modelArmorTemplateConfig)
		throws ConfigurationException {

		if ((modelArmorTemplateConfig == null) ||
			Validator.isNull(modelArmorTemplateConfig.getTemplateId())) {

			return;
		}

		ModelArmorConfiguration modelArmorConfiguration =
			ConfigurationProviderUtil.getCompanyConfiguration(
				ModelArmorConfiguration.class, companyId);
		VertexAIConfiguration vertexAIConfiguration =
			ConfigurationProviderUtil.getCompanyConfiguration(
				VertexAIConfiguration.class, companyId);

		modelArmorClient.createTemplate(
			LocationName.of(
				vertexAIConfiguration.projectId(),
				modelArmorConfiguration.location()),
			_buildTemplate(modelArmorConfiguration, modelArmorTemplateConfig),
			modelArmorTemplateConfig.getTemplateId());
	}

	public static void deleteTemplate(
			long companyId, ModelArmorClient modelArmorClient,
			String templateId)
		throws ConfigurationException {

		try {
			modelArmorClient.deleteTemplate(
				_buildTemplatePath(companyId, templateId));
		}
		catch (NotFoundException notFoundException) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Model Armor template not found, nothing to delete: " +
						templateId,
					notFoundException);
			}
		}
	}

	public static boolean isMatchFound(
			long companyId, ModelArmorClient modelArmorClient,
			ModelArmorTemplateConfig modelArmorTemplateConfig, String text)
		throws ConfigurationException {

		Template template;

		try {
			template = modelArmorClient.getTemplate(
				_buildTemplatePath(
					companyId, modelArmorTemplateConfig.getTemplateId()));
		}
		catch (NotFoundException notFoundException) {
			throw new ConfigurationException(
				"The Model Armor template " +
					modelArmorTemplateConfig.getTemplateId() +
						" was not found.",
				notFoundException);
		}

		SanitizationResult sanitizationResult = null;

		if (modelArmorTemplateConfig.getGuardrailType() ==
				ModelArmorTemplateConfig.GuardrailType.INPUT) {

			SanitizeUserPromptResponse sanitizeUserPromptResponse =
				modelArmorClient.sanitizeUserPrompt(
					SanitizeUserPromptRequest.newBuilder(
					).setName(
						template.getName()
					).setUserPromptData(
						DataItem.newBuilder(
						).setText(
							text
						).build()
					).build());

			sanitizationResult =
				sanitizeUserPromptResponse.getSanitizationResult();
		}
		else {
			SanitizeModelResponseResponse sanitizeModelResponseResponse =
				modelArmorClient.sanitizeModelResponse(
					SanitizeModelResponseRequest.newBuilder(
					).setName(
						template.getName()
					).setModelResponseData(
						DataItem.newBuilder(
						).setText(
							text
						).build()
					).build());

			sanitizationResult =
				sanitizeModelResponseResponse.getSanitizationResult();
		}

		if (sanitizationResult.getFilterMatchState() ==
				FilterMatchState.MATCH_FOUND) {

			return true;
		}

		return false;
	}

	public static void updateTemplate(
			long companyId, ModelArmorClient modelArmorClient,
			ModelArmorTemplateConfig modelArmorTemplateConfig)
		throws ConfigurationException {

		if ((modelArmorTemplateConfig == null) ||
			Validator.isNull(modelArmorTemplateConfig.getTemplateId())) {

			return;
		}

		ModelArmorConfiguration modelArmorConfiguration =
			ConfigurationProviderUtil.getCompanyConfiguration(
				ModelArmorConfiguration.class, companyId);

		Template template = Template.newBuilder(
			_buildTemplate(modelArmorConfiguration, modelArmorTemplateConfig)
		).setName(
			_buildTemplatePath(
				companyId, modelArmorTemplateConfig.getTemplateId())
		).build();

		modelArmorClient.updateTemplate(
			template,
			FieldMask.newBuilder(
			).addPaths(
				"filter_config"
			).addPaths(
				"template_metadata"
			).addPaths(
				"labels"
			).build());
	}

	private static FilterConfig _buildFilterConfig(
		ModelArmorTemplateConfig modelArmorTemplateConfig) {

		FilterConfig.Builder builder = FilterConfig.newBuilder();

		if (modelArmorTemplateConfig.isMaliciousUriFilterEnabled()) {
			builder.setMaliciousUriFilterSettings(
				MaliciousUriFilterSettings.newBuilder(
				).setFilterEnforcement(
					MaliciousUriFilterSettings.MaliciousUriFilterEnforcement.
						ENABLED
				));
		}

		if (modelArmorTemplateConfig.isPiAndJailbreakFilterEnabled()) {
			builder.setPiAndJailbreakFilterSettings(
				PiAndJailbreakFilterSettings.newBuilder(
				).setFilterEnforcement(
					PiAndJailbreakFilterSettings.
						PiAndJailbreakFilterEnforcement.ENABLED
				).setConfidenceLevel(
					modelArmorTemplateConfig.getPiAndJailbreakConfidenceLevel()
				));
		}

		if (modelArmorTemplateConfig.isSdpFilterEnabled()) {
			builder.setSdpSettings(
				SdpFilterSettings.newBuilder(
				).setBasicConfig(
					SdpBasicConfig.newBuilder(
					).setFilterEnforcement(
						SdpBasicConfig.SdpBasicConfigEnforcement.ENABLED
					)
				));
		}

		Map<RaiFilterType, DetectionConfidenceLevel> raiFilters =
			modelArmorTemplateConfig.getRaiFilters();

		if (!raiFilters.isEmpty()) {
			RaiFilterSettings.Builder raiFilterSettingsBuilder =
				RaiFilterSettings.newBuilder();

			for (Map.Entry<RaiFilterType, DetectionConfidenceLevel> entry :
					raiFilters.entrySet()) {

				raiFilterSettingsBuilder.addRaiFilters(
					RaiFilterSettings.RaiFilter.newBuilder(
					).setFilterType(
						entry.getKey()
					).setConfidenceLevel(
						entry.getValue()
					));
			}

			builder.setRaiSettings(raiFilterSettingsBuilder);
		}

		return builder.build();
	}

	private static Template _buildTemplate(
		ModelArmorConfiguration modelArmorConfiguration,
		ModelArmorTemplateConfig modelArmorTemplateConfig) {

		return Template.newBuilder(
		).setFilterConfig(
			_buildFilterConfig(modelArmorTemplateConfig)
		).setTemplateMetadata(
			_buildTemplateMetadata(
				modelArmorConfiguration, modelArmorTemplateConfig)
		).build();
	}

	private static Template.TemplateMetadata _buildTemplateMetadata(
		ModelArmorConfiguration modelArmorConfiguration,
		ModelArmorTemplateConfig modelArmorTemplateConfig) {

		return Template.TemplateMetadata.newBuilder(
		).setLogSanitizeOperations(
			modelArmorConfiguration.logSanitizeOperationsEnabled()
		).setLogTemplateOperations(
			modelArmorConfiguration.logTemplateOperationsEnabled()
		).setMultiLanguageDetection(
			Template.TemplateMetadata.MultiLanguageDetection.newBuilder(
			).setEnableMultiLanguageDetection(
				modelArmorTemplateConfig.isMultiLanguageDetectionEnabled()
			)
		).build();
	}

	private static String _buildTemplatePath(long companyId, String templateId)
		throws ConfigurationException {

		ModelArmorConfiguration modelArmorConfiguration =
			ConfigurationProviderUtil.getCompanyConfiguration(
				ModelArmorConfiguration.class, companyId);

		VertexAIConfiguration vertexAIConfiguration =
			ConfigurationProviderUtil.getCompanyConfiguration(
				VertexAIConfiguration.class, companyId);

		return StringBundler.concat(
			"projects/", vertexAIConfiguration.projectId(), "/locations/",
			modelArmorConfiguration.location(), "/templates/", templateId);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ModelArmorTemplateUtil.class);

}