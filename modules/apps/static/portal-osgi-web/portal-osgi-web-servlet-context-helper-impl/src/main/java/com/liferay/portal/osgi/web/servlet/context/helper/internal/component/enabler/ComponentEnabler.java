/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.osgi.web.servlet.context.helper.internal.component.enabler;

import com.liferay.osgi.util.ComponentUtil;
import com.liferay.portal.osgi.web.servlet.context.helper.internal.ServletContextHelperFactoryImpl;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.runtime.HttpServiceRuntime;

/**
 * @author Joao Victor Alves
 */
@Component(service = {})
public class ComponentEnabler {

	@Activate
	protected void activate(ComponentContext componentContext) {
		ComponentUtil.enableComponents(
			HttpServiceRuntime.class, null, componentContext,
			ServletContextHelperFactoryImpl.class);
	}

}