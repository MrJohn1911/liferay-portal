/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.properties.swapper.internal.component.enabler;

import com.liferay.document.library.kernel.store.Store;
import com.liferay.osgi.util.ComponentUtil;
import com.liferay.portal.properties.swapper.internal.portal.profile.ModulePortalProfile;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * @author Joao Victor Alves
 */
@Component(service = {})
public class ComponentEnabler {

	@Activate
	protected void activate(ComponentContext componentContext) {
		ComponentUtil.enableComponents(
			Store.class, "(default=true)", componentContext,
			ModulePortalProfile.class);
	}

}