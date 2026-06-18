/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {
	commitGeneration,
	createGeneration,
	getGeneration,
	getGenerationItems,
	getGenerationPages,
} from '../../../src/main/resources/META-INF/resources/js/services/generations';

import type {GenerationItem} from '../../../src/main/resources/META-INF/resources/js/types/GenerationItem';

const API_URL = '/o/c/csggenerations';

const fetchMock = fetch as any;

function lastCall() {
	return fetchMock.mock.calls[fetchMock.mock.calls.length - 1];
}

describe('generations service', () => {
	describe('createGeneration', () => {
		it('posts the prompt and title as a refining generation', async () => {
			fetchMock.mockResponseOnce(
				JSON.stringify({
					externalReferenceCode: 'erc',
					generationStatus: {key: 'refining'},
					id: 9,
					prompt: 'Build a store',
					title: 'Build a store',
				})
			);

			const generation = await createGeneration(API_URL, {
				prompt: 'Build a store',
				title: 'Build a store',
			});

			expect(generation.id).toBe(9);

			const [url, init] = lastCall();

			expect(url).toBe(API_URL);
			expect(init.method).toBe('POST');
			expect(JSON.parse(init.body)).toEqual({
				generationStatus: {key: 'refining'},
				prompt: 'Build a store',
				title: 'Build a store',
			});
		});
	});

	describe('getGeneration', () => {
		it('requests the generation by id', async () => {
			fetchMock.mockResponseOnce(
				JSON.stringify({
					externalReferenceCode: 'erc',
					generationStatus: {key: 'ready'},
					id: 3,
					prompt: 'p',
					title: 't',
				})
			);

			const generation = await getGeneration(API_URL, 3);

			expect(generation.generationStatus.key).toBe('ready');
			expect(lastCall()[0]).toBe(`${API_URL}/3`);
		});
	});

	describe('getGenerationItems', () => {
		it('returns the items sorted by load order', async () => {
			fetchMock.mockResponseOnce(
				JSON.stringify({
					items: [
						{externalReferenceCode: 'i1', fileName: 'f', id: 1},
					],
				})
			);

			const items = await getGenerationItems(API_URL, 3);

			expect(items).toHaveLength(1);
			expect(lastCall()[0]).toBe(
				`${API_URL}/3/items?pageSize=100&sort=loadOrder:asc`
			);
		});

		it('returns an empty array when the response has no items', async () => {
			fetchMock.mockResponseOnce(JSON.stringify({}));

			const items = await getGenerationItems(API_URL, 3);

			expect(items).toEqual([]);
		});
	});

	describe('commitGeneration', () => {
		it('puts to the commit object action and resolves on a 204', async () => {
			fetchMock.mockResponseOnce('', {status: 204});

			await expect(commitGeneration(API_URL, 5)).resolves.toBeUndefined();

			const [url, init] = lastCall();

			expect(url).toBe(`${API_URL}/5/object-actions/commit`);
			expect(init.method).toBe('PUT');
		});
	});

	describe('error handling', () => {
		it('throws the problem detail title from the error body', async () => {
			fetchMock.mockResponseOnce(
				JSON.stringify({title: 'Prompt is required'}),
				{status: 400}
			);

			await expect(getGeneration(API_URL, 1)).rejects.toThrow(
				'Prompt is required'
			);
		});

		it('falls back to the status text when the error body is not json', async () => {
			fetchMock.mockResponseOnce('<html>nope</html>', {
				status: 503,
				statusText: 'Service Unavailable',
			});

			await expect(getGeneration(API_URL, 1)).rejects.toThrow(
				'503 Service Unavailable'
			);
		});
	});

	describe('getGenerationPages', () => {
		const pagesItem: GenerationItem = {
			batchFile: {link: {href: '/documents/batch/06-pages.json'}},
			externalReferenceCode: 'i1',
			fileName: '06-pages.batch-engine-data.json',
			id: 1,
			languages: 'en,es',
		};

		it('reads pages from a batch file whose body is a json array', async () => {
			fetchMock.mockResponseOnce(
				JSON.stringify([
					{friendlyUrlPath: '/home', title: 'Home'},
					{title: 'About'},
				])
			);

			const pages = await getGenerationPages([pagesItem]);

			expect(pages).toHaveLength(2);
			expect(pages[0]).toMatchObject({
				icon: 'page',
				id: '1-0',
				languages: ['en', 'es'],
				title: 'Home',
				url: '/home',
			});
			expect(pages[1].title).toBe('About');
			expect(pages[1].url).toBeNull();
		});

		it('reads pages from a batch file whose body wraps the items', async () => {
			fetchMock.mockResponseOnce(
				JSON.stringify({items: [{name: 'Solo', urlTitle: '/solo'}]})
			);

			const pages = await getGenerationPages([pagesItem]);

			expect(pages).toHaveLength(1);
			expect(pages[0].title).toBe('Solo');
			expect(pages[0].url).toBe('/solo');
		});

		it('ignores a batch file over the size limit and uses the preview item', async () => {
			fetchMock.mockResponseOnce('x'.repeat(2 * 1024 * 1024 + 1));

			const pages = await getGenerationPages([
				{
					...pagesItem,
					previewItem: JSON.stringify({
						friendlyUrlPath: '/preview',
						title: 'Preview Home',
					}),
				},
			]);

			expect(pages).toHaveLength(1);
			expect(pages[0].title).toBe('Preview Home');
			expect(pages[0].url).toBe('/preview');
		});

		it('returns no pages when the batch fetch fails and there is no preview item', async () => {
			fetchMock.mockRejectOnce(new Error('network'));

			const pages = await getGenerationPages([pagesItem]);

			expect(pages).toEqual([]);
		});

		it('falls back to the preview item when there is no batch file', async () => {
			const pages = await getGenerationPages([
				{
					externalReferenceCode: 'i2',
					fileName: '07-blogs.batch-engine-data.json',
					id: 2,
					languages: 'en',
					previewItem: JSON.stringify({headline: 'My Post'}),
				},
			]);

			expect(pages).toHaveLength(1);
			expect(pages[0].icon).toBe('document-text');
			expect(pages[0].title).toBe('My Post');
		});
	});
});
