/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.internal.model.util;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import dev.langchain4j.model.chat.StreamingChatModel;

import java.io.Closeable;

/**
 * @author João Victor Alves
 */
public class StreamingChatModelUtil {

	public static void close(StreamingChatModel streamingChatModel) {
		try {
			if (streamingChatModel instanceof Closeable) {
				Closeable closeable = (Closeable)streamingChatModel;

				closeable.close();
			}
		}
		catch (Exception exception) {
			_log.error(exception);
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		StreamingChatModelUtil.class);

}