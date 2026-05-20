package space.nebula.nexus.service;

/**
 * Service for filtering sensitive words in user-generated content.
 */
public interface SensitiveWordService {

	/**
	 * Checks if the text contains sensitive words.
	 */
	boolean containsSensitiveWord(String text);

	/**
	 * Replaces sensitive words in the text with a mask (e.g., ***).
	 */
	String filter(String text);
}
