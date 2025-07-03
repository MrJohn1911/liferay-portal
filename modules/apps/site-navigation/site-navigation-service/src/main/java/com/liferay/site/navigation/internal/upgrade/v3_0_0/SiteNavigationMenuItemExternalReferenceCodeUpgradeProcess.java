/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.navigation.internal.upgrade.v3_0_0;

import com.liferay.asset.kernel.service.AssetVocabularyLocalServiceUtil;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.knowledge.base.model.KBArticle;
import com.liferay.knowledge.base.service.KBArticleLocalServiceUtil;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.model.ExternalReferenceCodeModel;
import com.liferay.portal.kernel.model.PersistedModel;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.LayoutLocalServiceUtil;
import com.liferay.portal.kernel.service.PersistedModelLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.UnicodePropertiesBuilder;
import com.liferay.portal.service.PersistedModelLocalServiceRegistryUtil;
import com.liferay.site.navigation.menu.item.layout.constants.SiteNavigationMenuItemTypeConstants;
import com.liferay.site.navigation.menu.item.util.SiteNavigationMenuItemUtil;

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
					"update SiteNavigationMenuItem set typeSettings = ? " +
						"where ctCollectionId = ? and " +
							"siteNavigationMenuItemId = ?");
			ResultSet resultSet = preparedStatement1.executeQuery()) {

			while (resultSet.next()) {
				String navigationMenuItemType = resultSet.getString("type_");

				if (!SiteNavigationMenuItemUtil.isExternalReferenceCodeType(
						navigationMenuItemType)) {

					continue;
				}

				PersistedModel model;
				UnicodeProperties typeSettingsUnicodeProperties =
					UnicodePropertiesBuilder.fastLoad(
						resultSet.getString("typeSettings")
					).build();

				if (Objects.equals(
						navigationMenuItemType,
						SiteNavigationMenuItemTypeConstants.LAYOUT)) {

					model = LayoutLocalServiceUtil.fetchLayoutByUuidAndGroupId(
						typeSettingsUnicodeProperties.getProperty("layoutUuid"),
						GetterUtil.getLong(
							typeSettingsUnicodeProperties.getProperty(
								"groupId")),
						GetterUtil.getBoolean(
							typeSettingsUnicodeProperties.getProperty(
								"privateLayout")));
				}
				else if (Objects.equals(
							navigationMenuItemType,
							SiteNavigationMenuItemTypeConstants.
								ASSET_VOCABULARY)) {

					model =
						AssetVocabularyLocalServiceUtil.
							fetchAssetVocabularyByUuidAndGroupId(
								typeSettingsUnicodeProperties.getProperty(
									"uuid"),
								GetterUtil.getLong(
									typeSettingsUnicodeProperties.getProperty(
										"groupId")));
				}
				else if (Objects.equals(
							navigationMenuItemType,
							JournalArticle.class.getName())) {

					model = JournalArticleLocalServiceUtil.getLatestArticle(
						GetterUtil.getLong(
							typeSettingsUnicodeProperties.getProperty(
								"classPK")));
				}
				else if (Objects.equals(
							navigationMenuItemType,
							KBArticle.class.getName())) {

					model = KBArticleLocalServiceUtil.getLatestKBArticle(
						GetterUtil.getLong(
							typeSettingsUnicodeProperties.getProperty(
								"classPK")));
				}
				else {
					String className =
						typeSettingsUnicodeProperties.getProperty("className");

					if (className.equals(FileEntry.class.getName())) {
						className = DLFileEntry.class.getName();
					}
					else if (className.contains(
								ObjectDefinition.class.getName())) {

						className = ObjectEntry.class.getName();
					}

					PersistedModelLocalService persistedModelLocalService =
						PersistedModelLocalServiceRegistryUtil.
							getPersistedModelLocalService(className);

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

				preparedStatement2.setString(
					1, typeSettingsUnicodeProperties.toString());

				preparedStatement2.setLong(
					2, resultSet.getLong("ctCollectionId"));

				preparedStatement2.setLong(
					3, resultSet.getLong("siteNavigationMenuItemId"));

				preparedStatement2.addBatch();
			}

			preparedStatement2.executeBatch();
		}
	}

}