package org.geowebcache.io;

import java.awt.image.RenderedImage;
import java.util.List;
import java.util.Map;

public interface ImageEncoder {

    public void encode(RenderedImage image, Object destination,
            boolean aggressiveOutputStreamOptimization, Map<String,?> option);
    
    public List<String> getSupportedMimeTypes();
    
    public boolean isAgressiveOutputStreamSupported();
    
}
