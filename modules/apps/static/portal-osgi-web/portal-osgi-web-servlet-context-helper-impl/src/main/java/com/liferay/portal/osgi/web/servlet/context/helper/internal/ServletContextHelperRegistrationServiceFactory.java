/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.osgi.web.servlet.context.helper.internal;

import com.liferay.petra.executor.PortalExecutorManager;
import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.portal.osgi.web.servlet.JSPServletFactory;
import com.liferay.portal.osgi.web.servlet.context.helper.ServletContextHelperRegistration;

import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * @author Raymond Augé
 */
@Component(service = ServiceFactory.class)
public class ServletContextHelperRegistrationServiceFactory
	implements ServiceFactory<ServletContextHelperRegistration> {

	@Override
	public ServletContextHelperRegistration getService(
		Bundle bundle,
		ServiceRegistration<ServletContextHelperRegistration>
			serviceRegistration) {

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

		return new ServletContextHelperRegistrationImpl(
			bundle, _jspServletFactory, _saxParserFactory,
			new HashMap<String, Object>(),
			_portalExecutorManager.getPortalExecutor(
				ServletContextHelperRegistrationServiceFactory.class.
					getName()));
	}

	@Override
	public void ungetService(
		Bundle bundle,
		ServiceRegistration<ServletContextHelperRegistration>
			serviceRegistration,
		ServletContextHelperRegistration servletContextHelperRegistration) {

		servletContextHelperRegistration.close();
	}

	private static final String _FEATURES_DISALLOW_DOCTYPE_DECL =
		"http://apache.org/xml/features/disallow-doctype-decl";

	private static final String _FEATURES_EXTERNAL_GENERAL_ENTITIES =
		"http://xml.org/sax/features/external-general-entities";

	private static final String _FEATURES_EXTERNAL_PARAMETER_ENTITIES =
		"http://xml.org/sax/features/external-parameter-entities";

	private static final String _FEATURES_LOAD_EXTERNAL_DTD =
		"http://apache.org/xml/features/nonvalidating/load-external-dtd";

	@Reference
	private JSPServletFactory _jspServletFactory;

	@Reference
	private PortalExecutorManager _portalExecutorManager;

	@Reference
	private SAXParserFactory _saxParserFactory;

}