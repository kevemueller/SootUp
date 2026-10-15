package sootup.java.spoon.frontend.inputlocation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sootup.core.IdentifierFactory;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.interceptor.BodyInterceptor;
import sootup.core.model.SourceType;
import sootup.core.signatures.PackageName;
import sootup.core.types.ClassType;
import sootup.core.views.View;
import sootup.java.core.signatures.ModulePackageName;
import sootup.java.core.signatures.ModuleSignature;
import sootup.java.core.views.JavaView;
import sootup.java.spoon.frontend.conversion.SpoonModelClassSource;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtModule;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * An implementation of the {@link AnalysisInputLocation} interface backed by a
 * Spoon Model.
 *
 * @author Keve Müller
 */
public final class SpoonModelAnalysisInputLocation implements AnalysisInputLocation {
  private static final Logger LOG = LoggerFactory.getLogger(SpoonModelAnalysisInputLocation.class);

  /**
   * Create a {@link SpoonModelAnalysisInputLocation} using the provided CtModel.
   *
   * @param ctModel the source to locate types in
   */
  public static SpoonModelAnalysisInputLocation of(final CtModel ctModel) {
    return new SpoonModelAnalysisInputLocation(ctModel);
  }

  @NonNull
  private final List<BodyInterceptor> bodyInterceptors;

  @NonNull
  private final CtModel ctModel;

  private SpoonModelAnalysisInputLocation(@NonNull final CtModel ctModel) {
    this.ctModel = ctModel;
    this.bodyInterceptors = List.of();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final SpoonModelAnalysisInputLocation other = (SpoonModelAnalysisInputLocation) obj;
    return Objects.equals(bodyInterceptors, other.bodyInterceptors)
        && Objects.equals(ctModel, other.ctModel);
  }

  @Override
  @NonNull
  public List<BodyInterceptor> getBodyInterceptors() {
    return bodyInterceptors;
  }

  @Override
  public @NonNull Optional<SpoonModelClassSource> getClassSource(@NonNull final ClassType type,
      @NonNull final View view) {
    LOG.trace("getClassSource({},{})", type, view);
    final PackageName jiPackageName = type.getPackageName();
    final Optional<CtPackage> ctPackageO;
    if (jiPackageName instanceof final ModulePackageName modulePackage) {
      final ModuleSignature moduleSignature = modulePackage.getModuleSignature();
      final Optional<CtModule> ctModule = moduleSignature.isUnnamedModule()
          ? Optional.of(ctModel.getUnnamedModule())
          : ctModel.getAllModules().stream()
              .filter(m -> Objects.equals(m.getSimpleName(), moduleSignature.getModuleName()))
              .findFirst();
      ctPackageO = ctModule.flatMap(m -> m.getElements(new TypeFilter<>(CtPackage.class)).stream()
          .filter(ctP -> Objects.equals(ctP.getQualifiedName(), modulePackage.getName()))
          .findFirst());
    } else {
      ctPackageO = ctModel.getAllPackages().stream()
          .filter(ctP -> Objects.equals(ctP.getQualifiedName(), jiPackageName.getName()))
          .findFirst();
    }
    if (ctPackageO.isEmpty()) {
      return Optional.empty();
    }
    final CtPackage ctPackage = ctPackageO.get();
    LOG.trace("getClassSource( in package {}", ctPackageO);
    // whomever thought that $ is a good separator while it is also allowed in the
    // name...
    final Optional<CtType<?>> topTypeO = Optional
        .<CtType<?>>ofNullable(
            ctPackage.getType(type.getClassName()))
        .or(() -> Optional.ofNullable(ctPackage.filterChildren(el -> el instanceof final CtType<?> t
            && t.getQualifiedName().equals(type.getFullyQualifiedName())).first()));
    return topTypeO.map(ctT -> new SpoonModelClassSource((JavaView) view, this, type, ctT));
  }

  @Override
  public @NonNull Stream<SpoonModelClassSource> getClassSources(@NonNull final View view) {
    LOG.debug("getClassSources({})", view);

    final Stream<CtType<?>> allTypes = ctModel.getAllTypes().stream().flatMap(this::allNested);
    final IdentifierFactory identifierFactory = view.getIdentifierFactory();
    return allTypes.map(ctT -> {
      final ClassType classSignature = identifierFactory.getClassType(ctT.getQualifiedName());
      return new SpoonModelClassSource((JavaView) view, this, classSignature, ctT);
    });
  }

  @Override
  @NonNull
  public SourceType getSourceType() {
    return SourceType.Application;
  }

  @Override
  public int hashCode() {
    return Objects.hash(bodyInterceptors, ctModel);
  }

  // TODO: allow entry points CtPackage, CtType, CtExecutable, String
  // methodSnippet

  private Stream<CtType<?>> allNested(final CtType<?> type) {
    final Set<CtType<?>> nestedTypes = type.getNestedTypes();
    if (nestedTypes.isEmpty()) {
      return Stream.of(type);
    }
    return Stream.concat(Stream.of(type), nestedTypes.stream().flatMap(this::allNested));
  }
}
