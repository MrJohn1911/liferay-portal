/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util;

import com.google.cloud.modelarmor.v1.DetectionConfidenceLevel;
import com.google.cloud.modelarmor.v1.RaiFilterType;

import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.CamelCaseUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author João Victor Alves
 */
public class ModelArmorTemplateConfigFactory {

	public static ModelArmorTemplateConfig get(
		com.liferay.object.model.ObjectEntry objectEntry) {

		return _getModelArmorTemplateConfig(
			objectEntry.getExternalReferenceCode(), objectEntry.getValues());
	}

	public static ModelArmorTemplateConfig get(ObjectEntry objectEntry) {
		return _getModelArmorTemplateConfig(
			objectEntry.getExternalReferenceCode(),
			objectEntry.getProperties());
	}

	private static Map<RaiFilterType, DetectionConfidenceLevel>
		_buildRaiFilters(Map<String, ?> values) {

		Map<RaiFilterType, DetectionConfidenceLevel> raiFilters = new EnumMap<>(
			RaiFilterType.class);

		_putRaiFilter(
			raiFilters, RaiFilterType.HATE_SPEECH,
			values.get("raiHateSpeechLevel"));
		_putRaiFilter(
			raiFilters, RaiFilterType.DANGEROUS,
			values.get("raiDangerousLevel"));
		_putRaiFilter(
			raiFilters, RaiFilterType.SEXUALLY_EXPLICIT,
			values.get("raiSexuallyExplicitLevel"));
		_putRaiFilter(
			raiFilters, RaiFilterType.HARASSMENT,
			values.get("raiHarassmentLevel"));

		return raiFilters;
	}

	private static ModelArmorTemplateConfig _getModelArmorTemplateConfig(
		String templateId, Map<String, ?> values) {

		return ModelArmorTemplateConfig.builder(
			templateId
		).guardrailType(
			_toGuardrailType(
				GetterUtil.getString(values.get("guardrailType"), "input"))
		).maliciousUriFilterEnabled(
			GetterUtil.getBoolean(values.get("maliciousUriFilterEnabled"))
		).multiLanguageDetectionEnabled(
			GetterUtil.getBoolean(values.get("multiLanguageDetectionEnabled"))
		).name(
			GetterUtil.getString(values.get("name"))
		).piAndJailbreakFilterEnabled(
			GetterUtil.getBoolean(values.get("piAndJailbreakFilterEnabled"))
		).piAndJailbreakConfidenceLevel(
			_toConfidenceLevel(
				GetterUtil.getString(
					values.get("piAndJailbreakConfidenceLevel"),
					"mediumAndAbove"))
		).sdpFilterEnabled(
			GetterUtil.getBoolean(values.get("sdpFilterEnabled"))
		).raiFilters(
			_buildRaiFilters(values)
		).build();
	}

	private static void _putRaiFilter(
		Map<RaiFilterType, DetectionConfidenceLevel> raiFilters,
		RaiFilterType raiFilterType, Object levelProperty) {

		String key = GetterUtil.getString(levelProperty, "none");

		if (Validator.isNull(key) || StringUtil.equalsIgnoreCase(key, "none")) {
			return;
		}

		DetectionConfidenceLevel confidenceLevel = _toConfidenceLevel(key);

		raiFilters.put(raiFilterType, confidenceLevel);
	}

	private static DetectionConfidenceLevel _toConfidenceLevel(String key) {
		try {
			return DetectionConfidenceLevel.valueOf(_toEnumKey(key));
		}
		catch (IllegalArgumentException illegalArgumentException) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Unknown confidence level " + key,
					illegalArgumentException);
			}

			return DetectionConfidenceLevel.UNRECOGNIZED;
		}
	}

	private static String _toEnumKey(String key) {
		if (Validator.isNull(key)) {
			return StringPool.BLANK;
		}

		return StringUtil.toUpperCase(
			CamelCaseUtil.fromCamelCase(key, CharPool.UNDERLINE));
	}

	private static ModelArmorTemplateConfig.GuardrailType _toGuardrailType(
		String key) {

		try {
			return ModelArmorTemplateConfig.GuardrailType.valueOf(
				_toEnumKey(key));
		}
		catch (IllegalArgumentException illegalArgumentException) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Unknown guardrail type " + key, illegalArgumentException);
			}

			return ModelArmorTemplateConfig.GuardrailType.INPUT;
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ModelArmorTemplateConfigFactory.class);

}