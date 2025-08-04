/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.commerce.internal.model.listener;

import com.liferay.commerce.product.model.CProduct;
import com.liferay.commerce.service.CPDAvailabilityEstimateLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.ModelListener;

import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.UnicodePropertiesBuilder;
import com.liferay.site.navigation.model.SiteNavigationMenu;
import com.liferay.site.navigation.model.SiteNavigationMenuItem;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;
import java.util.Objects;

/**
 * @author Alessio Antonio Rendina
 */
@Component(service = ModelListener.class)
public class CProductModelListener extends BaseModelListener<CProduct> {

	@Override
	public void onBeforeRemove(CProduct cProduct) {
		_cpdAvailabilityEstimateLocalService.
			deleteCPDAvailabilityEstimateByCProductId(cProduct.getCProductId());
	}

	@Override
	public void onAfterUpdate(CProduct originalCProduct, CProduct cProduct) {

		if (Objects.equals(originalCProduct.getExternalReferenceCode(), cProduct.getExternalReferenceCode()) {
			_updateSiteNavigationMenuItem(originalCProduct.getExternalReferenceCode(), cProduct);
		}
	}


	private void _updateSiteNavigationMenuItem(String externalReferenceCode, CProduct cProduct)
		throws PortalException {

		List<SiteNavigationMenu> siteNavigationMenus =
			_siteNavigationMenuLocalService.getSiteNavigationMenus(
				cProduct.getGroupId());

		List<SiteNavigationMenuItem> siteNavigationMenuItems =
			_siteNavigationMenuItemLocalService.getSiteNavigationMenuItems(
				siteNavigationMenu.getSiteNavigationMenuId());

		for (SiteNavigationMenuItem siteNavigationMenuItem :
			siteNavigationMenuItems) {

			UnicodeProperties unicodeProperties =
				UnicodePropertiesBuilder.fastLoad(
					siteNavigationMenuItem.getTypeSettings()
				).build();

			String layoutUuid = unicodeProperties.getProperty("layoutUuid");

			if (Objects.equals(layout.getUuid(), layoutUuid)) {
				_siteNavigationMenuItemLocalService.
					deleteSiteNavigationMenuItem(
						siteNavigationMenuItem.getSiteNavigationMenuItemId());
			}
		}
	}

	@Reference
	private CPDAvailabilityEstimateLocalService
		_cpdAvailabilityEstimateLocalService;

	@Reference
	private SiteNavigationMenuItemLocalService
		_siteNavigationMenuItemLocalService;

	@Reference
	private SiteNavigationMenuLocalService
		_siteNavigationMenuLocalService;

}