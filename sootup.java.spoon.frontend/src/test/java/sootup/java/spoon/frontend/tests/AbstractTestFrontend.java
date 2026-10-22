package sootup.java.spoon.frontend.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.Body;
import sootup.core.model.ClassModifier;
import sootup.core.model.FieldModifier;
import sootup.core.model.MethodModifier;
import sootup.core.model.SourceType;
import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.core.util.JimpleBodySerializer;
import sootup.java.bytecode.frontend.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootField;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;
import sootup.java.spoon.frontend.inputlocation.SpoonModelAnalysisInputLocation;
import sootup.java.spoon.frontend.util.DebugScanner;
import sootup.java.spoon.frontend.util.SpoonUtil;
import sootup.jimple.frontend.JimpleAnalysisInputLocation;
import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.reflect.CtModel;
import spoon.reflect.visitor.ModelConsistencyChecker;

/**
 * Abstract top class for Frontend related tests.
 *
 * @author Keve Müller
 */
public abstract class AbstractTestFrontend {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractTestFrontend.class);

  protected Launcher launcher;

  @AfterEach
  public void closeReport() {
    if (null != launcher) {
      LOG.debug("Spoon closing report");
      final Environment env = launcher.getEnvironment();
      final ModelConsistencyChecker modelConsistencyChecker = new ModelConsistencyChecker(env,
          false, false);
      modelConsistencyChecker.scan(launcher.getModel().getAllModules());
      env.reportEnd();
      assertEquals(0, env.getWarningCount());
    }
  }

  @BeforeEach
  public void init() {
    launcher = null;
  }

  protected boolean assertEqualsFieldModifier(final Collection<FieldModifier> expectedIn,
      final Collection<FieldModifier> actualIn) {
    final EnumSet<FieldModifier> expected = expectedIn.isEmpty()
        ? EnumSet.noneOf(FieldModifier.class)
        : EnumSet.copyOf(expectedIn);
    final EnumSet<FieldModifier> actual = actualIn.isEmpty() ? EnumSet.noneOf(FieldModifier.class)
        : EnumSet.copyOf(actualIn);
    if (expected.equals(actual)) {
      return true;
    }
    // Jimple does not preserve synthetic
    expected.remove(FieldModifier.SYNTHETIC);
    actual.remove(FieldModifier.SYNTHETIC);
    assertEquals(expected, actual);
    return false;
  }

  protected final void assertEqualsJavaSootClassMetadata(final JavaSootClass jiExpectedClass,
      final JavaSootClass jiActualClass) throws IOException {
    assertEquals(jiExpectedClass.getName(), jiActualClass.getName());
    assertEquals(jiExpectedClass.getType(), jiActualClass.getType());

    final Set<ClassModifier> expectedModifiers = jiExpectedClass.getModifiers();
    final Set<ClassModifier> spoonModifiers = jiActualClass.getModifiers();
    assertNotNull(expectedModifiers);
    assertNotNull(spoonModifiers);

    // TODO: wait for synthesized modifiers
//     assertTrue(expectedModifiers.containsAll(spoonModifiers),
//     expectedModifiers.toString() + "<>" + spoonModifiers.toString());
    // TODO: wait for redundant modifier cleanup
//     assertTrue(spoonModifiers.containsAll(expectedModifiers),
//     spoonModifiers.toString() + "<>" + expectedModifiers.toString());

    /* Hierarchy */
    assertEquals(jiExpectedClass.getSuperclass(), jiActualClass.getSuperclass());

    final Set<? extends ClassType> expectedInterfaces = jiExpectedClass.getInterfaces();
    final Set<? extends ClassType> spoonInterfaces = jiActualClass.getInterfaces();

    assertIterableEquals(expectedInterfaces, spoonInterfaces);

    final Optional<? extends ClassType> expectedOuterClass = jiExpectedClass.getOuterClass();
    final Optional<? extends ClassType> spoonOuterClass = jiActualClass.getOuterClass();

    assertNotNull(expectedOuterClass);
    assertNotNull(spoonOuterClass);

    // Jimple is alway TopClass, Bytecode is always empty.
    // nestHostOuterClass??
    // assertEquals(expectedOuterClass, spoonOuterClass);

  }

  protected void assertEqualsJavaSootField(final JavaSootField jiExpectedField,
      final JavaSootField jiActualField) {
    assertEquals(jiExpectedField.getName(), jiActualField.getName());
    assertEquals(jiExpectedField.getType(), jiActualField.getType());
    assertEquals(jiExpectedField.getSignature(), jiActualField.getSignature());
    final boolean strict = assertEqualsFieldModifier(jiExpectedField.getModifiers(),
        jiActualField.getModifiers());
    if (strict) {
      assertEquals(jiExpectedField.equivHashCode(), jiActualField.equivHashCode());
    }
  }

  protected void assertEqualsJavaSootMethod(final JavaSootMethod jiExpectedMethod,
      final JavaSootMethod jiActualMethod) {
    assertEquals(jiExpectedMethod.getName(), jiActualMethod.getName());
    assertEquals(jiExpectedMethod.getDeclaringClassType(), jiActualMethod.getDeclaringClassType());
    final boolean strict = assertEqualsMethodModifier(jiExpectedMethod.getModifiers(),
        jiActualMethod.getModifiers(), jiExpectedMethod + "<>" + jiActualMethod);
    assertIterableEquals(jiExpectedMethod.getParameterTypes(), jiActualMethod.getParameterTypes(),
        jiExpectedMethod + "<>" + jiActualMethod);
    assertEquals(jiExpectedMethod.getSignature(), jiActualMethod.getSignature());
    if (strict) {
      assertEquals(jiExpectedMethod.equivHashCode(), jiActualMethod.equivHashCode());
    }
    if (jiExpectedMethod.isConcrete()) {
      assertEqualsBody(jiExpectedMethod.getBody(), jiActualMethod);
    }
  }

  protected boolean assertEqualsMethodModifier(final Collection<MethodModifier> expectedIn,
      final Collection<MethodModifier> actualIn, final String message) {
    final EnumSet<MethodModifier> expected = expectedIn.isEmpty()
        ? EnumSet.noneOf(MethodModifier.class)
        : EnumSet.copyOf(expectedIn);
    final EnumSet<MethodModifier> actual = actualIn.isEmpty() ? EnumSet.noneOf(MethodModifier.class)
        : EnumSet.copyOf(actualIn);
    if (expected.equals(actual)) {
      return true;
    }
    // Jimple does not preserve synthetic
    expected.remove(MethodModifier.SYNTHETIC);
    actual.remove(MethodModifier.SYNTHETIC);
    assertEquals(expected, actual, message);
    return false;
  }

  protected <T> Entry<T, T> bind(final Entry<Optional<T>, Optional<T>> optionalEntry) {
    return Map.entry(optionalEntry.getKey().get(), optionalEntry.getValue().get());
  }

  protected final JavaView buildByteCodeView(final Path classesDir, final Path... ignoredPaths) {
    final List<AnalysisInputLocation> inputLocations = List.of(PathBasedAnalysisInputLocation
        .create(classesDir, SourceType.Application, List.of(), Arrays.asList(ignoredPaths)));
    final JavaView validateView = new JavaView(inputLocations);
    return validateView;
  }

  protected final CtModel buildCtModel(final Path sourcesDir) {
    try {
      launcher = SpoonUtil.defaultLauncher();
      SpoonUtil.addSources(launcher, sourcesDir);
      final CtModel ctModel = launcher.buildModel();
      assertNotNull(ctModel);
      return ctModel;
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected final JavaView buildJimpleView(final Path jimpleDir) {
    final List<AnalysisInputLocation> inputLocations = List
        .of(new JimpleAnalysisInputLocation(jimpleDir, SourceType.Application));
    return new JavaView(inputLocations);
  }

  protected final JavaView buildSpoonView(final CtModel ctModel) {
    final SpoonModelAnalysisInputLocation inputLocation = SpoonModelAnalysisInputLocation
        .of(ctModel);
    final JavaView spoonView = new JavaView(inputLocation);
    return spoonView;
  }

  protected final <T> DynamicTest checkPaired(final Optional<T> expected, final Optional<T> actual,
      final Function<T, String> namer, final BiConsumer<T, T> checker,
      final BiConsumer<Optional<T>, Optional<T>> unpaired) {
    if (expected.isEmpty() || actual.isEmpty()) {
      return DynamicTest.dynamicTest(namer.apply(actual.or(() -> expected).get()),
          () -> unpaired.accept(expected, actual));
    }
    return DynamicTest.dynamicTest(namer.apply(expected.get()),
        () -> checker.accept(expected.get(), actual.get()));
  }

  protected Entry<JavaView, JavaView> initGroupByteCodeSpoon(final Path sourcesDir,
      final Path classesDir, final Path... ignoredPaths) {
    assertTrue(Files.isDirectory(sourcesDir));
    assertTrue(Files.isDirectory(classesDir));

    final JavaView jiByteCodeView = buildByteCodeView(classesDir, ignoredPaths);
    assertNotNull(jiByteCodeView);

    final CtModel ctModel = buildCtModel(sourcesDir);
    assertNotNull(ctModel);

    final JavaView jiSpoonView = buildSpoonView(ctModel);
    return Map.entry(jiByteCodeView, jiSpoonView);
  }

  protected Entry<JavaView, JavaView> initGroupJimpleByteCode(final Path sourcesDir,
      final Path classesDir, final Path... ignoredPaths) {
    assertTrue(Files.isDirectory(sourcesDir));
    assertTrue(Files.isDirectory(classesDir));

    final JavaView jiJimpleView = buildJimpleView(sourcesDir);

    final JavaView jiByteCodeView = buildByteCodeView(classesDir, ignoredPaths);
    assertNotNull(jiByteCodeView);
    return Map.entry(jiJimpleView, jiByteCodeView);
  }

  /**
   * Full outer join of the classes contained in the provided views.
   *
   * @param jiExpectedView the view with the expected classes
   * @param jiActualView   the view with the actual classes
   * @return a stream of paired classes, including unmatched expected as well as
   *         unmatched actual
   */
  protected Stream<Entry<Optional<JavaSootClass>, Optional<JavaSootClass>>> pairByClass(
      final JavaView jiExpectedView, final JavaView jiActualView) {
    // FIXME: this is not guaranteed to work as Stream.concat binds early.
    final Map<String, JavaSootClass> expectedClasses = jiExpectedView.getClasses()
        .collect(Collectors.toConcurrentMap(JavaSootClass::getName, Function.identity()));
    final Stream<Entry<Optional<JavaSootClass>, Optional<JavaSootClass>>> pairedClasses = jiActualView
        .getClasses()
        .map(actualClass -> Map.entry(
            Optional.ofNullable(expectedClasses.remove(actualClass.getName())),
            Optional.of(actualClass)));
    return Stream.concat(pairedClasses, expectedClasses.values().stream()
        .map(expectedClass -> Map.entry(Optional.of(expectedClass), Optional.empty())));
  }

  protected final <T> Stream<Entry<Optional<T>, Optional<T>>> pairByMatcher(
      final Collection<T> jiExpected, final Collection<T> jiActualIn,
      final BiPredicate<T, T> matcher) {
    final List<T> jiActual = new ArrayList<>(jiActualIn);
    final Map<T, Optional<T>> matchedMethods = new LinkedHashMap<>();
    jiExpected.forEach(expectedT -> {
      boolean found = false;
      for (final Iterator<T> it = jiActual.iterator(); it.hasNext();) {
        final T actualT = it.next();
        if (matcher.test(expectedT, actualT)) {
          matchedMethods.put(expectedT, Optional.of(actualT));
          it.remove();
          found = true;
          break;
        }
      }
      if (!found) {
        matchedMethods.put(expectedT, Optional.empty());
      }
    });
    final Stream<Entry<Optional<T>, Optional<T>>> matchedStream = matchedMethods.entrySet().stream()
        .map(e -> Map.entry(Optional.of(e.getKey()), e.getValue()));
    if (jiActual.isEmpty()) {
      return matchedStream;
    }
    return Stream.concat(matchedStream,
        jiActual.stream().map(m -> Map.entry(Optional.empty(), Optional.of(m))));
  }

  protected Stream<Entry<Optional<JavaSootField>, Optional<JavaSootField>>> pairFields(
      final JavaSootClass jiExpectedClass, final JavaSootClass jiActualClass) {
    return pairByMatcher(jiExpectedClass.getFields(), jiActualClass.getFields(),
        (e, a) -> e.getName().equals(a.getName()));
  }

  protected Stream<Entry<Optional<JavaSootMethod>, Optional<JavaSootMethod>>> pairMethods(
      final JavaSootClass jiExpectedClass, final JavaSootClass jiActualClass) {
    return pairByMatcher(jiExpectedClass.getMethods(), jiActualClass.getMethods(),
        (a, e) -> a.getSubSignature().equals(e.getSubSignature()));
  }

  protected Stream<DynamicNode> testAll(final JavaView expectedView, final JavaView actualView) {
    return pairByClass(expectedView, actualView).map(classPair -> {
      if (classPair.getKey().isEmpty()) {
        return DynamicTest.dynamicTest(classPair.getValue().get().getType().getClassName(),
            () -> assumeTrue(false, "only in actual"));
      }
      if (classPair.getValue().isEmpty()) {
        return DynamicTest.dynamicTest(classPair.getKey().get().getType().getClassName(),
            () -> assumeTrue(false, "only in expected"));
      }
      final JavaSootClass expectedClass = classPair.getKey().get();
      final JavaSootClass actualClass = classPair.getValue().get();
      final DynamicNode checks = DynamicContainer.dynamicContainer(
          expectedClass.getType().getClassName(),
          concat(
              Stream.of(DynamicTest.dynamicTest("metadata",
                  () -> assertEqualsJavaSootClassMetadata(expectedClass, actualClass))),
              pairFields(expectedClass, actualClass)
                  .map(pair -> checkPaired(pair.getKey(), pair.getValue(), JavaSootField::getName,
                      this::assertEqualsJavaSootField, this::unpairedGeneric)),
              pairMethods(expectedClass, actualClass).map(pair -> checkPaired(pair.getKey(),
                  pair.getValue(), m -> m.getSubSignature().toString(),
                  this::assertEqualsJavaSootMethod, this::unpairedMethod))));
      return checks;
    });
  }

  protected <T> void unpairedGeneric(final Optional<T> expected, final Optional<T> actual) {
    final String where = expected.isEmpty() ? "actual" : "expected";
    assumeTrue(false, "only in " + where);
  }

  protected void unpairedMethod(final Optional<JavaSootMethod> expected,
      final Optional<JavaSootMethod> actual) {
    final String where = expected.isEmpty() ? "actual" : "expected";
    final JavaSootMethod there = expected.isEmpty() ? actual.get() : expected.get();
    final String body = there.isConcrete() ? there.getBody().toString() : "";
    assumeTrue(false, "only in " + where + ":\n" + body);
  }

  private void assertEqualsBody(@NonNull final Body jiExpectedBody,
      final JavaSootMethod jiActualMethod) {
    final BodyAligner bodyAligner = BodyAligner.of();
    final JavaSootMethod jiActualAlignedMethod = bodyAligner.alignedTo(jiActualMethod,
        jiExpectedBody);
//    final JavaSootMethod jiActualAlignedMethod = jiActualMethod;

//    assertEquals(jiExpectedBody.toString(),
//        CommentingPrinter.toString(jiActualAlignedMethod.getBody()));

    assertEquals(jiExpectedBody.toString(), jiActualAlignedMethod.getBody().toString());

    final String expectedCode = JimpleBodySerializer.serialize(jiExpectedBody);
    final String actualCode = JimpleBodySerializer.serialize(jiActualAlignedMethod.getBody());
//    assertEquals(expectedCode, actualCode);

    final StringBuilder expectedJimpleScan = new StringBuilder();
    new DebugScanner(expectedJimpleScan).scan(jiExpectedBody.getStmts());

    final StringBuilder actualAlignedJimpleScan = new StringBuilder();
    new DebugScanner(actualAlignedJimpleScan).scan(jiActualAlignedMethod.getBody().getStmts());
//    this is guaranteed to fail, as we are creating JavaLocals and ByteCode creates JavaSlotLocal
    // assertEquals(expectedJimpleScan.toString(),
    // actualAlignedJimpleScan.toString());
  }

  protected static final URI makeMethodURI(final JavaSootMethod m) {
    try {
      return new URI("method", m.getDeclaringClassType().getFullyQualifiedName(),
          m.getName() + "("
              + m.getParameterTypes().stream().map(Type::toString).collect(Collectors.joining(","))
              + ")");
    } catch (final URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @SafeVarargs
  private <T> Stream<T> concat(final Stream<T> first, final Stream<T>... next) {
    if (0 == next.length) {
      return first;
    }
    Stream<T> current = first;
    for (final Stream<T> n : next) {
      current = Stream.concat(current, n);
    }
    return current;
  }
}
