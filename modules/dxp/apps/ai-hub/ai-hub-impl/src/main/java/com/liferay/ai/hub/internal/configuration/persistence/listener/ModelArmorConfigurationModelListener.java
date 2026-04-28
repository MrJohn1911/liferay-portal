/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.internal.configuration.persistence.listener;

import com.liferay.ai.hub.internal.configuration.ModelArmorConfiguration;
import com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util.ModelArmorClientUtil;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.portal.configuration.module.configuration.ConfigurationProviderUtil;
import com.liferay.portal.configuration.persistence.listener.ConfigurationModelListener;
import com.liferay.portal.configuration.persistence.listener.ConfigurationModelListenerException;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.module.configuration.ConfigurationException;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LocaleThreadLocal;
import com.liferay.portal.kernel.util.Validator;

import java.util.Dictionary;
import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author João Victor Alves
 */
@Component(
	property = "model.class.name=com.liferay.ai.hub.internal.configuration.ModelArmorConfiguration",
	service = ConfigurationModelListener.class
)
public class ModelArmorConfigurationModelListener
	implements ConfigurationModelListener {

	@Override
	public void onAfterSave(String pid, Dictionary<String, Object> properties) {
		long companyId = GetterUtil.getLong(
			properties.get("companyId"), CompanyConstants.SYSTEM);

		if (companyId == CompanyConstants.SYSTEM) {
			return;
		}

		ModelArmorClientUtil.invalidate(companyId);
	}

	@Override
	public void onBeforeSave(String pid, Dictionary<String, Object> properties)
		throws ConfigurationModelListenerException {

		long companyId = GetterUtil.getLong(
			properties.get("companyId"), CompanyConstants.SYSTEM);

		if (companyId == CompanyConstants.SYSTEM) {
			return;
		}

		String currentLocation = null;

		try {
			currentLocation = _getModelArmorCurrentLocation(companyId);
		}
		catch (ConfigurationException configurationException) {
			throw new ConfigurationModelListenerException(
				configurationException.getMessage(),
				ModelArmorConfiguration.class,
				ModelArmorConfigurationModelListener.class, properties);
		}

		if (Validator.isNull(currentLocation)) {
			return;
		}

		String newLocation = GetterUtil.getString(properties.get("location"));

		if (Objects.equals(currentLocation, newLocation)) {
			return;
		}

		ObjectDefinition objectDefinition =
			_objectDefinitionLocalService.
				fetchObjectDefinitionByExternalReferenceCode(
					"L_AI_HUB_MODEL_ARMOR_TEMPLATE", companyId);

		if (objectDefinition == null) {
			return;
		}

		long objectEntriesCount =
			_objectEntryLocalService.getObjectEntriesCount(
				objectDefinition.getObjectDefinitionId());

		if (objectEntriesCount > 0) {
			throw new ConfigurationModelListenerException(
				_language.get(
					LocaleThreadLocal.getSiteDefaultLocale(),
					"unable-to-change-the-model-armor-location-while-" +
						"templates-exist"),
				ModelArmorConfiguration.class, getClass(), properties);
		}
	}

	private String _getModelArmorCurrentLocation(long companyId)
		throws ConfigurationException {

		ModelArmorConfiguration modelArmorConfiguration =
			ConfigurationProviderUtil.getCompanyConfiguration(
				ModelArmorConfiguration.class, companyId);

		return modelArmorConfiguration.location();
	}

	@Reference
	private Language _language;

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference
	private ObjectEntryLocalService _objectEntryLocalService;

}