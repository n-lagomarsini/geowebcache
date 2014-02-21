package org.geowebcache.io;

import it.geosolutions.imageio.stream.output.ImageOutputStreamAdapter;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.apache.log4j.Logger;

public class ImageEncoderImpl implements ImageEncoder{

    private static final Logger LOGGER = Logger.getLogger(ImageEncoderImpl.class);

    private static final IIORegistry theRegistry = IIORegistry.getDefaultInstance();

    public static final String OPERATION_NOT_SUPPORTED = "Operation not supported";

    private final boolean isAggressiveOutputStreamSupported;

    private final List<String> supportedMimeTypes;

    private ImageWriterSpi spi;

    /**
     * Encodes the selected image with the defined output object. The user can set the aggressive outputStream if supported.
     * 
     * @param image Image to write.
     * @param destination Destination object where the image is written.
     * @param aggressiveOutputStreamOptimization Parameter used if aggressive outputStream optimization must be used.
     * @throws IOException
     */
    public void encode(RenderedImage image, Object destination,
            boolean aggressiveOutputStreamOptimization, Map<String,?> map) {

        if (!isAgressiveOutputStreamSupported() && aggressiveOutputStreamOptimization) {
            throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
        }

        // Selection of the first priority writerSpi
        ImageWriterSpi newSpi = getWriterSpi();
        
        if(newSpi!=null){
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

                    // Image writing
                    writer.setOutput(stream);
                    writer.write(image);
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

    /**
     * Returns the ImageSpiWriter associated to 
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
            List<String> supportedMimeTypes, List<String> writerSpi) {
        this.isAggressiveOutputStreamSupported = aggressiveOutputStreamOptimization;
        this.supportedMimeTypes = new ArrayList<String>(supportedMimeTypes);
        // Registration of the plugins
        theRegistry.registerApplicationClasspathSpis();
        // Checks for each Spi class if it is present and then it is added to the list.
        for (String spi : writerSpi) {
            try {
             
                Class<?> clazz = Class.forName(spi);
                ImageWriterSpi writer = (ImageWriterSpi) theRegistry
                        .getServiceProviderByClass(clazz);
                if (writer != null) {
                    this.spi=writer;
                    break;
                }
            } catch (ClassNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
            }

        }
    }
}
