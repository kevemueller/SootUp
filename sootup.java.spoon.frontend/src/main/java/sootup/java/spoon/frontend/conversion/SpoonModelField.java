package sootup.java.spoon.frontend.conversion;

import org.jspecify.annotations.NonNull;

import sootup.core.model.FieldModifier;
import sootup.core.model.Position;
import sootup.core.signatures.FieldSignature;
import sootup.java.core.AnnotationUsage;
import sootup.java.core.JavaSootField;
import spoon.reflect.declaration.CtField;

/**
 * A {@link JavaSootField} backed by a Spoon {@link CtField}.
 *
 * @author Keve Müller
 */
public class SpoonModelField extends JavaSootField {

  public SpoonModelField(@NonNull final FieldSignature signature,
      @NonNull final Iterable<FieldModifier> modifiers, @NonNull final Iterable<AnnotationUsage> annotations,
      @NonNull final Position position) {
    super(signature, modifiers, annotations, position);
  }
}
