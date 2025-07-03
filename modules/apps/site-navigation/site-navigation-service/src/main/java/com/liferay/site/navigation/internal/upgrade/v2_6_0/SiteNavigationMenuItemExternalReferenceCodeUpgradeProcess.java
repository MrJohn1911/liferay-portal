/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.navigation.internal.upgrade.v2_6_0;

import com.liferay.asset.kernel.service.AssetVocabularyLocalServiceUtil;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.model.ExternalReferenceCodeModel;
import com.liferay.portal.kernel.model.PersistedModel;
import com.liferay.portal.kernel.service.LayoutLocalServiceUtil;
import com.liferay.portal.kernel.service.PersistedModelLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.UnicodePropertiesBuilder;
import com.liferay.portal.service.PersistedModelLocalServiceRegistryUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Objects;

/**
 * @author Joao Victor Alves
 */
public class SiteNavigationMenuItemExternalReferenceCodeUpgradeProcess
	extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		try (PreparedStatement preparedStatement1 = connection.prepareStatement(
				"select ctCollectionId, siteNavigationMenuItemId, type_, " +
					"typeSettings from SiteNavigationMenuItem");
			PreparedStatement preparedStatement2 =
				AutoBatchPreparedStatementUtil.concurrentAutoBatch(
					connection,
					"update SiteNavigationMenuItem set typeSettings = ?");
			ResultSet resultSet = preparedStatement1.executeQuery()) {

			while (resultSet.next()) {
				UnicodeProperties typeSettingsUnicodeProperties =
					UnicodePropertiesBuilder.fastLoad(
						resultSet.getString("typeSettings")
					).build();

				PersistedModel model;

				String navigationMenuItemType = resultSet.getString("type_");

				if (Objects.equals(navigationMenuItemType, "layout")) {
					model = LayoutLocalServiceUtil.fetchLayoutByUuidAndGroupId(
						typeSettingsUnicodeProperties.getProperty("Uuid"),
						GetterUtil.getLong(
							typeSettingsUnicodeProperties.getProperty(
								"groupId")),
						GetterUtil.getBoolean(
							typeSettingsUnicodeProperties.getProperty(
								"privateLayout")));
				}
				else if (Objects.equals(
							navigationMenuItemType, "asset-vocabulary")) {

					model =
						AssetVocabularyLocalServiceUtil.
							fetchAssetVocabularyByUuidAndGroupId(
								typeSettingsUnicodeProperties.getProperty(
									"Uuid"),
								GetterUtil.getLong(
									typeSettingsUnicodeProperties.getProperty(
										"groupId")));
				}
				else {
					PersistedModelLocalService persistedModelLocalService =
						PersistedModelLocalServiceRegistryUtil.
							getPersistedModelLocalService(
								typeSettingsUnicodeProperties.getProperty(
									"className"));

					model = persistedModelLocalService.getPersistedModel(
						GetterUtil.getLong(
							typeSettingsUnicodeProperties.getProperty(
								"classPK")));
				}

				if (model instanceof ExternalReferenceCodeModel) {
					ExternalReferenceCodeModel externalReferenceCodeModel =
						(ExternalReferenceCodeModel)model;

					typeSettingsUnicodeProperties.setProperty(
						"externalReferenceCode",
						externalReferenceCodeModel.getExternalReferenceCode());
				}
				else if (Objects.equals(
							model.getClass(
							).getName(),
							"com.liferay.commerce.product.model." +
								"CPDefinition")) {

					// faz alguma coisa ai n sei

				}

				preparedStatement2.setString(
					1, typeSettingsUnicodeProperties.toString());

				preparedStatement2.addBatch();
			}

			preparedStatement2.executeBatch();
		}
	}

}