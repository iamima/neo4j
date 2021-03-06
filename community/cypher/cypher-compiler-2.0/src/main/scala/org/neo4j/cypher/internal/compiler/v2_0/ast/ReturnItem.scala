/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.v2_0.ast

import org.neo4j.cypher.internal.compiler.v2_0._
import org.neo4j.cypher.internal.compiler.v2_0.commands

sealed trait ReturnItems extends AstNode with SemanticCheckable {
  def toCommands: Seq[commands.ReturnColumn]
  def declareIdentifiers(currentState: SemanticState): SemanticCheck
}

case class ListedReturnItems(items: Seq[ReturnItem])(val token: InputToken) extends ReturnItems {
  def semanticCheck = items.semanticCheck

  def declareIdentifiers(currentState: SemanticState) =
    items.foldSemanticCheck(item => item.alias match {
      case Some(identifier) => identifier.declare(item.expression.types(currentState))
      case None             => SemanticCheckResult.success
    })

  def toCommands = items.map(_.toCommand)
}

case class ReturnAll()(val token: InputToken) extends ReturnItems {
  def semanticCheck = SemanticCheckResult.success

  def declareIdentifiers(currentState: SemanticState) = s => SemanticCheckResult.success(s.importSymbols(currentState.symbolTable))

  def toCommands = Seq(commands.AllIdentifiers())
}


sealed trait ReturnItem extends AstNode with SemanticCheckable {
  def expression: Expression
  def alias: Option[Identifier]
  def name: String

  def semanticCheck = expression.semanticCheck(Expression.SemanticContext.Results)

  def toCommand: commands.ReturnItem
}

case class UnaliasedReturnItem(expression: Expression, inputText: String)(val token: InputToken) extends ReturnItem {
  val alias = expression match {
    case i: Identifier => Some(i)
    case _ => None
  }
  val name = alias.map(_.name) getOrElse { inputText.trim }

  def toCommand = commands.ReturnItem(expression.toCommand, name)
}

case class AliasedReturnItem(expression: Expression, identifier: Identifier)(val token: InputToken) extends ReturnItem {
  val alias = Some(identifier)
  val name = identifier.name

  def toCommand = commands.ReturnItem(expression.toCommand, name, true)
}
