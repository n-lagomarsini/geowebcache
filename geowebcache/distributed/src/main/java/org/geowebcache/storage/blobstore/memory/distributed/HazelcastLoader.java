package org.geowebcache.storage.blobstore.memory.distributed;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastLoader implements InitializingBean {
    /** {@link Logger} object used for logging exceptions */
    private final static Log LOGGER = LogFactory.getLog(HazelcastLoader.class);

    public final static String HAZELCAST_CONFIG_DIR = "hazelcast.config.dir";

    public final static String HAZELCAST_NAME = "hazelcast.xml";

    private HazelcastInstance instance;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (instance == null) {
            // Search for the Hazelcast configuration directory
            String hazelDirPath = System.getProperty(HAZELCAST_CONFIG_DIR);
            if (hazelDirPath != null) {
                File hazelCastDir = new File(hazelDirPath);
                if (hazelCastDir.exists() && hazelCastDir.isDirectory() && hazelCastDir.canRead()) {
                    FileFilter filter = new NameFileFilter(HAZELCAST_NAME);
                    File[] files = hazelCastDir.listFiles(filter);
                    if (files != null && files.length > 0) {
                        File hazelCastConf = files[0];
                        InputStream stream = new FileInputStream(hazelCastConf);
                        Config config = null;
                        try {
                            config = new XmlConfigBuilder(stream).build();
                        } finally {
                            if (stream != null) {
                                try {
                                    stream.close();
                                } catch (Exception e) {
                                    stream = null;
                                    if (LOGGER.isErrorEnabled()) {
                                        LOGGER.error(e.getMessage(), e);
                                    }
                                }
                            }
                        }
                        ConfigValidator validator = new ConfigValidator(config);
                        if (validator.isAccepted()) {
                            instance = Hazelcast.newHazelcastInstance(config);
                        } else {
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info("No mapping for CacheProvider Map is present");
                            }
                        }
                    }
                }
            }
        } else if (!instance.getConfig().getMapConfigs()
                .containsKey(HazelcastCacheProvider.HAZELCAST_MAP_DEFINITION)) {
            instance = null;
        }
    }

    public boolean isConfigured() {
        return instance != null;
    }

    public void setInstance(HazelcastInstance instance) {
        this.instance = instance;
    }

    public HazelcastInstance getInstance() {
        return isConfigured() ? instance : null;
    }

    static class ConfigValidator {

        private final boolean accepted;

        public ConfigValidator(Config config) {
            boolean configAccepted = false;
            if (config != null) {
                // Check if the cache map is present
                if (config.getMapConfigs().containsKey(
                        HazelcastCacheProvider.HAZELCAST_MAP_DEFINITION)) {
                    MapConfig mapConfig = config
                            .getMapConfig(HazelcastCacheProvider.HAZELCAST_MAP_DEFINITION);
                    // Check size policy
                    boolean sizeDefined = mapConfig.getMaxSizeConfig().getSize() > 0;
                    boolean policyExists = mapConfig.getEvictionPolicy() != MapConfig.DEFAULT_EVICTION_POLICY;
                    boolean sizeFromHeap = mapConfig.getMaxSizeConfig().getMaxSizePolicy() == MaxSizeConfig.POLICY_USED_HEAP_SIZE;
                    if (sizeDefined && policyExists && sizeFromHeap) {
                        configAccepted = true;
                    }
                }
            }

            accepted = configAccepted;
        }

        public boolean isAccepted() {
            return accepted;
        }

    }
}
