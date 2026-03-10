/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.internal.model;

import com.liferay.portal.kernel.util.PortalRunMode;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.vertexai.gemini.VertexAiGeminiStreamingChatModel;

/**
 * @author João Victor Alves
 */
public class ChatModelFactory {

	public static StreamingChatModel create() {
		if (PortalRunMode.isTestMode()) {
			return OllamaStreamingChatModel.builder(
			).baseUrl(
				"http://localhost:11434"
			).modelName(
				"qwen2.5:7b"
			).build();
		}

		return VertexAiGeminiStreamingChatModel.builder(
		).location(
			"europe-central2"
		).modelName(
			"gemini-2.5-flash-lite"
		).project(
			"ai-hub-liferay"
		).build();
	}

}