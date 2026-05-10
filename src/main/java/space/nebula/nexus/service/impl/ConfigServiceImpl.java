package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Config;
import space.nebula.nexus.mapper.ConfigMapper;
import space.nebula.nexus.payload.request.ConfigRequest;
import space.nebula.nexus.payload.response.ConfigResponse;
import space.nebula.nexus.repository.ConfigRepository;
import space.nebula.nexus.service.IConfigService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements IConfigService {

    private final ConfigRepository configRepository;
    private final ConfigMapper configMapper;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<ConfigResponse>> getAllConfigs() {
        return ApiResponse.success(configMapper.toResponseList(configRepository.findAll()));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "sys_config", key = "'public_configs'")
    public ApiResponse<List<ConfigResponse>> getPublicConfigs() {
        return ApiResponse.success(configMapper.toResponseList(configRepository.findByIsPublicTrue()));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "sys_config", key = "#configKey")
    public ApiResponse<ConfigResponse> getConfigByKey(String configKey) {
        return configRepository.findByConfigKey(configKey)
                .map(config -> ApiResponse.success(configMapper.toResponse(config)))
                .orElseThrow(() -> new BusinessException(404, "Config not found with key: " + configKey));
    }

    @Override
    @Transactional
    @CacheEvict(value = "sys_config", allEntries = true)
    @LogOperation("Create Config")
    public ApiResponse<ConfigResponse> createConfig(ConfigRequest request) {
        if (configRepository.existsByConfigKey(request.configKey())) {
            throw new BusinessException("Config key already exists");
        }

        Config config = configMapper.toEntity(request);
        configRepository.save(config);
        log.info("System configuration created: {}", config.getConfigKey());

        return ApiResponse.success("Config created successfully", configMapper.toResponse(config));
    }

    @Override
    @Transactional
    @CacheEvict(value = "sys_config", allEntries = true)
    @LogOperation("Update Config")
    public ApiResponse<ConfigResponse> updateConfig(Long id, ConfigRequest request) {
        Config config = configRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Config not found"));

        if (!config.getConfigKey().equals(request.configKey()) && configRepository.existsByConfigKey(request.configKey())) {
            throw new BusinessException("Config key already exists");
        }

        config.setConfigKey(request.configKey());
        config.setConfigValue(request.configValue());
        config.setConfigName(request.configName());
        config.setDescription(request.description());
        config.setIsPublic(request.isPublic());

        configRepository.save(config);
        log.info("System configuration updated: {}", config.getConfigKey());

        return ApiResponse.success("Config updated successfully", configMapper.toResponse(config));
    }

    @Override
    @Transactional
    @CacheEvict(value = "sys_config", allEntries = true)
    @LogOperation("Delete Config")
    public ApiResponse<Void> deleteConfig(Long id) {
        if (!configRepository.existsById(id)) {
            throw new BusinessException(404, "Config not found");
        }
        configRepository.deleteById(id);
        log.info("System configuration deleted with id: {}", id);
        return ApiResponse.success("Config deleted successfully", null);
    }
}
