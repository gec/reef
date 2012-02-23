package org.totalgrid.reef.calc.lib.parse

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import util.parsing.combinator.JavaTokenParsers


object DumbMathParser extends JavaTokenParsers {
  def math: Parser[Any] = term ~ rep("+" ~ term | "-" ~ term)
  def term: Parser[Any] = factor ~ rep("*" ~ factor | "/" ~ factor)
  def factor: Parser[Any] = floatingPointNumber  | "(" ~ math ~ ")"

  def parse(str: String) = parseAll(math, str)
}

sealed trait Node
case class Op(op: String, left: Node, right: Node) extends Node
case class Value(v: Double)

case class Mark(mk: String, rest: Any)

object MathParser extends JavaTokenParsers {

  def math: Parser[Any] = term ~ rep("+" ~ term ^^ mark("+") | "-" ~ term) ^^ mark("term")
  def term: Parser[Any] = factor ~ rep("*" ~ factor ^^ mark("*") | "/" ~ factor)
  def factor: Parser[Any] = floatingPointNumber ^^ {x => Value(x.toDouble)}  | "(" ~> math <~ ")"

  def mark(mk: String): Any => Mark = {
    case x => 
      println(mk + ": " + x)
      Mark(mk, x)
  }
  /*
  def math: Parser[Any] = term ~ rep("+" ~ term | "-" ~ term)
  def term: Parser[Any] = factor ~ rep("*" ~ factor | "/" ~ factor)
  def factor: Parser[Any] = floatingPointNumber  | "(" ~ math ~ ")"
   */
  
  def parse(str: String) = parseAll(math, str)

}

@RunWith(classOf[JUnitRunner])
class OperationParserTest extends FunSuite with ShouldMatchers {

  test("ParseMath") {

    val mathStr = "(5 * 3) + 2"
    val mathStr2 = "5 * 3 + 2"

    val superDumb = "8 * 4"

    println(DumbMathParser.parse(mathStr))
    println(DumbMathParser.parse(mathStr2))

    println("")

    println(MathParser.parse(mathStr))
    println(MathParser.parse(mathStr2))


    println(MathParser.parse(superDumb))
  }

  /*test("ParseTest") {
    OperationParser.test()
  }*/

}
