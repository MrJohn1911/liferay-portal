/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Button from '@clayui/button';
import {Option, Picker} from '@clayui/core';
import ClayForm, {ClayInput, ClayToggle} from '@clayui/form';
import Icon from '@clayui/icon';
import ClayLayout from '@clayui/layout';
import Link from '@clayui/link';
import ClayPanel from '@clayui/panel';
import {openToast} from '@liferay/object-js-components-web';
import React, {useCallback, useEffect, useMemo, useState} from 'react';

import './ModelArmorTemplateForm.scss';
import Toolbar from '../components/ToolBar';
import {
	getModelArmorTemplate,
	putModelArmorTemplate,
} from './services/ModelArmorTemplateService';
import {ModelArmorTemplate} from './types/ModelArmorTemplate';

type FormErrors = {
	externalReferenceCode?: string;
	name?: string;
};

export default function ModelArmorTemplateForm({
	accountEntryExternalReferenceCode,
	backURL,
	externalReferenceCode,
}: {
	accountEntryExternalReferenceCode: string;
	backURL: string;
	externalReferenceCode: string;
}) {
	const getDefaults = useCallback(
		(): ModelArmorTemplate => ({
			active: true,
			description: '',
			externalReferenceCode: '',
			guardrailType: 'input',
			maliciousUriFilterEnabled: false,
			multiLanguageDetectionEnabled: false,
			name: '',
			piAndJailbreakConfidenceLevel: 'mediumAndAbove',
			piAndJailbreakFilterEnabled: false,
			r_accountToAIHubModelArmorTemplates_accountEntryERC:
				accountEntryExternalReferenceCode,
			raiDangerousLevel: 'none',
			raiHarassmentLevel: 'none',
			raiHateSpeechLevel: 'none',
			raiSexuallyExplicitLevel: 'none',
			sdpFilterEnabled: false,
		}),
		[accountEntryExternalReferenceCode]
	);

	const [formData, setFormData] = useState<ModelArmorTemplate>(getDefaults);
	const [errors, setErrors] = useState<FormErrors>({});

	const confidenceOptions = useMemo(
		() => [
			{
				label: Liferay.Language.get('low-and-above'),
				value: 'lowAndAbove',
			},
			{
				label: Liferay.Language.get('medium-and-above'),
				value: 'mediumAndAbove',
			},
			{label: Liferay.Language.get('high'), value: 'high'},
		],
		[]
	);

	const guardrailTypeOptions = useMemo(
		() => [
			{label: Liferay.Language.get('input'), value: 'input'},
			{label: Liferay.Language.get('output'), value: 'output'},
		],
		[]
	);

	const raiLevelOptions = useMemo(
		() => [
			{label: Liferay.Language.get('none'), value: 'none'},
			{
				label: Liferay.Language.get('low-and-above'),
				value: 'lowAndAbove',
			},
			{
				label: Liferay.Language.get('medium-and-above'),
				value: 'mediumAndAbove',
			},
			{label: Liferay.Language.get('high'), value: 'high'},
		],
		[]
	);

	const raiFilters = useMemo<
		Array<{
			field: keyof ModelArmorTemplate;
			label: string;
		}>
	>(
		() => [
			{
				field: 'raiHateSpeechLevel',
				label: Liferay.Language.get('hate-speech'),
			},
			{
				field: 'raiDangerousLevel',
				label: Liferay.Language.get('dangerous'),
			},
			{
				field: 'raiSexuallyExplicitLevel',
				label: Liferay.Language.get('sexually-explicit'),
			},
			{
				field: 'raiHarassmentLevel',
				label: Liferay.Language.get('harassment'),
			},
		],
		[]
	);

	const handleInputChange = (
		event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
	) => {
		const {name, value} = event.target;
		setFormData((prev) => ({
			...prev,
			[name]: value,
		}));
	};

	const handleToggle = (name: keyof ModelArmorTemplate) => {
		setFormData((prev) => ({
			...prev,
			[name]: !prev[name],
		}));
	};

	const validateForm = (data: ModelArmorTemplate): FormErrors => {
		const validationErrors: FormErrors = {};

		if (!data.name) {
			validationErrors.name = Liferay.Language.get('name-is-required');
		}

		if (!data.externalReferenceCode) {
			validationErrors.externalReferenceCode = Liferay.Language.get(
				'external-reference-code-is-required'
			);
		}

		return validationErrors;
	};

	const handleSubmit = async () => {
		const validationErrors = validateForm(formData);

		if (Object.keys(validationErrors).length) {
			setErrors(validationErrors);

			return;
		}

		setErrors({});

		try {
			const response = await putModelArmorTemplate(formData);

			if (response?.externalReferenceCode) {
				openToast({
					message: Liferay.Language.get(
						'model-armor-template-saved-successfully'
					),
					type: 'success',
				});
			}
			else {
				openToast({
					message: Liferay.Language.get(
						'failed-to-save-model-armor-template'
					),
					type: 'danger',
				});
			}
		}
		catch (error) {
			console.error(error);

			openToast({
				message: Liferay.Language.get('an-unexpected-error-occurred'),
				type: 'danger',
			});
		}
	};

	useEffect(() => {
		async function fetchFormData() {
			if (!externalReferenceCode) {
				setFormData(getDefaults());

				return;
			}

			try {
				const modelArmorTemplate = await getModelArmorTemplate(
					externalReferenceCode
				);

				const pickKey = (value: any, fallback: string) =>
					value?.key || value || fallback;

				setFormData({
					active: modelArmorTemplate.active,
					description: modelArmorTemplate.description || '',
					externalReferenceCode:
						modelArmorTemplate.externalReferenceCode,
					guardrailType: pickKey(
						modelArmorTemplate.guardrailType,
						'input'
					),
					maliciousUriFilterEnabled:
						modelArmorTemplate.maliciousUriFilterEnabled ?? false,
					multiLanguageDetectionEnabled:
						modelArmorTemplate.multiLanguageDetectionEnabled ??
						false,
					name: modelArmorTemplate.name || '',
					piAndJailbreakConfidenceLevel: pickKey(
						modelArmorTemplate.piAndJailbreakConfidenceLevel,
						'mediumAndAbove'
					),
					piAndJailbreakFilterEnabled:
						modelArmorTemplate.piAndJailbreakFilterEnabled ?? false,
					r_accountToAIHubModelArmorTemplates_accountEntryERC:
						modelArmorTemplate.r_accountToAIHubModelArmorTemplates_accountEntryERC ||
						accountEntryExternalReferenceCode,
					raiDangerousLevel: pickKey(
						modelArmorTemplate.raiDangerousLevel,
						'none'
					),
					raiHarassmentLevel: pickKey(
						modelArmorTemplate.raiHarassmentLevel,
						'none'
					),
					raiHateSpeechLevel: pickKey(
						modelArmorTemplate.raiHateSpeechLevel,
						'none'
					),
					raiSexuallyExplicitLevel: pickKey(
						modelArmorTemplate.raiSexuallyExplicitLevel,
						'none'
					),
					sdpFilterEnabled:
						modelArmorTemplate.sdpFilterEnabled ?? false,
				});
			}
			catch (error) {
				openToast({
					message: Liferay.Language.get(
						'failed-to-load-model-armor-template-data'
					),
					type: 'danger',
				});
			}
		}

		fetchFormData();
	}, [accountEntryExternalReferenceCode, externalReferenceCode, getDefaults]);

	return (
		<>
			<Toolbar
				backURL={backURL}
				title={
					externalReferenceCode
						? Liferay.Language.get('edit-model-armor-template')
						: Liferay.Language.get('new-model-armor-template')
				}
			>
				<Toolbar.Item>
					<Link
						aria-label={Liferay.Language.get('cancel')}
						borderless
						button
						displayType="secondary"
						href={backURL}
						small
					>
						{Liferay.Language.get('cancel')}
					</Link>
				</Toolbar.Item>

				<Toolbar.Item>
					<Button
						aria-label={Liferay.Language.get('save')}
						onClick={handleSubmit}
						size="sm"
					>
						{Liferay.Language.get('save')}
					</Button>
				</Toolbar.Item>
			</Toolbar>

			<ClayLayout.ContainerFluid className="model-armor-template-form">
				<ClayForm>
					<ClayLayout.Row>
						<ClayLayout.Col md={12}>
							<ClayPanel
								className="model-armor-template-form-details"
								collapsable={false}
								title={Liferay.Language.get('details')}
							>
								<ClayPanel.Body>
									<div className="model-armor-template-form-header">
										<h2>
											{Liferay.Language.get('details')}
										</h2>

										<ClayToggle
											label={Liferay.Language.get(
												'active'
											)}
											onToggle={() =>
												handleToggle('active')
											}
											toggled={formData.active}
										/>
									</div>

									<ClayForm.Group>
										<label htmlFor="name">
											{Liferay.Language.get('name')}

											<span className="ml-1 reference-mark text-warning">
												<Icon symbol="asterisk" />
											</span>
										</label>

										<ClayInput
											className={
												errors.name ? 'is-invalid' : ''
											}
											id="name"
											name="name"
											onChange={handleInputChange}
											required
											type="text"
											value={formData.name || ''}
										/>

										{errors.name && (
											<div className="d-block invalid-feedback">
												{errors.name}
											</div>
										)}
									</ClayForm.Group>

									<ClayForm.Group>
										<label htmlFor="externalReferenceCode">
											{Liferay.Language.get(
												'external-reference-code'
											)}

											<span className="ml-1 reference-mark text-warning">
												<Icon symbol="asterisk" />
											</span>
										</label>

										<ClayInput
											className={
												errors.externalReferenceCode
													? 'is-invalid'
													: ''
											}
											id="externalReferenceCode"
											name="externalReferenceCode"
											onChange={handleInputChange}
											required
											type="text"
											value={
												formData.externalReferenceCode ||
												''
											}
										/>

										{errors.externalReferenceCode && (
											<div className="d-block invalid-feedback">
												{errors.externalReferenceCode}
											</div>
										)}
									</ClayForm.Group>

									<ClayForm.Group>
										<label htmlFor="description">
											{Liferay.Language.get(
												'description'
											)}
										</label>

										<textarea
											className="form-control"
											id="description"
											name="description"
											onChange={handleInputChange}
											rows={3}
											value={formData.description || ''}
										/>
									</ClayForm.Group>

									<ClayForm.Group>
										<label htmlFor="guardrailType">
											{Liferay.Language.get(
												'guardrail-type'
											)}
										</label>

										<Picker
											className="model-armor-template-form-picker"
											items={guardrailTypeOptions}
											onSelectionChange={(value) => {
												setFormData((prev) => ({
													...prev,
													guardrailType:
														value as string,
												}));
											}}
											selectedKey={formData.guardrailType}
										>
											{({label, value}) => (
												<Option key={value}>
													{label}
												</Option>
											)}
										</Picker>
									</ClayForm.Group>

									<ClayForm.Group>
										<ClayToggle
											label={Liferay.Language.get(
												'multi-language-detection'
											)}
											onToggle={() =>
												handleToggle(
													'multiLanguageDetectionEnabled'
												)
											}
											toggled={
												formData.multiLanguageDetectionEnabled
											}
										/>
									</ClayForm.Group>
								</ClayPanel.Body>
							</ClayPanel>

							<ClayPanel
								className="model-armor-template-form-filters"
								collapsable={false}
								title={Liferay.Language.get('filters')}
							>
								<ClayPanel.Body>
									<ClayForm.Group>
										<ClayToggle
											label={Liferay.Language.get(
												'malicious-uri-filter'
											)}
											onToggle={() =>
												handleToggle(
													'maliciousUriFilterEnabled'
												)
											}
											toggled={
												formData.maliciousUriFilterEnabled
											}
										/>
									</ClayForm.Group>

									<ClayForm.Group>
										<ClayToggle
											label={Liferay.Language.get(
												'prompt-injection-and-jailbreak-filter'
											)}
											onToggle={() =>
												handleToggle(
													'piAndJailbreakFilterEnabled'
												)
											}
											toggled={
												formData.piAndJailbreakFilterEnabled
											}
										/>

										{formData.piAndJailbreakFilterEnabled ? (
											<Picker
												className="model-armor-template-form-picker mt-2"
												items={confidenceOptions}
												onSelectionChange={(value) => {
													setFormData((prev) => ({
														...prev,
														piAndJailbreakConfidenceLevel:
															value as string,
													}));
												}}
												selectedKey={
													formData.piAndJailbreakConfidenceLevel
												}
											>
												{({label, value}) => (
													<Option key={value}>
														{label}
													</Option>
												)}
											</Picker>
										) : null}
									</ClayForm.Group>

									<ClayForm.Group>
										<ClayToggle
											label={Liferay.Language.get(
												'sensitive-data-protection-filter'
											)}
											onToggle={() =>
												handleToggle('sdpFilterEnabled')
											}
											toggled={formData.sdpFilterEnabled}
										/>
									</ClayForm.Group>
								</ClayPanel.Body>
							</ClayPanel>

							<ClayPanel
								className="model-armor-template-form-rai"
								collapsable={false}
								title={Liferay.Language.get(
									'responsible-ai-filter'
								)}
							>
								<ClayPanel.Body>
									{raiFilters.map(({field, label}) => (
										<div
											className="model-armor-template-form-rai-row"
											key={field}
										>
											<label htmlFor={field}>
												{label}
											</label>

											<Picker
												className="model-armor-template-form-picker"
												items={raiLevelOptions}
												onSelectionChange={(value) => {
													setFormData((prev) => ({
														...prev,
														[field]:
															value as string,
													}));
												}}
												selectedKey={
													(formData[
														field
													] as string) || 'none'
												}
											>
												{({label, value}) => (
													<Option key={value}>
														{label}
													</Option>
												)}
											</Picker>
										</div>
									))}
								</ClayPanel.Body>
							</ClayPanel>
						</ClayLayout.Col>
					</ClayLayout.Row>
				</ClayForm>
			</ClayLayout.ContainerFluid>
		</>
	);
}
