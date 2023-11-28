/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.commerce.frontend.internal.account;

import com.liferay.account.model.AccountEntry;
import com.liferay.account.service.AccountEntryLocalService;
import com.liferay.commerce.frontend.internal.account.model.Account;
import com.liferay.commerce.frontend.internal.account.model.AccountList;
import com.liferay.commerce.util.CommerceAccountHelper;
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.webserver.WebServerServletToken;

import java.util.List;

import org.osgi.service.component.annotations.Reference;

/**
 * @author Joao Victor Alves
 */
public abstract class BaseAccountResource {

	protected AccountList getAccountList(
			long userId, long parentAccountId, int commerceSiteType,
			String keywords, int page, int pageSize, String imagePath)
		throws PortalException {

		int start = (page - 1) * pageSize;
		int end = page * pageSize;

		List<AccountEntry> userAccountEntries =
			accountEntryLocalService.getUserAccountEntries(
				userId, parentAccountId, keywords,
				commerceAccountHelper.toAccountEntryTypes(commerceSiteType),
				commerceAccountHelper.toAccountEntryStatus(true), start, end);

		return new AccountList(
			TransformUtil.transform(
				userAccountEntries,
				accountEntry -> new Account(
					String.valueOf(accountEntry.getAccountEntryId()),
					accountEntry.getName(),
					getLogoThumbnailSrc(accountEntry.getLogoId(), imagePath))),
			_getAccountsCount(
				userId, parentAccountId, commerceSiteType, keywords));
	}

	protected String getLogoThumbnailSrc(long logoId, String imagePath) {
		return StringBundler.concat(
			imagePath, "/organization_logo?img_id=", logoId, "&t=",
			webServerServletToken.getToken(logoId));
	}

	@Reference
	protected AccountEntryLocalService accountEntryLocalService;

	@Reference
	protected CommerceAccountHelper commerceAccountHelper;

	@Reference
	protected WebServerServletToken webServerServletToken;

	private int _getAccountsCount(
			long userId, Long parentAccountId, int commerceSiteType,
			String keywords)
		throws PortalException {

		return accountEntryLocalService.getUserAccountEntriesCount(
			userId, parentAccountId, keywords,
			commerceAccountHelper.toAccountEntryTypes(commerceSiteType),
			commerceAccountHelper.toAccountEntryStatus(true));
	}

}