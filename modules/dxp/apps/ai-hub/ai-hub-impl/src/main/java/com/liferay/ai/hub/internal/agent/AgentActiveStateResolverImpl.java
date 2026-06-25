/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.internal.agent;

import com.liferay.ai.hub.agent.AgentActiveStateResolver;
import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.object.rest.manager.v1_0.ObjectEntryManager;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.vulcan.dto.converter.DTOConverterContext;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Mylena Monte
 */
@Component(service = AgentActiveStateResolver.class)
public class AgentActiveStateResolverImpl implements AgentActiveStateResolver {

	@Override
	public Map<String, Boolean> getActiveByAgentExternalReferenceCode(
			long companyId, DTOConverterContext dtoConverterContext)
		throws Exception {

		if (dtoConverterContext == null) {
			return Collections.emptyMap();
		}

		Page<ObjectEntry> page = _objectEntryManager.getObjectEntries(
			companyId,
			_objectDefinitionLocalService.getObjectDefinition(
				companyId, "AIHubAgentDefinitionSetting"),
			null, null, dtoConverterContext, "name eq 'active'",
			Pagination.of(1, 10000), null, null);

		Map<String, Boolean> activeByAgentExternalReferenceCode =
			new HashMap<>();

		for (ObjectEntry objectEntry : page.getItems()) {
			activeByAgentExternalReferenceCode.put(
				GetterUtil.getString(
					objectEntry.getPropertyValue(
						"agentDefinitionExternalReferenceCode")),
				GetterUtil.getBoolean(objectEntry.getPropertyValue("value")));
		}

		return activeByAgentExternalReferenceCode;
	}

	@Override
	public boolean isActive(
			long companyId, DTOConverterContext dtoConverterContext,
			String agentExternalReferenceCode, boolean defaultActive)
		throws Exception {

		Map<String, Boolean> activeByAgentExternalReferenceCode =
			getActiveByAgentExternalReferenceCode(
				companyId, dtoConverterContext);

		return activeByAgentExternalReferenceCode.getOrDefault(
			agentExternalReferenceCode, defaultActive);
	}

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference(target = "(object.entry.manager.storage.type=default)")
	private ObjectEntryManager _objectEntryManager;

}