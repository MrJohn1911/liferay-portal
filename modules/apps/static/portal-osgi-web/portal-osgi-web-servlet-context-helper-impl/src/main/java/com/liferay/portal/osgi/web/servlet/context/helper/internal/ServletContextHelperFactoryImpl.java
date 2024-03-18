/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.osgi.web.servlet.context.helper.internal;

import com.liferay.petra.concurrent.DCLSingleton;
import com.liferay.petra.executor.PortalExecutorManager;
import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.portal.osgi.web.servlet.JSPServletFactory;
import com.liferay.portal.osgi.web.servlet.context.helper.ServletContextHelperFactory;
import com.liferay.portal.osgi.web.servlet.context.helper.ServletContextHelperRegistration;

import java.util.Collections;
import java.util.concurrent.ExecutorService;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.runtime.HttpServiceRuntime;

import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * @author Raymond Augé
 */
@Component(service = ServletContextHelperFactory.class)
public class ServletContextHelperFactoryImpl
	implements ServletContextHelperFactory {

	@Override
	public void ensureReady() {
		if (_ready) {
			return;
		}

		_ready = true;

		_saxParserFactory.setNamespaceAware(false);
		_saxParserFactory.setValidating(false);
		_saxParserFactory.setXIncludeAware(false);

		try {
			_saxParserFactory.setFeature(_FEATURES_DISALLOW_DOCTYPE_DECL, true);
			_saxParserFactory.setFeature(
				_FEATURES_EXTERNAL_GENERAL_ENTITIES, false);
			_saxParserFactory.setFeature(
				_FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
			_saxParserFactory.setFeature(_FEATURES_LOAD_EXTERNAL_DTD, false);
		}
		catch (ParserConfigurationException | SAXNotRecognizedException |
			   SAXNotSupportedException exception) {

			ReflectionUtil.throwException(exception);
		}

		_executorService = _portalExecutorManager.getPortalExecutor(
			ServletContextHelperFactoryImpl.class.getName());

		Bundle bundle = FrameworkUtil.getBundle(
			ServletContextHelperRegistrationImpl.class);

		BundleContext bundleContext = bundle.getBundleContext();

		_serviceRegistrationDCLSingleton.getSingleton(
			() -> bundleContext.registerService(
				ServletContextHelperRegistration.class.getName(),
				new ServletContextHelperRegistrationServiceFactory(
					_jspServletFactory, _saxParserFactory,
					Collections.emptyMap(), _executorService),
				null));
	}

	@Deactivate
	protected void deactivate() throws Exception {
		_serviceRegistrationDCLSingleton.destroy(
			ServiceRegistration::unregister);

		_executorService.shutdownNow();
	}

	private static final String _FEATURES_DISALLOW_DOCTYPE_DECL =
		"http://apache.org/xml/features/disallow-doctype-decl";

	private static final String _FEATURES_EXTERNAL_GENERAL_ENTITIES =
		"http://xml.org/sax/features/external-general-entities";

	private static final String _FEATURES_EXTERNAL_PARAMETER_ENTITIES =
		"http://xml.org/sax/features/external-parameter-entities";

	private static final String _FEATURES_LOAD_EXTERNAL_DTD =
		"http://apache.org/xml/features/nonvalidating/load-external-dtd";

	private ExecutorService _executorService;

	@Reference
	private HttpServiceRuntime _httpServiceRuntime;

	@Reference
	private JSPServletFactory _jspServletFactory;

	@Reference
	private PortalExecutorManager _portalExecutorManager;

	private boolean _ready;

	@Reference
	private SAXParserFactory _saxParserFactory;

	private final DCLSingleton<ServiceRegistration<?>>
		_serviceRegistrationDCLSingleton = new DCLSingleton<>();

}