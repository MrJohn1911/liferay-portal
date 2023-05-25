package com.liferay.portal.search.elasticsearch7.internal.index.util;

import java.util.HashSet;
import java.util.Set;

public class CompanyIdRegisterUtil {

	public static Set<Long> getCompanyIds() {
		return _companyIds;
	}

	public static synchronized void registerCompanyId(long companyId) {
		_companyIds.add(companyId);
	}

	public static synchronized void unregisterCompanyId(long companyId) {
		_companyIds.remove(companyId);
	}

	private static final Set<Long> _companyIds = new HashSet<>();

}