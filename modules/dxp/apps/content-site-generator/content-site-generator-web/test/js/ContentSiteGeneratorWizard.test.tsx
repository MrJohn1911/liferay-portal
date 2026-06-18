/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {act, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {openToast} from 'frontend-js-components-web';
import React from 'react';

import '@testing-library/jest-dom';

import ContentSiteGeneratorWizard from '../../src/main/resources/META-INF/resources/js/ContentSiteGeneratorWizard';

import type {Generation} from '../../src/main/resources/META-INF/resources/js/types/Generation';

jest.mock(
	'@liferay/ai-hub-cell-js-components-web',
	() => ({
		AIAssistantChat: () => null,
		ChatContext: {},
	}),
	{virtual: true}
);

jest.mock(
	'frontend-js-components-web',
	() => ({
		openToast: jest.fn(),
	}),
	{virtual: true}
);

const API_URL = '/o/c/csggenerations';
const GENERATIONS_URL = '/group/guest/~/control_panel/manage?p=csg';
const POLL_INTERVAL = 3000;

const readyGeneration: Generation = {
	externalReferenceCode: 'erc',
	generationStatus: {key: 'ready'},
	id: 42,
	prompt: 'Build a marketing site',
	title: 'Marketing site',
};

const fetchMock = fetch as any;

async function flushPromises() {
	for (let index = 0; index < 5; index++) {
		await act(async () => {
			await Promise.resolve();
		});
	}
}

beforeEach(() => {
	jest.clearAllMocks();
});

afterEach(() => {
	jest.useRealTimers();
});

describe('ContentSiteGeneratorWizard', () => {
	it('shows the ideate step when there is no generation', () => {
		render(
			<ContentSiteGeneratorWizard
				apiURL={API_URL}
				generationsURL={GENERATIONS_URL}
			/>
		);

		expect(
			screen.getByText('what-do-you-want-to-create')
		).toBeInTheDocument();
		expect(
			screen.getByRole('button', {name: /analyze-and-configure/})
		).toBeInTheDocument();
	});

	it('routes to the review step when the generation is already ready', async () => {
		fetchMock.mockResponseOnce(JSON.stringify(readyGeneration));
		fetchMock.mockResponseOnce(JSON.stringify({items: []}));

		render(
			<ContentSiteGeneratorWizard
				apiURL={API_URL}
				generationId={42}
				generationsURL={GENERATIONS_URL}
			/>
		);

		expect(
			await screen.findByRole('button', {name: 'publish'})
		).toBeInTheDocument();
	});

	it('routes to the refine step and shows progress while generating', async () => {
		fetchMock.mockResponseOnce(
			JSON.stringify({
				...readyGeneration,
				generationStatus: {key: 'generating'},
			})
		);
		fetchMock.mockResponseOnce(JSON.stringify({items: []}));

		render(
			<ContentSiteGeneratorWizard
				apiURL={API_URL}
				generationId={42}
				generationsURL={GENERATIONS_URL}
			/>
		);

		expect(await screen.findByText('generate')).toBeInTheDocument();
		expect(screen.getByText('analyzing-your-prompt')).toBeInTheDocument();
	});

	it('shows an error when the initial load fails', async () => {
		fetchMock.mockResponseOnce(JSON.stringify({title: 'Boom'}), {
			status: 500,
		});
		fetchMock.mockResponseOnce(JSON.stringify({items: []}));

		render(
			<ContentSiteGeneratorWizard
				apiURL={API_URL}
				generationId={42}
				generationsURL={GENERATIONS_URL}
			/>
		);

		expect(await screen.findByText('Boom')).toBeInTheDocument();
		expect(
			screen.getByText('what-do-you-want-to-create')
		).toBeInTheDocument();
	});

	it('creates a generation and advances to refine when a prompt is analyzed', async () => {
		fetchMock.mockResponseOnce(
			JSON.stringify({
				...readyGeneration,
				generationStatus: {key: 'refining'},
			})
		);

		render(
			<ContentSiteGeneratorWizard
				apiURL={API_URL}
				generationsURL={GENERATIONS_URL}
			/>
		);

		await userEvent.type(
			screen.getByLabelText('describe-your-content'),
			'Build a blog'
		);
		await userEvent.click(
			screen.getByRole('button', {name: /analyze-and-configure/})
		);

		expect(await screen.findByText('generate')).toBeInTheDocument();

		const [url, init] = fetchMock.mock.calls[0];

		expect(url).toBe(API_URL);
		expect(init.method).toBe('POST');
		expect(JSON.parse(init.body)).toMatchObject({
			generationStatus: {key: 'refining'},
			prompt: 'Build a blog',
		});
	});

	it('commits the generation and navigates away after publishing', async () => {
		fetchMock.mockResponseOnce(JSON.stringify(readyGeneration));
		fetchMock.mockResponseOnce(JSON.stringify({items: []}));
		fetchMock.mockResponseOnce('', {status: 204});

		render(
			<ContentSiteGeneratorWizard
				apiURL={API_URL}
				generationId={42}
				generationsURL={GENERATIONS_URL}
			/>
		);

		await userEvent.click(
			await screen.findByRole('button', {name: 'publish'})
		);

		await waitFor(() =>
			expect(Liferay.Util.navigate).toHaveBeenCalledWith(GENERATIONS_URL)
		);

		expect(openToast).toHaveBeenCalled();

		const commitCall = fetchMock.mock.calls.find(
			(call: unknown[]) =>
				typeof call[0] === 'string' &&
				(call[0] as string).includes('/object-actions/commit')
		);

		expect(commitCall).toBeDefined();
		expect(commitCall[1].method).toBe('PUT');
	});

	it('polls while generating and stops once the generation is ready', async () => {
		jest.useFakeTimers();

		let statusKey = 'generating';

		fetchMock.mockResponse((request: any) => {
			if (request.url.includes('/items')) {
				return Promise.resolve(JSON.stringify({items: []}));
			}

			return Promise.resolve(
				JSON.stringify({
					...readyGeneration,
					generationStatus: {key: statusKey},
				})
			);
		});

		render(
			<ContentSiteGeneratorWizard
				apiURL={API_URL}
				generationId={42}
				generationsURL={GENERATIONS_URL}
			/>
		);

		await flushPromises();

		expect(screen.getByText('generate')).toBeInTheDocument();

		const callsAfterLoad = fetchMock.mock.calls.length;

		await act(async () => {
			jest.advanceTimersByTime(POLL_INTERVAL);
		});
		await flushPromises();

		expect(fetchMock.mock.calls.length).toBeGreaterThan(callsAfterLoad);

		statusKey = 'ready';

		await act(async () => {
			jest.advanceTimersByTime(POLL_INTERVAL);
		});
		await flushPromises();

		const callsAfterReady = fetchMock.mock.calls.length;

		await act(async () => {
			jest.advanceTimersByTime(POLL_INTERVAL * 3);
		});
		await flushPromises();

		expect(fetchMock.mock.calls.length).toBe(callsAfterReady);
	});
});
