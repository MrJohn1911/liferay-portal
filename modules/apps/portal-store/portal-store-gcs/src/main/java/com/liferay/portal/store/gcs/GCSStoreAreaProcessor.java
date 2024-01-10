/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.store.gcs;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageBatch;
import com.google.cloud.storage.StorageException;

import com.liferay.document.library.kernel.store.Store;
import com.liferay.document.library.kernel.store.StoreArea;
import com.liferay.document.library.kernel.store.StoreAreaProcessor;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.feature.flag.FeatureFlagManagerUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.store.gcs.configuration.GCSStoreConfiguration;

import java.time.Instant;
import java.time.temporal.TemporalAmount;

import java.util.function.Predicate;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Joao Victor Alves
 */
@Component(service = StoreAreaProcessor.class)
public class GCSStoreAreaProcessor implements StoreAreaProcessor {

	@Override
	public String cleanUpDeletedStoreArea(
		long companyId, int deletionQuota, Predicate<String> predicate,
		String startOffset, TemporalAmount temporalAmount) {

		return _processStoreArea(
			companyId, deletionQuota, blob -> predicate.test(blob.getName()),
			startOffset, StoreArea.DELETED, temporalAmount);
	}

	@Override
	public String cleanUpNewStoreArea(
		long companyId, int evictionQuota, Predicate<String> predicate,
		String startOffset, TemporalAmount temporalAmount) {

		return _processStoreArea(
			companyId, evictionQuota,
			blob -> {
				if (predicate.test(blob.getName())) {
					return copy(
						blob.getName(),
						StoreArea.NEW.relocate(
							blob.getName(), StoreArea.DELETED));
				}

				return copy(
					blob.getName(),
					StoreArea.NEW.relocate(blob.getName(), StoreArea.LIVE));
			},
			startOffset, StoreArea.NEW, temporalAmount);
	}

	@Override
	public boolean copy(String sourceFileName, String destinationFileName) {
		try {
			if (!FeatureFlagManagerUtil.isEnabled("LPS-174816")) {
				return true;
			}

			GCSStoreConfiguration gcsStoreConfiguration =
				_getGCSStoreConfiguration();

			Storage storage = _getGCSStorage();

			CopyWriter copyWriter = storage.copy(
				Storage.CopyRequest.newBuilder(
				).setSource(
					gcsStoreConfiguration.bucketName(), sourceFileName
				).setTarget(
					BlobId.of(
						gcsStoreConfiguration.bucketName(), destinationFileName)
				).build());

			while (!copyWriter.isDone()) {
				copyWriter.copyChunk();
			}

			return true;
		}
		catch (StorageException storageException) {
			if (_log.isInfoEnabled()) {
				_log.info(storageException);
			}

			return false;
		}
	}

	@Override
	public boolean copyDirectory(
		long companyId, long repositoryId, String dirName,
		StoreArea[] sourceStoreAreas, StoreArea destinationStoreArea) {

		try {
			if (!FeatureFlagManagerUtil.isEnabled("LPS-174816")) {
				return true;
			}

			for (StoreArea sourceStoreArea : sourceStoreAreas) {
				String[] filePaths = StoreArea.withStoreArea(
					sourceStoreArea,
					() -> _getFilePaths(companyId, repositoryId, dirName));

				for (String filePath : filePaths) {
					copy(
						filePath,
						sourceStoreArea.relocate(
							filePath, destinationStoreArea));
				}
			}

			return true;
		}
		catch (StorageException storageException) {
			if (_log.isInfoEnabled()) {
				_log.info(storageException);
			}

			return false;
		}
	}

	private String[] _getFilePaths(
		long companyId, long repositoryId, String dirName) {

		GCSStoreConfiguration gcsStoreConfiguration =
			_getGCSStoreConfiguration();

		GCSStore gcsStore = (GCSStore)_store;

		Storage storage = _getGCSStorage();

		return gcsStore.getFilePaths(
			storage.get(gcsStoreConfiguration.bucketName()), companyId,
			repositoryId, dirName);
	}

	private Storage _getGCSStorage() {
		GCSStore gcsStore = (GCSStore)_store;

		return gcsStore.getGcsStorage();
	}

	private GCSStoreConfiguration _getGCSStoreConfiguration() {
		GCSStore gcsStore = (GCSStore)_store;

		return gcsStore.getGcsStoreConfiguration();
	}

	private String _processStoreArea(
		long companyId, int evictionQuota, Predicate<Blob> predicate,
		String startOffset, StoreArea storeArea,
		TemporalAmount temporalAmount) {

		if (!FeatureFlagManagerUtil.isEnabled("LPS-174816")) {
			return StringPool.BLANK;
		}

		GCSStoreConfiguration gcsStoreConfiguration =
			_getGCSStoreConfiguration();

		Storage storage = _getGCSStorage();

		Bucket bucket = storage.get(gcsStoreConfiguration.bucketName());

		int evictedBlobQuota = Math.max(evictionQuota, 1);
		int evictedBlobsCount = 0;
		Instant instant = Instant.now();
		String lastVisitedBlobName = startOffset;

		StorageBatch storageBatch = storage.batch();
		int pageSize = Math.max(evictedBlobQuota * 2, 10);
		int visitedPageLimit = Math.max(evictedBlobQuota / 10, 10);

		while ((evictedBlobQuota > 0) && (visitedPageLimit > 0)) {
			boolean emptyPage = true;

			Page<Blob> blobPage = bucket.list(
				Storage.BlobListOption.fields(
					Storage.BlobField.ID, Storage.BlobField.NAME,
					Storage.BlobField.UPDATED),
				Storage.BlobListOption.pageSize(pageSize),
				Storage.BlobListOption.prefix(storeArea.getPath(companyId)),
				Storage.BlobListOption.startOffset(lastVisitedBlobName));

			for (Blob blob : blobPage.getValues()) {
				Instant updateTimeInstant = Instant.ofEpochMilli(
					blob.getUpdateTime());

				Instant evictionInstant = updateTimeInstant.plus(
					temporalAmount);

				if (evictionInstant.isBefore(instant) && predicate.test(blob)) {
					storageBatch.delete(blob.getBlobId());

					evictedBlobQuota--;
					evictedBlobsCount++;
				}

				emptyPage = false;

				lastVisitedBlobName = blob.getName();
			}

			if (evictedBlobsCount >= _EVICTED_BATCH_SIZE) {
				storageBatch.submit();

				evictedBlobsCount = 0;

				storageBatch = storage.batch();
			}

			if (emptyPage) {
				lastVisitedBlobName = StringPool.BLANK;

				break;
			}

			visitedPageLimit--;
		}

		if (evictedBlobsCount > 0) {
			storageBatch.submit();
		}

		return lastVisitedBlobName;
	}

	private static final int _EVICTED_BATCH_SIZE = 10;

	private static final Log _log = LogFactoryUtil.getLog(
		GCSStoreAreaProcessor.class);

	@Reference(target = "(store.type=com.liferay.portal.store.gcs.GCSStore)")
	private Store _store;

}