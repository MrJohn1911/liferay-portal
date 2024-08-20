/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.friendly.url.internal.upgrade.v3_4_2;

import com.liferay.friendly.url.model.FriendlyURLEntry;
import com.liferay.friendly.url.service.FriendlyURLEntryLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Joao Victor Alves
 */
public class FriendlyURLFormatUpgradeProcess extends UpgradeProcess {

	public FriendlyURLFormatUpgradeProcess(
		FriendlyURLEntryLocalService friendlyURLEntryLocalService) {

		_friendlyURLEntryLocalService = friendlyURLEntryLocalService;
	}

	@Override
	protected void doUpgrade() throws Exception {
		try (PreparedStatement preparedStatement1 = connection.prepareStatement(
				"select groupId, urlTitle, friendlyURLEntryId, classNameId" +
					", languageId, classPK from FriendlyURLEntryLocalization " +
						"where urlTitle like '%/'");
			ResultSet resultSet = preparedStatement1.executeQuery()) {

			while (resultSet.next()) {
				long groupId = resultSet.getLong(1);
				String urlTitle = resultSet.getString(2);
				long friendlyURLEntryId = resultSet.getLong(3);
				long classNameId = resultSet.getLong(4);
				String languageId = resultSet.getString(5);

				FriendlyURLEntry friendlyURLEntry =
					_friendlyURLEntryLocalService.fetchFriendlyURLEntry(
						friendlyURLEntryId);

				urlTitle = urlTitle.substring(0, urlTitle.length() - 1);

				FriendlyURLEntry duplicatedFriendlyURLEntry =
					_friendlyURLEntryLocalService.fetchFriendlyURLEntry(
						groupId, classNameId, urlTitle);

				if (duplicatedFriendlyURLEntry != null) {
					long classPK = resultSet.getLong(6);

					urlTitle = _friendlyURLEntryLocalService.getUniqueUrlTitle(
						groupId, classNameId, classPK, urlTitle, languageId);
				}

				_friendlyURLEntryLocalService.
					updateFriendlyURLEntryLocalization(
						friendlyURLEntry, languageId, urlTitle);
			}
		}
	}

	private final FriendlyURLEntryLocalService _friendlyURLEntryLocalService;

}