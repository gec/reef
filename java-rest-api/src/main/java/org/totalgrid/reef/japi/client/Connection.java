package org.totalgrid.reef.japi.client;

/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import org.totalgrid.reef.japi.ServiceIOException;

/**
 * Thread safe connection handler to connect to the greenbus, handles the starting and stopping
 * of the connection and provides a factory to create service clients.
 */
public interface Connection {

  /**
   * register a listener for open/close events
   *
   * @param listener Interace to call back with open/close events
   */
  void addConnectionListener(ConnectionListener listener);

  /**
   * remove a listener for open/close events
   *
   * @param listener Interace to call back with open/close events
   */
  void removeConnectionListener(ConnectionListener listener);

  /**
   * Starts execution of the messaging connection. Once the service has been started the connection to
   * the broker may be lost so it is important to use an ConnectionListener to be informed of those
   * non-client initiated disconnection events.
   * @param timeoutMs how long to wait (milliseconds) for the first good connection before throwing ServiceIOException.
   * values of 0 or less will throw illegal argument exception
   */
  void connect(long timeoutMs) throws ServiceIOException;

  /**
   * Starts execution of the messaging connection. Once the service has been started the connection to
   * the broker may be lost so it is important to use an ConnectionListener to be informed of those
   * non-client initiated disconnection events.
   */
  void start();

  /**
   * Halts the messaging connection and waits for a closed callback
   * @param timeoutMs how long to wait (milliseconds) for stop before throwing ServiceIOException.
   *    values of 0 or less will throw illegal argument exception
   */
  void disconnect(long timeoutMs) throws ServiceIOException;

  /**
   * Begins halting execution of the messaging connection
   */
  void stop();

  /**
   * creates a non thread-safe (use from single thread only) client
   * TODO: have newSession throw exception if not open
   */
  Session newSession();

  /**
   * get a session pool that manages a group of ISessions and automatically handles monitoring the connection
   * state and threading concerns.
   */
  SessionExecutionPool newSessionPool();
}
