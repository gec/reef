/**
 * Copyright 2011 Green Energy Corp.
 * 
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.client.settings.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Provides mechanism for obtaining a populated Properties object from a text file.
 *
 * This makes it easier to load the provided org.totalgrid.reef.xxx.cfg files without
 * replicating this boiler plate code in multiple places.
 */
public class PropertyReader
{
    private PropertyReader()
    {
        // static class should not be constructed
    }

    /**
     * Read a properties object from a text file.
     * @param fileName Absolute or relative file path
     * @return Properties object
     * @throws IOException if file is unreadable or inaccessible
     */
    public static Properties readFromFile( String fileName ) throws IOException
    {
        List<File> files = new ArrayList<File>();
        files.add( new File( fileName ) );
        return readFromFileObjects( files );
    }

    /**
     * Read a properties object from a text file.
     * @param file File reference
     * @return Properties object
     * @throws IOException if file is unreadable or inaccessible
     */
    public static Properties readFromFile( File file ) throws IOException
    {
        List<File> files = new ArrayList<File>();
        files.add( file );
        return readFromFileObjects( files );
    }

    /**
     * Read out properties from a set of text files. If the same key is in multiple files the
     * _last_ file with the key wins. This allows the use of default files which are then overriden
     * by more specific files.
     * @param fileNames names of files to load (all must exist)
     * @return Properties object
     * @throws IOException if any fileis unreadable or inaccessible
     */
    public static Properties readFromFiles( List<String> fileNames ) throws IOException
    {
        List<File> files = new ArrayList<File>();
        for ( String fName : fileNames )
            files.add( new File( fName ) );
        return readFromFileObjects( files );
    }

    /**
     * Read a properties object from a number of text files
     * @param files File reference
     * @return Properties object
     * @throws IOException if file is unreadable or inaccessible
     */
    private static Properties readFromFileObjects( List<File> files ) throws IOException
    {

        Properties props = new Properties();
        for ( File file : files )
        {
            if ( !file.canRead() )
            {
                throw new IOException( "Cannot find or access file: " + file.getAbsolutePath() );
            }

            FileInputStream fis = new FileInputStream( file );
            try
            {
                props.load( fis );
            }
            finally
            {
                fis.close();
            }
        }
        return props;
    }
}
