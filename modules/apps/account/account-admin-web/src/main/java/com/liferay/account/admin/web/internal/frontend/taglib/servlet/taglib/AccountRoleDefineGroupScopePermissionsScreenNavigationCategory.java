package com.liferay.account.admin.web.internal.frontend.taglib.servlet.taglib;

import com.liferay.account.admin.web.internal.constants.AccountScreenNavigationEntryConstants;
import com.liferay.frontend.taglib.servlet.taglib.ScreenNavigationCategory;

import org.osgi.service.component.annotations.Component;

@Component(
	property = {"screen.navigation.category.order:Integer=20"},
	service = ScreenNavigationCategory.class
)
public class AccountRoleDefineGroupScopePermissionsScreenNavigationCategory
	extends BaseAccountRoleDefinePermissionsScreenNavigationCategory {

	@Override
	public String getCategoryKey() {
		return AccountScreenNavigationEntryConstants.
			CATEGORY_KEY_DEFINE_GROUP_SCOPE_PERMISSIONS;
	}

}