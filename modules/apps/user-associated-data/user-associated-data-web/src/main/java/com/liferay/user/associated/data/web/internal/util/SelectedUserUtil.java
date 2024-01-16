/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.user.associated.data.web.internal.util;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.user.associated.data.anonymizer.UADAnonymousUserProvider;

import java.util.Objects;

import javax.portlet.PortletRequest;

/**
 * @author Drew Brokke
 */
public class SelectedUserUtil {

	public static User getSelectedUser(
			PortletRequest portletRequest,
			UADAnonymousUserProvider uadAnonymousUserProvider)
		throws PortalException {

		User selectedUser = PortalUtil.getSelectedUser(portletRequest);

		if (Objects.equals(PortalUtil.getUser(portletRequest), selectedUser)) {
			throw new PortalException(
				"The selected user cannot be the logged in user");
		}

		if (uadAnonymousUserProvider.isAnonymousUser(selectedUser)) {
			throw new PortalException(
				"The selected user cannot be the anonymous user");
		}

		return selectedUser;
	}

	public static long getSelectedUserId(
			PortletRequest portletRequest,
			UADAnonymousUserProvider uadAnonymousUserProvider)
		throws PortalException {

		User selectedUser = getSelectedUser(
			portletRequest, uadAnonymousUserProvider);

		return selectedUser.getUserId();
	}

}