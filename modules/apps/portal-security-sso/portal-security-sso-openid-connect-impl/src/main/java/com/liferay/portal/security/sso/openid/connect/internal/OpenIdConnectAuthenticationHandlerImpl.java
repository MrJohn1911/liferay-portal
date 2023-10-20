/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.sso.openid.connect.internal;

import com.liferay.oauth.client.persistence.model.OAuthClientEntry;
import com.liferay.oauth.client.persistence.service.OAuthClientEntryLocalService;
import com.liferay.petra.function.UnsafeConsumer;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.exception.UserEmailAddressException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Contact;
import com.liferay.portal.kernel.model.Country;
import com.liferay.portal.kernel.model.ListType;
import com.liferay.portal.kernel.model.Region;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.UserConstants;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.service.AddressLocalService;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.CountryLocalService;
import com.liferay.portal.kernel.service.ListTypeLocalService;
import com.liferay.portal.kernel.service.PhoneLocalService;
import com.liferay.portal.kernel.service.RegionLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Props;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.security.sso.openid.connect.OpenIdConnectAuthenticationHandler;
import com.liferay.portal.security.sso.openid.connect.OpenIdConnectServiceException;
import com.liferay.portal.security.sso.openid.connect.constants.OpenIdConnectConstants;
import com.liferay.portal.security.sso.openid.connect.constants.OpenIdConnectWebKeys;
import com.liferay.portal.security.sso.openid.connect.internal.exception.StrangersNotAllowedException;
import com.liferay.portal.security.sso.openid.connect.internal.session.manager.OfflineOpenIdConnectSessionManager;
import com.liferay.portal.security.sso.openid.connect.internal.util.OpenIdConnectProviderUtil;
import com.liferay.portal.security.sso.openid.connect.internal.util.OpenIdConnectRequestParametersUtil;
import com.liferay.portal.security.sso.openid.connect.internal.util.OpenIdConnectTokenRequestUtil;

import com.nimbusds.jwt.JWT;
import com.nimbusds.langtag.LangTag;
import com.nimbusds.langtag.LangTagException;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeChallenge;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import com.nimbusds.openid.connect.sdk.AuthenticationErrorResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;

import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.minidev.json.JSONObject;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Thuong Dinh
 * @author Edward C. Han
 * @author Arthur Chan
 */
@Component(service = OpenIdConnectAuthenticationHandler.class)
public class OpenIdConnectAuthenticationHandlerImpl
	implements OpenIdConnectAuthenticationHandler {

	@Override
	public void processAuthenticationResponse(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse,
			UnsafeConsumer<Long, Exception> userIdUnsafeConsumer)
		throws Exception {

		HttpSession httpSession = httpServletRequest.getSession();

		OpenIdConnectAuthenticationSession openIdConnectAuthenticationSession =
			(OpenIdConnectAuthenticationSession)httpSession.getAttribute(
				_OPEN_ID_CONNECT_AUTHENTICATION_SESSION);

		httpSession.removeAttribute(_OPEN_ID_CONNECT_AUTHENTICATION_SESSION);

		if (openIdConnectAuthenticationSession == null) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"OpenId Connect authentication was not requested or " +
						"removed");
			}

			return;
		}

		AuthenticationSuccessResponse authenticationSuccessResponse =
			_getAuthenticationSuccessResponse(httpServletRequest);

		_validateState(
			openIdConnectAuthenticationSession.getState(),
			authenticationSuccessResponse.getState());

		OAuthClientEntry oAuthClientEntry =
			_oAuthClientEntryLocalService.getOAuthClientEntry(
				openIdConnectAuthenticationSession.getOAuthClientEntryId());

		OIDCClientInformation oidcClientInformation =
			OIDCClientInformation.parse(
				JSONObjectUtils.parse(oAuthClientEntry.getInfoJSON()));

		OIDCProviderMetadata oidcProviderMetadata =
			_authorizationServerMetadataResolver.resolveOIDCProviderMetadata(
				oAuthClientEntry.getAuthServerWellKnownURI());

		OIDCTokens oidcTokens = OpenIdConnectTokenRequestUtil.request(
			authenticationSuccessResponse,
			openIdConnectAuthenticationSession.getCodeVerifier(),
			openIdConnectAuthenticationSession.getNonce(),
			oidcClientInformation, oidcProviderMetadata,
			_getLoginRedirectURI(httpServletRequest),
			oAuthClientEntry.getTokenRequestParametersJSON());

		String userInfoJSON = _requestUserInfoJSON(
			oidcTokens.getAccessToken(), oidcProviderMetadata);

		long userId = _oidcUserInfoProcessor.processUserInfo(
			_portal.getCompanyId(httpServletRequest),
			String.valueOf(oidcProviderMetadata.getIssuer()),
			ServiceContextFactory.getInstance(httpServletRequest), userInfoJSON,
			oAuthClientEntry.getOIDCUserInfoMapperJSON());

		userIdUnsafeConsumer.accept(userId);

		httpSession = httpServletRequest.getSession();

		long openIdConnectSessionId =
			_offlineOpenIdConnectSessionManager.startOpenIdConnectSession(
				oAuthClientEntry.getAuthServerWellKnownURI(),
				String.valueOf(oidcClientInformation.getID()), oidcTokens,
				userId);

		httpSession.setAttribute(
			OpenIdConnectWebKeys.OPEN_ID_CONNECT_SESSION,
			new OpenIdConnectSessionImpl(
				openIdConnectSessionId,
				oAuthClientEntry.getAuthServerWellKnownURI(),
				openIdConnectAuthenticationSession.getNonce(),
				openIdConnectAuthenticationSession.getState(), userId));
		httpSession.setAttribute(
			OpenIdConnectWebKeys.OPEN_ID_CONNECT_SESSION_ID,
			openIdConnectSessionId);
	}

	@Override
	public void requestAuthentication(
			long oAuthClientEntryId, HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws PortalException {

		HttpSession httpSession = httpServletRequest.getSession();

		Long openIdConnectSessionId = (Long)httpSession.getAttribute(
			OpenIdConnectWebKeys.OPEN_ID_CONNECT_SESSION_ID);

		if (openIdConnectSessionId != null) {
			httpSession.removeAttribute(
				OpenIdConnectWebKeys.OPEN_ID_CONNECT_SESSION_ID);
		}

		CodeVerifier codeVerifier = new CodeVerifier();
		OAuthClientEntry oAuthClientEntry =
			_oAuthClientEntryLocalService.getOAuthClientEntry(
				oAuthClientEntryId);

		Map<String, Object> runtimeRequestParameters =
			HashMapBuilder.<String, Object>put(
				"code_challenge",
				CodeChallenge.compute(CodeChallengeMethod.S256, codeVerifier)
			).put(
				"nonce", new Nonce()
			).put(
				"redirect_uri", _getLoginRedirectURI(httpServletRequest)
			).put(
				"state", new State()
			).put(
				"ui_Locals", _getLangTags(httpServletRequest)
			).build();

		try {
			OIDCProviderMetadata oidcProviderMetadata =
				_authorizationServerMetadataResolver.
					resolveOIDCProviderMetadata(
						oAuthClientEntry.getAuthServerWellKnownURI());

			URI authenticationRequestURI = _getAuthenticationRequestURI(
				oidcProviderMetadata.getAuthorizationEndpointURI(),
				oAuthClientEntry.getAuthRequestParametersJSON(),
				oAuthClientEntry.getClientId(), runtimeRequestParameters);

			if (_log.isDebugEnabled()) {
				_log.debug(
					"Authentication request query: " +
						authenticationRequestURI.getQuery());
			}

			httpServletResponse.sendRedirect(
				authenticationRequestURI.toString());

			httpSession.setAttribute(
				_OPEN_ID_CONNECT_AUTHENTICATION_SESSION,
				new OpenIdConnectAuthenticationSession(
					codeVerifier, (Nonce)runtimeRequestParameters.get("nonce"),
					oAuthClientEntryId,
					(State)runtimeRequestParameters.get("state")));
		}
		catch (Exception exception) {
			throw new PortalException(exception);
		}
	}

	@Override
	public void requestAuthentication(
			String openIdConnectProviderName,
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws PortalException {

		requestAuthentication(
			OpenIdConnectProviderUtil.getOAuthClientEntryId(
				_portal.getCompanyId(httpServletRequest),
				openIdConnectProviderName, _oAuthClientEntryLocalService),
			httpServletRequest, httpServletResponse);
	}

	private URI _getAuthenticationRequestURI(
			URI authenticationEndpointURI,
			String authenticationRequestParametersJSON, String clientId,
			Map<String, Object> runtimeRequestParameters)
		throws Exception {

		JSONObject authenticationRequestParametersJSONObject =
			JSONObjectUtils.parse(authenticationRequestParametersJSON);

		AuthenticationRequest.Builder builder =
			new AuthenticationRequest.Builder(
				OpenIdConnectRequestParametersUtil.getResponseType(
					authenticationRequestParametersJSONObject),
				OpenIdConnectRequestParametersUtil.getScope(
					authenticationRequestParametersJSONObject),
				new ClientID(clientId),
				(URI)runtimeRequestParameters.get("redirect_uri"));

		builder = builder.endpointURI(
			authenticationEndpointURI
		).codeChallenge(
			(CodeChallenge)runtimeRequestParameters.get("code_challenge"),
			CodeChallengeMethod.S256
		).nonce(
			(Nonce)runtimeRequestParameters.get("nonce")
		).resources(
			OpenIdConnectRequestParametersUtil.getResourceURIs(
				authenticationRequestParametersJSONObject)
		).state(
			(State)runtimeRequestParameters.get("state")
		).uiLocales(
			(List<LangTag>)runtimeRequestParameters.get("ui_locales")
		);

		OpenIdConnectRequestParametersUtil.consumeCustomRequestParameters(
			builder::customParameter,
			authenticationRequestParametersJSONObject);

		return builder.build(
		).toURI();
	}

	private AuthenticationSuccessResponse _getAuthenticationSuccessResponse(
			HttpServletRequest httpServletRequest)
		throws OpenIdConnectServiceException.AuthenticationException {

		StringBuffer requestURL = httpServletRequest.getRequestURL();

		if (Validator.isNotNull(httpServletRequest.getQueryString())) {
			requestURL.append(StringPool.QUESTION);
			requestURL.append(httpServletRequest.getQueryString());
		}

		try {
			URI requestURI = new URI(requestURL.toString());

			AuthenticationResponse authenticationResponse =
				AuthenticationResponseParser.parse(requestURI);

			if (authenticationResponse instanceof AuthenticationErrorResponse) {
				AuthenticationErrorResponse authenticationErrorResponse =
					(AuthenticationErrorResponse)authenticationResponse;

				ErrorObject errorObject =
					authenticationErrorResponse.getErrorObject();

				JSONObject jsonObject = errorObject.toJSONObject();

				throw new OpenIdConnectServiceException.AuthenticationException(
					jsonObject.toString());
			}

			return (AuthenticationSuccessResponse)authenticationResponse;
		}
		catch (ParseException | URISyntaxException exception) {
			throw new OpenIdConnectServiceException.AuthenticationException(
				StringBundler.concat(
					"Unable to process response from ", requestURL, ": ",
					exception.getMessage()),
				exception);
		}
	}

	private List<LangTag> _getLangTags(HttpServletRequest httpServletRequest) {
		Locale locale = _portal.getLocale(httpServletRequest);

		if (locale == null) {
			return null;
		}

		try {
			return Collections.singletonList(new LangTag(locale.getLanguage()));
		}
		catch (LangTagException langTagException) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Unable to create a lang tag with locale " +
						locale.getLanguage(),
					langTagException);
			}

			return null;
		}
	}

	private URI _getLoginRedirectURI(HttpServletRequest httpServletRequest) {
		try {
			return new URI(
				StringBundler.concat(
					_portal.getPortalURL(httpServletRequest),
					_portal.getPathContext(),
					OpenIdConnectConstants.REDIRECT_URL_PATTERN));
		}
		catch (URISyntaxException uriSyntaxException) {
			throw new SystemException(
				"Unable to generate OpenId Connect login redirect URI: " +
					uriSyntaxException.getMessage(),
				uriSyntaxException);
		}
	}

	private String _requestUserInfoJSON(
			AccessToken accessToken, OIDCProviderMetadata oidcProviderMetadata)
		throws OpenIdConnectServiceException.UserInfoException {

		UserInfoRequest userInfoRequest = new UserInfoRequest(
			oidcProviderMetadata.getUserInfoEndpointURI(),
			(BearerAccessToken)accessToken);

		HTTPRequest httpRequest = userInfoRequest.toHTTPRequest();

		httpRequest.setAccept(
			"text/html, image/gif, image/jpeg, */*; q=0.2, */*; q=0.2");

		try {
			HTTPResponse httpResponse = httpRequest.send();

			UserInfoResponse userInfoResponse = UserInfoResponse.parse(
				httpResponse);

			if (userInfoResponse instanceof UserInfoErrorResponse) {
				UserInfoErrorResponse userInfoErrorResponse =
					(UserInfoErrorResponse)userInfoResponse;

				ErrorObject errorObject =
					userInfoErrorResponse.getErrorObject();

				JSONObject jsonObject = errorObject.toJSONObject();

				throw new OpenIdConnectServiceException.UserInfoException(
					jsonObject.toString());
			}

			UserInfoSuccessResponse userInfoSuccessResponse =
				(UserInfoSuccessResponse)userInfoResponse;

			UserInfo userInfo = userInfoSuccessResponse.getUserInfo();

			if (userInfo == null) {
				JWT userInfoJWT = userInfoSuccessResponse.getUserInfoJWT();

				userInfo = new UserInfo(userInfoJWT.getJWTClaimsSet());
			}

			return userInfo.toJSONString();
		}
		catch (IOException ioException) {
			throw new OpenIdConnectServiceException.UserInfoException(
				StringBundler.concat(
					"Unable to get user information from ",
					oidcProviderMetadata.getUserInfoEndpointURI(), ": ",
					ioException.getMessage()),
				ioException);
		}
		catch (java.text.ParseException | ParseException exception) {
			throw new OpenIdConnectServiceException.UserInfoException(
				StringBundler.concat(
					"Unable to parse user information response from ",
					oidcProviderMetadata.getUserInfoEndpointURI(), ": ",
					exception.getMessage()),
				exception);
		}
	}

	private void _validateState(State requestedState, State state)
		throws Exception {

		if (!state.equals(requestedState)) {
			throw new OpenIdConnectServiceException.AuthenticationException(
				StringBundler.concat(
					"Requested value \"", requestedState.getValue(),
					"\" and approved state \"", state.getValue(),
					"\" do not match"));
		}
	}

	private static final String _OPEN_ID_CONNECT_AUTHENTICATION_SESSION =
		OpenIdConnectAuthenticationHandlerImpl.class.getName() +
			"#OPEN_ID_CONNECT_AUTHENTICATION_SESSION";

	private static final Log _log = LogFactoryUtil.getLog(
		OpenIdConnectAuthenticationHandlerImpl.class);

	@Reference
	private AddressLocalService _addressLocalService;

	@Reference
	private AuthorizationServerMetadataResolver
		_authorizationServerMetadataResolver;

	@Reference
	private CompanyLocalService _companyLocalService;

	@Reference
	private CountryLocalService _countryLocalService;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private ListTypeLocalService _listTypeLocalService;

	@Reference
	private OAuthClientEntryLocalService _oAuthClientEntryLocalService;

	@Reference
	private OfflineOpenIdConnectSessionManager
		_offlineOpenIdConnectSessionManager;

	private final OIDCUserInfoProcessor _oidcUserInfoProcessor =
		new OIDCUserInfoProcessor();

	@Reference
	private PhoneLocalService _phoneLocalService;

	@Reference
	private Portal _portal;

	@Reference
	private Props _props;

	@Reference
	private RegionLocalService _regionLocalService;

	@Reference
	private RoleLocalService _roleLocalService;

	@Reference
	private UserLocalService _userLocalService;

	private class OIDCUserInfoProcessor {

		public long processUserInfo(
				long companyId, String issuer, ServiceContext serviceContext,
				String userInfoJSON, String userInfoMapperJSON)
			throws Exception {

			long userId = _getUserId(
				companyId, userInfoJSON, userInfoMapperJSON);

			if (userId > 0) {
				return userId;
			}

			User user = _addUser(
				companyId, issuer, serviceContext, userInfoJSON,
				userInfoMapperJSON);

			try {
				_addAddress(
					serviceContext, user, userInfoJSON, userInfoMapperJSON);
			}
			catch (Exception exception) {
				if (_log.isWarnEnabled()) {
					_log.warn(exception);
				}
			}

			try {
				_addPhone(
					serviceContext, user, userInfoJSON, userInfoMapperJSON);
			}
			catch (Exception exception) {
				if (_log.isWarnEnabled()) {
					_log.warn(exception);
				}
			}

			return user.getUserId();
		}

		private void _addAddress(
				ServiceContext serviceContext, User user, String userInfoJSON,
				String userInfoMapperJSON)
			throws Exception {

			com.liferay.portal.kernel.json.JSONObject userInfoMapperJSONObject =
				_jsonFactory.createJSONObject(userInfoMapperJSON);

			com.liferay.portal.kernel.json.JSONObject addressMapperJSONObject =
				userInfoMapperJSONObject.getJSONObject("address");

			if (addressMapperJSONObject == null) {
				return;
			}

			com.liferay.portal.kernel.json.JSONObject userInfoJSONObject =
				_jsonFactory.createJSONObject(userInfoJSON);

			String streetClaimString = _getClaimString(
				"street", addressMapperJSONObject, userInfoJSONObject);

			if (Validator.isNull(streetClaimString)) {
				return;
			}

			String[] streetClaimStringParts = streetClaimString.split("\n");

			Region region = null;
			Country country = null;

			String countryClaimString = _getClaimString(
				"country", addressMapperJSONObject, userInfoJSONObject);

			if (Validator.isNotNull(countryClaimString)) {
				if ((countryClaimString.charAt(0) >= '0') &&
					(countryClaimString.charAt(0) <= '9')) {

					country = _countryLocalService.getCountryByNumber(
						user.getCompanyId(), countryClaimString);
				}
				else if (countryClaimString.length() == 2) {
					country = _countryLocalService.fetchCountryByA2(
						user.getCompanyId(),
						StringUtil.toUpperCase(countryClaimString));
				}
				else if (countryClaimString.length() == 3) {
					country = _countryLocalService.fetchCountryByA3(
						user.getCompanyId(),
						StringUtil.toUpperCase(countryClaimString));
				}
				else {
					country = _countryLocalService.fetchCountryByName(
						user.getCompanyId(),
						StringUtil.toLowerCase(countryClaimString));
				}

				String regionCode = _getClaimString(
					"region", addressMapperJSONObject, userInfoJSONObject);

				if ((country != null) && Validator.isNotNull(regionCode)) {
					region = _regionLocalService.fetchRegion(
						country.getCountryId(),
						StringUtil.toUpperCase(regionCode));
				}
			}

			ListType listType = _listTypeLocalService.getListType(
				user.getCompanyId(),
				_getClaimString(
					"addressType", addressMapperJSONObject, userInfoJSONObject),
				Contact.class.getName() + ".address");

			if (listType == null) {
				List<ListType> listTypes = _listTypeLocalService.getListTypes(
					user.getCompanyId(), Contact.class.getName() + ".address");

				listType = listTypes.get(0);
			}

			_addressLocalService.addAddress(
				null, user.getUserId(), Contact.class.getName(),
				user.getContactId(), null, null,
				(streetClaimStringParts.length > 0) ?
					streetClaimStringParts[0] : null,
				(streetClaimStringParts.length > 1) ?
					streetClaimStringParts[1] : null,
				(streetClaimStringParts.length > 2) ?
					streetClaimStringParts[2] : null,
				_getClaimString(
					"city", addressMapperJSONObject, userInfoJSONObject),
				_getClaimString(
					"zip", addressMapperJSONObject, userInfoJSONObject),
				(region == null) ? 0 : region.getRegionId(),
				(country == null) ? 0 : country.getCountryId(),
				listType.getListTypeId(), false, false, null, serviceContext);
		}

		private void _addPhone(
				ServiceContext serviceContext, User user, String userInfoJSON,
				String userInfoMapperJSON)
			throws Exception {

			com.liferay.portal.kernel.json.JSONObject userInfoMapperJSONObject =
				_jsonFactory.createJSONObject(userInfoMapperJSON);

			com.liferay.portal.kernel.json.JSONObject phoneMapperJSONObject =
				userInfoMapperJSONObject.getJSONObject("phone");

			if (phoneMapperJSONObject == null) {
				return;
			}

			com.liferay.portal.kernel.json.JSONObject userInfoJSONObject =
				_jsonFactory.createJSONObject(userInfoJSON);

			String phoneClaimString = _getClaimString(
				"phone", phoneMapperJSONObject, userInfoJSONObject);

			if (Validator.isNull(phoneClaimString)) {
				return;
			}

			ListType listType = _listTypeLocalService.getListType(
				user.getCompanyId(),
				_getClaimString(
					"phoneType", phoneMapperJSONObject, userInfoJSONObject),
				Contact.class.getName() + ".phone");

			if (listType == null) {
				List<ListType> listTypes = _listTypeLocalService.getListTypes(
					user.getCompanyId(), Contact.class.getName() + ".phone");

				listType = listTypes.get(0);
			}

			_phoneLocalService.addPhone(
				user.getUserId(), Contact.class.getName(), user.getContactId(),
				phoneClaimString, null, listType.getListTypeId(), false,
				serviceContext);
		}

		private User _addUser(
				long companyId, String issuer, ServiceContext serviceContext,
				String userInfoJSON, String userInfoMapperJSON)
			throws Exception {

			com.liferay.portal.kernel.json.JSONObject userInfoMapperJSONObject =
				_jsonFactory.createJSONObject(userInfoMapperJSON);

			com.liferay.portal.kernel.json.JSONObject userMapperJSONObject =
				userInfoMapperJSONObject.getJSONObject("user");

			com.liferay.portal.kernel.json.JSONObject userInfoJSONObject =
				_jsonFactory.createJSONObject(userInfoJSON);

			String emailAddress = _getClaimString(
				"emailAddress", userMapperJSONObject, userInfoJSONObject);

			if (Validator.isNull(emailAddress)) {
				throw new OpenIdConnectServiceException.UserMappingException(
					"Email address is null");
			}

			String firstName = _getClaimString(
				"firstName", userMapperJSONObject, userInfoJSONObject);

			if (Validator.isNull(firstName)) {
				throw new OpenIdConnectServiceException.UserMappingException(
					"First name is null");
			}

			String lastName = _getClaimString(
				"lastName", userMapperJSONObject, userInfoJSONObject);

			if (Validator.isNull(lastName)) {
				throw new OpenIdConnectServiceException.UserMappingException(
					"Last name is null");
			}

			_checkAddUser(companyId, emailAddress);

			long creatorUserId = 0;
			boolean autoPassword = true;
			String password1 = null;
			String password2 = null;
			String screenName = _getClaimString(
				"screenName", userMapperJSONObject, userInfoJSONObject);
			long prefixListTypeId = 0;
			long suffixListTypeId = 0;

			com.liferay.portal.kernel.json.JSONObject contactMapperJSONObject =
				userInfoMapperJSONObject.getJSONObject("contact");

			int[] birthday = _getBirthday(
				contactMapperJSONObject, userInfoJSONObject);

			long[] groupIds = null;
			long[] organizationIds = null;

			long[] roleIds = _getRoleIds(
				companyId, userInfoJSONObject,
				userInfoMapperJSONObject.getJSONObject("users_roles"));

			if ((roleIds == null) || (roleIds.length == 0)) {
				roleIds = _getRoleIds(companyId, issuer);
			}

			long[] userGroupIds = null;
			boolean sendEmail = false;

			User user = _userLocalService.addUser(
				creatorUserId, companyId, autoPassword, password1, password2,
				Validator.isNull(screenName), screenName, emailAddress,
				_getLocale(companyId, userInfoJSONObject, userMapperJSONObject),
				firstName,
				_getClaimString(
					"middleName", userMapperJSONObject, userInfoJSONObject),
				lastName, prefixListTypeId, suffixListTypeId,
				_isMale(contactMapperJSONObject, userInfoJSONObject),
				birthday[1], birthday[2], birthday[0],
				_getClaimString(
					"jobTitle", userMapperJSONObject, userInfoJSONObject),
				UserConstants.TYPE_REGULAR, groupIds, organizationIds, roleIds,
				userGroupIds, sendEmail, serviceContext);

			return _userLocalService.updatePasswordReset(
				user.getUserId(), false);
		}

		private void _checkAddUser(long companyId, String emailAddress)
			throws Exception {

			Company company = _companyLocalService.getCompany(companyId);

			if (!company.isStrangers()) {
				throw new StrangersNotAllowedException(companyId);
			}

			if (!company.isStrangersWithMx() &&
				company.hasCompanyMx(emailAddress)) {

				throw new UserEmailAddressException.MustNotUseCompanyMx(
					emailAddress);
			}
		}

		private int[] _getBirthday(
			com.liferay.portal.kernel.json.JSONObject contactMapperJSONObject,
			com.liferay.portal.kernel.json.JSONObject userInfoJSONObject) {

			int[] birthday = new int[3];

			birthday[0] = 1970;
			birthday[1] = Calendar.JANUARY;
			birthday[2] = 1;

			String birthdateClaimString = _getClaimString(
				"birthdate", contactMapperJSONObject, userInfoJSONObject);

			if (Validator.isNull(birthdateClaimString)) {
				return birthday;
			}

			String[] birthdateClaimStringParts = birthdateClaimString.split(
				"-");

			if (!birthdateClaimStringParts[0].equals("0000")) {
				birthday[0] = GetterUtil.getInteger(
					birthdateClaimStringParts[0]);
			}

			if (birthdateClaimStringParts.length == 3) {
				birthday[1] =
					GetterUtil.getInteger(birthdateClaimStringParts[1]) - 1;
				birthday[2] = GetterUtil.getInteger(
					birthdateClaimStringParts[2]);
			}

			return birthday;
		}

		private JSONArray _getClaimJSONArray(
			String key,
			com.liferay.portal.kernel.json.JSONObject mapperJSONObject,
			com.liferay.portal.kernel.json.JSONObject userInfoJSONObject) {

			Object claimObject = _getClaimObject(
				key, mapperJSONObject, userInfoJSONObject);

			if ((claimObject == null) || (claimObject instanceof JSONArray)) {
				return (JSONArray)claimObject;
			}

			return null;
		}

		private Object _getClaimObject(
			String key,
			com.liferay.portal.kernel.json.JSONObject mapperJSONObject,
			com.liferay.portal.kernel.json.JSONObject userInfoJSONObject) {

			String value = mapperJSONObject.getString(key);

			if (Validator.isNull(value)) {
				return null;
			}

			String[] valueParts = value.split("->");

			Object claimObject = userInfoJSONObject.get(valueParts[0]);

			for (int i = 1; i < valueParts.length; ++i) {
				com.liferay.portal.kernel.json.JSONObject claimJSONObject =
					(com.liferay.portal.kernel.json.JSONObject)claimObject;

				if (claimJSONObject != null) {
					claimObject = claimJSONObject.get(valueParts[i]);
				}
			}

			return claimObject;
		}

		private String _getClaimString(
			String key,
			com.liferay.portal.kernel.json.JSONObject mapperJSONObject,
			com.liferay.portal.kernel.json.JSONObject userInfoJSONObject) {

			Object claimObject = _getClaimObject(
				key, mapperJSONObject, userInfoJSONObject);

			if ((claimObject != null) && !(claimObject instanceof String)) {
				throw new IllegalArgumentException("Claim is not a string");
			}

			return (String)claimObject;
		}

		private Locale _getLocale(
				long companyId,
				com.liferay.portal.kernel.json.JSONObject userInfoJSONObject,
				com.liferay.portal.kernel.json.JSONObject userMapperJSONObject)
			throws Exception {

			String languageId = _getClaimString(
				"languageId", userMapperJSONObject, userInfoJSONObject);

			if (Validator.isNotNull(languageId)) {
				return new Locale(languageId);
			}

			Company company = _companyLocalService.getCompany(companyId);

			return company.getLocale();
		}

		private long[] _getRoleIds(
			long companyId,
			com.liferay.portal.kernel.json.JSONObject userInfoJSONObject,
			com.liferay.portal.kernel.json.JSONObject
				usersRolesMapperJSONObject) {

			if ((usersRolesMapperJSONObject == null) ||
				(usersRolesMapperJSONObject.length() < 1)) {

				return null;
			}

			JSONArray rolesJSONArray = _getClaimJSONArray(
				"roles", usersRolesMapperJSONObject, userInfoJSONObject);

			if (rolesJSONArray == null) {
				return null;
			}

			List<Long> roleIds = new ArrayList<>();

			for (int i = 0; i < rolesJSONArray.length(); ++i) {
				Role role = _roleLocalService.fetchRole(
					companyId, rolesJSONArray.getString(i));

				if (role == null) {
					if (_log.isWarnEnabled()) {
						_log.warn(
							"No role name " + rolesJSONArray.getString(i));
					}

					continue;
				}

				roleIds.add(role.getRoleId());
			}

			if (roleIds.isEmpty()) {
				return null;
			}

			return ArrayUtil.toLongArray(roleIds);
		}

		private long[] _getRoleIds(long companyId, String issuer) {
			if (Validator.isNull(issuer) ||
				!Objects.equals(
					issuer,
					_props.get(
						"open.id.connect.user.info.processor.impl.issuer"))) {

				return null;
			}

			String roleName = _props.get(
				"open.id.connect.user.info.processor.impl.regular.role");

			if (Validator.isNull(roleName)) {
				return null;
			}

			Role role = _roleLocalService.fetchRole(companyId, roleName);

			if (role == null) {
				return null;
			}

			if (role.getType() == RoleConstants.TYPE_REGULAR) {
				return new long[] {role.getRoleId()};
			}

			if (_log.isInfoEnabled()) {
				_log.info("Role " + roleName + " is not a regular role");
			}

			return null;
		}

		private long _getUserId(
				long companyId, String userInfoJSON, String userInfoMapperJSON)
			throws Exception {

			com.liferay.portal.kernel.json.JSONObject userInfoMapperJSONObject =
				_jsonFactory.createJSONObject(userInfoMapperJSON);

			com.liferay.portal.kernel.json.JSONObject userMapperJSONObject =
				userInfoMapperJSONObject.getJSONObject("user");

			com.liferay.portal.kernel.json.JSONObject userInfoJSONObject =
				_jsonFactory.createJSONObject(userInfoJSON);

			User user = _userLocalService.fetchUserByEmailAddress(
				companyId,
				_getClaimString(
					"emailAddress", userMapperJSONObject, userInfoJSONObject));

			if (user != null) {
				return user.getUserId();
			}

			return 0;
		}

		private boolean _isMale(
			com.liferay.portal.kernel.json.JSONObject contactMapperJSONObject,
			com.liferay.portal.kernel.json.JSONObject userInfoJSONObject) {

			String gender = _getClaimString(
				"gender", contactMapperJSONObject, userInfoJSONObject);

			if (Validator.isNull(gender) || gender.equals("male")) {
				return true;
			}

			return false;
		}

	}

}