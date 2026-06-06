package space.nebula.nexus.service;

import space.nebula.nexus.entity.Post;

public interface IStaticGenerationService
{

	/**
	 * Generates a static HTML file for the given post and uploads it to storage.
	 */
	void generatePostStaticHtml(Post post);

	/**
	 * Deletes the static HTML file for the given post.
	 */
	void deletePostStaticHtml(String slug);

	/**
	 * Regenerates static HTML files for all published posts.
	 */
	void regenerateAllPosts();
}
