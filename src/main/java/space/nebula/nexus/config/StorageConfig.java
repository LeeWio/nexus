package space.nebula.nexus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import space.nebula.nexus.common.storage.AliyunStorageProvider;
import space.nebula.nexus.common.storage.LocalStorageProvider;
import space.nebula.nexus.common.storage.StorageProvider;

@Configuration
public class StorageConfig {

	@Bean
	@Primary
	public StorageProvider storageProvider(StorageProperties properties) {
		String type = properties.getType();
		if ("local".equalsIgnoreCase(type)) {
			return new LocalStorageProvider(properties);
		}

		if ("aliyun".equalsIgnoreCase(type)) {
			return new AliyunStorageProvider(properties.getAliyun());
		}

		return new LocalStorageProvider(properties);
	}
}
