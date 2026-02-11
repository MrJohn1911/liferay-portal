/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.workflow.kaleo.service.test;

import com.liferay.account.constants.AccountConstants;
import com.liferay.account.model.AccountEntry;
import com.liferay.account.service.AccountEntryLocalService;
import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.test.AssertUtils;
import com.liferay.portal.kernel.test.rule.DataGuard;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.workflow.WorkflowException;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.workflow.constants.WorkflowDefinitionConstants;
import com.liferay.portal.workflow.kaleo.exception.NoSuchDefinitionException;
import com.liferay.portal.workflow.kaleo.model.KaleoDefinition;
import com.liferay.portal.workflow.kaleo.service.KaleoDefinitionLocalService;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Inácio Nery
 */
@DataGuard(scope = DataGuard.Scope.METHOD)
@RunWith(Arquillian.class)
public class KaleoDefinitionLocalServiceTest
	extends BaseKaleoLocalServiceTestCase {

	@Test
	public void testAddKaleoDefinition() throws Exception {
		KaleoDefinition kaleoDefinition = addKaleoDefinition();

		Assert.assertEquals(1, kaleoDefinition.getVersion());

		_testAddKaleoDefinitionWithAIScope();
	}

	@Test
	public void testDeactivateKaleoDefinition() throws Exception {
		KaleoDefinition kaleoDefinition = addKaleoDefinition();

		deactivateKaleoDefinition(kaleoDefinition);

		Assert.assertFalse(kaleoDefinition.isActive());
	}

	@Test(expected = WorkflowException.class)
	public void testDeleteKaleoDefinition1() throws Exception {
		KaleoDefinition kaleoDefinition = addKaleoDefinition();

		deleteKaleoDefinition(kaleoDefinition);
	}

	@Test(expected = NoSuchDefinitionException.class)
	public void testDeleteKaleoDefinition2() throws Exception {
		KaleoDefinition kaleoDefinition = addKaleoDefinition();

		deactivateKaleoDefinition(kaleoDefinition);

		deleteKaleoDefinition(kaleoDefinition);

		_kaleoDefinitionLocalService.getKaleoDefinition(
			kaleoDefinition.getKaleoDefinitionId());
	}

	@Test
	public void testUpdateKaleoDefinitionShouldIncrementVersion1()
		throws Exception {

		KaleoDefinition kaleoDefinition = addKaleoDefinition();

		kaleoDefinition = updateKaleoDefinition(kaleoDefinition);

		Assert.assertEquals(2, kaleoDefinition.getVersion());

		_testUpdateKaleoDefinitionWithAIScope();
	}

	private void _testAddKaleoDefinitionWithAIScope() throws Exception {
		serviceContext.setScopeGroupId(0);

		AssertUtils.assertFailure(
			WorkflowException.class, "groupId is required when scope is \"ai\"",
			() -> addKaleoDefinition(
				StringUtil.randomString(), StringUtil.randomString(),
				WorkflowDefinitionConstants.SCOPE_AI, StringUtil.randomString(),
				StringUtil.randomString()));

		serviceContext.setScopeGroupId(TestPropsValues.getGroupId());

		AssertUtils.assertFailure(
			WorkflowException.class,
			StringBundler.concat(
				"The group ", TestPropsValues.getGroupId(),
				" is not related to any account entry"),
			() -> addKaleoDefinition(
				StringUtil.randomString(), StringUtil.randomString(),
				WorkflowDefinitionConstants.SCOPE_AI, StringUtil.randomString(),
				StringUtil.randomString()));

		AccountEntry accountEntry = _accountEntryLocalService.addAccountEntry(
			RandomTestUtil.randomString(), TestPropsValues.getUserId(),
			AccountConstants.PARENT_ACCOUNT_ENTRY_ID_DEFAULT,
			RandomTestUtil.randomString(), RandomTestUtil.randomString(), null,
			RandomTestUtil.randomString() + "@liferay.com", null,
			RandomTestUtil.randomString(),
			AccountConstants.ACCOUNT_ENTRY_TYPE_BUSINESS,
			WorkflowConstants.STATUS_APPROVED, serviceContext);

		serviceContext.setScopeGroupId(accountEntry.getAccountEntryGroupId());

		Assert.assertNotNull(
			addKaleoDefinition(
				StringUtil.randomString(), StringUtil.randomString(),
				WorkflowDefinitionConstants.SCOPE_AI, StringUtil.randomString(),
				StringUtil.randomString()));
	}

	private void _testUpdateKaleoDefinitionWithAIScope() throws Exception {
		AccountEntry accountEntry = _accountEntryLocalService.addAccountEntry(
			RandomTestUtil.randomString(), TestPropsValues.getUserId(),
			AccountConstants.PARENT_ACCOUNT_ENTRY_ID_DEFAULT,
			RandomTestUtil.randomString(), RandomTestUtil.randomString(), null,
			RandomTestUtil.randomString() + "@liferay.com", null,
			RandomTestUtil.randomString(),
			AccountConstants.ACCOUNT_ENTRY_TYPE_BUSINESS,
			WorkflowConstants.STATUS_APPROVED, serviceContext);

		serviceContext.setScopeGroupId(accountEntry.getAccountEntryGroupId());

		KaleoDefinition kaleoDefinition = addKaleoDefinition(
			StringUtil.randomString(), StringUtil.randomString(),
			WorkflowDefinitionConstants.SCOPE_AI, StringUtil.randomString(),
			StringUtil.randomString());

		serviceContext.setScopeGroupId(0);

		AssertUtils.assertFailure(
			WorkflowException.class, "groupId is required when scope is \"ai\"",
			() -> updateKaleoDefinition(kaleoDefinition));

		serviceContext.setScopeGroupId(TestPropsValues.getGroupId());

		AssertUtils.assertFailure(
			WorkflowException.class,
			StringBundler.concat(
				"The group ", TestPropsValues.getGroupId(),
				" is not related to any account entry"),
			() -> updateKaleoDefinition(kaleoDefinition));

		serviceContext.setScopeGroupId(accountEntry.getAccountEntryGroupId());

		Assert.assertNotNull(
			addKaleoDefinition(
				StringUtil.randomString(), StringUtil.randomString(),
				WorkflowDefinitionConstants.SCOPE_AI, StringUtil.randomString(),
				StringUtil.randomString()));
	}

	@Inject
	private AccountEntryLocalService _accountEntryLocalService;

	@Inject
	private KaleoDefinitionLocalService _kaleoDefinitionLocalService;

}