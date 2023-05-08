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

package com.liferay.mail.outlook.auth.provider.internal;

import com.liferay.mail.kernel.auth.token.provider.MailAuthTokenProvider;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Component;

/**
 * @author Rafael Praxedes
 */
@Component(
	property = {
		"mail.server.name=outlook.office365.com",
		"mail.server.name=smtp.office365.com"
	},
	service = MailAuthTokenProvider.class
)
public class OutlookMailAuthTokenProvider implements MailAuthTokenProvider {

	public String getAccessToken(long companyId) {
		String clientId = "85da7eef-ec6d-4752-83a0-99c3e1591197";
		String secret = "k1Z8Q~bUHBCEgjX30LnuPmGYVYv3K50GKuDexb8N";
		String authority =
			"https://login.microsoftonline.com" +
				"/3d9be7b5-ee16-43d0-a66b-20e1d50cdc12/";
		String scope = "https://outlook.office365.com/.default";

		try {
			ConfidentialClientApplication confidentialClientApplication =
				ConfidentialClientApplication.builder(
					clientId, ClientCredentialFactory.createFromSecret(secret)
				).authority(
					authority
				).build();

			ClientCredentialParameters clientCredentialParam =
				ClientCredentialParameters.builder(
					Collections.singleton(scope)
				).build();

			CompletableFuture<IAuthenticationResult> completableFuture =
				confidentialClientApplication.acquireToken(
					clientCredentialParam);

			IAuthenticationResult iAuthenticationResult =
				completableFuture.get();

			return iAuthenticationResult.accessToken();
		}
		catch (Exception exception) {
			_log.error(exception);

			throw new SystemException(exception);
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		OutlookMailAuthTokenProvider.class);

}