package sootup.java.spoon.frontend.interceptors;

import org.jspecify.annotations.NonNull;

import sootup.core.graph.MutableControlFlowGraph;
import sootup.core.interceptor.BodyInterceptor;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body.BodyBuilder;
import sootup.core.views.View;

public class NopRemover implements BodyInterceptor {

  @Override
  public void interceptBody(@NonNull BodyBuilder builder, @NonNull View view) {
    // remove NOP glue, we first collect everything into a list, then run the
    // remover to avoid racing on the iteration
    MutableControlFlowGraph controlFlowGraph = builder.getControlFlowGraph();
    controlFlowGraph.getNodes().stream().filter(Stmt::isJNopStmt).toList()
        .forEach(controlFlowGraph::removeNode);
  }

}
