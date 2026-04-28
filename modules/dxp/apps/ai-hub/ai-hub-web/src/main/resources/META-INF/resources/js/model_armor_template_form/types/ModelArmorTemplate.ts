/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export type ModelArmorTemplate = {
	active: boolean;
	description: string;
	externalReferenceCode: string;
	guardrailType: string;
	maliciousUriFilterEnabled: boolean;
	multiLanguageDetectionEnabled: boolean;
	name: string;
	piAndJailbreakConfidenceLevel: string;
	piAndJailbreakFilterEnabled: boolean;
	r_accountToAIHubModelArmorTemplates_accountEntryERC: string;
	raiDangerousLevel: string;
	raiHarassmentLevel: string;
	raiHateSpeechLevel: string;
	raiSexuallyExplicitLevel: string;
	sdpFilterEnabled: boolean;
};
