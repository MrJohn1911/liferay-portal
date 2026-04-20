/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util;

import com.google.cloud.modelarmor.v1.DetectionConfidenceLevel;
import com.google.cloud.modelarmor.v1.FilterConfig;
import com.google.cloud.modelarmor.v1.LocationName;
import com.google.cloud.modelarmor.v1.MaliciousUriFilterSettings;
import com.google.cloud.modelarmor.v1.ModelArmorClient;
import com.google.cloud.modelarmor.v1.PiAndJailbreakFilterSettings;
import com.google.cloud.modelarmor.v1.RaiFilterSettings;
import com.google.cloud.modelarmor.v1.RaiFilterType;
import com.google.cloud.modelarmor.v1.SdpBasicConfig;
import com.google.cloud.modelarmor.v1.SdpFilterSettings;
import com.google.cloud.modelarmor.v1.Template;

import com.liferay.portal.kernel.util.Validator;

/**
 * @author João Victor Alves
 */
public class ModelArmorTemplateUtil {

	public static Template getOrCreate(
		ModelArmorClient modelArmorClient, String templateName) {

		if (Validator.isNull(templateName)) {
			return null;
		}

		Template template = modelArmorClient.getTemplate(
			"projects/ai-hub-liferay/locations/europe-southwest1/templates/" +
				templateName);

		if (template != null) {
			return template;
		}

		FilterConfig filterConfig = FilterConfig.newBuilder(
		).setMaliciousUriFilterSettings(
			MaliciousUriFilterSettings.newBuilder(
			).setFilterEnforcement(
				MaliciousUriFilterSettings.MaliciousUriFilterEnforcement.ENABLED
			)
		).setPiAndJailbreakFilterSettings(
			PiAndJailbreakFilterSettings.newBuilder(
			).setFilterEnforcement(
				PiAndJailbreakFilterSettings.PiAndJailbreakFilterEnforcement.
					ENABLED
			).setConfidenceLevel(
				DetectionConfidenceLevel.MEDIUM_AND_ABOVE
			)
		).setSdpSettings(
			SdpFilterSettings.newBuilder(
			).setBasicConfig(
				SdpBasicConfig.newBuilder(
				).setFilterEnforcement(
					SdpBasicConfig.SdpBasicConfigEnforcement.ENABLED
				)
			)
		).setRaiSettings(
			RaiFilterSettings.newBuilder(
			).addRaiFilters(
				RaiFilterSettings.RaiFilter.newBuilder(
				).setFilterType(
					RaiFilterType.HATE_SPEECH
				).setConfidenceLevel(
					DetectionConfidenceLevel.HIGH
				)
			).addRaiFilters(
				RaiFilterSettings.RaiFilter.newBuilder(
				).setFilterType(
					RaiFilterType.DANGEROUS
				).setConfidenceLevel(
					DetectionConfidenceLevel.HIGH
				)
			).addRaiFilters(
				RaiFilterSettings.RaiFilter.newBuilder(
				).setFilterType(
					RaiFilterType.SEXUALLY_EXPLICIT
				).setConfidenceLevel(
					DetectionConfidenceLevel.HIGH
				)
			).addRaiFilters(
				RaiFilterSettings.RaiFilter.newBuilder(
				).setFilterType(
					RaiFilterType.HARASSMENT
				).setConfidenceLevel(
					DetectionConfidenceLevel.HIGH
				)
			)
		).build();

		Template.TemplateMetadata metadata =
			Template.TemplateMetadata.newBuilder(
			).setMultiLanguageDetection(
				Template.TemplateMetadata.MultiLanguageDetection.newBuilder(
				).setEnableMultiLanguageDetection(
					true
				)
			).setLogTemplateOperations(
				true
			).setLogSanitizeOperations(
				true
			).build();

		template = Template.newBuilder(
		).setFilterConfig(
			filterConfig
		).setTemplateMetadata(
			metadata
		).putLabels(
			"env", "dev"
		).build();

		return modelArmorClient.createTemplate(
			LocationName.of("ai-hub-liferay", "europe-southwest1"), template,
			templateName);
	}

}