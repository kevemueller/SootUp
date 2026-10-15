package sootup.java.spoon.frontend.tests;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.bytecode.frontend.inputlocation.JavaModulePathAnalysisInputLocation;
import sootup.java.core.views.JavaView;
import sootup.java.spoon.frontend.inputlocation.SpoonModelAnalysisInputLocation;
import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.reflect.CtModel;

public final class SpoonModelView {

  public static JavaView of(final Launcher ctLauncher) {
    final List<AnalysisInputLocation> jiInputLocations = new ArrayList<>();
    final Environment ctEnvironment = ctLauncher.getEnvironment();
    jiInputLocations.add(new JavaClassPathAnalysisInputLocation(
        String.join(":", ctEnvironment.getSourceClasspath()), SourceType.Library));
    for (final String ctModulePath : ctEnvironment.getSourceModulePath()) {
      jiInputLocations
          .add(new JavaModulePathAnalysisInputLocation(Path.of(ctModulePath), SourceType.Library));
    }
    final CtModel ctModel = ctLauncher.getModel();
    assert ctModel.isBuildModelFinished();
    jiInputLocations.add(SpoonModelAnalysisInputLocation.of(ctModel));
    return new JavaView(jiInputLocations);
  }

  private SpoonModelView() {

  }
}
