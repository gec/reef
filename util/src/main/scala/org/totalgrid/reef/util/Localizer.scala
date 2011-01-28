/**
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
package org.totalgrid.reef.util

import java.util.{ ResourceBundle, Locale }
import java.text.MessageFormat

/**
 * System-wide config information (singleton). These can be updated any time
 * and should be auto-updated when the config file (or database) changes.
 *
 * TODO: Tie this to a Configgy notification service or DB update service
 *       so we know when the local has changed.
 */
object SystemConfig {
  var locale = Locale.getDefault()
}

/**
 * Access or update the System-wide Locale singleton.
 */
trait SystemLocale {
  def locale = SystemConfig.locale
  def locale_=(l: Locale) { SystemConfig.locale = l }
}

/**
 * Trait for localizing messages.
 *
 * <h3>Goals</h3>
 *
 * Each subsystem has it's own resource file. This helps with large numbers
 * of resources and allows easy integration of third party resources.
 *
 * The locale defaults to system-wide locale, but allows custom locales.
 *
 * Log messages can have arbitrary set of arguments. It's an array of AnyRef
 * for now.
 *
 * Use Java's default localization support. It's a rich environment for
 * managing locale based resource files, as well as custom localizations and
 * formatting for message arguments.
 *
 * <h3>Use Cases</h3>
 *
 * <b>Format and localize a resource string</b>
 * <pre>
 *   val mr = getMessageResource( event.getSubsystem, event.getEventType)
 *   mr( event.getArgList().toArray);
 * </pre>
 *
 * <b>Format and localize a resource string</b>
 * <pre>
 *    val res     = getBundle( subsystem, loc).getString( messageId)
 *    val message = formatMessage( res, args)
 * </pre>
 *
 * @author flint
 */
trait Localizer extends SystemLocale {

  /**
   *  MessageResource is a function type that takes an Array of AnyRef and
   *  returns the final message.
   */
  type MessageResource = (Array[AnyRef]) => String

  /**
   * Get a ResourceBundle given a subsystem and optional Locale. The
   * ResourceBundle file name is <subsystem>_<Locale>.properties.
   * It will reside in the ./resources directory
   *
   * @param subsystem   The subsystem name that translates to the
   *                    ResourceBundle name. Ex: "FEP" is FEP_en_US.properties
   * @param loc         Optional locale. Specify this for client side rendering.
   */
  def getBundle(subsystem: String, loc: Locale = locale) = ResourceBundle.getBundle(subsystem, loc)

  /**
   * Get a MessageResource fuction that takes set of args to compose a final
   * message.
   *
   * <h3>Use Cases</h3>
   * <b>Format and localize a resource string</b>
   * <pre>
   *   val mr = getMessageResource( event.getSubsystem, event.getEventType)
   *   mr( event.getArgList().toArray);
   * </pre>
   *
   * @param subsystem   The subsystem name that translates to the
   *                    ResourceBundle name. Ex: "FEP" is FEP_en_US.properties
   * @param messageId   The ID of the resource message in the ResourceBundle
   * @param loc         Optional locale. Specify this for client side rendering.
   *
   */
  def getMessageResource(subsystem: String, messageId: String, loc: Locale = locale): MessageResource = {
    val message = getBundle(subsystem, loc).getString(messageId)
    formatMessage(message, loc)_
  }

  /**
   * Format a message.
   *
   * <h3>Use Cases</h3>
   * <b>Format and localize a resource string</b>
   * <pre>
   *    val res     = getBundle( subsystem, loc).getString( messageId)
   *    val message = formatMessage( res, args)
   * </pre>
   *
   * @param resourceString  The parameterized resource message string
   * @param args            The array of arguments to plug into the resource string.
   * @param loc             Optional Locale. Specify this for client side rendering.
   */
  def formatMessage(resourceString: String, args: Array[AnyRef], loc: Locale = locale): String = {
    val mf = new MessageFormat(resourceString, loc)
    mf.format(args)
  }

  /**
   * Format a message. This function is used by getMessageResouce.
   *
   * <h3>Use Cases</h3>
   * <b>Format and localize a resource string</b>
   * <pre>
   *    val res     = getBundle( subsystem, loc).getString( messageId)
   *    val message = formatMessage( res, locale)(args)
   * </pre>
   *
   * @param resourceString  The parameterized resource message string
   * @param loc             Locale specific formatting.
   * @param args            The array of arguments to plug into the resource string.
   *
   * @see #getMessageResource
   */
  def formatMessage(resourceString: String, loc: Locale)(args: Array[AnyRef]): String = {
    formatMessage(resourceString, args, loc)
  }
}

