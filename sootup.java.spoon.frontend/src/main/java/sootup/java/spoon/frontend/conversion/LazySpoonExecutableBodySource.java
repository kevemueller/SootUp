package sootup.java.spoon.frontend.conversion;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sootup.core.frontend.BodySource;
import sootup.core.frontend.ResolveException;
import sootup.core.interceptor.BodyInterceptor;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.Body.BodyBuilder;
import sootup.core.model.MethodModifier;
import sootup.core.signatures.MethodSignature;
import sootup.core.views.View;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.views.JavaView;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;

/**
 * Generate a body lazily from the Spoon CtExecutable.
 *
 * As our bodies are still in developing state, we return an empty body instead
 * of throwing a RuntimeException. Observe the logs for the details.
 */
public class LazySpoonExecutableBodySource implements BodySource {
  private static final Logger LOG = LoggerFactory.getLogger(LazySpoonExecutableBodySource.class);

  public static LazySpoonExecutableBodySource of(final JavaView view,
      final List<BodyInterceptor> bodyInterceptors, final MethodSignature methodSignature,
      final CtExecutable<?> ctExecutable) {
    return new LazySpoonExecutableBodySource(view, bodyInterceptors, methodSignature, ctExecutable);
  }

  private final @NonNull MethodSignature methodSignature;
  private final @NonNull CtExecutable<?> ctExecutable;

  private final JavaIdentifierFactory identifierFactory;

  private final List<BodyInterceptor> bodyInterceptors;

  private final View view;
  private Map<Stmt, CtElement> stmtElementLink;

  private LazySpoonExecutableBodySource(final JavaView view,
      final List<BodyInterceptor> bodyInterceptors, @NonNull final MethodSignature methodSignature,
      final CtExecutable<?> ctExecutable) {
    this.view = view;
    this.bodyInterceptors = bodyInterceptors;
    this.methodSignature = methodSignature;
    this.ctExecutable = ctExecutable;
    this.identifierFactory = view.getIdentifierFactory();
  }

  @Override
  public @NonNull MethodSignature getSignature() {
    return methodSignature;
  }

  @Override
  public Object resolveAnnotationsDefaultValue() {
    return null;
  }

  @Override
  public @NonNull Body resolveBody(@NonNull final Iterable<MethodModifier> modifiersIterable)
      throws ResolveException, IOException {
    final BodyBuilder bodyBuilder = makeBuilder(modifiersIterable);

    try {
      SpoonModelBodyConverter spoonModelBodyConverter = new SpoonModelBodyConverter(
          identifierFactory, bodyBuilder);
      spoonModelBodyConverter.scan(ctExecutable);
      stmtElementLink = spoonModelBodyConverter.getStmtElementLink();
    } catch (final NullPointerException npex) {
      LOG.error("NPE", npex);
//      throw npex;
    }

    try {
      for (final BodyInterceptor bodyInterceptor : bodyInterceptors) {
        bodyInterceptor.interceptBody(bodyBuilder, view);
      }
      return bodyBuilder.build();
    } catch (final RuntimeException ex) {
      if (ex.getCause() instanceof final IllegalStateException isx
          && isx.getMessage().startsWith("visualize invalid ControlFlowGraph: ")) {
        LOG.error("CFG", ex);
        LOG.error("{}", ex.getMessage());
        LOG.error("{}", isx.getMessage().substring(36));
        return makeBuilder(modifiersIterable).build();
      }
      throw ex;
    }
  }

  private BodyBuilder makeBuilder(@NonNull final Iterable<MethodModifier> modifiersIterable) {
    final BodyBuilder bodyBuilder = Body.builder();
    bodyBuilder.setMethodSignature(methodSignature);
    bodyBuilder.setPosition(SpoonConverter.toPosition(ctExecutable.getBody()));
    final EnumSet<MethodModifier> modifiers = EnumSet.noneOf(MethodModifier.class);
    modifiersIterable.forEach(modifiers::add);
    bodyBuilder.setModifiers(modifiers);
    return bodyBuilder;
  }

  public Map<Stmt, CtElement> getStmElementLink() {
    return stmtElementLink;
  }
}
