package org.totalgrid.reef.calc.lib.eval

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

import util.parsing.combinator.JavaTokenParsers

/**
 * expr :: = mult { "+" mult | "-" mult }
 * mult :: = exp { "*" fac | "/" fac | "%" fac }
 * exp ::= leaf { "^" leaf }
 * leaf ::= const | fun | var | "(" expr ")"
 * fun ::= ident "(" csv ")"
 * csv ::= expr { "," expr }
 *
 */
object OperationParser extends JavaTokenParsers {

  import OperationInterpreter._

  def expr: Parser[Expression] = mult ~ rep("+" ~ mult ^^ part | "-" ~ mult ^^ part) ^^ multiInfix

  def mult: Parser[Expression] = exp ~ rep("*" ~ exp ^^ part | "/" ~ exp ^^ part) ^^ multiInfix

  def exp: Parser[Expression] = leaf ~ rep("^" ~ leaf ^^ part) ^^ multiInfix

  def leaf: Parser[Expression] = constant | fun | variable | "(" ~> expr <~ ")"

  def fun: Parser[Expression] = ident ~ ("(" ~> csv <~ ")") ^^ {
    case f ~ args => Fun(f, args)
  }

  def csv: Parser[List[Expression]] = expr ~ rep("," ~> expr) ^^ {
    case head ~ tail => head :: tail
  }

  def variable: Parser[Expression] = ident ^^ {
    Var(_)
  }

  def constant: Parser[Expression] = floatingPointNumber ^^ { x =>
    Const(x.toDouble)
  }

  case class Part(op: String, right: Expression)

  def part: ~[String, Expression] => Part = {
    case op ~ r => Part(op, r)
  }

  def multiInfix: ~[Expression, List[Part]] => Expression = {
    case n ~ Nil => n
    case l ~ reps => reps.foldLeft(l) {
      case (l, part) => Infix(part.op, l, part.right)
    }
  }

  def parseFormula(formula: String): Expression = {
    parseAll(expr, formula) getOrElse {
      throw new Exception("Bad Parse: " + formula)
    }
  }
}
