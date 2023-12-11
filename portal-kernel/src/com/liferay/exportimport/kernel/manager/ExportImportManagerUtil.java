/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.exportimport.kernel.manager;

import com.liferay.exportimport.kernel.lar.MissingReferences;
import com.liferay.exportimport.kernel.model.ExportImportConfiguration;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.module.service.Snapshot;

import java.io.File;
import java.io.InputStream;

/**
 * @author Gabriel Santos
 */
public class ExportImportManagerUtil {

	public static File exportLayoutsAsFile(
			boolean checkPermission,
			ExportImportConfiguration exportImportConfiguration)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.exportLayoutsAsFile(
			checkPermission, exportImportConfiguration);
	}

	public static File exportLayoutsAsFile(
			ExportImportConfiguration exportImportConfiguration)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.exportLayoutsAsFile(
			exportImportConfiguration);
	}

	public static File exportPortletInfoAsFile(
			ExportImportConfiguration exportImportConfiguration)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.exportPortletInfoAsFile(
			exportImportConfiguration);
	}

	public static long exportPortletInfoAsFileInBackground(
			long userId, ExportImportConfiguration exportImportConfiguration)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.exportPortletInfoAsFileInBackground(
			userId, exportImportConfiguration);
	}

	public static long exportPortletInfoAsFileInBackground(
			long userId, long exportImportConfigurationId)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.exportPortletInfoAsFileInBackground(
			userId, exportImportConfigurationId);
	}

	public static void importLayouts(
			boolean checkPermission,
			ExportImportConfiguration exportImportConfiguration, File file)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		exportImportManager.importLayouts(
			checkPermission, exportImportConfiguration, file);
	}

	public static void importLayouts(
			boolean checkPermission,
			ExportImportConfiguration exportImportConfiguration,
			InputStream inputStream)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		exportImportManager.importLayouts(
			checkPermission, exportImportConfiguration, inputStream);
	}

	public static void importLayouts(
			ExportImportConfiguration exportImportConfiguration, File file)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		exportImportManager.importLayouts(exportImportConfiguration, file);
	}

	public static void importLayouts(
			ExportImportConfiguration exportImportConfiguration,
			InputStream inputStream)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		exportImportManager.importLayouts(
			exportImportConfiguration, inputStream);
	}

	public static void importLayoutsDataDeletions(
			ExportImportConfiguration exportImportConfiguration, File file)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		exportImportManager.importLayoutsDataDeletions(
			exportImportConfiguration, file);
	}

	public static long importLayoutSetPrototypeInBackground(
			long userId, ExportImportConfiguration exportImportConfiguration,
			File file)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.importLayoutSetPrototypeInBackground(
			userId, exportImportConfiguration, file);
	}

	public static long importLayoutsInBackground(
			long userId, ExportImportConfiguration exportImportConfiguration,
			File file)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.importLayoutSetPrototypeInBackground(
			userId, exportImportConfiguration, file);
	}

	public static long importLayoutsInBackground(
			long userId, ExportImportConfiguration exportImportConfiguration,
			InputStream inputStream)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.importLayoutsInBackground(
			userId, exportImportConfiguration, inputStream);
	}

	public static long importLayoutsInBackground(
			long userId, long exportImportConfigurationId, File file)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.importLayoutsInBackground(
			userId, exportImportConfigurationId, file);
	}

	public static long importLayoutsInBackground(
			long userId, long exportImportConfigurationId,
			InputStream inputStream)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.importLayoutsInBackground(
			userId, exportImportConfigurationId, inputStream);
	}

	public static void importPortletDataDeletions(
			ExportImportConfiguration exportImportConfiguration, File file)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		exportImportManager.importPortletDataDeletions(
			exportImportConfiguration, file);
	}

	public static void importPortletInfo(
			ExportImportConfiguration exportImportConfiguration, File file)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		exportImportManager.importPortletInfo(exportImportConfiguration, file);
	}

	public static void importPortletInfo(
			ExportImportConfiguration exportImportConfiguration,
			InputStream inputStream)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		exportImportManager.importPortletInfo(
			exportImportConfiguration, inputStream);
	}

	public static long importPortletInfoInBackground(
			long userId, ExportImportConfiguration exportImportConfiguration,
			File file)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.importLayoutsInBackground(
			userId, exportImportConfiguration, file);
	}

	public static long importPortletInfoInBackground(
			long userId, ExportImportConfiguration exportImportConfiguration,
			InputStream inputStream)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.importPortletInfoInBackground(
			userId, exportImportConfiguration, inputStream);
	}

	public static long importPortletInfoInBackground(
			long userId, long exportImportConfigurationId, File file)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.importPortletInfoInBackground(
			userId, exportImportConfigurationId, file);
	}

	public static long importPortletInfoInBackground(
			long userId, long exportImportConfigurationId,
			InputStream inputStream)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.importPortletInfoInBackground(
			userId, exportImportConfigurationId, inputStream);
	}

	public static long mergeLayoutSetPrototypeInBackground(
			long userId, long groupId,
			ExportImportConfiguration exportImportConfiguration)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.mergeLayoutSetPrototypeInBackground(
			userId, groupId, exportImportConfiguration);
	}

	public static MissingReferences validateImportLayoutsFile(
			ExportImportConfiguration exportImportConfiguration, File file)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.validateImportLayoutsFile(
			exportImportConfiguration, file);
	}

	public static MissingReferences validateImportLayoutsFile(
			ExportImportConfiguration exportImportConfiguration,
			InputStream inputStream)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.validateImportLayoutsFile(
			exportImportConfiguration, inputStream);
	}

	public static MissingReferences validateImportPortletInfo(
			ExportImportConfiguration exportImportConfiguration, File file)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.validateImportLayoutsFile(
			exportImportConfiguration, file);
	}

	public static MissingReferences validateImportPortletInfo(
			ExportImportConfiguration exportImportConfiguration,
			InputStream inputStream)
		throws PortalException {

		ExportImportManager exportImportManager =
			_exportImportManagerSnapshot.get();

		return exportImportManager.validateImportLayoutsFile(
			exportImportConfiguration, inputStream);
	}

	private static final Snapshot<ExportImportManager>
		_exportImportManagerSnapshot = new Snapshot<>(
			ExportImportManagerUtil.class, ExportImportManager.class);

}