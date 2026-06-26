/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.content.site.generator.internal.workflow.node.delegate;

import com.liferay.ai.hub.rest.resource.v1_0.util.SseUtil;
import com.liferay.ai.hub.workflow.node.ServiceNodeDelegate;
import com.liferay.content.site.generator.internal.workflow.node.delegate.util.SitePlanBatchFileBuilder;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.io.Serializable;

import java.nio.charset.StandardCharsets;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Mario Gomes
 * @author Iliyan Peychev
 */
@Component(service = ServiceNodeDelegate.class)
public class WriteCSGGenerationItemsServiceNodeDelegate
	implements ServiceNodeDelegate {

	@Override
	public String execute(
			Map<String, String> inputVariables,
			Map<String, Serializable> workflowContext)
		throws Exception {

		String generationExternalReferenceCode = inputVariables.get(
			"generationExternalReferenceCode");

		if (Validator.isBlank(generationExternalReferenceCode)) {
			throw new IllegalArgumentException(
				"The \"generationExternalReferenceCode\" input variable is " +
					"required");
		}

		long companyId = _getCompanyId(workflowContext);
		long userId = _getUserId(workflowContext);

		String sseEventSinkKey = GetterUtil.getString(
			workflowContext.get("sseEventSinkKey"));

		ObjectDefinition csgGenerationObjectDefinition =
			_objectDefinitionLocalService.
				getObjectDefinitionByExternalReferenceCode(
					"L_CSG_GENERATION", companyId);

		ObjectEntry csgGenerationObjectEntry =
			_objectEntryLocalService.getObjectEntry(
				generationExternalReferenceCode, 0L,
				csgGenerationObjectDefinition.getObjectDefinitionId());

		try {
			_objectEntryLocalService.partialUpdateObjectEntry(
				userId, csgGenerationObjectEntry.getObjectEntryId(),
				csgGenerationObjectEntry.getObjectEntryFolderId(),
				HashMapBuilder.<String, Serializable>put(
					"generationStatus", "generating"
				).build(),
				new ServiceContext());

			String enrichedSitePlan = inputVariables.get("enrichedSitePlan");

			String blogEntries = _normalizeBlogEntries(
				inputVariables.get("blogEntries"));

			SitePlanBatchFileBuilder sitePlanBatchFileBuilder =
				new SitePlanBatchFileBuilder(enrichedSitePlan, blogEntries);

			List<SitePlanBatchFileBuilder.BatchFile> batchFiles =
				sitePlanBatchFileBuilder.build();

			ObjectDefinition csgGenerationItemObjectDefinition =
				_objectDefinitionLocalService.
					getObjectDefinitionByExternalReferenceCode(
						"L_CSG_GENERATION_ITEM", companyId);

			Company company = _companyLocalService.getCompany(companyId);

			Set<String> targetLanguages = new LinkedHashSet<>();

			int loadOrder = 0;

			for (SitePlanBatchFileBuilder.BatchFile batchFile : batchFiles) {
				loadOrder++;

				String languages = _writeGenerationItem(
					batchFile, company.getGroupId(),
					generationExternalReferenceCode,
					csgGenerationItemObjectDefinition.getObjectDefinitionId(),
					csgGenerationObjectEntry.getObjectEntryId(), loadOrder,
					userId);

				for (String language : StringUtil.split(languages, ',')) {
					String trimmedLanguage = StringUtil.trim(language);

					if (Validator.isNotNull(trimmedLanguage)) {
						targetLanguages.add(trimmedLanguage);
					}
				}

				if (Validator.isNotNull(sseEventSinkKey)) {
					SseUtil.send(
						batchFile.getFileName(), "Artifacts Updated", null,
						sseEventSinkKey);
				}
			}

			_objectEntryLocalService.partialUpdateObjectEntry(
				userId, csgGenerationObjectEntry.getObjectEntryId(),
				csgGenerationObjectEntry.getObjectEntryFolderId(),
				HashMapBuilder.<String, Serializable>put(
					"targetLanguages", StringUtil.merge(targetLanguages, ",")
				).build(),
				new ServiceContext());

			return StringBundler.concat(
				"Wrote ", batchFiles.size(), " generation items.");
		}
		catch (Exception exception1) {
			_log.error(
				"Unable to write CSG generation items for generation " +
					generationExternalReferenceCode,
				exception1);

			try {
				_objectEntryLocalService.partialUpdateObjectEntry(
					userId, csgGenerationObjectEntry.getObjectEntryId(),
					csgGenerationObjectEntry.getObjectEntryFolderId(),
					HashMapBuilder.<String, Serializable>put(
						"failureReason", exception1.getMessage()
					).put(
						"generationStatus", "failed"
					).build(),
					new ServiceContext());
			}
			catch (Exception exception2) {
				_log.error(exception2);
			}

			throw exception1;
		}
	}

	@Override
	public String getKey() {
		return "javaDelegate#writeCSGGenerationItems";
	}

	private long _addBatchFileEntry(
			long groupId, long userId, String generationExternalReferenceCode,
			int loadOrder, String fileName, JSONObject envelopeJSONObject)
		throws Exception {

		String sourceFileName = StringBundler.concat(
			generationExternalReferenceCode, "-", loadOrder, "-", fileName);

		return GetterUtil.getLong(
			_dlAppLocalService.addFileEntry(
				null, userId, groupId, 0L, sourceFileName,
				ContentTypes.APPLICATION_JSON,
				envelopeJSONObject.toString(
				).getBytes(
					StandardCharsets.UTF_8
				),
				null, null, null, new ServiceContext()
			).getFileEntryId());
	}

	private long _getCompanyId(Map<String, Serializable> workflowContext) {
		ServiceContext serviceContext = (ServiceContext)workflowContext.get(
			WorkflowConstants.CONTEXT_SERVICE_CONTEXT);

		if ((serviceContext != null) && (serviceContext.getCompanyId() > 0)) {
			return serviceContext.getCompanyId();
		}

		return CompanyThreadLocal.getCompanyId();
	}

	private long _getUserId(Map<String, Serializable> workflowContext) {
		ServiceContext serviceContext = (ServiceContext)workflowContext.get(
			WorkflowConstants.CONTEXT_SERVICE_CONTEXT);

		if (serviceContext != null) {
			return serviceContext.getUserId();
		}

		return 0;
	}

	private String _normalizeBlogEntries(String blogEntries) {
		if (Validator.isBlank(blogEntries)) {
			return null;
		}

		String trimmedBlogEntries = blogEntries.trim();

		if (!trimmedBlogEntries.startsWith("[") &&
			!trimmedBlogEntries.startsWith("{")) {

			return null;
		}

		return trimmedBlogEntries;
	}

	private String _writeGenerationItem(
			SitePlanBatchFileBuilder.BatchFile batchFile, long groupId,
			String generationExternalReferenceCode, long objectDefinitionId,
			long csgGenerationId, int loadOrder, long userId)
		throws Exception {

		String fileName = batchFile.getFileName();

		JSONObject envelopeJSONObject = batchFile.getEnvelopeJSONObject();

		JSONArray itemsJSONArray = envelopeJSONObject.getJSONArray("items");

		int itemCount = (itemsJSONArray == null) ? 0 : itemsJSONArray.length();

		String previewItem = "";

		if (itemCount > 0) {
			previewItem = String.valueOf(
				SitePlanBatchFileBuilder.stripMetadata(
					itemsJSONArray.getJSONObject(0)));
		}

		String languages = SitePlanBatchFileBuilder.detectLanguages(
			itemsJSONArray, fileName);

		long fileEntryId = _addBatchFileEntry(
			groupId, userId, generationExternalReferenceCode, loadOrder,
			fileName, envelopeJSONObject);

		_objectEntryLocalService.addObjectEntry(
			0L, userId, objectDefinitionId, 0L, "en_US",
			HashMapBuilder.<String, Serializable>put(
				"batchFile", fileEntryId
			).put(
				"fileName", fileName
			).put(
				"itemCount", itemCount
			).put(
				"languages", languages
			).put(
				"loadOrder", loadOrder
			).put(
				"previewItem", previewItem
			).put(
				"r_items_l_csgGenerationId", csgGenerationId
			).build(),
			new ServiceContext());

		return languages;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		WriteCSGGenerationItemsServiceNodeDelegate.class);

	@Reference
	private CompanyLocalService _companyLocalService;

	@Reference
	private DLAppLocalService _dlAppLocalService;

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference
	private ObjectEntryLocalService _objectEntryLocalService;

}