/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.user.associated.data.web.internal.util;

import com.liferay.user.associated.data.web.internal.configuration.AnonymousUserConfiguration;

import java.io.IOException;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * @author Drew Brokke
 */
public class AnonymousUserConfigurationRetrieverUtil {

	public static Configuration get(
			ConfigurationAdmin configurationAdmin, long companyId)
		throws InvalidSyntaxException, IOException {

		return _get(
			configurationAdmin.listConfigurations(
				String.format(
					"(&(service.factoryPid=%s)(companyId=%s))",
					_getFactoryPid(), companyId)));
	}

	public static Configuration get(
			ConfigurationAdmin configurationAdmin, long companyId, long userId)
		throws InvalidSyntaxException, IOException {

		return _get(
			configurationAdmin.listConfigurations(
				String.format(
					"(&(service.factoryPid=%s)(companyId=%s)(userId=%s))",
					_getFactoryPid(), companyId, userId)));
	}

	private static Configuration _get(Configuration[] configurations) {
		if (configurations == null) {
			return null;
		}

		return configurations[0];
	}

	private static String _getFactoryPid() {
		return AnonymousUserConfiguration.class.getName() + ".scoped";
	}

}