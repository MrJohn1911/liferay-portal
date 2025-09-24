/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.welcome.site.initializer.internal.instance.lifecycle;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.liferay.headless.site.dto.v1_0.Site;
import com.liferay.headless.site.resource.v1_0.SiteResource;
import com.liferay.layout.util.LayoutServiceContextHelper;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.instance.lifecycle.InitialRequestPortalInstanceLifecycleListener;
import com.liferay.portal.instance.lifecycle.PortalInstanceLifecycleListener;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.model.LayoutTypePortlet;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.portlet.InvokerPortlet;
import com.liferay.portal.kernel.portlet.LiferayRenderRequest;
import com.liferay.portal.kernel.portlet.PortletConfigFactoryUtil;
import com.liferay.portal.kernel.portlet.PortletInstanceFactoryUtil;
import com.liferay.portal.kernel.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.PortletLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.ThemeLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.servlet.DummyHttpServletResponse;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;
import com.liferay.portal.kernel.util.ColorSchemeFactoryUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.FriendlyURLNormalizer;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PortletKeys;
import com.liferay.portal.kernel.util.PrefsProps;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.security.permission.PermissionCacheUtil;
import com.liferay.portal.vulcan.multipart.BinaryFile;
import com.liferay.portal.vulcan.multipart.MultipartBody;
import com.liferay.portlet.RenderRequestFactory;
import com.liferay.portlet.RenderResponseFactory;
import com.liferay.site.initializer.SiteInitializer;
import com.liferay.site.initializer.SiteInitializerRegistry;
import com.liferay.site.initializer.extender.SiteInitializerUtil;

import jakarta.portlet.PortletConfig;
import jakarta.portlet.PortletMode;
import jakarta.portlet.PortletRequest;
import jakarta.portlet.WindowState;

import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;

import java.net.URL;
import java.net.URLConnection;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * @author Pavel Savinov
 */
@Component(service = PortalInstanceLifecycleListener.class)
public class AddDefaultLayoutInitialRequestPortalInstanceLifecycleListener
	extends InitialRequestPortalInstanceLifecycleListener {

	@Activate
	@Override
	protected void activate(BundleContext bundleContext) {
		super.activate(bundleContext);
	}

	@Deactivate
	protected void deactivate() {
		_bundleTracker.close();
	}

	@Override
	protected void doPortalInstanceRegistered(long companyId) throws Exception {
		Group group = _groupLocalService.getGroup(
			companyId, GroupConstants.GUEST);

		String friendlyURL = _friendlyURLNormalizer.normalizeWithEncoding(
			PropsValues.DEFAULT_GUEST_PUBLIC_LAYOUT_FRIENDLY_URL);

		Layout defaultLayout = _layoutLocalService.fetchLayoutByFriendlyURL(
			group.getGroupId(), false, friendlyURL);

		if (defaultLayout != null) {
			return;
		}

		defaultLayout = _layoutLocalService.fetchFirstLayout(
			group.getGroupId(), false, LayoutConstants.DEFAULT_PARENT_LAYOUT_ID,
			false);

		if (defaultLayout != null) {
			return;
		}

		String name = PrincipalThreadLocal.getName();

		PermissionChecker permissionChecker =
			PermissionThreadLocal.getPermissionChecker();

		ServiceContext currentThreadServiceContext =
			ServiceContextThreadLocal.getServiceContext();

		if (currentThreadServiceContext == null) {
			currentThreadServiceContext = new ServiceContext();
		}

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.
					setInitializingPortalInstanceWithSafeCloseable(true)) {

			User user = _getUser(companyId);

			PrincipalThreadLocal.setName(user.getUserId());

			PermissionThreadLocal.setPermissionChecker(
				_defaultPermissionCheckerFactory.create(user));

			ServiceContextThreadLocal.pushServiceContext(
				_populateServiceContext(
					_companyLocalService.getCompanyById(companyId), group,
					currentThreadServiceContext.getRequest(), permissionChecker,
					(ServiceContext)currentThreadServiceContext.clone(), user));

			UnicodeProperties typeSettingsUnicodeProperties =
				group.getTypeSettingsProperties();

			String siteInitializerKey =
				typeSettingsUnicodeProperties.getProperty("siteInitializerKey");

			if (Validator.isNull(siteInitializerKey)) {
				siteInitializerKey = _SITE_INITIALIZER_KEY_WELCOME;
			}

			if (!Objects.equals(
					siteInitializerKey, _SITE_INITIALIZER_KEY_WELCOME) &&
				!Objects.equals(
					siteInitializerKey, _SITE_INITIALIZER_KEY_BLANK)) {

				_layoutLocalService.deleteLayouts(
					group.getGroupId(), false, new ServiceContext());
			}

			SiteInitializer siteInitializer =
				_siteInitializerRegistry.getSiteInitializer(siteInitializerKey);

			siteInitializer.initialize(group.getGroupId());
		}
		finally {
			PrincipalThreadLocal.setName(name);

			PermissionThreadLocal.setPermissionChecker(permissionChecker);

			ServiceContextThreadLocal.popServiceContext();

			Bundle bundle = FrameworkUtil.getBundle(
				AddDefaultLayoutInitialRequestPortalInstanceLifecycleListener.
					class);

			_bundleTracker = new BundleTracker<>(
				bundle.getBundleContext(), Bundle.ACTIVE,
				new SiteInitializerClientExtension());

			_bundleTracker.open();
		}
	}

	private User _getUser(long companyId) throws PortalException {
		Role role = _roleLocalService.fetchRole(
			companyId, RoleConstants.ADMINISTRATOR);

		if (role == null) {
			return _userLocalService.getGuestUser(companyId);
		}

		List<User> adminUsers = _userLocalService.getRoleUsers(
			role.getRoleId(), 0, 1);

		if (adminUsers.isEmpty()) {
			return _userLocalService.getGuestUser(companyId);
		}

		return adminUsers.get(0);
	}

	private ServiceContext _populateServiceContext(
			Company company, Group group, HttpServletRequest httpServletRequest,
			PermissionChecker permissionChecker, ServiceContext serviceContext,
			User user)
		throws PortalException {

		serviceContext.setCompanyId(user.getCompanyId());
		serviceContext.setRequest(httpServletRequest);
		serviceContext.setScopeGroupId(group.getGroupId());
		serviceContext.setUserId(user.getUserId());

		if (httpServletRequest == null) {
			return serviceContext;
		}

		long controlPanelPlid = _portal.getControlPanelPlid(
			company.getCompanyId());

		Layout controlPanelLayout = _layoutLocalService.getLayout(
			controlPanelPlid);

		httpServletRequest.setAttribute(WebKeys.LAYOUT, controlPanelLayout);

		ThemeDisplay currentThemeDisplay =
			(ThemeDisplay)httpServletRequest.getAttribute(
				WebKeys.THEME_DISPLAY);

		ThemeDisplay themeDisplay = null;

		if (currentThemeDisplay != null) {
			try {
				themeDisplay = (ThemeDisplay)currentThemeDisplay.clone();
			}
			catch (CloneNotSupportedException cloneNotSupportedException) {
				_log.error(cloneNotSupportedException);
			}
		}
		else {
			themeDisplay = new ThemeDisplay();
		}

		themeDisplay.setCompany(company);
		themeDisplay.setLayout(controlPanelLayout);
		themeDisplay.setLayoutSet(controlPanelLayout.getLayoutSet());
		themeDisplay.setLayoutTypePortlet(
			(LayoutTypePortlet)controlPanelLayout.getLayoutType());
		themeDisplay.setLayouts(ListUtil.fromArray(controlPanelLayout));
		themeDisplay.setLocale(LocaleUtil.getSiteDefault());

		String themeId = _prefsProps.getString(
			company.getCompanyId(),
			PropsKeys.CONTROL_PANEL_LAYOUT_REGULAR_THEME_ID);

		themeDisplay.setLookAndFeel(
			_themeLocalService.getTheme(company.getCompanyId(), themeId),
			ColorSchemeFactoryUtil.getDefaultRegularColorScheme());

		themeDisplay.setPermissionChecker(permissionChecker);
		themeDisplay.setPlid(controlPanelPlid);
		themeDisplay.setRealUser(user);
		themeDisplay.setRequest(httpServletRequest);
		themeDisplay.setScopeGroupId(controlPanelLayout.getGroupId());
		themeDisplay.setSiteGroupId(controlPanelLayout.getGroupId());
		themeDisplay.setUser(user);

		httpServletRequest.setAttribute(WebKeys.THEME_DISPLAY, themeDisplay);

		PortletRequest portletRequest =
			(PortletRequest)httpServletRequest.getAttribute(
				JavaConstants.JAKARTA_PORTLET_REQUEST);

		if (portletRequest != null) {
			return serviceContext;
		}

		Portlet portlet = _portletLocalService.getPortletById(
			CompanyConstants.SYSTEM, PortletKeys.PORTAL);

		try {
			InvokerPortlet invokerPortlet = PortletInstanceFactoryUtil.create(
				portlet, httpServletRequest.getServletContext());

			PortletConfig portletConfig = PortletConfigFactoryUtil.create(
				portlet, httpServletRequest.getServletContext());

			LiferayRenderRequest liferayRenderRequest =
				RenderRequestFactory.create(
					httpServletRequest, portlet, invokerPortlet,
					portletConfig.getPortletContext(), WindowState.NORMAL,
					PortletMode.VIEW,
					PortletPreferencesFactoryUtil.fromDefaultXML(
						portlet.getDefaultPreferences()),
					themeDisplay.getPlid());

			httpServletRequest.setAttribute(
				JavaConstants.JAKARTA_PORTLET_REQUEST, liferayRenderRequest);
			httpServletRequest.setAttribute(
				JavaConstants.JAKARTA_PORTLET_RESPONSE,
				RenderResponseFactory.create(
					new DummyHttpServletResponse(), liferayRenderRequest));
		}
		catch (Exception exception) {
			_log.error(exception);
		}

		return serviceContext;
	}

	private static final String _SITE_INITIALIZER_KEY_BLANK =
		"blank-site-initializer";

	private static final String _SITE_INITIALIZER_KEY_WELCOME =
		"com.liferay.site.initializer.welcome";

	private static final Log _log = LogFactoryUtil.getLog(
		AddDefaultLayoutInitialRequestPortalInstanceLifecycleListener.class);

	private BundleTracker<?> _bundleTracker;

	@Reference
	private CompanyLocalService _companyLocalService;

	@Reference
	private PermissionCheckerFactory _defaultPermissionCheckerFactory;

	@Reference
	private FriendlyURLNormalizer _friendlyURLNormalizer;

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private LayoutLocalService _layoutLocalService;

	@Reference
	private LayoutServiceContextHelper _layoutServiceContextHelper;

	@Reference
	private Portal _portal;

	@Reference
	private PortletLocalService _portletLocalService;

	@Reference
	private PrefsProps _prefsProps;

	@Reference
	private RoleLocalService _roleLocalService;

	@Reference
	private SiteInitializerRegistry _siteInitializerRegistry;

	@Reference
	private SiteResource.Factory _siteResourceFactory;

	@Reference
	private ThemeLocalService _themeLocalService;

	@Reference
	private UserLocalService _userLocalService;

	private class SiteInitializerClientExtension
		implements BundleTrackerCustomizer<Bundle> {

		@Override
		public Bundle addingBundle(Bundle bundle, BundleEvent bundleEvent) {
			Dictionary<String, String> headers = bundle.getHeaders(
				StringPool.BLANK);

			if (Validator.isNull(
					headers.get("Liferay-Client-Extension-Site-Initializer")) ||
				_isAlreadyProcessed(bundle)) {

				return null;
			}

			PermissionChecker permissionChecker =
				PermissionThreadLocal.getPermissionChecker();
			String name = PrincipalThreadLocal.getName();

			try {
				_initialize(bundle, headers);
			}
			catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
			finally {
				PermissionThreadLocal.setPermissionChecker(permissionChecker);
				PrincipalThreadLocal.setName(name);
			}

			return bundle;
		}

		@Override
		public void modifiedBundle(
			Bundle bundle, BundleEvent bundleEvent, Bundle unusedBundle) {
		}

		@Override
		public void removedBundle(
			Bundle bundle, BundleEvent bundleEvent, Bundle unusedBundle) {
		}

		private Site _addOrUpdateSite(
				String externalReferenceCode, MultipartBody multipartBody,
				User user)
			throws Exception {

			ServiceContext serviceContext =
				ServiceContextThreadLocal.getServiceContext();

			HttpServletRequest httpServletRequest = serviceContext.getRequest();

			ThemeDisplay themeDisplay =
				(ThemeDisplay)httpServletRequest.getAttribute(
					WebKeys.THEME_DISPLAY);

			// LayoutServiceContextHelper#getServiceContextAutoCloseable ensures
			// the wrapped HTTP servlet request has an attribute for WebKeys#
			// LAYOUT and WebKeys#THEME_DISPLAY. However, fragments are
			// processed with com.liferay.taglib.portletext.RuntimeTag which
			// grabs the original HTTP servlet request.

			httpServletRequest.setAttribute(
				WebKeys.LAYOUT, themeDisplay.getLayout());
			httpServletRequest.setAttribute(
				WebKeys.THEME_DISPLAY, themeDisplay);

			SiteResource.Builder builder = _siteResourceFactory.create();

			SiteResource siteResource = builder.user(
				user
			).httpServletRequest(
				httpServletRequest
			).build();

			return siteResource.putSiteByExternalReferenceCode(
				externalReferenceCode, multipartBody);
		}

		private void _initialize(
				Bundle bundle, Dictionary<String, String> headers)
			throws Throwable {

			Map<String, BinaryFile> binaryFiles = new HashMap<>();
			Site site = null;

			Enumeration<URL> enumeration = bundle.findEntries(
				headers.get("Liferay-Client-Extension-Site-Initializer"), "*",
				true);

			while (enumeration.hasMoreElements()) {
				URL url = enumeration.nextElement();

				if (StringUtil.endsWith(
						url.getPath(), "site-initializer.json")) {

					String json = SiteInitializerUtil.read(
						bundle, "site-initializer.json", url);

					site = Site.toDTO(json);

					if (site == null) {
						throw new Exception(
							"Unable to transform site from JSON: " + json);
					}
				}
				else if (StringUtil.endsWith(
							url.getPath(), "site-initializer.zip")) {

					URLConnection urlConnection = url.openConnection();

					binaryFiles.put(
						"file",
						new BinaryFile(
							".zip", "site-initializer",
							urlConnection.getInputStream(),
							urlConnection.getContentLength()));
				}
			}

			String webId = GetterUtil.getString(
				headers.get("Liferay-Virtual-Instance-Id"), "default");

			if (Objects.equals(webId, "default")) {
				webId = PropsUtil.get(PropsKeys.COMPANY_DEFAULT_WEB_ID);
			}

			Company company = _companyLocalService.getCompanyByWebId(webId);

			long companyId = company.getCompanyId();

			try (SafeCloseable safeCloseable =
					CompanyThreadLocal.setCompanyIdWithSafeCloseable(
						companyId)) {

				List<User> users = _userLocalService.getUsersByRoleName(
					companyId, RoleConstants.ADMINISTRATOR, 0, 1);

				TransactionInvokerUtil.invoke(
					_transactionConfig,
					new SiteCallable(
						company,
						MultipartBody.of(
							binaryFiles, __ -> _objectMapper,
							Collections.singletonMap("site", site.toString())),
						site, users.get(0)));
			}
		}

		private boolean _isAlreadyProcessed(Bundle bundle) {
			String lastModifiedString = String.valueOf(
				bundle.getLastModified());

			File file = bundle.getDataFile(
				".liferay-client-extension-site-initializer");

			try {
				if ((file != null) && file.exists() &&
					Objects.equals(FileUtil.read(file), lastModifiedString)) {

					return true;
				}

				if (!file.exists()) {
					file.createNewFile();
				}

				FileUtil.write(file, lastModifiedString, true);
			}
			catch (IOException ioException) {
				ReflectionUtil.throwException(ioException);
			}

			return false;
		}

		private static final ObjectMapper _objectMapper = new ObjectMapper();
		private static final TransactionConfig _transactionConfig =
			TransactionConfig.Factory.create(
				Propagation.REQUIRED, new Class<?>[] {Exception.class});

		private class SiteCallable implements Callable<Site> {

			@Override
			public Site call() throws Exception {
				try (AutoCloseable autoCloseable =
						_layoutServiceContextHelper.
							getServiceContextAutoCloseable(_company)) {

					return _addOrUpdateSite(
						_site.getExternalReferenceCode(), _multipartBody,
						_user);
				}
				catch (Exception exception) {
					PermissionCacheUtil.clearCache(_user.getUserId());

					throw exception;
				}
			}

			private SiteCallable(
				Company company, MultipartBody multipartBody, Site site,
				User user) {

				_company = company;
				_multipartBody = multipartBody;
				_site = site;
				_user = user;
			}

			private final Company _company;
			private final MultipartBody _multipartBody;
			private final Site _site;
			private final User _user;

		}

	}

}