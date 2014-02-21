package org.geowebcache.io;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

public interface ImageDecoder {

    public List<String> getSupportedMimeTypes();

    public BufferedImage decode(Object input, boolean aggressiveInputStreamOptimization,
            Map<String, Object> map);

    public boolean isAgressiveInputStreamSupported();
    
    

}
