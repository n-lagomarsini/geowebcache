package org.geowebcache.io;

import it.geosolutions.imageio.stream.output.ImageOutputStreamAdapter;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.apache.log4j.Logger;
import org.geotools.image.ImageWorker.PNGImageWriteParam;
import org.geotools.image.io.ImageIOExt;
import org.geotools.resources.i18n.ErrorKeys;
import org.geotools.resources.i18n.Errors;

import com.sun.imageio.plugins.png.PNGImageWriter;
import com.sun.media.imageioimpl.plugins.clib.CLibImageWriter;

public class ImageEncoderImpl implements ImageEncoder {

    private static final Logger LOGGER = Logger.getLogger(ImageEncoderImpl.class);

    private static final IIORegistry theRegistry = IIORegistry.getDefaultInstance();

    public static final String OPERATION_NOT_SUPPORTED = "Operation not supported";

    private final boolean isAggressiveOutputStreamSupported;

    private final List<String> supportedMimeTypes;

    private ImageWriterSpi spi;

    private Map<String, String> inputParams;

    public enum WriteParams {
        PNG("png", "png; mode=24bit"), JPEG("jpeg"), GIF("gif"), TIFF("tiff"), BMP("bmp");

        private String[] formatNames;

        WriteParams(String... formatNames) {
            this.formatNames = formatNames;
        }

        private boolean isFormatNameAccepted(String formatName) {
            boolean accepted = false;
            for (String format : formatNames) {
                accepted = format.equalsIgnoreCase(formatName);
                if (accepted) {
                    break;
                }
            }
            return accepted;
        }

        public static WriteParams getWriteParamForName(String formatName) {

            if (PNG.isFormatNameAccepted(formatName)) {
                return PNG;
            } else if (JPEG.isFormatNameAccepted(formatName)) {
                return JPEG;
            } else if (GIF.isFormatNameAccepted(formatName)) {
                return GIF;
            } else if (TIFF.isFormatNameAccepted(formatName)) {
                return TIFF;
            } else if (BMP.isFormatNameAccepted(formatName)) {
                return BMP;
            }
            return null;
        }
    }

    /**
     * Encodes the selected image with the defined output object. The user can set the aggressive outputStream if supported.
     * 
     * @param image Image to write.
     * @param destination Destination object where the image is written.
     * @param aggressiveOutputStreamOptimization Parameter used if aggressive outputStream optimization must be used.
     * @throws IOException
     */
    public void encode(RenderedImage image, Object destination,
            boolean aggressiveOutputStreamOptimization, Map<String, ?> map) {

        if (!isAgressiveOutputStreamSupported() && aggressiveOutputStreamOptimization) {
            throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
        }

        // Selection of the first priority writerSpi
        ImageWriterSpi newSpi = getWriterSpi();

        if (newSpi != null) {
            // Creation of the associated Writer
            ImageWriter writer = null;
            ImageOutputStream stream = null;
            try {
                writer = newSpi.createWriterInstance();
                // Check if the input object is an OutputStream
                if (destination instanceof OutputStream) {
                    // Use of the ImageOutputStreamAdapter
                    if (isAgressiveOutputStreamSupported()) {
                        stream = new ImageOutputStreamAdapter((OutputStream) destination);
                    } else {
                        stream = new MemoryCacheImageOutputStream((OutputStream) destination);
                    }

                    // Preparation of the ImageWriteParams
                    ImageWriteParam params = prepareParams(writer);

                    // Image writing
                    writer.setOutput(stream);
                    writer.write(null, new IIOImage(image, null, null), params);
                } else {
                    throw new IllegalArgumentException("Wrong output object");
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                // Writer disposal
                if (writer != null) {
                    writer.dispose();
                }
                // Stream closure
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                    stream = null;
                }
            }
        }
    }

    private ImageWriteParam prepareParams(ImageWriter writer) throws IOException {

        String mimeType0 = supportedMimeTypes.get(0);

        int beginIndex = 6;
        WriteParams paramEnum = WriteParams.getWriteParamForName(mimeType0.substring(beginIndex));

        ImageWriteParam params = null;
        // Selection of the compression type
        String compression = inputParams.get("COMPRESSION");
        // Boolean indicating if compression is present
        boolean compressUsed = compression != null && !compression.isEmpty()
                && !compression.equalsIgnoreCase("null");
        // Selection of the compression rate
        String compressionRateValue = inputParams.get("COMPRESSION_RATE");
        // Initial value for the compression rate
        float compressionRate = -1;
        // Evaluation of the compression rate
        if (compressionRateValue != null) {
            try {
                compressionRate = Float.parseFloat(compressionRateValue);
            } catch (NumberFormatException e) {
                //Do nothing and skip compression rate
            }
        }

        switch (paramEnum) {
        case PNG:
            if (writer instanceof CLibImageWriter) {
                params = writer.getDefaultWriteParam();
                // Define compression mode
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                if (compressUsed) {
                    // best compression
                    params.setCompressionType(compression);
                }
                if (compressionRate > -1) {
                    // we can control quality here
                    params.setCompressionQuality(compressionRate);
                }
            } else if (writer instanceof PNGImageWriter) {
                params = new PNGImageWriteParam();
                // Define compression mode
                params.setCompressionMode(ImageWriteParam.MODE_DEFAULT);
            }
            break;
        case JPEG:
            params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            if (compressUsed) {
                // Lossy compression.
                params.setCompressionType(compression);
            }
            if (compressionRate > -1) {
                // we can control quality here
                params.setCompressionQuality(compressionRate);
            }
            // If JPEGWriteParams, additional parameters are set
            if (params instanceof JPEGImageWriteParam) {
                final JPEGImageWriteParam jpegParams = (JPEGImageWriteParam) params;
                jpegParams.setOptimizeHuffmanTables(true);
                try {
                    jpegParams.setProgressiveMode(JPEGImageWriteParam.MODE_DEFAULT);
                } catch (UnsupportedOperationException e) {
                    throw new IOException(e);
                }

                params = jpegParams;
            }
            break;
        case GIF:
        case TIFF:
        case BMP:
            params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            if (compressUsed) {
                // best compression
                params.setCompressionType(compression);
            }
            if (compressionRate > -1) {
                // we can control quality here
                params.setCompressionQuality(compressionRate);
            }
            break;
        default:
            break;
        }

        return params;
    }

    /**
     * Returns the ImageSpiWriter associated to
     * 
     * @return
     */
    private ImageWriterSpi getWriterSpi() {
        return spi;
    }

    /**
     * Returns all the supported MimeTypes
     * 
     * @return supportedMimeTypes List of all the supported Mime Types
     */
    public List<String> getSupportedMimeTypes() {
        return supportedMimeTypes;
    }

    /**
     * Indicates if optimization on OutputStream can be used
     * 
     * @return isAggressiveOutputStreamSupported Boolean indicating if the selected encoder supports an aggressive output stream optimization
     */
    public boolean isAgressiveOutputStreamSupported() {
        return isAggressiveOutputStreamSupported;
    }

    /**
     * Creates a new Instance of ImageEncoder supporting or not OutputStream optimization, with the defined MimeTypes and Spi classes.
     * 
     * @param aggressiveOutputStreamOptimization
     * @param supportedMimeTypes
     * @param writerSpi
     */
    public ImageEncoderImpl(boolean aggressiveOutputStreamOptimization,
            List<String> supportedMimeTypes, List<String> writerSpi, Map<String, String> inputParams) {
        this.isAggressiveOutputStreamSupported = aggressiveOutputStreamOptimization;
        this.supportedMimeTypes = new ArrayList<String>(supportedMimeTypes);
        this.inputParams = inputParams;
        // Registration of the plugins
        theRegistry.registerApplicationClasspathSpis();
        // Checks for each Spi class if it is present and then it is added to the list.
        for (String spi : writerSpi) {
            try {

                Class<?> clazz = Class.forName(spi);
                ImageWriterSpi writer = (ImageWriterSpi) theRegistry
                        .getServiceProviderByClass(clazz);
                if (writer != null) {
                    this.spi = writer;
                    break;
                }
            } catch (ClassNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
            }

        }
    }
}
