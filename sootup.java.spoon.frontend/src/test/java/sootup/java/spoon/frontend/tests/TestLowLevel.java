package sootup.java.spoon.frontend.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import sootup.core.util.printer.JimplePrinter;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;
import spoon.experimental.SpoonifierVisitor;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;

public class TestLowLevel extends AbstractTestFrontend {
  private static final Path PKG_DIR = Path.of("sootup", "java", "sample", "ll");
  private static final Path XPKG_DIR = Path.of("sootup", "java", "spoon");
  private static final Path JAVA_SOURCES = Path.of("src", "test", "java").resolve(PKG_DIR);
  private static final Path JIMPLE_SOURCES = Path.of("target", "generated-sources", "jimple");
  private static final Path CLASSES = Path.of("target", "test-classes");

  @Test
  public void generateJimple() {
    final Path ignoredPaths = CLASSES.resolve(XPKG_DIR);
    final JavaView jiByteCodeView = buildByteCodeView(CLASSES.toAbsolutePath(), ignoredPaths);
    assertNotNull(jiByteCodeView);
    final JimplePrinter printer = new JimplePrinter();
    jiByteCodeView.getClasses().forEach(jiClass -> {
      final String[] splits = jiClass.getName().split("\\.");
      splits[splits.length - 1] += ".jimple";
      Path target = JIMPLE_SOURCES;
      for (final String split : splits) {
        target = target.resolve(split);
      }
      try {
        Files.createDirectories(target.getParent());
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(target))) {
          printer.printTo(jiClass, writer);
        }
      } catch (final IOException ex) {
        throw new UncheckedIOException(ex);
      }
    });
  }

  @TestFactory
  public DynamicContainer testAlign() {
    final String classSimpleName = "LoopTest";
//    final List<String> methodSimpleNames = List.of("ppLazyOr");
    final List<String> methodSimpleNames = List.of();
    // List.of("simpleDo", "simpleWhile");

    final Path ignored = CLASSES.resolve(XPKG_DIR);
    assertTrue(Files.exists(ignored));

    final Entry<JavaView, JavaView> views = initGroupByteCodeSpoon(JAVA_SOURCES,
        CLASSES.toAbsolutePath(), ignored);
    final Entry<JavaSootClass, JavaSootClass> classPair = pairByClass(views.getKey(),
        views.getValue())
        .filter(pair -> classSimpleName.equals(pair.getKey().get().getType().getClassName()))
        .map(this::bind).findFirst().get();
    JavaSootClass jiExpectedClass = classPair.getKey();
    JavaSootClass jiActualClass = classPair.getValue();
    final Stream<Entry<JavaSootMethod, JavaSootMethod>> methodPairs = pairMethods(jiExpectedClass,
        jiActualClass)
        .filter(pair -> methodSimpleNames.isEmpty()
            || methodSimpleNames.contains(pair.getKey().get().getName()))
        .map(this::bind);

    System.out.println(URI.create("class:" + jiExpectedClass.getName()));
    return DynamicContainer.dynamicContainer(classSimpleName,
        URI.create("class:" + jiExpectedClass.getName()), methodPairs.map(methodPair -> {
          final JavaSootMethod jiExpectedMethod = methodPair.getKey();
          final JavaSootMethod jiActualMethod = methodPair.getValue();
          System.out.println(methodPair);
          return DynamicTest.dynamicTest(jiExpectedMethod.getName(),
              makeMethodURI(jiExpectedMethod),
              () -> assertEqualsJavaSootMethod(jiExpectedMethod, jiActualMethod));
        }));
  }

  @TestFactory
  public Stream<DynamicNode> testByteCodeSpoon() {
    final Path ignored = CLASSES.resolve(XPKG_DIR);
    assertTrue(Files.exists(ignored));

    final Entry<JavaView, JavaView> views = initGroupByteCodeSpoon(JAVA_SOURCES,
        CLASSES.toAbsolutePath(), ignored);
    return testAll(views.getKey(), views.getValue());
  }

  @TestFactory
  public Stream<DynamicNode> testJimpleByteCode() {
    final Path ignored = CLASSES.resolve(XPKG_DIR);
    assertTrue(Files.exists(ignored));
    final Entry<JavaView, JavaView> views = initGroupJimpleByteCode(JIMPLE_SOURCES,
        CLASSES.toAbsolutePath(), ignored);
    return testAll(views.getKey(), views.getValue());
  }

  @Test
  public void testSpoon() {
    final CtModel ctModel = buildCtModel(JAVA_SOURCES);
    final CtType<?> ctType = ctModel.getAllTypes().stream()
        .filter(t -> "TopEnum".equals(t.getSimpleName())).findFirst().get();
    final SpoonifierVisitor v = new SpoonifierVisitor();
    v.scan(ctType);
    System.out.println(v.getResult());
  }

}
