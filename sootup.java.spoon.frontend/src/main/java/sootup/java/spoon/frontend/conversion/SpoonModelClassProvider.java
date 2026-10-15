package sootup.java.spoon.frontend.conversion;

import java.util.Optional;

import org.jspecify.annotations.NonNull;

import sootup.core.frontend.ClassProvider;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.types.ClassType;
import sootup.java.core.views.JavaView;
import spoon.reflect.declaration.CtType;

/**
 * Spoon model specific ClassProvider responsible for creating
 * {@link SpoonModelClassSource}s.
 *
 * @author Keve Müller
 */
public class SpoonModelClassProvider implements ClassProvider<CtType<?>> {
  private final JavaView view;

  public SpoonModelClassProvider(final JavaView view) {
    this.view = view;
  }

  @Override
  public Optional<SpoonModelClassSource> createClassSource(
      @NonNull final AnalysisInputLocation inputLocation, @NonNull final CtType<?> item,
      @NonNull final ClassType classSignature) {
    return Optional.of(new SpoonModelClassSource(view, inputLocation, classSignature, item));
  }
}
