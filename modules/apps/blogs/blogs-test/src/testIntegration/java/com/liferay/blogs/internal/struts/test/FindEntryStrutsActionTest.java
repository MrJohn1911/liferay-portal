/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.blogs.internal.struts.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.configuration.test.util.CompanyConfigurationTemporarySwapper;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.struts.StrutsAction;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author Joao Victor Alves
 */
@RunWith(Arquillian.class)
public class FindEntryStrutsActionTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@BeforeClass
	public static void setUpClass() throws Exception {
		_group = GroupTestUtil.addGroup();

		_user = UserTestUtil.getAdminUser(_group.getCompanyId());

		UserTestUtil.setUser(_user);
	}

	@Test
	public void testAllowedDomainsCanRedirect() throws Exception {
		try (CompanyConfigurationTemporarySwapper
				companyConfigurationTemporarySwapper =
					new CompanyConfigurationTemporarySwapper(
						TestPropsValues.getCompanyId(),
						"com.liferay.redirect.internal.configuration." +
							"RedirectURLConfiguration",
						HashMapDictionaryBuilder.<String, Object>put(
							"allowedDomains",
							new String[] {
								"localhost", "PORTAL_DOMAINS", "*.google.com"
							}
						).put(
							"securityMode", "domain"
						).build())) {

			MockHttpServletRequest mockHttpServletRequest =
				new MockHttpServletRequest();

			mockHttpServletRequest.addParameter(
				"noSuchEntryRedirect", "https://mail.google.com");

			MockHttpServletResponse mockHttpServletResponse =
				new MockHttpServletResponse();

			_editInfoItemStrutsAction.execute(
				mockHttpServletRequest, mockHttpServletResponse);

			Assert.assertEquals(
				"https://mail.google.com",
				mockHttpServletResponse.getRedirectedUrl());
		}
	}

	@DeleteAfterTestRun
	private static Group _group;

	private static User _user;

	@Inject(
		filter = "component.name=com.liferay.blogs.web.internal.struts.FindEntryStrutsAction"
	)
	private StrutsAction _editInfoItemStrutsAction;

}