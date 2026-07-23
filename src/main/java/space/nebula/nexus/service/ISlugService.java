package space.nebula.nexus.service;

/**
 * Service for managing URL-friendly slugs.
 */
public interface ISlugService {

	/**
	 * Normalizes a string into a URL-friendly slug.
	 */
	String toSlug(String input);

	/**
	 * Validates and ensures the uniqueness of a slug for a given domain.
	 * 
	 * @param requestedSlug
	 *            The slug provided by the user (optional).
	 * @param fallbackTitle
	 *            The title to use for generation if requestedSlug is blank.
	 * @param existsChecker
	 *            A predicate to check if the slug already exists.
	 * @return A unique, normalized slug.
	 */
	String generateUniqueSlug(String requestedSlug, String fallbackTitle,
			java.util.function.Predicate<String> existsChecker);
}
