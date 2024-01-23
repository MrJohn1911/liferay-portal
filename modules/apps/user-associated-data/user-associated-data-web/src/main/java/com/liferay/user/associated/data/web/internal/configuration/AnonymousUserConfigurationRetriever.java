/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.user.associated.data.web.internal.configuration;

import java.io.IOException;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;

/**
 * @author Drew Brokke
 */
@Component(service = AnonymousUserConfigurationRetriever.class)
public class AnonymousUserConfigurationRetriever {

	public Configuration get(
			ConfigurationAdmin configurationAdmin, long companyId)
		throws InvalidSyntaxException, IOException {

		return _get(
			configurationAdmin.listConfigurations(
				String.format(
					"(&(service.factoryPid=%s)(companyId=%s))",
					_getFactoryPid(), companyId)));
	}

	public Configuration get(
			ConfigurationAdmin configurationAdmin, long companyId, long userId)
		throws InvalidSyntaxException, IOException {

		return _get(
			configurationAdmin.listConfigurations(
				String.format(
					"(&(service.factoryPid=%s)(companyId=%s)(userId=%s))",
					_getFactoryPid(), companyId, userId)));
	}

	private Configuration _get(Configuration[] configurations) {
		if (configurations == null) {
			return null;
		}

		return configurations[0];
	}

	private String _getFactoryPid() {
		return AnonymousUserConfiguration.class.getName() + ".scoped";
	}

}