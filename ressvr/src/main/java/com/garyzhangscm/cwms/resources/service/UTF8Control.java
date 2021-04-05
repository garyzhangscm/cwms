package com.garyzhangscm.cwms.resources.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class UTF8Control extends ResourceBundle.Control {
    private static final Logger logger
            = LoggerFactory.getLogger(UTF8Control.class);

    public ResourceBundle newBundle
            (String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
            throws IllegalAccessException, InstantiationException, IOException
    {
        // The below is a copy of the default implementation.

        logger.debug("## UTF8Control / baseName : {}", baseName);
        logger.debug("## UTF8Control / locale : {}", locale.getDisplayName());
        String bundleName = toBundleName(baseName, locale);
        logger.debug("## UTF8Control / bundleName : {}", bundleName);
        String resourceName = toResourceName(bundleName, "properties");

        logger.debug("## UTF8Control / resourceName : {}", resourceName);
        ResourceBundle bundle = null;
        InputStream stream = null;
        logger.debug("## UTF8Control / reload : {}", reload);
        if (reload) {
            URL url = loader.getResource(resourceName);
            logger.debug("## UTF8Control / url : {}", url);
            if (url != null) {
                URLConnection connection = url.openConnection();
                if (connection != null) {
                    connection.setUseCaches(false);
                    stream = connection.getInputStream();
                }
            }
        } else {
            stream = loader.getResourceAsStream(resourceName);
        }
        if (stream != null) {
            try {
                // Only this line is changed to make it to read properties files as UTF-8.
                bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
            } finally {
                stream.close();
            }
        }
        return bundle;
    }
}
