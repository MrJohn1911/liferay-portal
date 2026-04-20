package com.liferay.ai.hub.cell.configuration.persistence.listener;

import com.liferay.ai.hub.cell.configuration.AIHubCellSecretConfiguration;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.configuration.persistence.listener.ConfigurationModelListener;
import com.liferay.portal.configuration.persistence.listener.ConfigurationModelListenerException;
import com.liferay.portal.kernel.module.configuration.ConfigurationException;
import com.liferay.portal.kernel.security.SecureRandomUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Dictionary;

@Component(
	property = "model.class.name=com.liferay.ai.hub.cell.configuration.AIHubCellSecretConfiguration",
	service = ConfigurationModelListener.class
)

public class AIHubCellSecretConfigurationModelListener implements ConfigurationModelListener {
	@Override
	public void onBeforeSave(
		String pid, Dictionary<String, Object> properties)
		throws ConfigurationModelListenerException {

		properties.put("secret", _generateSecret());

		try {
		_configurationProvider.saveCompanyConfiguration(
			AIHubCellSecretConfiguration.class, GetterUtil.getLong(properties.get("companyId")), properties);
		} catch (ConfigurationException configurationException) {

		}
	}

	/**@Override
	public void onAfterSave(
		String pid, Dictionary<String, Object> properties)
		throws ConfigurationModelListenerException {

		String secret = GetterUtil.getString(
			properties.get("secret"));
		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + secret);

		try {
			AIHubCellSecretConfiguration config =
				_configurationProvider.getCompanyConfiguration(
					AIHubCellSecretConfiguration.class, GetterUtil.getLong(properties.get("companyId")));

			config.secret();
		} catch (ConfigurationException configurationException) {

		}
	}**/

	public byte[] _generateSecret() {
		int sha256BlockSize = 64;

		byte[] secret = new byte[sha256BlockSize];

		for (int i = 0; i < secret.length; i++) {
			secret[i] = SecureRandomUtil.nextByte();
		}

		return secret;
	}

	@Reference
	private ConfigurationProvider _configurationProvider;
}
