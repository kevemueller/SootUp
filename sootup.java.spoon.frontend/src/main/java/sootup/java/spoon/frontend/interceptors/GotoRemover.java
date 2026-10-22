package sootup.java.spoon.frontend.interceptors;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sootup.core.graph.BasicBlock;
import sootup.core.graph.MutableBasicBlock;
import sootup.core.graph.MutableControlFlowGraph;
import sootup.core.interceptor.BodyInterceptor;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.stmt.BranchingStmt;
import sootup.core.jimple.common.stmt.FallsThroughStmt;
import sootup.core.jimple.common.stmt.JGotoStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body.BodyBuilder;
import sootup.core.views.View;
import sootup.java.spoon.frontend.util.DebugScanner;

public class GotoRemover implements BodyInterceptor {
  private static final Logger LOG = LoggerFactory.getLogger(GotoRemover.class);

  @Override
  public void interceptBody(@NonNull BodyBuilder builder, @NonNull View view) {
    MutableControlFlowGraph controlFlowGraph = builder.getControlFlowGraph();

    List<? extends BasicBlock<?>> jmpBlocks = controlFlowGraph.getBlocks().stream()
        .filter(b -> b.getHead() == b.getTail() && b.getHead() instanceof JGotoStmt).toList();

    loop: for (BasicBlock<?> jmpBlock0 : jmpBlocks) {
      MutableBasicBlock jmpBlock = (MutableBasicBlock) jmpBlock0;
      JGotoStmt jmp = (JGotoStmt) jmpBlock.getHead();
      LOG.debug("Trying to remove {}" + new DebugScanner().scan(jmp));
      List<MutableBasicBlock> successors = jmpBlock.getSuccessors();
      assert 1 == successors.size() : successors.size() + "!= 1";
      MutableBasicBlock jmpTargetBlock = successors.get(0);
      Stmt jmpTarget = jmpTargetBlock.getHead();
      if (jmpTarget.equals(jmp)) {
        LOG.debug("Skipping, as it branches to itself");
        continue;
      }
      LOG.debug("branching to: {} ", new DebugScanner().scan(jmpTarget));
      for (BasicBlock<?> pre : List.copyOf(jmpBlock.getPredecessors())) {
        Stmt preTail = pre.getTail();
        LOG.debug("branching from: {}", new DebugScanner().scan(preTail));

        switch (preTail) {
        case JIfStmt jIfStmt:
          // creates graphs with infinite loops
          continue loop;
// NOP: see https://github.com/soot-oss/SootUp/issues/1826
//          boolean found = controlFlowGraph.replaceSucessorEdge(jIfStmt, jmp, jmpTarget);
//          List<? extends BasicBlock<?>> preSucc = pre.getSuccessors();
//          for (int idx = 0; idx < preSucc.size(); idx++) {
//            if (jmpBlock.equals(preSucc.get(idx))) {
//              LOG.debug("branching idx {}", idx);
//              controlFlowGraph.removeEdge(jIfStmt, jmp);
//              controlFlowGraph.putEdge(jIfStmt, idx, jmpTarget);
//              break;
//            }
//          }
//          break;
        case BranchingStmt branchingStmt:
          controlFlowGraph.removeEdge(preTail, jmp);
          controlFlowGraph.putEdge(branchingStmt, JGotoStmt.BRANCH_IDX, jmpTarget);
          break;
        case JReturnStmt rstmt:
          // this cannot happen
          break;
        case FallsThroughStmt fstmt:
          JGotoStmt newGoto = Jimple.newGotoStmt(StmtPositionInfo.getNoStmtPositionInfo());
          LOG.debug("introducing {} to split falling through basic block",
              DebugScanner.identify(newGoto));
          controlFlowGraph.putEdge(fstmt, newGoto); // this will put newGoto into a new basic block
                                                    // and then merge that basicblock in!
          controlFlowGraph.putEdge(newGoto, JGotoStmt.BRANCH_IDX, jmpTarget);
          break;
        default:
          break;
        }
      }
      if (jmp.equals(controlFlowGraph.getStartingStmt())) {
        controlFlowGraph.setStartingStmt(jmpTarget);
      }
      controlFlowGraph.removeNode(jmp, false); // will also remove the empty basic block
    }
  }

}
