package org.geowebcache.io;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

public class ImageEncoderContainer {

    @Autowired
    private List<ImageEncoder> encoders;

    private Map<String, ImageEncoder> mapEncoders;

    public ImageEncoderContainer() {

        for (ImageEncoder encoder : encoders) {

            List<String> supportedMimeTypes = encoder.getSupportedMimeTypes();

            for (String mimeType : supportedMimeTypes) {
                if (!mapEncoders.containsKey(mimeType)) {
                    mapEncoders.put(mimeType, encoder);
                }
            }
        }
    }

    public void encode(RenderedImage image, String mimeType, Object destination,
            boolean aggressiveOutputStreamOptimization, Map<String,Object> map) throws IOException {
        mapEncoders.get(mimeType).encode(image, destination, aggressiveOutputStreamOptimization, map);
    }
       
    public boolean isAggressiveOutputStreamSupported(String mimeType){
        return mapEncoders.get(mimeType).isAgressiveOutputStreamSupported();
    }
}
