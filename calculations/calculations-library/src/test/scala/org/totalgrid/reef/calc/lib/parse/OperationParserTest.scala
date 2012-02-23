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
case class Op(op: String, left: Node, right: Node) extends Node {
  override def toString() = "(" + left + " " + op + " " + right + ")"
}
case class Value(v: Double) extends Node {
  override def toString() = v.toString
}
case class Part(op: String, right: Node)

object MathParser extends JavaTokenParsers {

  def math: Parser[Node] = term ~ rep("+" ~ term ^^ part | "-" ~ term ^^ part) ^^ opList
  def term: Parser[Node] = factor ~ rep("*" ~ factor ^^ part | "/" ~ factor ^^ part) ^^ opList
  def factor: Parser[Node] = floatingPointNumber ^^ {x => Value(x.toDouble)}  | "(" ~> math <~ ")"

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

@RunWith(classOf[JUnitRunner])
class OperationParserTest extends FunSuite with ShouldMatchers {

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
