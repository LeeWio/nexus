package space.nebula.nexus.service;

public interface ISeoService
{

	/**
	 * Generates a standard XML Sitemap for search engine crawlers. Includes posts,
	 * categories, tags, and series.
	 */
	String generateSitemapXml();

	/**
	 * Generates an RSS 2.0 Feed for content distribution. Contains the most recent
	 * published posts.
	 */
	String generateRssFeedXml();

	/**
	 * Generates a standard robots.txt file for search engine crawlers.
	 */
	String generateRobotsTxt();
}
