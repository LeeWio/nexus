package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.event.ConfigChangedEvent;
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
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.SYS_CONFIG, key = "'all'", sync = true)
	public ApiResponse<List<ConfigResponse>> getAllConfigs() {
		return ApiResponse.success(configMapper.toResponseList(configRepository.findAll()));
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.SYS_CONFIG, key = CacheConstants.PUBLIC_CONFIGS_KEY, sync = true)
	public ApiResponse<List<ConfigResponse>> getPublicConfigs() {
		return ApiResponse.success(configMapper.toResponseList(configRepository.findByIsPublicTrue()));
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.SYS_CONFIG, key = "#configKey", sync = true)
	public ApiResponse<ConfigResponse> getConfigByKey(String configKey) {
		return configRepository.findByConfigKey(configKey)
				.map(config -> ApiResponse.success(configMapper.toResponse(config))).orElseThrow(
						() -> new BusinessException(BusinessCode.NOT_FOUND, "Config not found with key: " + configKey));
	}

	@Override
	@Transactional
	@LogOperation("Create Config")
	@CacheEvict(value = {CacheConstants.SYS_CONFIG, CacheConstants.SEO}, allEntries = true)
	public ApiResponse<ConfigResponse> createConfig(ConfigRequest request) {
		Assert.isFalse(configRepository.existsByConfigKey(request.configKey()),
				() -> new BusinessException(BusinessCode.DUPLICATE_KEY, "Config key already exists"));

		Config config = configMapper.toEntity(request);
		configRepository.save(config);
		log.info("System configuration created: {}", config.getConfigKey());

		eventPublisher.publishEvent(new ConfigChangedEvent(this, config.getConfigKey(), false));

		return ApiResponse.success("Config created successfully", configMapper.toResponse(config));
	}

	@Override
	@Transactional
	@LogOperation("Update Config")
	@CacheEvict(value = {CacheConstants.SYS_CONFIG, CacheConstants.SEO}, allEntries = true)
	public ApiResponse<ConfigResponse> updateConfig(Long id, ConfigRequest request) {
		Config config = configRepository.findById(id)
				.orElseThrow(() -> new BusinessException(BusinessCode.NOT_FOUND, "Config not found"));

		String oldKey = config.getConfigKey();

		if (!StrUtil.equals(oldKey, request.configKey())) {
			Assert.isFalse(configRepository.existsByConfigKey(request.configKey()),
					() -> new BusinessException(BusinessCode.DUPLICATE_KEY, "Config key already exists"));
		}

		config.setConfigKey(request.configKey());
		config.setConfigValue(request.configValue());
		config.setConfigName(request.configName());
		config.setDescription(request.description());
		config.setIsPublic(request.isPublic());

		configRepository.save(config);
		log.info("System configuration updated: {}", config.getConfigKey());

		eventPublisher.publishEvent(new ConfigChangedEvent(this, request.configKey(), false));
		if (!StrUtil.equals(oldKey, request.configKey())) {
			eventPublisher.publishEvent(new ConfigChangedEvent(this, oldKey, true));
		}

		return ApiResponse.success("Config updated successfully", configMapper.toResponse(config));
	}

	@Override
	@Transactional
	@LogOperation("Delete Config")
	@CacheEvict(value = {CacheConstants.SYS_CONFIG, CacheConstants.SEO}, allEntries = true)
	public ApiResponse<Void> deleteConfig(Long id) {
		Config config = configRepository.findById(id)
				.orElseThrow(() -> new BusinessException(BusinessCode.NOT_FOUND, "Config not found"));

		String configKey = config.getConfigKey();
		configRepository.delete(config);
		log.info("System configuration deleted: {}", configKey);

		eventPublisher.publishEvent(new ConfigChangedEvent(this, configKey, true));

		return ApiResponse.success("Config deleted successfully", null);
	}
}
