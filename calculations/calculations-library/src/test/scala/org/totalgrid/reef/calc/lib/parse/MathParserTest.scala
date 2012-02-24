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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import util.parsing.combinator.JavaTokenParsers

object DumbMathParser extends JavaTokenParsers {
  def math: Parser[Any] = term ~ rep("+" ~ term | "-" ~ term)
  def term: Parser[Any] = factor ~ rep("*" ~ factor | "/" ~ factor)
  def factor: Parser[Any] = floatingPointNumber | "(" ~ math ~ ")"

  def parse(str: String) = parseAll(math, str)
}

object MathParser extends JavaTokenParsers {

  sealed trait Node
  case class Op(op: String, left: Node, right: Node) extends Node {
    override def toString() = "(" + left + " " + op + " " + right + ")"
  }
  case class Value(v: Double) extends Node {
    override def toString() = v.toString
  }
  case class Part(op: String, right: Node)

  def math: Parser[Node] = term ~ rep("+" ~ term ^^ part | "-" ~ term ^^ part) ^^ opList
  def term: Parser[Node] = factor ~ rep("*" ~ factor ^^ part | "/" ~ factor ^^ part) ^^ opList
  def factor: Parser[Node] = floatingPointNumber ^^ { x => Value(x.toDouble) } | "(" ~> math <~ ")"

  def part: Any => Part = {
    case (op: String) ~ (r: Node) => Part(op, r)
  }

  def opList: ~[Node, List[Part]] => Node = {
    case n ~ Nil => n
    case l ~ List(Part(op, r)) => Op(op, l, r)
    case l ~ list /*(list: List[Part])*/ => list.foldLeft(l) { case (l, part) => Op(part.op, l, part.right) }
    case x =>
      throw new Exception("transform failure: " + x)
  }

  def parse(str: String) = parseAll(math, str)
}

object FunParser extends JavaTokenParsers {
  sealed trait Node
  case class Fun(name: String, inputs: List[Node]) extends Node
  case class Var(name: String) extends Node

  def expr: Parser[Node] = fun | ident ^^ { Var(_) }
  def fun: Parser[Node] = ident ~ ("(" ~> csv <~ ")") ^^ {
    case f ~ list => Fun(f, list)
  }
  def csv: Parser[List[Node]] = expr ~ rep("," ~> expr) ^^ {
    case l ~ list => l :: list
  }

  def parse(str: String) = parseAll(expr, str)
}

@RunWith(classOf[JUnitRunner])
class MathParserTest extends FunSuite with ShouldMatchers {

  test("FunParser") {
    val simple = "AVG(A, B)"
    println(FunParser.parse(simple))
  }

  test("ParseMath") {

    val mathStr = "(5 + 3) * 2"
    val mathStr2 = "5 + 3 * 2"
    val mathStr3 = "5 + 3 * 2 / 10"

    val superDumb = "8 * 4"

    println(DumbMathParser.parse(mathStr))
    println(DumbMathParser.parse(mathStr2))

    println("")

    println("")

    println(MathParser.parse(mathStr))
    println("")
    println(MathParser.parse(mathStr2))
    println("")
    println(MathParser.parse(superDumb))
    println("")
    println(MathParser.parse(mathStr3))
  }

}
