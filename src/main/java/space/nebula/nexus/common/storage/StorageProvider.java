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
	 * Generates a public URL for the file.
	 */
	String getUrl(String filename);
}
