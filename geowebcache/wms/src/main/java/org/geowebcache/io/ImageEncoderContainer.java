package org.geowebcache.io;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ImageEncoderContainer implements ApplicationContextAware {

    private Collection<ImageEncoder> encoders;

    private Map<String, ImageEncoder> mapEncoders;

    public ImageEncoderContainer() {
    }

    public void encode(RenderedImage image, String mimeType, Object destination,
            boolean aggressiveOutputStreamOptimization, Map<String, Object> map) throws IOException {
        if (mapEncoders == null) {
            throw new IllegalArgumentException("ApplicationContext must be set before encoding");
        }
        mapEncoders.get(mimeType).encode(image, destination, aggressiveOutputStreamOptimization,
                map);
    }

    public boolean isAggressiveOutputStreamSupported(String mimeType) {
        if (mapEncoders == null) {
            throw new IllegalArgumentException(
                    "ApplicationContext must be set before checking the AggressiveOutputStrean support");
        }
        return mapEncoders.get(mimeType).isAgressiveOutputStreamSupported();
    }

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        encoders = context.getBeansOfType(ImageEncoder.class).values();
        if (encoders == null || encoders.isEmpty()) {
            throw new IllegalArgumentException("No Encoder found");
        }

        mapEncoders = new HashMap<String, ImageEncoder>();

        for (ImageEncoder encoder : encoders) {

            List<String> supportedMimeTypes = encoder.getSupportedMimeTypes();

            for (String mimeType : supportedMimeTypes) {
                if (!mapEncoders.containsKey(mimeType)) {
                    mapEncoders.put(mimeType, encoder);
                }
            }
        }
    }

}
