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

package com.liferay.commerce.discount.internal.target;

import com.liferay.commerce.discount.model.CommerceDiscount;
import com.liferay.commerce.discount.model.CommerceDiscountRel;
import com.liferay.commerce.discount.service.CommerceDiscountRelLocalService;
import com.liferay.commerce.discount.target.CommerceDiscountProductTarget;
import com.liferay.commerce.product.model.CPDefinition;
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.portal.kernel.search.BooleanClauseOccur;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.filter.BooleanFilter;
import com.liferay.portal.kernel.search.filter.ExistsFilter;
import com.liferay.portal.kernel.search.filter.Filter;
import com.liferay.portal.kernel.search.filter.TermFilter;
import com.liferay.portal.kernel.search.filter.TermsFilter;
import com.liferay.portal.kernel.util.ArrayUtil;

import org.osgi.service.component.annotations.Reference;

/**
 * @author Joao Victor Alves
 */
public abstract class BaseCommerceDiscountProductTarget
	implements CommerceDiscountProductTarget {

	public void contributeDocument(
		Document document, CommerceDiscount commerceDiscount, String name) {

		document.addKeyword(
			name,
			TransformUtil.transformToLongArray(
				commerceDiscountRelLocalService.getCommerceDiscountRels(
					commerceDiscount.getCommerceDiscountId(),
					CPDefinition.class.getName()),
				CommerceDiscountRel::getClassPK));
	}

	public void postProcessContextBooleanFilter(
		BooleanFilter contextBooleanFilter, CPDefinition cpDefinition,
		String filter) {

		TermFilter termFilter = new TermFilter(
			filter, String.valueOf(cpDefinition.getCPDefinitionId()));

		_postProcessContextBooleanFilter(
			contextBooleanFilter, filter, termFilter, null);
	}

	public void postProcessContextBooleanFilter(
		BooleanFilter contextBooleanFilter, long[] pricingClassIds,
		String filter) {

		TermsFilter termsFilter = new TermsFilter(filter);

		termsFilter.addValues(ArrayUtil.toStringArray(pricingClassIds));

		_postProcessContextBooleanFilter(
			contextBooleanFilter, filter, null, termsFilter);
	}

	@Reference
	protected CommerceDiscountRelLocalService commerceDiscountRelLocalService;

	private void _postProcessContextBooleanFilter(
		BooleanFilter contextBooleanFilter, String filter,
		TermFilter termFilter, TermsFilter termsFilter) {

		Filter existFilter = new ExistsFilter(filter);

		BooleanFilter existBooleanFilter = new BooleanFilter();

		existBooleanFilter.add(existFilter, BooleanClauseOccur.MUST_NOT);

		BooleanFilter fieldBooleanFilter = new BooleanFilter();

		fieldBooleanFilter.add(existBooleanFilter, BooleanClauseOccur.SHOULD);

		if (termFilter != null) {
			fieldBooleanFilter.add(termFilter, BooleanClauseOccur.SHOULD);
		}
		else {
			fieldBooleanFilter.add(termsFilter, BooleanClauseOccur.SHOULD);
		}

		contextBooleanFilter.add(fieldBooleanFilter, BooleanClauseOccur.MUST);
	}

}