/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.frontend.js.importmaps.extender.internal.servlet.taglib.util;

import com.liferay.frontend.js.importmaps.extender.internal.servlet.taglib.JSImportMapsRegistration;
import com.liferay.petra.concurrent.DCLSingleton;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Joao Victor Alves
 */
public class JSImportMapsRegistryUtil {

	public static String getImportMaps(JSONFactory jsonFactory) {
		return _importMaps.getSingleton(
			() -> JSImportMapsRegistryUtil.rebuildImportMaps(jsonFactory));
	}

	public static boolean isEmptyGlobalImportMapJSONObjects() {
		return _globalImportMapJSONObjects.isEmpty();
	}

	public static boolean isEmptyScopedImportMapJSONObjects() {
		return _scopedImportMapJSONObjects.isEmpty();
	}

	private static String rebuildImportMaps(JSONFactory jsonFactory) {
		JSONObject jsonObject = jsonFactory.createJSONObject();

		jsonObject.put(
			"imports",
			() -> {
				JSONObject importsJSONObject = jsonFactory.createJSONObject();

				for (JSONObject globalImportMapJSONObject :
						_globalImportMapJSONObjects.values()) {

					for (String key : globalImportMapJSONObject.keySet()) {
						importsJSONObject.put(
							key, globalImportMapJSONObject.getString(key));
					}
				}

				return importsJSONObject;
			}
		).put(
			"scopes",
			() -> {
				JSONObject scopesJSONObject = jsonFactory.createJSONObject();

				for (Map.Entry<String, JSONObject> entry :
						_scopedImportMapJSONObjects.entrySet()) {

					scopesJSONObject.put(entry.getKey(), entry.getValue());
				}

				return scopesJSONObject;
			}
		);

		return jsonFactory.looseSerializeDeep(jsonObject);
	}

	public static <T> JSImportMapsRegistration register(
		String scope, JSONObject jsonObject) {

		if (scope == null) {
			return createJSImportMapsRegistration(
				_nextGlobalId.getAndIncrement(), _globalImportMapJSONObjects,
				jsonObject);
		}

		return createJSImportMapsRegistration(
			scope, _scopedImportMapJSONObjects, jsonObject);
	}

	private static <T> JSImportMapsRegistration createJSImportMapsRegistration(
		T key, Map<T, JSONObject> map, JSONObject jsonObject) {

			map.put(key, jsonObject);

			_importMaps.destroy(string -> {});

			return () -> {
				_importMaps.destroy(string -> {});

				map.remove(key);
			};
	}

	private static final ConcurrentMap<Long, JSONObject>
		_globalImportMapJSONObjects = new ConcurrentHashMap<>();
	private static final DCLSingleton<String> _importMaps =
		new DCLSingleton<>();
	private static final AtomicLong _nextGlobalId = new AtomicLong();
	private static final ConcurrentMap<String, JSONObject>
		_scopedImportMapJSONObjects = new ConcurrentHashMap<>();

}