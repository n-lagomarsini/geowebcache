package org.geowebcache.io;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

public class ImageDecoderContainer {

    
    @Autowired
    private List<ImageDecoder> decoders;

    private Map<String, ImageDecoder> mapDecoders;

    public ImageDecoderContainer() {

        for (ImageDecoder encoder : decoders) {

            List<String> supportedMimeTypes = encoder.getSupportedMimeTypes();

            for (String mimeType : supportedMimeTypes) {
                if (!mapDecoders.containsKey(mimeType)) {
                    mapDecoders.put(mimeType, encoder);
                }
            }
        }
    }

    public BufferedImage decode(String mimeType, Object input,
            boolean aggressiveInputStreamOptimization, Map<String,Object> map) throws IOException {
        return mapDecoders.get(mimeType).decode(input, aggressiveInputStreamOptimization, map);
    }
    
    public boolean isAggressiveInputStreamSupported(String mimeType){
        return mapDecoders.get(mimeType).isAgressiveInputStreamSupported();
    }
    
}
