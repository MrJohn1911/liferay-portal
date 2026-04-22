/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.cell.internal.security;

import com.liferay.portal.kernel.portlet.PortalPreferences;
import com.liferay.portal.kernel.security.SecureRandomUtil;
import com.liferay.portal.kernel.service.PortalPreferenceValueLocalService;
import com.liferay.portal.kernel.service.PortalPreferencesLocalService;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LogEntry;
import com.liferay.portal.test.log.LoggerTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Rafael Praxedes
 */
public class JWTTokenImplTest {

	@ClassRule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		_jwtTokenImpl = new JWTTokenImpl();

		PortalPreferences portalPreferences = Mockito.mock(
			PortalPreferences.class);

		Mockito.when(
			portalPreferences.getValue(Mockito.anyString(), Mockito.anyString())
		).thenAnswer(
			invocation -> _values.get(
				invocation.getArgument(0) + ":" + invocation.getArgument(1))
		);

		Mockito.doAnswer(
			invocation -> {
				_values.put(
					invocation.getArgument(0) + ":" +
						invocation.getArgument(1),
					invocation.getArgument(2));

				return null;
			}
		).when(
			portalPreferences
		).setValue(
			Mockito.anyString(), Mockito.anyString(), Mockito.anyString()
		);

		PortalPreferenceValueLocalService portalPreferenceValueLocalService =
			Mockito.mock(PortalPreferenceValueLocalService.class);

		Mockito.when(
			portalPreferenceValueLocalService.getPortalPreferences(
				Mockito.any(), Mockito.anyBoolean())
		).thenReturn(
			portalPreferences
		);

		ReflectionTestUtil.setFieldValue(
			_jwtTokenImpl, "_portalPreferenceValueLocalService",
			portalPreferenceValueLocalService);

		PortalPreferencesLocalService portalPreferencesLocalService =
			Mockito.mock(PortalPreferencesLocalService.class);

		Mockito.when(
			portalPreferencesLocalService.fetchPortalPreferences(
				Mockito.anyLong(), Mockito.anyInt())
		).thenReturn(
			Mockito.mock(
				com.liferay.portal.kernel.model.PortalPreferences.class)
		);

		ReflectionTestUtil.setFieldValue(
			_jwtTokenImpl, "_portalPreferencesLocalService",
			portalPreferencesLocalService);
	}

	@Test
	public void testGenerateToken() throws Exception {
		String token = _jwtTokenImpl.generateToken(
			_COMPANY_ID, TimeUnit.MINUTES.toMillis(1), _ISSUER, _USER_ID);

		Assert.assertNotNull(token);
		Assert.assertFalse(token.isEmpty());

		SignedJWT signedJWT = SignedJWT.parse(token);

		JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();

		Assert.assertEquals(_ISSUER, jwtClaimsSet.getIssuer());
		Assert.assertEquals(
			String.valueOf(_USER_ID), jwtClaimsSet.getSubject());
	}

	@Test
	public void testGetUserId() throws Exception {
		String token = _jwtTokenImpl.generateToken(
			_COMPANY_ID, TimeUnit.MINUTES.toMillis(1), _ISSUER, _USER_ID);

		Assert.assertEquals(
			_USER_ID, _jwtTokenImpl.getUserId(_COMPANY_ID, token));

		String originalEncodedSecret = _values.get(_SECRET_VALUE_KEY);

		byte[] secret = new byte[64];

		for (int i = 0; i < secret.length; i++) {
			secret[i] = SecureRandomUtil.nextByte();
		}

		_values.put(_SECRET_VALUE_KEY, Base64.encode(secret));

		_testGetUserId("Invalid JWT signature", token);

		_values.put(_SECRET_VALUE_KEY, originalEncodedSecret);

		_testGetUserId(
			"Invalid JWT signature",
			token.substring(0, token.length() - 5) + "abcde");
		_testGetUserId(
			"The JWT token is expired",
			_jwtTokenImpl.generateToken(
				_COMPANY_ID, 0, _ISSUER, _USER_ID));
		_testGetUserId(
			"Unable to parse and verify the JWT token",
			RandomTestUtil.randomString());
	}

	private void _testGetUserId(String expectedLogMessage, String token) {
		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				"com.liferay.ai.hub.cell.internal.security.JWTTokenImpl",
				LoggerTestUtil.DEBUG)) {

			_jwtTokenImpl.getUserId(_COMPANY_ID, token);

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 1, logEntries.size());

			LogEntry logEntry = logEntries.get(0);

			Assert.assertEquals(LoggerTestUtil.DEBUG, logEntry.getPriority());

			Assert.assertEquals(expectedLogMessage, logEntry.getMessage());
		}
	}

	private static final long _COMPANY_ID = RandomTestUtil.randomLong();

	private static final String _ISSUER = RandomTestUtil.randomString();

	private static final String _SECRET_VALUE_KEY =
		JWTTokenImpl.class.getName() + ":secret";

	private static final long _USER_ID = RandomTestUtil.randomLong();

	private JWTTokenImpl _jwtTokenImpl;
	private final Map<String, String> _values = new HashMap<>();

}
