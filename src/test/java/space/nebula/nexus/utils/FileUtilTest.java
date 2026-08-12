package space.nebula.nexus.utils;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilTest {

	private final FileUtil fileUtil = new FileUtil();

	@Test
	void getImageDimensions_readsPngWithoutDecodingPixels() throws Exception {
		byte[] png = createPng(37, 19);

		FileUtil.ImageDimensions dimensions = fileUtil.getImageDimensions(png);

		assertNotNull(dimensions);
		assertEquals(37, dimensions.width());
		assertEquals(19, dimensions.height());
	}

	@Test
	void generateThumbnail_convertsPngToJpegWithoutWebPCodec() throws Exception {
		byte[] thumbnail = fileUtil.generateThumbnail(createPng(600, 400), 300, 300);

		assertEquals((byte) 0xff, thumbnail[0]);
		assertEquals((byte) 0xd8, thumbnail[1]);
	}

	@Test
	void getImageDimensions_readsWebPHeaderWithoutNativeDecoder() {
		byte[] webp = new byte[]{'R', 'I', 'F', 'F', 22, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', 'X', 10, 0, 0, 0,
				0, 0, 0, 0, 79, 0, 0, 44, 0, 0};

		FileUtil.ImageDimensions dimensions = fileUtil.getImageDimensions(webp);

		assertNotNull(dimensions);
		assertEquals(80, dimensions.width());
		assertEquals(45, dimensions.height());
	}

	private byte[] createPng(int width, int height) throws Exception {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		assertTrue(ImageIO.write(image, "png", output));
		return output.toByteArray();
	}
}
