/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.integration.helpers;

import org.totalgrid.reef.messaging.ProtoServiceTypes.*;
import org.totalgrid.reef.messaging.javabridge.*;

public class MockEventAcceptor<T> implements EventAcceptor<T> {

	private BlockingQueue<Event<T>> queue = new BlockingQueue<Event<T>>();

	public void onEvent(Event<T> event) {
		queue.push(event);
	}

	public void clear() {
		queue.clear();
	}

	public Event<T> pop(long timeoutms) throws InterruptedException {
		return queue.pop(timeoutms);
	}

	public boolean waitFor(T value, long timeoutms) {
		long start = System.currentTimeMillis();
		do {
			try {
				T ret = queue.pop(timeoutms).getResult();
				if (ret.equals(value)) return true;
			} catch (Exception ex) {
				System.out.println(ex);
				return false;
			}
		} while (start + timeoutms > System.currentTimeMillis());

		return false;
	}
}
