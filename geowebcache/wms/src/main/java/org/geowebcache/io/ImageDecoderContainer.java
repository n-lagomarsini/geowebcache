package org.geowebcache.io;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ImageDecoderContainer implements ApplicationContextAware {

    private Collection<ImageDecoder> decoders;

    private Map<String, ImageDecoder> mapDecoders;

    public ImageDecoderContainer() {
    }

    public BufferedImage decode(String mimeType, Object input,
            boolean aggressiveInputStreamOptimization, Map<String, Object> map) throws IOException {
        if (mapDecoders == null) {
            throw new IllegalArgumentException("ApplicationContext must be set before decoding");
        }
        return mapDecoders.get(mimeType).decode(input, aggressiveInputStreamOptimization, map);
    }

    public boolean isAggressiveInputStreamSupported(String mimeType) {
        if (mapDecoders == null) {
            throw new IllegalArgumentException(
                    "ApplicationContext must be set before checking the AggressiveInputStrean support");
        }
        return mapDecoders.get(mimeType).isAgressiveInputStreamSupported();
    }

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        decoders = context.getBeansOfType(ImageDecoder.class).values();
        if (decoders == null || decoders.isEmpty()) {
            throw new IllegalArgumentException("No Encoder found");
        }

        mapDecoders = new HashMap<String, ImageDecoder>();

        for (ImageDecoder encoder : decoders) {

            List<String> supportedMimeTypes = encoder.getSupportedMimeTypes();

            for (String mimeType : supportedMimeTypes) {
                if (!mapDecoders.containsKey(mimeType)) {
                    mapDecoders.put(mimeType, encoder);
                }
            }
        }
    }
}
