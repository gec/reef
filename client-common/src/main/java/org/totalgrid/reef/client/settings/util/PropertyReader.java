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
import java.util.Properties;

/**
 * Provides mechanisms for obtaining a Properties object
 */
public class PropertyReader
{

    /**
     * Read a properties objectg from a file
     * @param fileName Absolute or relative file path
     * @return Properties object
     * @throws IOException
     */
    public static Properties readFromFile( String fileName ) throws IOException
    {
        File file = new File( fileName );

        if ( !file.canRead() )
        {
            throw new IOException( "Cannot find or access file: " + file.getAbsolutePath() );
        }

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
