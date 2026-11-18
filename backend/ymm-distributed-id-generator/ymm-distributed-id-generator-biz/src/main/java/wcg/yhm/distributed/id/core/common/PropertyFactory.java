package wcg.yhm.distributed.id.core.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;
import java.util.Locale;

public class PropertyFactory {
    private static final Logger logger = LoggerFactory.getLogger(PropertyFactory.class);
    private static final Properties prop = new Properties();
    static {
        try {
            prop.load(PropertyFactory.class.getClassLoader().getResourceAsStream("leaf.properties"));
        } catch (IOException e) {
            logger.warn("Load Properties Ex", e);
        }
        // leaf.properties is loaded directly, so Spring placeholders do not apply.
        // Allow deployment secrets to be supplied without editing tracked files.
        for (String key : prop.stringPropertyNames()) {
            String envName = key.toUpperCase(Locale.ROOT).replace('.', '_');
            String value = System.getenv(envName);
            if (value != null) {
                prop.setProperty(key, value);
            }
        }
    }
    public static Properties getProperties() {
        return prop;
    }
}
