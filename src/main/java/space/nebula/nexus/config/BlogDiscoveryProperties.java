package space.nebula.nexus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.blog.discovery")
public class BlogDiscoveryProperties
{
	private int sectionSize = 6;
	private int categoryGroupSize = 3;
	private int categoryPostSize = 4;
	private int candidateSize = 48;
	private int freshnessWindowDays = 90;
	private double featuredWeight = 1_000;
	private double viewWeight = 18;
	private double likeWeight = 4;
	private double favoriteWeight = 6;
	private double freshnessWeight = 0.8;
	private double summaryWeight = 8;
	private double coverWeight = 6;
	private double categoryWeight = 4;
}
