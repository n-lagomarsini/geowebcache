package org.geowebcache.io;

import it.geosolutions.imageio.stream.input.FileImageInputStreamExtImpl;
import it.geosolutions.imageio.stream.input.ImageInputStreamAdapter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.apache.log4j.Logger;

public class ImageDecoderImpl implements ImageDecoder{
    
    private static final Logger LOGGER = Logger.getLogger(ImageEncoderImpl.class);

    private static final IIORegistry theRegistry = IIORegistry.getDefaultInstance();

    public static final String OPERATION_NOT_SUPPORTED = "Operation not supported";

    private final boolean isAggressiveInputStreamSupported;

    private final List<String> supportedMimeTypes;

    private ImageReaderSpi spi;

    /**
     * Creates a new Instance of ImageEncoder supporting or not OutputStream optimization, with the defined MimeTypes and Spi classes.
     * 
     * @param aggressiveOutputStreamOptimization
     * @param supportedMimeTypes
     * @param writerSpi
     */
    public ImageDecoderImpl(boolean aggressiveInputStreamOptimization,
            List<String> supportedMimeTypes, List<String> readerSpi) {

        this.isAggressiveInputStreamSupported = aggressiveInputStreamOptimization;
        this.supportedMimeTypes = new ArrayList<String>(supportedMimeTypes);
        // Registration of the plugins
        theRegistry.registerApplicationClasspathSpis();
        // Checks for each Spi class if it is present and then it is added to the list.
        for (String spi : readerSpi) {
            try {
                Class<?> clazz = Class.forName(spi);
                ImageReaderSpi reader = (ImageReaderSpi) theRegistry
                        .getServiceProviderByClass(clazz);
                if (reader != null) {
                    this.spi=reader;
                    break;
                }
            } catch (ClassNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
            }

        }
    }
    
    
    /**
     * Encodes the selected image with the defined output object. The user can set the aggressive outputStream if supported.
     * 
     * @param image Image to write.
     * @param source Destination object where the image is written.
     * @param aggressiveOutputStreamOptimization Parameter used if aggressive outputStream optimization must be used.
     * @throws IOException
     */
    public BufferedImage decode(Object source,
            boolean aggressiveInputStreamOptimization, Map<String,Object> map) {

        if (!isAgressiveInputStreamSupported() && aggressiveInputStreamOptimization) {
            throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
        }

        // Selection of the first priority writerSpi
        ImageReaderSpi newSpi = getReaderSpi();
        
        if(newSpi!=null){
            // Creation of the associated Writer
            ImageReader reader = null;
            ImageInputStream stream = null;
            try {
                reader = newSpi.createReaderInstance();
                if (source instanceof FileResource) {
                    // file
                    stream= new FileImageInputStreamExtImpl(((FileResource)source).getFile());
                    // Image reading
                    reader.setInput(stream);
                    return reader.read(0);                    
                } else {
                    // create a stream and move on
                    source=((Resource)source).getInputStream();
                }
                
                // Check if the input object is an InputStream
                if (source instanceof InputStream) {
                    // Use of the ImageInputStreamAdapter
                    if (isAgressiveInputStreamSupported()) {
                        stream = new ImageInputStreamAdapter((InputStream) source);
                    } else {
                        stream = new MemoryCacheImageInputStream((InputStream) source);
                    }

                    // Image reading
                    reader.setInput(stream);
                    return reader.read(0);
                }else{
                    throw new IllegalArgumentException("Wrong input object");
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                // reader disposal
                if (reader != null) {
                    reader.dispose();
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
        
        return null;
    }

    /**
     * Returns the ImageSpiReader associated to 
     * @return
     */
    private ImageReaderSpi getReaderSpi() {
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
     * Indicates if optimization on InputStream can be used
     * 
     * @return isAggressiveInputStreamSupported Boolean indicating if the selected decoder supports an aggressive input stream optimization
     */
    public boolean isAgressiveInputStreamSupported() {
        return isAggressiveInputStreamSupported;
    }
    
    
    
}
