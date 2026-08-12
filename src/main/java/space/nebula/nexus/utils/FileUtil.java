package space.nebula.nexus.utils;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

@Slf4j
@Component
public class FileUtil {

	private final Tika tika = new Tika();

	/**
	 * Detects the real MIME type of a file based on its content (Magic Number).
	 */
	public String detectMimeType(InputStream inputStream) throws IOException {
		return tika.detect(inputStream);
	}

	/**
	 * Gets dimensions of an image.
	 */
	public ImageDimensions getImageDimensions(byte[] imageBytes) {
		if (isWebP(imageBytes)) {
			return getWebPDimensions(imageBytes);
		}

		try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
			if (input == null) {
				return null;
			}
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (readers.hasNext()) {
				ImageReader reader = readers.next();
				try {
					reader.setInput(input, true, true);
					return new ImageDimensions(reader.getWidth(0), reader.getHeight(0));
				} finally {
					reader.dispose();
				}
			}
		} catch (IOException | RuntimeException e) {
			log.warn("Could not read image dimensions", e);
		}
		return null;
	}

	public record ImageDimensions(int width, int height) {
	}

	/**
	 * Checks if a MIME type represents a processable image.
	 */
	public boolean isImage(String mimeType) {
		return mimeType != null && mimeType.startsWith("image/");
	}

	/**
	 * WebP processing used a native decoder that can terminate the JVM. Keep the
	 * source file, but do not try to decode it for a thumbnail.
	 */
	public boolean supportsThumbnailGeneration(String mimeType) {
		return "image/jpeg".equals(mimeType) || "image/png".equals(mimeType) || "image/gif".equals(mimeType);
	}

	/**
	 * Generates a thumbnail for an image.
	 */
	public byte[] generateThumbnail(byte[] imageBytes, int width, int height) throws IOException {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Thumbnails.of(bais).size(width, height).outputFormat("jpg").outputQuality(0.8).toOutputStream(outputStream);
			return outputStream.toByteArray();
		}
	}

	/**
	 * Compresses an image while maintaining reasonable quality.
	 */
	public byte[] compressImage(byte[] imageBytes) throws IOException {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Thumbnails.of(bais).scale(1.0).outputQuality(0.7).toOutputStream(outputStream);
			return outputStream.toByteArray();
		}
	}

	private boolean isWebP(byte[] imageBytes) {
		return imageBytes.length >= 12 && readAscii(imageBytes, 0, "RIFF") && readAscii(imageBytes, 8, "WEBP");
	}

	private ImageDimensions getWebPDimensions(byte[] imageBytes) {
		for (int offset = 12; offset + 8 <= imageBytes.length;) {
			long chunkSize = readLittleEndianInt(imageBytes, offset + 4);
			long dataOffset = offset + 8L;
			long nextOffset = dataOffset + chunkSize + (chunkSize & 1);
			if (nextOffset > imageBytes.length) {
				return null;
			}

			ImageDimensions dimensions = switch (readChunkType(imageBytes, offset)) {
				case "VP8X" -> readVp8xDimensions(imageBytes, (int) dataOffset, (int) chunkSize);
				case "VP8 " -> readVp8Dimensions(imageBytes, (int) dataOffset, (int) chunkSize);
				case "VP8L" -> readVp8lDimensions(imageBytes, (int) dataOffset, (int) chunkSize);
				default -> null;
			};
			if (dimensions != null) {
				return dimensions;
			}
			offset = (int) nextOffset;
		}
		return null;
	}

	private ImageDimensions readVp8xDimensions(byte[] imageBytes, int offset, int chunkSize) {
		if (chunkSize < 10) {
			return null;
		}
		return new ImageDimensions(readLittleEndian24(imageBytes, offset + 4) + 1,
				readLittleEndian24(imageBytes, offset + 7) + 1);
	}

	private ImageDimensions readVp8Dimensions(byte[] imageBytes, int offset, int chunkSize) {
		if (chunkSize < 10 || (imageBytes[offset + 3] & 0xff) != 0x9d || (imageBytes[offset + 4] & 0xff) != 0x01
				|| (imageBytes[offset + 5] & 0xff) != 0x2a) {
			return null;
		}
		return new ImageDimensions(readLittleEndianShort(imageBytes, offset + 6) & 0x3fff,
				readLittleEndianShort(imageBytes, offset + 8) & 0x3fff);
	}

	private ImageDimensions readVp8lDimensions(byte[] imageBytes, int offset, int chunkSize) {
		if (chunkSize < 5 || (imageBytes[offset] & 0xff) != 0x2f) {
			return null;
		}
		int first = imageBytes[offset + 1] & 0xff;
		int second = imageBytes[offset + 2] & 0xff;
		int third = imageBytes[offset + 3] & 0xff;
		int fourth = imageBytes[offset + 4] & 0xff;
		return new ImageDimensions(1 + first + ((second & 0x3f) << 8),
				1 + (second >>> 6) + (third << 2) + ((fourth & 0x0f) << 10));
	}

	private boolean readAscii(byte[] bytes, int offset, String expected) {
		for (int index = 0; index < expected.length(); index++) {
			if (bytes[offset + index] != (byte) expected.charAt(index)) {
				return false;
			}
		}
		return true;
	}

	private String readChunkType(byte[] bytes, int offset) {
		return new String(bytes, offset, 4, java.nio.charset.StandardCharsets.US_ASCII);
	}

	private int readLittleEndian24(byte[] bytes, int offset) {
		return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8) | ((bytes[offset + 2] & 0xff) << 16);
	}

	private int readLittleEndianShort(byte[] bytes, int offset) {
		return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
	}

	private long readLittleEndianInt(byte[] bytes, int offset) {
		return (bytes[offset] & 0xffL) | ((bytes[offset + 1] & 0xffL) << 8) | ((bytes[offset + 2] & 0xffL) << 16)
				| ((bytes[offset + 3] & 0xffL) << 24);
	}
}
