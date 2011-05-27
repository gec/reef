/**
 * Copyright 2011 Green Energy Corp.
 * 
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.gnu.org/licenses/agpl.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.integration.helpers;

import java.util.*;

public class BlockingQueue<T>
{

    private final LinkedList<T> queue = new LinkedList<T>();

    public void clear()
    {
        queue.clear();
    }

    public void push( T o )
    {
        synchronized ( queue )
        {
            queue.add( o );
            queue.notify();
        }
    }

    public T pop( long timeout ) throws InterruptedException
    {
        synchronized ( queue )
        {
            if ( queue.size() == 0 )
                queue.wait( timeout );
            return queue.removeFirst();
        }
    }

    public int size()
    {
        return queue.size();
    }
}
