package space.nebula.nexus.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import space.nebula.nexus.common.aspect.AnalyticsInterceptor;
import space.nebula.nexus.common.aspect.TraceInterceptor;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private StorageProperties storageProperties;

    @Resource
    private AnalyticsInterceptor analyticsInterceptor;

    @Resource
    private TraceInterceptor traceInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceInterceptor)
                .addPathPatterns("/**")
                .order(-1); // Highest priority

        registry.addInterceptor(analyticsInterceptor)
                .addPathPatterns("/api/v1/public/**")
                .excludePathPatterns("/api/v1/public/files/**", "/api/v1/public/seo/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = storageProperties.getLocal().getLocation();
        String baseUrl = storageProperties.getLocal().getBaseUrl();
        
        String path = Paths.get(location).toAbsolutePath().toUri().toString();
        
        registry.addResourceHandler(baseUrl + "**")
                .addResourceLocations(path);
    }
}
