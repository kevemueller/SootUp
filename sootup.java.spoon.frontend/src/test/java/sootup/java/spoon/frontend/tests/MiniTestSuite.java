package sootup.java.spoon.frontend.tests;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

public class MiniTestSuite extends AbstractTestFrontend {
  private static final Path MINITESTSUITE = Path.of(System.getProperty("user.home"), "git.own",
      "SootUp", "sootup.java.bytecode.frontend", "src", "test", "resources", "miniTestSuite");
//  private static final Path MINITESTSUITE = Path.of("src", "test", "resources", "miniTestSuite");

  @Test
  public void testLocation() throws IOException {
    allSources().forEach(System.out::println);
    assertNotEquals(0, allSources().count());
  }

  @ParameterizedTest
  @ValueSource(strings = { "breakInWhileLoop" })
  public void test1(String methodName) throws IOException {
    String javaVersion = "java6";
    String classSimpleName = "BreakInWhileLoop";
    Path sourcePath = allSources()
        .filter(p -> javaVersion.equals(p.getParent().getFileName().toString())).findFirst().get();
    final Entry<JavaView, JavaView> views = initGroupByteCodeSpoon(sourcePath,
        sourcePath.getParent().resolve("binary"));
    Entry<JavaSootClass, JavaSootClass> classPair = pairByClass(views.getKey(), views.getValue())
//        .peek(System.out::println)
        .filter(pair -> classSimpleName.equals(pair.getKey().get().getType().getClassName()))
        .map(this::bind).findFirst().get();
    Entry<JavaSootMethod, JavaSootMethod> methodPair = pairMethods(classPair.getKey(),
        classPair.getValue()).peek(System.out::println)
        .filter(pair -> methodName.equals(pair.getKey().get().getName())).map(this::bind)
        .findFirst().get();
    assertEqualsJavaSootMethod(methodPair.getKey(), methodPair.getValue());
  }

  @TestFactory
  Stream<DynamicNode> miniTestSuite() throws Exception {
    return allSources().map(sourcePath -> {
      final Entry<JavaView, JavaView> views = initGroupByteCodeSpoon(sourcePath,
          sourcePath.getParent().resolve("binary"));
      final Stream<DynamicNode> allTests = testAll(views.getKey(), views.getValue());
      return DynamicContainer.dynamicContainer(sourcePath.getParent().getFileName().toString(),
          sourcePath.toUri(), allTests);
    });
  }

  private Stream<Path> allSources() throws IOException {
    return Files.walk(MINITESTSUITE, FileVisitOption.FOLLOW_LINKS).filter(Files::isDirectory)
        .filter(p -> p.endsWith("source"));
  }

}
