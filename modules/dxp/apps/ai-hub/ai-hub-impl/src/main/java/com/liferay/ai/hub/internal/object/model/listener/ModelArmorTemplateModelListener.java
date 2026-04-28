/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.internal.object.model.listener;

import com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util.ModelArmorClientUtil;
import com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util.ModelArmorTemplateConfig;
import com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util.ModelArmorTemplateConfigFactory;
import com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util.ModelArmorTemplateUtil;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;

import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author João Victor Alves
 */
@Component(service = ModelListener.class)
public class ModelArmorTemplateModelListener
	extends BaseModelListener<ObjectEntry> {

	@Override
	public void onAfterCreate(ObjectEntry objectEntry)
		throws ModelListenerException {

		if (!_isModelArmorTemplate(objectEntry)) {
			return;
		}

		ModelArmorTemplateConfig modelArmorTemplateConfig =
			ModelArmorTemplateConfigFactory.get(objectEntry);

		try {
			ModelArmorTemplateUtil.createTemplate(
				objectEntry.getCompanyId(),
				ModelArmorClientUtil.getModelArmorClient(
					objectEntry.getCompanyId()),
				modelArmorTemplateConfig);
		}
		catch (Exception exception) {
			try {
				_objectEntryLocalService.deleteObjectEntry(
					objectEntry.getObjectEntryId());
			}
			catch (PortalException portalException) {
				throw new ModelListenerException(
					"Unable to delete object entry " +
						objectEntry.getObjectEntryId(),
					portalException);
			}

			throw new ModelListenerException(
				"Unable to create Model Armor template " +
					modelArmorTemplateConfig.getName(),
				exception);
		}
	}

	@Override
	public void onAfterUpdate(
			ObjectEntry originalObjectEntry, ObjectEntry objectEntry)
		throws ModelListenerException {

		if (!_isModelArmorTemplate(objectEntry)) {
			return;
		}

		ModelArmorTemplateConfig modelArmorTemplateConfig =
			ModelArmorTemplateConfigFactory.get(objectEntry);

		try {
			ModelArmorTemplateUtil.updateTemplate(
				objectEntry.getCompanyId(),
				ModelArmorClientUtil.getModelArmorClient(
					objectEntry.getCompanyId()),
				modelArmorTemplateConfig);
		}
		catch (Exception exception) {
			throw new ModelListenerException(
				"Unable to update Model Armor template " +
					modelArmorTemplateConfig.getName(),
				exception);
		}
	}

	@Override
	public void onBeforeRemove(ObjectEntry objectEntry)
		throws ModelListenerException {

		if (!_isModelArmorTemplate(objectEntry)) {
			return;
		}

		String templateId = objectEntry.getExternalReferenceCode();

		try {
			ModelArmorTemplateUtil.deleteTemplate(
				objectEntry.getCompanyId(),
				ModelArmorClientUtil.getModelArmorClient(
					objectEntry.getCompanyId()),
				templateId);
		}
		catch (Exception exception) {
			throw new ModelListenerException(
				"Unable to delete Model Armor template " + templateId,
				exception);
		}
	}

	private boolean _isModelArmorTemplate(ObjectEntry objectEntry) {
		ObjectDefinition objectDefinition =
			_objectDefinitionLocalService.fetchObjectDefinition(
				objectEntry.getObjectDefinitionId());

		if (objectDefinition == null) {
			return false;
		}

		return Objects.equals(
			objectDefinition.getExternalReferenceCode(),
			"L_AI_HUB_MODEL_ARMOR_TEMPLATE");
	}

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference
	private ObjectEntryLocalService _objectEntryLocalService;

}