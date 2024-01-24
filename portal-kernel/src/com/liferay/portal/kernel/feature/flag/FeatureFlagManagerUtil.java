/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.feature.flag;

import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.module.service.Snapshot;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PortalRunMode;
import com.liferay.portal.kernel.util.PropsUtil;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Drew Brokke
 */
public class FeatureFlagManagerUtil {

	public static String getJSON(long companyId) {
		return _withFeatureFlagManager(
			featureFlagManager -> featureFlagManager.getJSON(companyId),
			FeatureFlagManagerUtil::_getJSON);
	}

	public static boolean isEnabled(long companyId, String key) {
		return _withFeatureFlagManager(
			featureFlagManager -> featureFlagManager.isEnabled(companyId, key),
			() -> {
				if (PortalRunMode.isTestMode()) {
					return GetterUtil.getBoolean(
						PropsUtil.get("feature.flag." + key));
				}

				try (SafeCloseable safeCloseable =
						CompanyThreadLocal.setWithSafeCloseable(companyId)) {

					return GetterUtil.getBoolean(
						PropsUtil.get("feature.flag." + key));
				}
			});
	}

	public static boolean isEnabled(String key) {
		return _withFeatureFlagManager(
			featureFlagManager -> featureFlagManager.isEnabled(key),
			() -> GetterUtil.getBoolean(PropsUtil.get("feature.flag." + key)));
	}

	private static String _getJSON() {
		JSONFactory jsonFactory = _jsonFactorySnapshot.get();

		return String.valueOf(
			jsonFactory.createJSONObject(
				PropsUtil.getProperties("feature.flag.", true)));
	}

	private static <T> T _withFeatureFlagManager(
		Function<FeatureFlagManager, T> function, Supplier<T> supplier) {

		if (PortalRunMode.isTestMode()) {
			return supplier.get();
		}

		FeatureFlagManager featureFlagManager =
			_featureFlagManagerSnapshot.get();

		if (featureFlagManager != null) {
			return function.apply(featureFlagManager);
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				"No feature flag manager service found. Returning the " +
					"default value.");
		}

		return supplier.get();
	}

	private static final Log _log = LogFactoryUtil.getLog(
		FeatureFlagManagerUtil.class);

	private static final Snapshot<FeatureFlagManager>
		_featureFlagManagerSnapshot = new Snapshot<>(
			FeatureFlagManagerUtil.class, FeatureFlagManager.class);
	private static final Snapshot<JSONFactory> _jsonFactorySnapshot =
		new Snapshot<>(FeatureFlagManagerUtil.class, JSONFactory.class);

}