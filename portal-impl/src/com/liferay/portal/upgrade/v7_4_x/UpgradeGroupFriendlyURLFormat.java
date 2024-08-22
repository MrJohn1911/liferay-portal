/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.v7_4_x;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.persistence.GroupUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Joao Victor Alves
 */
public class UpgradeGroupFriendlyURLFormat extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		try (PreparedStatement preparedStatement1 = connection.prepareStatement(
			"select friendlyURL, groupId, companyId, ctCollectionId from " +
			"Group_ where friendlyURL like '%/'");
			 PreparedStatement preparedStatement2 =
				 AutoBatchPreparedStatementUtil.concurrentAutoBatch(
					 connection,
					 "update Group_ set friendlyURL = ? where groupId = ? " +
					 "and ctCollectionId = ? and companyId = ?");

			 ResultSet resultSet = preparedStatement1.executeQuery()
			 ) {

			while (resultSet.next()) {
				String friendlyURL = resultSet.getString(1);
				long groupId = resultSet.getLong(2);
				long companyId = resultSet.getLong(3);
				long ctCollectionId = resultSet.getLong(4);

				friendlyURL = friendlyURL.substring(
					0, friendlyURL.length() - 1);

				Group group = GroupUtil.fetchByC_F(companyId, friendlyURL);

				if (group != null) {
					friendlyURL = _getUniqueURL(group, friendlyURL, companyId);
				}

				preparedStatement2.setString(1, friendlyURL);

				preparedStatement2.setLong(2, groupId);

				preparedStatement2.setLong(3, ctCollectionId);

				preparedStatement2.setLong(4, companyId);

				preparedStatement2.addBatch();
			}

			preparedStatement2.executeBatch();
		}
	}

	private String _getUniqueURL(
		Group group, String friendlyURL, long companyId) {

		String tempFriendlyURL = friendlyURL;

		for (int i = 1; group != null; i++) {
			tempFriendlyURL = friendlyURL + StringPool.DASH + i;

			group = GroupUtil.fetchByC_F(companyId, tempFriendlyURL);
		}

		return tempFriendlyURL;
	}

}