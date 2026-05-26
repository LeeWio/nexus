package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.ConfigRequest;
import space.nebula.nexus.payload.response.ConfigResponse;

import java.util.List;

public interface IConfigService
{
	ApiResponse<List<ConfigResponse>> getAllConfigs();

	ApiResponse<List<ConfigResponse>> getPublicConfigs();

	ApiResponse<ConfigResponse> getConfigByKey(String configKey);

	ApiResponse<ConfigResponse> createConfig(ConfigRequest request);

	ApiResponse<ConfigResponse> updateConfig(Long id, ConfigRequest request);

	ApiResponse<Void> deleteConfig(Long id);
}
