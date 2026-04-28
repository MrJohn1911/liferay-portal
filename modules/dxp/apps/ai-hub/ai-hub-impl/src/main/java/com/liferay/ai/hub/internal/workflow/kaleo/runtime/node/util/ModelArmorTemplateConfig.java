/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util;

import com.google.cloud.modelarmor.v1.DetectionConfidenceLevel;
import com.google.cloud.modelarmor.v1.RaiFilterType;

import java.util.Collections;
import java.util.Map;

/**
 * @author João Victor Alves
 */
public class ModelArmorTemplateConfig {

	public static Builder builder(String templateId) {
		return new Builder(templateId);
	}

	public ModelArmorTemplateConfig(Builder builder) {
		_guardrailType = builder._guardrailType;
		_maliciousUriFilterEnabled = builder._maliciousUriFilterEnabled;
		_multiLanguageDetectionEnabled = builder._multiLanguageDetectionEnabled;
		_name = builder._name;
		_piAndJailbreakConfidenceLevel = builder._piAndJailbreakConfidenceLevel;
		_piAndJailbreakFilterEnabled = builder._piAndJailbreakFilterEnabled;
		_raiFilters = Map.copyOf(builder._raiFilters);
		_sdpFilterEnabled = builder._sdpFilterEnabled;
		_templateId = builder._templateId;
	}

	public GuardrailType getGuardrailType() {
		return _guardrailType;
	}

	public String getName() {
		return _name;
	}

	public DetectionConfidenceLevel getPiAndJailbreakConfidenceLevel() {
		return _piAndJailbreakConfidenceLevel;
	}

	public Map<RaiFilterType, DetectionConfidenceLevel> getRaiFilters() {
		return _raiFilters;
	}

	public String getTemplateId() {
		return _templateId;
	}

	public boolean isMaliciousUriFilterEnabled() {
		return _maliciousUriFilterEnabled;
	}

	public boolean isMultiLanguageDetectionEnabled() {
		return _multiLanguageDetectionEnabled;
	}

	public boolean isPiAndJailbreakFilterEnabled() {
		return _piAndJailbreakFilterEnabled;
	}

	public boolean isSdpFilterEnabled() {
		return _sdpFilterEnabled;
	}

	public static class Builder {

		public ModelArmorTemplateConfig build() {
			return new ModelArmorTemplateConfig(this);
		}

		public Builder guardrailType(GuardrailType guardrailType) {
			_guardrailType = guardrailType;

			return this;
		}

		public Builder maliciousUriFilterEnabled(
			boolean maliciousUriFilterEnabled) {

			_maliciousUriFilterEnabled = maliciousUriFilterEnabled;

			return this;
		}

		public Builder multiLanguageDetectionEnabled(
			boolean multiLanguageDetectionEnabled) {

			_multiLanguageDetectionEnabled = multiLanguageDetectionEnabled;

			return this;
		}

		public Builder name(String name) {
			_name = name;

			return this;
		}

		public Builder piAndJailbreakConfidenceLevel(
			DetectionConfidenceLevel piAndJailbreakConfidenceLevel) {

			_piAndJailbreakConfidenceLevel = piAndJailbreakConfidenceLevel;

			return this;
		}

		public Builder piAndJailbreakFilterEnabled(
			boolean piAndJailbreakFilterEnabled) {

			_piAndJailbreakFilterEnabled = piAndJailbreakFilterEnabled;

			return this;
		}

		public Builder raiFilters(
			Map<RaiFilterType, DetectionConfidenceLevel> raiFilters) {

			_raiFilters = raiFilters;

			return this;
		}

		public Builder sdpFilterEnabled(boolean sdpFilterEnabled) {
			_sdpFilterEnabled = sdpFilterEnabled;

			return this;
		}

		private Builder(String templateId) {
			_templateId = templateId;
		}

		private GuardrailType _guardrailType = GuardrailType.INPUT;
		private boolean _maliciousUriFilterEnabled;
		private boolean _multiLanguageDetectionEnabled;
		private String _name;
		private DetectionConfidenceLevel _piAndJailbreakConfidenceLevel =
			DetectionConfidenceLevel.MEDIUM_AND_ABOVE;
		private boolean _piAndJailbreakFilterEnabled;
		private Map<RaiFilterType, DetectionConfidenceLevel> _raiFilters =
			Collections.emptyMap();
		private boolean _sdpFilterEnabled;
		private final String _templateId;

	}

	public enum GuardrailType {

		INPUT, OUTPUT

	}

	private final GuardrailType _guardrailType;
	private final boolean _maliciousUriFilterEnabled;
	private final boolean _multiLanguageDetectionEnabled;
	private final String _name;
	private final DetectionConfidenceLevel _piAndJailbreakConfidenceLevel;
	private final boolean _piAndJailbreakFilterEnabled;
	private final Map<RaiFilterType, DetectionConfidenceLevel> _raiFilters;
	private final boolean _sdpFilterEnabled;
	private final String _templateId;

}