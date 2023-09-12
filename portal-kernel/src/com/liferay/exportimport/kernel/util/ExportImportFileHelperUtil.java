/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.exportimport.kernel.util;

import com.liferay.exportimport.kernel.lar.MissingReferences;
import com.liferay.exportimport.kernel.model.ExportImportConfiguration;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.ServiceProxyFactory;

import java.io.File;
import java.io.InputStream;

/**
 * @author Gabriel Santos
 */
public class ExportImportFileHelperUtil {

	public static File exportLayoutsAsFile(
			ExportImportConfiguration exportImportConfiguration)
		throws PortalException {

		return _exportImportFileHelper.exportLayoutsAsFile(
			exportImportConfiguration);
	}

	public static long exportLayoutsAsFileInBackground(
			long userId, ExportImportConfiguration exportImportConfiguration)
		throws PortalException {

		return _exportImportFileHelper.exportLayoutsAsFileInBackground(
			userId, exportImportConfiguration);
	}

	public static long exportLayoutsAsFileInBackground(
			long userId, long exportImportConfigurationId)
		throws PortalException {

		return _exportImportFileHelper.exportLayoutsAsFileInBackground(
			userId, exportImportConfigurationId);
	}

	public static File exportPortletInfoAsFile(
			ExportImportConfiguration exportImportConfiguration)
		throws PortalException {

		return _exportImportFileHelper.exportPortletInfoAsFile(
			exportImportConfiguration);
	}

	public static long exportPortletInfoAsFileInBackground(
			long userId, ExportImportConfiguration exportImportConfiguration)
		throws PortalException {

		return _exportImportFileHelper.exportPortletInfoAsFileInBackground(
			userId, exportImportConfiguration);
	}

	public static long exportPortletInfoAsFileInBackground(
			long userId, long exportImportConfigurationId)
		throws PortalException {

		return _exportImportFileHelper.exportPortletInfoAsFileInBackground(
			userId, exportImportConfigurationId);
	}

	public static void importLayouts(
			ExportImportConfiguration exportImportConfiguration, File file)
		throws PortalException {

		_exportImportFileHelper.importLayouts(exportImportConfiguration, file);
	}

	public static void importLayouts(
			ExportImportConfiguration exportImportConfiguration,
			InputStream inputStream)
		throws PortalException {

		_exportImportFileHelper.importLayouts(
			exportImportConfiguration, inputStream);
	}

	public static void importLayoutsDataDeletions(
			ExportImportConfiguration exportImportConfiguration, File file)
		throws PortalException {

		_exportImportFileHelper.importLayoutsDataDeletions(
			exportImportConfiguration, file);
	}

	public static long importLayoutSetPrototypeInBackground(
			long userId, ExportImportConfiguration exportImportConfiguration,
			File file)
		throws PortalException {

		return _exportImportFileHelper.importLayoutSetPrototypeInBackground(
			userId, exportImportConfiguration, file);
	}

	public static long importLayoutsInBackground(
			long userId, ExportImportConfiguration exportImportConfiguration,
			File file)
		throws PortalException {

		return _exportImportFileHelper.importLayoutSetPrototypeInBackground(
			userId, exportImportConfiguration, file);
	}

	public static long importLayoutsInBackground(
			long userId, ExportImportConfiguration exportImportConfiguration,
			InputStream inputStream)
		throws PortalException {

		return _exportImportFileHelper.importLayoutsInBackground(
			userId, exportImportConfiguration, inputStream);
	}

	public static long importLayoutsInBackground(
			long userId, long exportImportConfigurationId, File file)
		throws PortalException {

		return _exportImportFileHelper.importLayoutsInBackground(
			userId, exportImportConfigurationId, file);
	}

	public static long importLayoutsInBackground(
			long userId, long exportImportConfigurationId,
			InputStream inputStream)
		throws PortalException {

		return _exportImportFileHelper.importLayoutsInBackground(
			userId, exportImportConfigurationId, inputStream);
	}

	public static void importPortletDataDeletions(
			ExportImportConfiguration exportImportConfiguration, File file)
		throws PortalException {

		_exportImportFileHelper.importPortletDataDeletions(
			exportImportConfiguration, file);
	}

	public static void importPortletInfo(
			ExportImportConfiguration exportImportConfiguration, File file)
		throws PortalException {

		_exportImportFileHelper.importPortletInfo(
			exportImportConfiguration, file);
	}

	public static void importPortletInfo(
			ExportImportConfiguration exportImportConfiguration,
			InputStream inputStream)
		throws PortalException {

		_exportImportFileHelper.importPortletInfo(
			exportImportConfiguration, inputStream);
	}

	public static long importPortletInfoInBackground(
			long userId, ExportImportConfiguration exportImportConfiguration,
			File file)
		throws PortalException {

		return _exportImportFileHelper.importLayoutsInBackground(
			userId, exportImportConfiguration, file);
	}

	public static long importPortletInfoInBackground(
			long userId, ExportImportConfiguration exportImportConfiguration,
			InputStream inputStream)
		throws PortalException {

		return _exportImportFileHelper.importPortletInfoInBackground(
			userId, exportImportConfiguration, inputStream);
	}

	public static long importPortletInfoInBackground(
			long userId, long exportImportConfigurationId, File file)
		throws PortalException {

		return _exportImportFileHelper.importPortletInfoInBackground(
			userId, exportImportConfigurationId, file);
	}

	public static long importPortletInfoInBackground(
			long userId, long exportImportConfigurationId,
			InputStream inputStream)
		throws PortalException {

		return _exportImportFileHelper.importPortletInfoInBackground(
			userId, exportImportConfigurationId, inputStream);
	}

	public static long mergeLayoutSetPrototypeInBackground(
			long userId, long groupId,
			ExportImportConfiguration exportImportConfiguration)
		throws PortalException {

		return _exportImportFileHelper.mergeLayoutSetPrototypeInBackground(
			userId, groupId, exportImportConfiguration);
	}

	public static MissingReferences validateImportLayoutsFile(
			ExportImportConfiguration exportImportConfiguration, File file)
		throws PortalException {

		return _exportImportFileHelper.validateImportLayoutsFile(
			exportImportConfiguration, file);
	}

	public static MissingReferences validateImportLayoutsFile(
			ExportImportConfiguration exportImportConfiguration,
			InputStream inputStream)
		throws PortalException {

		return _exportImportFileHelper.validateImportLayoutsFile(
			exportImportConfiguration, inputStream);
	}

	public static MissingReferences validateImportPortletInfo(
			ExportImportConfiguration exportImportConfiguration, File file)
		throws PortalException {

		return _exportImportFileHelper.validateImportLayoutsFile(
			exportImportConfiguration, file);
	}

	public static MissingReferences validateImportPortletInfo(
			ExportImportConfiguration exportImportConfiguration,
			InputStream inputStream)
		throws PortalException {

		return _exportImportFileHelper.validateImportLayoutsFile(
			exportImportConfiguration, inputStream);
	}

	private static volatile ExportImportFileHelper _exportImportFileHelper =
		ServiceProxyFactory.newServiceTrackedInstance(
			ExportImportFileHelper.class, ExportImportFileHelperUtil.class,
			"_exportImportHelper", false);

}