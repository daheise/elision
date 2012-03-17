/*       _ _     _
 *   ___| (_)___(_) ___  _ __
 *  / _ \ | / __| |/ _ \| '_ \
 * |  __/ | \__ \ | (_) | | | |
 *  \___|_|_|___/_|\___/|_| |_|
 *
 * Copyright (c) 2012 by Stacy Prowell (sprowell@gmail.com).
 * All rights reserved.  http://stacyprowell.com
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package sjp.elision.core.matcher
import sjp.elision.core._

/**
 * Match two sequences whose elements can be re-ordered or re-grouped.  That is,
 * the lists are associative and commutative.
 */
object ACMatcher {

  /**
   * Attempt to match two lists.  The second list can be re-ordered and
   * re-grouped arbitrarily.
   * 
   * @param plist	The pattern list.
   * @param slist	The subject list.
   * @param binds	Bindings that must be honored in any match.
   * @param op		An optional operator to apply to sublists.
   * @return	The match outcome.
   */
  def tryMatch(plist: AtomList, slist: AtomList, binds: Bindings,
      op: Option[Operator]): Outcome = {
    if (plist.atoms.length > slist.atoms.length)
      return Fail("More patterns than subjects, so no match is possible.",
          plist, slist)
          
    // If there are the same number, then this is a simple case of commutative
    // matching.
    if (plist.atoms.length == slist.atoms.length)
      return CMatcher.tryMatch(plist, slist, binds)
      
    // Step one is to perform constant elimination.  Any constants must match
    // exactly, and we match and remove them.
    var (patterns, subjects, fail) = MatchHelper.eliminateConstants(plist, slist)
    if (fail.isDefined) return fail.get
    
    // Step two is to match and eliminate any unbindable atoms.  These are
    // atoms that are not variables, and so their matching is much more
    // restrictive.  We obtain an iterator over these, and then combine it
    // with the iterator for "everything else."
    val um = new UnbindableMatcher(patterns, subjects, binds)
    
    // This is not so simple.  We need to perform the match.
    val iter = um ~ (bindings => {
      val pats = AtomList(bindings.patterns.getOrElse(patterns), plist.props)
      val subs = AtomList(bindings.subjects.getOrElse(subjects), slist.props)
      new ACMatchIterator(pats, subs, bindings, op)
    })
    if (iter.hasNext) return Many(iter)
    else Fail("The lists do not match.", plist, slist)
  }
  
  /* How associative and commutative matching works.
   * 
   * The subject list must be at least as long as the pattern list, or no
   * match is possible.
   * 
   * We first permute the subjects, and then iterate over all groupings of
   * the subjects.
   */
  
  private class ACMatchIterator(patterns: AtomList, subjects: AtomList,
      binds: Bindings, op: Option[Operator]) extends MatchIterator {
    /** An iterator over all permutations of the subjects. */
    private val _perms = subjects.atoms.permutations

    /**
     * Find the next match.  At the end of running this method either we
     * have `_current` set to the next match or we have exhausted the
     * iterator.
     */
    protected def findNext {
      print("AC Searching... ")
      _current = null
      if (_local != null && _local.hasNext) _current = _local.next
      else {
        _local = null
	      if (_perms.hasNext)
	        AMatcher.tryMatch(patterns, AtomList(_perms.next, subjects.props),
	            binds, op) match {
	        case Fail(_,_) =>
	          // We ignore this case.  We only fail if we exhaust all attempts.
	          findNext
	        case Match(binds) =>
	          // This case we care about.  Save the bindings as the current match.
	          _current = binds
	          println("AC Found.")
	        case Many(iter) =>
	          // We've potentially found many matches.  We save this as a local
	          // iterator and then use it in the future.
	          _local = iter
	          findNext
	      } else {
	        // We have exhausted the permutations.  We have exhausted this
	        // iterator.
	        _exhausted = true
	        println("AC Exhausted.")
	      }
      }
    }
  }
}
