package space.nebula.nexus.utils;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(bais);
            if (image != null) {
                return new ImageDimensions(image.getWidth(), image.getHeight());
            }
        } catch (IOException e) {
            log.warn("Could not read image dimensions", e);
        }
        return null;
    }

    public record ImageDimensions(int width, int height) {}

    /**
     * Checks if a MIME type represents a processable image.
     */
    public boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * Generates a thumbnail for an image.
     */
    public byte[] generateThumbnail(byte[] imageBytes, int width, int height) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Thumbnails.of(bais)
                    .size(width, height)
                    .outputFormat("jpg")
                    .outputQuality(0.8)
                    .toOutputStream(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Compresses an image while maintaining reasonable quality.
     */
    public byte[] compressImage(byte[] imageBytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Thumbnails.of(bais)
                    .scale(1.0)
                    .outputQuality(0.7)
                    .toOutputStream(outputStream);
            return outputStream.toByteArray();
        }
    }
}
