/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.search.internal.buffer;

import com.liferay.portal.search.configuration.IndexerRegistryConfiguration;

/**
 * @author Bryan Engler
 * @author Michael C. Han
 */
public class IndexerRequestBufferHandler {

	public IndexerRequestBufferHandler(
		IndexerRegistryConfiguration indexerRegistryConfiguration,
		float minimumBufferAvailabilityPercentage) {

		_indexerRegistryConfiguration = indexerRegistryConfiguration;
		_minimumBufferAvailabilityPercentage =
			minimumBufferAvailabilityPercentage;
	}

	public void bufferRequest(
			IndexerRequest indexerRequest,
			IndexerRequestBuffer indexerRequestBuffer)
		throws Exception {

		if (!BufferOverflowThreadLocal.isOverflowMode()) {
			int maxBufferSize = _indexerRegistryConfiguration.maxBufferSize();

			indexerRequestBuffer.add(
				indexerRequest, _minimumBufferAvailabilityPercentage,
				maxBufferSize);
		}
		else {
			indexerRequest.execute();
		}
	}

	private final IndexerRegistryConfiguration _indexerRegistryConfiguration;
	private volatile float _minimumBufferAvailabilityPercentage;

}