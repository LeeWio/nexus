package space.nebula.nexus.common.storage;

import java.io.InputStream;

/**
 * Interface for different storage strategies (Local, OSS, S3, etc.).
 */
public interface StorageProvider {

	/**
	 * Stores a file and returns its access name or path.
	 */
	String store(InputStream inputStream, String filename);

	/**
	 * Deletes a file.
	 */
	void delete(String filename);

	/**
	 * Checks whether a stored object exists. Implementations must raise an error
	 * when storage is unavailable rather than reporting an unknown object as
	 * missing.
	 */
	boolean exists(String filename);

	/**
	 * Generates a public URL for the file.
	 */
	String getUrl(String filename);

	/**
	 * Get a signed/temporary URL for a file (useful for private buckets). Defaults
	 * to getUrl if not specialized.
	 * 
	 * @param filename
	 *            file identifier
	 * @param expireSeconds
	 *            duration of validity
	 */
	default String getSignedUrl(String filename, long expireSeconds) {
		return getUrl(filename);
	}
}
