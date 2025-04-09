/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.navigation.internal.search;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.search.BaseIndexer;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.IndexWriterHelperUtil;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.Summary;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.site.navigation.model.SiteNavigationMenu;
import com.liferay.site.navigation.service.SiteNavigationMenuLocalService;

import java.util.List;
import java.util.Locale;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Joao Victor Alves
 */
@Component(service = Indexer.class)
public class SiteNavigationMenuIndexer extends BaseIndexer<SiteNavigationMenu> {

	public static final String CLASS_NAME = SiteNavigationMenu.class.getName();

	public SiteNavigationMenuIndexer() {
		setDefaultSelectedFieldNames(
			Field.COMPANY_ID, Field.GROUP_ID, Field.ENTRY_CLASS_NAME,
			Field.ENTRY_CLASS_PK, Field.UID, Field.TITLE);
		setFilterSearch(true);
		setPermissionAware(true);
	}

	@Override
	public String getClassName() {
		return CLASS_NAME;
	}

	@Override
	protected void doDelete(SiteNavigationMenu siteNavigationMenu)
		throws Exception {

		deleteDocument(
			siteNavigationMenu.getCompanyId(),
			siteNavigationMenu.getPrimaryKey());
	}

	@Override
	protected Document doGetDocument(SiteNavigationMenu siteNavigationMenu) {
		Document document = getBaseModelDocument(
			CLASS_NAME, siteNavigationMenu);

		document.addText(Field.TITLE, siteNavigationMenu.getName());
		document.addDate(
			Field.MODIFIED_DATE, siteNavigationMenu.getModifiedDate());

		return document;
	}

	@Override
	protected Summary doGetSummary(
			Document document, Locale locale, String snippet,
			PortletRequest portletRequest, PortletResponse portletResponse)
		throws Exception {

		String title = document.get(Field.TITLE);

		return new Summary(title, snippet);
	}

	@Override
	protected void doReindex(SiteNavigationMenu siteNavigationMenu)
		throws Exception {

		Document document = getDocument(siteNavigationMenu);

		if (document != null) {
			IndexWriterHelperUtil.updateDocument(
				siteNavigationMenu.getCompanyId(), document);
		}
	}

	@Override
	protected void doReindex(String className, long classPK) throws Exception {
		doReindex(
			_siteNavigationMenuLocalService.getSiteNavigationMenu(classPK));
	}

	@Override
	protected void doReindex(String[] ids) throws Exception {
		long companyId = GetterUtil.getLong(ids[0]);

		List<SiteNavigationMenu> menus =
			_siteNavigationMenuLocalService.getSiteNavigationMenus(
				QueryUtil.ALL_POS, QueryUtil.ALL_POS);

		for (SiteNavigationMenu menu : menus) {
			if (menu.getCompanyId() == companyId) {
				doReindex(menu);
			}
		}
	}

	@Reference
	private SiteNavigationMenuLocalService _siteNavigationMenuLocalService;

}