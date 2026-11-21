package com.sajha.backend.marketplace.media;

import com.sajha.backend.api.ApiProblem;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class ImageSafetyTest {
    @Test void rejectsRandomContentAndEmpty() {
        assertThrows(ApiProblem.class,()->ImageSafety.clean(new byte[]{1,2,3}));
        assertThrows(ApiProblem.class,()->ImageSafety.clean(new byte[0]));
    }
    @Test void acceptsAndReencodesPng() throws Exception {
        var image=new BufferedImage(40,24,BufferedImage.TYPE_INT_ARGB);
        var bytes=new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image,"png",bytes));
        var cleaned=ImageSafety.clean(bytes.toByteArray());
        assertEquals("image/png",cleaned.mime());
        assertNotNull(ImageIO.read(new java.io.ByteArrayInputStream(cleaned.bytes())));
    }
    @Test void rejectsOversizedInput() {
        assertThrows(ApiProblem.class,()->ImageSafety.clean(new byte[ImageSafety.MAX_INPUT_BYTES+1]));
    }
}
