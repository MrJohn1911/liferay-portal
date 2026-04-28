/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.internal.configuration;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

/**
 * @author João Victor Alves
 */
@ExtendedObjectClassDefinition(
	category = "ai-hub", featureFlagKey = "LPD-62272",
	scope = ExtendedObjectClassDefinition.Scope.COMPANY
)
@Meta.OCD(
	id = "com.liferay.ai.hub.internal.configuration.ModelArmorConfiguration",
	localization = "content/Language", name = "model-armor-configuration-name"
)
public interface ModelArmorConfiguration {

	@Meta.AD(description = "model-armor-location-help", name = "location")
	public String location();

	@Meta.AD(
		deflt = "false",
		description = "model-armor-log-sanitize-operations-enabled-help",
		name = "log-sanitize-operations-enabled", required = false
	)
	public boolean logSanitizeOperationsEnabled();

	@Meta.AD(
		deflt = "false",
		description = "model-armor-log-template-operations-enabled-help",
		name = "log-template-operations-enabled", required = false
	)
	public boolean logTemplateOperationsEnabled();

	@Meta.AD(
		deflt = "8000", description = "model-armor-sanitize-rpc-timeout-help",
		name = "sanitize-rpc-timeout", required = false
	)
	public long sanitizeRPCTimeout();

}