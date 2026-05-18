package space.nebula.nexus.config;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import space.nebula.nexus.common.aspect.AnalyticsInterceptor;
import space.nebula.nexus.common.aspect.LogInterceptor;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.location:uploads}")
    private String uploadLocation;

    @Resource
    private LogInterceptor logInterceptor;

    @Resource
    private AnalyticsInterceptor analyticsInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/v1/public/files/**", "/static/**", "/swagger-ui/**", "/v3/api-docs/**");

        registry.addInterceptor(analyticsInterceptor)
                .addPathPatterns("/api/v1/public/**")
                .excludePathPatterns("/api/v1/public/files/**", "/api/v1/public/seo/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = Paths.get(uploadLocation).toAbsolutePath().toUri().toString();
        
        registry.addResourceHandler("/api/v1/public/files/**")
                .addResourceLocations(path);
    }
}
