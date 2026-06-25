/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.rest.internal.manager.v1_0;

import com.liferay.account.model.AccountEntry;
import com.liferay.ai.hub.agent.AgentActiveStateResolver;
import com.liferay.ai.hub.configuration.VertexAIConfiguration;
import com.liferay.ai.hub.rest.dto.v1_0.AgentDefinition;
import com.liferay.ai.hub.rest.dto.v1_0.Model;
import com.liferay.ai.hub.rest.dto.v1_0.Status;
import com.liferay.ai.hub.rest.dto.v1_0.Variable;
import com.liferay.ai.hub.rest.internal.resource.v1_0.AgentDefinitionResourceImpl;
import com.liferay.ai.hub.rest.manager.v1_0.AgentDefinitionManager;
import com.liferay.ai.hub.util.AccountEntryUtil;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectRelationship;
import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.object.rest.manager.v1_0.DefaultObjectEntryManager;
import com.liferay.object.rest.manager.v1_0.ObjectEntryManager;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectRelationshipLocalService;
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.module.configuration.ConfigurationException;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.resource.ModelResourcePermission;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.workflow.WorkflowDefinition;
import com.liferay.portal.vulcan.dto.converter.DTOConverterContext;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;
import com.liferay.portal.vulcan.util.ActionUtil;
import com.liferay.portal.workflow.constants.WorkflowDefinitionConstants;
import com.liferay.portal.workflow.kaleo.model.KaleoDefinition;
import com.liferay.portal.workflow.manager.WorkflowDefinitionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Feliphe Marinho
 * @author João Victor Alves
 */
@Component(service = AgentDefinitionManager.class)
public class AgentDefinitionManagerImpl implements AgentDefinitionManager {

	@Override
	public void deleteAgentDefinition(
			long companyId, DTOConverterContext dtoConverterContext,
			String externalReferenceCode)
		throws Exception {

		ObjectEntry objectEntry = _objectEntryManager.getObjectEntry(
			companyId, dtoConverterContext, externalReferenceCode,
			_getObjectDefinition(companyId), null);

		_objectEntryManager.deleteObjectEntry(
			companyId, dtoConverterContext,
			objectEntry.getExternalReferenceCode(),
			_getObjectDefinition(companyId), null);

		WorkflowDefinition workflowDefinition =
			_workflowDefinitionManager.getLatestWorkflowDefinition(
				companyId,
				GetterUtil.getString(
					objectEntry.getPropertyValue("workflowDefinitionName")));

		_workflowDefinitionManager.updateActive(
			false, workflowDefinition.getCompanyId(),
			workflowDefinition.getName(), dtoConverterContext.getUserId(),
			workflowDefinition.getVersion());

		_workflowDefinitionManager.undeployWorkflowDefinition(
			workflowDefinition.getCompanyId(), workflowDefinition.getName(),
			dtoConverterContext.getUserId(), workflowDefinition.getVersion());

		_deleteAgentDefinitionSettings(
			companyId, dtoConverterContext, externalReferenceCode);
	}

	@Override
	public AgentDefinition getAgentDefinition(
			long companyId, DTOConverterContext dtoConverterContext,
			String externalReferenceCode)
		throws Exception {

		ObjectEntry objectEntry = _objectEntryManager.getObjectEntry(
			companyId, dtoConverterContext, externalReferenceCode,
			_getObjectDefinition(companyId), null);

		return _toAgentDefinition(
			companyId, dtoConverterContext,
			_agentActiveStateResolver.isActive(
				companyId, dtoConverterContext, externalReferenceCode,
				GetterUtil.getBoolean(objectEntry.getPropertyValue("active"))),
			objectEntry);
	}

	@Override
	public Page<AgentDefinition> getAgentDefinitionsPage(
			long companyId, DTOConverterContext dtoConverterContext,
			String filterString, Pagination pagination, String search,
			Sort[] sorts)
		throws Exception {

		Map<String, Map<String, String>> actions = null;

		if (dtoConverterContext != null) {
			actions = HashMapBuilder.<String, Map<String, String>>put(
				"get",
				ActionUtil.addAction(
					ActionKeys.VIEW, AgentDefinitionResourceImpl.class, null,
					"getAgentDefinitionsPage",
					_kaleoDefinitionModelResourcePermission, (Long)null,
					dtoConverterContext.getUriInfo())
			).build();
		}

		Map<String, Boolean> activeByAgentExternalReferenceCode =
			_agentActiveStateResolver.getActiveByAgentExternalReferenceCode(
				companyId, dtoConverterContext);

		Boolean active = _getActiveFilter(filterString);

		if (active == null) {
			Page<ObjectEntry> objectEntriesPage =
				_objectEntryManager.getObjectEntries(
					companyId, _getObjectDefinition(companyId), null, null,
					dtoConverterContext, _getFilterString(filterString),
					pagination, search, sorts);

			return Page.of(
				actions,
				TransformUtil.transform(
					objectEntriesPage.getItems(),
					objectEntry -> _toAgentDefinition(
						companyId, dtoConverterContext,
						_isActive(
							activeByAgentExternalReferenceCode, objectEntry),
						objectEntry)),
				pagination, objectEntriesPage.getTotalCount());
		}

		Page<ObjectEntry> objectEntriesPage =
			_objectEntryManager.getObjectEntries(
				companyId, _getObjectDefinition(companyId), null, null,
				dtoConverterContext, _getFilterString(null),
				Pagination.of(1, 10000), search, sorts);

		List<AgentDefinition> agentDefinitions = new ArrayList<>();

		for (ObjectEntry objectEntry : objectEntriesPage.getItems()) {
			boolean agentActive = _isActive(
				activeByAgentExternalReferenceCode, objectEntry);

			if (agentActive == active) {
				agentDefinitions.add(
					_toAgentDefinition(
						companyId, dtoConverterContext, agentActive,
						objectEntry));
			}
		}

		return Page.of(
			actions,
			ListUtil.subList(
				agentDefinitions, pagination.getStartPosition(),
				pagination.getEndPosition()),
			pagination, agentDefinitions.size());
	}

	@Override
	public AgentDefinition patchAgentDefinitionUpdateActive(
			boolean active, long companyId,
			DTOConverterContext dtoConverterContext,
			String externalReferenceCode)
		throws Exception {

		ObjectEntry objectEntry = _objectEntryManager.getObjectEntry(
			companyId, dtoConverterContext, externalReferenceCode,
			_getObjectDefinition(companyId), null);

		_updateAgentDefinitionSettingActive(
			active, companyId, dtoConverterContext, externalReferenceCode);

		return _toAgentDefinition(
			companyId, dtoConverterContext, active, objectEntry);
	}

	@Override
	public AgentDefinition postAgentDefinitionCopy(
			long companyId, DTOConverterContext dtoConverterContext,
			String externalReferenceCode)
		throws Exception {

		AccountEntry accountEntry = AccountEntryUtil.getUserAccountEntry(
			dtoConverterContext.getUserId());

		ObjectDefinition objectDefinition = _getObjectDefinition(companyId);

		ObjectEntry objectEntry = _objectEntryManager.getObjectEntry(
			companyId, dtoConverterContext, externalReferenceCode,
			objectDefinition, null);

		WorkflowDefinition workflowDefinition =
			_workflowDefinitionManager.getLatestWorkflowDefinition(
				companyId,
				GetterUtil.getString(
					objectEntry.getPropertyValue("workflowDefinitionName")));

		String content = workflowDefinition.getContent();

		Locale locale = dtoConverterContext.getLocale();

		String workflowDefinitionName = PortalUUIDUtil.generate();

		_workflowDefinitionManager.deployWorkflowDefinition(
			content.getBytes(), companyId, null,
			accountEntry.getAccountEntryGroupId(), workflowDefinitionName,
			WorkflowDefinitionConstants.SCOPE_AI,
			LanguageUtil.format(
				locale, "copy-of-x",
				workflowDefinition.getTitle(locale.getDisplayLanguage())),
			dtoConverterContext.getUserId());

		Map<String, String> title =
			(Map<String, String>)objectEntry.getPropertyValue("title_i18n");

		title.replaceAll(
			(key, value) -> LanguageUtil.format(locale, "copy-of-x", value));

		ObjectEntry copiedObjectEntry = _objectEntryManager.addObjectEntry(
			dtoConverterContext, _getObjectDefinition(companyId),
			new ObjectEntry() {
				{
					setProperties(
						() -> Map.of(
							"active",
							GetterUtil.getBoolean(
								objectEntry.getPropertyValue("active")),
							"description",
							GetterUtil.getString(
								objectEntry.getPropertyValue("description")),
							"inputVariables",
							GetterUtil.getString(
								objectEntry.getPropertyValue("inputVariables")),
							"outputVariable",
							GetterUtil.getString(
								objectEntry.getPropertyValue("outputVariable")),
							"r_accountToAIHubAgentDefinitions_accountEntryId",
							accountEntry.getAccountEntryId(), "title_i18n",
							title, "workflowDefinitionName",
							workflowDefinitionName));
				}
			},
			null);

		ObjectRelationship objectRelationship =
			_objectRelationshipLocalService.getObjectRelationship(
				objectDefinition.getObjectDefinitionId(),
				"agentDefinitionsToContentRetrievers");

		DefaultObjectEntryManager defaultObjectEntryManager =
			(DefaultObjectEntryManager)_objectEntryManager;

		Page<ObjectEntry> objectEntriesPage =
			defaultObjectEntryManager.getRelatedObjectEntries(
				dtoConverterContext, objectEntry.getId(), objectRelationship,
				Pagination.of(QueryUtil.ALL_POS, QueryUtil.ALL_POS));

		if (objectEntriesPage.getTotalCount() != 0) {
			for (ObjectEntry contentRetrieverObjectEntry :
					objectEntriesPage.getItems()) {

				defaultObjectEntryManager.
					addObjectRelationshipMappingTableValues(
						dtoConverterContext, objectRelationship,
						copiedObjectEntry.getId(),
						contentRetrieverObjectEntry.getId());
			}
		}

		return _toAgentDefinition(
			companyId, dtoConverterContext,
			GetterUtil.getBoolean(copiedObjectEntry.getPropertyValue("active")),
			copiedObjectEntry);
	}

	private Map<String, String> _addAction(
		DTOConverterContext dtoConverterContext, String methodName,
		WorkflowDefinition workflowDefinition) {

		if (Objects.equals(
				methodName, "postAgentDefinitionByExternalReferenceCodeCopy") ||
			(workflowDefinition.isSystem() &&
			 Objects.equals(
				 methodName,
				 "patchAgentDefinitionByExternalReferenceCodeUpdateActive"))) {

			return ActionUtil.addAction(
				ActionKeys.VIEW, AgentDefinitionResourceImpl.class,
				workflowDefinition.getWorkflowDefinitionId(), methodName,
				_kaleoDefinitionModelResourcePermission, (Long)null,
				dtoConverterContext.getUriInfo());
		}

		return ActionUtil.addAction(
			ActionKeys.ADD_DEFINITION, AgentDefinitionResourceImpl.class,
			workflowDefinition.getWorkflowDefinitionId(), methodName,
			_kaleoDefinitionModelResourcePermission, (Long)null,
			dtoConverterContext.getUriInfo());
	}

	private void _deleteAgentDefinitionSettings(
			long companyId, DTOConverterContext dtoConverterContext,
			String agentExternalReferenceCode)
		throws Exception {

		ObjectDefinition objectDefinition =
			_getAgentDefinitionSettingObjectDefinition(companyId);

		Page<ObjectEntry> page = _objectEntryManager.getObjectEntries(
			companyId, objectDefinition, null, null, dtoConverterContext,
			"agentDefinitionExternalReferenceCode eq '" +
				agentExternalReferenceCode + "'",
			Pagination.of(1, 10000), null, null);

		for (ObjectEntry objectEntry : page.getItems()) {
			_objectEntryManager.deleteObjectEntry(
				companyId, dtoConverterContext,
				objectEntry.getExternalReferenceCode(), objectDefinition, null);
		}
	}

	private Boolean _getActiveFilter(String filterString) {
		if (Validator.isNull(filterString)) {
			return null;
		}

		Matcher matcher = _activeFilterPattern.matcher(filterString.trim());

		if (!matcher.matches()) {
			return null;
		}

		return GetterUtil.getBoolean(matcher.group(1));
	}

	private ObjectDefinition _getAgentDefinitionSettingObjectDefinition(
			long companyId)
		throws Exception {

		return _objectDefinitionLocalService.getObjectDefinition(
			companyId, "AIHubAgentDefinitionSetting");
	}

	private String _getFilterString(String filterString) {
		if (Validator.isNull(filterString)) {
			return "externalReferenceCode ne 'L_PAGE_BUILDER'";
		}

		return "(" + filterString +
			") and externalReferenceCode ne 'L_PAGE_BUILDER'";
	}

	private ObjectDefinition _getObjectDefinition(long companyId)
		throws Exception {

		return _objectDefinitionLocalService.getObjectDefinition(
			companyId, "AIHubAgentDefinition");
	}

	private Status _getStatus(
		boolean active, DTOConverterContext dtoConverterContext) {

		if (dtoConverterContext == null) {
			return null;
		}

		Locale locale = dtoConverterContext.getLocale();

		if (active) {
			return _toStatus("active", locale);
		}

		return _toStatus("inactive", locale);
	}

	private boolean _isActive(
		Map<String, Boolean> activeByAgentExternalReferenceCode,
		ObjectEntry objectEntry) {

		return activeByAgentExternalReferenceCode.getOrDefault(
			GetterUtil.getString(
				objectEntry.getPropertyValue("externalReferenceCode")),
			GetterUtil.getBoolean(objectEntry.getPropertyValue("active")));
	}

	private AgentDefinition _toAgentDefinition(
			long companyId, DTOConverterContext dtoConverterContext,
			boolean agentActive, ObjectEntry objectEntry)
		throws PortalException {

		WorkflowDefinition workflowDefinition =
			_workflowDefinitionManager.getLatestWorkflowDefinition(
				companyId,
				GetterUtil.getString(
					objectEntry.getPropertyValue("workflowDefinitionName")));

		return new AgentDefinition() {
			{
				setActions(
					() -> {
						if (dtoConverterContext == null) {
							return null;
						}

						return HashMapBuilder.put(
							"activate",
							() -> {
								if (agentActive) {
									return null;
								}

								return _addAction(
									dtoConverterContext,
									"patchAgentDefinitionByExternalReference" +
										"CodeUpdateActive",
									workflowDefinition);
							}
						).put(
							"copy",
							_addAction(
								dtoConverterContext,
								"postAgentDefinitionByExternalReferenceCode" +
									"Copy",
								workflowDefinition)
						).put(
							"deactivate",
							() -> {
								if (!agentActive) {
									return null;
								}

								return _addAction(
									dtoConverterContext,
									"patchAgentDefinitionByExternalReference" +
										"CodeUpdateActive",
									workflowDefinition);
							}
						).put(
							"delete",
							() -> _addAction(
								dtoConverterContext,
								"deleteAgentDefinitionByExternalReferenceCode",
								workflowDefinition)
						).put(
							"permissions",
							() -> {
								Map<String, Map<String, String>> actions =
									objectEntry.getActions();

								if (MapUtil.isEmpty(actions)) {
									return null;
								}

								return actions.get("permissions");
							}
						).build();
					});
				setActive(() -> agentActive);
				setDescription(
					() -> GetterUtil.getString(
						objectEntry.getPropertyValue("description")));
				setExternalReferenceCode(
					() -> GetterUtil.getString(
						objectEntry.getPropertyValue("externalReferenceCode")));
				setId(
					() -> GetterUtil.getLong(
						objectEntry.getPropertyValue("id")));
				setInputVariables(
					() -> TransformUtil.transform(
						StringUtil.split(
							GetterUtil.getString(
								objectEntry.getPropertyValue(
									"inputVariables"))),
						inputVariable -> _toVariable(inputVariable),
						Variable.class));
				setModel(() -> _toModel(dtoConverterContext, companyId));
				setOutputVariable(
					() -> _toVariable(
						GetterUtil.getString(
							objectEntry.getPropertyValue("outputVariable"))));
				setStatus(() -> _getStatus(agentActive, dtoConverterContext));
				setSystem(
					() -> GetterUtil.getBoolean(
						objectEntry.getPropertyValue("system")));
				setTitle(
					() -> GetterUtil.getString(
						objectEntry.getPropertyValue("title")));
				setVersion(workflowDefinition::getVersion);
				setWorkflowDefinitionName(workflowDefinition::getName);
			}
		};
	}

	private Model _toModel(
			DTOConverterContext dtoConverterContext, long companyId)
		throws ConfigurationException {

		VertexAIConfiguration vertexAIConfiguration =
			_configurationProvider.getCompanyConfiguration(
				VertexAIConfiguration.class, companyId);

		return new Model() {
			{
				setLabel(
					() -> _language.get(
						dtoConverterContext.getLocale(),
						vertexAIConfiguration.modelName()));
				setName(vertexAIConfiguration::modelName);
				setProviderLabel(
					() -> _language.get(
						dtoConverterContext.getLocale(), "google"));
			}
		};
	}

	private Status _toStatus(String label, Locale locale) {
		Status status = new Status();

		status.setLabel(() -> label);
		status.setLabel_i18n(() -> _language.get(locale, label));

		return status;
	}

	private Variable _toVariable(String variableName) {
		return new Variable() {
			{
				setName(() -> variableName);
				setType(() -> "string");
			}
		};
	}

	private void _updateAgentDefinitionSettingActive(
			boolean active, long companyId,
			DTOConverterContext dtoConverterContext,
			String agentExternalReferenceCode)
		throws Exception {

		AccountEntry accountEntry = AccountEntryUtil.getUserAccountEntry(
			dtoConverterContext.getUserId());

		if (accountEntry == null) {
			return;
		}

		ObjectDefinition objectDefinition =
			_getAgentDefinitionSettingObjectDefinition(companyId);

		Page<ObjectEntry> page = _objectEntryManager.getObjectEntries(
			companyId, objectDefinition, null, null, dtoConverterContext,
			"agentDefinitionExternalReferenceCode eq '" +
				agentExternalReferenceCode + "' and name eq 'active'",
			Pagination.of(1, 1), null, null);

		for (ObjectEntry objectEntry : page.getItems()) {
			_objectEntryManager.partialUpdateObjectEntry(
				companyId, dtoConverterContext,
				objectEntry.getExternalReferenceCode(), objectDefinition,
				new ObjectEntry() {
					{
						setProperties(
							() -> Map.of("value", String.valueOf(active)));
					}
				},
				null);

			return;
		}

		_objectEntryManager.addObjectEntry(
			dtoConverterContext, objectDefinition,
			new ObjectEntry() {
				{
					setProperties(
						() -> Map.of(
							"agentDefinitionExternalReferenceCode",
							agentExternalReferenceCode, "name", "active",
							"r_accountToAIHubAgentDefinitionSettings_" +
								"accountEntryId",
							accountEntry.getAccountEntryId(), "value",
							String.valueOf(active)));
				}
			},
			null);
	}

	private static final Pattern _activeFilterPattern = Pattern.compile(
		"\\(?\\s*active\\s+eq\\s+'?(true|false)'?\\s*\\)?",
		Pattern.CASE_INSENSITIVE);

	@Reference
	private AgentActiveStateResolver _agentActiveStateResolver;

	@Reference
	private ConfigurationProvider _configurationProvider;

	@Reference(
		target = "(model.class.name=com.liferay.portal.workflow.kaleo.model.KaleoDefinition)"
	)
	private ModelResourcePermission<KaleoDefinition>
		_kaleoDefinitionModelResourcePermission;

	@Reference
	private Language _language;

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference(target = "(object.entry.manager.storage.type=default)")
	private ObjectEntryManager _objectEntryManager;

	@Reference
	private ObjectRelationshipLocalService _objectRelationshipLocalService;

	@Reference
	private WorkflowDefinitionManager _workflowDefinitionManager;

}