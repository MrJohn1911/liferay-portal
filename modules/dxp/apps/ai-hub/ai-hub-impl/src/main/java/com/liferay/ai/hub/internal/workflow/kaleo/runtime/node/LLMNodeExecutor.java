/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.internal.workflow.kaleo.runtime.node;

import com.google.cloud.modelarmor.v1.DataItem;
import com.google.cloud.modelarmor.v1.FilterMatchState;
import com.google.cloud.modelarmor.v1.ModelArmorClient;
import com.google.cloud.modelarmor.v1.ModelArmorSettings;
import com.google.cloud.modelarmor.v1.SanitizationResult;
import com.google.cloud.modelarmor.v1.SanitizeModelResponseRequest;
import com.google.cloud.modelarmor.v1.SanitizeModelResponseResponse;
import com.google.cloud.modelarmor.v1.SanitizeUserPromptRequest;
import com.google.cloud.modelarmor.v1.SanitizeUserPromptResponse;
import com.google.cloud.modelarmor.v1.Template;

import com.liferay.ai.hub.internal.assistant.handler.AssistantHandlerContext;
import com.liferay.ai.hub.internal.assistant.handler.AssistantHandlerUtil;
import com.liferay.ai.hub.internal.mcp.tool.provider.MCPToolProviderUtil;
import com.liferay.ai.hub.internal.model.VertexAiGeminiStreamingChatModelUtil;
import com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util.KaleoLogUtil;
import com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util.ModelArmorTemplateUtil;
import com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util.PromptUtil;
import com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util.RetrievalAugmentorUtil;
import com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util.ToolsUtil;
import com.liferay.ai.hub.internal.workflow.kaleo.runtime.node.util.VariablesUtil;
import com.liferay.ai.hub.rest.resource.v1_0.util.SseUtil;
import com.liferay.dynamic.data.mapping.expression.CreateExpressionRequest;
import com.liferay.dynamic.data.mapping.expression.DDMExpression;
import com.liferay.dynamic.data.mapping.expression.DDMExpressionFactory;
import com.liferay.dynamic.data.mapping.expression.DDMExpressionFieldAccessor;
import com.liferay.dynamic.data.mapping.expression.GetFieldPropertyRequest;
import com.liferay.dynamic.data.mapping.expression.GetFieldPropertyResponse;
import com.liferay.object.constants.ObjectDefinitionConstants;
import com.liferay.object.rest.manager.v1_0.ObjectEntryManager;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.auth.CompanyInheritableThreadLocalCallable;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowNodeManager;
import com.liferay.portal.search.engine.adapter.SearchEngineAdapter;
import com.liferay.portal.search.highlight.FieldConfigBuilderFactory;
import com.liferay.portal.search.highlight.HighlightBuilderFactory;
import com.liferay.portal.vulcan.dto.converter.DTOConverterRegistry;
import com.liferay.portal.workflow.kaleo.definition.NodeType;
import com.liferay.portal.workflow.kaleo.model.KaleoInstanceToken;
import com.liferay.portal.workflow.kaleo.model.KaleoNode;
import com.liferay.portal.workflow.kaleo.model.KaleoNodeSetting;
import com.liferay.portal.workflow.kaleo.model.KaleoTransition;
import com.liferay.portal.workflow.kaleo.runtime.ExecutionContext;
import com.liferay.portal.workflow.kaleo.runtime.graph.PathElement;
import com.liferay.portal.workflow.kaleo.runtime.node.BaseNodeExecutor;
import com.liferay.portal.workflow.kaleo.runtime.node.NodeExecutor;
import com.liferay.portal.workflow.kaleo.service.KaleoNodeSettingLocalService;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.vertexai.gemini.VertexAiGeminiStreamingChatModel;

import java.io.IOException;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Feliphe Marinho
 */
@Component(service = NodeExecutor.class)
public class LLMNodeExecutor extends BaseNodeExecutor {

	@Override
	public NodeType getNodeType() {
		return NodeType.LLM;
	}

	public static class MessageDDMExpressionFieldAccessor
		implements DDMExpressionFieldAccessor {

		public MessageDDMExpressionFieldAccessor(String message) {
			_message = message;
		}

		@Override
		public GetFieldPropertyResponse getFieldProperty(
			GetFieldPropertyRequest getFieldPropertyRequest) {

			GetFieldPropertyResponse.Builder builder =
				GetFieldPropertyResponse.Builder.newBuilder(
					isField(getFieldPropertyRequest.getField()) ? _message :
						null);

			return builder.build();
		}

		@Override
		public boolean isField(String parameter) {
			return parameter.equals("message");
		}

		private final String _message;

	}

	public class DDMExpressionInputGuardrail implements InputGuardrail {

		public DDMExpressionInputGuardrail(String expression) {
			_expression = expression;
		}

		@Override
		public InputGuardrailResult validate(UserMessage userMessage) {
			try {
				DDMExpression<Boolean> ddmExpression =
					_ddmExpressionFactory.createExpression(
						CreateExpressionRequest.Builder.newBuilder(
							_expression
						).withDDMExpressionFieldAccessor(
							new MessageDDMExpressionFieldAccessor(
								userMessage.singleText())
						).build());

				if (ddmExpression.evaluate()) {
					return fatal(
						"Input rejected: Blocked by guardrail expression.");
				}

				return success();
			}
			catch (Exception exception) {
				_log.error(exception);

				return fatal(
					"Input rejected: Guardrail expression evaluation failed.");
			}
		}

		private final String _expression;

	}

	public class DDMExpressionOutputGuardrail implements OutputGuardrail {

		public DDMExpressionOutputGuardrail(String expression) {
			_expression = expression;
		}

		@Override
		public OutputGuardrailResult validate(AiMessage aiMessage) {
			try {
				DDMExpression<Boolean> ddmExpression =
					_ddmExpressionFactory.createExpression(
						CreateExpressionRequest.Builder.newBuilder(
							_expression
						).withDDMExpressionFieldAccessor(
							new MessageDDMExpressionFieldAccessor(
								aiMessage.text())
						).build());

				if (ddmExpression.evaluate()) {
					return fatal(
						"Response blocked: Blocked by guardrail expression.");
				}

				return success();
			}
			catch (Exception exception) {
				_log.error(exception);

				return fatal(
					"Response blocked: Guardrail expression evaluation " +
						"failed.");
			}
		}

		private final String _expression;

	}

	public class ModelArmorInputGuardrail implements InputGuardrail {

		public ModelArmorInputGuardrail(String templateName) {
			_templateName = templateName;
		}

		@Override
		public InputGuardrailResult validate(UserMessage userMessage) {
			try (ModelArmorClient modelArmorClient = ModelArmorClient.create(
					ModelArmorSettings.newBuilder(
					).setEndpoint(
						"modelarmor.europe-southwest1.rep.googleapis.com:443"
					).build())) {

				Template template = ModelArmorTemplateUtil.getOrCreate(
					modelArmorClient, _templateName);

				if (template == null) {
					return success();
				}

				DataItem promptData = DataItem.newBuilder(
				).setText(
					userMessage.singleText()
				).build();

				SanitizeUserPromptRequest request =
					SanitizeUserPromptRequest.newBuilder(
					).setName(
						template.getName()
					).setUserPromptData(
						promptData
					).build();

				SanitizeUserPromptResponse response =
					modelArmorClient.sanitizeUserPrompt(request);

				SanitizationResult sanitizationResult =
					response.getSanitizationResult();

				if (sanitizationResult.getFilterMatchState() ==
						FilterMatchState.MATCH_FOUND) {

					return fatal(
						"Input rejected: Security policy violation detected.");
				}

				return success();
			}
			catch (IOException ioException) {
				_log.error(ioException);

				return fatal(
					"Input rejected: Security policy violation detected.");
			}
		}

		private final String _templateName;

	}

	public class ModelArmorOutputGuardrail implements OutputGuardrail {

		public ModelArmorOutputGuardrail(String templateName) {
			_templateName = templateName;
		}

		@Override
		public OutputGuardrailResult validate(AiMessage aiMessage) {
			try (ModelArmorClient modelArmorClient = ModelArmorClient.create(
					ModelArmorSettings.newBuilder(
					).setEndpoint(
						"modelarmor.europe-southwest1.rep.googleapis.com:443"
					).build())) {

				Template template = ModelArmorTemplateUtil.getOrCreate(
					modelArmorClient, _templateName);

				if (template == null) {
					return success();
				}

				SanitizeModelResponseRequest request =
					SanitizeModelResponseRequest.newBuilder(
					).setName(
						template.getName()
					).setModelResponseData(
						DataItem.newBuilder(
						).setText(
							aiMessage.text()
						).build()
					).build();

				SanitizeModelResponseResponse response =
					modelArmorClient.sanitizeModelResponse(request);

				SanitizationResult sanitizationResult =
					response.getSanitizationResult();

				if (sanitizationResult.getFilterMatchState() ==
						FilterMatchState.MATCH_FOUND) {

					return fatal(
						"Response blocked: Contains restricted content.");
				}

				return success();
			}
			catch (IOException ioException) {
				_log.error(ioException);

				return fatal("Response blocked: Contains restricted content.");
			}
		}

		private final String _templateName;

	}

	@Override
	protected boolean doEnter(
		KaleoNode currentKaleoNode, ExecutionContext executionContext) {

		return true;
	}

	@Override
	protected void doExecute(
			KaleoNode currentKaleoNode, ExecutionContext executionContext,
			List<PathElement> remainingPathElements)
		throws PortalException {

		KaleoInstanceToken kaleoInstanceToken =
			executionContext.getKaleoInstanceToken();

		Map<String, String> kaleoNodeSettingValues = new HashMap<>();

		List<KaleoNodeSetting> kaleoNodeSettings =
			_kaleoNodeSettingLocalService.getKaleoNodeSettings(
				currentKaleoNode.getKaleoNodeId());

		for (KaleoNodeSetting kaleoNodeSetting : kaleoNodeSettings) {
			kaleoNodeSettingValues.put(
				kaleoNodeSetting.getName(), kaleoNodeSetting.getValue());
		}

		String prompt = PromptUtil.composePrompt(
			kaleoInstanceToken.getCompanyId(), _dtoConverterRegistry,
			executionContext, kaleoNodeSettingValues, _objectEntryManager);
		String userMessage = VariablesUtil.applyInputVariables(
			executionContext, "userMessage", kaleoNodeSettingValues);

		ServiceContext serviceContext = executionContext.getServiceContext();

		VertexAiGeminiStreamingChatModel vertexAiGeminiStreamingChatModel =
			VertexAiGeminiStreamingChatModelUtil.create(
				serviceContext.getCompanyId());

		Map<String, Serializable> workflowContext =
			executionContext.getWorkflowContext();

		AtomicReference<ChatResponse> chatResponseAtomicReference =
			new AtomicReference<>();

		Callable<Void> completeResponseCallable =
			new CompanyInheritableThreadLocalCallable<>(
				() -> {
					_completeResponse(
						chatResponseAtomicReference.get(), executionContext,
						currentKaleoNode, kaleoNodeSettingValues, prompt,
						userMessage);

					return null;
				});

		String sseEventSinkKey = GetterUtil.getString(
			workflowContext.get("sseEventSinkKey"));

		List<InputGuardrail> inputGuardrails = new ArrayList<>();
		List<OutputGuardrail> outputGuardrails = new ArrayList<>();

		String templateName = "ai-hub-model-armor";

		if (Validator.isNotNull(templateName)) {
			inputGuardrails.add(new ModelArmorInputGuardrail(templateName));
			outputGuardrails.add(new ModelArmorOutputGuardrail(templateName));
		}

		String ddmInputExpression = "contains(message, \"death\")";

		if (Validator.isNotNull(ddmInputExpression)) {
			inputGuardrails.add(
				new DDMExpressionInputGuardrail(ddmInputExpression));
		}

		String ddmOutputExpression = "";

		if (Validator.isNotNull(ddmOutputExpression)) {
			outputGuardrails.add(
				new DDMExpressionOutputGuardrail(ddmOutputExpression));
		}

		AssistantHandlerUtil.handle(
			AssistantHandlerContext.builder(
			).inputGuardrails(
				inputGuardrails
			).outputGuardrails(
				outputGuardrails
			).invocationParameters(
				InvocationParameters.from(
					Map.of(
						"executionContext", executionContext,
						"permissionChecker",
						PermissionThreadLocal.getPermissionChecker()))
			).memoryId(
				GetterUtil.getString(workflowContext.get("memoryId"))
			).onCompleteResponseConsumer(
				response -> {
					chatResponseAtomicReference.set(response);

					try {
						completeResponseCallable.call();
					}
					catch (Exception exception) {
						throw new RuntimeException(exception);
					}
					finally {
						MCPToolProviderUtil.close(sseEventSinkKey);

						vertexAiGeminiStreamingChatModel.close();
					}
				}
			).onErrorConsumer(
				throwable -> {
					MCPToolProviderUtil.close(sseEventSinkKey);

					vertexAiGeminiStreamingChatModel.close();

					_log.error(throwable);
				}
			).retrievalAugmentor(
				RetrievalAugmentorUtil.createRetrievalAugmentor(
					kaleoInstanceToken.getCompanyId(), _dtoConverterRegistry,
					_fieldConfigBuilderFactory, _highlightBuilderFactory,
					kaleoNodeSettingValues, serviceContext.getLocale(),
					_objectEntryManager, _searchEngineAdapter,
					serviceContext.getUserId(), workflowContext)
			).systemMessageProviderFunction(
				memoryId -> prompt
			).toolProvider(
				MCPToolProviderUtil.create(
					kaleoInstanceToken.getCompanyId(), _dtoConverterRegistry,
					kaleoInstanceToken.getGroupId(), serviceContext.getLocale(),
					ToolsUtil.getMCPServerExternalReferenceCodes(
						_jsonFactory, kaleoNodeSettingValues),
					_objectEntryManager, sseEventSinkKey,
					serviceContext.getUserId())
			).userMessage(
				userMessage
			).vertexAiGeminiStreamingChatModel(
				vertexAiGeminiStreamingChatModel
			).build());
	}

	@Override
	protected void doExit(
			KaleoNode currentKaleoNode, ExecutionContext executionContext,
			List<PathElement> remainingPathElements)
		throws PortalException {

		KaleoTransition kaleoTransition = null;

		if (Validator.isNull(executionContext.getTransitionName())) {
			kaleoTransition = currentKaleoNode.getDefaultKaleoTransition();
		}
		else {
			kaleoTransition = currentKaleoNode.getKaleoTransition(
				executionContext.getTransitionName());
		}

		remainingPathElements.add(
			new PathElement(
				null, kaleoTransition.getTargetKaleoNode(),
				new ExecutionContext(
					executionContext.getKaleoInstanceToken(),
					executionContext.getWorkflowContext(),
					executionContext.getServiceContext())));
	}

	private void _completeResponse(
		ChatResponse chatResponse, ExecutionContext executionContext,
		KaleoNode kaleoNode, Map<String, String> kaleoNodeSettingValues,
		String prompt, String userMessage) {

		AiMessage aiMessage = chatResponse.aiMessage();

		Map<String, Serializable> workflowContext =
			executionContext.getWorkflowContext();

		JSONArray jsonArray = VariablesUtil.getVariablesJSONArray(
			"outputVariables", kaleoNodeSettingValues);

		if ((jsonArray != null) && (jsonArray.length() > 0)) {
			JSONObject jsonObject = jsonArray.getJSONObject(0);

			workflowContext.put(jsonObject.getString("name"), aiMessage.text());
		}

		workflowContext.put("output", aiMessage.text());

		SseUtil.send(
			aiMessage.text(),
			GetterUtil.getString(workflowContext.get("outBoundEventName")),
			kaleoNode.getName(),
			GetterUtil.getString(workflowContext.get("sseEventSinkKey")));

		KaleoInstanceToken kaleoInstanceToken =
			executionContext.getKaleoInstanceToken();

		KaleoLogUtil.addNodeUsageKaleoLog(
			chatResponse, kaleoInstanceToken, aiMessage.text(), prompt,
			executionContext.getServiceContext(), userMessage);

		List<KaleoTransition> kaleoTransitions =
			kaleoNode.getKaleoTransitions();

		KaleoTransition kaleoTransition = kaleoTransitions.get(0);

		try {
			_workflowNodeManager.completeWorkflowNode(
				kaleoInstanceToken.getCompanyId(),
				kaleoInstanceToken.getUserId(),
				kaleoInstanceToken.getKaleoInstanceTokenId(),
				kaleoTransition.getName(), workflowContext, false);
		}
		catch (PortalException portalException) {
			throw new RuntimeException(portalException);
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		LLMNodeExecutor.class);

	@Reference
	private DDMExpressionFactory _ddmExpressionFactory;

	@Reference
	private DTOConverterRegistry _dtoConverterRegistry;

	@Reference
	private FieldConfigBuilderFactory _fieldConfigBuilderFactory;

	@Reference
	private HighlightBuilderFactory _highlightBuilderFactory;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private KaleoNodeSettingLocalService _kaleoNodeSettingLocalService;

	@Reference(
		target = "(object.entry.manager.storage.type=" + ObjectDefinitionConstants.STORAGE_TYPE_DEFAULT + ")"
	)
	private ObjectEntryManager _objectEntryManager;

	@Reference
	private SearchEngineAdapter _searchEngineAdapter;

	@Reference
	private WorkflowNodeManager _workflowNodeManager;

}