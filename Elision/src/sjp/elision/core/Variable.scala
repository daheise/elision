/* Copyright (c) 2012 by Stacy Prowell (sprowell@gmail.com).
 * All rights reserved.  http://stacyprowell.com
 *       _ _     _
 *   ___| (_)___(_) ___  _ __
 *  / _ \ | / __| |/ _ \| '_ \
 * |  __/ | \__ \ | (_) | | | |
 *  \___|_|_|___/_|\___/|_| |_|
 */
package sjp.elision.core

import scala.collection.mutable.HashMap

/**
 * Represent a variable.
 * @param typ		The variable type.
 * @param name	The variable name.
 */
case class Variable(typ: BasicAtom, name: String) extends BasicAtom {
  /** The type of this variable. */
  val theType = typ
  val deBrujinIndex = 0
  
  /** By default, variables can be bound. */
  override val isBindable = true

  def tryMatchWithoutTypes(subject: BasicAtom, binds: Bindings) =
    // We don't need to worry about the types here.  We can bind the variable
    // if the variable allows binding, and it is not already bound to a
    // different atom.
    if (isBindable) binds.get(name) match {
      case None => Match(binds + (name -> subject))
      case Some(atom) if atom == subject => Match(binds)
      case _ => Fail("Variable already bound to another term.", this, subject)
    }
    else Fail("Variable is not bindable.", this, subject)

  def rewrite(binds: Bindings) = {
    // If this variable is bound in the provided bindings, replace it with the
    // bound value.
    binds.get(name) match {
      case Some(atom) =>
        // We don't rewrite De Brujin indices to different indices.
        if (isDeBrujinIndex && atom.isDeBrujinIndex) (this, false)
        else (atom, true)
      case None =>
        // While the atom is not bound, its type might have to be rewritten.
        theType.rewrite(binds) match {
          case (newtype, changed) =>
            if (changed) (Variable(newtype, name), true) else (this, false)
          case _ => (this, false)
        }
    }
  }

  def toParseString = "$" + toESymbol(name) + ":" + typ.toParseString
  
  override def toString = "Variable(" + typ + "," + toEString(name) + ")"
}
