package org.totalgrid.reef.api.japi.settings.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Provides mechanisms for obtaining a Properties object
 */
public class PropertyReader
{

    /**
     * Read a properties objectg from a file
     * @param file Absolute or relative file path
     * @return Properties object
     * @throws IOException
     */
    public static Properties readFromFile( String file ) throws IOException
    {
        FileInputStream fis = new FileInputStream( file );
        Properties props = new Properties();
        try
        {
            props.load( fis );
        }
        finally
        {
            fis.close();
        }
        return props;
    }
}
