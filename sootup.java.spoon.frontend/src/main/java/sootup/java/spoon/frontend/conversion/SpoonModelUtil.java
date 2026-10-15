package sootup.java.spoon.frontend.conversion;

import java.util.Optional;

import sootup.core.jimple.basic.NoPositionInformation;
import sootup.core.model.FieldModifier;
import sootup.core.model.FullPosition;
import sootup.core.model.MethodModifier;
import sootup.core.model.Position;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.ModifierKind;

/**
 * Generic untilites to work with Spoon and SootUp together.
 *
 * @author Keve Müller
 */
public final class SpoonModelUtil {

  public static FieldModifier mapFieldModifier(final ModifierKind ctModifier) {
    return switch (ctModifier) {
    case FINAL -> FieldModifier.FINAL;
    case PRIVATE -> FieldModifier.PRIVATE;
    case PROTECTED -> FieldModifier.PROTECTED;
    case PUBLIC -> FieldModifier.PUBLIC;
    case STATIC -> FieldModifier.STATIC;
    case VOLATILE -> FieldModifier.VOLATILE;
    case TRANSIENT -> FieldModifier.TRANSIENT;
    default -> throw new IllegalArgumentException(ctModifier.toString());
    };
  }

  public static MethodModifier mapMethodModifier(final ModifierKind ctModifier) {
    return switch (ctModifier) {
    case ABSTRACT -> MethodModifier.ABSTRACT;
    case FINAL -> MethodModifier.FINAL;
    case NATIVE -> MethodModifier.NATIVE;
    case PRIVATE -> MethodModifier.PRIVATE;
    case PROTECTED -> MethodModifier.PROTECTED;
    case PUBLIC -> MethodModifier.PUBLIC;
    case STATIC -> MethodModifier.STATIC;
    case STRICTFP -> MethodModifier.STRICTFP;
    case SYNCHRONIZED -> MethodModifier.SYNCHRONIZED;
    default -> throw new IllegalArgumentException(ctModifier.toString());
    };
  }
}
