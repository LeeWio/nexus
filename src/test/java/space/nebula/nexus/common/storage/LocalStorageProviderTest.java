package space.nebula.nexus.common.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import space.nebula.nexus.config.StorageProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalStorageProviderTest {

	@TempDir
	Path storageDirectory;

	@Test
	void exists_DistinguishesStoredAndMissingObjects() throws IOException {
		StorageProperties properties = new StorageProperties();
		properties.getLocal().setLocation(storageDirectory.toString());
		LocalStorageProvider provider = new LocalStorageProvider(properties);
		Files.writeString(storageDirectory.resolve("present.txt"), "present");

		assertTrue(provider.exists("present.txt"));
		assertFalse(provider.exists("missing.txt"));
	}
}
