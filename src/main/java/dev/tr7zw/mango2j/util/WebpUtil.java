package dev.tr7zw.mango2j.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import com.luciad.imageio.webp.CompressionType;
import com.luciad.imageio.webp.WebPReadParam;
import com.luciad.imageio.webp.WebPWriteParam;

public class WebpUtil {

    private final static ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();

    public static BufferedImage readWebp(File file) throws FileNotFoundException, IOException {
        // Obtain a WebP ImageReader instance
        ImageReader reader = ImageIO.getImageReadersByMIMEType("image/webp").next();

        // Configure decoding parameters
        WebPReadParam readParam = new WebPReadParam();
        readParam.setBypassFiltering(true);

        // Configure the input on the ImageReader
        reader.setInput(new FileImageInputStream(file));

        // Decode the image
        BufferedImage image = reader.read(0, readParam);
        return image;
    }

    public static void writeAsWebp(ImageOutputStream out, BufferedImage img) throws IOException {
        try (out) {
            WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
            float q = 0.8f;
            if (q == 1) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionType(CompressionType.Lossless);
            } else {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionType(CompressionType.Lossy);
                writeParam.setCompressionQuality(q);
                writeParam.setNearLossless(9);
            }

            writer.setOutput(out);
            writer.write(null, new IIOImage(img, null, null), writeParam);
            writer.dispose();
        }
    }

    public static File minimizeFile(File file, File outDir) throws FileNotFoundException, IOException {
        File out = new File(outDir, file.getName().replace(".png", ".webp").replace(".jpg", ".webp")
                .replace(".jpeg", ".webp").replace(".gif", ".webp"));
        try (ImageOutputStream str = new FileImageOutputStream(out)) {
            writeAsWebp(str, ImageIO.read(file));
        } catch (Exception ex) {
            out.delete();
            throw ex;
        }
        return out;
    }

}
