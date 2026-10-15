package sootup.java.spoon.frontend.tests;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.myers.MyersDiff;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;

import sootup.core.jimple.basic.EquivTo;
import sootup.core.jimple.basic.IgnoreLocalNameComparator;
import sootup.core.jimple.basic.JimpleComparator;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.Body.BodyBuilder;
import sootup.java.core.JavaSootMethod;

/**
 * Initial take on a Diff utility comparing semantically equivalent method
 * bodies.
 *
 * Leverages SootUp's {@link EquivTo#equivTo(Object, JimpleComparator)} and
 * {@link DiffUtils#diff(List, List, com.github.difflib.algorithm.DiffAlgorithmI, com.github.difflib.algorithm.DiffAlgorithmListener, boolean)}.
 *
 * @author Keve Müller
 */
public final class BodyAligner {
  private static final Logger LOG = LoggerFactory.getLogger(BodyAligner.class);
  /**
   * Dump the mapping for debugging purposes.
   *
   * @param out     the PrintStream to dump to.
   * @param mapping the mapping to dump.
   */
  public static void dumpMapping(final PrintStream out, final Map<Local, List<Local>> mapping) {
    out.println("Proposed mappings original -> revised");
    mapping.forEach((k, v) -> out.printf("%s -> %s\n", k, 1 == v.size() ? v.get(0) : v));
  }

  public static BodyAligner of() {
    return new BodyAligner(new IgnoreLocalNameComparator());
  }

  private final JimpleComparator jiComparator;

  private BodyAligner(final JimpleComparator jiComparator) {
    this.jiComparator = jiComparator;
  }

  /**
   * Compute a mapping of Locals between original an revised, apply the mapping on
   * original, renaming its locals, so that they are better aligned to revised.
   *
   * @param original the method to align (to be modified)
   * @param revised  the method to align to (read-only)
   * @return the aligned original method, possibly resembling revised more.
   */
  public JavaSootMethod alignedTo(final JavaSootMethod original, final Body revised) {
    // TODO: handle circular rename, e.g. swap
    // TODO: handle ambiguous renames, e.g. by performing non-ambiguous renames,
    // then "locking" those Locals, i.e. enforce strict equality on them, rinse and
    // repeat until no more mappings are produced.
    final Map<Local, Set<Local>> mappings = computeLocalMapping(original.getBody(), revised);
    if (mappings.isEmpty()) {
      return original;
    }
    int iteration = 0;
    final int maxIterations = 3;
    final BodyBuilder alignedBody = Body.builder(original.getBody(), original.getModifiers());
    do {
      for (final Iterator<Entry<Local, Set<Local>>> mappingIt = mappings.entrySet().iterator(); mappingIt
          .hasNext();) {
        final Entry<Local, Set<Local>> mapping = mappingIt.next();
        final Local sourceLocal = mapping.getKey();
        if (1 == mapping.getValue().size()) {
          final Local targetLocal = mapping.getValue().iterator().next();
          final Local alignedLocal = sourceLocal.withName(targetLocal.getName());
          if (sourceLocal.getName().equals(targetLocal.getName())) {
            mappingIt.remove(); // NOP
          } else if (alignedBody.getLocals().contains(alignedLocal)) {
            LOG.debug("Blocked mapping: {}->{}", sourceLocal, alignedLocal);
          } else {
            LOG.debug("Applying mapping: {}->{}", sourceLocal, alignedLocal);
            alignedBody.replaceLocal(sourceLocal, alignedLocal);
            mappingIt.remove();
          }
        } else {
          LOG.debug("Ambiguous mapping: {}->{}", sourceLocal, mapping.getValue());
        }
      }
      if (!mappings.isEmpty()) {
        mappings.forEach(
            (k, v) -> LOG.info("Not mapped: {}->{}", k, 1 == v.size() ? v.iterator().next() : v));
      }
      iteration++;
    } while (iteration < maxIterations);
    return original.withBody(alignedBody.build());
  }

  /**
   * Diff provided original body with revised body, walk the List of deltas that
   * are matched EQUAL and extract local rename candidates.
   *
   * @param jiOriginalBody the original SootUp body of statements.
   * @param jiRevisedBody  the revised SootUp body of statements.
   * @return mapping from Local in original body to a List of Local in the revised
   *         body.
   */
  public Map<Local, Set<Local>> computeLocalMapping(final Body jiOriginalBody, final Body jiRevisedBody) {
    final BiPredicate<Stmt, Stmt> equalizer = (l, r) -> l.equivTo(r, jiComparator);
    final Patch<Stmt> patch = DiffUtils.diff(jiOriginalBody.getStmts(), jiRevisedBody.getStmts(),
        MyersDiff.factory().create(equalizer), null, true);

    final Map<Local, Set<Local>> mapping = new LinkedHashMap<>();
    final BiConsumer<Local, Local> addMapping = (l, r) -> mapping.compute(l, (k, v) -> {
      if (null == v) {
        return Set.of(r);
      }
      final Set<Local> m = new LinkedHashSet<>(v);
      m.add(r);
      return m;
    });

    for (final AbstractDelta<Stmt> delta : patch.getDeltas()) {
      if (DeltaType.EQUAL == delta.getType()) {
        for (Iterator<Stmt> sourceLinesIt = delta.getSource().getLines().iterator(),
            targetLinesIt = delta.getTarget().getLines().iterator(); sourceLinesIt.hasNext()
                && targetLinesIt.hasNext();) {
          final Stmt sourceLine = sourceLinesIt.next();
          final Stmt targetLine = targetLinesIt.next();
          if (sourceLine instanceof final JIdentityStmt jiSourceIdentityStmt
              && targetLine instanceof final JIdentityStmt jiTargetIdentityStmt) {
            addMapping.accept(jiSourceIdentityStmt.getLeftOp(), jiTargetIdentityStmt.getLeftOp());
          } else if (sourceLine instanceof final JAssignStmt jiSourceAssignStmt
              && jiSourceAssignStmt.getLeftOp() instanceof final Local jiSourceLocal
              && targetLine instanceof final JAssignStmt jiTargetAssignStmt
              && jiTargetAssignStmt.getLeftOp() instanceof final Local jiTargetLocal) {
            addMapping.accept(jiSourceLocal, jiTargetLocal);
          }
        }
      }
    }
    return mapping;
  }

}
