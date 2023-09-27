/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.search.internal.buffer;

import com.liferay.portal.kernel.module.util.SystemBundleUtil;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.search.configuration.IndexerRegistryConfiguration;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * @author Bryan Engler
 * @author André de Oliveira
 */
public class IndexerRequestBufferHandlerTest {

	@ClassRule
	public static LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@BeforeClass
	public static void setUpClass() {
		BundleContext bundleContext = SystemBundleUtil.getBundleContext();

		Mockito.when(
			FrameworkUtil.getBundle(Mockito.any())
		).thenReturn(
			bundleContext.getBundle()
		);
	}

	@AfterClass
	public static void tearDownClass() {
		_frameworkUtilMockedStatic.close();
	}

	public IndexerRequestBufferHandlerTest() throws Exception {
		_method = Indexer.class.getDeclaredMethod(
			"reindex", String.class, long.class);
	}

	@Test
	public void testDeepReindexMustNotOverflow() throws Exception {
		int maxBufferSize = 5;

		_indexerRequestBufferHandler = new IndexerRequestBufferHandler(
			new IndexerRequestBufferOverflowHandler(),
			_createIndexerRegistryConfiguration(maxBufferSize));

		_indexerRequestBuffer = IndexerRequestBuffer.create();

		Indexer<?> indexer = _createIndexerWithDeepReindex();

		List<IndexerRequest> indexerRequests = _createIndexerRequests(
			indexer, maxBufferSize + 3);

		for (IndexerRequest indexerRequest : indexerRequests) {
			_indexerRequestBufferHandler.bufferRequest(
				indexerRequest, _indexerRequestBuffer);
		}
	}

	private IndexerRegistryConfiguration _createIndexerRegistryConfiguration(
		int maxBufferSize) {

		IndexerRegistryConfiguration indexerRegistryConfiguration =
			Mockito.mock(IndexerRegistryConfiguration.class);

		Mockito.doReturn(
			maxBufferSize
		).when(
			indexerRegistryConfiguration
		).maxBufferSize();

		return indexerRegistryConfiguration;
	}

	private IndexerRequest _createIndexerRequest(Indexer<?> indexer) {
		return new IndexerRequest(
			_method, indexer, RandomTestUtil.randomString(),
			RandomTestUtil.randomLong());
	}

	private List<IndexerRequest> _createIndexerRequests(
		Indexer<?> indexer, int count) {

		List<IndexerRequest> indexerRequests = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			indexerRequests.add(_createIndexerRequest(indexer));
		}

		return indexerRequests;
	}

	private Indexer<?> _createIndexerWithDeepReindex() throws Exception {
		Indexer<?> indexer = Mockito.mock(Indexer.class);

		Mockito.doAnswer(
			invocationOnMock -> {
				_deepReindex();

				return null;
			}
		).when(
			indexer
		).reindex(
			Mockito.anyString(), Mockito.anyLong()
		);

		return indexer;
	}

	private void _deepReindex() throws Exception {
		IndexerRequest indexerRequest = _createIndexerRequest(_indexer);

		_indexerRequestBufferHandler.bufferRequest(
			indexerRequest, _indexerRequestBuffer);
	}

	private static final MockedStatic<FrameworkUtil>
		_frameworkUtilMockedStatic = Mockito.mockStatic(FrameworkUtil.class);

	private final Indexer<?> _indexer = Mockito.mock(Indexer.class);
	private IndexerRequestBuffer _indexerRequestBuffer;
	private IndexerRequestBufferHandler _indexerRequestBufferHandler;
	private final Method _method;

}