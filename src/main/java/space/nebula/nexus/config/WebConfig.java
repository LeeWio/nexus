package space.nebula.nexus.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import space.nebula.nexus.common.aspect.AnalyticsInterceptor;
import space.nebula.nexus.common.aspect.TraceInterceptor;

import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final StorageProperties storageProperties;

	private final AnalyticsInterceptor analyticsInterceptor;

	private final TraceInterceptor traceInterceptor;

	private final HandlerInterceptor commentReadCacheControlInterceptor = new HandlerInterceptor() {
		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
			if ("GET".equalsIgnoreCase(request.getMethod()) || "HEAD".equalsIgnoreCase(request.getMethod())) {
				response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
				response.setHeader("Pragma", "no-cache");
				response.setDateHeader("Expires", 0);
				response.addHeader("Vary", "Authorization");
			}
			return true;
		}
	};

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(traceInterceptor).addPathPatterns("/**").order(-1); // Highest priority

		registry.addInterceptor(analyticsInterceptor).addPathPatterns("/api/v1/public/**")
				.excludePathPatterns("/api/v1/public/files/**", "/api/v1/public/seo/**");

		registry.addInterceptor(commentReadCacheControlInterceptor).addPathPatterns("/api/v1/public/comments/**",
				"/api/v1/public/guestbook/**");
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String location = storageProperties.getLocal().getLocation();
		String baseUrl = storageProperties.getLocal().getBaseUrl();

		String path = Paths.get(location).toAbsolutePath().toUri().toString();

		registry.addResourceHandler(baseUrl + "**").addResourceLocations(path);

		// Explicitly register Swagger UI and Webjars resources to ensure they are
		// served correctly
		registry.addResourceHandler("/swagger-ui/**").addResourceLocations(
				"classpath:/META-INF/resources/webjars/swagger-ui/", "classpath:/META-INF/resources/");
		registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
	}
}
