/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getSiteByExternalReferenceCode} from '../../../src/main/resources/META-INF/resources/js/services/sites';

const fetchMock = fetch as any;

describe('sites service', () => {
	it('requests the site by an encoded external reference code', async () => {
		fetchMock.mockResponseOnce(JSON.stringify({friendlyUrlPath: '/store'}));

		const site = await getSiteByExternalReferenceCode('A/B C');

		expect(site).toEqual({friendlyUrlPath: '/store'});
		expect(fetchMock.mock.calls[fetchMock.mock.calls.length - 1][0]).toBe(
			'/o/headless-admin-site/v1.0/sites/by-external-reference-code/A%2FB%20C'
		);
	});

	it('returns null when the site is not found', async () => {
		fetchMock.mockResponseOnce('', {status: 404});

		const site = await getSiteByExternalReferenceCode('missing');

		expect(site).toBeNull();
	});
});
