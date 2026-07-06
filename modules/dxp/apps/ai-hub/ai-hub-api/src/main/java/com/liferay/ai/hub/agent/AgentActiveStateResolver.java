/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.agent;

import com.liferay.portal.vulcan.dto.converter.DTOConverterContext;

import java.util.Map;

/**
 * @author Mylena Monte
 */
public interface AgentActiveStateResolver {

	public Map<String, Boolean> getActiveByAgentExternalReferenceCode(
			long companyId, DTOConverterContext dtoConverterContext)
		throws Exception;

	public boolean isActive(
			long companyId, DTOConverterContext dtoConverterContext,
			String agentExternalReferenceCode, boolean defaultActive)
		throws Exception;

}