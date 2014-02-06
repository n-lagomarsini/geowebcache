package org.geowebcache.inputoutput;

import it.geosolutions.imageio.plugins.png.PNGJWriter;
import it.geosolutions.imageio.stream.output.ImageOutputStreamAdapter;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.apache.log4j.Logger;
import org.geotools.image.ImageWorker;
import org.geowebcache.mime.ImageMime;
import org.springframework.beans.factory.annotation.Value;

import ar.com.hjg.pngj.FilterType;

public class PNGImageEncoder extends ImageEncoder {
    
    private static final String FILTER_TYPE = "filterType";

    private static final Logger LOGGER = Logger.getLogger(PNGImageEncoder.class);

    private static List<String> supportedMimeTypes;

    @Value("#{propSource[disablePNG]}")
    private boolean disablePNG;

    private final boolean isAggressiveSupported;
    
    private final static float DEFAULT_QUALITY=1;
    
    private final float quality;
    
    static{
        supportedMimeTypes = new ArrayList<String>();
        supportedMimeTypes.add(ImageMime.png.getMimeType());
        supportedMimeTypes.add(ImageMime.png8.getMimeType());
        supportedMimeTypes.add(ImageMime.png24.getMimeType());
        supportedMimeTypes.add(ImageMime.png_24.getMimeType());
    }
    

    public PNGImageEncoder(boolean aggressiveOutputStreamOptimization,Float quality, List<String> writerSpi) {
        super(aggressiveOutputStreamOptimization, supportedMimeTypes, writerSpi);
        
        if(quality!=null){
            this.quality=quality;
        }else{
            this.quality=DEFAULT_QUALITY;
        }
        
        // Setting of the Aggressive OutputStream only if the first ImageWriterSpi object is an instance of the Default PNGImageWriterSpi
        this.isAggressiveSupported = (!this.disablePNG);
    }           
    
    public boolean isAgressiveOutputStreamSupported() {
        // If Default PNG Writer must not be used, then Aggressive OutputStream is not supported.
        return super.isAgressiveOutputStreamSupported() && isAggressiveSupported;
    }
    
    
    public void encode(RenderedImage image, Object destination,
            boolean aggressiveOutputStreamOptimization, Map<String,Object> map) {

        if (!isAgressiveOutputStreamSupported() && aggressiveOutputStreamOptimization) {
            throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
        }
        
        // If the new PNGWriter must be disabled then the other writers are used
        if(disablePNG){
           super.encode(image, destination, aggressiveOutputStreamOptimization,map); 
        }else{
            // Creation of the associated Writer
            PNGJWriter writer = new PNGJWriter();
            OutputStream stream = null;
            try {
                //writer = new PNGJWriter();
                // Check if the input object is an OutputStream
                if (destination instanceof OutputStream) {
                    boolean isScanlinePresent = writer.isScanlineSupported(image);
                    if(!isScanlinePresent){
                        image = new ImageWorker(image).rescaleToBytes().forceComponentColorModel().getRenderedImage();
                    }        
                    
                    Object filterObj = map.get(FILTER_TYPE);
                    FilterType filter = null;
                    if(filterObj==null || !(filterObj instanceof FilterType)){
                        filter = FilterType.FILTER_NONE;
                    }else{
                        filter = (FilterType)filterObj;
                    }
                    stream = (OutputStream) destination;
                    // Image writing
                    writer.writePNG(image, stream, quality, filter);
                }else{
                    throw new IllegalArgumentException("Only an OutputStream can be provided to the PNGEncoder");
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                // Writer disposal
                if (writer != null) {
                    writer=null;
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
}
