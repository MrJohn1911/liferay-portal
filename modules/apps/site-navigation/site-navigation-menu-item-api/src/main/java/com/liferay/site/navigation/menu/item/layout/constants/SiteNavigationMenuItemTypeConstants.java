/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.navigation.menu.item.layout.constants;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.blogs.model.BlogsEntry;
import com.liferay.journal.model.JournalArticle;
import com.liferay.knowledge.base.model.KBArticle;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.portal.kernel.repository.model.FileEntry;

/**
 * @author Eudaldo Alonso
 */
public class SiteNavigationMenuItemTypeConstants {

	public static final String ASSET_VOCABULARY = "asset_vocabulary";

	public static final String LAYOUT = "layout";

	public static final String NODE = "node";

	public static final String[] SUPPORTED_ERC_SITE_NAVIGATION_MENU_ITEM_TYPES =
		{
			AssetCategory.class.getName(), ASSET_VOCABULARY,
			BlogsEntry.class.getName(), FileEntry.class.getName(),
			JournalArticle.class.getName(), KBArticle.class.getName(), LAYOUT,
			ObjectDefinition.class.getName()
		};

	public static final String URL = "url";

}