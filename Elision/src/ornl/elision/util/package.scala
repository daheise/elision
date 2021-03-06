/*======================================================================
 *       _ _     _
 *   ___| (_)___(_) ___  _ __
 *  / _ \ | / __| |/ _ \| '_ \
 * |  __/ | \__ \ | (_) | | | |
 *  \___|_|_|___/_|\___/|_| |_|
 * The Elision Term Rewriter
 * 
 * Copyright (c) 2012 by UT-Battelle, LLC.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * Collection of administrative costs for redistribution of the source code or
 * binary form is allowed. However, collection of a royalty or other fee in excess
 * of good faith amount for cost recovery for such redistribution is prohibited.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER, THE DOE, OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
======================================================================
* */
package ornl.elision

/**
 * This is the utility package for Elision.
 * 
 * This package provides common services and support for other parts of the
 * Elision system.  Any part of Elision may use it, but it cannot use other
 * parts of the Elision system!  That is, it is a leaf in the use hierarchy.
 */
import scala.collection.IndexedSeq
import scala.annotation.tailrec
package object util {
  /**
   * Turn a string into a properly-escaped double-quoted string.  The following
   * transformations are performed.
   * {{{
   * double quotation mark   -> \"
   * newline                 -> \n
   * tab                     -> \t
   * carriage return         -> \r
   * backslash               -> \\
   * }}}
   * The resulting string is enclosed in double quotation marks.
   * 
   * @param str The string.
   * @return  The string with special character escaped.
   */
  def toQuotedString(str: String) = {
    var buf = new scala.collection.mutable.StringBuilder
    buf ++= "\""
    for (ch <- str) {
      ch match {
        case '"' => buf ++= "\\\""
        case '\n' => buf ++= """\n"""
        case '\t' => buf ++= """\t"""
        case '\r' => buf ++= """\r"""
        case '\\' => buf ++= """\\"""
        case _ => buf ++= ch.toString
      }
    }
    buf ++= "\""
    buf.toString
  }
  
  /**
   * Provide a convenient method to compute a hash code from many different
   * objects.  This is intended to be suitable for a few basic cases.
   * 
   * If you want to create a hash code for two objects alice and bob, try this.
   * {{{
   * 0 hashify alice hashify bob
   * }}}
   * 
   * If you want to create a hash code for a sequence of objects in stuff, try
   * this.
   * {{{
   * stuff.foldLeft(0)(_ hashify _)
   * }}}
   * 
   * If you want to combine your hash code with your children's hash codes,
   * try this.
   * {{{
   * children.hashify(hashCode)(_ hashfiy _)
   * }}}
   * 
   * @param hash    The initial hash code.
   * @param obj     The next object whose hash should be added.
   */
  def hashify(hash: Int = 0, obj: Any) = {
    // Add a constant to the end so that single element collections have a
    // different hashcode than the element they contain.    
    hash * 12289 + obj.hashCode + (if (hash==0) 31 else 0)
  }

  /**
   * Compute an alternate hash code from many different objects. An
   * alternate hash code is used in some cases to provide 2 different
   * hash codes for an Elision object. These 2 hash codes are used to
   * lower the chances of a hash collision (both different hash codes
   * will need to collide for a hash collision to occur).
   * 
   * If the object provides an "other" hash code, it must have a field or
   * a field-like method `otherHashCode` that returns a `BigInt`.  This is
   * used to compute the hash code returned by this method.
   *
   * @param hash    The initial hash code.
   * @param obj     The next object whose hash should be added.
   */
  def other_hashify(hash: Long = 0, obj: Any): Long = {
    obj match {
      case ohc: HasOtherHash => hash + 8191*ohc.otherHashCode
      case _ => hash + 8191*obj.hashCode
    }
  }
  
  
  // The following two functions exist because for comprehensions are slow in 
  // Scala, but typing while loops over and over is annoying. If other looping
  // contructs get better, we can switch these out.
  /**
   * Execute the closure a fixed number of times.
   * 
   * @param start     Where to begin iteration
   * @param end       Where to end iteration
   * @param increment How much to increment the index on each loop
   * @param inclusive Whether to include the uppper bound
   * @param closure   The function to execute. The argument to the closure is
   *                   the loop index.
   */  
  @inline
  def countedloop(start: Int, end: Int, increment : Int = 1, inclusive : Boolean = true, closure : Int => Unit){
    var i = start
    val _end = if(inclusive) end+1 else end
    while(i < _end){
      closure(i)
      i += increment
    }
  }
  
  /**
   * Iterate over a sequence. The argument to the closure is the current
   * sequence index.
   * 
   * @param collection     The collection to iterate over.
   * @param closure   The function to execute. The argument to the closure is
   *                   the loop index.
   */
  @inline
  def seqloop(collection : Seq[Any], closure : Int => Unit){
    var i = 0
    val len = collection.length
    while(i < len){
      closure(i)
      i += 1
    }
  }

  // Set the maximum factorial we will support.
  // 21! overflows a Long, so we use 20 here.
  private val maxfact = 20
  private val factlookup = {
    def _factorial(n: Int) = {
      @tailrec
      def __factorial(n: Int, acc: Long = 1): Long = {
        if (n == 0) acc
        else __factorial(n - 1, n * acc)
      }
      __factorial(n)
    }
    for(i <- 0 to maxfact) yield _factorial(maxfact)
  }
  
  def factorial(n: Int) = {
    if(n > maxfact) Long.MaxValue else factlookup(n)
  }
}
