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
package org.totalgrid.reef.client.settings;


import java.io.IOException;
import java.util.Dictionary;

import org.totalgrid.reef.client.settings.util.PropertyLoading;
import org.totalgrid.reef.client.settings.util.PropertyReader;

/**
 * Encapsulates the fields necessary to login to reef.
 *
 * Notice that the name and password will probably be different than the credentials used to
 * connect to the broker.
 */
public class UserSettings
{
    private String userName;
    private String userPassword;

    public UserSettings( String userName, String userPassword )
    {
        this.userName = userName;
        this.userPassword = userPassword;
    }

    public UserSettings( Dictionary properties )
    {
        userName = PropertyLoading.getString( "org.totalgrid.reef.user.username", properties );
        userPassword = PropertyLoading.getString( "org.totalgrid.reef.user.password", properties );
    }

    public UserSettings( String file ) throws IllegalArgumentException, IOException
    {
        this( PropertyReader.readFromFile( file ) );
    }

    public String getUserName()
    {
        return userName;
    }

    public String getUserPassword()
    {
        return userPassword;
    }
}
