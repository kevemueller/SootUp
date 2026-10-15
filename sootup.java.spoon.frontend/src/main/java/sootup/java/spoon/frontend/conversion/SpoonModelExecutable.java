package sootup.java.spoon.frontend.conversion;

import java.util.Map;

import org.jspecify.annotations.NonNull;

import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.MethodModifier;
import sootup.core.model.Position;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.java.core.AnnotationUsage;
import sootup.java.core.JavaSootMethod;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;

/**
 * A {@link JavaSootMethod} backed by a Spoon {@link CtExecutable}.
 *
 * @author Keve Müller
 */
public class SpoonModelExecutable extends JavaSootMethod {

  public SpoonModelExecutable(@NonNull final LazySpoonExecutableBodySource source,
      @NonNull final MethodSignature methodSignature,
      @NonNull final Iterable<MethodModifier> modifiers,
      @NonNull final Iterable<ClassType> thrownExceptions,
      @NonNull final Iterable<AnnotationUsage> annotations, @NonNull final Position position) {
    super(source, methodSignature, modifiers, thrownExceptions, annotations, position);
  }

  /**
   * Obtain the mapping between emitted Stmt and correspoding Spoon AST elements.
   * 
   * @return the mapping.
   */
  public Map<Stmt, CtElement> getStmtElementLink() {
    getBody();
    return ((LazySpoonExecutableBodySource) getBodySource()).getStmElementLink();
  }
}
