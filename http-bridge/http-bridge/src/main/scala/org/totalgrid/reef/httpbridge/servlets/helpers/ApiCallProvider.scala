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
package org.totalgrid.reef.httpbridge.servlets.helpers

import com.google.protobuf.Message
import org.totalgrid.reef.client.sapi.client.Promise
import org.totalgrid.reef.client.Client
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.client.SubscriptionResult

/**
 * ApiCall and its implementations are used to encapsulate an action we can take with a Client
 * that will produce a Promise() with data in it. We only require that the result of the promise
 * be a protobuf to make sure it is serializable.
 */
sealed trait ApiCall
case class SingleResultApiCall[A <: Message](executeFunction: (Client) => Promise[A]) extends ApiCall
case class OptionalResultApiCall[A <: Message](executeFunction: (Client) => Promise[Option[A]]) extends ApiCall
case class MultiResultApiCall[A <: Message](executeFunction: (Client) => Promise[List[A]]) extends ApiCall
case class SubscriptionResultApiCall[A <: Message](executeFunction: (Client) => Promise[SubscriptionResult[List[A], A]]) extends ApiCall

case class PreparableApiCall[A <: Message](resultClass: Class[A], prepareFunction: (ArgumentSource) => ApiCall)

/**
 * a container for looking up and preparing an ApiCall. It will parse the arguments
 * and be 100% ready to be sent to the server if this method returns without throwing an exception
 */
trait ApiCallProvider {
  def prepareApiCall(function: String, args: ArgumentSource): ApiCall
}

/**
 * Each function name may have more than one overload implementation. We calculate
 * a score for each overload (from most specific to least specific) and sort them
 * so we try the most specific overloads first when choosing which one to call.
 *
 * This works well, the only case it doesn't handle well is if there are two overloads
 * with different names that are both being requested:
 *   get(name : String)
 *   get(score : Int)
 *
 * If the user has set both arguments "?name=SearchName&score=100", we will choose the more
 * specific overload (in this case the integer overload).
 *
 * TODO: add "no extra settings" function and state tracking to the ArgumentSource interface
 */
trait ApiCallLookup extends ApiCallProvider {
  /**
   * map of function names -> functions with their "overload score"
   */
  private var apiCalls = Map.empty[String, List[(Long, PreparableApiCall[_])]]

  /**
   * called by the ApiCallLibraries to populate the list of available apis
   */
  protected def addCall[A <: Message](name: String, c: PreparableApiCall[A]) {

    val overloadScore = ArgumentUtilites.calculateOverloadScore(c)

    apiCalls.get(name) match {
      case None => apiCalls += name -> List((overloadScore, c))
      case Some(existingMethods) =>
        apiCalls += name -> ((overloadScore, c) :: existingMethods).sortWith(_._1 > _._1)
    }
  }

  def prepareApiCall(function: String, args: ArgumentSource): ApiCall = {
    apiCalls.get(function) match {
      case None => throw new BadRequestException("Unknown function: " + function)
      case Some(possibleFunctions) =>
        possibleFunctions match {
          case List((_, oneOption)) => handleSingleFunction(function, args, oneOption)
          case _ => handleOverloadedFunction(function, args, possibleFunctions.map { _._2 })
        }
    }
  }

  private def handleSingleFunction(function: String, args: ArgumentSource, oneOption: PreparableApiCall[_]) = {
    try {
      oneOption.prepareFunction(args)
    } catch {
      case bre: BadRequestException =>
        throw new BadRequestException("Error parsing " + function + " : " +
          bre.getMessage + " valid arguments are: " + ArgumentUtilites.argumentsAsString(oneOption))
    }
  }

  private def handleOverloadedFunction(function: String, args: ArgumentSource, possibleFunctions: List[PreparableApiCall[_]]) = {
    possibleFunctions.find(overloadHasMatchingArgs(_, args)) match {
      case Some(validOverload) => validOverload.prepareFunction(args)
      case None =>
        val validArguments = possibleFunctions.map { ArgumentUtilites.argumentsAsString(_) }.mkString(", ")
        throw new BadRequestException("None of the overloads of " + function + " matched your arguments. Valid argument sets are: " + validArguments)
    }
  }

  private def overloadHasMatchingArgs(preparer: PreparableApiCall[_], args: ArgumentSource) = {
    try {
      preparer.prepareFunction(args)
      true
    } catch {
      case bre: BadRequestException =>
        false
    }
  }
}

/**
 * base trait for classes that are defining api functions. It provides helpers to make defining
 * a map of the available calls simple enough to enable generating the bindings. The call is done
 * in two steps, first arguments are collected, then the specific api call is executed. This makes
 * generation a bit easier and enables us to use a fake ArgumentSource to capture the necessary
 * arguments. We also require that the binding define the return class, this is not needed by the
 * type system, but it is useful to make the functions fully self describing.
 */
trait ApiCallLibrary[ServiceClass] extends ApiCallLookup {

  /**
   * each api library can define which service interfaces they want (AllScadaService, LoaderService, etc)
   */
  def serviceClass: Class[ServiceClass]

  /**
   * define a binding for a function that returns 1 resultClass object or throws exception
   */
  protected def single[A <: Message](name: String, resultClass: Class[A], prepareFunction: (ArgumentSource) => ((ServiceClass) => Promise[A])) = {
    val preparer = (args: ArgumentSource) => singleCall(prepareFunction(args))
    addCall(name, PreparableApiCall(resultClass, preparer))
  }
  /**
   * define a binding for a function that returns 0 or 1 resultClass object or throws exception
   */
  protected def optional[A <: Message](name: String, resultClass: Class[A], prepareFunction: (ArgumentSource) => ((ServiceClass) => Promise[Option[A]])) = {
    val preparer = (args: ArgumentSource) => optionalCall(prepareFunction(args))
    addCall(name, PreparableApiCall(resultClass, preparer))
  }
  /**
   * define a binding for a function that a list of resultClass object or throws exception
   */
  protected def multi[A <: Message](name: String, resultClass: Class[A], prepareFunction: (ArgumentSource) => ((ServiceClass) => Promise[List[A]])) = {
    val preparer = (args: ArgumentSource) => multiCall(prepareFunction(args))
    addCall(name, PreparableApiCall(resultClass, preparer))
  }

  protected def subscription[A <: Message](name: String, resultClass: Class[A], prepareFunction: (ArgumentSource) => ((ServiceClass) => Promise[SubscriptionResult[List[A], A]])) = {
    val preparer = (args: ArgumentSource) => subscriptionCall(prepareFunction(args))
    addCall(name, PreparableApiCall(resultClass, preparer))
  }

  /**
   * these helper functions allow us to keep the ApiCall interface "ServicesList" agnostic
   * and we convert to specific serviceClass
   */
  private def singleCall[A <: Message](executeFunction: (ServiceClass) => Promise[A]) = {
    SingleResultApiCall(c => executeFunction(c.getService(serviceClass)))
  }
  private def optionalCall[A <: Message](executeFunction: (ServiceClass) => Promise[Option[A]]) = {
    OptionalResultApiCall(c => executeFunction(c.getService(serviceClass)))
  }
  private def multiCall[A <: Message](executeFunction: (ServiceClass) => Promise[List[A]]) = {
    MultiResultApiCall(c => executeFunction(c.getService(serviceClass)))
  }
  private def subscriptionCall[A <: Message](executeFunction: (ServiceClass) => Promise[SubscriptionResult[List[A], A]]) = {
    SubscriptionResultApiCall(c => executeFunction(c.getService(serviceClass)))
  }
}