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
package org.totalgrid.reef.calc.lib.parse

import util.parsing.combinator.syntactical.StandardTokenParsers
import util.parsing.combinator.JavaTokenParsers

/**
 *
 * expr ::= ident | ident "(" csv ")"
 * csv ::= expr { "," expr }
 *
 */

object OperationParser extends JavaTokenParsers {

  def math: Parser[Any] = term ~ rep("+" ~ term | "-" ~ term)
  def term: Parser[Any] = factor ~ rep("*" ~ factor | "/" ~ factor)
  def factor: Parser[Any] = floatingPointNumber | "(" ~ math ~ ")"
  
  def expr: Parser[Any] = ident ~ "(" ~ csv ~ ")" | ident
  def csv: Parser[Any] = expr ~ rep("," ~ expr)

  def test() {
    val cmd = "SUM ( A, PROD ( B, C ) )"
    val cmd2 = "SUM(A,PROD (B,C))"

    val mathStr = "(5 * 3) + 2"
    val mathStr2 = "5 * 3 + 2 "

    //lexical.delimiters ++ List("(", ")")

    println(parseAll(expr, cmd))
    println(parseAll(expr, cmd2))
    
    println(parseAll(math, mathStr))
    println(parseAll(math, mathStr2))
    
   /* val tokens = new lexical.Scanner(cmd)
    var tokeTemp = tokens
    while(!tokeTemp.atEnd) {
      println(tokeTemp.first)
      tokeTemp = tokeTemp.rest
    }*/

    //val result = phrase(expr)(tokens)

    //val result = expr(tokens)
    
    //println(result)
  }

}
