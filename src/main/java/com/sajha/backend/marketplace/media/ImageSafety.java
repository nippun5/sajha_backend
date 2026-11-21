package com.sajha.backend.marketplace.media;

import com.sajha.backend.api.ApiProblem;
import org.springframework.http.HttpStatus;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/** Validate actual bytes, cap pixels, re-encode and strip EXIF/metadata. */
public final class ImageSafety {
    public static final int MAX_INPUT_BYTES = 4 * 1024 * 1024;
    public static final int MAX_EDGE = 5000;
    public static final long MAX_PIXELS = 12_000_000L;
    private ImageSafety() {}
    public record Clean(String mime, String extension, byte[] bytes) {}

    public static Clean clean(byte[] input) {
        if (input == null || input.length == 0 || input.length > MAX_INPUT_BYTES)
            throw bad("Choose a nonempty image under 4 MB");
        String type;
        if (input.length >= 3 && (input[0] & 255) == 0xFF && (input[1] & 255) == 0xD8
                && (input[2] & 255) == 0xFF) type="jpeg";
        else if (input.length >= 8 && (input[0] & 255) == 137 && (input[1] & 255) == 80
                && (input[2] & 255) == 78 && (input[3] & 255) == 71
                && (input[4] & 255) == 13 && (input[5] & 255) == 10
                && (input[6] & 255) == 26 && (input[7] & 255) == 10) type="png";
        else throw bad("Only JPEG and PNG images are supported for local uploads");

        // Prevent deceptively small compressed files from allocating enormous pixel buffers.
        try (ImageInputStream source = ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
            if (source == null) throw bad("Unable to decode image");
            Iterator<ImageReader> available = ImageIO.getImageReaders(source);
            if (!available.hasNext()) throw bad("Image format is not supported");
            ImageReader reader = available.next();
            try {
                reader.setInput(source, true, true); // ignore metadata
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if (width<1 || height<1 || width>MAX_EDGE || height>MAX_EDGE
                      || (long)width*height>MAX_PIXELS) throw bad("Image dimensions are too large");
                BufferedImage decoded = reader.read(0);
                if (decoded==null) throw bad("Unable to decode image");
                BufferedImage cleaned = new BufferedImage(width,height,
                    type.equals("jpeg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
                // Avoid Graphics2D/X11 dependencies on headless Linux servers.
                int[] line = new int[width];
                for (int y=0; y<height; y++) {
                    decoded.getRGB(0,y,width,1,line,0,width);
                    cleaned.setRGB(0,y,width,1,line,0,width);
                }
                decoded.flush();
                try (ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                    if (!ImageIO.write(cleaned, type, out)) throw bad("Image encoding is unsupported");
                    byte[] bytes=out.toByteArray();
                    if(bytes.length==0 || bytes.length>MAX_INPUT_BYTES) throw bad("Image is too large after processing");
                    return new Clean(type.equals("jpeg")?"image/jpeg":"image/png",
                        type.equals("jpeg")?"jpg":"png",bytes);
                } finally {cleaned.flush();}
            } finally {reader.dispose();}
        } catch (ApiProblem e) {throw e;}
        catch (IOException | RuntimeException e) {throw bad("Invalid or damaged image");}
    }
    private static ApiProblem bad(String reason) {
        return new ApiProblem(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_IMAGE",reason);
    }
}
