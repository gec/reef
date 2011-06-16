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
package org.totalgrid.reef.services.core.util

import org.totalgrid.reef.proto.Utils

/**
 *
 * Render the resource string using an AttributeList.
 *
 * This class is like MessageFormat.java, but uses custom resource strings that
 * have named attributes to fill in. The standard Java classes uses ordered
 * attributes.
 *
 * <h3>Goals</h3>
 * The resource strings should use the same syntax as the standard Java resource libraries,
 * with the exception that properties are named.
 *
 * <h3>Use Cases</h3>
 * <b> Render a message given a resource string and an AttributeList proto.
 *
 * <pre>
 * val (severity, designation, alarmState, resource) = eventConfig.getProperties(req.getEventType)
 * theString = MessageFormatter.format( resource, req.getArgs)
 * </pre>
 *
 * @author flint
 */

object MessageFormatter {

  /**
   * Render the resource string using the AttributeList.
   */
  def format(resource: String, alist: AttributeList): String =
    parseResource(resource).map(_.apply(alist)).mkString("")

  /**
   * Return a list of ResourceSegment
   */
  protected def parseResource(resource: String): List[ResourceSegment] = {
    var segments: List[ResourceSegment] = List()
    var index = 0;
    var leftBrace = indexOfWithEscape(resource, '{', index)
    while (leftBrace >= 0) {
      val rightBrace = indexOfWithEscape(resource, '}', leftBrace + 1)
      if (rightBrace < 0)
        throw new IllegalArgumentException("Braces do not match in resource string")

      // Is there a string before the brace?
      if (leftBrace > index)
        segments ::= ResourceSegmentString(resource.substring(index, leftBrace))

      // For now we have a simple name inside the brace
      segments ::= ResourceSegmentNamedValue(resource.substring(leftBrace + 1, rightBrace).trim, resource.substring(leftBrace, rightBrace + 1))

      index = rightBrace + 1
      leftBrace = indexOfWithEscape(resource, '{', index)
    }

    // Either there were never any braces or there's a string
    // after the last brace
    if (index < resource.length)
      segments ::= ResourceSegmentString(resource.substring(index))

    segments.reverse
  }

  /**
   * Return the index that the delimiter occurs or -1.
   * Escape sequence for resource strings is '{'name'}' or '{name}'
   *
   */
  def indexOfWithEscape(s: String, delimiter: Int, index: Int) = {
    s.indexOf(delimiter, index)
    /* TODO: The escape sequence for resource strings uses single quotes and it's complicated!
    var foundAt = s.indexOf(delimiter, index)
    var i = index;

    while (foundAt >= 0 /* &&bla bla bla*/) {
      i = foundAt + 1
      foundAt = s.indexOf(delimiter, i)
    }

    foundAt
    */
  }
}

/**
 * A resource string is chopped up into segments. A "segment" is either
 * a named variable or a string of characters between named variables.
 */
abstract class ResourceSegment {
  /**
   * Apply the values from the AttributeList to the named
   * properties in the resource segment.
   */
  def apply(alist: AttributeList): String
}

/**
 * Just a fixed string segment of a resource string.
 */
case class ResourceSegmentString(s: String) extends ResourceSegment {
  def apply(alist: AttributeList) = s
}

/**
 * A named value part of a resource string.
 */
case class ResourceSegmentNamedValue(name: String, original: String) extends ResourceSegment {
  // TODO: we'll be adding date and number types.
  // TODO: This breaks if the named attribute is not in the AttributeList. Need to return empty string in those cases.

  def apply(alist: AttributeList) = alist.get(name).map { _.getString }.getOrElse(original)
}

