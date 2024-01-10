/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.store.gcs;

import com.google.api.gax.paging.Page;
import com.google.api.gax.retrying.RetrySettings;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageBatch;
import com.google.cloud.storage.StorageBatchResult;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;

import com.liferay.document.library.kernel.exception.NoSuchFileException;
import com.liferay.document.library.kernel.store.Store;
import com.liferay.document.library.kernel.store.StoreArea;
import com.liferay.document.library.kernel.util.comparator.VersionNumberComparator;
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.io.StreamUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.store.gcs.configuration.GCSStoreConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.channels.Channels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

import org.threeten.bp.Duration;

/**
 * @author Shanon Mathai
 * @author Alicia García
 * @author Adolfo Pérez
 */
@Component(
	configurationPid = "com.liferay.portal.store.gcs.configuration.GCSStoreConfiguration",
	configurationPolicy = ConfigurationPolicy.REQUIRE,
	property = "store.type=com.liferay.portal.store.gcs.GCSStore",
	service = Store.class
)
public class GCSStore implements Store {

	@Override
	public void addFile(
			long companyId, long repositoryId, String fileName,
			String versionLabel, InputStream inputStream)
		throws PortalException {

		if (hasFile(companyId, repositoryId, fileName, versionLabel)) {
			deleteFile(companyId, repositoryId, fileName, versionLabel);
		}

		String path = _getFileVersionKey(
			companyId, repositoryId, fileName, versionLabel);

		BlobInfo blobInfo = BlobInfo.newBuilder(
			_getBucketInfo(), path
		).build();

		try (WriteChannel writeChannel = _getWriteChannel(blobInfo)) {
			StreamUtil.transfer(
				inputStream, Channels.newOutputStream(writeChannel));
		}
		catch (IOException ioException) {
			throw new PortalException("Unable to add file", ioException);
		}
	}

	@Override
	public void deleteDirectory(
		long companyId, long repositoryId, String dirName) {

		String path = _getDirectoryKey(companyId, repositoryId, dirName);

		try {
			Page<Blob> blobPage = _gcsStorage.list(
				_gcsStoreConfiguration.bucketName(),
				Storage.BlobListOption.pageSize(_PAGE_SIZE),
				Storage.BlobListOption.prefix(path));

			Iterable<Blob> blobs = blobPage.iterateAll();

			List<StorageBatchResult<Boolean>> results = new ArrayList<>();

			StorageBatch storageBatch = _gcsStorage.batch();

			try {
				blobs.forEach(
					blob -> results.add(_deleteBlob(blob, storageBatch)));
			}
			finally {
				if (!results.isEmpty()) {
					storageBatch.submit();

					for (StorageBatchResult<Boolean> result : results) {
						if ((result == null) || !result.get()) {
							_log.error(
								StringBundler.concat(
									"Error deleting objects in bucket ",
									_gcsStoreConfiguration.bucketName(), " at ",
									path));

							break;
						}
					}
				}
			}
		}
		catch (StorageException storageException) {
			_log.error("Unable to delete " + path, storageException);
		}
	}

	@Override
	public void deleteFile(
		long companyId, long repositoryId, String fileName,
		String versionLabel) {

		_gcsStorage.delete(
			BlobId.of(
				_gcsStoreConfiguration.bucketName(),
				_getHeadVersionLabel(
					companyId, repositoryId, fileName, versionLabel)));
	}

	@Override
	public InputStream getFileAsStream(
			long companyId, long repositoryId, String fileName,
			String versionLabel)
		throws NoSuchFileException {

		Blob blob = _gcsStorage.get(
			BlobId.of(
				_gcsStoreConfiguration.bucketName(),
				_getHeadVersionLabel(
					companyId, repositoryId, fileName, versionLabel)));

		if (blob == null) {
			throw new NoSuchFileException(
				companyId, repositoryId, fileName, versionLabel);
		}

		return Channels.newInputStream(_getReadChannel(blob));
	}

	@Override
	public String[] getFileNames(
		long companyId, long repositoryId, String dirName) {

		String prefix =
			StoreArea.getCurrentStoreAreaPath(companyId, repositoryId) +
				StringPool.SLASH;

		return TransformUtil.transform(
			getFilePaths(
				_gcsStorage.get(_gcsStoreConfiguration.bucketName()), companyId,
				repositoryId, dirName),
			filePath -> filePath.substring(
				filePath.indexOf(prefix) + prefix.length(),
				filePath.lastIndexOf(StringPool.SLASH)),
			String.class);
	}

	@Override
	public long getFileSize(
			long companyId, long repositoryId, String fileName,
			String versionLabel)
		throws PortalException {

		String headVersionLabel = _getHeadVersionLabel(
			companyId, repositoryId, fileName, versionLabel);

		Blob blob = _gcsStorage.get(
			BlobId.of(_gcsStoreConfiguration.bucketName(), headVersionLabel));

		if (blob == null) {
			throw new NoSuchFileException(
				"No file exists for " + headVersionLabel);
		}

		return blob.getSize();
	}

	@Override
	public String[] getFileVersions(
		long companyId, long repositoryId, String fileName) {

		return TransformUtil.transform(
			getFilePaths(
				_gcsStorage.get(_gcsStoreConfiguration.bucketName()), companyId,
				repositoryId, fileName),
			path -> {
				String[] parts = StringUtil.split(path, CharPool.SLASH);

				return parts[parts.length - 1];
			},
			String.class);
	}

	@Override
	public boolean hasFile(
		long companyId, long repositoryId, String fileName,
		String versionLabel) {

		String path = _getFileVersionKey(
			companyId, repositoryId, fileName, versionLabel);

		Page<Blob> blobPage = _gcsStorage.list(
			_gcsStoreConfiguration.bucketName(),
			Storage.BlobListOption.pageSize(1),
			Storage.BlobListOption.prefix(path));

		Iterable<Blob> filesFoundIterable = blobPage.getValues();

		Iterator<Blob> filesFoundIterator = filesFoundIterable.iterator();

		return filesFoundIterator.hasNext();
	}

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		try {
			_gcsStoreConfiguration = ConfigurableUtil.createConfigurable(
				GCSStoreConfiguration.class, properties);

			_initEncryption();

			_initGCSStore(_gcsStoreConfiguration);
		}
		catch (PortalException portalException) {
			throw new IllegalStateException(
				"Unable to initialize GCS store", portalException);
		}
	}

	protected String[] getFilePaths(
		Bucket bucket, long companyId, long repositoryId, String dirName) {

		List<String> filePaths = new ArrayList<>();

		String path = null;

		if (Validator.isNull(dirName) ||
			dirName.equals(StringPool.FORWARD_SLASH)) {

			path = StoreArea.getCurrentStoreAreaPath(companyId, repositoryId);
		}
		else {
			path = StoreArea.getCurrentStoreAreaPath(
				companyId, repositoryId, dirName);
		}

		Page<Blob> blobPage = bucket.list(Storage.BlobListOption.prefix(path));

		Iterable<Blob> blobs = blobPage.iterateAll();

		blobs.forEach(blob -> filePaths.add(blob.getName()));

		return filePaths.toArray(new String[0]);
	}

	protected Storage getGcsStorage() {
		return _gcsStorage;
	}

	protected GCSStoreConfiguration getGcsStoreConfiguration() {
		return _gcsStoreConfiguration;
	}

	private StorageBatchResult<Boolean> _deleteBlob(
		Blob blob, StorageBatch storageBatch) {

		if (_decryptStorageBlobSourceOption == null) {
			return storageBatch.delete(blob.getBlobId());
		}

		return storageBatch.delete(
			blob.getBlobId(), _decryptStorageBlobSourceOption);
	}

	private BucketInfo _getBucketInfo() {
		if (_bucketInfo == null) {
			_bucketInfo = BucketInfo.newBuilder(
				_gcsStoreConfiguration.bucketName()
			).build();
		}

		return _bucketInfo;
	}

	private String _getDirectoryKey(
		long companyId, long repositoryId, String folderName) {

		return _getFileKey(companyId, repositoryId, folderName);
	}

	private String _getFileKey(
		long companyId, long repositoryId, String fileName) {

		return StoreArea.getCurrentStoreAreaPath(
			companyId, repositoryId, fileName);
	}

	private String _getFileVersionKey(
		long companyId, long repositoryId, String fileName,
		String versionLabel) {

		return StoreArea.getCurrentStoreAreaPath(
			companyId, repositoryId, fileName, versionLabel);
	}

	private String _getHeadVersionLabel(
		long companyId, long repositoryId, String fileName,
		String versionLabel) {

		if (Validator.isNotNull(versionLabel)) {
			return _getFileVersionKey(
				companyId, repositoryId, fileName, versionLabel);
		}

		String path = _getFileKey(companyId, repositoryId, fileName);

		String[] fileNames = getFilePaths(
			_gcsStorage.get(_gcsStoreConfiguration.bucketName()), companyId,
			repositoryId, path);

		if ((fileNames == null) || (fileNames.length == 0)) {
			if (_log.isDebugEnabled()) {
				_log.debug("Using default version for " + path);
			}

			return _getFileVersionKey(
				companyId, repositoryId, fileName, VERSION_DEFAULT);
		}

		List<String> fileNamesList = Arrays.asList(fileNames);

		fileNamesList.sort(new VersionNumberComparator());

		return fileNamesList.get(fileNamesList.size() - 1);
	}

	private ReadChannel _getReadChannel(Blob blob) {
		if (_decryptBlobBlobSourceOption == null) {
			return blob.reader();
		}

		return blob.reader(_decryptBlobBlobSourceOption);
	}

	private WriteChannel _getWriteChannel(BlobInfo blobInfo) {
		if (_encryptStorageBlobWriteOption == null) {
			return _gcsStorage.writer(blobInfo);
		}

		return _gcsStorage.writer(blobInfo, _encryptStorageBlobWriteOption);
	}

	private void _initEncryption() {
		String aes256Key = _gcsStoreConfiguration.aes256Key();

		if (Validator.isNull(aes256Key)) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Files are not encrypted because the portal property " +
						"\"dl.store.gcs.aes256.key\" is not set");
			}

			_decryptBlobBlobSourceOption = null;
			_decryptStorageBlobSourceOption = null;
			_encryptStorageBlobWriteOption = null;
		}
		else {
			_decryptBlobBlobSourceOption = Blob.BlobSourceOption.decryptionKey(
				aes256Key);
			_decryptStorageBlobSourceOption =
				Storage.BlobSourceOption.decryptionKey(aes256Key);
			_encryptStorageBlobWriteOption =
				Storage.BlobWriteOption.encryptionKey(aes256Key);
		}
	}

	private void _initGCSStore(GCSStoreConfiguration gcsStoreConfiguration)
		throws PortalException {

		String serviceAccountKey = gcsStoreConfiguration.serviceAccountKey();

		GoogleCredentials googleCredentials;

		try {
			if (Validator.isBlank(serviceAccountKey)) {
				if (_log.isInfoEnabled()) {
					_log.info(
						"Using application default credentials because " +
							"service account key was not set");
				}

				googleCredentials =
					ServiceAccountCredentials.getApplicationDefault();
			}
			else {
				googleCredentials = ServiceAccountCredentials.fromStream(
					new ByteArrayInputStream(serviceAccountKey.getBytes()));
			}
		}
		catch (IOException ioException) {
			throw new PortalException(
				"Unable to authenticate with GCS", ioException);
		}

		StorageOptions storageOptions = StorageOptions.newBuilder(
		).setCredentials(
			googleCredentials
		).setRetrySettings(
			RetrySettings.newBuilder(
			).setInitialRetryDelay(
				Duration.ofMillis(gcsStoreConfiguration.initialRetryDelay())
			).setInitialRpcTimeout(
				Duration.ofMillis(gcsStoreConfiguration.initialRPCTimeout())
			).setJittered(
				gcsStoreConfiguration.retryJitter()
			).setMaxAttempts(
				gcsStoreConfiguration.maxRetryAttempts()
			).setMaxRetryDelay(
				Duration.ofMillis(gcsStoreConfiguration.maxRetryDelay())
			).setMaxRpcTimeout(
				Duration.ofMillis(gcsStoreConfiguration.maxRPCTimeout())
			).setRetryDelayMultiplier(
				gcsStoreConfiguration.retryDelayMultiplier()
			).setRpcTimeoutMultiplier(
				gcsStoreConfiguration.rpcTimeoutMultiplier()
			).build()
		).build();

		_gcsStorage = storageOptions.getService();
	}

	private static final long _PAGE_SIZE = 50L;

	private static final Log _log = LogFactoryUtil.getLog(GCSStore.class);

	private BucketInfo _bucketInfo;
	private Blob.BlobSourceOption _decryptBlobBlobSourceOption;
	private Storage.BlobSourceOption _decryptStorageBlobSourceOption;
	private Storage.BlobWriteOption _encryptStorageBlobWriteOption;
	private Storage _gcsStorage;
	private volatile GCSStoreConfiguration _gcsStoreConfiguration;

}