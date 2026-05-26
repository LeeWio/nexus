package space.nebula.nexus.config;

import cn.hutool.core.util.StrUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import space.nebula.nexus.common.storage.AliyunStorageProvider;
import space.nebula.nexus.common.storage.LocalStorageProvider;
import space.nebula.nexus.common.storage.StorageProvider;

import space.nebula.nexus.common.storage.S3StorageProvider;

@Configuration
public class StorageConfig {

	@Bean
	@Primary
	public StorageProvider storageProvider(StorageProperties properties) {
		String type = properties.getType();
		if (StrUtil.equalsIgnoreCase("local", type)) {
			return new LocalStorageProvider(properties);
		}

		if (StrUtil.equalsIgnoreCase("aliyun", type)) {
			return new AliyunStorageProvider(properties.getAliyun());
		}

		if (StrUtil.equalsIgnoreCase("s3", type)) {
			return new S3StorageProvider(properties.getS3());
		}

		return new LocalStorageProvider(properties);
	}
}
