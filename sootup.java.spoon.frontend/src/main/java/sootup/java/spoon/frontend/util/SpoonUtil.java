package sootup.java.spoon.frontend.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.support.Level;
import spoon.support.compiler.FileSystemFile;
import spoon.support.compiler.FileSystemFolder;
import spoon.support.compiler.ZipFolder;

public final class SpoonUtil {
  public static final Path generatedSources = Path.of("target/generated-sources/spoon");
  // public static final Path classes = Path.of("target/classes");
  public static final Path generatedClasses = Path.of("target/generated-classes/spoon");

  public static final Launcher addSources(final Launcher launcher, final Path... addSources)
      throws IOException {
    for (final Path p : addSources) {
      addSource(launcher, p);
    }
    return launcher;
  }

  public static Launcher defaultLauncher() {
    if (null == System.getProperty("spoon.log.path")) {
      System.setProperty("spoon.log.path", "target/spoon-log.log");
    }

    final Launcher launcher = new Launcher();
    final Environment env = launcher.getEnvironment();
    env.setLevel(Level.DEBUG.toString());
    env.setNoClasspath(true);
    env.setAutoImports(true);
    env.setComplianceLevel(21);
    env.setShouldCompile(false);
    env.setCopyResources(false);

    env.setSourceOutputDirectory(generatedSources.toFile());
    env.setBinaryOutputDirectory(generatedClasses.toString());

    // env.setSourceClasspath(sourceClasspath.toArray(new String[0]));

    return launcher;
  }

  private static void addSource(final Launcher launcher, final Path addSource) throws IOException {
    if (Files.isDirectory(addSource)) {
      launcher.addInputResource(
          new FileSystemFolder(addSource.toAbsolutePath() /* .normalize() */.toFile()));
    } else if (addSource.getFileName().toString().endsWith(".zip")) {
      launcher
          .addInputResource(new ZipFolder(addSource.toAbsolutePath() /* .normalize() */.toFile()));
    } else {
      launcher.addInputResource(
          new FileSystemFile(addSource.toAbsolutePath() /* .normalize() */.toFile()));
    }
  }

  private SpoonUtil() {
  }
}
