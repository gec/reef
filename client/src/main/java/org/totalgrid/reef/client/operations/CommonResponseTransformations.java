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
package org.totalgrid.reef.client.operations;

import org.totalgrid.reef.client.Promise;
import org.totalgrid.reef.client.PromiseTransform;
import org.totalgrid.reef.client.exception.ExpectationException;
import org.totalgrid.reef.client.exception.ReefServiceException;

import java.util.List;

/**
 * When building a new client api we generally want to convert the generic response object we get from the server
 * into the type our user really cares about. These common transformations handle the boiler plate error checking
 * and convertion to either a single object, list or "optional single object".
 */
public class CommonResponseTransformations
{

    private CommonResponseTransformations()
    {
        assert (false);
    }

    /**
     * Checks the Response of a request for a successfull status code, verifies there is only a single result and
     * returns it. If there is an error it throws an approriate ReefServiceException
     * @return promise of a single object
     */
    public static <T> Promise<T> one( Promise<Response<T>> promise )
    {
        return promise.transform( new PromiseTransform<Response<T>, T>() {
            public T transform( Response<T> response ) throws ReefServiceException
            {
                throwIfNotSuccess( response );
                List<T> results = response.getList();
                int listSize = results.size();
                if ( listSize == 0 )
                    throw new ExpectationException( "Expected a response list of size 1, but got an empty list" );
                else if ( listSize > 1 )
                    throw new ExpectationException( "Expected a response list of size 1, but got a list of size: " + listSize );
                else
                    return results.get( 0 );
            }
        } );
    }

    /**
     * Checks the Response of a request for a successfull status code, verifies there is either one or zero results and
     * returns either the value or a null. If there is an error it throws an approriate ReefServiceException. Should
     * only be used for "find" operations.
     * @return promise of a single object (maybe null).
     */
    public static <T> Promise<T> oneOrNone( Promise<Response<T>> promise )
    {
        return promise.transform( new PromiseTransform<Response<T>, T>() {
            public T transform( Response<T> response ) throws ReefServiceException
            {
                throwIfNotSuccess( response );
                List<T> results = response.getList();
                int listSize = results.size();
                if ( listSize > 1 )
                    throw new ExpectationException( "Expected a response list of size 1, but got a list of size: " + listSize );
                else if ( listSize == 0 )
                    return null;
                else
                    return results.get( 0 );
            }
        } );
    }

    /**
     * Checks the response to for a succesfull status code and then returns all results (regardless of length of the list)
     * @return a promise of a list of objects
     */
    public static <T> Promise<List<T>> many( Promise<Response<T>> promise )
    {
        return promise.transform( new PromiseTransform<Response<T>, List<T>>() {
            public List<T> transform( Response<T> response ) throws ReefServiceException
            {
                throwIfNotSuccess( response );
                return response.getList();
            }
        } );
    }

    private static <T> void throwIfNotSuccess( Response<T> response ) throws ReefServiceException
    {
        if ( !response.isSuccess() )
        {
            throw response.getException();
        }
    }
}
